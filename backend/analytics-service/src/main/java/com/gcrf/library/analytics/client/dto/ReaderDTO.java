package com.gcrf.library.analytics.client.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 读者DTO（用于Feign调用）
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
public class ReaderDTO {

    private Long id;
    private String readerId;
    private String cardNo;
    private String name;
    private String readerType;
    private String readerTypeName;
    private String phone;
    private String email;
    private String avatar;
    private Integer maxBorrowCount;
    private Integer maxBorrowDays;
    private LocalDateTime createdAt;
}
