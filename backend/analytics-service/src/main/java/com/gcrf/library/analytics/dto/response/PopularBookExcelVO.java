package com.gcrf.library.analytics.dto.response;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 热门图书Excel导出VO
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopularBookExcelVO {

    @ExcelProperty("排名")
    @ColumnWidth(8)
    private Integer rank;

    @ExcelProperty("ISBN")
    @ColumnWidth(18)
    private String isbn;

    @ExcelProperty("书名")
    @ColumnWidth(30)
    private String title;

    @ExcelProperty("作者")
    @ColumnWidth(15)
    private String author;

    @ExcelProperty("分类")
    @ColumnWidth(12)
    private String categoryName;

    @ExcelProperty("借阅次数")
    @ColumnWidth(12)
    private Long borrowCount;

    @ExcelProperty("评分")
    @ColumnWidth(8)
    private BigDecimal rating;

    @ExcelProperty("总副本数")
    @ColumnWidth(10)
    private Integer totalCopies;

    @ExcelProperty("可借副本数")
    @ColumnWidth(12)
    private Integer availableCopies;

    @ExcelProperty("预约数")
    @ColumnWidth(10)
    private Integer reservationCount;
}
