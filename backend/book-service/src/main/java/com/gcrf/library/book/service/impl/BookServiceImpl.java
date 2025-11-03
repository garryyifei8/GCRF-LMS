package com.gcrf.library.book.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.book.dto.BookQueryRequest;
import com.gcrf.library.book.dto.request.BookCreateRequest;
import com.gcrf.library.book.dto.request.BookUpdateRequest;
import com.gcrf.library.book.dto.response.BookDetailVO;
import com.gcrf.library.book.dto.response.BookVO;
import com.gcrf.library.book.entity.Book;
import com.gcrf.library.book.mapper.BookMapper;
import com.gcrf.library.book.service.BookService;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.stream.Collectors;

/**
 * 图书服务实现
 *
 * @author GCRF Team
 * @date 2025-10-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookMapper bookMapper;

    /**
     * 分页查询图书
     */
    @Override
    public PageResult<BookVO> queryBooks(BookQueryRequest request) {
        log.info("分页查询图书: {}", request);

        Page<Book> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();

        // 只查询未删除的记录
        queryWrapper.isNull(Book::getDeletedAt);

        // 关键词搜索（标题、作者、ISBN）
        if (StringUtils.hasText(request.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Book::getTitle, request.getKeyword())
                    .or().like(Book::getAuthor, request.getKeyword())
                    .or().like(Book::getIsbn, request.getKeyword())
            );
        }

        // 作者筛选
        if (StringUtils.hasText(request.getAuthor())) {
            queryWrapper.like(Book::getAuthor, request.getAuthor());
        }

        // 出版社筛选
        if (StringUtils.hasText(request.getPublisher())) {
            queryWrapper.like(Book::getPublisher, request.getPublisher());
        }

        // 状态筛选 - status为Integer: 1=ACTIVE, 0=INACTIVE
        if (request.getStatus() != null) {
            String statusStr = request.getStatus() == 1 ? "ACTIVE" : "INACTIVE";
            queryWrapper.eq(Book::getStatus, statusStr);
        }

        // 按创建时间倒序
        queryWrapper.orderByDesc(Book::getCreatedAt);

        Page<Book> result = bookMapper.selectPage(page, queryWrapper);

        // 转换为VO
        var records = result.getRecords().stream()
                .map(BookVO::from)
                .collect(Collectors.toList());

        // 使用ofRecords()方法，因为前端期望data.records字段
        return PageResult.ofRecords(result.getTotal(), (int)result.getCurrent(), (int)result.getSize(), records);
    }

    /**
     * 根据ID查询图书详情
     */
    @Override
    public BookDetailVO getBookById(Long id) {
        Book book = findBookById(id);
        return BookDetailVO.from(book);
    }

    /**
     * 内部方法：查询图书实体
     */
    private Book findBookById(Long id) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "图书不存在");
        }
        return book;
    }

    /**
     * 创建图书
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookDetailVO createBook(BookCreateRequest request) {
        log.info("创建图书: {}", request.getTitle());

        // 检查ISBN是否重复
        LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Book::getIsbn, request.getIsbn());
        if (bookMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_DATA, "ISBN已存在");
        }

        // 转换为实体
        Book book = request.toEntity();

        // 初始化可借数量等于总数
        book.setAvailableQuantity(book.getTotalQuantity());

        bookMapper.insert(book);
        return BookDetailVO.from(book);
    }

    /**
     * 更新图书
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookDetailVO updateBook(BookUpdateRequest request) {
        log.info("更新图书: id={}", request.getId());

        Book existingBook = findBookById(request.getId());

        // 如果修改了ISBN，检查是否重复
        if (!existingBook.getIsbn().equals(request.getIsbn())) {
            LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Book::getIsbn, request.getIsbn());
            queryWrapper.ne(Book::getId, request.getId());
            if (bookMapper.selectCount(queryWrapper) > 0) {
                throw new BusinessException(ResultCode.DUPLICATE_DATA, "ISBN已存在");
            }
        }

        // 转换为实体并更新
        Book book = request.toEntity();
        bookMapper.updateById(book);

        // 重新查询返回最新数据
        return BookDetailVO.from(findBookById(book.getId()));
    }

    /**
     * 删除图书
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBook(Long id) {
        log.info("删除图书: id={}", id);

        Book book = findBookById(id);

        // 检查是否有在借记录
        if (book.getAvailableQuantity() < book.getTotalQuantity()) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "该图书有在借记录，无法删除");
        }

        bookMapper.deleteById(id);
    }

    /**
     * 减少可借数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decreaseAvailableQuantity(Long bookId) {
        Book book = findBookById(bookId);

        if (book.getAvailableQuantity() <= 0) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "图书已全部借出");
        }

        book.setAvailableQuantity(book.getAvailableQuantity() - 1);
        bookMapper.updateById(book);
    }

    /**
     * 增加可借数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increaseAvailableQuantity(Long bookId) {
        Book book = findBookById(bookId);

        if (book.getAvailableQuantity() >= book.getTotalQuantity()) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "可借数量异常");
        }

        book.setAvailableQuantity(book.getAvailableQuantity() + 1);
        bookMapper.updateById(book);
    }
}
