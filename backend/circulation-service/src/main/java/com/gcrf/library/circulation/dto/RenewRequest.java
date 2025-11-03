package com.gcrf.library.circulation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 续借请求DTO
 *
 * @author GCRF Team
 * @since 2025-10-28
 */
@Data
public class RenewRequest {

    /**
     * 借阅记录ID
     */
    @NotNull(message = "借阅记录ID不能为空")
    private Long borrowId;

    /**
     * 续借天数 (默认30天)
     */
    @Min(value = 1, message = "续借天数至少为1天")
    @Max(value = 90, message = "续借天数最多为90天")
    private Integer renewDays = 30;

    /**
     * 续借理由
     */
    private String reason;

    /**
     * 备注
     */
    private String remarks;
}
