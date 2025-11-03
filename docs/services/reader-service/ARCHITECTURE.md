# Reader Service Architecture Document
# 读者服务架构设计文档

**Version**: 1.0.0
**Date**: 2025-10-28
**Author**: GCRF Backend Architecture Team
**Stage**: Stage 9 - Reader Service Implementation

---

## 1. Service Overview

The Reader Service (读者服务) is a core microservice in the GCRF Library Management System responsible for managing reader information, reader cards, and borrowing privileges. This service follows the established patterns from the Book Service (Stage 8) and integrates with the Common modules.

### 1.1 Key Responsibilities
- Reader registration and profile management
- Reader card lifecycle (issue, activate, suspend, expire, cancel)
- Borrowing privileges and limits management
- Credit score tracking
- Reader status management
- Integration with circulation service for borrowing validation

### 1.2 Technology Stack
- **Framework**: Spring Boot 3.2.2
- **ORM**: MyBatis-Plus 3.5.9
- **Database**: PostgreSQL 15+
- **Cache**: Redis 7.x
- **Validation**: Jakarta Bean Validation
- **Documentation**: Swagger/OpenAPI 3.0

---

## 2. Package Structure

```
reader-service/
├── src/main/java/com/gcrf/library/reader/
│   ├── ReaderServiceApplication.java         # Spring Boot Application Entry Point
│   ├── controller/
│   │   └── ReaderController.java            # RESTful API Endpoints
│   ├── service/
│   │   ├── ReaderService.java              # Service Interface
│   │   └── impl/
│   │       └── ReaderServiceImpl.java      # Service Implementation
│   ├── mapper/
│   │   └── ReaderMapper.java               # MyBatis-Plus Mapper
│   ├── domain/
│   │   ├── entity/
│   │   │   └── Reader.java                 # Reader Entity (Database Model)
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   │   ├── ReaderCreateRequest.java    # Creation Request DTO
│   │   │   │   ├── ReaderUpdateRequest.java    # Update Request DTO
│   │   │   │   └── ReaderQueryRequest.java     # Query Request DTO
│   │   │   └── response/
│   │   │       ├── ReaderVO.java              # List View VO
│   │   │       └── ReaderDetailVO.java        # Detail View VO
│   │   └── enums/
│   │       ├── ReaderType.java                # Reader Types Enum
│   │       ├── ReaderStatus.java              # Reader Status Enum
│   │       └── Gender.java                    # Gender Enum
│   ├── config/
│   │   └── ReaderServiceConfig.java           # Service Configuration
│   └── constant/
│       └── ReaderConstants.java               # Service Constants
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   └── application-prod.yml
└── src/test/java/
```

---

## 3. Entity Design

### 3.1 Reader Entity (完整版本)

```java
package com.gcrf.library.reader.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 读者实体类
 *
 * @TableName readers
 * @author GCRF Team
 * @date 2025-10-28
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("readers")
public class Reader implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID (自增)
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 读者证号 (业务ID, 格式: YYYYMMDD+4位序号)
     * 示例: 202510280001
     */
    @TableField("reader_id")
    private String readerId;

    /**
     * 姓名
     */
    @TableField("name")
    private String name;

    /**
     * 性别: MALE/FEMALE/OTHER
     */
    @TableField("gender")
    private String gender;

    /**
     * 身份证号 (18位)
     */
    @TableField("id_card")
    private String idCard;

    /**
     * 手机号码 (11位)
     */
    @TableField("phone")
    private String phone;

    /**
     * 电子邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 读者类型: STUDENT-学生, TEACHER-教师, STAFF-职工, EXTERNAL-校外人员
     */
    @TableField("reader_type")
    private String readerType;

    /**
     * 所属院系/部门
     */
    @TableField("department")
    private String department;

    /**
     * 专业 (学生专用)
     */
    @TableField("major")
    private String major;

    /**
     * 年级 (学生专用)
     */
    @TableField("grade")
    private String grade;

    /**
     * 班级 (学生专用)
     */
    @TableField("class_name")
    private String className;

    /**
     * 读者状态: ACTIVE-正常, SUSPENDED-挂失/暂停, EXPIRED-过期, CANCELLED-注销
     */
    @TableField("status")
    private String status;

    /**
     * 最大借阅数量 (根据读者类型设定)
     * STUDENT: 10本, TEACHER: 20本, STAFF: 15本, EXTERNAL: 5本
     */
    @TableField("max_borrow_count")
    private Integer maxBorrowCount;

    /**
     * 最长借阅天数 (根据读者类型设定)
     * STUDENT: 30天, TEACHER: 90天, STAFF: 60天, EXTERNAL: 15天
     */
    @TableField("max_borrow_days")
    private Integer maxBorrowDays;

    /**
     * 当前借阅数量
     */
    @TableField("current_borrow_count")
    private Integer currentBorrowCount;

    /**
     * 累计借阅次数
     */
    @TableField("total_borrow_count")
    private Integer totalBorrowCount;

    /**
     * 逾期次数
     */
    @TableField("overdue_count")
    private Integer overdueCount;

    /**
     * 信用积分 (默认100分, 最低0分, 最高150分)
     */
    @TableField("credit_score")
    private Integer creditScore;

    /**
     * 押金金额 (单位: 分)
     */
    @TableField("deposit_amount")
    private Integer depositAmount;

    /**
     * 账户余额 (单位: 分, 用于罚款支付等)
     */
    @TableField("account_balance")
    private Integer accountBalance;

    /**
     * 照片URL
     */
    @TableField("photo_url")
    private String photoUrl;

    /**
     * 人脸特征向量 (JSON格式存储)
     */
    @TableField("face_features")
    private String faceFeatures;

    /**
     * 发卡日期
     */
    @TableField("issue_date")
    private LocalDate issueDate;

    /**
     * 到期日期
     */
    @TableField("expiry_date")
    private LocalDate expiryDate;

    /**
     * 备注信息
     */
    @TableField("remarks")
    private String remarks;

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
     * 删除时间 (软删除)
     */
    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
```

