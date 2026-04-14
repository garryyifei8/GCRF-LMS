package com.gcrf.library.book.dto.response;

import com.gcrf.library.book.entity.Inventory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存详情响应VO
 *
 * @author GCRF Team
 * @date 2025-12-20
 */
@Data
@Schema(description = "库存详情响应")
public class InventoryDetailVO {

    /**
     * 库存ID
     */
    @Schema(description = "库存ID")
    private Long id;

    /**
     * 图书ID
     */
    @Schema(description = "图书ID")
    private Long bookId;

    /**
     * 图书标题
     */
    @Schema(description = "图书标题")
    private String bookTitle;

    /**
     * ISBN
     */
    @Schema(description = "ISBN")
    private String isbn;

    /**
     * 存放位置
     */
    @Schema(description = "存放位置")
    private String location;

    /**
     * 书架号
     */
    @Schema(description = "书架号")
    private String shelfNumber;

    /**
     * 馆藏总数
     */
    @Schema(description = "馆藏总数")
    private Integer totalQuantity;

    /**
     * 可借数量
     */
    @Schema(description = "可借数量")
    private Integer availableQuantity;

    /**
     * 已借出数量
     */
    @Schema(description = "已借出数量")
    private Integer borrowedQuantity;

    /**
     * 预约数量
     */
    @Schema(description = "预约数量")
    private Integer reservedQuantity;

    /**
     * 预警阈值
     */
    @Schema(description = "预警阈值")
    private Integer alertThreshold;

    /**
     * 是否需要预警
     */
    @Schema(description = "是否需要预警")
    private Boolean alertRequired;

    /**
     * 最后盘点时间
     */
    @Schema(description = "最后盘点时间")
    private LocalDateTime lastCheckTime;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    /**
     * 从实体转换
     */
    public static InventoryDetailVO from(Inventory entity) {
        if (entity == null) {
            return null;
        }
        InventoryDetailVO vo = new InventoryDetailVO();
        vo.setId(entity.getId());
        vo.setBookId(entity.getBookId());
        vo.setLocation(entity.getLocation());
        vo.setShelfNumber(entity.getShelfNumber());
        vo.setTotalQuantity(entity.getTotalQuantity());
        vo.setAvailableQuantity(entity.getAvailableQuantity());
        vo.setBorrowedQuantity(entity.getBorrowedQuantity());
        vo.setReservedQuantity(entity.getReservedQuantity());
        vo.setAlertThreshold(entity.getAlertThreshold());
        vo.setAlertRequired(entity.isAlertRequired());
        vo.setLastCheckTime(entity.getLastCheckTime());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    /**
     * 从实体转换（带图书信息）
     */
    public static InventoryDetailVO from(Inventory entity, String bookTitle, String isbn) {
        InventoryDetailVO vo = from(entity);
        if (vo != null) {
            vo.setBookTitle(bookTitle);
            vo.setIsbn(isbn);
        }
        return vo;
    }
}
