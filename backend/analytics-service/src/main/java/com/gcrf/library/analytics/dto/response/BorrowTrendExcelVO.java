package com.gcrf.library.analytics.dto.response;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 借阅趋势Excel导出VO
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowTrendExcelVO {

    @ExcelProperty("日期")
    @ColumnWidth(15)
    private String dateStr;

    @ExcelProperty("借阅量")
    @ColumnWidth(12)
    private Long borrowed;

    @ExcelProperty("归还量")
    @ColumnWidth(12)
    private Long returned;

    @ExcelProperty("到馆人次")
    @ColumnWidth(12)
    private Long visits;

    @ExcelProperty("新增读者")
    @ColumnWidth(12)
    private Long newReaders;

    @ExcelProperty("预约量")
    @ColumnWidth(12)
    private Long reserved;

    @ExcelProperty("续借量")
    @ColumnWidth(12)
    private Long renewed;
}
