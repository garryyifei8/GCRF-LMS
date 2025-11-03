package com.gcrf.library.reader.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 创建读者请求DTO
 *
 * @author GCRF Team
 * @since 2025-10-13
 */
@Data
public class ReaderCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 读者证号（唯一）
     */
    @NotBlank(message = "读者证号不能为空")
    @Size(max = 50, message = "读者证号长度不能超过50个字符")
    private String readerId;

    /**
     * 姓名
     */
    @NotBlank(message = "姓名不能为空")
    @Size(max = 100, message = "姓名长度不能超过100个字符")
    private String name;

    /**
     * 性别（MALE/FEMALE/OTHER）
     */
    @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "性别必须为MALE、FEMALE或OTHER")
    private String gender;

    /**
     * 身份证号
     */
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$",
             message = "身份证号格式不正确")
    private String idCard;

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
     * 读者类型（STUDENT/TEACHER/STAFF/PUBLIC）
     */
    @NotBlank(message = "读者类型不能为空")
    @Pattern(regexp = "^(STUDENT|TEACHER|STAFF|PUBLIC)$", message = "读者类型必须为STUDENT、TEACHER、STAFF或PUBLIC")
    private String readerType;

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
     * 押金金额（分）
     */
    @Min(value = 0, message = "押金金额不能为负数")
    private Integer depositAmount;

    /**
     * 最大借阅数量
     */
    @Min(value = 1, message = "最大借阅数量至少为1")
    @Max(value = 100, message = "最大借阅数量不能超过100")
    private Integer maxBorrowQuantity;

    /**
     * 备注
     */
    @Size(max = 1000, message = "备注长度不能超过1000个字符")
    private String remarks;
}
