package com.gcrf.library.opac.mapper;

import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrossSchoolMapper {

    /** All books matching the given ISBN across all schools (from mview). */
    @Select("SELECT school_schema, book_id, isbn, title, author, classification,"
          + " total_count, available_count, created_at"
          + " FROM gcrf_region.book_search_mview WHERE isbn = #{isbn}")
    List<BookSearchItemVO> findByIsbn(@Param("isbn") String isbn);
}
