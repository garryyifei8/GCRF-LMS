package com.gcrf.library.reader.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 读者类型更新请求
 *
 * @author GCRF Team
 * @date 2025-11-08
 */
@Data
public class ReaderTypeUpdateRequest {

    private Long id;

    @Size(max = 100, message = "类型名称长度不能超过100个字符")
    private String typeName;

    @Min(value = 1, message = "最大借阅数量至少为1")
    @Max(value = 50, message = "最大借阅数量不能超过50")
    private Integer maxBorrowCount;

    @Min(value = 1, message = "最长借阅天数至少为1天")
    @Max(value = 365, message = "最长借阅天数不能超过365天")
    private Integer maxBorrowDays;

    @Min(value = 0, message = "最大续借次数不能为负数")
    @Max(value = 10, message = "最大续借次数不能超过10次")
    private Integer maxRenewCount;

    @Min(value = 0, message = "押金金额不能为负数")
    private Integer depositAmount;

    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;

    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "状态必须为ACTIVE或INACTIVE")
    private String status;

    private Integer sortOrder;
}
