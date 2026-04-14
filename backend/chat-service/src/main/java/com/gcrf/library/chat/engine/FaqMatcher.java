package com.gcrf.library.chat.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gcrf.library.chat.entity.FaqKnowledge;
import com.gcrf.library.chat.mapper.FaqKnowledgeMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * FAQ匹配器
 *
 * 负责根据用户输入和意图匹配最佳FAQ答案
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FaqMatcher {

    private final FaqKnowledgeMapper faqKnowledgeMapper;
    private final JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();

    /**
     * FAQ缓存
     */
    private final Cache<String, List<FaqKnowledge>> faqCache = CacheBuilder.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    private static final String FAQ_CACHE_KEY = "all_faqs";

    /**
     * 初始化加载FAQ
     */
    @PostConstruct
    public void init() {
        loadFaqs();
    }

    /**
     * 加载所有FAQ到缓存
     */
    public void loadFaqs() {
        try {
            LambdaQueryWrapper<FaqKnowledge> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FaqKnowledge::getStatus, 1)
                    .isNull(FaqKnowledge::getDeletedAt)
                    .orderByDesc(FaqKnowledge::getPriority)
                    .orderByDesc(FaqKnowledge::getViewCount);
            List<FaqKnowledge> faqs = faqKnowledgeMapper.selectList(wrapper);
            faqCache.put(FAQ_CACHE_KEY, faqs);
            log.info("Loaded {} FAQs into cache", faqs.size());
        } catch (Exception e) {
            log.error("Failed to load FAQs", e);
        }
    }

    /**
     * 获取所有FAQ
     */
    private List<FaqKnowledge> getAllFaqs() {
        List<FaqKnowledge> faqs = faqCache.getIfPresent(FAQ_CACHE_KEY);
        if (faqs == null || faqs.isEmpty()) {
            loadFaqs();
            faqs = faqCache.getIfPresent(FAQ_CACHE_KEY);
        }
        return faqs != null ? faqs : Collections.emptyList();
    }

    /**
     * 根据意图匹配FAQ
     *
     * @param intentResult 意图识别结果
     * @param userInput 用户原始输入
     * @return 匹配的FAQ列表
     */
    public List<FaqKnowledge> matchByIntent(IntentResult intentResult, String userInput) {
        List<FaqKnowledge> allFaqs = getAllFaqs();

        if (!intentResult.isMatched() || "UNKNOWN".equals(intentResult.getIntentCode())) {
            // 意图未知时，通过关键词匹配
            return matchByKeywords(userInput, allFaqs);
        }

        // 先按意图标签筛选
        List<FaqKnowledge> intentMatched = allFaqs.stream()
                .filter(faq -> faq.getIntentTags() != null &&
                        faq.getIntentTags().contains(intentResult.getIntentCode()))
                .collect(Collectors.toList());

        if (intentMatched.isEmpty()) {
            // 没有匹配意图标签的FAQ，退回关键词匹配
            return matchByKeywords(userInput, allFaqs);
        }

        // 在意图匹配的FAQ中进行关键词排序
        return rankByRelevance(userInput, intentMatched);
    }

    /**
     * 根据关键词匹配FAQ
     */
    private List<FaqKnowledge> matchByKeywords(String userInput, List<FaqKnowledge> faqs) {
        String normalizedInput = normalizeText(userInput);
        List<String> inputKeywords = extractKeywords(normalizedInput);

        List<FaqScore> scores = new ArrayList<>();

        for (FaqKnowledge faq : faqs) {
            double score = calculateFaqScore(normalizedInput, inputKeywords, faq);
            if (score > 0.2) {
                scores.add(new FaqScore(faq, score));
            }
        }

        scores.sort((a, b) -> Double.compare(b.score, a.score));

        return scores.stream()
                .limit(5)
                .map(FaqScore::faq)
                .collect(Collectors.toList());
    }

    /**
     * 根据相关性排序
     */
    private List<FaqKnowledge> rankByRelevance(String userInput, List<FaqKnowledge> faqs) {
        String normalizedInput = normalizeText(userInput);
        List<String> inputKeywords = extractKeywords(normalizedInput);

        List<FaqScore> scores = new ArrayList<>();
        for (FaqKnowledge faq : faqs) {
            double score = calculateFaqScore(normalizedInput, inputKeywords, faq);
            scores.add(new FaqScore(faq, score));
        }

        scores.sort((a, b) -> {
            // 先按优先级，再按匹配得分
            int priorityCompare = Integer.compare(b.faq.getPriority(), a.faq.getPriority());
            if (priorityCompare != 0) return priorityCompare;
            return Double.compare(b.score, a.score);
        });

        return scores.stream()
                .limit(5)
                .map(FaqScore::faq)
                .collect(Collectors.toList());
    }

    /**
     * 计算FAQ匹配得分
     */
    private double calculateFaqScore(String input, List<String> inputKeywords, FaqKnowledge faq) {
        double maxScore = 0;

        // 1. 问题相似度
        String normalizedQuestion = normalizeText(faq.getQuestion());
        double questionSim = similarity.apply(input, normalizedQuestion);
        maxScore = Math.max(maxScore, questionSim);

        // 2. 关键词匹配
        List<String> faqKeywords = faq.getKeywords();
        if (faqKeywords != null && !faqKeywords.isEmpty()) {
            int matchedCount = 0;
            for (String inputKeyword : inputKeywords) {
                for (String faqKeyword : faqKeywords) {
                    if (inputKeyword.contains(faqKeyword) || faqKeyword.contains(inputKeyword)) {
                        matchedCount++;
                        break;
                    }
                }
            }
            double keywordScore = faqKeywords.isEmpty() ? 0 :
                    (double) matchedCount / faqKeywords.size();
            maxScore = Math.max(maxScore, keywordScore);
        }

        // 3. 包含匹配
        for (String keyword : inputKeywords) {
            if (normalizedQuestion.contains(keyword)) {
                maxScore = Math.max(maxScore, 0.7);
                break;
            }
        }

        return maxScore;
    }

    /**
     * 提取关键词
     */
    private List<String> extractKeywords(String text) {
        // 简单的关键词提取：按空格分词，过滤停用词
        Set<String> stopWords = Set.of("的", "是", "在", "了", "和", "与",
                "有", "我", "你", "他", "她", "它", "吗", "呢", "啊", "吧",
                "怎么", "如何", "什么", "哪里", "哪个", "可以", "能", "要");

        return Arrays.stream(text.split("\\s+"))
                .filter(word -> word.length() >= 1)
                .filter(word -> !stopWords.contains(word))
                .distinct()
                .collect(Collectors.toList());
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
     * 获取相关问题
     *
     * @param currentFaqId 当前FAQ ID
     * @param limit 数量限制
     * @return 相关FAQ列表
     */
    public List<FaqKnowledge> getRelatedFaqs(Long currentFaqId, int limit) {
        List<FaqKnowledge> allFaqs = getAllFaqs();

        FaqKnowledge currentFaq = allFaqs.stream()
                .filter(f -> f.getId().equals(currentFaqId))
                .findFirst()
                .orElse(null);

        if (currentFaq == null) {
            return Collections.emptyList();
        }

        // 同分类的其他FAQ
        return allFaqs.stream()
                .filter(f -> !f.getId().equals(currentFaqId))
                .filter(f -> f.getCategoryId().equals(currentFaq.getCategoryId()))
                .sorted((a, b) -> Integer.compare(b.getPriority(), a.getPriority()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 增加FAQ查看次数
     */
    public void incrementViewCount(Long faqId) {
        try {
            faqKnowledgeMapper.incrementViewCount(faqId);
        } catch (Exception e) {
            log.error("Failed to increment view count for FAQ {}", faqId, e);
        }
    }

    /**
     * 刷新FAQ缓存
     */
    public void refreshCache() {
        faqCache.invalidateAll();
        loadFaqs();
    }

    /**
     * FAQ得分内部类
     */
    private record FaqScore(FaqKnowledge faq, double score) {
    }
}
