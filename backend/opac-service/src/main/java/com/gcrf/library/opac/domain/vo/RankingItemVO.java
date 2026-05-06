package com.gcrf.library.opac.domain.vo;

import lombok.Data;

@Data
public class RankingItemVO {
    private int rank;
    private String isbn;
    private String title;
    private String author;
    private String classification;
    private long borrowCount;
}
