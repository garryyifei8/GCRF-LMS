package com.gcrf.library.opac.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SchoolAvailabilityVO {
    private String schoolSchema;
    private String schoolName;
    private Integer totalCount;
    private Integer availableCount;
}
