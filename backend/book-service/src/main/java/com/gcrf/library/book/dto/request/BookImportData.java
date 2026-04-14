package com.gcrf.library.book.dto.request;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * Excel导入图书数据模型
 *
 * @author GCRF Team
 */
@Data
public class BookImportData {

    @ExcelProperty("ISBN")
    @ColumnWidth(20)
    private String isbn;

    @ExcelProperty("书名")
    @ColumnWidth(40)
    private String title;

    @ExcelProperty("作者")
    @ColumnWidth(25)
    private String author;

    @ExcelProperty("出版社")
    @ColumnWidth(25)
    private String publisher;

    @ExcelProperty("出版日期")
    @ColumnWidth(15)
    private String publishDate;

    @ExcelProperty("分类")
    @ColumnWidth(15)
    private String categoryName;

    @ExcelProperty("价格")
    @ColumnWidth(10)
    private String price;

    @ExcelProperty("总数量")
    @ColumnWidth(10)
    private Integer totalQuantity;

    @ExcelProperty("语言")
    @ColumnWidth(10)
    private String language;

    @ExcelProperty("简介")
    @ColumnWidth(50)
    private String description;

    @ExcelProperty("馆藏位置")
    @ColumnWidth(20)
    private String location;

    @ExcelProperty("书架号")
    @ColumnWidth(15)
    private String shelfNumber;
}
