package com.gcrf.library.reader.dto.response;

import com.gcrf.library.reader.entity.ReaderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 读者类型响应VO
 *
 * @author GCRF Team
 * @date 2025-11-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReaderTypeVO {

    private Long id;
    private String typeCode;
    private String typeName;
    private Integer maxBorrowCount;
    private Integer maxBorrowDays;
    private Integer maxRenewCount;
    private Integer depositAmount;
    private String description;
    private String status;
    private Integer sortOrder;
    private Integer readerCount; // 使用该类型的读者数量
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReaderTypeVO from(ReaderType entity) {
        if (entity == null) {
            return null;
        }

        return ReaderTypeVO.builder()
                .id(entity.getId())
                .typeCode(entity.getTypeCode())
                .typeName(entity.getTypeName())
                .maxBorrowCount(entity.getMaxBorrowCount())
                .maxBorrowDays(entity.getMaxBorrowDays())
                .maxRenewCount(entity.getMaxRenewCount())
                .depositAmount(entity.getDepositAmount())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .sortOrder(entity.getSortOrder())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
