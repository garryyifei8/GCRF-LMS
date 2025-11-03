package com.gcrf.library.reader.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 读者实体类
 *
 * @author GCRF Team
 * @since 2025-10-13
 */
@Data
@TableName("readers")
public class Reader implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 读者证号（唯一）
     */
    @TableField("reader_id")
    @NotBlank(message = "读者证号不能为空")
    @Size(max = 20, message = "读者证号长度不能超过20个字符")
    private String readerId;

    /**
     * 姓名
     */
    @TableField("name")
    @NotBlank(message = "姓名不能为空")
    @Size(max = 100, message = "姓名长度不能超过100个字符")
    private String name;

    /**
     * 身份证号
     */
    @TableField("id_card")
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$",
             message = "身份证号格式不正确")
    private String idCard;

    /**
     * 联系电话
     */
    @TableField("phone")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 邮箱
     */
    @TableField("email")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    /**
     * 读者类型（STUDENT/TEACHER/STAFF/EXTERNAL）
     */
    @TableField("reader_type")
    @NotBlank(message = "读者类型不能为空")
    @Pattern(regexp = "^(STUDENT|TEACHER|STAFF|EXTERNAL)$", message = "读者类型必须为STUDENT、TEACHER、STAFF或EXTERNAL")
    private String readerType;

    /**
     * 所属院系/部门
     */
    @TableField("department")
    @Size(max = 100, message = "院系/部门长度不能超过100个字符")
    private String department;

    /**
     * 学号（学生专用）
     */
    @TableField("student_no")
    @Size(max = 50, message = "学号长度不能超过50个字符")
    private String studentNo;

    /**
     * 工号（教师/职工专用）
     */
    @TableField("employee_no")
    @Size(max = 50, message = "工号长度不能超过50个字符")
    private String employeeNo;

    /**
     * 最大借阅数量
     */
    @TableField("max_borrow_count")
    @NotNull(message = "最大借阅数量不能为空")
    @Min(value = 0, message = "最大借阅数量不能为负数")
    private Integer maxBorrowCount;

    /**
     * 最长借阅天数
     */
    @TableField("max_borrow_days")
    @NotNull(message = "最长借阅天数不能为空")
    @Min(value = 1, message = "最长借阅天数至少为1天")
    private Integer maxBorrowDays;

    /**
     * 状态（ACTIVE/SUSPENDED/EXPIRED）
     */
    @TableField("status")
    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "^(ACTIVE|SUSPENDED|EXPIRED)$", message = "状态必须为ACTIVE、SUSPENDED或EXPIRED")
    private String status;

    /**
     * 证件有效期
     */
    @TableField("expiry_date")
    private LocalDate expiryDate;

    /**
     * 头像URL
     */
    @TableField("avatar_url")
    @Size(max = 500, message = "头像URL长度不能超过500个字符")
    private String avatarUrl;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 删除时间（软删除标记）
     * Note: 不使用@TableLogic，因为使用timestamp而非整数类型
     * 软删除逻辑由Service层手动处理：deleted_at IS NULL表示未删除
     */
    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
