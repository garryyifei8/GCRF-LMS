package com.gcrf.library.book.dto.response;

import com.gcrf.library.book.entity.Inventory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存预警响应VO
 *
 * @author GCRF Team
 * @date 2025-12-20
 */
@Data
@Schema(description = "库存预警响应")
public class InventoryAlertVO {

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
     * 可借数量
     */
    @Schema(description = "可借数量")
    private Integer availableQuantity;

    /**
     * 预警阈值
     */
    @Schema(description = "预警阈值")
    private Integer alertThreshold;

    /**
     * 差值（可借数量 - 预警阈值）
     */
    @Schema(description = "差值（可借数量 - 预警阈值）")
    private Integer gap;

    /**
     * 预警级别：CRITICAL-紧急（0本可借），WARNING-警告（低于阈值）
     */
    @Schema(description = "预警级别", allowableValues = {"CRITICAL", "WARNING"})
    private String alertLevel;

    /**
     * 最后盘点时间
     */
    @Schema(description = "最后盘点时间")
    private LocalDateTime lastCheckTime;

    /**
     * 从实体转换
     */
    public static InventoryAlertVO from(Inventory entity) {
        if (entity == null) {
            return null;
        }
        InventoryAlertVO vo = new InventoryAlertVO();
        vo.setId(entity.getId());
        vo.setBookId(entity.getBookId());
        vo.setLocation(entity.getLocation());
        vo.setShelfNumber(entity.getShelfNumber());
        vo.setAvailableQuantity(entity.getAvailableQuantity());
        vo.setAlertThreshold(entity.getAlertThreshold());

        int available = entity.getAvailableQuantity() != null ? entity.getAvailableQuantity() : 0;
        int threshold = entity.getAlertThreshold() != null ? entity.getAlertThreshold() : 0;
        vo.setGap(available - threshold);

        // 设置预警级别
        if (available == 0) {
            vo.setAlertLevel("CRITICAL");
        } else {
            vo.setAlertLevel("WARNING");
        }

        vo.setLastCheckTime(entity.getLastCheckTime());
        return vo;
    }

    /**
     * 从实体转换（带图书信息）
     */
    public static InventoryAlertVO from(Inventory entity, String bookTitle, String isbn) {
        InventoryAlertVO vo = from(entity);
        if (vo != null) {
            vo.setBookTitle(bookTitle);
            vo.setIsbn(isbn);
        }
        return vo;
    }
}