### 3.2 Enums

```java
// ReaderType.java
package com.gcrf.library.reader.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReaderType {
    STUDENT("STUDENT", "学生", 10, 30),
    TEACHER("TEACHER", "教师", 20, 90),
    STAFF("STAFF", "职工", 15, 60),
    EXTERNAL("EXTERNAL", "校外人员", 5, 15);

    private final String code;
    private final String description;
    private final Integer maxBorrowCount;
    private final Integer maxBorrowDays;
}

// ReaderStatus.java
package com.gcrf.library.reader.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReaderStatus {
    ACTIVE("ACTIVE", "正常"),
    SUSPENDED("SUSPENDED", "挂失/暂停"),
    EXPIRED("EXPIRED", "已过期"),
    CANCELLED("CANCELLED", "已注销");

    private final String code;
    private final String description;
}
```

---

## 4. Service Interface Design

### 4.1 ReaderService Interface

```java
package com.gcrf.library.reader.service;

import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.reader.domain.dto.request.ReaderCreateRequest;
import com.gcrf.library.reader.domain.dto.request.ReaderUpdateRequest;
import com.gcrf.library.reader.domain.dto.request.ReaderQueryRequest;
import com.gcrf.library.reader.domain.dto.response.ReaderVO;
import com.gcrf.library.reader.domain.dto.response.ReaderDetailVO;

/**
 * 读者服务接口
 *
 * @author GCRF Team
 * @date 2025-10-28
 */
public interface ReaderService {

    /**
     * 分页查询读者列表
     *
     * @param request 查询请求参数
     * @return 分页结果 (使用records字段)
     */
    PageResult<ReaderVO> queryReaders(ReaderQueryRequest request);

    /**
     * 根据ID获取读者详情
     *
     * @param id 读者ID
     * @return 读者详细信息
     */
    ReaderDetailVO getReaderById(Long id);

    /**
     * 根据读者证号获取读者详情
     *
     * @param readerId 读者证号
     * @return 读者详细信息
     */
    ReaderDetailVO getReaderByReaderId(String readerId);

    /**
     * 创建新读者
     *
     * @param request 创建请求
     * @return 创建后的读者详情
     */
    ReaderDetailVO createReader(ReaderCreateRequest request);

    /**
     * 更新读者信息
     *
     * @param request 更新请求
     * @return 更新后的读者详情
     */
    ReaderDetailVO updateReader(ReaderUpdateRequest request);

    /**
     * 删除读者 (软删除)
     *
     * @param id 读者ID
     */
    void deleteReader(Long id);

    /**
     * 挂失读者证 (暂停借阅权限)
     *
     * @param id 读者ID
     * @return 更新后的读者详情
     */
    ReaderDetailVO suspendReader(Long id);

    /**
     * 激活读者证 (恢复借阅权限)
     *
     * @param id 读者ID
     * @return 更新后的读者详情
     */
    ReaderDetailVO activateReader(Long id);

    /**
     * 延期读者证有效期
     *
     * @param id 读者ID
     * @param months 延期月数
     * @return 更新后的读者详情
     */
    ReaderDetailVO extendExpiry(Long id, Integer months);

    /**
     * 注销读者证
     *
     * @param id 读者ID
     * @return 更新后的读者详情
     */
    ReaderDetailVO cancelReader(Long id);

    /**
     * 更新信用积分
     *
     * @param id 读者ID
     * @param scoreChange 积分变化值 (正数加分, 负数扣分)
     * @param reason 变更原因
     */
    void updateCreditScore(Long id, Integer scoreChange, String reason);

    /**
     * 增加当前借阅数量 (内部调用)
     *
     * @param readerId 读者证号
     */
    void incrementBorrowCount(String readerId);

    /**
     * 减少当前借阅数量 (内部调用)
     *
     * @param readerId 读者证号
     */
    void decrementBorrowCount(String readerId);
}
```

