package io.hhplus.tdd.common.util;

import java.util.regex.Pattern;

/**
 * 유효성 검증을 위한 공통 유틸리티 클래스
 * 
 * 실무에서 자주 사용되는 검증 패턴들을 모아놓은 클래스
 */
public final class ValidationUtil {

    // 이메일 정규식 패턴
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    
    // 휴대폰 번호 정규식 패턴
    private static final Pattern PHONE_PATTERN = 
        Pattern.compile("^01[016789]\\d{7,8}$");

    private ValidationUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 문자열이 비어있는지 확인
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 문자열이 비어있지 않은지 확인
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * 이메일 형식이 올바른지 확인
     */
    public static boolean isValidEmail(String email) {
        if (isEmpty(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * 휴대폰 번호 형식이 올바른지 확인
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (isEmpty(phoneNumber)) {
            return false;
        }
        return PHONE_PATTERN.matcher(phoneNumber).matches();
    }

    /**
     * 숫자가 양수인지 확인
     */
    public static boolean isPositive(long number) {
        return number > 0;
    }

    /**
     * 숫자가 범위 내에 있는지 확인
     */
    public static boolean isInRange(long number, long min, long max) {
        return number >= min && number <= max;
    }

    /**
     * 사용자 ID가 유효한지 확인 (양수)
     */
    public static boolean isValidUserId(long userId) {
        return isPositive(userId);
    }

    /**
     * 포인트 금액이 유효한지 확인 (양수이고 최대값 이하)
     */
    public static boolean isValidPointAmount(long amount) {
        return isPositive(amount) && amount <= 1_000_000L; // 최대 100만 포인트
    }
}