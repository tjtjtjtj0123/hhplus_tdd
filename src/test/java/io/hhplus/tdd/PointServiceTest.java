package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.domain.PointHistory;
import io.hhplus.tdd.domain.TransactionType;
import io.hhplus.tdd.domain.UserPoint;
import io.hhplus.tdd.repository.PointHistoryRepository;
import io.hhplus.tdd.repository.PointHistoryRepositoryImpl;
import io.hhplus.tdd.repository.UserPointRepository;
import io.hhplus.tdd.repository.UserPointRepositoryImpl;
import io.hhplus.tdd.service.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PointService의 단위 테스트 클래스
 * 각 기능(포인트 조회, 내역 조회, 충전, 사용)에 대한 테스트를 작성합니다.
 */
class PointServiceTest {

    private PointService pointService;
    private UserPointRepository userPointRepository;
    private PointHistoryRepository pointHistoryRepository;

    @BeforeEach
    void setUp() {
        // 각 테스트마다 새로운 인스턴스를 생성하여 테스트 간 격리를 보장
        UserPointTable userPointTable = new UserPointTable();
        PointHistoryTable pointHistoryTable = new PointHistoryTable();
        
        // Repository 구현체 생성
        userPointRepository = new UserPointRepositoryImpl(userPointTable);
        pointHistoryRepository = new PointHistoryRepositoryImpl(pointHistoryTable);
        
        // Service 생성
        pointService = new PointService(userPointRepository, pointHistoryRepository);
    }

    @Test
    @DisplayName("존재하지 않는 유저 ID로 포인트 조회 시 빈 UserPoint 반환")
    void getUserPoint_WhenUserNotExists_ShouldReturnEmptyUserPoint() {
        // given: 존재하지 않는 유저 ID
        long nonExistentUserId = 999L;

        // when: 포인트 조회
        UserPoint result = pointService.getUserPoint(nonExistentUserId);

        // then: 빈 UserPoint가 반환되어야 함 (포인트 0, 유저 ID는 동일)
        assertNotNull(result);
        assertEquals(nonExistentUserId, result.id());
        assertEquals(0L, result.point());
        assertTrue(result.updateMillis() > 0); // 타임스탬프는 0보다 큰 값
    }

    @Test
    @DisplayName("기존 유저의 포인트 조회 성공")
    void getUserPoint_WhenUserExists_ShouldReturnCorrectUserPoint() {
        // given: 기존에 포인트가 있는 유저
        long userId = 1L;
        long initialPoint = 1000L;
        userPointRepository.insertOrUpdate(userId, initialPoint);

        // when: 포인트 조회
        UserPoint result = pointService.getUserPoint(userId);

        // then: 올바른 포인트 정보가 반환되어야 함
        assertEquals(userId, result.id());
        assertEquals(initialPoint, result.point());
    }

    @Test
    @DisplayName("존재하지 않는 유저 ID로 포인트 내역 조회 시 빈 리스트 반환")
    void getUserPointHistories_WhenUserNotExists_ShouldReturnEmptyList() {
        // given: 존재하지 않는 유저 ID
        long nonExistentUserId = 999L;

        // when: 포인트 내역 조회
        List<PointHistory> result = pointService.getUserPointHistories(nonExistentUserId);

        // then: 빈 리스트가 반환되어야 함
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("포인트 충전 후 내역 조회 시 충전 기록 확인")
    void getUserPointHistories_AfterCharge_ShouldReturnChargeHistory() {
        // given: 유저가 포인트를 충전
        long userId = 1L;
        long chargeAmount = 500L;
        pointService.chargeUserPoint(userId, chargeAmount);

        // when: 포인트 내역 조회
        List<PointHistory> result = pointService.getUserPointHistories(userId);

        // then: 충전 내역이 기록되어야 함
        assertEquals(1, result.size());
        PointHistory history = result.get(0);
        assertEquals(userId, history.userId());
        assertEquals(chargeAmount, history.amount());
        assertEquals(TransactionType.CHARGE, history.type());
    }

    @Test
    @DisplayName("유효한 금액으로 포인트 충전 성공")
    void chargeUserPoint_WithValidAmount_ShouldIncreasePoint() {
        // given: 유저와 충전할 금액
        long userId = 1L;
        long chargeAmount = 1000L;

        // when: 포인트 충전
        UserPoint result = pointService.chargeUserPoint(userId, chargeAmount);

        // then: 포인트가 증가해야 함
        assertEquals(userId, result.id());
        assertEquals(chargeAmount, result.point());
        
        // 포인트 내역도 확인
        List<PointHistory> histories = pointService.getUserPointHistories(userId);
        assertEquals(1, histories.size());
        assertEquals(TransactionType.CHARGE, histories.get(0).type());
    }

    @Test
    @DisplayName("기존 포인트에 추가 충전 시 누적된 포인트 반환")
    void chargeUserPoint_WithExistingPoint_ShouldAccumulatePoint() {
        // given: 기존에 포인트가 있는 유저
        long userId = 1L;
        long initialPoint = 500L;
        long chargeAmount = 300L;
        userPointRepository.insertOrUpdate(userId, initialPoint);

        // when: 포인트 충전
        UserPoint result = pointService.chargeUserPoint(userId, chargeAmount);

        // then: 기존 포인트 + 충전 포인트 = 총 포인트
        assertEquals(userId, result.id());
        assertEquals(initialPoint + chargeAmount, result.point());
    }

    @Test
    @DisplayName("0 이하의 금액으로 포인트 충전 시 예외 발생")
    void chargeUserPoint_WithInvalidAmount_ShouldThrowException() {
        // given: 유저 ID와 잘못된 충전 금액들
        long userId = 1L;

        // when & then: 0 이하의 금액으로 충전 시 예외 발생
        assertThrows(IllegalArgumentException.class, () -> {
            pointService.chargeUserPoint(userId, 0);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            pointService.chargeUserPoint(userId, -100);
        });
    }

    @Test
    @DisplayName("충분한 잔고로 포인트 사용 성공")
    void useUserPoint_WithSufficientBalance_ShouldDecreasePoint() {
        // given: 충분한 포인트가 있는 유저
        long userId = 1L;
        long initialPoint = 1000L;
        long useAmount = 300L;
        userPointRepository.insertOrUpdate(userId, initialPoint);

        // when: 포인트 사용
        UserPoint result = pointService.useUserPoint(userId, useAmount);

        // then: 포인트가 감소해야 함
        assertEquals(userId, result.id());
        assertEquals(initialPoint - useAmount, result.point());
        
        // 포인트 내역도 확인
        List<PointHistory> histories = pointService.getUserPointHistories(userId);
        assertEquals(1, histories.size());
        assertEquals(TransactionType.USE, histories.get(0).type());
    }

    @Test
    @DisplayName("포인트 잔고가 부족할 때 사용 시 예외 발생")
    void useUserPoint_WithInsufficientBalance_ShouldThrowException() {
        // given: 포인트가 부족한 유저
        long userId = 1L;
        long initialPoint = 100L;
        long useAmount = 500L;
        userPointRepository.insertOrUpdate(userId, initialPoint);

        // when & then: 잔고 부족 시 예외 발생
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            pointService.useUserPoint(userId, useAmount);
        });
        
