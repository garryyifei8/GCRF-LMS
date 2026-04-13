package com.gcrf.library.common.feign.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.gcrf.library.common.feign.constant.FeignConstants;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel规则初始化器
 * 初始化Feign客户端的熔断降级规则
 *
 * @author GCRF Team
 * @since 2025-12-01
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "library.feign.sentinel", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(FeignProperties.class)
public class SentinelRuleInitializer {

    private final FeignProperties feignProperties;

    /**
     * 服务资源名称列表
     */
    private static final String[] SERVICE_RESOURCES = {
            FeignConstants.AUTH_SERVICE,
            FeignConstants.BOOK_SERVICE,
            FeignConstants.READER_SERVICE,
            FeignConstants.CIRCULATION_SERVICE,
            FeignConstants.SYSTEM_SERVICE,
            FeignConstants.NOTIFICATION_SERVICE
    };

    @PostConstruct
    public void init() {
        log.info("初始化Sentinel规则...");
        initFlowRules();
        initDegradeRules();
        log.info("Sentinel规则初始化完成");
    }

    /**
     * 初始化流量控制规则
     */
    private void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        for (String resource : SERVICE_RESOURCES) {
            FlowRule rule = new FlowRule();
            rule.setResource(resource);
            // QPS限流
            rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
            // 阈值：每秒100个请求
            rule.setCount(100);
            // 流控效果：快速失败
            rule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
            rules.add(rule);

            log.debug("添加流量控制规则: resource={}, qps={}", resource, 100);
        }

        FlowRuleManager.loadRules(rules);
        log.info("流量控制规则加载完成: {} 条规则", rules.size());
    }

    /**
     * 初始化熔断降级规则
     */
    private void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();
        FeignProperties.Sentinel sentinel = feignProperties.getSentinel();

        for (String resource : SERVICE_RESOURCES) {
            // 慢调用比例熔断规则
            DegradeRule slowCallRule = new DegradeRule();
            slowCallRule.setResource(resource);
            // 熔断策略：慢调用比例
            slowCallRule.setGrade(RuleConstant.DEGRADE_GRADE_RT);
            // 慢调用阈值（毫秒）
            slowCallRule.setCount(sentinel.getSlowCallThreshold());
            // 熔断时长（秒）
            slowCallRule.setTimeWindow(sentinel.getCircuitBreakerTimeout());
            // 最小请求数
            slowCallRule.setMinRequestAmount(sentinel.getMinRequestAmount());
            // 慢调用比例阈值
            slowCallRule.setSlowRatioThreshold(sentinel.getSlowCallRatioThreshold());
            // 统计时长（毫秒）
            slowCallRule.setStatIntervalMs(sentinel.getStatIntervalMs());
            rules.add(slowCallRule);

            log.debug("添加慢调用比例熔断规则: resource={}, threshold={}ms, ratio={}",
                    resource, sentinel.getSlowCallThreshold(), sentinel.getSlowCallRatioThreshold());

            // 异常比例熔断规则
            DegradeRule errorRatioRule = new DegradeRule();
            errorRatioRule.setResource(resource);
            // 熔断策略：异常比例
            errorRatioRule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
            // 异常比例阈值
            errorRatioRule.setCount(sentinel.getErrorRatioThreshold());
            // 熔断时长（秒）
            errorRatioRule.setTimeWindow(sentinel.getCircuitBreakerTimeout());
            // 最小请求数
            errorRatioRule.setMinRequestAmount(sentinel.getMinRequestAmount());
            // 统计时长（毫秒）
            errorRatioRule.setStatIntervalMs(sentinel.getStatIntervalMs());
            rules.add(errorRatioRule);

            log.debug("添加异常比例熔断规则: resource={}, ratio={}",
                    resource, sentinel.getErrorRatioThreshold());

            // 异常数熔断规则
            DegradeRule errorCountRule = new DegradeRule();
            errorCountRule.setResource(resource);
            // 熔断策略：异常数
            errorCountRule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_COUNT);
            // 异常数阈值：5个异常
            errorCountRule.setCount(5);
            // 熔断时长（秒）
            errorCountRule.setTimeWindow(sentinel.getCircuitBreakerTimeout());
            // 最小请求数
            errorCountRule.setMinRequestAmount(sentinel.getMinRequestAmount());
            // 统计时长（毫秒）：60秒
            errorCountRule.setStatIntervalMs(60000);
            rules.add(errorCountRule);

            log.debug("添加异常数熔断规则: resource={}, count={}", resource, 5);
        }

        DegradeRuleManager.loadRules(rules);
        log.info("熔断降级规则加载完成: {} 条规则", rules.size());
    }
}
