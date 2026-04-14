package com.gcrf.library.chat.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 意图识别结果
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentResult {

    /**
     * 意图编码
     */
    private String intentCode;

    /**
     * 意图名称
     */
    private String intentName;

    /**
     * 置信度 (0-1)
     */
    private BigDecimal confidence;

    /**
     * 提取的实体
     */
    private Map<String, String> entities;

    /**
     * 动作类型: FAQ_LOOKUP, API_CALL, TRANSFER, NONE
     */
    private String actionType;

    /**
     * 响应模板（如果有）
     */
    private String responseTemplate;

    /**
     * 是否匹配成功
     */
    public boolean isMatched() {
        return intentCode != null && confidence != null
                && confidence.compareTo(BigDecimal.valueOf(0.3)) > 0;
    }
}
