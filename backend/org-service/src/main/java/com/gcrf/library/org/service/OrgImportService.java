package com.gcrf.library.org.service;

import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Excel 批量导入组织节点服务接口
 *
 * <p>Parses an .xlsx file where each row contains [类型, 上级 code, code, 名称],
 * resolves parent references by code, and delegates to {@link OrgNodeService#create} for each row.
 *
 * @author Claude Code
 * @since 2026-04-30
 */
public interface OrgImportService {

    /**
     * 导入结果报告
     */
    @Data
    @Builder
    class ImportReport {
        /** 成功创建的节点数 */
        int created;
        /** 失败的行数 */
        int failed;
        /** 失败行的错误信息列表（格式: "code: 错误原因"） */
        List<String> errors;
    }

    /**
     * 解析上传的 Excel 文件，逐行创建组织节点，并返回导入报告。
     *
     * @param file 上传的 .xlsx 文件（multipart/form-data）
     * @return 包含成功/失败统计及错误明细的 {@link ImportReport}
     */
    ImportReport importExcel(MultipartFile file);
}
