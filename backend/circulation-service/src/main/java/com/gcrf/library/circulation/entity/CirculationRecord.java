package com.gcrf.library.circulation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 流通记录实体
 *
 * @author GCRF Team
 * @date 2025-10-12
 */
@Data
@TableName("circulation_records")
public class CirculationRecord {

    /**
     * 记录ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 图书ID
     */
    private Long bookId;

    /**
     * 图书标题
     */
    private String bookTitle;

    /**
     * 读者ID
     */
    private Long readerId;

    /**
     * 读者姓名
     */
    private String readerName;

    /**
     * 借阅时间
     */
    private LocalDateTime borrowTime;

    /**
     * 应还时间
     */
    private LocalDateTime dueTime;

    /**
     * 实际归还时间
     */
    private LocalDateTime returnTime;

    /**
     * 续借次数
     */
    private Integer renewCount;

    /**
     * 状态：1-借阅中 2-已归还 3-逾期
     */
    private Integer status;

    /**
     * 罚款金额
     */
    private Long fineAmount;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    private Long createBy;

    /**
     * 更新人
     */
    private Long updateBy;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;
}
