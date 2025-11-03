package com.gcrf.library.circulation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 预约记录响应VO - 列表页展示
 *
 * @author GCRF Team
 * @since 2025-10-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveVO {

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
     * 图书ID
     */
    private Long bookId;

    /**
     * 图书标题
     */
    private String bookTitle;

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
     * 是否即将过期 (剩余1天内)
     */
    private Boolean expiringSoon;

    /**
     * 剩余天数
     */
    private Long remainingDays;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
