package com.gcrf.library.book.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 盘点范围枚举
 *
 * @author GCRF Team
 * @date 2025-12-20
 */
@Getter
@AllArgsConstructor
public enum TaskScope {

    /**
     * 全部
     */
    ALL("ALL", "全部"),

    /**
     * 按位置
     */
    LOCATION("LOCATION", "按位置"),

    /**
     * 按分类
     */
    CATEGORY("CATEGORY", "按分类");

    @EnumValue
    @JsonValue
    private final String code;

    private final String description;

    /**
     * 根据code获取枚举
     */
    public static TaskScope fromCode(String code) {
        for (TaskScope scope : values()) {
            if (scope.getCode().equals(code)) {
                return scope;
            }
        }
        throw new IllegalArgumentException("Unknown TaskScope code: " + code);
    }
}
