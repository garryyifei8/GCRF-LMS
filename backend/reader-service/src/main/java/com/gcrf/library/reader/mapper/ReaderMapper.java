package com.gcrf.library.reader.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.reader.entity.Reader;
import org.apache.ibatis.annotations.Mapper;

/**
 * 读者 Mapper 接口
 *
 * @author GCRF Team
 * @since 2025-10-13
 */
@Mapper
public interface ReaderMapper extends BaseMapper<Reader> {
}
