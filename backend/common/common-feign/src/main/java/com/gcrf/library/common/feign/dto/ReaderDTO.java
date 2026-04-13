package com.gcrf.library.common.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 读者DTO - 用于服务间调用
 *
 * @author GCRF Team
 * @since 2025-12-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReaderDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 读者证号（唯一）
     */
    private String readerId;

    /**
     * 姓名
     */
    private String name;

    /**
     * 身份证号
     */
    private String idCard;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 读者类型（STUDENT/TEACHER/STAFF/EXTERNAL）
     */
    private String readerType;

    /**
     * 所属院系/部门
     */
    private String department;

    /**
     * 学号（学生专用）
     */
    private String studentNo;

    /**
     * 工号（教师/职工专用）
     */
    private String employeeNo;

    /**
     * 最大借阅数量
     */
    private Integer maxBorrowCount;

    /**
     * 最长借阅天数
     */
    private Integer maxBorrowDays;

    /**
     * 状态（ACTIVE/SUSPENDED/EXPIRED）
     */
    private String status;

    /**
     * 证件有效期
     */
    private LocalDate expiryDate;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
