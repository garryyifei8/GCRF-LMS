package com.gcrf.library.book.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcrf.library.book.dto.response.IsbnLookupVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * ISBN API客户端
 * 使用Open Library API进行ISBN查询
 *
 * @author GCRF Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IsbnApiClient {

    private static final String OPEN_LIBRARY_API = "https://openlibrary.org/api/books?bibkeys=ISBN:%s&format=json&jscmd=data";
    private static final String OPEN_LIBRARY_COVERS = "https://covers.openlibrary.org/b/isbn/%s-L.jpg";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 通过ISBN查询图书信息
     *
     * @param isbn ISBN号
     * @return 图书信息VO
     */
    public IsbnLookupVO lookupByIsbn(String isbn) {
        log.info("通过ISBN查询图书信息: {}", isbn);

        // 清理ISBN格式
        String cleanIsbn = cleanIsbn(isbn);

        try {
            // 调用Open Library API
            String url = String.format(OPEN_LIBRARY_API, cleanIsbn);
            String response = restTemplate.getForObject(url, String.class);

            if (response == null || response.equals("{}")) {
                log.info("ISBN未找到图书信息: {}", isbn);
                return IsbnLookupVO.notFound(isbn);
            }

            // 解析响应
            JsonNode root = objectMapper.readTree(response);
            String key = "ISBN:" + cleanIsbn;
            JsonNode bookData = root.get(key);

            if (bookData == null) {
                log.info("ISBN未找到图书信息: {}", isbn);
                return IsbnLookupVO.notFound(isbn);
            }

            return parseOpenLibraryResponse(isbn, cleanIsbn, bookData);

        } catch (Exception e) {
            log.error("ISBN查询失败: isbn={}, error={}", isbn, e.getMessage(), e);
            return IsbnLookupVO.notFound(isbn);
        }
    }

    /**
     * 解析Open Library API响应
     */
    private IsbnLookupVO parseOpenLibraryResponse(String originalIsbn, String cleanIsbn, JsonNode bookData) {
        IsbnLookupVO.IsbnLookupVOBuilder builder = IsbnLookupVO.builder()
                .isbn(originalIsbn)
                .found(true)
                .source("Open Library");

        // 标题
        if (bookData.has("title")) {
            builder.title(bookData.get("title").asText());
        }

        // 副标题
        if (bookData.has("subtitle")) {
            builder.subtitle(bookData.get("subtitle").asText());
        }

        // 作者
        if (bookData.has("authors")) {
            List<String> authors = new ArrayList<>();
            for (JsonNode author : bookData.get("authors")) {
                if (author.has("name")) {
                    authors.add(author.get("name").asText());
                }
            }
            builder.authors(authors);
        }

        // 出版社
        if (bookData.has("publishers")) {
            JsonNode publishers = bookData.get("publishers");
            if (publishers.isArray() && publishers.size() > 0) {
                JsonNode firstPublisher = publishers.get(0);
                if (firstPublisher.has("name")) {
                    builder.publisher(firstPublisher.get("name").asText());
                }
            }
        }

        // 出版日期
        if (bookData.has("publish_date")) {
            builder.publishDate(bookData.get("publish_date").asText());
        }

        // 页数
        if (bookData.has("number_of_pages")) {
            builder.pages(bookData.get("number_of_pages").asInt());
        }

        // 封面图片
        if (bookData.has("cover")) {
            JsonNode cover = bookData.get("cover");
            if (cover.has("large")) {
                builder.coverUrl(cover.get("large").asText());
            } else if (cover.has("medium")) {
                builder.coverUrl(cover.get("medium").asText());
            } else if (cover.has("small")) {
                builder.coverUrl(cover.get("small").asText());
            }
        } else {
            // 使用默认封面URL
            builder.coverUrl(String.format(OPEN_LIBRARY_COVERS, cleanIsbn));
        }

        // 分类
        if (bookData.has("subjects")) {
            List<String> categories = new ArrayList<>();
            for (JsonNode subject : bookData.get("subjects")) {
                if (subject.has("name")) {
                    categories.add(subject.get("name").asText());
                }
            }
            builder.categories(categories);
        }

        log.info("ISBN查询成功: isbn={}, title={}", originalIsbn, builder.build().getTitle());
        return builder.build();
    }

    /**
     * 清理ISBN格式（移除连字符和空格）
     */
    private String cleanIsbn(String isbn) {
        if (isbn == null) {
            return "";
        }
        return isbn.replaceAll("[\\s-]", "");
    }
}
