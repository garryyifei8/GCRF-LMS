package com.gcrf.library.system.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 部门响应DTO
 *
 * @author GCRF Team
 * @since 2025-10-14
 */
@Data
public class DepartmentResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String deptCode;
    private String deptName;
    private Long parentId;
    private Integer deptLevel;
    private String deptPath;
    private Long leaderId;
    private String leaderName;
    private String phone;
    private String email;
    private Integer sortOrder;
    private String status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
