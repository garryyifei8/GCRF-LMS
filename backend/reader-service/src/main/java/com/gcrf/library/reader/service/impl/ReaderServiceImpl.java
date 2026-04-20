package com.gcrf.library.reader.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import com.gcrf.library.reader.client.CirculationServiceClient;
import com.gcrf.library.reader.dto.*;
import com.gcrf.library.reader.dto.response.ReaderDetailVO;
import com.gcrf.library.reader.dto.response.ReaderVO;
import com.gcrf.library.reader.entity.Reader;
import com.gcrf.library.reader.mapper.ReaderMapper;
import com.gcrf.library.reader.service.ReaderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

/**
 * 读者服务实现类
 *
 * @author GCRF Team
 * @since 2025-10-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReaderServiceImpl implements ReaderService {

    private final ReaderMapper readerMapper;
    private final CirculationServiceClient circulationServiceClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReaderDetailVO createReader(ReaderCreateRequest request) {
        // 检查读者证号是否已存在
        Long count = readerMapper.selectCount(
            new LambdaQueryWrapper<Reader>()
                .eq(Reader::getReaderId, request.getReaderId())
        );
        if (count > 0) {
            throw new BusinessException("读者证号已存在: " + request.getReaderId());
        }

        // 检查身份证号是否已存在
        if (StringUtils.hasText(request.getIdCard())) {
            count = readerMapper.selectCount(
                new LambdaQueryWrapper<Reader>()
                    .eq(Reader::getIdCard, request.getIdCard())
            );
            if (count > 0) {
                throw new BusinessException("身份证号已被注册: " + request.getIdCard());
            }
        }

        // 创建读者实体
        Reader reader = new Reader();
        reader.setReaderId(request.getReaderId());
        reader.setName(request.getName());
        reader.setIdCard(request.getIdCard());
        reader.setPhone(request.getPhone());
        reader.setEmail(request.getEmail());
        reader.setReaderType(request.getReaderType());
        reader.setDepartment(request.getDepartment());

        // 根据读者类型设置学号或工号
        if ("STUDENT".equals(request.getReaderType())) {
            reader.setStudentNo(request.getReaderId()); // 使用读者证号作为学号
        } else if ("TEACHER".equals(request.getReaderType()) || "STAFF".equals(request.getReaderType())) {
            reader.setEmployeeNo(request.getReaderId()); // 使用读者证号作为工号
        }

        // 设置默认值
        reader.setStatus("ACTIVE"); // 默认激活状态
        reader.setMaxBorrowCount(getDefaultMaxBorrowCount(request.getReaderType()));
        reader.setMaxBorrowDays(getDefaultMaxBorrowDays(request.getReaderType()));
        reader.setExpiryDate(LocalDate.now().plusYears(1)); // 默认有效期1年

        readerMapper.insert(reader);
        log.info("创建读者成功, readerId: {}", reader.getReaderId());

        return ReaderDetailVO.from(reader);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReaderDetailVO updateReader(ReaderUpdateRequest request) {
        Reader reader = readerMapper.selectById(request.getId());
        if (reader == null) {
            throw new BusinessException("读者不存在, id: " + request.getId());
        }

        // 复制可更新的字段
        if (StringUtils.hasText(request.getName())) {
            reader.setName(request.getName());
        }
        if (StringUtils.hasText(request.getPhone())) {
            reader.setPhone(request.getPhone());
        }
        if (StringUtils.hasText(request.getEmail())) {
            reader.setEmail(request.getEmail());
        }
        if (StringUtils.hasText(request.getDepartment())) {
            reader.setDepartment(request.getDepartment());
        }

        int updated = readerMapper.updateById(reader);
        if (updated == 0) {
            throw new BusinessException("更新失败,数据可能已被修改,请刷新后重试");
        }

        log.info("更新读者成功, id: {}", reader.getId());
        return ReaderDetailVO.from(reader);
    }

    @Override
    public ReaderDetailVO getReaderById(Long id) {
        Reader reader = readerMapper.selectById(id);
        if (reader == null) {
            throw new BusinessException("读者不存在, id: " + id);
        }
        return ReaderDetailVO.from(reader);
    }

    @Override
    public ReaderDetailVO getReaderByReaderId(String readerId) {
        Reader reader = readerMapper.selectOne(
            new LambdaQueryWrapper<Reader>()
                .eq(Reader::getReaderId, readerId)
        );
        if (reader == null) {
            throw new BusinessException("读者不存在, readerId: " + readerId);
        }
        return ReaderDetailVO.from(reader);
    }

    @Override
    public PageResult<ReaderVO> queryReaders(ReaderQueryRequest request) {
        Page<Reader> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<Reader> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(request.getReaderId()), Reader::getReaderId, request.getReaderId())
               .like(StringUtils.hasText(request.getName()), Reader::getName, request.getName())
               .eq(StringUtils.hasText(request.getPhone()), Reader::getPhone, request.getPhone())
               .eq(StringUtils.hasText(request.getIdCard()), Reader::getIdCard, request.getIdCard())
               .eq(StringUtils.hasText(request.getReaderType()), Reader::getReaderType, request.getReaderType())
               .eq(StringUtils.hasText(request.getCardStatus()), Reader::getStatus, request.getCardStatus())
               .like(StringUtils.hasText(request.getDepartment()), Reader::getDepartment, request.getDepartment())
               .orderByDesc(Reader::getCreatedAt);

        Page<Reader> readerPage = readerMapper.selectPage(page, wrapper);

        // 转换为VO列表
        List<ReaderVO> readerVOList = readerPage.getRecords().stream()
                .map(ReaderVO::from)
                .toList();

        // 使用PageResult.ofRecords确保返回data.records字段
        return PageResult.ofRecords(
                readerPage.getTotal(),
                (int) readerPage.getCurrent(),
                (int) readerPage.getSize(),
                readerVOList
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReader(Long id) {
        Reader reader = readerMapper.selectById(id);
        if (reader == null) {
            throw new BusinessException("读者不存在, id: " + id);
        }

        // 检查是否有未归还的图书
        try {
            Result<Integer> result = circulationServiceClient.getCurrentBorrowCount(id);
            if (result != null && result.getData() != null && result.getData() > 0) {
                throw new BusinessException("该读者有 " + result.getData() + " 本未还图书，无法删除");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("调用circulation-service失败，跳过借阅检查, readerId: {}, error: {}", id, e.getMessage());
        }

        readerMapper.deleteById(id);
        log.info("删除读者成功, id: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReaderDetailVO activateCard(Long id) {
        Reader reader = readerMapper.selectById(id);
        if (reader == null) {
            throw new BusinessException("读者不存在, id: " + id);
        }

        if (!"SUSPENDED".equals(reader.getStatus()) && !"EXPIRED".equals(reader.getStatus())) {
            throw new BusinessException("当前状态不允许激活操作");
        }

        reader.setStatus("ACTIVE");
        if (reader.getExpiryDate() == null || reader.getExpiryDate().isBefore(LocalDate.now())) {
            reader.setExpiryDate(LocalDate.now().plusYears(1)); // 默认有效期1年
        }

        readerMapper.updateById(reader);
        log.info("激活借书卡成功, id: {}", id);

        return ReaderDetailVO.from(reader);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReaderDetailVO suspendCard(Long id) {
        Reader reader = readerMapper.selectById(id);
        if (reader == null) {
            throw new BusinessException("读者不存在, id: " + id);
        }

        if (!"ACTIVE".equals(reader.getStatus())) {
            throw new BusinessException("只有激活状态的借书卡才能挂失");
        }

        reader.setStatus("SUSPENDED");
        readerMapper.updateById(reader);
        log.info("挂失借书卡成功, id: {}", id);

        return ReaderDetailVO.from(reader);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReaderDetailVO cancelCard(Long id) {
        Reader reader = readerMapper.selectById(id);
        if (reader == null) {
            throw new BusinessException("读者不存在, id: " + id);
        }

        // 检查是否有未归还的图书
        try {
            Result<Integer> result = circulationServiceClient.getCurrentBorrowCount(id);
            if (result != null && result.getData() != null && result.getData() > 0) {
                throw new BusinessException("该读者有 " + result.getData() + " 本未还图书，无法注销借书卡");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("调用circulation-service失败，跳过借阅检查, readerId: {}, error: {}", id, e.getMessage());
        }

        reader.setStatus("EXPIRED");
        readerMapper.updateById(reader);
        log.info("注销借书卡成功, id: {}", id);

        return ReaderDetailVO.from(reader);
    }

    @Override
    public boolean validateReaderStatus(Long readerId) {
        Reader reader = readerMapper.selectById(readerId);
        if (reader == null || reader.getDeletedAt() != null) {
            log.info("验证读者状态: readerId={}, 结果=false (读者不存在或已删除)", readerId);
            return false;
        }
        boolean isActive = "ACTIVE".equalsIgnoreCase(reader.getStatus());
        boolean notExpired = reader.getExpiryDate() == null || reader.getExpiryDate().isAfter(LocalDate.now());
        boolean valid = isActive && notExpired;
        log.info("验证读者状态: readerId={}, status={}, expiryDate={}, 结果={}", readerId, reader.getStatus(), reader.getExpiryDate(), valid);
        return valid;
    }

    /**
     * 根据读者类型获取默认最大借阅数量
     */
    private Integer getDefaultMaxBorrowCount(String readerType) {
        return switch (readerType) {
            case "STUDENT" -> 10;
            case "TEACHER" -> 20;
            case "STAFF" -> 15;
            case "EXTERNAL" -> 3;
            default -> 5;
        };
    }

    /**
     * 根据读者类型获取默认最长借阅天数
     */
    private Integer getDefaultMaxBorrowDays(String readerType) {
        return switch (readerType) {
            case "STUDENT" -> 30;
            case "TEACHER" -> 60;
            case "STAFF" -> 45;
            case "EXTERNAL" -> 15;
            default -> 30;
        };
    }
}
