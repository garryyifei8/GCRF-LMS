package com.gcrf.library.circulation.service;

import com.gcrf.library.circulation.event.FinePaidEvent;
import com.gcrf.library.circulation.event.OverdueDetectedEvent;
import com.gcrf.library.circulation.event.ReservationReadyEvent;
import com.gcrf.library.circulation.event.ReturnCompletedEvent;

/**
 * 流通事件发布接口
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
public interface CirculationEventPublisher {

    /**
     * 发布逾期检测事件
     * 发送到邮件通知队列
     *
     * @param event 逾期检测事件
     */
    void publishOverdueDetectedEvent(OverdueDetectedEvent event);

    /**
     * 发布归还完成事件
     * 发送到系统日志队列
     *
     * @param event 归还完成事件
     */
    void publishReturnCompletedEvent(ReturnCompletedEvent event);

    /**
     * 发布预约就绪事件
     * 发送到短信通知队列
     *
     * @param event 预约就绪事件
     */
    void publishReservationReadyEvent(ReservationReadyEvent event);

    /**
     * 发布罚金支付事件
     * 发送到系统日志队列
     *
     * @param event 罚金支付事件
     */
    void publishFinePaidEvent(FinePaidEvent event);
}
