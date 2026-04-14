package com.gcrf.library.chat.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gcrf.library.chat.entity.ChatIntent;
import com.gcrf.library.chat.mapper.ChatIntentMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 意图识别器
 *
 * 基于关键词匹配和相似度计算的意图识别引擎
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntentRecognizer {

    private final ChatIntentMapper chatIntentMapper;
    private final JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();

    /**
     * 意图缓存
     */
    private final Cache<String, List<ChatIntent>> intentCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    private static final String INTENT_CACHE_KEY = "all_intents";

    /**
     * 初始化加载意图
     */
    @PostConstruct
    public void init() {
        loadIntents();
    }

    /**
     * 加载所有意图到缓存
     */
    public void loadIntents() {
        try {
            LambdaQueryWrapper<ChatIntent> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ChatIntent::getStatus, 1)
                    .isNull(ChatIntent::getDeletedAt);
            List<ChatIntent> intents = chatIntentMapper.selectList(wrapper);
            intentCache.put(INTENT_CACHE_KEY, intents);
            log.info("Loaded {} intents into cache", intents.size());
        } catch (Exception e) {
            log.error("Failed to load intents", e);
        }
    }

    /**
     * 获取所有意图
     */
    private List<ChatIntent> getAllIntents() {
        List<ChatIntent> intents = intentCache.getIfPresent(INTENT_CACHE_KEY);
        if (intents == null || intents.isEmpty()) {
            loadIntents();
            intents = intentCache.getIfPresent(INTENT_CACHE_KEY);
        }
        return intents != null ? intents : Collections.emptyList();
    }

    /**
     * 识别用户输入的意图
     *
     * @param userInput 用户输入文本
     * @return 意图识别结果
     */
    public IntentResult recognize(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return createFallbackResult();
        }

        String normalizedInput = normalizeText(userInput);
        List<ChatIntent> allIntents = getAllIntents();

        // 计算每个意图的匹配得分
        List<IntentScore> scores = new ArrayList<>();
        for (ChatIntent intent : allIntents) {
            double score = calculateIntentScore(normalizedInput, intent);
            if (score > 0) {
                scores.add(new IntentScore(intent, score));
            }
        }

        // 按得分降序排序
        scores.sort((a, b) -> Double.compare(b.score, a.score));

        if (scores.isEmpty() || scores.get(0).score < 0.3) {
            return createFallbackResult();
        }

        IntentScore bestMatch = scores.get(0);
        ChatIntent intent = bestMatch.intent;

        // 提取实体
        Map<String, String> entities = extractEntities(userInput, intent);

        return IntentResult.builder()
                .intentCode(intent.getCode())
                .intentName(intent.getName())
                .confidence(BigDecimal.valueOf(bestMatch.score).setScale(4, RoundingMode.HALF_UP))
                .entities(entities)
                .actionType(intent.getActionType())
                .responseTemplate(intent.getResponseTemplate())
                .build();
    }

    /**
     * 计算意图匹配得分
     */
    private double calculateIntentScore(String input, ChatIntent intent) {
        List<String> patterns = intent.getPatterns();
        if (patterns == null || patterns.isEmpty()) {
            return 0;
        }

        double maxScore = 0;

        for (String pattern : patterns) {
            String normalizedPattern = normalizeText(pattern);

            // 1. 完全包含匹配（高权重）
            if (input.contains(normalizedPattern)) {
                double containScore = 0.8 + (0.2 * normalizedPattern.length() / Math.max(input.length(), 1));
                maxScore = Math.max(maxScore, Math.min(containScore, 1.0));
            }

            // 2. 相似度匹配
            double simScore = similarity.apply(input, normalizedPattern);
            maxScore = Math.max(maxScore, simScore);

            // 3. 关键词匹配
            String[] inputTokens = input.split("\\s+");
            String[] patternTokens = normalizedPattern.split("\\s+");
            int matchedTokens = 0;
            for (String inputToken : inputTokens) {
                for (String patternToken : patternTokens) {
                    if (inputToken.contains(patternToken) || patternToken.contains(inputToken)) {
                        matchedTokens++;
                        break;
                    }
                }
            }
            if (patternTokens.length > 0) {
                double tokenScore = (double) matchedTokens / patternTokens.length;
                maxScore = Math.max(maxScore, tokenScore * 0.9);
            }
        }

        return maxScore;
    }

    /**
     * 提取实体
     */
    private Map<String, String> extractEntities(String input, ChatIntent intent) {
        Map<String, String> entities = new HashMap<>();
        List<String> entityTypes = intent.getEntities();

        if (entityTypes == null || entityTypes.isEmpty()) {
            return entities;
        }

        // 简单的实体提取逻辑
        for (String entityType : entityTypes) {
            switch (entityType) {
                case "book_title" -> extractBookTitle(input, entities);
                case "author" -> extractAuthor(input, entities);
                case "reader_type" -> extractReaderType(input, entities);
                case "time" -> extractTime(input, entities);
                case "fine_amount" -> extractFineAmount(input, entities);
            }
        }

        return entities;
    }

    private void extractBookTitle(String input, Map<String, String> entities) {
        // 提取书名号中的内容
        int start = input.indexOf("《");
        int end = input.indexOf("》");
        if (start != -1 && end != -1 && end > start) {
            entities.put("book_title", input.substring(start + 1, end));
        }
    }

    private void extractAuthor(String input, Map<String, String> entities) {
        // 提取作者相关信息
        String[] authorPatterns = {"作者", "写的", "著"};
        for (String pattern : authorPatterns) {
            int idx = input.indexOf(pattern);
            if (idx != -1) {
                // 尝试提取作者名
                String after = input.substring(Math.max(0, idx - 10), idx);
                if (!after.isEmpty()) {
                    entities.put("author", after.trim());
                    break;
                }
            }
        }
    }

    private void extractReaderType(String input, Map<String, String> entities) {
        if (input.contains("学生")) {
            entities.put("reader_type", "STUDENT");
        } else if (input.contains("教师") || input.contains("老师")) {
            entities.put("reader_type", "TEACHER");
        } else if (input.contains("VIP")) {
            entities.put("reader_type", "VIP");
        }
    }

    private void extractTime(String input, Map<String, String> entities) {
        // 提取时间相关信息
        if (input.contains("周末") || input.contains("周六") || input.contains("周日")) {
            entities.put("time", "WEEKEND");
        } else if (input.contains("工作日") || input.contains("周一") || input.contains("周五")) {
            entities.put("time", "WEEKDAY");
        } else if (input.contains("节假日") || input.contains("假期")) {
            entities.put("time", "HOLIDAY");
        }
    }

    private void extractFineAmount(String input, Map<String, String> entities) {
        // 提取金额
        StringBuilder amount = new StringBuilder();
        boolean foundDigit = false;
        for (char c : input.toCharArray()) {
            if (Character.isDigit(c) || c == '.') {
                amount.append(c);
                foundDigit = true;
            } else if (foundDigit) {
                break;
            }
        }
        if (foundDigit && !amount.isEmpty()) {
            entities.put("fine_amount", amount.toString());
        }
    }

    /**
     * 标准化文本
     */
    private String normalizeText(String text) {
        if (text == null) return "";
        return text.toLowerCase()
                .replaceAll("[\\s\\p{Punct}]+", " ")
                .trim();
    }

    /**
     * 创建回退结果
     */
    private IntentResult createFallbackResult() {
        return IntentResult.builder()
                .intentCode("UNKNOWN")
                .intentName("未知意图")
                .confidence(BigDecimal.ZERO)
                .entities(new HashMap<>())
                .actionType("NONE")
                .build();
    }

    /**
     * 意图得分内部类
     */
    private record IntentScore(ChatIntent intent, double score) {
    }

    /**
     * 刷新意图缓存
     */
    public void refreshCache() {
        intentCache.invalidateAll();
        loadIntents();
    }
}