---

## 5. DTO/VO Design

### 5.1 Request DTOs

```java
// ReaderCreateRequest.java
package com.gcrf.library.reader.domain.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

/**
 * 读者创建请求
 */
@Data
public class ReaderCreateRequest {

    @NotBlank(message = "姓名不能为空")
    @Size(min = 2, max = 50, message = "姓名长度必须在2-50个字符之间")
    private String name;

    @NotBlank(message = "性别不能为空")
    @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "性别必须是MALE、FEMALE或OTHER")
    private String gender;

    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}(\\d|X)$",
             message = "身份证号格式不正确")
    private String idCard;

    @NotBlank(message = "手机号码不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号码格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "读者类型不能为空")
    @Pattern(regexp = "^(STUDENT|TEACHER|STAFF|EXTERNAL)$",
             message = "读者类型必须是STUDENT、TEACHER、STAFF或EXTERNAL")
    private String readerType;

    @Size(max = 100, message = "院系/部门名称不能超过100个字符")
    private String department;

    @Size(max = 100, message = "专业名称不能超过100个字符")
    private String major;

    @Size(max = 20, message = "年级不能超过20个字符")
    private String grade;

    @Size(max = 50, message = "班级名称不能超过50个字符")
    private String className;

    @Min(value = 0, message = "押金金额不能为负数")
    private Integer depositAmount;

    private String photoUrl;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remarks;

    @Future(message = "到期日期必须是将来的日期")
    private LocalDate expiryDate;
}

// ReaderUpdateRequest.java
package com.gcrf.library.reader.domain.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 读者更新请求
 */
@Data
public class ReaderUpdateRequest {

    @NotNull(message = "读者ID不能为空")
    private Long id;

    @Size(min = 2, max = 50, message = "姓名长度必须在2-50个字符之间")
    private String name;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号码格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Size(max = 100, message = "院系/部门名称不能超过100个字符")
    private String department;

    @Size(max = 100, message = "专业名称不能超过100个字符")
    private String major;

    @Size(max = 20, message = "年级不能超过20个字符")
    private String grade;

    @Size(max = 50, message = "班级名称不能超过50个字符")
    private String className;

    private String photoUrl;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remarks;
}

// ReaderQueryRequest.java
package com.gcrf.library.reader.domain.dto.request;

import com.gcrf.library.common.dto.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 读者查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReaderQueryRequest extends PageRequest {

    /**
     * 关键字搜索 (姓名/读者证号/手机号)
     */
    private String keyword;

    /**
     * 读者类型
     */
    private String readerType;

    /**
     * 读者状态
     */
    private String status;

    /**
     * 院系/部门
     */
    private String department;

    /**
     * 是否有逾期记录
     */
    private Boolean hasOverdue;

    /**
     * 信用分范围-最小值
     */
    private Integer minCreditScore;

    /**
     * 信用分范围-最大值
     */
    private Integer maxCreditScore;
}
```

### 5.2 Response VOs

