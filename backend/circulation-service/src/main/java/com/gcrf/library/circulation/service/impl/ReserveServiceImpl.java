package com.gcrf.library.circulation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.circulation.client.BookServiceClient;
import com.gcrf.library.circulation.client.ReaderServiceClient;
import com.gcrf.library.circulation.client.dto.BookDTO;
import com.gcrf.library.circulation.client.dto.ReaderDTO;
import com.gcrf.library.circulation.dto.ReserveRequest;
import com.gcrf.library.circulation.dto.response.ReserveDetailVO;
import com.gcrf.library.circulation.dto.response.ReserveVO;
import com.gcrf.library.circulation.entity.Reserve;
import com.gcrf.library.circulation.mapper.ReserveMapper;
import com.gcrf.library.circulation.service.ReserveService;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.exception.SystemException;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 预约服务实现类
 *
 * @author GCRF Team
 * @since 2025-10-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReserveServiceImpl implements ReserveService {

    private final ReserveMapper reserveMapper;
    private final BookServiceClient bookServiceClient;
    private final ReaderServiceClient readerServiceClient;

    /**
     * 默认预约有效期（天）
     */
    private static final int DEFAULT_RESERVE_DAYS = 7;

    /**
     * 提醒阈值（天）
     */
    private static final int EXPIRING_SOON_THRESHOLD = 1;

    @Override
    public PageResult<ReserveVO> queryReserves(Long readerId, String status, Integer pageNum, Integer pageSize) {
        Page<Reserve> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 20);

        LambdaQueryWrapper<Reserve> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(readerId != null, Reserve::getReaderId, readerId)
                .eq(StringUtils.hasText(status), Reserve::getStatus, status)
                .isNull(Reserve::getDeletedAt)
                .orderByDesc(Reserve::getCreatedAt);

        Page<Reserve> reservePage = reserveMapper.selectPage(page, wrapper);

        // 转换为VO列表
        List<ReserveVO> reserveVOList = reservePage.getRecords().stream()
                .map(this::convertToReserveVO)
                .collect(Collectors.toList());

        return PageResult.ofRecords(
                reservePage.getTotal(),
                (int) reservePage.getCurrent(),
                (int) reservePage.getSize(),
                reserveVOList
        );
    }

    @Override
    public ReserveDetailVO getReserveById(Long id) {
        Reserve reserve = reserveMapper.selectById(id);
        if (reserve == null || reserve.getDeletedAt() != null) {
            throw new BusinessException("预约记录不存在, id: " + id);
        }
        return convertToReserveDetailVO(reserve);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReserveDetailVO reserveBook(ReserveRequest request) {
        log.info("开始处理预约请求, readerId: {}, bookId: {}", request.getReaderId(), request.getBookId());

        // 1. 验证读者状态
        Result<Boolean> readerStatusResult = readerServiceClient.validateReaderStatus(request.getReaderId());
        if (!readerStatusResult.isSuccess() || !Boolean.TRUE.equals(readerStatusResult.getData())) {
            throw new BusinessException("读者状态不允许预约图书，请检查读者证是否激活或有效期是否过期");
        }

        // 2. 获取读者信息
        Result<ReaderDTO> readerResult = readerServiceClient.getReaderById(request.getReaderId());
        if (!readerResult.isSuccess() || readerResult.getData() == null) {
            throw new SystemException("无法获取读者信息, readerId: " + request.getReaderId());
        }

        // 3. 验证图书存在
        Result<BookDTO> bookResult = bookServiceClient.getBookById(request.getBookId());
        if (!bookResult.isSuccess() || bookResult.getData() == null) {
            throw new SystemException("无法获取图书信息, bookId: " + request.getBookId());
        }

        // 4. 检查是否已有该书的活动预约
        LambdaQueryWrapper<Reserve> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(Reserve::getReaderId, request.getReaderId())
                .eq(Reserve::getBookId, request.getBookId())
                .in(Reserve::getStatus, "RESERVED")
                .isNull(Reserve::getDeletedAt);

        Long activeReserveCount = reserveMapper.selectCount(checkWrapper);
        if (activeReserveCount > 0) {
            throw new BusinessException("您已预约过此图书，无需重复预约");
        }

        // 5. 生成预约编号（格式: RV-YYYYMMDD-0001）
        String reserveId = generateReserveId();

        // 6. 计算过期日期
        Integer reserveDays = request.getReserveDays() != null ? request.getReserveDays() : DEFAULT_RESERVE_DAYS;
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(reserveDays);

        // 7. 创建预约记录
        Reserve reserve = new Reserve();
        reserve.setReserveId(reserveId);
        reserve.setReaderId(request.getReaderId());
        reserve.setBookId(request.getBookId());
        reserve.setReserveDate(LocalDateTime.now());
        reserve.setExpiryDate(expiryDate);
        reserve.setStatus("RESERVED");
        reserve.setNotifySent(false);
        reserve.setNotifyCount(0);
        reserve.setRemarks(request.getRemarks());

        // 8. 保存预约记录
        reserveMapper.insert(reserve);
        log.info("预约成功, reserveId: {}, readerId: {}, bookId: {}", reserveId, request.getReaderId(), request.getBookId());

        return convertToReserveDetailVO(reserve);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReserveDetailVO pickupReserve(Long id) {
        log.info("开始处理取书请求, reserveId: {}", id);

        // 1. 查询预约记录
        Reserve reserve = reserveMapper.selectById(id);
        if (reserve == null || reserve.getDeletedAt() != null) {
            throw new BusinessException("预约记录不存在, id: " + id);
        }

        // 2. 检查状态是否为RESERVED
        if (!"RESERVED".equals(reserve.getStatus())) {
            throw new BusinessException("该预约记录状态不允许取书，当前状态: " + reserve.getStatus());
        }

        // 3. 检查是否已过期
        if (LocalDateTime.now().isAfter(reserve.getExpiryDate())) {
            // 自动更新为过期状态
            reserve.setStatus("EXPIRED");
            reserveMapper.updateById(reserve);
            throw new BusinessException("预约已过期，无法取书");
        }

        // 4. 更新预约记录
        reserve.setStatus("PICKED_UP");
        reserve.setPickupDate(LocalDateTime.now());

        // 5. 保存更新
        reserveMapper.updateById(reserve);
        log.info("取书成功, reserveId: {}, bookId: {}", reserve.getReserveId(), reserve.getBookId());

        return convertToReserveDetailVO(reserve);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReserveDetailVO cancelReserve(Long id) {
        log.info("开始处理取消预约请求, reserveId: {}", id);

        // 1. 查询预约记录
        Reserve reserve = reserveMapper.selectById(id);
        if (reserve == null || reserve.getDeletedAt() != null) {
            throw new BusinessException("预约记录不存在, id: " + id);
        }

        // 2. 检查状态是否为RESERVED
        if (!"RESERVED".equals(reserve.getStatus())) {
            throw new BusinessException("该预约记录状态不允许取消，当前状态: " + reserve.getStatus());
        }

        // 3. 更新预约记录
        reserve.setStatus("CANCELLED");
        reserve.setCancelDate(LocalDateTime.now());

        // 4. 保存更新
        reserveMapper.updateById(reserve);
        log.info("取消预约成功, reserveId: {}", reserve.getReserveId());

        return convertToReserveDetailVO(reserve);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void expireReserves() {
        log.info("开始批量过期预约记录");

        LambdaQueryWrapper<Reserve> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reserve::getStatus, "RESERVED")
                .lt(Reserve::getExpiryDate, LocalDateTime.now())
                .isNull(Reserve::getDeletedAt);

        List<Reserve> reserves = reserveMapper.selectList(wrapper);

        int expiredCount = 0;
        for (Reserve reserve : reserves) {
            reserve.setStatus("EXPIRED");
            reserveMapper.updateById(reserve);
            expiredCount++;
        }

        log.info("批量过期预约记录完成, 过期记录数: {}", expiredCount);
    }

    @Override
    public List<ReserveVO> getPendingNotifications() {
        LambdaQueryWrapper<Reserve> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reserve::getStatus, "RESERVED")
                .and(w -> w.eq(Reserve::getNotifySent, false)
                        .or()
                        .lt(Reserve::getNotifyCount, 3)) // 最多提醒3次
                .isNull(Reserve::getDeletedAt)
                .orderByAsc(Reserve::getReserveDate);

        List<Reserve> reserves = reserveMapper.selectList(wrapper);
        return reserves.stream()
                .map(this::convertToReserveVO)
                .collect(Collectors.toList());
    }

    /**
     * 生成预约编号（格式: RV-YYYYMMDD-0001）
     */
    private String generateReserveId() {
        String datePrefix = "RV-" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 查询当天最大序号
        LambdaQueryWrapper<Reserve> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Reserve::getReserveId, datePrefix)
                .orderByDesc(Reserve::getReserveId)
                .last("LIMIT 1");

        Reserve lastReserve = reserveMapper.selectOne(wrapper);

        int sequence = 1;
        if (lastReserve != null && lastReserve.getReserveId() != null) {
            String lastReserveId = lastReserve.getReserveId();
            String lastSequence = lastReserveId.substring(lastReserveId.lastIndexOf("-") + 1);
            sequence = Integer.parseInt(lastSequence) + 1;
        }

        return String.format("%s-%04d", datePrefix, sequence);
    }

    /**
     * 转换为ReserveVO
     */
    private ReserveVO convertToReserveVO(Reserve reserve) {
        ReserveVO vo = ReserveVO.builder()
                .id(reserve.getId())
                .reserveId(reserve.getReserveId())
                .readerId(reserve.getReaderId())
                .bookId(reserve.getBookId())
                .reserveDate(reserve.getReserveDate())
                .expiryDate(reserve.getExpiryDate())
                .pickupDate(reserve.getPickupDate())
                .cancelDate(reserve.getCancelDate())
                .status(reserve.getStatus())
                .notifySent(reserve.getNotifySent())
                .notifySentDate(reserve.getNotifySentDate())
                .createdAt(reserve.getCreatedAt())
                .build();

        // 计算是否即将过期
        if ("RESERVED".equals(reserve.getStatus())) {
            long remainingDays = ChronoUnit.DAYS.between(LocalDateTime.now(), reserve.getExpiryDate());
            vo.setRemainingDays(remainingDays);
            vo.setExpiringSoon(remainingDays <= EXPIRING_SOON_THRESHOLD);
        } else {
            vo.setRemainingDays(0L);
            vo.setExpiringSoon(false);
        }

        // 尝试获取读者和图书信息（失败不影响核心数据）
        try {
            Result<ReaderDTO> readerResult = readerServiceClient.getReaderById(reserve.getReaderId());
            if (readerResult.isSuccess() && readerResult.getData() != null) {
                vo.setReaderName(readerResult.getData().getName());
            }
        } catch (Exception e) {
            log.warn("获取读者信息失败, readerId: {}", reserve.getReaderId(), e);
        }

        try {
            Result<BookDTO> bookResult = bookServiceClient.getBookById(reserve.getBookId());
            if (bookResult.isSuccess() && bookResult.getData() != null) {
                vo.setBookTitle(bookResult.getData().getTitle());
            }
        } catch (Exception e) {
            log.warn("获取图书信息失败, bookId: {}", reserve.getBookId(), e);
        }

        return vo;
    }

    /**
     * 转换为ReserveDetailVO
     */
    private ReserveDetailVO convertToReserveDetailVO(Reserve reserve) {
        ReserveDetailVO vo = ReserveDetailVO.builder()
                .id(reserve.getId())
                .reserveId(reserve.getReserveId())
                .readerId(reserve.getReaderId())
                .bookId(reserve.getBookId())
                .reserveDate(reserve.getReserveDate())
                .expiryDate(reserve.getExpiryDate())
                .pickupDate(reserve.getPickupDate())
                .cancelDate(reserve.getCancelDate())
                .status(reserve.getStatus())
                .notifySent(reserve.getNotifySent())
                .notifySentDate(reserve.getNotifySentDate())
                .notifyCount(reserve.getNotifyCount())
                .remarks(reserve.getRemarks())
                .createdAt(reserve.getCreatedAt())
                .updatedAt(reserve.getUpdatedAt())
                .build();

        // 计算是否即将过期
        if ("RESERVED".equals(reserve.getStatus())) {
            long remainingDays = ChronoUnit.DAYS.between(LocalDateTime.now(), reserve.getExpiryDate());
            vo.setRemainingDays(remainingDays);
            vo.setExpiringSoon(remainingDays <= EXPIRING_SOON_THRESHOLD);
        } else {
            vo.setRemainingDays(0L);
            vo.setExpiringSoon(false);
        }

        // 获取读者详细信息
        try {
            Result<ReaderDTO> readerResult = readerServiceClient.getReaderById(reserve.getReaderId());
            if (readerResult.isSuccess() && readerResult.getData() != null) {
                ReaderDTO reader = readerResult.getData();
                vo.setReaderName(reader.getName());
                vo.setReaderCardId(reader.getReaderId());
                vo.setReaderType(reader.getReaderType());
                vo.setReaderPhone(maskPhone(reader.getPhone()));
            }
        } catch (Exception e) {
            log.warn("获取读者详细信息失败, readerId: {}", reserve.getReaderId(), e);
        }

        // 获取图书详细信息
        try {
            Result<BookDTO> bookResult = bookServiceClient.getBookById(reserve.getBookId());
            if (bookResult.isSuccess() && bookResult.getData() != null) {
                BookDTO book = bookResult.getData();
                vo.setBookTitle(book.getTitle());
                vo.setBookIsbn(book.getIsbn());
                vo.setBookAuthor(book.getAuthor());
            }
        } catch (Exception e) {
            log.warn("获取图书详细信息失败, bookId: {}", reserve.getBookId(), e);
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
