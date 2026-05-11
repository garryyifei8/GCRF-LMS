package com.gcrf.library.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AssignRoleRequest {
    @NotBlank
    private String roleCode;
    private Long schoolId;
    private LocalDateTime expiresAt;
}
