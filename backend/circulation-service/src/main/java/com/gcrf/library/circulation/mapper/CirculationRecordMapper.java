package com.gcrf.library.circulation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.circulation.entity.CirculationRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流通记录Mapper
 *
 * @author GCRF Team
 * @date 2025-10-12
 */
@Mapper
public interface CirculationRecordMapper extends BaseMapper<CirculationRecord> {
}
