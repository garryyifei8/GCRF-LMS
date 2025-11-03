package com.gcrf.library.circulation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 归还图书请求DTO
 *
 * @author GCRF Team
 * @since 2025-10-28
 */
@Data
public class ReturnRequest {

    /**
     * 借阅记录ID
     */
    @NotNull(message = "借阅记录ID不能为空")
    private Long borrowId;

    /**
     * 是否支付罚金
     */
    private Boolean payFine = false;

    /**
     * 支付罚金金额(元)
     */
    private BigDecimal fineAmount;

    /**
     * 支付方式 (CASH-现金, WECHAT-微信, ALIPAY-支付宝, CARD-银行卡)
     */
    private String paymentMethod;

    /**
     * 支付交易号
     */
    private String transactionId;

    /**
     * 备注
     */
    private String remarks;
}
