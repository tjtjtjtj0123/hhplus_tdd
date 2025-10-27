package io.hhplus.tdd.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 비즈니스 로직 관련 예외를 처리하기 위한 커스텀 예외 클래스
 * 
 * 사용 예시:
 * throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE, "포인트 잔고가 부족합니다.");
 * throw new BusinessException("INVALID_USER", "존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND);
 */
public class BusinessException extends RuntimeException {
    
    private final String errorCode;
    private final HttpStatus httpStatus;

    /**
     * 에러 코드와 메시지로 예외 생성
     * 기본 HTTP 상태는 BAD_REQUEST(400)
     */
    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    /**
     * 에러 코드, 메시지, HTTP 상태로 예외 생성
     */
    public BusinessException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    /**
     * 메시지만으로 예외 생성
     * 기본 에러 코드는 "BUSINESS_ERROR"
     */
    public BusinessException(String message) {
        this("BUSINESS_ERROR", message);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}