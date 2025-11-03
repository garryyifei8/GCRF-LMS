package com.gcrf.library.circulation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.circulation.entity.Reserve;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预约记录Mapper接口
 *
 * @author GCRF Team
 * @since 2025-10-28
 */
@Mapper
public interface ReserveMapper extends BaseMapper<Reserve> {
    // MyBatis-Plus BaseMapper已提供基础CRUD方法
    // 可在此添加自定义SQL方法
}
