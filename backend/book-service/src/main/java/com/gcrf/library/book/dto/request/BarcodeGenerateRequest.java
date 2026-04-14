package com.gcrf.library.book.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 条码生成请求
 *
 * @author GCRF Team
 */
@Data
public class BarcodeGenerateRequest {

    /**
     * 图书ID列表（为这些图书生成条码）
     */
    @NotNull(message = "请选择要生成条码的图书")
    @Size(min = 1, max = 100, message = "每次最多为100本图书生成条码")
    private List<Long> bookIds;

    /**
     * 条码前缀（可选，默认使用系统配置）
     */
    @Size(max = 10, message = "条码前缀不能超过10个字符")
    private String prefix;
}
