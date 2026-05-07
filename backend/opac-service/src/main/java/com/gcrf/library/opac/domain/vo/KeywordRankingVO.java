package com.gcrf.library.opac.domain.vo;

import lombok.Data;

@Data
public class KeywordRankingVO {
    private int rank;
    private String keyword;
    private long count;
}
