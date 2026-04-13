package com.gcrf.library.reader.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.reader.entity.ReaderType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 读者类型Mapper接口
 *
 * @author GCRF Team
 * @date 2025-11-08
 */
@Mapper
public interface ReaderTypeMapper extends BaseMapper<ReaderType> {

    /**
     * 检查类型代码是否存在
     */
    @Select("SELECT COUNT(*) FROM reader_types " +
            "WHERE type_code = #{typeCode} " +
            "AND deleted_at IS NULL " +
            "AND (#{excludeId} IS NULL OR id != #{excludeId})")
    int existsByTypeCode(@Param("typeCode") String typeCode, @Param("excludeId") Long excludeId);

    /**
     * 统计使用该类型的读者数量
     */
    @Select("SELECT COUNT(*) FROM readers " +
            "WHERE reader_type = #{typeCode} " +
            "AND deleted_at IS NULL")
    int countReadersByType(@Param("typeCode") String typeCode);
}
