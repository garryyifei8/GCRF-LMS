package com.gcrf.library.reader.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 人脸识别响应VO
 *
 * @author GCRF Team
 * @since 2025-11-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaceRecognitionVO {

    /**
     * 读者ID
     */
    private Long readerId;

    /**
     * 是否已注册人脸
     */
    private Boolean faceRegistered;

    /**
     * 人脸注册时间
     */
    private LocalDateTime registeredAt;

    /**
     * 操作结果状态
     */
    private String status;

    /**
     * 操作结果消息
     */
    private String message;

    /**
     * 人脸特征ID（占位，待Vision Service实现）
     */
    private String faceFeatureId;

    /**
     * 创建Mock响应 - 注册成功
     */
    public static FaceRecognitionVO mockRegisterSuccess(Long readerId) {
        return FaceRecognitionVO.builder()
                .readerId(readerId)
                .faceRegistered(true)
                .registeredAt(LocalDateTime.now())
                .status("SUCCESS")
                .message("人脸注册成功（Mock）- 待Vision Service实现")
                .faceFeatureId("MOCK_FACE_" + readerId + "_" + System.currentTimeMillis())
                .build();
    }

    /**
     * 创建Mock响应 - 更新成功
     */
    public static FaceRecognitionVO mockUpdateSuccess(Long readerId) {
        return FaceRecognitionVO.builder()
                .readerId(readerId)
                .faceRegistered(true)
                .registeredAt(LocalDateTime.now())
                .status("SUCCESS")
                .message("人脸更新成功（Mock）- 待Vision Service实现")
                .faceFeatureId("MOCK_FACE_" + readerId + "_" + System.currentTimeMillis())
                .build();
    }

    /**
     * 创建Mock响应 - 删除成功
     */
    public static FaceRecognitionVO mockDeleteSuccess(Long readerId) {
        return FaceRecognitionVO.builder()
                .readerId(readerId)
                .faceRegistered(false)
                .registeredAt(null)
                .status("SUCCESS")
                .message("人脸删除成功（Mock）- 待Vision Service实现")
                .faceFeatureId(null)
                .build();
    }

    /**
     * 创建Mock响应 - 查询结果
     */
    public static FaceRecognitionVO mockQueryResult(Long readerId, boolean registered) {
        return FaceRecognitionVO.builder()
                .readerId(readerId)
                .faceRegistered(registered)
                .registeredAt(registered ? LocalDateTime.now().minusDays(30) : null)
                .status("SUCCESS")
                .message("查询成功（Mock）- 待Vision Service实现")
                .faceFeatureId(registered ? "MOCK_FACE_" + readerId : null)
                .build();
    }
}
