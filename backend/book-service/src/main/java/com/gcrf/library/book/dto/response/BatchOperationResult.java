package com.gcrf.library.book.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量操作结果
 *
 * @author GCRF Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchOperationResult {

    /**
     * 成功数量
     */
    private int successCount;

    /**
     * 失败数量
     */
    private int failedCount;

    /**
     * 总数量
     */
    private int totalCount;

    /**
     * 错误详情列表
     */
    @Builder.Default
    private List<ErrorDetail> errors = new ArrayList<>();

    /**
     * 错误详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetail {
        /**
         * 行号（Excel导入时使用）
         */
        private Integer row;

        /**
         * ID（批量删除时使用）
         */
        private Long id;

        /**
         * 错误信息
         */
        private String message;

        /**
         * 相关数据（如ISBN、标题等）
         */
        private String data;
    }

    /**
     * 创建成功结果
     */
    public static BatchOperationResult success(int count) {
        return BatchOperationResult.builder()
                .successCount(count)
                .failedCount(0)
                .totalCount(count)
                .build();
    }

    /**
     * 添加错误
     */
    public void addError(Integer row, Long id, String message, String data) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(ErrorDetail.builder()
                .row(row)
                .id(id)
                .message(message)
                .data(data)
                .build());
        this.failedCount++;
    }

    /**
     * 是否全部成功
     */
    public boolean isAllSuccess() {
        return failedCount == 0;
    }

    /**
     * 是否有成功
     */
    public boolean hasSuccess() {
        return successCount > 0;
    }
}