```java
// ReaderVO.java
package com.gcrf.library.reader.domain.dto.response;

import com.gcrf.library.reader.domain.entity.Reader;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 读者列表响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReaderVO {

    private Long id;
    private String readerId;
    private String name;
    private String gender;
    private String phone;
    private String email;
    private String readerType;
    private String readerTypeDesc;
    private String department;
    private String status;
    private String statusDesc;
    private Integer currentBorrowCount;
    private Integer maxBorrowCount;
    private Integer creditScore;
    private LocalDate expiryDate;
    private LocalDateTime createdAt;

    /**
     * 从实体转换为VO
     */
    public static ReaderVO from(Reader reader) {
        if (reader == null) {
            return null;
        }

        return ReaderVO.builder()
                .id(reader.getId())
                .readerId(reader.getReaderId())
                .name(reader.getName())
                .gender(reader.getGender())
                .phone(reader.getPhone())
                .email(reader.getEmail())
                .readerType(reader.getReaderType())
                .readerTypeDesc(getReaderTypeDesc(reader.getReaderType()))
                .department(reader.getDepartment())
                .status(reader.getStatus())
                .statusDesc(getStatusDesc(reader.getStatus()))
                .currentBorrowCount(reader.getCurrentBorrowCount())
                .maxBorrowCount(reader.getMaxBorrowCount())
                .creditScore(reader.getCreditScore())
                .expiryDate(reader.getExpiryDate())
                .createdAt(reader.getCreatedAt())
                .build();
    }

    private static String getReaderTypeDesc(String type) {
        return switch (type) {
            case "STUDENT" -> "学生";
            case "TEACHER" -> "教师";
            case "STAFF" -> "职工";
            case "EXTERNAL" -> "校外人员";
            default -> type;
        };
    }

    private static String getStatusDesc(String status) {
        return switch (status) {
            case "ACTIVE" -> "正常";
            case "SUSPENDED" -> "挂失/暂停";
            case "EXPIRED" -> "已过期";
            case "CANCELLED" -> "已注销";
            default -> status;
        };
    }
}

// ReaderDetailVO.java
package com.gcrf.library.reader.domain.dto.response;

import com.gcrf.library.reader.domain.entity.Reader;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 读者详情响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReaderDetailVO {

    private Long id;
    private String readerId;
    private String name;
    private String gender;
    private String genderDesc;
    private String idCard;
    private String phone;
    private String email;
    private String readerType;
    private String readerTypeDesc;
    private String department;
    private String major;
    private String grade;
    private String className;
    private String status;
    private String statusDesc;
    private Integer maxBorrowCount;
    private Integer maxBorrowDays;
    private Integer currentBorrowCount;
    private Integer totalBorrowCount;
    private Integer overdueCount;
    private Integer creditScore;
    private BigDecimal depositAmount;
    private BigDecimal accountBalance;
    private String photoUrl;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 从实体转换为详情VO
     */
    public static ReaderDetailVO from(Reader reader) {
        if (reader == null) {
            return null;
        }

        return ReaderDetailVO.builder()
                .id(reader.getId())
                .readerId(reader.getReaderId())
                .name(reader.getName())
                .gender(reader.getGender())
                .genderDesc(getGenderDesc(reader.getGender()))
                .idCard(maskIdCard(reader.getIdCard()))
                .phone(reader.getPhone())
                .email(reader.getEmail())
                .readerType(reader.getReaderType())
                .readerTypeDesc(getReaderTypeDesc(reader.getReaderType()))
                .department(reader.getDepartment())
                .major(reader.getMajor())
                .grade(reader.getGrade())
                .className(reader.getClassName())
                .status(reader.getStatus())
                .statusDesc(getStatusDesc(reader.getStatus()))
                .maxBorrowCount(reader.getMaxBorrowCount())
                .maxBorrowDays(reader.getMaxBorrowDays())
                .currentBorrowCount(reader.getCurrentBorrowCount())
                .totalBorrowCount(reader.getTotalBorrowCount())
                .overdueCount(reader.getOverdueCount())
                .creditScore(reader.getCreditScore())
                .depositAmount(convertToBigDecimal(reader.getDepositAmount()))
                .accountBalance(convertToBigDecimal(reader.getAccountBalance()))
                .photoUrl(reader.getPhotoUrl())
                .issueDate(reader.getIssueDate())
                .expiryDate(reader.getExpiryDate())
                .remarks(reader.getRemarks())
                .createdAt(reader.getCreatedAt())
                .updatedAt(reader.getUpdatedAt())
                .build();
    }

    private static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 18) {
            return idCard;
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(14);
    }

    private static BigDecimal convertToBigDecimal(Integer amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(100));
    }

    private static String getGenderDesc(String gender) {
        return switch (gender) {
            case "MALE" -> "男";
            case "FEMALE" -> "女";
            case "OTHER" -> "其他";
            default -> gender;
        };
    }

    private static String getReaderTypeDesc(String type) {
        return switch (type) {
            case "STUDENT" -> "学生";
            case "TEACHER" -> "教师";
            case "STAFF" -> "职工";
            case "EXTERNAL" -> "校外人员";
            default -> type;
        };
    }

    private static String getStatusDesc(String status) {
        return switch (status) {
            case "ACTIVE" -> "正常";
            case "SUSPENDED" -> "挂失/暂停";
            case "EXPIRED" -> "已过期";
            case "CANCELLED" -> "已注销";
            default -> status;
        };
    }
}
```

