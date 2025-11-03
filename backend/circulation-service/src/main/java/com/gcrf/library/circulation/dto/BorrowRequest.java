package com.gcrf.library.circulation.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * 借阅请求DTO
 *
 * @author GCRF Team
 * @date 2025-10-12
 */
@Data
public class BorrowRequest {

    @NotNull(message = "图书ID不能为空")
    private Long bookId;

    @NotNull(message = "读者ID不能为空")
    private Long readerId;

    /**
     * 借阅天数（默认30天）
     */
    private Integer borrowDays = 30;

    /**
     * 备注
     */
    private String remark;
}
