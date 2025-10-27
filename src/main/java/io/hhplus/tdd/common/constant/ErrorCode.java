package io.hhplus.tdd.common.constant;

/**
 * API 에러 코드 상수 정의
 * 
 * 실무에서 자주 사용하는 에러 코드 패턴:
 * - 비즈니스 로직 관련: BUSINESS_
 * - 유효성 검증 관련: VALIDATION_
 * - 시스템 관련: SYSTEM_
 * - 인증/인가 관련: AUTH_
 */
public final class ErrorCode {

    // === 포인트 관련 에러 코드 ===
    public static final String INSUFFICIENT_BALANCE = "POINT_001";
    public static final String INVALID_POINT_AMOUNT = "POINT_002";
    public static final String POINT_OPERATION_FAILED = "POINT_003";

    // === 사용자 관련 에러 코드 ===
    public static final String USER_NOT_FOUND = "USER_001";
    public static final String INVALID_USER_ID = "USER_002";

    // === 유효성 검증 관련 에러 코드 ===
    public static final String INVALID_PARAMETER = "VALIDATION_001";
    public static final String REQUIRED_PARAMETER_MISSING = "VALIDATION_002";
    public static final String INVALID_REQUEST_FORMAT = "VALIDATION_003";

    // === 시스템 관련 에러 코드 ===
    public static final String INTERNAL_SERVER_ERROR = "SYSTEM_001";
    public static final String DATABASE_ERROR = "SYSTEM_002";
    public static final String EXTERNAL_API_ERROR = "SYSTEM_003";

    // === 상태 관련 에러 코드 ===
    public static final String INVALID_STATE = "STATE_001";
    public static final String OPERATION_NOT_ALLOWED = "STATE_002";

    private ErrorCode() {
        // 유틸리티 클래스는 인스턴스화 방지
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}