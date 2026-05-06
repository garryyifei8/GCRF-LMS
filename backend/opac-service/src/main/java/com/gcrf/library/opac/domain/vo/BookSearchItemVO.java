package com.gcrf.library.opac.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookSearchItemVO {
    private String schoolSchema;
    private Long bookId;
    private String isbn;
    private String title;
    private String author;
    private String classification;
    private Integer totalCount;
    private Integer availableCount;
    private LocalDateTime createdAt;
}