---

## 6. Controller Design

```java
package com.gcrf.library.reader.controller;

import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.common.result.Result;
import com.gcrf.library.reader.domain.dto.request.ReaderCreateRequest;
import com.gcrf.library.reader.domain.dto.request.ReaderUpdateRequest;
import com.gcrf.library.reader.domain.dto.request.ReaderQueryRequest;
import com.gcrf.library.reader.domain.dto.response.ReaderVO;
import com.gcrf.library.reader.domain.dto.response.ReaderDetailVO;
import com.gcrf.library.reader.service.ReaderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 读者管理控制器
 *
 * @author GCRF Team
 * @date 2025-10-28
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/readers")
@RequiredArgsConstructor
@Tag(name = "读者管理", description = "读者信息管理、读者证管理等接口")
public class ReaderController {

    private final ReaderService readerService;

    /**
     * 分页查询读者列表
     *
     * ⚠️ 重要: 返回的PageResult使用records字段, 不是list字段
     */
    @GetMapping
    @Operation(summary = "分页查询读者", description = "支持关键词搜索、类型筛选、状态筛选等")
    public Result<PageResult<ReaderVO>> queryReaders(@Valid ReaderQueryRequest request) {
        log.info("分页查询读者请求: {}", request);
        PageResult<ReaderVO> result = readerService.queryReaders(request);
        return Result.success(result);
    }

    /**
     * 根据ID查询读者详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询读者详情", description = "根据读者ID查询详细信息")
    public Result<ReaderDetailVO> getReaderById(@PathVariable Long id) {
        log.info("查询读者详情: id={}", id);
        ReaderDetailVO reader = readerService.getReaderById(id);
        return Result.success(reader);
    }

    /**
     * 根据读者证号查询读者详情
     */
    @GetMapping("/reader-id/{readerId}")
    @Operation(summary = "根据读者证号查询", description = "根据读者证号查询详细信息")
    public Result<ReaderDetailVO> getReaderByReaderId(@PathVariable String readerId) {
        log.info("根据读者证号查询: readerId={}", readerId);
        ReaderDetailVO reader = readerService.getReaderByReaderId(readerId);
        return Result.success(reader);
    }

    /**
     * 创建新读者
     */
    @PostMapping
    @Operation(summary = "创建读者", description = "新增读者信息并生成读者证")
    public Result<ReaderDetailVO> createReader(@Valid @RequestBody ReaderCreateRequest request) {
        log.info("创建读者: {}", request.getName());
        ReaderDetailVO reader = readerService.createReader(request);
        return Result.success(reader);
    }

    /**
     * 更新读者信息
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新读者信息", description = "修改读者基本信息")
    public Result<ReaderDetailVO> updateReader(
            @PathVariable Long id,
            @Valid @RequestBody ReaderUpdateRequest request) {
        log.info("更新读者信息: id={}", id);
        request.setId(id); // 确保路径参数和请求体ID一致
        ReaderDetailVO reader = readerService.updateReader(request);
        return Result.success(reader);
    }

    /**
     * 删除读者 (软删除)
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除读者", description = "删除读者信息（软删除）")
    public Result<Void> deleteReader(@PathVariable Long id) {
        log.info("删除读者: id={}", id);
        readerService.deleteReader(id);
        return Result.success();
    }

    /**
     * 挂失读者证
     */
    @PutMapping("/{id}/suspend")
    @Operation(summary = "挂失读者证", description = "挂失读者证，暂停借阅权限")
    public Result<ReaderDetailVO> suspendReader(@PathVariable Long id) {
        log.info("挂失读者证: id={}", id);
        ReaderDetailVO reader = readerService.suspendReader(id);
        return Result.success(reader);
    }

    /**
     * 激活读者证
     */
    @PutMapping("/{id}/activate")
    @Operation(summary = "激活读者证", description = "激活读者证，恢复借阅权限")
    public Result<ReaderDetailVO> activateReader(@PathVariable Long id) {
        log.info("激活读者证: id={}", id);
        ReaderDetailVO reader = readerService.activateReader(id);
        return Result.success(reader);
    }

    /**
     * 延期读者证
     */
    @PutMapping("/{id}/extend")
    @Operation(summary = "延期读者证", description = "延长读者证有效期")
    public Result<ReaderDetailVO> extendExpiry(
            @PathVariable Long id,
            @RequestParam @Parameter(description = "延期月数") Integer months) {
        log.info("延期读者证: id={}, months={}", id, months);
        ReaderDetailVO reader = readerService.extendExpiry(id, months);
        return Result.success(reader);
    }

    /**
     * 注销读者证
     */
    @PutMapping("/{id}/cancel")
    @Operation(summary = "注销读者证", description = "注销读者证，永久停用")
    public Result<ReaderDetailVO> cancelReader(@PathVariable Long id) {
        log.info("注销读者证: id={}", id);
        ReaderDetailVO reader = readerService.cancelReader(id);
        return Result.success(reader);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查读者服务是否正常运行")
    public Result<String> health() {
        return Result.success("Reader Service is running");
    }
}
```

