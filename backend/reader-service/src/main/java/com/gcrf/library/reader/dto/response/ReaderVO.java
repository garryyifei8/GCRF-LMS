package com.gcrf.library.reader.dto.response;

import com.gcrf.library.reader.entity.Reader;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 读者响应VO - 列表页展示
 *
 * @author GCRF Team
 * @since 2025-10-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReaderVO {

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
     * 状态（ACTIVE/SUSPENDED/EXPIRED）
     */
    private String status;

    /**
     * 证件有效期
     */
    private LocalDate expiryDate;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 从实体转换
     */
    public static ReaderVO from(Reader reader) {
        if (reader == null) {
            return null;
        }
        return ReaderVO.builder()
                .id(reader.getId())
                .readerId(reader.getReaderId())
                .name(reader.getName())
                .phone(desensitizePhone(reader.getPhone()))
                .email(reader.getEmail())
                .readerType(reader.getReaderType())
                .department(reader.getDepartment())
                .studentNo(reader.getStudentNo())
                .employeeNo(reader.getEmployeeNo())
                .maxBorrowCount(reader.getMaxBorrowCount())
                .status(reader.getStatus())
                .expiryDate(reader.getExpiryDate())
                .createdAt(reader.getCreatedAt())
                .build();
    }

    /**
     * 手机号脱敏
     */
    private static String desensitizePhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
