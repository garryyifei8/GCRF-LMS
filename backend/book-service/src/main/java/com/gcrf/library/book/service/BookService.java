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
     * 检查图书是否可借
     *
     * @param bookId 图书ID
     * @return true=可借, false=不可借或不存在
     */
    boolean checkAvailability(Long bookId);

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

    /**
     * 获取库存信息
     *
     * @param bookId 图书ID
     * @return 库存信息
     */
    com.gcrf.library.book.dto.response.InventoryVO getInventory(Long bookId);

    /**
     * 更新库存
     *
     * @param bookId 图书ID
     * @param request 更新请求
     * @return 更新后的库存信息
     */
    com.gcrf.library.book.dto.response.InventoryVO updateInventory(Long bookId, com.gcrf.library.book.dto.request.InventoryUpdateRequest request);

    /**
     * 全文搜索图书
     *
     * @param request 搜索请求
     * @return 分页结果
     */
    PageResult<com.gcrf.library.book.dto.response.BookVO> searchBooks(com.gcrf.library.book.dto.request.BookSearchRequest request);

    /**
     * 批量删除图书
     *
     * @param ids 图书ID列表
     * @return 批量操作结果
     */
    com.gcrf.library.book.dto.response.BatchOperationResult batchDelete(java.util.List<Long> ids);

    /**
     * 批量导入图书
     *
     * @param inputStream Excel文件输入流
     * @return 批量操作结果
     */
    com.gcrf.library.book.dto.response.BatchOperationResult batchImport(java.io.InputStream inputStream);

    /**
     * 下载导入模板
     *
     * @param outputStream 输出流
     */
    void downloadImportTemplate(java.io.OutputStream outputStream);

    /**
     * 通过ISBN查询图书信息（第三方API）
     *
     * @param isbn ISBN号
     * @return 图书信息
     */
    com.gcrf.library.book.dto.response.IsbnLookupVO lookupByIsbn(String isbn);

    /**
     * 批量生成条码
     *
     * @param bookIds 图书ID列表
     * @param prefix 条码前缀（可选）
     * @return 生成的条码列表
     */
    java.util.List<com.gcrf.library.book.dto.response.BarcodeVO> generateBarcodes(java.util.List<Long> bookIds, String prefix);

    /**
     * 根据条码查询图书
     *
     * @param barcode 条码
     * @return 图书信息
     */
    com.gcrf.library.book.dto.response.BookDetailVO findByBarcode(String barcode);
}
