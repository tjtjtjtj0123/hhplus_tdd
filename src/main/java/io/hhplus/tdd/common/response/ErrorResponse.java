package io.hhplus.tdd.common.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * API 에러 응답을 위한 공통 응답 객체
 * 
 * 실무에서 자주 사용되는 에러 응답 형태:
 * - code: 에러 코드 (예: INVALID_PARAMETER, INSUFFICIENT_BALANCE 등)
 * - message: 사용자에게 보여줄 에러 메시지
 * - timestamp: 에러 발생 시간
 * - path: 에러가 발생한 API 경로 (선택사항)
 */
@Builder
public record ErrorResponse(
        String code,
        String message,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime timestamp
) {
    
    /**
     * 기본 에러 응답 생성
     * 
     * @param code 에러 코드
     * @param message 에러 메시지
     * @return ErrorResponse 객체
     */
    public static ErrorResponse of(String code, String message) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 간단한 에러 응답 생성 (코드 없이)
     * 
     * @param message 에러 메시지
     * @return ErrorResponse 객체
     */
    public static ErrorResponse of(String message) {
        return of("ERROR", message);
    }
}