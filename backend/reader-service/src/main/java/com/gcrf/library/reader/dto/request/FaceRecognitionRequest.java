package com.gcrf.library.reader.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 人脸识别请求
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
public class FaceRecognitionRequest {

    /**
     * 人脸图像数据（Base64编码）
     */
    @NotBlank(message = "人脸图像数据不能为空")
    private String faceImage;

    /**
     * 操作类型：REGISTER-注册, UPDATE-更新, DELETE-删除
     */
    @NotBlank(message = "操作类型不能为空")
    private String operation;
}
