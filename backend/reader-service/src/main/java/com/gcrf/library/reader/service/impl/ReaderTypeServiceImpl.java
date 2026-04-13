package com.gcrf.library.reader.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.reader.dto.request.ReaderTypeCreateRequest;
import com.gcrf.library.reader.dto.request.ReaderTypeUpdateRequest;
import com.gcrf.library.reader.dto.response.ReaderTypeVO;
import com.gcrf.library.reader.entity.ReaderType;
import com.gcrf.library.reader.mapper.ReaderTypeMapper;
import com.gcrf.library.reader.service.ReaderTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 读者类型服务实现类
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReaderTypeServiceImpl implements ReaderTypeService {

    private final ReaderTypeMapper readerTypeMapper;

    @Override
    public List<ReaderTypeVO> listAllTypes() {
        LambdaQueryWrapper<ReaderType> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(ReaderType::getDeletedAt)
               .orderByAsc(ReaderType::getSortOrder)
               .orderByAsc(ReaderType::getId);

        List<ReaderType> types = readerTypeMapper.selectList(wrapper);

        return types.stream()
                .map(this::convertToVO)
                .toList();
    }

    @Override
    public ReaderTypeVO getTypeById(Long id) {
        ReaderType type = readerTypeMapper.selectById(id);
        if (type == null || type.getDeletedAt() != null) {
            throw new BusinessException("读者类型不存在, id: " + id);
        }
        return convertToVO(type);
    }

    @Override
    public ReaderTypeVO getTypeByCode(String typeCode) {
        LambdaQueryWrapper<ReaderType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReaderType::getTypeCode, typeCode)
               .isNull(ReaderType::getDeletedAt);

        ReaderType type = readerTypeMapper.selectOne(wrapper);
        if (type == null) {
            throw new BusinessException("读者类型不存在, typeCode: " + typeCode);
        }
        return convertToVO(type);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReaderTypeVO createType(ReaderTypeCreateRequest request) {
        // 检查类型代码是否已存在
        if (existsByTypeCode(request.getTypeCode(), null)) {
            throw new BusinessException("类型代码已存在: " + request.getTypeCode());
        }

        ReaderType type = new ReaderType();
        type.setTypeCode(request.getTypeCode());
        type.setTypeName(request.getTypeName());
        type.setMaxBorrowCount(request.getMaxBorrowCount());
        type.setMaxBorrowDays(request.getMaxBorrowDays());
        type.setMaxRenewCount(request.getMaxRenewCount());
        type.setDepositAmount(request.getDepositAmount() != null ? request.getDepositAmount() : 0);
        type.setDescription(request.getDescription());
        type.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "ACTIVE");
        type.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        type.setCreatedAt(LocalDateTime.now());
        type.setUpdatedAt(LocalDateTime.now());

        readerTypeMapper.insert(type);
        log.info("创建读者类型成功, typeCode: {}", type.getTypeCode());

        return convertToVO(type);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReaderTypeVO updateType(ReaderTypeUpdateRequest request) {
        ReaderType type = readerTypeMapper.selectById(request.getId());
        if (type == null || type.getDeletedAt() != null) {
            throw new BusinessException("读者类型不存在, id: " + request.getId());
        }

        // 更新字段（只更新非空字段）
        if (StringUtils.hasText(request.getTypeName())) {
            type.setTypeName(request.getTypeName());
        }
        if (request.getMaxBorrowCount() != null) {
            type.setMaxBorrowCount(request.getMaxBorrowCount());
        }
        if (request.getMaxBorrowDays() != null) {
            type.setMaxBorrowDays(request.getMaxBorrowDays());
        }
        if (request.getMaxRenewCount() != null) {
            type.setMaxRenewCount(request.getMaxRenewCount());
        }
        if (request.getDepositAmount() != null) {
            type.setDepositAmount(request.getDepositAmount());
        }
        if (request.getDescription() != null) {
            type.setDescription(request.getDescription());
        }
        if (StringUtils.hasText(request.getStatus())) {
            type.setStatus(request.getStatus());
        }
        if (request.getSortOrder() != null) {
            type.setSortOrder(request.getSortOrder());
        }

        type.setUpdatedAt(LocalDateTime.now());

        int updated = readerTypeMapper.updateById(type);
        if (updated == 0) {
            throw new BusinessException("更新失败,数据可能已被修改,请刷新后重试");
        }

        log.info("更新读者类型成功, id: {}", type.getId());
        return convertToVO(type);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteType(Long id) {
        ReaderType type = readerTypeMapper.selectById(id);
        if (type == null || type.getDeletedAt() != null) {
            throw new BusinessException("读者类型不存在, id: " + id);
        }

        // 检查是否有读者正在使用该类型
        int readerCount = readerTypeMapper.countReadersByType(type.getTypeCode());
        if (readerCount > 0) {
            throw new BusinessException("该读者类型正在被 " + readerCount + " 位读者使用，无法删除");
        }

        // 逻辑删除：设置 deleted_at
        type.setDeletedAt(LocalDateTime.now());
        type.setUpdatedAt(LocalDateTime.now());
        readerTypeMapper.updateById(type);

        log.info("删除读者类型成功, id: {}", id);
    }

    @Override
    public boolean existsByTypeCode(String typeCode, Long excludeId) {
        return readerTypeMapper.existsByTypeCode(typeCode, excludeId) > 0;
    }

    /**
     * 转换实体为VO，并填充读者数量
     */
    private ReaderTypeVO convertToVO(ReaderType type) {
        ReaderTypeVO vo = ReaderTypeVO.from(type);
        // 填充使用该类型的读者数量
        int readerCount = readerTypeMapper.countReadersByType(type.getTypeCode());
        vo.setReaderCount(readerCount);
        return vo;
    }
}
