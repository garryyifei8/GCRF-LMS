package com.gcrf.library.opac.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class BookDetailVO {
    private String isbn;
    private String title;
    private String author;
    private String classification;
    private List<SchoolAvailabilityVO> schools;
}