        // 예외 메시지 확인
        assertTrue(exception.getMessage().contains("포인트 잔고가 부족합니다"));
        assertTrue(exception.getMessage().contains("현재 잔고: " + initialPoint));
        assertTrue(exception.getMessage().contains("사용 요청: " + useAmount));
    }

    @Test
    @DisplayName("0 이하의 금액으로 포인트 사용 시 예외 발생")
    void useUserPoint_WithInvalidAmount_ShouldThrowException() {
        // given: 유저 ID와 잘못된 사용 금액들
        long userId = 1L;
        userPointRepository.insertOrUpdate(userId, 1000L); // 충분한 잔고 설정

        // when & then: 0 이하의 금액으로 사용 시 예외 발생
        assertThrows(IllegalArgumentException.class, () -> {
            pointService.useUserPoint(userId, 0);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            pointService.useUserPoint(userId, -100);
        });
    }

    @Test
    @DisplayName("포인트 충전과 사용을 연속으로 수행할 때 정확한 잔고와 내역 관리")
    void complexScenario_ChargeAndUse_ShouldMaintainCorrectBalanceAndHistory() {
        // given: 유저 ID
        long userId = 1L;
        
        // when: 연속된 포인트 충전 및 사용
        // 1. 1000 포인트 충전
        UserPoint afterCharge1 = pointService.chargeUserPoint(userId, 1000L);
        assertEquals(1000L, afterCharge1.point());
        
        // 2. 300 포인트 사용
        UserPoint afterUse1 = pointService.useUserPoint(userId, 300L);
        assertEquals(700L, afterUse1.point());
        
        // 3. 500 포인트 추가 충전
        UserPoint afterCharge2 = pointService.chargeUserPoint(userId, 500L);
        assertEquals(1200L, afterCharge2.point());
        
        // 4. 200 포인트 사용
        UserPoint afterUse2 = pointService.useUserPoint(userId, 200L);
        assertEquals(1000L, afterUse2.point());

        // then: 내역 확인
        List<PointHistory> histories = pointService.getUserPointHistories(userId);
        assertEquals(4, histories.size());
        
        // 내역 순서 확인 (시간순)
        assertEquals(TransactionType.CHARGE, histories.get(0).type());
        assertEquals(1000L, histories.get(0).amount());
        
        assertEquals(TransactionType.USE, histories.get(1).type());
        assertEquals(300L, histories.get(1).amount());
        
        assertEquals(TransactionType.CHARGE, histories.get(2).type());
        assertEquals(500L, histories.get(2).amount());
        
        assertEquals(TransactionType.USE, histories.get(3).type());
        assertEquals(200L, histories.get(3).amount());
    }

    @Test
    @DisplayName("동일한 금액으로 충전 후 사용 시 원래 포인트로 복귀")
    void chargeAndUseSameAmount_ShouldReturnToOriginalBalance() {
        // given: 초기 포인트가 있는 유저
        long userId = 1L;
        long initialPoint = 500L;
        long amount = 200L;
        userPointRepository.insertOrUpdate(userId, initialPoint);

        // when: 동일한 금액 충전 후 사용
        pointService.chargeUserPoint(userId, amount);
        UserPoint result = pointService.useUserPoint(userId, amount);

        // then: 원래 포인트와 같아야 함
        assertEquals(initialPoint, result.point());
        
        // 내역은 2개가 있어야 함
        List<PointHistory> histories = pointService.getUserPointHistories(userId);
        assertEquals(2, histories.size());
    }
}