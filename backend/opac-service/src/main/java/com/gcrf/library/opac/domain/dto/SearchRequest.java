package com.gcrf.library.opac.domain.dto;

import lombok.Data;

@Data
public class SearchRequest {
    private String q;             // keyword (matches title / author / isbn)
    private String clc;           // CLC code prefix, e.g. "I" or "I2"
    private String school;        // school_schema, e.g. "school_000003" (null = all schools)
    private int pageNum = 1;
    private int pageSize = 20;
}
