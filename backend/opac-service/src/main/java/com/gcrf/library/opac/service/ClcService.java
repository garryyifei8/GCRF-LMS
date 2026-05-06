package com.gcrf.library.opac.service;

import com.gcrf.library.opac.domain.vo.ClcNodeVO;

import java.util.List;

public interface ClcService {
    List<ClcNodeVO> getTree();
}
