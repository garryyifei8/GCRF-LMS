package com.gcrf.library.circulation.client.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 读者信息DTO - Feign调用返回
 *
 * @author GCRF Team
 * @since 2025-10-28
 */
@Data
public class ReaderDTO {

    /**
     * 读者ID
     */
    private Long id;

    /**
     * 读者证号
     */
    private String readerId;

    /**
     * 姓名
     */
    private String name;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 读者类型 (STUDENT/TEACHER/STAFF/EXTERNAL)
     */
    private String readerType;

    /**
     * 所属院系/部门
     */
    private String department;

    /**
     * 最大借阅数量
     */
    private Integer maxBorrowCount;

    /**
     * 最长借阅天数
     */
    private Integer maxBorrowDays;

    /**
     * 状态 (ACTIVE/SUSPENDED/EXPIRED)
     */
    private String status;

    /**
     * 证件有效期
     */
    private LocalDate expiryDate;
}
