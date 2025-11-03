package com.gcrf.library.circulation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.circulation.client.BookServiceClient;
import com.gcrf.library.circulation.client.ReaderServiceClient;
import com.gcrf.library.circulation.client.dto.BookDTO;
import com.gcrf.library.circulation.client.dto.ReaderDTO;
import com.gcrf.library.circulation.dto.BorrowQueryRequest;
import com.gcrf.library.circulation.dto.BorrowRequest;
import com.gcrf.library.circulation.dto.RenewRequest;
import com.gcrf.library.circulation.dto.ReturnRequest;
import com.gcrf.library.circulation.dto.response.BorrowDetailVO;
import com.gcrf.library.circulation.dto.response.BorrowVO;
import com.gcrf.library.circulation.entity.Borrow;
import com.gcrf.library.circulation.mapper.BorrowMapper;
import com.gcrf.library.circulation.service.BorrowService;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.exception.SystemException;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 借阅服务实现类
 *
 * @author GCRF Team
 * @since 2025-10-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowServiceImpl implements BorrowService {

    private final BorrowMapper borrowMapper;
    private final BookServiceClient bookServiceClient;
    private final ReaderServiceClient readerServiceClient;

    /**
     * 每日逾期罚金(元)
     */
    private static final BigDecimal DAILY_FINE = new BigDecimal("1.00");

    /**
     * 默认最大续借次数
     */
    private static final int DEFAULT_MAX_RENEW_COUNT = 2;

    @Override
    public PageResult<BorrowVO> queryBorrows(BorrowQueryRequest request) {
        Page<Borrow> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<Borrow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(request.getReaderId() != null, Borrow::getReaderId, request.getReaderId())
                .eq(request.getBookId() != null, Borrow::getBookId, request.getBookId())
                .like(StringUtils.hasText(request.getBorrowId()), Borrow::getBorrowId, request.getBorrowId())
                .like(StringUtils.hasText(request.getBookBarcode()), Borrow::getBookBarcode, request.getBookBarcode())
                .eq(StringUtils.hasText(request.getStatus()), Borrow::getStatus, request.getStatus())
                .ge(request.getBorrowDateStart() != null, Borrow::getBorrowDate, request.getBorrowDateStart())
                .le(request.getBorrowDateEnd() != null, Borrow::getBorrowDate, request.getBorrowDateEnd())
                .ge(request.getDueDateStart() != null, Borrow::getDueDate, request.getDueDateStart())
                .le(request.getDueDateEnd() != null, Borrow::getDueDate, request.getDueDateEnd())
                .isNull(Borrow::getDeletedAt)
                .orderByDesc(Borrow::getCreatedAt);

        // 仅查询逾期未归还
        if (Boolean.TRUE.equals(request.getOverdueOnly())) {
            wrapper.isNull(Borrow::getReturnDate)
                    .lt(Borrow::getDueDate, LocalDateTime.now());
        }

        // 仅查询未支付罚金
        if (Boolean.TRUE.equals(request.getUnpaidFineOnly())) {
            wrapper.eq(Borrow::getFinePaid, false)
                    .gt(Borrow::getFineAmount, BigDecimal.ZERO);
        }

        Page<Borrow> borrowPage = borrowMapper.selectPage(page, wrapper);

        // 转换为VO列表
        List<BorrowVO> borrowVOList = borrowPage.getRecords().stream()
                .map(this::convertToBorrowVO)
                .collect(Collectors.toList());

        return PageResult.ofRecords(
                borrowPage.getTotal(),
                (int) borrowPage.getCurrent(),
                (int) borrowPage.getSize(),
                borrowVOList
        );
    }

    @Override
    public BorrowDetailVO getBorrowById(Long id) {
        Borrow borrow = borrowMapper.selectById(id);
        if (borrow == null || borrow.getDeletedAt() != null) {
            throw new BusinessException("借阅记录不存在, id: " + id);
        }
        return convertToBorrowDetailVO(borrow);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BorrowDetailVO borrowBook(BorrowRequest request) {
        log.info("开始处理借书请求, readerId: {}, bookId: {}", request.getReaderId(), request.getBookId());

        // 1. 验证读者状态
        Result<Boolean> readerStatusResult = readerServiceClient.validateReaderStatus(request.getReaderId());
        if (!readerStatusResult.isSuccess() || !Boolean.TRUE.equals(readerStatusResult.getData())) {
            throw new BusinessException("读者状态不允许借书，请检查读者证是否激活或有效期是否过期");
        }

        // 2. 获取读者信息
        Result<ReaderDTO> readerResult = readerServiceClient.getReaderById(request.getReaderId());
        if (!readerResult.isSuccess() || readerResult.getData() == null) {
            throw new SystemException("无法获取读者信息, readerId: " + request.getReaderId());
        }
        ReaderDTO reader = readerResult.getData();

        // 3. 检查图书可借状态
        Result<Boolean> availabilityResult = bookServiceClient.checkAvailability(request.getBookId());
        if (!availabilityResult.isSuccess() || !Boolean.TRUE.equals(availabilityResult.getData())) {
            throw new BusinessException("图书暂无可借副本");
        }

        // 4. 获取图书信息
        Result<BookDTO> bookResult = bookServiceClient.getBookById(request.getBookId());
        if (!bookResult.isSuccess() || bookResult.getData() == null) {
            throw new SystemException("无法获取图书信息, bookId: " + request.getBookId());
        }
        BookDTO book = bookResult.getData();

        // 5. 生成借阅编号（格式: BW-YYYYMMDD-0001）
        String borrowId = generateBorrowId();

        // 6. 计算应还日期（根据读者的maxBorrowDays或请求的borrowDays）
        Integer borrowDays = request.getBorrowDays() != null ? request.getBorrowDays() : reader.getMaxBorrowDays();
        LocalDateTime dueDate = LocalDateTime.now().plusDays(borrowDays);

        // 7. 创建借阅记录
        Borrow borrow = new Borrow();
        borrow.setBorrowId(borrowId);
        borrow.setReaderId(request.getReaderId());
        borrow.setBookId(request.getBookId());
        borrow.setBookBarcode(book.getBarcode());
        borrow.setBorrowDate(LocalDateTime.now());
        borrow.setDueDate(dueDate);
        borrow.setRenewCount(0);
        borrow.setMaxRenewCount(DEFAULT_MAX_RENEW_COUNT);
        borrow.setStatus("BORROWED");
        borrow.setFineAmount(BigDecimal.ZERO);
        borrow.setFinePaid(false);
        borrow.setRemarks(request.getRemark());

        // 8. 减少图书可借数量
        Result<Void> decreaseResult = bookServiceClient.decreaseAvailableCopies(request.getBookId());
        if (!decreaseResult.isSuccess()) {
            throw new SystemException("减少图书可借数量失败");
        }

        // 9. 保存借阅记录
        borrowMapper.insert(borrow);
        log.info("借书成功, borrowId: {}, readerId: {}, bookId: {}", borrowId, request.getReaderId(), request.getBookId());

        return convertToBorrowDetailVO(borrow);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BorrowDetailVO returnBook(ReturnRequest request) {
        log.info("开始处理还书请求, borrowId: {}", request.getBorrowId());

        // 1. 查询借阅记录
        Borrow borrow = borrowMapper.selectById(request.getBorrowId());
        if (borrow == null || borrow.getDeletedAt() != null) {
            throw new BusinessException("借阅记录不存在, id: " + request.getBorrowId());
        }

        // 2. 检查是否已归还
        if (borrow.getReturnDate() != null) {
            throw new BusinessException("该图书已归还，无需重复操作");
        }

        // 3. 计算逾期罚金
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(borrow.getDueDate())) {
            long overdueDays = ChronoUnit.DAYS.between(borrow.getDueDate(), now);
            BigDecimal fineAmount = DAILY_FINE.multiply(new BigDecimal(overdueDays));
            borrow.setFineAmount(fineAmount);
            log.info("图书逾期 {} 天，罚金: {} 元", overdueDays, fineAmount);
        }

        // 4. 更新借阅记录
        borrow.setReturnDate(now);
        borrow.setStatus("RETURNED");
        if (request.getRemarks() != null) {
            borrow.setRemarks(borrow.getRemarks() != null
                    ? borrow.getRemarks() + "; " + request.getRemarks()
                    : request.getRemarks());
        }

        // 5. 处理罚金支付
        if (Boolean.TRUE.equals(request.getPayFine()) && borrow.getFineAmount().compareTo(BigDecimal.ZERO) > 0) {
            borrow.setFinePaid(true);
            borrow.setFinePaidDate(now);
            log.info("罚金已支付, 金额: {} 元, 支付方式: {}",
                    borrow.getFineAmount(), request.getPaymentMethod());
        }

        // 6. 增加图书可借数量
        Result<Void> increaseResult = bookServiceClient.increaseAvailableCopies(borrow.getBookId());
        if (!increaseResult.isSuccess()) {
            throw new SystemException("增加图书可借数量失败");
        }

        // 7. 保存更新
        borrowMapper.updateById(borrow);
        log.info("还书成功, borrowId: {}, bookId: {}", borrow.getBorrowId(), borrow.getBookId());

        return convertToBorrowDetailVO(borrow);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BorrowDetailVO renewBook(RenewRequest request) {
        log.info("开始处理续借请求, borrowId: {}", request.getBorrowId());

        // 1. 查询借阅记录
        Borrow borrow = borrowMapper.selectById(request.getBorrowId());
        if (borrow == null || borrow.getDeletedAt() != null) {
            throw new BusinessException("借阅记录不存在, id: " + request.getBorrowId());
        }

        // 2. 检查状态是否为BORROWED
        if (!"BORROWED".equals(borrow.getStatus())) {
            throw new BusinessException("只有借阅中的图书才能续借");
        }

        // 3. 检查续借次数
        if (borrow.getRenewCount() >= borrow.getMaxRenewCount()) {
            throw new BusinessException("已达到最大续借次数限制 (" + borrow.getMaxRenewCount() + " 次)");
        }

        // 4. 延长应还日期
        Integer renewDays = request.getRenewDays() != null ? request.getRenewDays() : 30;
        LocalDateTime newDueDate = borrow.getDueDate().plusDays(renewDays);
        borrow.setDueDate(newDueDate);
        borrow.setRenewCount(borrow.getRenewCount() + 1);

        // 5. 更新备注
        String renewRemark = String.format("续借 %d 天 (第 %d 次续借)", renewDays, borrow.getRenewCount());
        if (request.getReason() != null) {
            renewRemark += ", 理由: " + request.getReason();
        }
        borrow.setRemarks(borrow.getRemarks() != null
                ? borrow.getRemarks() + "; " + renewRemark
                : renewRemark);

        // 6. 保存更新
        borrowMapper.updateById(borrow);
        log.info("续借成功, borrowId: {}, 新应还日期: {}, 续借次数: {}/{}",
                borrow.getBorrowId(), newDueDate, borrow.getRenewCount(), borrow.getMaxRenewCount());

        return convertToBorrowDetailVO(borrow);
    }

    @Override
    public List<BorrowVO> getOverdueBorrows() {
        LambdaQueryWrapper<Borrow> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(Borrow::getReturnDate)
                .lt(Borrow::getDueDate, LocalDateTime.now())
                .isNull(Borrow::getDeletedAt)
                .orderByAsc(Borrow::getDueDate);

        List<Borrow> borrows = borrowMapper.selectList(wrapper);
        return borrows.stream()
                .map(this::convertToBorrowVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOverdueStatus() {
        log.info("开始批量更新逾期状态");

        LambdaQueryWrapper<Borrow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Borrow::getStatus, "BORROWED")
                .lt(Borrow::getDueDate, LocalDateTime.now())
                .isNull(Borrow::getDeletedAt);

        List<Borrow> borrows = borrowMapper.selectList(wrapper);

        int updatedCount = 0;
        for (Borrow borrow : borrows) {
            borrow.setStatus("OVERDUE");
            borrowMapper.updateById(borrow);
            updatedCount++;
        }

        log.info("批量更新逾期状态完成, 更新记录数: {}", updatedCount);
    }

    /**
     * 生成借阅编号（格式: BW-YYYYMMDD-0001）
     */
    private String generateBorrowId() {
        String datePrefix = "BW-" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 查询当天最大序号
        LambdaQueryWrapper<Borrow> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Borrow::getBorrowId, datePrefix)
                .orderByDesc(Borrow::getBorrowId)
                .last("LIMIT 1");

        Borrow lastBorrow = borrowMapper.selectOne(wrapper);

        int sequence = 1;
        if (lastBorrow != null && lastBorrow.getBorrowId() != null) {
            String lastBorrowId = lastBorrow.getBorrowId();
            String lastSequence = lastBorrowId.substring(lastBorrowId.lastIndexOf("-") + 1);
            sequence = Integer.parseInt(lastSequence) + 1;
        }

        return String.format("%s-%04d", datePrefix, sequence);
    }

    /**
     * 转换为BorrowVO
     */
    private BorrowVO convertToBorrowVO(Borrow borrow) {
        BorrowVO vo = BorrowVO.builder()
                .id(borrow.getId())
                .borrowId(borrow.getBorrowId())
                .readerId(borrow.getReaderId())
                .bookId(borrow.getBookId())
                .bookBarcode(borrow.getBookBarcode())
                .borrowDate(borrow.getBorrowDate())
                .dueDate(borrow.getDueDate())
                .returnDate(borrow.getReturnDate())
                .renewCount(borrow.getRenewCount())
                .maxRenewCount(borrow.getMaxRenewCount())
                .status(borrow.getStatus())
                .fineAmount(borrow.getFineAmount())
                .finePaid(borrow.getFinePaid())
                .finePaidDate(borrow.getFinePaidDate())
                .createdAt(borrow.getCreatedAt())
                .build();

        // 计算是否逾期
        boolean overdue = borrow.getReturnDate() == null && LocalDateTime.now().isAfter(borrow.getDueDate());
        vo.setOverdue(overdue);

        // 计算逾期天数
        if (overdue) {
            long overdueDays = ChronoUnit.DAYS.between(borrow.getDueDate(), LocalDateTime.now());
            vo.setOverdueDays(overdueDays);
        } else {
            vo.setOverdueDays(0L);
        }

        // 判断是否可以续借
        boolean canRenew = "BORROWED".equals(borrow.getStatus())
                && borrow.getRenewCount() < borrow.getMaxRenewCount();
        vo.setCanRenew(canRenew);

        // 尝试获取读者和图书信息（失败不影响核心数据）
        try {
            Result<ReaderDTO> readerResult = readerServiceClient.getReaderById(borrow.getReaderId());
            if (readerResult.isSuccess() && readerResult.getData() != null) {
                vo.setReaderName(readerResult.getData().getName());
            }
        } catch (Exception e) {
            log.warn("获取读者信息失败, readerId: {}", borrow.getReaderId(), e);
        }

        try {
            Result<BookDTO> bookResult = bookServiceClient.getBookById(borrow.getBookId());
            if (bookResult.isSuccess() && bookResult.getData() != null) {
                vo.setBookTitle(bookResult.getData().getTitle());
            }
        } catch (Exception e) {
            log.warn("获取图书信息失败, bookId: {}", borrow.getBookId(), e);
        }

        return vo;
    }

    /**
     * 转换为BorrowDetailVO
     */
    private BorrowDetailVO convertToBorrowDetailVO(Borrow borrow) {
        BorrowDetailVO vo = BorrowDetailVO.builder()
                .id(borrow.getId())
                .borrowId(borrow.getBorrowId())
                .readerId(borrow.getReaderId())
                .bookId(borrow.getBookId())
                .bookBarcode(borrow.getBookBarcode())
                .borrowDate(borrow.getBorrowDate())
                .dueDate(borrow.getDueDate())
                .returnDate(borrow.getReturnDate())
                .renewCount(borrow.getRenewCount())
                .maxRenewCount(borrow.getMaxRenewCount())
                .status(borrow.getStatus())
                .fineAmount(borrow.getFineAmount())
                .finePaid(borrow.getFinePaid())
                .finePaidDate(borrow.getFinePaidDate())
                .remarks(borrow.getRemarks())
                .createdAt(borrow.getCreatedAt())
                .updatedAt(borrow.getUpdatedAt())
                .build();

        // 计算是否逾期
        boolean overdue = borrow.getReturnDate() == null && LocalDateTime.now().isAfter(borrow.getDueDate());
        vo.setOverdue(overdue);

        // 计算逾期天数
        if (overdue) {
            long overdueDays = ChronoUnit.DAYS.between(borrow.getDueDate(), LocalDateTime.now());
            vo.setOverdueDays(overdueDays);
        } else {
            vo.setOverdueDays(0L);
        }

        // 判断是否可以续借
        boolean canRenew = "BORROWED".equals(borrow.getStatus())
                && borrow.getRenewCount() < borrow.getMaxRenewCount();
        vo.setCanRenew(canRenew);

        // 获取读者详细信息
        try {
            Result<ReaderDTO> readerResult = readerServiceClient.getReaderById(borrow.getReaderId());
            if (readerResult.isSuccess() && readerResult.getData() != null) {
                ReaderDTO reader = readerResult.getData();
                vo.setReaderName(reader.getName());
                vo.setReaderCardId(reader.getReaderId());
                vo.setReaderType(reader.getReaderType());
                vo.setReaderPhone(maskPhone(reader.getPhone()));
            }
        } catch (Exception e) {
            log.warn("获取读者详细信息失败, readerId: {}", borrow.getReaderId(), e);
        }

        // 获取图书详细信息
        try {
            Result<BookDTO> bookResult = bookServiceClient.getBookById(borrow.getBookId());
            if (bookResult.isSuccess() && bookResult.getData() != null) {
                BookDTO book = bookResult.getData();
                vo.setBookTitle(book.getTitle());
                vo.setBookIsbn(book.getIsbn());
                vo.setBookAuthor(book.getAuthor());
            }
        } catch (Exception e) {
            log.warn("获取图书详细信息失败, bookId: {}", borrow.getBookId(), e);
        }

        return vo;
    }

    /**
     * 手机号脱敏
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
