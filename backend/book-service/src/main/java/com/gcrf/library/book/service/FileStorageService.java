package com.gcrf.library.book.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.Map;

/**
 * 文件存储服务接口
 *
 * @author GCRF Team
 * @date 2025-11-04
 */
public interface FileStorageService {

    /**
     * 上传图书封面
     *
     * @param bookId 图书ID
     * @param file   图片文件
     * @return 封面URL
     */
    String uploadBookCover(Long bookId, MultipartFile file);

    /**
     * 上传图书PDF
     *
     * @param bookId 图书ID
     * @param file   PDF文件
     * @return PDF元数据（URL、大小等）
     */
    Map<String, Object> uploadBookPdf(Long bookId, MultipartFile file);

    /**
     * 下载图书PDF
     *
     * @param bookId 图书ID
     * @return 文件流
     */
    InputStream downloadBookPdf(Long bookId);

    /**
     * 删除图书封面
     *
     * @param bookId 图书ID
     */
    void deleteBookCover(Long bookId);

    /**
     * 删除图书PDF
     *
     * @param bookId 图书ID
     */
    void deleteBookPdf(Long bookId);
}
