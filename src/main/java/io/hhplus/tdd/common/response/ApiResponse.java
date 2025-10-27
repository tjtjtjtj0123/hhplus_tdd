package io.hhplus.tdd.common.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * API 성공 응답을 위한 공통 응답 객체
 * 
 * 실무에서 자주 사용되는 성공 응답 형태:
 * - success: 성공 여부 (true/false)
 * - data: 실제 응답 데이터
 * - message: 성공 메시지 (선택사항)
 * - timestamp: 응답 시간
 */
@Builder
public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime timestamp
) {
    
    /**
     * 성공 응답 생성 (데이터 + 메시지)
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 성공 응답 생성 (데이터만)
     */
    public static <T> ApiResponse<T> success(T data) {
        return success(data, "요청이 성공적으로 처리되었습니다.");
    }
    
    /**
     * 성공 응답 생성 (메시지만)
     */
    public static <T> ApiResponse<T> success(String message) {
        return success(null, message);
    }
}