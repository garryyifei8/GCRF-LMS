package com.gcrf.library.book.dto.response;

import com.gcrf.library.book.entity.Book;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 条码信息VO
 *
 * @author GCRF Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarcodeVO {

    /**
     * 图书ID
     */
    private Long bookId;

    /**
     * 条码
     */
    private String barcode;

    /**
     * ISBN
     */
    private String isbn;

    /**
     * 书名
     */
    private String title;

    /**
     * 作者
     */
    private String author;

    /**
     * 从Book实体创建
     */
    public static BarcodeVO from(Book book) {
        return BarcodeVO.builder()
                .bookId(book.getId())
                .barcode(book.getBarcode())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .author(book.getAuthor())
                .build();
    }
}
