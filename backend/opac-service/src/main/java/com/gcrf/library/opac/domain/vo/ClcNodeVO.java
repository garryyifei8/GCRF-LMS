package com.gcrf.library.opac.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClcNodeVO {
    private String code;
    private String name;
    private String parentCode;
    private List<ClcNodeVO> children = new ArrayList<>();
}
