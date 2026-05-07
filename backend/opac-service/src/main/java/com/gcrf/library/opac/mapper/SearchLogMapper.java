package com.gcrf.library.opac.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SearchLogMapper {

    @Insert("INSERT INTO gcrf_region.search_log (keyword, client_ip, result_count) "
          + "VALUES (#{keyword}, #{clientIp}, #{resultCount})")
    void insert(@Param("keyword") String keyword,
                @Param("clientIp") String clientIp,
                @Param("resultCount") long resultCount);

    @org.apache.ibatis.annotations.Select("""
        SELECT keyword_lower AS keyword, count(*) AS count
          FROM gcrf_region.search_log
         WHERE created_at >= NOW() - (#{days}::text || ' days')::INTERVAL
         GROUP BY keyword_lower
         ORDER BY count DESC, keyword_lower
         LIMIT #{limit}
        """)
    java.util.List<com.gcrf.library.opac.domain.vo.KeywordRankingVO> topKeywords(
        @org.apache.ibatis.annotations.Param("days") int days,
        @org.apache.ibatis.annotations.Param("limit") int limit);
}
