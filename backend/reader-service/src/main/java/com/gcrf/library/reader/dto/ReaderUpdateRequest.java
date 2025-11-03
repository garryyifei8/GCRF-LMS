package com.gcrf.library.reader.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 更新读者请求DTO
 *
 * @author GCRF Team
 * @since 2025-10-13
 */
@Data
public class ReaderUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @NotNull(message = "读者ID不能为空")
    private Long id;

    /**
     * 姓名
     */
    @Size(max = 100, message = "姓名长度不能超过100个字符")
    private String name;

    /**
     * 性别（MALE/FEMALE/OTHER）
     */
    @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "性别必须为MALE、FEMALE或OTHER")
    private String gender;

    /**
     * 出生日期
     */
    @Past(message = "出生日期必须是过去的日期")
    private LocalDate birthDate;

    /**
     * 联系电话
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    /**
     * 地址
     */
    @Size(max = 500, message = "地址长度不能超过500个字符")
    private String address;

    /**
     * 所属院系/部门
     */
    @Size(max = 200, message = "院系/部门长度不能超过200个字符")
    private String department;

    /**
     * 专业/职位
     */
    @Size(max = 200, message = "专业/职位长度不能超过200个字符")
    private String majorOrPosition;

    /**
     * 头像URL
     */
    @Size(max = 500, message = "头像URL长度不能超过500个字符")
    private String avatarUrl;

    /**
     * 备注
     */
    @Size(max = 1000, message = "备注长度不能超过1000个字符")
    private String remarks;

    /**
     * 版本号（乐观锁）
     */
    @NotNull(message = "版本号不能为空")
    private Integer version;
}
