package com.gcrf.library.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果封装类
 *
 * 注意：为了兼容前端不同模块的期望：
 * - 图书/读者服务使用 records 字段
 * - 流通服务使用 list 字段
 *
 * @author 张三
 * @date 2025-10-11
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Integer pageNum;

    /**
     * 每页大小
     */
    private Integer pageSize;

    /**
     * 总页数
     */
    private Integer pages;

    /**
     * 数据列表 - 用于流通服务
     */
    private List<T> list;

    /**
     * 数据列表 - 用于图书/读者服务
     */
    private List<T> records;

    /**
     * 构建分页结果 - 使用list字段（流通服务）
     *
     * @param total 总记录数
     * @param pageNum 当前页码
     * @param pageSize 每页大小
     * @param list 数据列表
     */
    public static <T> PageResult<T> of(Long total, Integer pageNum, Integer pageSize, List<T> list) {
        PageResult<T> pageResult = new PageResult<>();
        pageResult.setTotal(total);
        pageResult.setPageNum(pageNum);
        pageResult.setPageSize(pageSize);
        pageResult.setPages(calculatePages(total, pageSize));
        pageResult.setList(list);
        return pageResult;
    }

    /**
     * 构建分页结果 - 使用records字段（图书/读者服务）
     *
     * @param total 总记录数
     * @param pageNum 当前页码
     * @param pageSize 每页大小
     * @param records 数据列表
     */
    public static <T> PageResult<T> ofRecords(Long total, Integer pageNum, Integer pageSize, List<T> records) {
        PageResult<T> pageResult = new PageResult<>();
        pageResult.setTotal(total);
        pageResult.setPageNum(pageNum);
        pageResult.setPageSize(pageSize);
        pageResult.setPages(calculatePages(total, pageSize));
        pageResult.setRecords(records);
        return pageResult;
    }

    /**
     * 空分页结果 - list字段
     */
    public static <T> PageResult<T> empty(Integer pageNum, Integer pageSize) {
        return of(0L, pageNum, pageSize, List.of());
    }

    /**
     * 空分页结果 - records字段
     */
    public static <T> PageResult<T> emptyRecords(Integer pageNum, Integer pageSize) {
        return ofRecords(0L, pageNum, pageSize, List.of());
    }

    /**
     * 计算总页数
     */
    private static int calculatePages(Long total, Integer pageSize) {
        if (total == null || total == 0 || pageSize == null || pageSize == 0) {
            return 0;
        }
        return (int) Math.ceil((double) total / pageSize);
    }
}
