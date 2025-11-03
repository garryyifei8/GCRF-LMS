package com.gcrf.library.circulation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 预约图书请求DTO
 *
 * @author GCRF Team
 * @since 2025-10-28
 */
@Data
public class ReserveRequest {

    /**
     * 图书ID
     */
    @NotNull(message = "图书ID不能为空")
    private Long bookId;

    /**
     * 读者ID
     */
    @NotNull(message = "读者ID不能为空")
    private Long readerId;

    /**
     * 预约天数 (默认7天)
     */
    private Integer reserveDays = 7;

    /**
     * 备注
     */
    private String remarks;
}
