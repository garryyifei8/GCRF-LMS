package com.gcrf.library.reader.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 读者响应DTO
 *
 * @author GCRF Team
 * @since 2025-10-13
 */
@Data
public class ReaderResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
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
     * 性别
     */
    private String gender;

    /**
     * 出生日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    /**
     * 身份证号（脱敏）
     */
    private String idCard;

    /**
     * 联系电话（脱敏）
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 地址
     */
    private String address;

    /**
     * 读者类型
     */
    private String readerType;

    /**
     * 学号/工号
     */
    private String studentOrEmployeeId;

    /**
     * 所属院系/部门
     */
    private String department;

    /**
     * 专业/职位
     */
    private String majorOrPosition;

    /**
     * 入学/入职年份
     */
    private Integer enrollmentOrEmploymentYear;

    /**
     * 借书卡状态
     */
    private String cardStatus;

    /**
     * 开卡时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate cardIssueDate;

    /**
     * 有效期至
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate cardExpiryDate;

    /**
     * 押金金额（分）
     */
    private Integer depositAmount;

    /**
     * 押金状态
     */
    private String depositStatus;

    /**
     * 信用积分
     */
    private Integer creditScore;

    /**
     * 当前借阅数量
     */
    private Integer currentBorrowCount;

    /**
     * 最大借阅数量
     */
    private Integer maxBorrowCount;

    /**
     * 累计借阅次数
     */
    private Integer totalBorrowCount;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 版本号
     */
    private Integer version;
}
