package com.gcrf.library.circulation.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预约就绪事件
 * 当预约的图书可供取书时发布此事件，触发短信通知
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationReadyEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件ID
     */
    private String eventId;

    /**
     * 事件类型
     */
    @Builder.Default
    private String eventType = "RESERVATION_READY";

    /**
     * 事件发生时间
     */
    private LocalDateTime eventTime;

    /**
     * 预约记录ID
     */
    private Long reserveId;

    /**
     * 预约编号
     */
    private String reserveIdStr;

    /**
     * 读者ID
     */
    private Long readerId;

    /**
     * 读者姓名
     */
    private String readerName;

    /**
     * 读者电话
     */
    private String readerPhone;

    /**
     * 读者邮箱
     */
    private String readerEmail;

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
     * 预约日期
     */
    private LocalDateTime reserveDate;

    /**
     * 预约过期日期
     */
    private LocalDateTime expiryDate;

    /**
     * 取书地点
     */
    private String pickupLocation;

    /**
     * 通知类型 (SMS, EMAIL, BOTH)
     */
    @Builder.Default
    private String notificationType = "SMS";

    /**
     * 剩余取书天数
     */
    private Integer remainingDays;
}
