package com.gcrf.library.book.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 盘点任务类型枚举
 *
 * @author GCRF Team
 * @date 2025-12-20
 */
@Getter
@AllArgsConstructor
public enum TaskType {

    /**
     * 全面盘点
     */
    FULL("FULL", "全面盘点"),

    /**
     * 部分盘点
     */
    PARTIAL("PARTIAL", "部分盘点"),

    /**
     * 抽查盘点
     */
    SPOT("SPOT", "抽查盘点");

    @EnumValue
    @JsonValue
    private final String code;

    private final String description;

    /**
     * 根据code获取枚举
     */
    public static TaskType fromCode(String code) {
        for (TaskType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown TaskType code: " + code);
    }
}
