package com.gcrf.library.circulation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gcrf.library.circulation.dto.BorrowRequest;
import com.gcrf.library.circulation.entity.CirculationRecord;
import com.gcrf.library.circulation.mapper.CirculationRecordMapper;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流通服务
 *
 * @author GCRF Team
 * @date 2025-10-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CirculationService {

    private final CirculationRecordMapper circulationRecordMapper;
    // TODO: 注入BookService Feign客户端

    /**
     * 借阅图书
     */
    @Transactional(rollbackFor = Exception.class)
    public CirculationRecord borrowBook(BorrowRequest request) {
        log.info("借阅图书: bookId={}, readerId={}", request.getBookId(), request.getReaderId());

        // 检查读者是否有逾期未还的图书
        LambdaQueryWrapper<CirculationRecord> overdueQuery = new LambdaQueryWrapper<>();
        overdueQuery.eq(CirculationRecord::getReaderId, request.getReaderId());
        overdueQuery.eq(CirculationRecord::getStatus, 3); // 逾期状态
        if (circulationRecordMapper.selectCount(overdueQuery) > 0) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "存在逾期未还图书，无法借阅");
        }

        // 检查是否超过借阅上限（假设最多5本）
        LambdaQueryWrapper<CirculationRecord> borrowedQuery = new LambdaQueryWrapper<>();
        borrowedQuery.eq(CirculationRecord::getReaderId, request.getReaderId());
        borrowedQuery.eq(CirculationRecord::getStatus, 1); // 借阅中
        long borrowedCount = circulationRecordMapper.selectCount(borrowedQuery);
        if (borrowedCount >= 5) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "已达借阅上限（5本）");
        }

        // TODO: 调用book-service减少可借数量
        // bookServiceFeign.decreaseAvailableCopies(request.getBookId());

        // 创建借阅记录
        CirculationRecord record = new CirculationRecord();
        record.setBookId(request.getBookId());
        record.setReaderId(request.getReaderId());
        record.setBorrowTime(LocalDateTime.now());
        record.setDueTime(LocalDateTime.now().plusDays(request.getBorrowDays()));
        record.setStatus(1); // 借阅中
        record.setRenewCount(0);
        record.setRemark(request.getRemark());

        circulationRecordMapper.insert(record);

        log.info("借阅成功: recordId={}", record.getId());
        return record;
    }

    /**
     * 归还图书
     */
    @Transactional(rollbackFor = Exception.class)
    public CirculationRecord returnBook(Long recordId) {
        log.info("归还图书: recordId={}", recordId);

        CirculationRecord record = circulationRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "借阅记录不存在");
        }

        if (record.getStatus() == 2) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "图书已归还");
        }

        // TODO: 调用book-service增加可借数量
        // bookServiceFeign.increaseAvailableCopies(record.getBookId());

        // 更新记录
        record.setReturnTime(LocalDateTime.now());
        record.setStatus(2); // 已归还

        // 计算罚款（逾期每天1元）
        if (LocalDateTime.now().isAfter(record.getDueTime())) {
            long overdueDays = java.time.Duration.between(record.getDueTime(), LocalDateTime.now()).toDays();
            record.setFineAmount(overdueDays * 100); // 单位：分
        }

        circulationRecordMapper.updateById(record);

        log.info("归还成功: recordId={}, fineAmount={}", recordId, record.getFineAmount());
        return record;
    }

    /**
     * 续借图书
     */
    @Transactional(rollbackFor = Exception.class)
    public CirculationRecord renewBook(Long recordId, Integer renewDays) {
        log.info("续借图书: recordId={}, renewDays={}", recordId, renewDays);

        CirculationRecord record = circulationRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "借阅记录不存在");
        }

        if (record.getStatus() != 1) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "只能续借借阅中的图书");
        }

        if (record.getRenewCount() >= 2) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "已达续借上限（2次）");
        }

        // 更新应还时间和续借次数
        record.setDueTime(record.getDueTime().plusDays(renewDays));
        record.setRenewCount(record.getRenewCount() + 1);

        circulationRecordMapper.updateById(record);

        log.info("续借成功: recordId={}, newDueTime={}", recordId, record.getDueTime());
        return record;
    }

    /**
     * 查询读者的借阅记录
     */
    public List<CirculationRecord> getReaderRecords(Long readerId, Integer status) {
        LambdaQueryWrapper<CirculationRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CirculationRecord::getReaderId, readerId);

        if (status != null) {
            queryWrapper.eq(CirculationRecord::getStatus, status);
        }

        queryWrapper.orderByDesc(CirculationRecord::getBorrowTime);

        return circulationRecordMapper.selectList(queryWrapper);
    }
}
