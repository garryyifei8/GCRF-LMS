package com.gcrf.library.book.service;

import com.gcrf.library.book.dto.BookQueryRequest;
import com.gcrf.library.book.dto.request.BookCreateRequest;
import com.gcrf.library.book.dto.request.BookUpdateRequest;
import com.gcrf.library.book.dto.response.BookDetailVO;
import com.gcrf.library.book.dto.response.BookVO;
import com.gcrf.library.common.result.PageResult;

/**
 * 图书服务接口
 *
 * @author GCRF Team
 * @date 2025-10-12
 */
public interface BookService {

    /**
     * 分页查询图书
     *
     * @param request 查询请求
     * @return 分页结果
     */
    PageResult<BookVO> queryBooks(BookQueryRequest request);

    /**
     * 根据ID查询图书详情
     *
     * @param id 图书ID
     * @return 图书详情
     */
    BookDetailVO getBookById(Long id);

    /**
     * 创建图书
     *
     * @param request 创建请求
     * @return 创建后的图书详情
     */
    BookDetailVO createBook(BookCreateRequest request);

    /**
     * 更新图书
     *
     * @param request 更新请求
     * @return 更新后的图书详情
     */
    BookDetailVO updateBook(BookUpdateRequest request);

    /**
     * 删除图书
     *
     * @param id 图书ID
     */
    void deleteBook(Long id);

    /**
     * 减少可借数量
     *
     * @param bookId 图书ID
     */
    void decreaseAvailableQuantity(Long bookId);

    /**
     * 增加可借数量
     *
     * @param bookId 图书ID
     */
    void increaseAvailableQuantity(Long bookId);
}
