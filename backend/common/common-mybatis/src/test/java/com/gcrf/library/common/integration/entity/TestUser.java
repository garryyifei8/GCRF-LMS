package com.gcrf.library.common.integration.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gcrf.library.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 测试用户实体
 *
 * @author Claude Code
 * @date 2025-10-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("test_user")
public class TestUser extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 年龄
     */
    private Integer age;
}
