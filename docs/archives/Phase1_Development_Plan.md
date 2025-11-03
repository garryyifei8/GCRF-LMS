# Phase 1 开发计划 - 核心业务服务完善

**创建日期**: 2025-10-13
**预计工期**: Day 1-4 (4天)
**Story Points**: 22 SP
**状态**: 准备工作已完成,开始业务开发

---

## 目录

1. [准备工作检查](#1-准备工作检查)
2. [US-1: 读者服务开发 (8 SP)](#2-us-1-读者服务开发-8-sp)
3. [US-2: 系统服务开发 (8 SP)](#3-us-2-系统服务开发-8-sp)
4. [US-3: 通知服务开发 (6 SP)](#4-us-3-通知服务开发-6-sp)
5. [集成测试](#5-集成测试)
6. [参考资源](#6-参考资源)

---

## 1. 准备工作检查

### ✅ 已完成

- [x] RabbitMQ 已部署并运行 (localhost:5672)
- [x] reader-service 项目骨架已创建 (端口 8083)
- [x] system-service 项目骨架已创建 (端口 8084)
- [x] notification-service 项目骨架已创建 (端口 8085)
- [x] reader-service 已成功注册到 Nacos

### ⏳ 待完成

- [ ] 执行数据库初始化脚本
- [ ] 验证 PostgreSQL 连接
- [ ] 配置 RabbitMQ 交换机和队列

---

## 2. US-1: 读者服务开发 (8 SP)

### 2.1 数据库初始化 (0.5 SP)

**任务**: 创建 reader_service 数据库和表

**步骤**:

```bash
# 1. 连接 PostgreSQL
psql -U postgres -h localhost -p 5432

# 2. 创建数据库
CREATE DATABASE reader_service;

# 3. 连接到 reader_service 数据库
\c reader_service

# 4. 执行建表脚本
\i /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend/database/schema/04_reader_service.sql

# 5. 验证表创建
\dt
```

**验证**: 确认创建了 6 张表:
- readers (读者表)
- card_records (读者证记录表)
- reader_behavior_logs (行为日志表)
- reader_favorites (收藏表)
- reader_reviews (评价表)
- reader_notifications (通知表)

---

### 2.2 创建 Entity 实体类 (1 SP)

**目录**: `reader-service/src/main/java/com/gcrf/library/reader/entity/`

#### Reader.java

```java
package com.gcrf.library.reader.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 读者实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("readers")
public class Reader {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("reader_id")
    private String readerId;  // 读者证号(学号/工号)

    @TableField("card_number")
    private String cardNumber;  // 卡号

    @TableField("reader_name")
    private String readerName;  // 姓名

    @TableField("reader_type")
    private String readerType;  // 读者类型: STUDENT/TEACHER/GUEST

    @TableField("gender")
    private String gender;  // 性别

    @TableField("id_card")
    private String idCard;  // 身份证号

    @TableField("phone")
    private String phone;  // 手机号

    @TableField("email")
    private String email;  // 邮箱

    @TableField("department")
    private String department;  // 院系/部门

    @TableField("major")
    private String major;  // 专业

    @TableField("grade")
    private String grade;  // 年级

    @TableField("class_name")
    private String className;  // 班级

    @TableField("student_type")
    private String studentType;  // 学生类型

    @TableField("photo_url")
    private String photoUrl;  // 照片URL

    @TableField("face_features")
    private String faceFeatures;  // 人脸特征

    @TableField("deposit_amount")
    private BigDecimal depositAmount;  // 押金金额

    @TableField("credit_score")
    private Integer creditScore;  // 信用分

    @TableField("max_borrow_quantity")
    private Integer maxBorrowQuantity;  // 最大借阅数量

    @TableField("current_borrow_count")
    private Integer currentBorrowCount;  // 当前借阅数量

    @TableField("total_borrow_count")
    private Integer totalBorrowCount;  // 累计借阅次数

    @TableField("overdue_count")
    private Integer overdueCount;  // 逾期次数

    @TableField("card_status")
    private String cardStatus;  // 证件状态

    @TableField("account_balance")
    private BigDecimal accountBalance;  // 账户余额

    @TableField("issue_date")
    private LocalDate issueDate;  // 发证日期

    @TableField("expire_date")
    private LocalDate expireDate;  // 证件有效期

    @TableField("remarks")
    private String remarks;  // 备注

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
```

#### CardRecord.java

```java
package com.gcrf.library.reader.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 读者证办理记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("card_records")
public class CardRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("reader_id")
    private Long readerId;

    @TableField("record_type")
    private String recordType;  // ISSUE/RENEW/REISSUE/CANCEL

    @TableField("old_card_number")
    private String oldCardNumber;

    @TableField("new_card_number")
    private String newCardNumber;

    @TableField("fee_amount")
    private BigDecimal feeAmount;

    @TableField("payment_status")
    private String paymentStatus;  // UNPAID/PAID

    @TableField("payment_method")
    private String paymentMethod;  // CASH/WECHAT/ALIPAY/BANK

    @TableField("reason")
    private String reason;

    @TableField("operator_id")
    private Long operatorId;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

**创建其他实体类** (可选,先实现核心功能):
- ReaderBehaviorLog.java
- ReaderFavorite.java
- ReaderReview.java
- ReaderNotification.java

---

### 2.3 创建 DTO 类 (1 SP)

**目录**: `reader-service/src/main/java/com/gcrf/library/reader/dto/`

#### request/ReaderCreateRequest.java

```java
package com.gcrf.library.reader.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 创建读者请求
 */
@Data
@Schema(description = "创建读者请求")
public class ReaderCreateRequest {

    @NotBlank(message = "读者证号不能为空")
    @Schema(description = "读者证号(学号/工号)")
    private String readerId;

    @NotBlank(message = "姓名不能为空")
    @Schema(description = "姓名")
    private String readerName;

    @NotBlank(message = "读者类型不能为空")
    @Pattern(regexp = "STUDENT|TEACHER|GUEST", message = "读者类型必须是 STUDENT/TEACHER/GUEST")
    @Schema(description = "读者类型", allowableValues = {"STUDENT", "TEACHER", "GUEST"})
    private String readerType;

    @Pattern(regexp = "MALE|FEMALE|OTHER", message = "性别必须是 MALE/FEMALE/OTHER")
    @Schema(description = "性别")
    private String gender;

    @Pattern(regexp = "\\d{17}[0-9X]", message = "身份证号格式不正确")
    @Schema(description = "身份证号")
    private String idCard;

    @Pattern(regexp = "1\\d{10}", message = "手机号格式不正确")
    @Schema(description = "手机号")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "院系/部门")
    private String department;

    @Schema(description = "专业")
    private String major;

    @Schema(description = "年级")
    private String grade;

    @Schema(description = "班级")
    private String className;

    @Schema(description = "备注")
    private String remarks;
}
```

#### request/ReaderUpdateRequest.java

```java
package com.gcrf.library.reader.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "更新读者请求")
public class ReaderUpdateRequest {

    @Pattern(regexp = "1\\d{10}", message = "手机号格式不正确")
    @Schema(description = "手机号")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "院系/部门")
    private String department;

    @Schema(description = "专业")
    private String major;

    @Schema(description = "年级")
    private String grade;

    @Schema(description = "班级")
    private String className;

    @Schema(description = "备注")
    private String remarks;
}
```

#### request/ReaderQueryRequest.java

```java
package com.gcrf.library.reader.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "查询读者请求")
public class ReaderQueryRequest {

    @Schema(description = "读者证号")
    private String readerId;

    @Schema(description = "姓名(模糊查询)")
    private String readerName;

    @Schema(description = "读者类型")
    private String readerType;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "证件状态")
    private String cardStatus;

    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小", example = "20")
    private Integer pageSize = 20;
}
```

#### request/CardIssueRequest.java

```java
package com.gcrf.library.reader.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "办理读者证请求")
public class CardIssueRequest {

