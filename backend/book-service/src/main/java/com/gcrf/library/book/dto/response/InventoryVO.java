package com.gcrf.library.book.dto.response;

import com.gcrf.library.book.entity.Book;
import lombok.Data;

/**
 * 库存响应VO
 *
 * @author GCRF Team
 * @date 2025-11-04
 */
@Data
public class InventoryVO {

    /**
     * 图书ID
     */
    private Long bookId;

    /**
     * 图书标题
     */
    private String title;

    /**
     * ISBN
     */
    private String isbn;

    /**
     * 馆藏总数
     */
    private Integer totalCopies;

    /**
     * 可借数量
     */
    private Integer availableCopies;

    /**
     * 已借出数量
     */
    private Integer borrowedCopies;

    /**
     * 预约数量
     */
    private Integer reservedCopies;

    /**
     * 版本号
     */
    private Long version;

    /**
     * 从实体转换
     */
    public static InventoryVO from(Book book) {
        if (book == null) {
            return null;
        }
        InventoryVO vo = new InventoryVO();
        vo.setBookId(book.getId());
        vo.setTitle(book.getTitle());
        vo.setIsbn(book.getIsbn());
        vo.setTotalCopies(book.getTotalQuantity());
        vo.setAvailableCopies(book.getAvailableQuantity());
        vo.setBorrowedCopies(book.getBorrowedQuantity() != null ? book.getBorrowedQuantity() : 0);
        vo.setReservedCopies(book.getReservedQuantity() != null ? book.getReservedQuantity() : 0);
        vo.setVersion(book.getVersion());
        return vo;
    }
}
