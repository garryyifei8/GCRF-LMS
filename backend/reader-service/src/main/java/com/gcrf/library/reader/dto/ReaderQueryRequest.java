package com.gcrf.library.reader.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 读者查询请求DTO
 *
 * @author GCRF Team
 * @since 2025-10-13
 */
@Data
public class ReaderQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 读者证号（模糊查询）
     */
    private String readerId;

    /**
     * 姓名（模糊查询）
     */
    private String name;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 身份证号
     */
    private String idCard;

    /**
     * 读者类型（STUDENT/TEACHER/STAFF/PUBLIC）
     */
    private String readerType;

    /**
     * 借书卡状态（PENDING/ACTIVE/SUSPENDED/EXPIRED/CANCELLED）
     */
    private String cardStatus;

    /**
     * 所属院系/部门（模糊查询）
     */
    private String department;

    /**
     * 学号/工号
     */
    private String studentOrEmployeeId;

    /**
     * 当前页码（默认1）
     */
    private Integer pageNum = 1;

    /**
     * 每页数量（默认10）
     */
    private Integer pageSize = 10;
}
