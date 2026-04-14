package com.gcrf.library.book.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 批量删除请求
 *
 * @author GCRF Team
 */
@Data
public class BatchDeleteRequest {

    /**
     * 要删除的图书ID列表
     */
    @NotEmpty(message = "请选择要删除的图书")
    @Size(max = 100, message = "每次最多删除100本图书")
    private List<Long> ids;
}
