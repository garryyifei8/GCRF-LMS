package com.gcrf.library.system.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户视图对象（供前端系统管理页使用）
 *
 * @author GCRF Team
 * @since 2026-04-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "系统用户视图对象")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserVO {

    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "用户名", example = "admin")
    private String username;

    @Schema(description = "真实姓名（当前映射自用户名）", example = "管理员")
    private String realName;

    @Schema(description = "邮箱", example = "admin@gcrf.com")
    private String email;

    @Schema(description = "手机号", example = "13800000000")
    private String phone;

    @Schema(description = "用户类型：STUDENT/TEACHER/ADMIN", example = "ADMIN")
    private String userType;

    @Schema(description = "头像URL")
    private String avatarUrl;

    @Schema(description = "账号状态：ACTIVE/INACTIVE/LOCKED", example = "ACTIVE")
    private String status;

    @Schema(description = "最后登录时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDateTime lastLoginTime;

    @Schema(description = "创建时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDateTime createdAt;
}
