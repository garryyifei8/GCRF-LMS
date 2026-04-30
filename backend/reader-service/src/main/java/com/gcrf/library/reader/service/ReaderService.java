package com.gcrf.library.reader.service;

import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.reader.dto.*;
import com.gcrf.library.reader.dto.response.ReaderDetailVO;
import com.gcrf.library.reader.dto.response.ReaderVO;

/**
 * 读者服务接口
 *
 * @author GCRF Team
 * @since 2025-10-13
 */
public interface ReaderService {

    /**
     * 创建读者
     *
     * @param request 创建读者请求
     * @return 读者详情
     */
    ReaderDetailVO createReader(ReaderCreateRequest request);

    /**
     * 更新读者信息
     *
     * @param request 更新读者请求
     * @return 读者详情
     */
    ReaderDetailVO updateReader(ReaderUpdateRequest request);

    /**
     * 根据ID获取读者信息
     *
     * @param id 读者ID
     * @return 读者详情
     */
    ReaderDetailVO getReaderById(Long id);

    /**
     * 根据读者证号获取读者信息
     *
     * @param readerId 读者证号
     * @return 读者详情
     */
    ReaderDetailVO getReaderByReaderId(String readerId);

    /**
     * 分页查询读者列表
     *
     * @param request 查询请求
     * @return 分页结果
     */
    PageResult<ReaderVO> queryReaders(ReaderQueryRequest request);

    /**
     * 删除读者（逻辑删除）
     *
     * @param id 读者ID
     */
    void deleteReader(Long id);

    /**
     * 激活借书卡
     *
     * @param id 读者ID
     * @return 读者详情
     */
    ReaderDetailVO activateCard(Long id);

    /**
     * 挂失借书卡
     *
     * @param id 读者ID
     * @return 读者详情
     */
    ReaderDetailVO suspendCard(Long id);

    /**
     * 注销借书卡
     *
     * @param id 读者ID
     * @return 读者详情
     */
    ReaderDetailVO cancelCard(Long id);

    /**
     * 验证读者状态（是否可以借书）
     *
     * @param readerId 读者ID
     * @return true表示状态正常可借书，false表示不可借书
     */
    boolean validateReaderStatus(Long readerId);

    /**
     * 按年级批量注销学生读者
     *
     * @param grade 年级（如"2020"），匹配学号中包含该年级字符串的学生
     * @return 注销的读者数量
     */
    int batchCancelByGrade(String grade);
}