    @NotNull(message = "读者ID不能为空")
    @Schema(description = "读者ID")
    private Long readerId;

    @Positive(message = "押金金额必须大于0")
    @Schema(description = "押金金额")
    private BigDecimal depositAmount;

    @Positive(message = "有效月数必须大于0")
    @Schema(description = "有效月数")
    private Integer validMonths;

    @Schema(description = "支付方式", allowableValues = {"CASH", "WECHAT", "ALIPAY", "BANK"})
    private String paymentMethod;
}
```

#### response/ReaderResponse.java

```java
package com.gcrf.library.reader.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "读者响应")
public class ReaderResponse {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "读者证号")
    private String readerId;

    @Schema(description = "卡号")
    private String cardNumber;

    @Schema(description = "姓名")
    private String readerName;

    @Schema(description = "读者类型")
    private String readerType;

    @Schema(description = "性别")
    private String gender;

    @Schema(description = "身份证号(脱敏)")
    private String idCard;

    @Schema(description = "手机号(脱敏)")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "院系/部门")
    private String department;

    @Schema(description = "专业")
    private String major;

    @Schema(description = "年级")
    private String grade;

    @Schema(description = "班级")
    private String className;

    @Schema(description = "照片URL")
    private String photoUrl;

    @Schema(description = "押金金额")
    private BigDecimal depositAmount;

    @Schema(description = "信用分")
    private Integer creditScore;

    @Schema(description = "最大借阅数量")
    private Integer maxBorrowQuantity;

    @Schema(description = "当前借阅数量")
    private Integer currentBorrowCount;

    @Schema(description = "累计借阅次数")
    private Integer totalBorrowCount;

    @Schema(description = "逾期次数")
    private Integer overdueCount;

    @Schema(description = "证件状态")
    private String cardStatus;

    @Schema(description = "账户余额")
    private BigDecimal accountBalance;

    @Schema(description = "发证日期")
    private LocalDate issueDate;

    @Schema(description = "证件有效期")
    private LocalDate expireDate;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
