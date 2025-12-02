package io.hhplus.tdd.common.constant;

/**
 * 포인트 관련 정책 상수 정의
 * 
 * 비즈니스 정책을 코드로 명시화하여 일관성 있는 검증과 관리 제공
 */
public final class PointPolicy {

    // === 포인트 금액 정책 ===
    /** 최소 충전/사용 단위 */
    public static final long MIN_AMOUNT = 10L;
    
    /** 최대 충전 금액 (한 번에) */
    public static final long MAX_CHARGE_AMOUNT = 1_000_000L;
    
    /** 최대 사용 금액 (한 번에) */
    public static final long MAX_USE_AMOUNT = 500_000L;
    
    /** 최대 보유 가능 포인트 */
    public static final long MAX_BALANCE = 10_000_000L;
    
    /** 최소 보유 포인트 (음수 방지) */
    public static final long MIN_BALANCE = 0L;

    // === 사용자 정책 ===
    /** 유효한 최소 사용자 ID */
    public static final long MIN_USER_ID = 1L;
    
    /** 유효한 최대 사용자 ID */
    public static final long MAX_USER_ID = 999_999_999L;

    // === 시스템 정책 ===
    /** 일일 최대 충전 횟수 */
    public static final int MAX_DAILY_CHARGE_COUNT = 10;
    
    /** 일일 최대 사용 횟수 */
    public static final int MAX_DAILY_USE_COUNT = 50;

    private PointPolicy() {
        // 유틸리티 클래스는 인스턴스화 방지
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 금액이 충전 정책에 맞는지 검증
     */
    public static boolean isValidChargeAmount(long amount) {
        return amount >= MIN_AMOUNT && amount <= MAX_CHARGE_AMOUNT;
    }

    /**
     * 금액이 사용 정책에 맞는지 검증
     */
    public static boolean isValidUseAmount(long amount) {
        return amount >= MIN_AMOUNT && amount <= MAX_USE_AMOUNT;
    }

    /**
     * 충전 후 잔고가 정책에 맞는지 검증
     */
    public static boolean isValidBalanceAfterCharge(long currentBalance, long chargeAmount) {
        return currentBalance + chargeAmount <= MAX_BALANCE;
    }

    /**
     * 사용자 ID가 유효한지 검증
     */
    public static boolean isValidUserId(long userId) {
        return userId >= MIN_USER_ID && userId <= MAX_USER_ID;
    }

    /**
     * 최소 단위로 나누어떨어지는지 검증
     */
    public static boolean isValidAmountUnit(long amount) {
        return amount % MIN_AMOUNT == 0;
    }
}