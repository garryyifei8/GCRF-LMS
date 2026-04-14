package com.gcrf.library.analytics.dto.response;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 活跃读者Excel导出VO
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveReaderExcelVO {

    @ExcelProperty("排名")
    @ColumnWidth(8)
    private Integer rank;

    @ExcelProperty("读者证号")
    @ColumnWidth(15)
    private String cardNo;

    @ExcelProperty("姓名")
    @ColumnWidth(12)
    private String realName;

    @ExcelProperty("读者类型")
    @ColumnWidth(12)
    private String readerTypeName;

    @ExcelProperty("借阅次数")
    @ColumnWidth(12)
    private Long borrowCount;

    @ExcelProperty("到馆次数")
    @ColumnWidth(12)
    private Long visitCount;

    @ExcelProperty("偏好分类")
    @ColumnWidth(15)
    private String favoriteCategory;

    @ExcelProperty("当前借阅数")
    @ColumnWidth(12)
    private Integer currentBorrowCount;

    @ExcelProperty("累计逾期次数")
    @ColumnWidth(12)
    private Integer overdueCount;
}
