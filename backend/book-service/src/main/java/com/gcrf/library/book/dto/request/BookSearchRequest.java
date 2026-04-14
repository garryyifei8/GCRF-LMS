package com.gcrf.library.book.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 图书搜索请求
 *
 * @author GCRF Team
 * @date 2025-11-04
 */
@Data
public class BookSearchRequest {

    /**
     * 搜索关键词（必填）
     */
    @NotBlank(message = "搜索关键词不能为空")
    @Size(max = 200, message = "搜索关键词长度不能超过200")
    private String query;

    /**
     * 页码
     */
    @Min(value = 1, message = "页码必须大于0")
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页大小必须大于0")
    @Max(value = 100, message = "每页大小不能超过100")
    private Integer pageSize = 20;

    /**
     * 分类ID筛选（可选）
     */
    private Long categoryId;

    /**
     * 出版社筛选（可选）
     */
    @Size(max = 100, message = "出版社长度不能超过100")
    private String publisher;

    /**
     * 语言筛选（可选）
     */
    @Size(max = 50, message = "语言长度不能超过50")
    private String language;

    /**
     * 仅显示有库存的图书
     */
    private Boolean availableOnly = false;
}
