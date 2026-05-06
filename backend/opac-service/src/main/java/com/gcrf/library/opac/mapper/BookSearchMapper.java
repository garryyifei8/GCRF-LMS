package com.gcrf.library.opac.mapper;

import com.gcrf.library.opac.domain.vo.BookSearchItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BookSearchMapper {

    @Select("""
        <script>
        SELECT school_schema, book_id, isbn, title, author, classification,
               total_count, available_count, created_at
          FROM gcrf_region.book_search_mview
         <where>
           <if test="q != null and q != ''">
             AND (title ILIKE CONCAT('%', #{q}, '%')
               OR author ILIKE CONCAT('%', #{q}, '%')
               OR isbn ILIKE CONCAT(#{q}, '%'))
           </if>
           <if test="clc != null and clc != ''">
             AND classification LIKE CONCAT(#{clc}, '%')
           </if>
           <if test="school != null and school != ''">
             AND school_schema = #{school}
           </if>
         </where>
         ORDER BY title
         LIMIT #{pageSize} OFFSET #{offset}
        </script>
        """)
    List<BookSearchItemVO> search(@Param("q") String q, @Param("clc") String clc,
                                  @Param("school") String school,
                                  @Param("pageSize") int pageSize, @Param("offset") int offset);

    @Select("""
        <script>
        SELECT count(*) FROM gcrf_region.book_search_mview
         <where>
           <if test="q != null and q != ''">
             AND (title ILIKE CONCAT('%', #{q}, '%')
               OR author ILIKE CONCAT('%', #{q}, '%')
               OR isbn ILIKE CONCAT(#{q}, '%'))
           </if>
           <if test="clc != null and clc != ''">
             AND classification LIKE CONCAT(#{clc}, '%')
           </if>
           <if test="school != null and school != ''">
             AND school_schema = #{school}
           </if>
         </where>
        </script>
        """)
    long count(@Param("q") String q, @Param("clc") String clc, @Param("school") String school);
}
