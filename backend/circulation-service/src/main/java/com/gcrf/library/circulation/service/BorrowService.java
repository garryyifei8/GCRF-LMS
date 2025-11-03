package com.gcrf.library.circulation.service;

import com.gcrf.library.circulation.dto.BorrowQueryRequest;
import com.gcrf.library.circulation.dto.BorrowRequest;
import com.gcrf.library.circulation.dto.RenewRequest;
import com.gcrf.library.circulation.dto.ReturnRequest;
import com.gcrf.library.circulation.dto.response.BorrowDetailVO;
import com.gcrf.library.circulation.dto.response.BorrowVO;
import com.gcrf.library.common.result.PageResult;

import java.util.List;

/**
 * 借阅服务接口
 *
 * @author GCRF Team
 * @since 2025-10-28
 */
public interface BorrowService {

    /**
     * 分页查询借阅记录
     *
     * @param request 查询条件
     * @return 借阅记录分页列表
     */
    PageResult<BorrowVO> queryBorrows(BorrowQueryRequest request);

    /**
     * 根据ID获取借阅详情
     *
     * @param id 借阅记录ID
     * @return 借阅详情
     */
    BorrowDetailVO getBorrowById(Long id);

    /**
     * 借书
     *
     * @param request 借阅请求
     * @return 借阅详情
     */
    BorrowDetailVO borrowBook(BorrowRequest request);

    /**
     * 还书
     *
     * @param request 归还请求
     * @return 借阅详情
     */
    BorrowDetailVO returnBook(ReturnRequest request);

    /**
     * 续借
     *
     * @param request 续借请求
     * @return 借阅详情
     */
    BorrowDetailVO renewBook(RenewRequest request);

    /**
     * 获取所有逾期未归还的借阅记录
     *
     * @return 逾期借阅记录列表
     */
    List<BorrowVO> getOverdueBorrows();

    /**
     * 批量更新逾期状态（定时任务调用）
     * 将已超过应还日期但状态仍为BORROWED的记录状态更新为OVERDUE
     */
    void updateOverdueStatus();
}
