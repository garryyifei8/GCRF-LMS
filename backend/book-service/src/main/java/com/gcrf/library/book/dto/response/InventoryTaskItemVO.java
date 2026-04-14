package com.gcrf.library.book.dto.response;

import com.gcrf.library.book.entity.InventoryTaskItem;
import com.gcrf.library.book.entity.enums.TaskItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 盘点明细响应VO
 *
 * @author GCRF Team
 * @date 2025-12-20
 */
@Data
@Schema(description = "盘点明细响应")
public class InventoryTaskItemVO {

    /**
     * 明细ID
     */
    @Schema(description = "明细ID")
    private Long id;

    /**
     * 任务ID
     */
    @Schema(description = "任务ID")
    private Long taskId;

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
     * 期望数量
     */
    @Schema(description = "期望数量")
    private Integer expectedQuantity;

    /**
     * 实际数量
     */
    @Schema(description = "实际数量")
    private Integer actualQuantity;

    /**
     * 差异数量
     */
    @Schema(description = "差异数量")
    private Integer discrepancy;

    /**
     * 状态
     */
    @Schema(description = "状态")
    private TaskItemStatus status;

    /**
     * 状态描述
     */
    @Schema(description = "状态描述")
    private String statusDesc;

    /**
     * 盘点时间
     */
    @Schema(description = "盘点时间")
    private LocalDateTime checkedTime;

    /**
     * 盘点人ID
     */
    @Schema(description = "盘点人ID")
    private Long checkerId;

    /**
     * 盘点人姓名
     */
    @Schema(description = "盘点人姓名")
    private String checkerName;

    /**
     * 备注
     */
    @Schema(description = "备注")
    private String notes;

    /**
     * 是否有差异
     */
    @Schema(description = "是否有差异")
    private Boolean hasDiscrepancy;

    /**
     * 是否可以录入结果
     */
    @Schema(description = "是否可以录入结果")
    private Boolean canCheck;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /**
     * 从实体转换
     */
    public static InventoryTaskItemVO from(InventoryTaskItem entity) {
        if (entity == null) {
            return null;
        }
        InventoryTaskItemVO vo = new InventoryTaskItemVO();
        vo.setId(entity.getId());
        vo.setTaskId(entity.getTaskId());
        vo.setBookId(entity.getBookId());
        vo.setBookTitle(entity.getBookTitle());
        vo.setIsbn(entity.getIsbn());
        vo.setExpectedQuantity(entity.getExpectedQuantity());
        vo.setActualQuantity(entity.getActualQuantity());
        vo.setDiscrepancy(entity.getDiscrepancy());
        vo.setStatus(entity.getStatus());
        vo.setStatusDesc(entity.getStatus() != null ? entity.getStatus().getDescription() : null);
        vo.setCheckedTime(entity.getCheckedTime());
        vo.setCheckerId(entity.getCheckerId());
        vo.setCheckerName(entity.getCheckerName());
        vo.setNotes(entity.getNotes());
        vo.setHasDiscrepancy(entity.hasDiscrepancy());
        vo.setCanCheck(entity.canCheck());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