```

#### response/CardRecordResponse.java

```java
package com.gcrf.library.reader.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "读者证记录响应")
public class CardRecordResponse {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "读者ID")
    private Long readerId;

    @Schema(description = "记录类型")
    private String recordType;

    @Schema(description = "旧卡号")
    private String oldCardNumber;

    @Schema(description = "新卡号")
    private String newCardNumber;

    @Schema(description = "费用")
    private BigDecimal feeAmount;

    @Schema(description = "支付状态")
    private String paymentStatus;

    @Schema(description = "支付方式")
    private String paymentMethod;

    @Schema(description = "办理原因")
    private String reason;

    @Schema(description = "操作员ID")
    private Long operatorId;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
```

---

### 2.4 创建 Mapper 接口 (0.5 SP)

**目录**: `reader-service/src/main/java/com/gcrf/library/reader/mapper/`

#### ReaderMapper.java

```java
package com.gcrf.library.reader.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.reader.entity.Reader;
import org.apache.ibatis.annotations.Mapper;

/**
 * 读者 Mapper
 */
@Mapper
public interface ReaderMapper extends BaseMapper<Reader> {
}
```

#### CardRecordMapper.java

```java
package com.gcrf.library.reader.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.reader.entity.CardRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CardRecordMapper extends BaseMapper<CardRecord> {
}
```

---

### 2.5 创建 Service 层 (2.5 SP)

**目录**: `reader-service/src/main/java/com/gcrf/library/reader/service/`

#### ReaderService.java

```java
package com.gcrf.library.reader.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.reader.dto.request.*;
import com.gcrf.library.reader.dto.response.*;

/**
 * 读者服务接口
 */
public interface ReaderService {

    /**
     * 创建读者
     */
    ReaderResponse createReader(ReaderCreateRequest request);

    /**
     * 更新读者信息
     */
    ReaderResponse updateReader(Long id, ReaderUpdateRequest request);

    /**
     * 删除读者
     */
    void deleteReader(Long id);

    /**
     * 查询读者详情
     */
    ReaderResponse getReaderById(Long id);

    /**
     * 根据读者证号查询
     */
    ReaderResponse getReaderByReaderId(String readerId);

    /**
     * 分页查询读者列表
     */
    Page<ReaderResponse> queryReaders(ReaderQueryRequest request);

    /**
     * 办理读者证
     */
    CardRecordResponse issueCard(CardIssueRequest request);

    /**
     * 续证
     */
    CardRecordResponse renewCard(Long readerId, Integer validMonths);

    /**
     * 挂失
     */
    CardRecordResponse reportLost(Long readerId, String reason);

    /**
     * 注销读者证
     */
    void cancelCard(Long readerId, String reason);

