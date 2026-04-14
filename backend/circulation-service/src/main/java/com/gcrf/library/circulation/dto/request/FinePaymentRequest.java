package com.gcrf.library.circulation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 罚金支付请求
 *
 * @author GCRF Team
 * @date 2025-11-08
 */
@Data
public class FinePaymentRequest {

    /**
     * 借阅记录ID
     */
    @NotNull(message = "借阅记录ID不能为空")
    private Long borrowId;

    /**
     * 支付方式 (CASH-现金, CARD-银行卡, ONLINE-在线支付)
     */
    @NotBlank(message = "支付方式不能为空")
    private String paymentMethod;

    /**
     * 支付备注
     */
    private String remarks;
}
