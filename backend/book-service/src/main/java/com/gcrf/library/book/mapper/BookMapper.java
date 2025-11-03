package com.gcrf.library.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcrf.library.book.entity.Book;
import org.apache.ibatis.annotations.Mapper;

/**
 * 图书Mapper
 *
 * @author GCRF Team
 * @date 2025-10-12
 */
@Mapper
public interface BookMapper extends BaseMapper<Book> {
}
