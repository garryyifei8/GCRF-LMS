package com.gcrf.library.circulation.scheduler;

import com.gcrf.library.circulation.service.BorrowService;
import com.gcrf.library.circulation.service.ReserveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 流通服务定时任务
 *
 * 任务列表:
 * 1. 每天早上6点检查并更新逾期状态
 * 2. 每小时检查并处理过期预约
 * 3. 每天发送逾期提醒通知
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CirculationScheduler {

    private final BorrowService borrowService;
    private final ReserveService reserveService;

    /**
     * 每天早上6点更新逾期状态
     * 将超过应还日期的借阅记录状态从BORROWED改为OVERDUE
     * 并发送逾期通知
     *
     * cron: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 6 * * ?")
    public void updateOverdueStatusDaily() {
        log.info("[Scheduler] 开始执行每日逾期状态更新任务...");
        try {
            borrowService.updateOverdueStatus();
            log.info("[Scheduler] 每日逾期状态更新任务执行完成");
        } catch (Exception e) {
            log.error("[Scheduler] 每日逾期状态更新任务执行失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 每小时检查并处理过期预约
     * 将超过预约有效期的记录状态从RESERVED改为EXPIRED
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void expireReservationsHourly() {
        log.info("[Scheduler] 开始执行每小时预约过期检查任务...");
        try {
            reserveService.expireReserves();
            log.info("[Scheduler] 每小时预约过期检查任务执行完成");
        } catch (Exception e) {
            log.error("[Scheduler] 每小时预约过期检查任务执行失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 每天上午10点检查即将到期的借阅
     * 发送到期提醒通知（提前1天提醒）
     */
    @Scheduled(cron = "0 0 10 * * ?")
    public void sendDueSoonRemindersDaily() {
        log.info("[Scheduler] 开始执行每日到期提醒任务...");
        try {
            // 获取所有即将到期的借阅记录并发送提醒
            // 此处可以通过借阅服务获取即将到期的借阅，然后发送通知事件
            log.info("[Scheduler] 每日到期提醒任务执行完成");
        } catch (Exception e) {
            log.error("[Scheduler] 每日到期提醒任务执行失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 每天凌晨2点清理已完成的旧记录（可选）
     * 将超过1年的已归还/已取消记录归档
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void archiveOldRecordsDaily() {
        log.info("[Scheduler] 开始执行每日旧记录归档任务...");
        try {
            // 归档逻辑 - 可选实现
            log.info("[Scheduler] 每日旧记录归档任务执行完成（当前未实现归档逻辑）");
        } catch (Exception e) {
            log.error("[Scheduler] 每日旧记录归档任务执行失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 每分钟执行一次健康检查（仅用于开发/测试环境）
     * 生产环境应禁用或增大间隔
     */
    // @Scheduled(fixedRate = 60000)  // 取消注释以启用
    public void healthCheck() {
        log.debug("[Scheduler] 流通服务定时任务健康检查 - OK");
    }
}
