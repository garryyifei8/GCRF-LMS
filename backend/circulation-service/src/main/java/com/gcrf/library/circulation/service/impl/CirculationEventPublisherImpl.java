package com.gcrf.library.circulation.service.impl;

import com.gcrf.library.circulation.config.RabbitMQConfig;
import com.gcrf.library.circulation.event.FinePaidEvent;
import com.gcrf.library.circulation.event.OverdueDetectedEvent;
import com.gcrf.library.circulation.event.ReservationReadyEvent;
import com.gcrf.library.circulation.event.ReturnCompletedEvent;
import com.gcrf.library.circulation.service.CirculationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 流通事件发布服务实现
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CirculationEventPublisherImpl implements CirculationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发布逾期检测事件
     * 路由到邮件通知队列 (notification.email.queue)
     */
    @Override
    public void publishOverdueDetectedEvent(OverdueDetectedEvent event) {
        try {
            // 设置事件元数据
            if (event.getEventId() == null) {
                event.setEventId(generateEventId());
            }
            if (event.getEventTime() == null) {
                event.setEventTime(LocalDateTime.now());
            }

            log.info("[Event] Publishing OVERDUE_DETECTED event: eventId={}, borrowId={}, readerId={}, overdueDays={}",
                    event.getEventId(), event.getBorrowId(), event.getReaderId(), event.getOverdueDays());

            // 发送到通知交换机，路由到邮件队列
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_EXCHANGE,
                    "notification.email.overdue",
                    event
            );

            log.info("[Event] OVERDUE_DETECTED event published successfully: eventId={}", event.getEventId());

        } catch (AmqpException e) {
            log.error("[Event] Failed to publish OVERDUE_DETECTED event: eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
            // 不抛出异常，避免影响主业务流程
        }
    }

    /**
     * 发布归还完成事件
     * 路由到系统日志队列 (system.log.queue)
     */
    @Override
    public void publishReturnCompletedEvent(ReturnCompletedEvent event) {
        try {
            // 设置事件元数据
            if (event.getEventId() == null) {
                event.setEventId(generateEventId());
            }
            if (event.getEventTime() == null) {
                event.setEventTime(LocalDateTime.now());
            }

            log.info("[Event] Publishing RETURN_COMPLETED event: eventId={}, borrowId={}, readerId={}, bookId={}",
                    event.getEventId(), event.getBorrowId(), event.getReaderId(), event.getBookId());

            // 发送到系统日志交换机
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SYSTEM_LOG_EXCHANGE,
                    "system.log.circulation.return",
                    event
            );

            log.info("[Event] RETURN_COMPLETED event published successfully: eventId={}", event.getEventId());

        } catch (AmqpException e) {
            log.error("[Event] Failed to publish RETURN_COMPLETED event: eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
        }
    }

    /**
     * 发布预约就绪事件
     * 路由到短信通知队列 (notification.sms.queue)
     */
    @Override
    public void publishReservationReadyEvent(ReservationReadyEvent event) {
        try {
            // 设置事件元数据
            if (event.getEventId() == null) {
                event.setEventId(generateEventId());
            }
            if (event.getEventTime() == null) {
                event.setEventTime(LocalDateTime.now());
            }

            log.info("[Event] Publishing RESERVATION_READY event: eventId={}, reserveId={}, readerId={}, bookId={}",
                    event.getEventId(), event.getReserveId(), event.getReaderId(), event.getBookId());

            // 发送到通知交换机，路由到短信队列
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_EXCHANGE,
                    "notification.sms.reservation",
                    event
            );

            log.info("[Event] RESERVATION_READY event published successfully: eventId={}", event.getEventId());

        } catch (AmqpException e) {
            log.error("[Event] Failed to publish RESERVATION_READY event: eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
        }
    }

    /**
     * 发布罚金支付事件
     * 路由到系统日志队列
     */
    @Override
    public void publishFinePaidEvent(FinePaidEvent event) {
        try {
            // 设置事件元数据
            if (event.getEventId() == null) {
                event.setEventId(generateEventId());
            }
            if (event.getEventTime() == null) {
                event.setEventTime(LocalDateTime.now());
            }

            log.info("[Event] Publishing FINE_PAID event: eventId={}, borrowId={}, readerId={}, amount={}",
                    event.getEventId(), event.getBorrowId(), event.getReaderId(), event.getFineAmount());

            // 发送到系统日志交换机
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SYSTEM_LOG_EXCHANGE,
                    "system.log.circulation.fine",
                    event
            );

            log.info("[Event] FINE_PAID event published successfully: eventId={}", event.getEventId());

        } catch (AmqpException e) {
            log.error("[Event] Failed to publish FINE_PAID event: eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
        }
    }

    /**
     * 生成事件ID
     * 格式: EVT-{timestamp}-{random}
     */
    private String generateEventId() {
        return "EVT-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
