package com.gcrf.library.org.domain.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * Excel 导入行 DTO
 *
 * <p>Maps a single row from the import spreadsheet. Column headers are in Chinese
 * to match the template provided to administrators.
 *
 * @author Claude Code
 * @since 2026-04-30
 */
@Data
public class OrgImportRow {

    /** 节点类型: REGION / DISTRICT / SCHOOL / SUB_SCHOOL / BRANCH / STAGE / GRADE / CLASS */
    @ExcelProperty("类型")
    private String type;

    /** 上级节点 code，根节点留空 */
    @ExcelProperty("上级 code")
    private String parentCode;

    /** 节点唯一标识 code（仅允许字母/数字/下划线/连字符） */
    @ExcelProperty("code")
    private String code;

    /** 节点名称 */
    @ExcelProperty("名称")
    private String name;
}
