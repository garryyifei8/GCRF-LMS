package com.gcrf.library.circulation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 预约记录详情响应VO
 *
 * @author GCRF Team
 * @since 2025-10-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveDetailVO {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 预约编号
     */
    private String reserveId;

    /**
     * 读者ID
     */
    private Long readerId;

    /**
     * 读者姓名
     */
    private String readerName;

    /**
     * 读者证号
     */
    private String readerCardId;

    /**
     * 读者类型
     */
    private String readerType;

    /**
     * 读者联系电话 (脱敏)
     */
    private String readerPhone;

    /**
     * 图书ID
     */
    private Long bookId;

    /**
     * 图书标题
     */
    private String bookTitle;

    /**
     * 图书ISBN
     */
    private String bookIsbn;

    /**
     * 图书作者
     */
    private String bookAuthor;

    /**
     * 预约日期
     */
    private LocalDateTime reserveDate;

    /**
     * 预约过期日期
     */
    private LocalDateTime expiryDate;

    /**
     * 取书日期
     */
    private LocalDateTime pickupDate;

    /**
     * 取消日期
     */
    private LocalDateTime cancelDate;

    /**
     * 状态 (RESERVED-已预约, PICKED_UP-已取书, CANCELLED-已取消, EXPIRED-已过期)
     */
    private String status;

    /**
     * 是否已发送通知
     */
    private Boolean notifySent;

    /**
     * 通知发送日期
     */
    private LocalDateTime notifySentDate;

    /**
     * 通知发送次数
     */
    private Integer notifyCount;

    /**
     * 是否即将过期 (剩余1天内)
     */
    private Boolean expiringSoon;

    /**
     * 剩余天数
     */
    private Long remainingDays;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