    /**
     * 查询读者证办理记录
     */
    Page<CardRecordResponse> getCardRecords(Long readerId, Integer pageNum, Integer pageSize);
}
```

#### impl/ReaderServiceImpl.java

```java
package com.gcrf.library.reader.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.reader.dto.request.*;
import com.gcrf.library.reader.dto.response.*;
import com.gcrf.library.reader.entity.CardRecord;
import com.gcrf.library.reader.entity.Reader;
import com.gcrf.library.reader.mapper.CardRecordMapper;
import com.gcrf.library.reader.mapper.ReaderMapper;
import com.gcrf.library.reader.service.ReaderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 读者服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReaderServiceImpl implements ReaderService {

    private final ReaderMapper readerMapper;
    private final CardRecordMapper cardRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReaderResponse createReader(ReaderCreateRequest request) {
        log.info("创建读者: {}", request.getReaderName());

        // 检查读者证号是否已存在
        Long count = readerMapper.selectCount(
            new LambdaQueryWrapper<Reader>()
                .eq(Reader::getReaderId, request.getReaderId())
        );
        if (count > 0) {
            throw new BusinessException("读者证号已存在: " + request.getReaderId());
        }

        // 创建读者
        Reader reader = new Reader();
        BeanUtils.copyProperties(request, reader);

        // 设置默认值
        reader.setCreditScore(100);
        reader.setMaxBorrowQuantity(10);
        reader.setCurrentBorrowCount(0);
        reader.setTotalBorrowCount(0);
        reader.setOverdueCount(0);
        reader.setCardStatus("PENDING");  // 待办理读者证
        reader.setDepositAmount(BigDecimal.ZERO);
        reader.setAccountBalance(BigDecimal.ZERO);

        readerMapper.insert(reader);

        log.info("读者创建成功: id={}, readerId={}", reader.getId(), reader.getReaderId());

        return convertToResponse(reader);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReaderResponse updateReader(Long id, ReaderUpdateRequest request) {
        log.info("更新读者信息: id={}", id);

        Reader reader = readerMapper.selectById(id);
        if (reader == null) {
            throw new BusinessException("读者不存在: " + id);
        }

        // 更新字段
        if (StringUtils.hasText(request.getPhone())) {
            reader.setPhone(request.getPhone());
        }
        if (StringUtils.hasText(request.getEmail())) {
            reader.setEmail(request.getEmail());
        }
        if (StringUtils.hasText(request.getDepartment())) {
            reader.setDepartment(request.getDepartment());
        }
        if (StringUtils.hasText(request.getMajor())) {
            reader.setMajor(request.getMajor());
        }
        if (StringUtils.hasText(request.getGrade())) {
            reader.setGrade(request.getGrade());
        }
        if (StringUtils.hasText(request.getClassName())) {
            reader.setClassName(request.getClassName());
        }
        if (StringUtils.hasText(request.getRemarks())) {
            reader.setRemarks(request.getRemarks());
        }

        readerMapper.updateById(reader);

        log.info("读者信息更新成功: id={}", id);

        return convertToResponse(reader);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReader(Long id) {
        log.info("删除读者: id={}", id);

        Reader reader = readerMapper.selectById(id);
        if (reader == null) {
            throw new BusinessException("读者不存在: " + id);
        }

        // 检查是否有未归还图书
        if (reader.getCurrentBorrowCount() > 0) {
            throw new BusinessException("该读者有未归还图书，无法删除");
        }

        // 软删除
        readerMapper.deleteById(id);

        log.info("读者删除成功: id={}", id);
    }

    @Override
    public ReaderResponse getReaderById(Long id) {
        Reader reader = readerMapper.selectById(id);
        if (reader == null) {
            throw new BusinessException("读者不存在: " + id);
        }
        return convertToResponse(reader);
    }

    @Override
    public ReaderResponse getReaderByReaderId(String readerId) {
        Reader reader = readerMapper.selectOne(
            new LambdaQueryWrapper<Reader>()
                .eq(Reader::getReaderId, readerId)
        );
        if (reader == null) {
            throw new BusinessException("读者不存在: " + readerId);
        }
        return convertToResponse(reader);
    }

    @Override
    public Page<ReaderResponse> queryReaders(ReaderQueryRequest request) {
        Page<Reader> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<Reader> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(request.getReaderId()), Reader::getReaderId, request.getReaderId())
               .like(StringUtils.hasText(request.getReaderName()), Reader::getReaderName, request.getReaderName())
               .eq(StringUtils.hasText(request.getReaderType()), Reader::getReaderType, request.getReaderType())
               .eq(StringUtils.hasText(request.getPhone()), Reader::getPhone, request.getPhone())
               .eq(StringUtils.hasText(request.getCardStatus()), Reader::getCardStatus, request.getCardStatus())
               .orderByDesc(Reader::getCreatedAt);

        Page<Reader> readerPage = readerMapper.selectPage(page, wrapper);

        // 转换为响应对象
        Page<ReaderResponse> responsePage = new Page<>();
        BeanUtils.copyProperties(readerPage, responsePage, "records");
        responsePage.setRecords(
            readerPage.getRecords().stream()
                .map(this::convertToResponse)
                .toList()
        );

        return responsePage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CardRecordResponse issueCard(CardIssueRequest request) {
        log.info("办理读者证: readerId={}", request.getReaderId());

        Reader reader = readerMapper.selectById(request.getReaderId());
        if (reader == null) {
            throw new BusinessException("读者不存在");
        }

        if ("NORMAL".equals(reader.getCardStatus())) {
            throw new BusinessException("读者证已办理");
        }

        // 生成卡号
        String cardNumber = generateCardNumber();

        // 更新读者信息
        reader.setCardNumber(cardNumber);
        reader.setCardStatus("NORMAL");
        reader.setDepositAmount(request.getDepositAmount());
        reader.setIssueDate(LocalDate.now());
        reader.setExpireDate(LocalDate.now().plusMonths(request.getValidMonths()));
        readerMapper.updateById(reader);

        // 创建办理记录
        CardRecord record = CardRecord.builder()
            .readerId(request.getReaderId())
            .recordType("ISSUE")
            .newCardNumber(cardNumber)
            .feeAmount(request.getDepositAmount())
            .paymentStatus("PAID")
            .paymentMethod(request.getPaymentMethod())
            .reason("新办读者证")
            .build();
        cardRecordMapper.insert(record);

        log.info("读者证办理成功: cardNumber={}", cardNumber);

        return convertToCardRecordResponse(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CardRecordResponse renewCard(Long readerId, Integer validMonths) {
        log.info("续证: readerId={}, validMonths={}", readerId, validMonths);

        Reader reader = readerMapper.selectById(readerId);
        if (reader == null) {
            throw new BusinessException("读者不存在");
        }

        // 更新有效期
        LocalDate newExpireDate = reader.getExpireDate().plusMonths(validMonths);
        reader.setExpireDate(newExpireDate);
        reader.setCardStatus("NORMAL");
        readerMapper.updateById(reader);

        // 创建续证记录
        CardRecord record = CardRecord.builder()
            .readerId(readerId)
            .recordType("RENEW")
            .newCardNumber(reader.getCardNumber())
            .feeAmount(BigDecimal.ZERO)
            .paymentStatus("PAID")
            .reason("续证" + validMonths + "个月")
            .build();
        cardRecordMapper.insert(record);

        log.info("续证成功: readerId={}, newExpireDate={}", readerId, newExpireDate);

        return convertToCardRecordResponse(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CardRecordResponse reportLost(Long readerId, String reason) {
        log.info("挂失: readerId={}", readerId);

        Reader reader = readerMapper.selectById(readerId);
        if (reader == null) {
            throw new BusinessException("读者不存在");
        }

        String oldCardNumber = reader.getCardNumber();
        String newCardNumber = generateCardNumber();

        // 更新读者信息
        reader.setCardNumber(newCardNumber);
        reader.setCardStatus("NORMAL");
        readerMapper.updateById(reader);

        // 创建挂失记录
        CardRecord record = CardRecord.builder()
            .readerId(readerId)
            .recordType("REISSUE")
            .oldCardNumber(oldCardNumber)
            .newCardNumber(newCardNumber)
            .feeAmount(new BigDecimal("10.00"))  // 补卡费
            .paymentStatus("PAID")
            .reason(reason)
            .build();
        cardRecordMapper.insert(record);

        log.info("挂失成功: oldCardNumber={}, newCardNumber={}", oldCardNumber, newCardNumber);

        return convertToCardRecordResponse(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelCard(Long readerId, String reason) {
        log.info("注销读者证: readerId={}", readerId);

        Reader reader = readerMapper.selectById(readerId);
        if (reader == null) {
            throw new BusinessException("读者不存在");
        }

        // 检查是否有未归还图书
        if (reader.getCurrentBorrowCount() > 0) {
            throw new BusinessException("有未归还图书，无法注销");
        }

        // 更新状态
        reader.setCardStatus("CANCELLED");
        readerMapper.updateById(reader);

        // 创建注销记录
        CardRecord record = CardRecord.builder()
            .readerId(readerId)
            .recordType("CANCEL")
            .oldCardNumber(reader.getCardNumber())
            .feeAmount(BigDecimal.ZERO)
            .paymentStatus("PAID")
            .reason(reason)
            .build();
        cardRecordMapper.insert(record);

        log.info("注销成功: readerId={}", readerId);
    }

    @Override
    public Page<CardRecordResponse> getCardRecords(Long readerId, Integer pageNum, Integer pageSize) {
        Page<CardRecord> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<CardRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CardRecord::getReaderId, readerId)
               .orderByDesc(CardRecord::getCreatedAt);

        Page<CardRecord> recordPage = cardRecordMapper.selectPage(page, wrapper);

        // 转换为响应对象
        Page<CardRecordResponse> responsePage = new Page<>();
        BeanUtils.copyProperties(recordPage, responsePage, "records");
        responsePage.setRecords(
            recordPage.getRecords().stream()
                .map(this::convertToCardRecordResponse)
                .toList()
        );

        return responsePage;
    }

    // ========== 私有方法 ==========

    /**
     * 生成卡号
     */
    private String generateCardNumber() {
        return "RC" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    /**
     * 转换为响应对象
     */
    private ReaderResponse convertToResponse(Reader reader) {
        ReaderResponse response = new ReaderResponse();
        BeanUtils.copyProperties(reader, response);

        // 脱敏处理
        if (StringUtils.hasText(reader.getIdCard()) && reader.getIdCard().length() >= 14) {
            response.setIdCard(reader.getIdCard().replaceAll("(\\d{6})\\d{8}(\\d{4})", "$1********$2"));
        }
        if (StringUtils.hasText(reader.getPhone()) && reader.getPhone().length() == 11) {
            response.setPhone(reader.getPhone().replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
        }

        return response;
    }

    /**
     * 转换为读者证记录响应
     */
    private CardRecordResponse convertToCardRecordResponse(CardRecord record) {
        CardRecordResponse response = new CardRecordResponse();
        BeanUtils.copyProperties(record, response);
        return response;
    }
}
```

---

### 2.6 创建 Controller 层 (2 SP)

**目录**: `reader-service/src/main/java/com/gcrf/library/reader/controller/`

#### ReaderController.java

```java
package com.gcrf.library.reader.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.common.result.Result;
import com.gcrf.library.reader.dto.request.*;
import com.gcrf.library.reader.dto.response.*;
import com.gcrf.library.reader.service.ReaderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 读者管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/readers")
@RequiredArgsConstructor
@Validated
@Tag(name = "读者管理", description = "读者信息 CRUD、读者证管理")
public class ReaderController {

    private final ReaderService readerService;

    @PostMapping
    @Operation(summary = "创建读者", description = "注册新读者")
    public Result<ReaderResponse> createReader(@RequestBody @Valid ReaderCreateRequest request) {
        log.info("创建读者请求: {}", request.getReaderName());
        ReaderResponse response = readerService.createReader(request);
        return Result.success(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新读者信息", description = "更新读者基本信息")
    public Result<ReaderResponse> updateReader(
        @PathVariable Long id,
        @RequestBody @Valid ReaderUpdateRequest request
    ) {
        log.info("更新读者请求: id={}", id);
        ReaderResponse response = readerService.updateReader(id, request);
        return Result.success(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除读者", description = "软删除读者(需确保无未归还图书)")
    public Result<Void> deleteReader(@PathVariable Long id) {
        log.info("删除读者请求: id={}", id);
        readerService.deleteReader(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询读者详情", description = "根据主键ID查询读者")
    public Result<ReaderResponse> getReaderById(@PathVariable Long id) {
        ReaderResponse response = readerService.getReaderById(id);
        return Result.success(response);
    }

    @GetMapping("/by-reader-id/{readerId}")
    @Operation(summary = "根据读者证号查询", description = "根据读者证号(学号/工号)查询")
    public Result<ReaderResponse> getReaderByReaderId(@PathVariable String readerId) {
        ReaderResponse response = readerService.getReaderByReaderId(readerId);
        return Result.success(response);
    }

    @GetMapping
    @Operation(summary = "分页查询读者列表", description = "支持多条件筛选")
    public Result<Page<ReaderResponse>> queryReaders(@ModelAttribute ReaderQueryRequest request) {
        Page<ReaderResponse> page = readerService.queryReaders(request);
        return Result.success(page);
    }

    @PostMapping("/cards/issue")
    @Operation(summary = "办理读者证", description = "为已注册读者办理读者证")
    public Result<CardRecordResponse> issueCard(@RequestBody @Valid CardIssueRequest request) {
        log.info("办理读者证请求: readerId={}", request.getReaderId());
        CardRecordResponse response = readerService.issueCard(request);
        return Result.success(response);
    }

    @PostMapping("/{readerId}/cards/renew")
    @Operation(summary = "续证", description = "延长读者证有效期")
    public Result<CardRecordResponse> renewCard(
        @PathVariable Long readerId,
        @RequestParam Integer validMonths
    ) {
        log.info("续证请求: readerId={}, validMonths={}", readerId, validMonths);
        CardRecordResponse response = readerService.renewCard(readerId, validMonths);
        return Result.success(response);
    }

    @PostMapping("/{readerId}/cards/report-lost")
    @Operation(summary = "挂失补办", description = "挂失旧卡并生成新卡号")
    public Result<CardRecordResponse> reportLost(
        @PathVariable Long readerId,
        @RequestParam String reason
    ) {
        log.info("挂失请求: readerId={}", readerId);
        CardRecordResponse response = readerService.reportLost(readerId, reason);
        return Result.success(response);
    }

    @PostMapping("/{readerId}/cards/cancel")
    @Operation(summary = "注销读者证", description = "注销读者证(需确保无未归还图书)")
    public Result<Void> cancelCard(
        @PathVariable Long readerId,
        @RequestParam String reason
    ) {
        log.info("注销读者证请求: readerId={}", readerId);
        readerService.cancelCard(readerId, reason);
        return Result.success();
    }

    @GetMapping("/{readerId}/cards/records")
    @Operation(summary = "查询读者证办理记录", description = "查询某读者的所有证件办理记录")
    public Result<Page<CardRecordResponse>> getCardRecords(
        @PathVariable Long readerId,
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        Page<CardRecordResponse> page = readerService.getCardRecords(readerId, pageNum, pageSize);
        return Result.success(page);
    }
}
```

---

### 2.7 配置 MyBatis Plus (0.5 SP)

**创建配置类**: `reader-service/src/main/java/com/gcrf/library/reader/config/MybatisPlusConfig.java`

```java
package com.gcrf.library.reader.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus 配置
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }
}
```

---

### 2.8 创建通用 Result 类 (如果 common-core 没有) (0.5 SP)

**检查**: 先查看 `common/common-core` 是否已有 Result 类

如果没有,创建: `common/common-core/src/main/java/com/gcrf/library/common/result/Result.java`

```java
package com.gcrf.library.common.result;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        return result;
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }
}
```

---

### 2.9 创建全局异常处理 (0.5 SP)

**创建异常类**: `common/common-core/src/main/java/com/gcrf/library/common/exception/BusinessException.java`

```java
package com.gcrf.library.common.exception;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
```

**创建全局异常处理器**: `common/common-web/src/main/java/com/gcrf/library/common/web/exception/GlobalExceptionHandler.java`

```java
package com.gcrf.library.common.web.exception;

import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage());
        return Result.error(e.getMessage());
    }

    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        log.error("参数校验异常: {}", message);
        return Result.error(message);
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error("系统异常: " + e.getMessage());
    }
}
```

---

### 2.10 测试 API (0.5 SP)

#### 启动服务

```bash
cd /Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend/reader-service

JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn spring-boot:run
```

#### 测试用例

**1. 创建读者**

```bash
curl -X POST http://localhost:8083/api/v1/readers \
  -H "Content-Type: application/json" \
  -d '{
    "readerId": "2021001",
    "readerName": "张三",
    "readerType": "STUDENT",
    "gender": "MALE",
    "idCard": "320123199901011234",
    "phone": "13800138000",
    "email": "zhangsan@example.com",
    "department": "计算机学院",
    "major": "软件工程",
    "grade": "2021",
    "className": "软工21-1"
  }'
```

**2. 办理读者证**

```bash
curl -X POST http://localhost:8083/api/v1/readers/cards/issue \
  -H "Content-Type: application/json" \
  -d '{
    "readerId": 1,
    "depositAmount": 50.00,
    "validMonths": 12,
    "paymentMethod": "WECHAT"
  }'
```

**3. 查询读者列表**

```bash
curl "http://localhost:8083/api/v1/readers?pageNum=1&pageSize=20"
```

**4. 查询读者详情**

```bash
curl "http://localhost:8083/api/v1/readers/1"
```

**5. 访问 API 文档**

打开浏览器访问: http://localhost:8083/doc.html

---

## 3. US-2: 系统服务开发 (8 SP)

**说明**: system-service 的开发步骤与 reader-service 类似,主要实现以下功能:

### 3.1 核心功能

1. **用户管理** (User)
   - 创建、更新、删除用户
   - 用户登录、登出
   - 密码修改、重置

2. **角色管理** (Role)
   - CRUD 操作
   - 角色权限分配

3. **权限管理** (Permission)
   - 权限树查询
   - 权限验证

4. **部门管理** (Department)
   - 部门树形结构
   - 部门人员管理

5. **系统配置** (SystemConfig)
   - 配置的 CRUD
   - 配置推送到 Nacos

### 3.2 数据库表

参考: `/Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend/database/schema/05_system_service.sql` (需要创建)

建议表结构:
- `sys_users` - 用户表
- `sys_roles` - 角色表
- `sys_permissions` - 权限表
- `sys_user_roles` - 用户角色关联表
- `sys_role_permissions` - 角色权限关联表
- `sys_departments` - 部门表
- `sys_configs` - 系统配置表

### 3.3 开发步骤

仿照 reader-service 的结构:

1. 创建数据库表 (0.5 SP)
2. 创建 Entity 实体类 (1 SP)
3. 创建 DTO 类 (1 SP)
4. 创建 Mapper 接口 (0.5 SP)
5. 创建 Service 层 (2.5 SP)
6. 创建 Controller 层 (2 SP)
7. 测试 API (0.5 SP)

---

## 4. US-3: 通知服务开发 (6 SP)

### 4.1 核心功能

1. **邮件通知**
   - 模板邮件发送
   - 批量邮件发送

2. **短信通知**
   - 验证码短信
   - 通知短信

3. **站内信**
   - 创建、查询、标记已读
   - 消息分类

4. **RabbitMQ 消息消费**
   - 监听借阅事件
   - 监听读者证办理事件

### 4.2 数据库表

参考: `/Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend/database/schema/06_notification_service.sql` (需要创建)

建议表结构:
- `notification_templates` - 通知模板表
- `notification_records` - 通知记录表
- `notification_queues` - 通知队列表

### 4.3 RabbitMQ 配置

#### RabbitMQConfig.java

```java
package com.gcrf.library.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // 交换机
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";

    // 队列
    public static final String EMAIL_QUEUE = "notification.email.queue";
    public static final String SMS_QUEUE = "notification.sms.queue";

    // 路由键
    public static final String EMAIL_ROUTING_KEY = "notification.email.send";
    public static final String SMS_ROUTING_KEY = "notification.sms.send";

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Queue emailQueue() {
        return new Queue(EMAIL_QUEUE, true);
    }

    @Bean
    public Queue smsQueue() {
        return new Queue(SMS_QUEUE, true);
    }

    @Bean
    public Binding emailBinding() {
        return BindingBuilder.bind(emailQueue())
            .to(notificationExchange())
            .with(EMAIL_ROUTING_KEY);
    }

    @Bean
    public Binding smsBinding() {
        return BindingBuilder.bind(smsQueue())
            .to(notificationExchange())
            .with(SMS_ROUTING_KEY);
    }
}
```

#### EmailConsumer.java

```java
package com.gcrf.library.notification.consumer;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailConsumer {

    @RabbitListener(queues = "notification.email.queue")
    public void handleEmailMessage(Message message, Channel channel) throws IOException {
        try {
            String content = new String(message.getBody());
            log.info("收到邮件消息: {}", content);

            // TODO: 解析消息,发送邮件

            // 手动确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("处理邮件消息失败", e);
            // 拒绝消息,重新入队
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }
}
```

### 4.4 开发步骤

1. 配置 RabbitMQ (0.5 SP)
2. 创建数据库表 (0.5 SP)
3. 创建 Entity/DTO 类 (1 SP)
4. 创建邮件服务 (1.5 SP)
5. 创建短信服务 (1 SP)
6. 创建消息消费者 (1 SP)
7. 测试通知发送 (0.5 SP)

---

## 5. 集成测试

### 5.1 场景 1: 读者注册 → 办证 → 通知

```bash
# 1. 创建读者
POST /api/v1/readers

# 2. 办理读者证 (触发 RabbitMQ 消息)
POST /api/v1/readers/cards/issue

# 3. 验证 notification-service 收到消息
# 4. 验证邮件/短信发送成功
```

### 5.2 场景 2: 系统配置 → Nacos 同步

```bash
# 1. 创建系统配置
POST /api/v1/system/configs

# 2. 验证 Nacos 配置中心已更新
curl http://localhost:8848/nacos/v1/cs/configs?...

# 3. 验证其他服务配置自动刷新
```

### 5.3 Postman 集合

创建 Postman 集合,包含所有 API 测试用例。

---

## 6. 参考资源

### 6.1 已有服务参考

- **book-service**: `/Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend/book-service`
- **circulation-service**: `/Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend/circulation-service`
- **auth-service**: `/Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend/auth-service`

### 6.2 数据库脚本

- **reader_service**: `/Users/garyyifei/Documents/Corp/Projects/GCRF_LibraryManagementSystem/backend/database/schema/04_reader_service.sql`

### 6.3 技术文档

- **Spring Boot 3.2**: https://spring.io/projects/spring-boot
- **MyBatis Plus**: https://baomidou.com/
- **Knife4j**: https://doc.xiaominfo.com/
- **RabbitMQ**: https://www.rabbitmq.com/

---

## 附录: 快速命令

```bash
# 启动 reader-service
cd backend/reader-service
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn spring-boot:run

# 启动 system-service
cd backend/system-service
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn spring-boot:run

# 启动 notification-service
cd backend/notification-service
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn spring-boot:run

# 编译所有服务
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean package -DskipTests

# 初始化数据库
psql -U postgres -h localhost -p 5432 < backend/database/schema/04_reader_service.sql
```

---

**祝开发顺利!**
