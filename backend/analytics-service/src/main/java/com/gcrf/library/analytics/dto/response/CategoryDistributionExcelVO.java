package com.gcrf.library.analytics.dto.response;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 分类分布Excel导出VO
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDistributionExcelVO {

    @ExcelProperty("分类编码")
    @ColumnWidth(12)
    private String code;

    @ExcelProperty("分类名称")
    @ColumnWidth(18)
    private String name;

    @ExcelProperty("图书数量")
    @ColumnWidth(12)
    private Long bookCount;

    @ExcelProperty("借阅次数")
    @ColumnWidth(12)
    private Long borrowCount;

    @ExcelProperty("流通率")
    @ColumnWidth(10)
    private BigDecimal circulationRate;

    @ExcelProperty("读者数量")
    @ColumnWidth(12)
    private Long readerCount;

    @ExcelProperty("占比")
    @ColumnWidth(10)
    private BigDecimal percentage;

    @ExcelProperty("零借阅数量")
    @ColumnWidth(12)
    private Long zeroCirculationCount;

    @ExcelProperty("零借阅率")
    @ColumnWidth(10)
    private BigDecimal zeroCirculationRate;
}
