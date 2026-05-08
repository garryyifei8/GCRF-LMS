package com.gcrf.library.system.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Feign 反序列化 auth-service /api/v1/users 分页响应专用 DTO。
 * 仅包含我们关心的字段，多余的 Page 内部字段（orders/searchCount/...）忽略。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserPageVO {
    private List<UserVO> records;
    private Long total;
    private Long current;
    private Long size;
}