---

## 7. Business Logic Requirements

### 7.1 Reader ID Generation
```java
/**
 * 生成读者证号
 * 格式: YYYYMMDD + 4位序号
 * 示例: 202510280001
 */
private String generateReaderId() {
    String prefix = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

    // 获取今日最大序号
    String maxReaderId = readerMapper.selectMaxReaderIdByPrefix(prefix);

    int sequence = 1;
    if (maxReaderId != null) {
        String sequenceStr = maxReaderId.substring(8);
        sequence = Integer.parseInt(sequenceStr) + 1;
    }

    return prefix + String.format("%04d", sequence);
}
```

### 7.2 Validation Rules

1. **身份证号验证**
   - 必须是18位
   - 校验码验证
   - 唯一性检查（防止重复注册）

2. **手机号验证**
   - 11位数字
   - 以1开头，第二位为3-9

3. **邮箱验证**
   - 标准邮箱格式
   - 可选字段

4. **读者类型限制**
   - 根据类型自动设置借阅权限
   - STUDENT: 10本/30天
   - TEACHER: 20本/90天
   - STAFF: 15本/60天
   - EXTERNAL: 5本/15天

5. **到期日期**
   - 默认1年有效期
   - 可手动设置
   - 必须是将来日期

### 7.3 Status Management

```java
/**
 * 读者状态转换规则
 */
ACTIVE -> SUSPENDED    // 挂失
SUSPENDED -> ACTIVE    // 解挂
ACTIVE -> EXPIRED      // 自动过期（定时任务）
EXPIRED -> ACTIVE      // 续期后激活
ANY -> CANCELLED       // 注销（不可逆）
```

### 7.4 Credit Score Rules

```java
/**
 * 信用积分规则
 * 初始: 100分
 * 范围: 0-150分
 */
// 加分项
按时归还: +1分
推荐图书被采纳: +5分
参与图书馆活动: +3分

// 扣分项
逾期归还: -5分/次
图书损坏: -10分
图书遗失: -20分
违规行为: -10分

// 权限限制
< 60分: 暂停借阅权限
< 30分: 需要增加押金
```

---

## 8. Service Implementation Pattern

