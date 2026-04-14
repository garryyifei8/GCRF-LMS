package com.gcrf.library.book.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 盘点任务状态枚举
 *
 * @author GCRF Team
 * @date 2025-12-20
 */
@Getter
@AllArgsConstructor
public enum TaskStatus {

    /**
     * 待执行
     */
    PENDING("PENDING", "待执行"),

    /**
     * 进行中
     */
    IN_PROGRESS("IN_PROGRESS", "进行中"),

    /**
     * 已完成
     */
    COMPLETED("COMPLETED", "已完成"),

    /**
     * 已取消
     */
    CANCELLED("CANCELLED", "已取消");

    @EnumValue
    @JsonValue
    private final String code;

    private final String description;

    /**
     * 根据code获取枚举
     */
    public static TaskStatus fromCode(String code) {
        for (TaskStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown TaskStatus code: " + code);
    }

    /**
     * 是否可以开始
     */
    public boolean canStart() {
        return this == PENDING;
    }

    /**
     * 是否可以完成
     */
    public boolean canComplete() {
        return this == IN_PROGRESS;
    }

    /**
     * 是否可以取消
     */
    public boolean canCancel() {
        return this == PENDING || this == IN_PROGRESS;
    }

    /**
     * 是否可以编辑
     */
    public boolean canEdit() {
        return this == PENDING;
    }
}
