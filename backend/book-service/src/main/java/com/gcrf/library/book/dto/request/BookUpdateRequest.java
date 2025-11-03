package com.gcrf.library.book.dto.request;

import com.gcrf.library.book.entity.Book;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 更新图书请求DTO
 *
 * @author GCRF Team
 * @date 2025-10-28
 */
@Data
public class BookUpdateRequest {

    /**
     * 图书ID（必填）
     */
    @NotNull(message = "图书ID不能为空")
    private Long id;

    /**
     * ISBN号
     */
    @NotBlank(message = "ISBN不能为空")
    @Pattern(regexp = "^97[89]\\d{10}$", message = "ISBN格式不正确，必须是13位以97开头的数字")
    private String isbn;

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
    private String classificationCode;

    /**
     * 主题关键词
     */
    private String subjectKeywords;

    /**
     * 摘要/简介
     */
    private String description;

    /**
     * 封面图片URL
     */
    private String coverUrl;

    /**
     * 馆藏总数
     */
    @NotNull(message = "库存数量不能为空")
    @Min(value = 0, message = "库存数量不能为负数")
    private Integer totalQuantity;

    /**
     * 可借数量
     */
    @Min(value = 0, message = "可借数量不能为负数")
    private Integer availableQuantity;

    /**
     * 状态：ACTIVE-正常 INACTIVE-下架
     */
    private String status;

    /**
     * 转换为实体
     */
    public Book toEntity() {
        Book book = new Book();
        book.setId(this.id);
        book.setIsbn(this.isbn);
        book.setTitle(this.title);
        book.setSubtitle(this.subtitle);
        book.setAuthor(this.author);
        book.setTranslator(this.translator);
        book.setPublisher(this.publisher);
        book.setPublishDate(this.publishDate);
        book.setEdition(this.edition);
        book.setPages(this.pages);
        book.setPrice(this.price);
        book.setBinding(this.binding);
        book.setLanguage(this.language);
        book.setClassificationCode(this.classificationCode);
        book.setSubjectKeywords(this.subjectKeywords);
        book.setDescription(this.description);
        book.setCoverUrl(this.coverUrl);
        book.setTotalQuantity(this.totalQuantity);
        book.setAvailableQuantity(this.availableQuantity);
        book.setStatus(this.status);
        return book;
    }
}
