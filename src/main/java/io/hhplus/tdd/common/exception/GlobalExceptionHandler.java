package io.hhplus.tdd.common.exception;

import io.hhplus.tdd.common.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 전역 예외 처리를 담당하는 컨트롤러 어드바이스
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * 비즈니스 로직 관련 예외 처리
     * 예: 포인트 잔고 부족, 유효하지 않은 사용자 등
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.warn("비즈니스 예외 발생: {}", e.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            e.getErrorCode(),
            e.getMessage()
        );
        
        return ResponseEntity
                .status(e.getHttpStatus())
                .body(errorResponse);
    }

    /**
     * 유효성 검증 예외 처리
     * 예: 잘못된 파라미터, 필수값 누락 등
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("파라미터 유효성 검증 실패: {}", e.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            "INVALID_PARAMETER",
            e.getMessage()
        );
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * 상태 관련 예외 처리  
     * 예: 이미 처리된 요청, 상태 변경 불가 등
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e) {
        log.warn("상태 관련 예외 발생: {}", e.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            "INVALID_STATE",
            e.getMessage()
        );
        
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(errorResponse);
    }

    /**
     * 예상하지 못한 시스템 예외 처리
     * 모든 예외의 최종 처리자
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("예상하지 못한 시스템 예외 발생", e);
        
        ErrorResponse errorResponse = ErrorResponse.of(
            "INTERNAL_SERVER_ERROR",
            "서버에서 예상하지 못한 오류가 발생했습니다."
        );
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }
}