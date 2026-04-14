package com.gcrf.library.book.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 图书实体
 *
 * @author GCRF Team
 * @date 2025-10-12
 */
@Data
@TableName("books")
public class Book {

    /**
     * 图书ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * ISBN号
     */
    @NotBlank(message = "ISBN不能为空")
    @Pattern(regexp = "^97[89]\\d{10}$", message = "ISBN格式不正确，必须是13位以97开头的数字")
    private String isbn;

    /**
     * 条形码
     */
    @Size(max = 50, message = "条形码长度不能超过50")
    private String barcode;

    /**
     * 图书标题
     */
    @NotBlank(message = "书名不能为空")
    @Size(max = 500, message = "书名长度不能超过500")
    private String title;

    /**
     * 副标题
     */
    @Size(max = 500, message = "副标题长度不能超过500")
    private String subtitle;

    /**
     * 作者
     */
    @Size(max = 200, message = "作者长度不能超过200")
    private String author;

    /**
     * 译者
     */
    @Size(max = 200, message = "译者长度不能超过200")
    private String translator;

    /**
     * 出版社
     */
    @Size(max = 200, message = "出版社长度不能超过200")
    private String publisher;

    /**
     * 出版日期
     */
    @TableField("publish_date")
    private LocalDate publishDate;

    /**
     * 版本
     */
    private String edition;

    /**
     * 页数
     */
    @Min(value = 1, message = "页数必须大于0")
    private Integer pages;

    /**
     * 价格
     */
    @DecimalMin(value = "0.00", message = "价格不能为负数")
    private BigDecimal price;

    /**
     * 装帧
     */
    private String binding;

    /**
     * 语言
     */
    private String language;

    /**
     * 分类代码
     */
    @TableField("classification_code")
    private String classificationCode;

    /**
     * 主题关键词
     */
    @TableField("subject_keywords")
    private String subjectKeywords;

    /**
     * 摘要/简介
     */
    @TableField("abstract")
    private String description;

    /**
     * 封面图片URL
     */
    @TableField("cover_url")
    private String coverUrl;

    /**
     * PDF文件URL
     */
    @TableField("pdf_url")
    private String pdfUrl;

    /**
     * PDF文件原始名称
     */
    @TableField("pdf_file_name")
    private String pdfFileName;

    /**
     * PDF文件大小（字节）
     */
    @TableField("pdf_file_size")
    private Long pdfFileSize;

    /**
     * 馆藏总数
     */
    @NotNull(message = "库存数量不能为空")
    @Min(value = 0, message = "库存数量不能为负数")
    @TableField("total_quantity")
    private Integer totalQuantity;

    /**
     * 可借数量
     */
    @NotNull(message = "可借数量不能为空")
    @Min(value = 0, message = "可借数量不能为负数")
    @TableField("available_quantity")
    private Integer availableQuantity;

    /**
     * 已借出数量
     */
    @TableField("borrowed_quantity")
    private Integer borrowedQuantity;

    /**
     * 预约数量
     */
    @TableField("reserved_quantity")
    private Integer reservedQuantity;

    /**
     * 版本号（乐观锁）
     */
    @Version
    @TableField("version")
    private Long version;

    /**
     * 状态：ACTIVE-正常 INACTIVE-下架
     */
    private String status;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 删除时间（逻辑删除 - 手动处理）
     */
    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
