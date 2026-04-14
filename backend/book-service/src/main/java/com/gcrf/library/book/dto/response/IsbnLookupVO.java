package com.gcrf.library.book.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ISBN查询结果VO
 *
 * @author GCRF Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IsbnLookupVO {

    /**
     * ISBN
     */
    private String isbn;

    /**
     * 书名
     */
    private String title;

    /**
     * 副标题
     */
    private String subtitle;

    /**
     * 作者列表
     */
    private List<String> authors;

    /**
     * 出版社
     */
    private String publisher;

    /**
     * 出版日期
     */
    private String publishDate;

    /**
     * 页数
     */
    private Integer pages;

    /**
     * 封面图片URL
     */
    private String coverUrl;

    /**
     * 描述/简介
     */
    private String description;

    /**
     * 语言
     */
    private String language;

    /**
     * 分类
     */
    private List<String> categories;

    /**
     * 数据来源
     */
    private String source;

    /**
     * 是否找到数据
     */
    private boolean found;

    /**
     * 获取作者字符串（逗号分隔）
     */
    public String getAuthorsString() {
        if (authors == null || authors.isEmpty()) {
            return null;
        }
        return String.join(", ", authors);
    }

    /**
     * 创建未找到结果
     */
    public static IsbnLookupVO notFound(String isbn) {
        return IsbnLookupVO.builder()
                .isbn(isbn)
                .found(false)
                .build();
    }
}
