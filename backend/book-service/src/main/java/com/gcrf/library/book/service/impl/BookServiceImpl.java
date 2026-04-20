package com.gcrf.library.book.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.book.dto.BookQueryRequest;
import com.gcrf.library.book.dto.request.BookCreateRequest;
import com.gcrf.library.book.dto.request.BookImportData;
import com.gcrf.library.book.dto.request.BookUpdateRequest;
import com.gcrf.library.book.dto.response.BatchOperationResult;
import com.gcrf.library.book.dto.response.BookDetailVO;
import com.gcrf.library.book.dto.response.BookVO;
import com.gcrf.library.book.dto.response.BarcodeVO;
import com.gcrf.library.book.dto.response.IsbnLookupVO;
import com.gcrf.library.book.entity.Book;
import com.gcrf.library.book.mapper.BookMapper;
import com.gcrf.library.book.client.IsbnApiClient;
import com.gcrf.library.book.config.CacheConfig;
import com.gcrf.library.book.event.BookEvent;
import com.gcrf.library.book.event.BookEventPublisher;
import com.gcrf.library.book.service.BookService;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
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
    private final IsbnApiClient isbnApiClient;
    private final BookEventPublisher eventPublisher;

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
    @Cacheable(value = CacheConfig.CACHE_BOOK_DETAIL, key = "#id", unless = "#result == null")
    public BookDetailVO getBookById(Long id) {
        log.debug("从数据库查询图书详情: id={}", id);
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

        // 发布图书创建事件
        eventPublisher.publishBookCreated(BookEvent.created(
                book.getId(),
                book.getIsbn(),
                book.getTitle(),
                book.getAuthor(),
                null,  // categoryId - 可从classificationCode解析
                null   // categoryName
        ));

        return BookDetailVO.from(book);
    }

    /**
     * 更新图书
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheConfig.CACHE_BOOK_DETAIL, key = "#request.id")
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

        // 发布图书更新事件
        Book updatedBook = findBookById(book.getId());
        eventPublisher.publishBookUpdated(BookEvent.updated(
                updatedBook.getId(),
                updatedBook.getIsbn(),
                updatedBook.getTitle(),
                updatedBook.getAuthor()
        ));

        // 重新查询返回最新数据
        return BookDetailVO.from(updatedBook);
    }

    /**
     * 删除图书
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CacheConfig.CACHE_BOOK_DETAIL, key = "#id")
    public void deleteBook(Long id) {
        log.info("删除图书: id={}", id);

        Book book = findBookById(id);

        // 检查是否有在借记录
        if (book.getAvailableQuantity() < book.getTotalQuantity()) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "该图书有在借记录，无法删除");
        }

        // 保存信息用于发布事件
        String isbn = book.getIsbn();
        String title = book.getTitle();

        bookMapper.deleteById(id);

        // 发布图书删除事件
        eventPublisher.publishBookDeleted(BookEvent.deleted(id, isbn, title));
    }

    /**
     * 检查图书是否可借
     */
    @Override
    public boolean checkAvailability(Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getDeletedAt() != null) {
            return false;
        }
        return book.getAvailableQuantity() != null && book.getAvailableQuantity() > 0;
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

    /**
     * 获取库存信息
     */
    @Override
    public com.gcrf.library.book.dto.response.InventoryVO getInventory(Long bookId) {
        log.info("获取库存信息: bookId={}", bookId);
        Book book = findBookById(bookId);
        return com.gcrf.library.book.dto.response.InventoryVO.from(book);
    }

    /**
     * 更新库存
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public com.gcrf.library.book.dto.response.InventoryVO updateInventory(Long bookId, com.gcrf.library.book.dto.request.InventoryUpdateRequest request) {
        log.info("更新库存: bookId={}, newTotal={}, reason={}", bookId, request.getTotalCopies(), request.getReason());

        Book book = findBookById(bookId);

        // 计算已借出数量
        Integer borrowedCount = book.getTotalQuantity() - book.getAvailableQuantity();

        // 新的总量不能小于已借出数量
        if (request.getTotalCopies() < borrowedCount) {
            throw new BusinessException(ResultCode.OPERATION_FAILED,
                String.format("新的总量(%d)不能小于已借出数量(%d)", request.getTotalCopies(), borrowedCount));
        }

        // 更新总量和可借数量
        book.setTotalQuantity(request.getTotalCopies());
        book.setAvailableQuantity(request.getTotalCopies() - borrowedCount);

        // 更新borrowed和reserved数量
        if (book.getBorrowedQuantity() == null) {
            book.setBorrowedQuantity(borrowedCount);
        }
        if (book.getReservedQuantity() == null) {
            book.setReservedQuantity(0);
        }

        bookMapper.updateById(book);

        log.info("库存更新成功: bookId={}, newTotal={}, newAvailable={}", bookId, book.getTotalQuantity(), book.getAvailableQuantity());
        return com.gcrf.library.book.dto.response.InventoryVO.from(book);
    }

    /**
     * 全文搜索图书 - 使用PostgreSQL原生全文搜索功能
     * 支持tsvector索引和ts_rank相关度排序
     */
    @Override
    public PageResult<BookVO> searchBooks(com.gcrf.library.book.dto.request.BookSearchRequest request) {
        log.info("全文搜索图书: query={}, pageNum={}, pageSize={}",
                request.getQuery(), request.getPageNum(), request.getPageSize());

        String query = request.getQuery().trim();
        int offset = (request.getPageNum() - 1) * request.getPageSize();
        int limit = request.getPageSize();

        // 构建分类代码
        String categoryCode = null;
        if (request.getCategoryId() != null) {
            categoryCode = request.getCategoryId().toString();
        }

        // 使用PostgreSQL全文搜索
        java.util.List<Book> books = bookMapper.fullTextSearchWithFilters(
                query,
                categoryCode,
                request.getPublisher(),
                request.getLanguage(),
                Boolean.TRUE.equals(request.getAvailableOnly()),
                offset,
                limit
        );

        // 获取总数
        long total = bookMapper.fullTextSearchCountWithFilters(
                query,
                categoryCode,
                request.getPublisher(),
                request.getLanguage(),
                Boolean.TRUE.equals(request.getAvailableOnly())
        );

        // 转换为VO
        var records = books.stream()
                .map(BookVO::from)
                .collect(Collectors.toList());

        log.info("全文搜索完成: query={}, total={}, returned={}", query, total, records.size());
        return PageResult.ofRecords(total, request.getPageNum(), request.getPageSize(), records);
    }

    /**
     * 批量删除图书
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResult batchDelete(List<Long> ids) {
        log.info("批量删除图书: ids={}", ids);

        BatchOperationResult result = BatchOperationResult.builder()
                .totalCount(ids.size())
                .successCount(0)
                .failedCount(0)
                .errors(new ArrayList<>())
                .build();

        for (Long id : ids) {
            try {
                Book book = bookMapper.selectById(id);
                if (book == null) {
                    result.addError(null, id, "图书不存在", null);
                    continue;
                }

                // 检查是否有在借记录
                if (book.getAvailableQuantity() < book.getTotalQuantity()) {
                    result.addError(null, id, "该图书有在借记录，无法删除", book.getTitle());
                    continue;
                }

                bookMapper.deleteById(id);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception e) {
                log.error("删除图书失败: id={}", id, e);
                result.addError(null, id, "删除失败: " + e.getMessage(), null);
            }
        }

        log.info("批量删除完成: total={}, success={}, failed={}",
                result.getTotalCount(), result.getSuccessCount(), result.getFailedCount());
        return result;
    }

    /**
     * 批量导入图书
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResult batchImport(InputStream inputStream) {
        log.info("开始批量导入图书");

        BatchOperationResult result = BatchOperationResult.builder()
                .totalCount(0)
                .successCount(0)
                .failedCount(0)
                .errors(new ArrayList<>())
                .build();

        List<BookImportData> dataList = new ArrayList<>();

        // 读取Excel数据
        EasyExcel.read(inputStream, BookImportData.class, new ReadListener<BookImportData>() {
            @Override
            public void invoke(BookImportData data, AnalysisContext context) {
                dataList.add(data);
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                // 读取完成
            }
        }).sheet().doRead();

        result.setTotalCount(dataList.size());

        // 处理每条数据
        int rowNum = 2; // Excel从第2行开始（第1行是表头）
        for (BookImportData data : dataList) {
            try {
                // 验证必填字段
                if (!StringUtils.hasText(data.getIsbn())) {
                    result.addError(rowNum, null, "ISBN不能为空", null);
                    rowNum++;
                    continue;
                }
                if (!StringUtils.hasText(data.getTitle())) {
                    result.addError(rowNum, null, "书名不能为空", data.getIsbn());
                    rowNum++;
                    continue;
                }

                // 检查ISBN是否已存在
                LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(Book::getIsbn, data.getIsbn().trim());
                if (bookMapper.selectCount(queryWrapper) > 0) {
                    result.addError(rowNum, null, "ISBN已存在", data.getIsbn());
                    rowNum++;
                    continue;
                }

                // 转换为实体
                Book book = convertImportDataToBook(data);
                bookMapper.insert(book);
                result.setSuccessCount(result.getSuccessCount() + 1);

            } catch (Exception e) {
                log.error("导入图书失败: row={}, isbn={}", rowNum, data.getIsbn(), e);
                result.addError(rowNum, null, "导入失败: " + e.getMessage(), data.getIsbn());
            }
            rowNum++;
        }

        log.info("批量导入完成: total={}, success={}, failed={}",
                result.getTotalCount(), result.getSuccessCount(), result.getFailedCount());
        return result;
    }

    /**
     * 下载导入模板
     */
    @Override
    public void downloadImportTemplate(OutputStream outputStream) {
        log.info("下载导入模板");

        // 创建示例数据
        List<BookImportData> templateData = new ArrayList<>();
        BookImportData sample = new BookImportData();
        sample.setIsbn("978-7-111-12345-6");
        sample.setTitle("示例图书名称");
        sample.setAuthor("作者姓名");
        sample.setPublisher("出版社名称");
        sample.setPublishDate("2024-01-01");
        sample.setCategoryName("文学");
        sample.setPrice("59.00");
        sample.setTotalQuantity(10);
        sample.setLanguage("zh-CN");
        sample.setDescription("图书简介...");
        sample.setLocation("A区");
        sample.setShelfNumber("A-01-01");
        templateData.add(sample);

        EasyExcel.write(outputStream, BookImportData.class)
                .sheet("图书导入模板")
                .doWrite(templateData);
    }

    /**
     * 将导入数据转换为Book实体
     */
    private Book convertImportDataToBook(BookImportData data) {
        Book book = new Book();
        book.setIsbn(data.getIsbn().trim());
        book.setTitle(data.getTitle().trim());
        book.setAuthor(StringUtils.hasText(data.getAuthor()) ? data.getAuthor().trim() : null);
        book.setPublisher(StringUtils.hasText(data.getPublisher()) ? data.getPublisher().trim() : null);

        // 解析出版日期
        if (StringUtils.hasText(data.getPublishDate())) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                book.setPublishDate(LocalDate.parse(data.getPublishDate().trim(), formatter));
            } catch (DateTimeParseException e) {
                log.warn("出版日期格式错误: {}", data.getPublishDate());
            }
        }

        // 解析价格
        if (StringUtils.hasText(data.getPrice())) {
            try {
                book.setPrice(new BigDecimal(data.getPrice().trim()));
            } catch (NumberFormatException e) {
                log.warn("价格格式错误: {}", data.getPrice());
            }
        }

        // 设置数量
        int quantity = data.getTotalQuantity() != null && data.getTotalQuantity() > 0 ? data.getTotalQuantity() : 1;
        book.setTotalQuantity(quantity);
        book.setAvailableQuantity(quantity);
        book.setBorrowedQuantity(0);
        book.setReservedQuantity(0);

        // 其他字段
        book.setLanguage(StringUtils.hasText(data.getLanguage()) ? data.getLanguage().trim() : "zh-CN");
        book.setDescription(StringUtils.hasText(data.getDescription()) ? data.getDescription().trim() : null);
        // Note: location and shelfNumber are stored in the Inventory table, not in Book
        // They will be handled separately when creating inventory records

        // 默认状态
        book.setStatus("ACTIVE");

        return book;
    }

    /**
     * 通过ISBN查询图书信息（第三方API）
     * 缓存24小时，避免频繁调用第三方API
     */
    @Override
    @Cacheable(value = CacheConfig.CACHE_ISBN_LOOKUP, key = "#isbn", unless = "#result == null || !#result.found")
    public IsbnLookupVO lookupByIsbn(String isbn) {
        log.info("通过ISBN查询图书信息: {}", isbn);

        if (isbn == null || isbn.trim().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "ISBN不能为空");
        }

        return isbnApiClient.lookupByIsbn(isbn.trim());
    }

    /**
     * 批量生成条码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<BarcodeVO> generateBarcodes(List<Long> bookIds, String prefix) {
        log.info("批量生成条码: bookIds={}, prefix={}", bookIds, prefix);

        String barcodePrefix = StringUtils.hasText(prefix) ? prefix : "GCRF";
        List<BarcodeVO> results = new ArrayList<>();

        for (Long bookId : bookIds) {
            Book book = bookMapper.selectById(bookId);
            if (book == null) {
                log.warn("图书不存在: id={}", bookId);
                continue;
            }

            // 如果已有条码，跳过
            if (StringUtils.hasText(book.getBarcode())) {
                log.info("图书已有条码: id={}, barcode={}", bookId, book.getBarcode());
                results.add(BarcodeVO.from(book));
                continue;
            }

            // 生成新条码
            Long sequence = bookMapper.getNextBarcodeSequence();
            String barcode = String.format("%s-%d-%08d", barcodePrefix, java.time.LocalDate.now().getYear(), sequence);

            book.setBarcode(barcode);
            bookMapper.updateById(book);

            log.info("生成条码成功: bookId={}, barcode={}", bookId, barcode);
            results.add(BarcodeVO.from(book));
        }

        return results;
    }

    /**
     * 根据条码查询图书
     */
    @Override
    public BookDetailVO findByBarcode(String barcode) {
        log.info("根据条码查询图书: {}", barcode);

        if (!StringUtils.hasText(barcode)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "条码不能为空");
        }

        Book book = bookMapper.findByBarcode(barcode.trim());
        if (book == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "未找到该条码对应的图书");
        }

        return BookDetailVO.from(book);
    }
}
