package com.gcrf.library.book.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 盘点明细状态枚举
 *
 * @author GCRF Team
 * @date 2025-12-20
 */
@Getter
@AllArgsConstructor
public enum TaskItemStatus {

    /**
     * 待盘点
     */
    PENDING("PENDING", "待盘点"),

    /**
     * 已盘点
     */
    CHECKED("CHECKED", "已盘点"),

    /**
     * 已跳过
     */
    SKIPPED("SKIPPED", "已跳过");

    @EnumValue
    @JsonValue
    private final String code;

    private final String description;

    /**
     * 根据code获取枚举
     */
    public static TaskItemStatus fromCode(String code) {
        for (TaskItemStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown TaskItemStatus code: " + code);
    }

    /**
     * 是否可以录入结果
     */
    public boolean canCheck() {
        return this == PENDING;
    }
}
