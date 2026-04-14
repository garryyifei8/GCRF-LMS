package com.gcrf.library.circulation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.circulation.dto.request.FinePaymentRequest;
import com.gcrf.library.circulation.dto.response.FineVO;
import com.gcrf.library.circulation.entity.Borrow;
import com.gcrf.library.circulation.event.FinePaidEvent;
import com.gcrf.library.circulation.event.ReturnCompletedEvent;
import com.gcrf.library.circulation.mapper.BorrowMapper;
import com.gcrf.library.circulation.service.CirculationEventPublisher;
import com.gcrf.library.circulation.service.FineService;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 罚金服务实现
 *
 * @author GCRF Team
 * @date 2025-11-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FineServiceImpl implements FineService {

    private final BorrowMapper borrowMapper;
    private final CirculationEventPublisher eventPublisher;

    // 罚金配置
    private static final BigDecimal FINE_PER_DAY = new BigDecimal("0.10"); // 每天0.1元
    private static final BigDecimal MAX_FINE = new BigDecimal("50.00");     // 最高50元
    private static final int FREE_GRACE_DAYS = 3;                           // 免费宽限期(天)

    @Override
    public PageResult<FineVO> queryOverdueRecords(Long readerId, Boolean paid, Integer pageNum, Integer pageSize) {
        log.info("查询逾期记录: readerId={}, paid={}, pageNum={}, pageSize={}", 
                 readerId, paid, pageNum, pageSize);

        // 构建查询条件
        LambdaQueryWrapper<Borrow> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(Borrow::getDeletedAt)
               .eq(Borrow::getStatus, "OVERDUE"); // 逾期状态

        if (readerId != null) {
            wrapper.eq(Borrow::getReaderId, readerId);
        }

        if (paid != null) {
            wrapper.eq(Borrow::getFinePaid, paid);
        }

        wrapper.orderByDesc(Borrow::getBorrowDate);

        // 分页查询
        Page<Borrow> page = new Page<>(pageNum, pageSize);
        Page<Borrow> resultPage = borrowMapper.selectPage(page, wrapper);

        // 转换为VO
        List<FineVO> voList = resultPage.getRecords().stream()
                .map(this::convertToFineVO)
                .collect(Collectors.toList());

        return PageResult.ofRecords(
                resultPage.getTotal(),
                (int) resultPage.getCurrent(),
                (int) resultPage.getSize(),
                voList
        );
    }

    @Override
    public Map<String, Object> calculateFine(Long borrowId) {
        log.info("计算罚金: borrowId={}", borrowId);

        Borrow borrow = borrowMapper.selectById(borrowId);
        if (borrow == null || borrow.getDeletedAt() != null) {
            throw new BusinessException("借阅记录不存在");
        }

        // 计算逾期天数
        LocalDateTime dueDate = borrow.getDueDate();
        LocalDateTime compareDate = borrow.getReturnDate() != null 
                ? borrow.getReturnDate() 
                : LocalDateTime.now();

        long overdueDays = ChronoUnit.DAYS.between(dueDate, compareDate);
        
        if (overdueDays <= FREE_GRACE_DAYS) {
            // 在宽限期内,无罚金
            Map<String, Object> result = new HashMap<>();
            result.put("borrowId", borrowId);
            result.put("overdueDays", 0);
            result.put("fineAmount", BigDecimal.ZERO);
            result.put("message", "未逾期,无需支付罚金");
            return result;
        }

        // 计算罚金: 逾期天数 * 每天罚金
        long actualOverdueDays = overdueDays - FREE_GRACE_DAYS;
        BigDecimal calculatedFine = FINE_PER_DAY.multiply(new BigDecimal(actualOverdueDays));
        
        // 限制最高罚金
        BigDecimal finalFine = calculatedFine.compareTo(MAX_FINE) > 0 ? MAX_FINE : calculatedFine;

        Map<String, Object> result = new HashMap<>();
        result.put("borrowId", borrowId);
        result.put("dueDate", dueDate);
        result.put("returnDate", borrow.getReturnDate());
        result.put("overdueDays", actualOverdueDays);
        result.put("finePerDay", FINE_PER_DAY);
        result.put("calculatedFine", calculatedFine);
        result.put("maxFine", MAX_FINE);
        result.put("fineAmount", finalFine);
        result.put("alreadyPaid", borrow.getFinePaid());

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FineVO payFine(FinePaymentRequest request) {
        log.info("支付罚金: borrowId={}, paymentMethod={}", 
                 request.getBorrowId(), request.getPaymentMethod());

        Borrow borrow = borrowMapper.selectById(request.getBorrowId());
        if (borrow == null || borrow.getDeletedAt() != null) {
            throw new BusinessException("借阅记录不存在");
        }

        if (borrow.getFinePaid()) {
            throw new BusinessException("罚金已支付,无需重复支付");
        }

        if (borrow.getFineAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("无需支付罚金");
        }

        // 更新支付状态
        borrow.setFinePaid(true);
        borrow.setFinePaidDate(LocalDateTime.now());
        
        if (request.getRemarks() != null) {
            borrow.setRemarks(
                (borrow.getRemarks() != null ? borrow.getRemarks() + "; " : "") +
                "罚金支付: " + request.getPaymentMethod() + " - " + request.getRemarks()
            );
        }

        borrowMapper.updateById(borrow);

        log.info("罚金支付成功: borrowId={}, amount={}", request.getBorrowId(), borrow.getFineAmount());

        // 发布罚金支付事件
        publishFinePaidEvent(borrow, request.getPaymentMethod(), request.getRemarks());

        return convertToFineVO(borrow);
    }

    @Override
    public PageResult<FineVO> queryFines(Long readerId, Boolean paid, Integer pageNum, Integer pageSize) {
        log.info("查询罚金记录: readerId={}, paid={}, pageNum={}, pageSize={}", 
                 readerId, paid, pageNum, pageSize);

        LambdaQueryWrapper<Borrow> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(Borrow::getDeletedAt)
               .gt(Borrow::getFineAmount, BigDecimal.ZERO); // 有罚金的记录

        if (readerId != null) {
            wrapper.eq(Borrow::getReaderId, readerId);
        }

        if (paid != null) {
            wrapper.eq(Borrow::getFinePaid, paid);
        }

        wrapper.orderByDesc(Borrow::getBorrowDate);

        Page<Borrow> page = new Page<>(pageNum, pageSize);
        Page<Borrow> resultPage = borrowMapper.selectPage(page, wrapper);

        List<FineVO> voList = resultPage.getRecords().stream()
                .map(this::convertToFineVO)
                .collect(Collectors.toList());

        return PageResult.ofRecords(
                resultPage.getTotal(),
                (int) resultPage.getCurrent(),
                (int) resultPage.getSize(),
                voList
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchReturn(List<Long> borrowIds) {
        log.info("批量归还: borrowIds={}", borrowIds);

        if (borrowIds == null || borrowIds.isEmpty()) {
            throw new BusinessException("借阅记录ID列表不能为空");
        }

        int successCount = 0;
        int failedCount = 0;
        BigDecimal totalFine = BigDecimal.ZERO;
        List<Map<String, Object>> results = new ArrayList<>();

        for (Long borrowId : borrowIds) {
            try {
                Borrow borrow = borrowMapper.selectById(borrowId);
                if (borrow == null || borrow.getDeletedAt() != null) {
                    failedCount++;
                    results.add(createResult(borrowId, false, "借阅记录不存在", null));
                    continue;
                }

                if ("RETURNED".equals(borrow.getStatus())) {
                    failedCount++;
                    results.add(createResult(borrowId, false, "图书已归还", null));
                    continue;
                }

                // 设置归还时间
                LocalDateTime now = LocalDateTime.now();
                borrow.setReturnDate(now);

                // 计算罚金
                Map<String, Object> fineCalc = calculateFine(borrowId);
                BigDecimal fineAmount = (BigDecimal) fineCalc.get("fineAmount");
                
                borrow.setFineAmount(fineAmount);
                
                // 根据是否逾期更新状态
                if (fineAmount.compareTo(BigDecimal.ZERO) > 0) {
                    borrow.setStatus("OVERDUE");
                    borrow.setFinePaid(false);
                } else {
                    borrow.setStatus("RETURNED");
                    borrow.setFinePaid(true);
                }

                borrowMapper.updateById(borrow);

                // 发布归还完成事件
                publishReturnCompletedEvent(borrow, fineAmount);

                successCount++;
                totalFine = totalFine.add(fineAmount);
                results.add(createResult(borrowId, true, "归还成功", fineAmount));

                log.info("批量归还成功: borrowId={}, fine={}", borrowId, fineAmount);

            } catch (Exception e) {
                failedCount++;
                results.add(createResult(borrowId, false, e.getMessage(), null));
                log.error("批量归还失败: borrowId={}, error={}", borrowId, e.getMessage());
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCount", borrowIds.size());
        summary.put("successCount", successCount);
        summary.put("failedCount", failedCount);
        summary.put("totalFine", totalFine);
        summary.put("results", results);

        return summary;
    }

    @Override
    public Map<String, Object> getFineStatistics(Long readerId) {
        log.info("查询罚金统计: readerId={}", readerId);

        LambdaQueryWrapper<Borrow> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(Borrow::getDeletedAt)
               .gt(Borrow::getFineAmount, BigDecimal.ZERO);

        if (readerId != null) {
            wrapper.eq(Borrow::getReaderId, readerId);
        }

        List<Borrow> borrows = borrowMapper.selectList(wrapper);

        BigDecimal totalFine = BigDecimal.ZERO;
        BigDecimal paidFine = BigDecimal.ZERO;
        BigDecimal unpaidFine = BigDecimal.ZERO;
        int totalCount = borrows.size();
        int paidCount = 0;
        int unpaidCount = 0;

        for (Borrow borrow : borrows) {
            BigDecimal fine = borrow.getFineAmount();
            totalFine = totalFine.add(fine);

            if (borrow.getFinePaid()) {
                paidFine = paidFine.add(fine);
                paidCount++;
            } else {
                unpaidFine = unpaidFine.add(fine);
                unpaidCount++;
            }
        }

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalCount", totalCount);
        statistics.put("paidCount", paidCount);
        statistics.put("unpaidCount", unpaidCount);
        statistics.put("totalFine", totalFine);
        statistics.put("paidFine", paidFine);
        statistics.put("unpaidFine", unpaidFine);

        return statistics;
    }

    // ==================== 私有辅助方法 ====================

    private FineVO convertToFineVO(Borrow borrow) {
        // 计算逾期天数
        LocalDateTime dueDate = borrow.getDueDate();
        LocalDateTime compareDate = borrow.getReturnDate() != null 
                ? borrow.getReturnDate() 
                : LocalDateTime.now();
        
        long overdueDays = ChronoUnit.DAYS.between(dueDate, compareDate);
        int actualOverdueDays = overdueDays > FREE_GRACE_DAYS 
                ? (int)(overdueDays - FREE_GRACE_DAYS) 
                : 0;

        return FineVO.builder()
                .borrowId(borrow.getId())
                .borrowIdStr(borrow.getBorrowId())
                .readerId(borrow.getReaderId())
                .bookId(borrow.getBookId())
                .dueDate(borrow.getDueDate())
                .returnDate(borrow.getReturnDate())
                .overdueDays(actualOverdueDays)
                .fineAmount(borrow.getFineAmount())
                .finePaid(borrow.getFinePaid())
                .finePaidDate(borrow.getFinePaidDate())
                .status(borrow.getStatus())
                .createdAt(borrow.getCreatedAt())
                .build();
    }

    private Map<String, Object> createResult(Long borrowId, boolean success, String message, BigDecimal fine) {
        Map<String, Object> result = new HashMap<>();
        result.put("borrowId", borrowId);
        result.put("success", success);
        result.put("message", message);
        if (fine != null) {
            result.put("fineAmount", fine);
        }
        return result;
    }

    // ==================== 事件发布方法 ====================

    /**
     * 发布罚金支付事件
     */
    private void publishFinePaidEvent(Borrow borrow, String paymentMethod, String remarks) {
        try {
            FinePaidEvent event = FinePaidEvent.builder()
                    .borrowId(borrow.getId())
                    .borrowIdStr(borrow.getBorrowId())
                    .readerId(borrow.getReaderId())
                    .bookId(borrow.getBookId())
                    .fineAmount(borrow.getFineAmount())
                    .paymentMethod(paymentMethod)
                    .paidTime(borrow.getFinePaidDate())
                    .remarks(remarks)
                    .build();

            eventPublisher.publishFinePaidEvent(event);
        } catch (Exception e) {
            log.warn("发布罚金支付事件失败: borrowId={}, error={}", borrow.getId(), e.getMessage());
        }
    }

    /**
     * 发布归还完成事件
     */
    private void publishReturnCompletedEvent(Borrow borrow, BigDecimal fineAmount) {
        try {
            // 计算逾期天数
            long overdueDays = 0;
            boolean isOverdue = false;
            if (borrow.getReturnDate() != null && borrow.getReturnDate().isAfter(borrow.getDueDate())) {
                overdueDays = ChronoUnit.DAYS.between(borrow.getDueDate(), borrow.getReturnDate());
                if (overdueDays > FREE_GRACE_DAYS) {
                    isOverdue = true;
                    overdueDays = overdueDays - FREE_GRACE_DAYS;
                } else {
                    overdueDays = 0;
                }
            }

            ReturnCompletedEvent event = ReturnCompletedEvent.builder()
                    .borrowId(borrow.getId())
                    .borrowIdStr(borrow.getBorrowId())
                    .readerId(borrow.getReaderId())
                    .bookId(borrow.getBookId())
                    .bookBarcode(borrow.getBookBarcode())
                    .borrowDate(borrow.getBorrowDate())
                    .dueDate(borrow.getDueDate())
                    .returnDate(borrow.getReturnDate())
                    .isOverdue(isOverdue)
                    .overdueDays((int) overdueDays)
                    .fineAmount(fineAmount)
                    .finePaid(borrow.getFinePaid())
                    .build();

            eventPublisher.publishReturnCompletedEvent(event);
        } catch (Exception e) {
            log.warn("发布归还完成事件失败: borrowId={}, error={}", borrow.getId(), e.getMessage());
        }
    }
}
