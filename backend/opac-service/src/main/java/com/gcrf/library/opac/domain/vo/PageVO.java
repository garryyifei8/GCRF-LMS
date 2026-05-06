package com.gcrf.library.opac.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PageVO<T> {
    private List<T> records;
    private long total;
    private int pageNum;
    private int pageSize;
}
