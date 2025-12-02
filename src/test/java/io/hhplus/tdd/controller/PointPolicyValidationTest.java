package io.hhplus.tdd.controller;

import io.hhplus.tdd.common.constant.ErrorCode;
import io.hhplus.tdd.common.constant.PointPolicy;
import io.hhplus.tdd.common.exception.BusinessException;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.repository.PointHistoryRepositoryImpl;
import io.hhplus.tdd.repository.UserPointRepositoryImpl;
import io.hhplus.tdd.service.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 포인트 정책 및 오류 응답 검증 테스트
 * HTTP 응답 코드와 에러 메시지의 일관성을 검증합니다.
 */
class PointPolicyValidationTest {

    private PointController pointController;
    private PointService pointService;

    @BeforeEach
    void setUp() {
        UserPointTable userPointTable = new UserPointTable();
        PointHistoryTable pointHistoryTable = new PointHistoryTable();
        
        pointService = new PointService(
            new UserPointRepositoryImpl(userPointTable),
            new PointHistoryRepositoryImpl(pointHistoryTable)
        );
        pointController = new PointController(pointService);
    }

    // ==================== 사용자 ID 정책 검증 ====================
    
    @Test
    @DisplayName("유효하지 않은 사용자 ID로 충전 시 INVALID_USER_ID 에러")
    void charge_WithInvalidUserId_ShouldThrowInvalidUserIdError() {
        // given: 유효 범위를 벗어난 사용자 ID
        long invalidUserId = -1L;
        long amount = 1000L;

        // when & then: INVALID_USER_ID 에러 발생
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            pointController.charge(invalidUserId, amount);
        });
        
        assertEquals(ErrorCode.INVALID_USER_ID, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("유효하지 않은 사용자 ID"));
    }

    @Test
    @DisplayName("최대 사용자 ID 초과 시 INVALID_USER_ID 에러")
    void use_WithExceededUserId_ShouldThrowInvalidUserIdError() {
        // given: 최대 범위를 초과한 사용자 ID
        long exceededUserId = PointPolicy.MAX_USER_ID + 1;
        long amount = 100L;

        // when & then: INVALID_USER_ID 에러 발생
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            pointController.use(exceededUserId, amount);
        });
        
        assertEquals(ErrorCode.INVALID_USER_ID, exception.getErrorCode());
    }

    // ==================== 충전 금액 정책 검증 ====================
    
    @Test
    @DisplayName("최소 충전 금액 미만 시 BELOW_MIN_AMOUNT 에러")
    void charge_WithBelowMinAmount_ShouldThrowBelowMinAmountError() {
        // given: 최소 금액 미만
        long userId = 1L;
        long belowMinAmount = PointPolicy.MIN_AMOUNT - 1;

        // when & then: BELOW_MIN_AMOUNT 에러 발생
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            pointController.charge(userId, belowMinAmount);
        });
        
        assertEquals(ErrorCode.BELOW_MIN_AMOUNT, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("최소 충전 금액"));
        assertTrue(exception.getMessage().contains(String.valueOf(PointPolicy.MIN_AMOUNT)));
    }

    @Test
    @DisplayName("최대 충전 금액 초과 시 EXCEED_MAX_CHARGE_AMOUNT 에러")
    void charge_WithExceedMaxAmount_ShouldThrowExceedMaxChargeAmountError() {
        // given: 최대 충전 금액 초과
        long userId = 1L;
        long exceedMaxAmount = PointPolicy.MAX_CHARGE_AMOUNT + 1;

        // when & then: EXCEED_MAX_CHARGE_AMOUNT 에러 발생
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            pointController.charge(userId, exceedMaxAmount);
        });
        
        assertEquals(ErrorCode.EXCEED_MAX_CHARGE_AMOUNT, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("최대 충전 금액"));
        assertTrue(exception.getMessage().contains(String.valueOf(PointPolicy.MAX_CHARGE_AMOUNT)));
    }

    @Test
    @DisplayName("단위가 맞지 않는 충전 금액 시 INVALID_AMOUNT_UNIT 에러")
    void charge_WithInvalidAmountUnit_ShouldThrowInvalidAmountUnitError() {
        // given: 최소 단위로 나누어떨어지지 않는 금액
        long userId = 1L;
        long invalidUnitAmount = PointPolicy.MIN_AMOUNT + 1; // 11포인트 (10포인트 단위가 아님)

        // when & then: INVALID_AMOUNT_UNIT 에러 발생
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            pointController.charge(userId, invalidUnitAmount);
        });
        
        assertEquals(ErrorCode.INVALID_AMOUNT_UNIT, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("단위로만 가능"));
        assertTrue(exception.getMessage().contains(String.valueOf(PointPolicy.MIN_AMOUNT)));
    }

    // ==================== 사용 금액 정책 검증 ====================

    @Test
    @DisplayName("최대 사용 금액 초과 시 EXCEED_MAX_USE_AMOUNT 에러")
    void use_WithExceedMaxUseAmount_ShouldThrowExceedMaxUseAmountError() {
        // given: 최대 사용 금액 초과
        long userId = 1L;
        long exceedMaxUseAmount = PointPolicy.MAX_USE_AMOUNT + 1;

        // when & then: EXCEED_MAX_USE_AMOUNT 에러 발생
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            pointController.use(userId, exceedMaxUseAmount);
        });
        
        assertEquals(ErrorCode.EXCEED_MAX_USE_AMOUNT, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("최대 사용 금액"));
        assertTrue(exception.getMessage().contains(String.valueOf(PointPolicy.MAX_USE_AMOUNT)));
    }

    @Test
    @DisplayName("단위가 맞지 않는 사용 금액 시 INVALID_AMOUNT_UNIT 에러")
    void use_WithInvalidAmountUnit_ShouldThrowInvalidAmountUnitError() {
        // given: 충분한 포인트를 충전한 후, 단위가 맞지 않는 사용 금액
        long userId = 1L;
        pointController.charge(userId, 1000L); // 충분한 포인트 충전
        
        long invalidUnitAmount = PointPolicy.MIN_AMOUNT + 1; // 11포인트

        // when & then: INVALID_AMOUNT_UNIT 에러 발생
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            pointController.use(userId, invalidUnitAmount);
        });
        
        assertEquals(ErrorCode.INVALID_AMOUNT_UNIT, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("사용 금액은"));
        assertTrue(exception.getMessage().contains("단위로만 가능"));
    }

    // ==================== 잔고 관련 정책 검증 ====================

    @Test
    @DisplayName("최대 잔고 초과 충전 시 EXCEED_MAX_BALANCE 에러")
    void charge_WithExceedMaxBalance_ShouldThrowExceedMaxBalanceError() {
        // given: 최대 잔고에 가까운 포인트를 충전
        long userId = 1L;
        long initialCharge = PointPolicy.MAX_BALANCE - 100L;
        pointController.charge(userId, initialCharge);
        
        long additionalCharge = 200L; // 최대 잔고 초과하는 충전

        // when & then: EXCEED_MAX_BALANCE 에러 발생
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            pointController.charge(userId, additionalCharge);
        });
        
        assertEquals(ErrorCode.EXCEED_MAX_BALANCE, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("최대 보유 가능 포인트"));
        assertTrue(exception.getMessage().contains(String.valueOf(PointPolicy.MAX_BALANCE)));
    }

    @Test
    @DisplayName("잔고 부족 시 INSUFFICIENT_BALANCE 에러")
    void use_WithInsufficientBalance_ShouldThrowInsufficientBalanceError() {
        // given: 잔고보다 많은 포인트 사용 시도
        long userId = 1L;
        pointController.charge(userId, 100L); // 100포인트 충전
        
        long useAmount = 200L; // 잔고보다 많은 사용

        // when & then: INSUFFICIENT_BALANCE 에러 발생
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            pointController.use(userId, useAmount);
        });
        
        assertEquals(ErrorCode.INSUFFICIENT_BALANCE, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("포인트 잔고가 부족"));
        assertTrue(exception.getMessage().contains("현재: 100"));
        assertTrue(exception.getMessage().contains("사용 시도: 200"));
    }

    // ==================== 정책 경계값 테스트 ====================

    @Test
    @DisplayName("정책 경계값에서 정상 동작 - 최소 금액")
    void charge_WithMinAmount_ShouldSucceed() {
        // given: 정확히 최소 금액
        long userId = 1L;
        long minAmount = PointPolicy.MIN_AMOUNT;

        // when: 최소 금액 충전
        pointController.charge(userId, minAmount);

        // then: 정상 처리
        assertEquals(minAmount, pointController.point(userId).point());
    }

    @Test
    @DisplayName("정책 경계값에서 정상 동작 - 최대 충전 금액")
    void charge_WithMaxChargeAmount_ShouldSucceed() {
        // given: 정확히 최대 충전 금액
        long userId = 1L;
        long maxChargeAmount = PointPolicy.MAX_CHARGE_AMOUNT;

        // when: 최대 충전 금액으로 충전
        pointController.charge(userId, maxChargeAmount);

        // then: 정상 처리
        assertEquals(maxChargeAmount, pointController.point(userId).point());
    }

    @Test
    @DisplayName("정책 경계값에서 정상 동작 - 최대 사용 금액")
    void use_WithMaxUseAmount_ShouldSucceed() {
        // given: 충분한 포인트 충전 후 최대 사용 금액
        long userId = 1L;
        long maxUseAmount = PointPolicy.MAX_USE_AMOUNT;
        pointController.charge(userId, maxUseAmount + 100L); // 충분한 포인트 충전

        // when: 최대 사용 금액으로 사용
        pointController.use(userId, maxUseAmount);

        // then: 정상 처리
        assertEquals(100L, pointController.point(userId).point());
    }
}