```java
package com.gcrf.library.reader.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.PageResult;
import com.gcrf.library.reader.domain.dto.request.*;
import com.gcrf.library.reader.domain.dto.response.*;
import com.gcrf.library.reader.domain.entity.Reader;
import com.gcrf.library.reader.domain.enums.ReaderStatus;
import com.gcrf.library.reader.domain.enums.ReaderType;
import com.gcrf.library.reader.mapper.ReaderMapper;
import com.gcrf.library.reader.service.ReaderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 读者服务实现
 *
 * @author GCRF Team
 * @date 2025-10-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReaderServiceImpl implements ReaderService {

    private final ReaderMapper readerMapper;

    @Override
    public PageResult<ReaderVO> queryReaders(ReaderQueryRequest request) {
        log.info("查询读者列表: {}", request);

        // 构建查询条件
        LambdaQueryWrapper<Reader> wrapper = new LambdaQueryWrapper<>();

        // 软删除过滤
        wrapper.isNull(Reader::getDeletedAt);

        // 关键字搜索
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w
                .like(Reader::getName, request.getKeyword())
                .or().like(Reader::getReaderId, request.getKeyword())
                .or().like(Reader::getPhone, request.getKeyword())
            );
        }

        // 类型筛选
        if (StringUtils.hasText(request.getReaderType())) {
            wrapper.eq(Reader::getReaderType, request.getReaderType());
        }

        // 状态筛选
        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(Reader::getStatus, request.getStatus());
        }

        // 部门筛选
        if (StringUtils.hasText(request.getDepartment())) {
            wrapper.like(Reader::getDepartment, request.getDepartment());
        }

        // 排序
        wrapper.orderByDesc(Reader::getCreatedAt);

        // 分页查询
        Page<Reader> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<Reader> readerPage = readerMapper.selectPage(page, wrapper);

        // 转换为VO
        List<ReaderVO> voList = readerPage.getRecords()
                .stream()
                .map(ReaderVO::from)
                .collect(Collectors.toList());

        // ⚠️ 重要: 使用ofRecords方法返回records字段
        return PageResult.ofRecords(
                readerPage.getTotal(),
                request.getPageNum(),
                request.getPageSize(),
                voList
        );
    }

    @Override
    public ReaderDetailVO getReaderById(Long id) {
        log.info("根据ID查询读者: id={}", id);

        Reader reader = readerMapper.selectById(id);
        if (reader == null || reader.getDeletedAt() != null) {
            throw new BusinessException("读者不存在");
        }

        return ReaderDetailVO.from(reader);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReaderDetailVO createReader(ReaderCreateRequest request) {
        log.info("创建读者: {}", request.getName());

        // 验证身份证唯一性
        LambdaQueryWrapper<Reader> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reader::getIdCard, request.getIdCard())
               .isNull(Reader::getDeletedAt);

        if (readerMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("该身份证号已注册");
        }

        // 构建实体
        Reader reader = new Reader();
        reader.setReaderId(generateReaderId());
        reader.setName(request.getName());
        reader.setGender(request.getGender());
        reader.setIdCard(request.getIdCard());
        reader.setPhone(request.getPhone());
        reader.setEmail(request.getEmail());
        reader.setReaderType(request.getReaderType());
        reader.setDepartment(request.getDepartment());
        reader.setMajor(request.getMajor());
        reader.setGrade(request.getGrade());
        reader.setClassName(request.getClassName());
        reader.setPhotoUrl(request.getPhotoUrl());
        reader.setRemarks(request.getRemarks());

        // 设置借阅权限
        ReaderType type = ReaderType.valueOf(request.getReaderType());
        reader.setMaxBorrowCount(type.getMaxBorrowCount());
        reader.setMaxBorrowDays(type.getMaxBorrowDays());

        // 设置初始值
        reader.setStatus(ReaderStatus.ACTIVE.getCode());
        reader.setCurrentBorrowCount(0);
        reader.setTotalBorrowCount(0);
        reader.setOverdueCount(0);
        reader.setCreditScore(100);
        reader.setDepositAmount(request.getDepositAmount() != null ? request.getDepositAmount() : 0);
        reader.setAccountBalance(0);
        reader.setIssueDate(LocalDate.now());

        // 设置有效期（默认1年）
        LocalDate expiryDate = request.getExpiryDate() != null
                ? request.getExpiryDate()
                : LocalDate.now().plusYears(1);
        reader.setExpiryDate(expiryDate);

        // 保存
        readerMapper.insert(reader);
        log.info("读者创建成功: id={}, readerId={}", reader.getId(), reader.getReaderId());

        return ReaderDetailVO.from(reader);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReader(Long id) {
        log.info("删除读者: id={}", id);

        Reader reader = readerMapper.selectById(id);
        if (reader == null) {
            throw new BusinessException("读者不存在");
        }

        // 检查是否有未归还图书
        if (reader.getCurrentBorrowCount() > 0) {
            throw new BusinessException("该读者有未归还的图书，无法删除");
        }

        // 软删除
        reader.setDeletedAt(LocalDateTime.now());
        reader.setStatus(ReaderStatus.CANCELLED.getCode());
        readerMapper.updateById(reader);

        log.info("读者删除成功: id={}", id);
    }

    /**
     * 生成读者证号
     */
    private String generateReaderId() {
        String prefix = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        // 查询当日最大序号
        LambdaQueryWrapper<Reader> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Reader::getReaderId, prefix)
               .orderByDesc(Reader::getReaderId)
               .last("LIMIT 1");

        Reader maxReader = readerMapper.selectOne(wrapper);

        int sequence = 1;
        if (maxReader != null && maxReader.getReaderId().startsWith(prefix)) {
            String sequenceStr = maxReader.getReaderId().substring(8);
            sequence = Integer.parseInt(sequenceStr) + 1;
        }

        return prefix + String.format("%04d", sequence);
    }

    // ... 其他方法实现
}
```

---

## 9. Critical Implementation Notes

### 9.1 Pagination Format (⚠️ CRITICAL)

