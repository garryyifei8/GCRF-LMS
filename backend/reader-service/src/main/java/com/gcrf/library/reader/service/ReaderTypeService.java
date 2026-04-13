package com.gcrf.library.reader.service;

import com.gcrf.library.reader.dto.request.ReaderTypeCreateRequest;
import com.gcrf.library.reader.dto.request.ReaderTypeUpdateRequest;
import com.gcrf.library.reader.dto.response.ReaderTypeVO;

import java.util.List;

/**
 * 读者类型服务接口
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
public interface ReaderTypeService {

    /**
     * 获取所有读者类型列表
     *
     * @return 读者类型列表
     */
    List<ReaderTypeVO> listAllTypes();

    /**
     * 根据ID获取读者类型详情
     *
     * @param id 读者类型ID
     * @return 读者类型详情
     */
    ReaderTypeVO getTypeById(Long id);

    /**
     * 根据类型代码获取读者类型详情
     *
     * @param typeCode 类型代码
     * @return 读者类型详情
     */
    ReaderTypeVO getTypeByCode(String typeCode);

    /**
     * 创建读者类型
     *
     * @param request 创建请求
     * @return 创建后的读者类型
     */
    ReaderTypeVO createType(ReaderTypeCreateRequest request);

    /**
     * 更新读者类型
     *
     * @param request 更新请求
     * @return 更新后的读者类型
     */
    ReaderTypeVO updateType(ReaderTypeUpdateRequest request);

    /**
     * 删除读者类型（逻辑删除）
     *
     * @param id 读者类型ID
     */
    void deleteType(Long id);

    /**
     * 检查类型代码是否存在
     *
     * @param typeCode 类型代码
     * @param excludeId 排除的ID（用于更新时检查）
     * @return 是否存在
     */
    boolean existsByTypeCode(String typeCode, Long excludeId);
}