**必须使用 `PageResult.ofRecords()` 方法，返回 `records` 字段，不是 `list` 字段！**

```java
// ✅ 正确 - 读者服务使用records
return PageResult.ofRecords(total, pageNum, pageSize, records);

// ❌ 错误 - 不要使用list
return PageResult.of(total, pageNum, pageSize, list);
```

前端期望的响应格式:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 10,
    "records": [...]  // ← 必须是records，不是list
  }
}
```

### 9.2 Exception Handling

```java
// 业务异常
throw new BusinessException("错误消息");

// 系统异常
throw new SystemException("系统错误");

// 参数验证 - 使用Jakarta Bean Validation
@NotBlank(message = "姓名不能为空")
@Size(min = 2, max = 50, message = "姓名长度必须在2-50个字符之间")
```

### 9.3 Transaction Management

```java
@Transactional(rollbackFor = Exception.class)
public ReaderDetailVO createReader(ReaderCreateRequest request) {
    // 所有写操作必须加事务注解
}
```

### 9.4 Soft Delete Pattern

```java
// 查询时过滤已删除
wrapper.isNull(Reader::getDeletedAt);

// 删除时设置删除时间
reader.setDeletedAt(LocalDateTime.now());
```

### 9.5 Audit Fields

```java
// 使用MyBatis-Plus自动填充
@TableField(value = "created_at", fill = FieldFill.INSERT)
private LocalDateTime createdAt;

@TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
private LocalDateTime updatedAt;
```

---

## 10. Testing Strategy

### 10.1 Unit Tests
- Service层业务逻辑测试
- 验证规则测试
- 读者证号生成测试

### 10.2 Integration Tests
- Controller层API测试
- 数据库操作测试
- 事务回滚测试

### 10.3 Key Test Cases
1. 创建读者 - 验证所有必填字段
2. 身份证重复检查
3. 读者证号唯一性
4. 状态转换合法性
5. 借阅权限验证
6. 信用分计算
7. 软删除功能
8. 分页查询格式验证

---

## 11. Performance Considerations

### 11.1 Database Indexes
```sql
CREATE INDEX idx_reader_id ON readers(reader_id);
CREATE INDEX idx_id_card ON readers(id_card);
CREATE INDEX idx_phone ON readers(phone);
CREATE INDEX idx_status ON readers(status);
CREATE INDEX idx_reader_type ON readers(reader_type);
CREATE INDEX idx_created_at ON readers(created_at);
CREATE INDEX idx_deleted_at ON readers(deleted_at);
```

### 11.2 Cache Strategy
```java
// Redis缓存键设计
reader:{id}           // 读者详情缓存 TTL: 30分钟
reader:rid:{readerId} // 按读者证号缓存 TTL: 30分钟
reader:stats:{id}     // 读者统计信息 TTL: 5分钟
```

### 11.3 Query Optimization
- 使用分页避免大数据量查询
- 索引优化提升查询速度
- 避免N+1查询问题
- 使用批量操作减少数据库交互

---

## 12. Security Considerations

### 12.1 Data Protection
- 身份证号脱敏显示
- 敏感信息加密存储
- 操作日志记录

### 12.2 Access Control
- JWT令牌验证
- 角色权限控制
- API访问频率限制

### 12.3 Input Validation
- 所有输入参数验证
- SQL注入防护
- XSS攻击防护

---

## 13. Integration Points

### 13.1 With Circulation Service
- 借阅时验证读者状态
- 更新当前借阅数量
- 逾期记录同步

### 13.2 With Notification Service
- 读者证到期提醒
- 借阅到期通知
- 信用分变更通知

### 13.3 With System Service
- 用户账号关联
- 权限同步
- 操作审计

---

## 14. Deployment Checklist

- [ ] 数据库表创建及索引
- [ ] 初始数据导入
- [ ] 配置文件更新
- [ ] 服务注册到Nacos
- [ ] API网关路由配置
- [ ] 监控告警设置
- [ ] 日志配置
- [ ] 性能测试
- [ ] 安全扫描
- [ ] 文档更新

---

## 15. Future Enhancements

1. **人脸识别集成**
   - 人脸特征存储
   - 刷脸借还书

2. **智能推荐**
   - 基于借阅历史
   - 个性化推荐

3. **积分商城**
   - 信用分兑换
   - 权益系统

4. **移动端支持**
   - 电子读者证
   - 在线续期

5. **数据分析**
   - 读者画像
   - 行为分析

---

**Document Version**: 1.0.0
**Last Updated**: 2025-10-28
**Next Review**: 2025-11-28