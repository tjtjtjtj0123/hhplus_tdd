package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PointController의 통합 테스트 클래스
 * 컨트롤러와 서비스가 함께 동작하는 것을 테스트합니다.
 */
class PointControllerTest {

    private PointController pointController;
    private UserPointTable userPointTable;
    private PointHistoryTable pointHistoryTable;

    @BeforeEach
    void setUp() {
        // 각 테스트마다 새로운 인스턴스를 생성하여 테스트 간 격리를 보장
        userPointTable = new UserPointTable();
        pointHistoryTable = new PointHistoryTable();
        PointService pointService = new PointService(userPointTable, pointHistoryTable);
        pointController = new PointController(pointService);
    }

    @Test
    @DisplayName("존재하지 않는 유저의 포인트 조회 - 빈 포인트 반환")
    void point_WhenUserNotExists_ShouldReturnEmptyPoint() {
        // given: 존재하지 않는 유저 ID
        long userId = 999L;

        // when: 포인트 조회 API 호출
        UserPoint result = pointController.point(userId);

        // then: 빈 포인트 정보 반환
        assertNotNull(result);
        assertEquals(userId, result.id());
        assertEquals(0L, result.point());
    }

    @Test
    @DisplayName("포인트 충전 후 조회 - 정확한 포인트 반환")
    void point_AfterCharge_ShouldReturnCorrectPoint() {
        // given: 유저가 포인트를 충전
        long userId = 1L;
        long chargeAmount = 1000L;
        pointController.charge(userId, chargeAmount);

        // when: 포인트 조회 API 호출
        UserPoint result = pointController.point(userId);

        // then: 충전된 포인트가 정확히 반환
        assertEquals(userId, result.id());
        assertEquals(chargeAmount, result.point());
    }

    @Test
    @DisplayName("존재하지 않는 유저의 포인트 내역 조회 - 빈 리스트 반환")
    void history_WhenUserNotExists_ShouldReturnEmptyList() {
        // given: 존재하지 않는 유저 ID
        long userId = 999L;

        // when: 포인트 내역 조회 API 호출
        List<PointHistory> result = pointController.history(userId);

        // then: 빈 리스트 반환
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("포인트 충전 후 내역 조회 - 충전 기록 확인")
    void history_AfterCharge_ShouldReturnChargeHistory() {
        // given: 유저가 포인트를 충전
        long userId = 1L;
        long chargeAmount = 500L;
        pointController.charge(userId, chargeAmount);

        // when: 포인트 내역 조회 API 호출  
        List<PointHistory> result = pointController.history(userId);

        // then: 충전 내역이 올바르게 기록
        assertEquals(1, result.size());
        PointHistory history = result.get(0);
        assertEquals(userId, history.userId());
        assertEquals(chargeAmount, history.amount());
        assertEquals(TransactionType.CHARGE, history.type());
    }

    @Test
    @DisplayName("포인트 충전 성공 - 증가된 포인트 반환")
    void charge_WithValidAmount_ShouldReturnIncreasedPoint() {
        // given: 유저 ID와 충전할 금액
        long userId = 1L;
        long chargeAmount = 1000L;

        // when: 포인트 충전 API 호출
        UserPoint result = pointController.charge(userId, chargeAmount);

        // then: 충전된 포인트 정보 반환
        assertEquals(userId, result.id());
        assertEquals(chargeAmount, result.point());
        assertTrue(result.updateMillis() > 0);
    }

    @Test
    @DisplayName("기존 포인트에 추가 충전 - 누적된 포인트 반환")
    void charge_WithExistingPoint_ShouldReturnAccumulatedPoint() {
        // given: 기존에 포인트가 있는 유저
        long userId = 1L;
        long firstChargeAmount = 500L;
        long secondChargeAmount = 300L;
        
        // 첫 번째 충전
        pointController.charge(userId, firstChargeAmount);

        // when: 두 번째 충전
        UserPoint result = pointController.charge(userId, secondChargeAmount);

        // then: 누적된 포인트 반환
        assertEquals(userId, result.id());
        assertEquals(firstChargeAmount + secondChargeAmount, result.point());
    }

    @Test
    @DisplayName("충분한 잔고로 포인트 사용 - 감소된 포인트 반환")
    void use_WithSufficientBalance_ShouldReturnDecreasedPoint() {
        // given: 충분한 포인트가 있는 유저
        long userId = 1L;
        long chargeAmount = 1000L;
        long useAmount = 300L;
        
        // 먼저 포인트 충전
        pointController.charge(userId, chargeAmount);

        // when: 포인트 사용 API 호출
        UserPoint result = pointController.use(userId, useAmount);

        // then: 사용 후 포인트 반환
        assertEquals(userId, result.id());
        assertEquals(chargeAmount - useAmount, result.point());
    }

    @Test
    @DisplayName("포인트 사용 후 내역 조회 - 사용 기록 확인") 
    void history_AfterUse_ShouldReturnUseHistory() {
        // given: 유저가 포인트를 충전하고 사용
        long userId = 1L;
        long chargeAmount = 1000L;
        long useAmount = 300L;
        
        pointController.charge(userId, chargeAmount);
        pointController.use(userId, useAmount);

        // when: 포인트 내역 조회
        List<PointHistory> result = pointController.history(userId);

        // then: 충전과 사용 내역이 모두 기록
        assertEquals(2, result.size());
        
        // 첫 번째 기록: 충전
        PointHistory chargeHistory = result.get(0);
        assertEquals(TransactionType.CHARGE, chargeHistory.type());
        assertEquals(chargeAmount, chargeHistory.amount());
        
        // 두 번째 기록: 사용
        PointHistory useHistory = result.get(1);
        assertEquals(TransactionType.USE, useHistory.type());
        assertEquals(useAmount, useHistory.amount());
    }

    @Test
    @DisplayName("복합 시나리오 - 충전, 사용, 재충전, 재사용의 완전한 플로우")
    void complexFlow_ChargeUseChargeUse_ShouldMaintainConsistency() {
        // given: 유저 ID
        long userId = 1L;

        // when: 복합적인 포인트 조작
        // 1. 1000 포인트 충전
        UserPoint afterCharge1 = pointController.charge(userId, 1000L);
        assertEquals(1000L, afterCharge1.point());

        // 2. 300 포인트 사용
        UserPoint afterUse1 = pointController.use(userId, 300L);
        assertEquals(700L, afterUse1.point());

        // 3. 500 포인트 추가 충전  
        UserPoint afterCharge2 = pointController.charge(userId, 500L);
        assertEquals(1200L, afterCharge2.point());

        // 4. 200 포인트 사용
        UserPoint afterUse2 = pointController.use(userId, 200L);
        assertEquals(1000L, afterUse2.point());

        // then: 최종 상태 검증
        UserPoint finalPoint = pointController.point(userId);
        assertEquals(1000L, finalPoint.point());

        // 내역 검증 - 4개의 거래 내역이 있어야 함
        List<PointHistory> histories = pointController.history(userId);
        assertEquals(4, histories.size());
        
        // 거래 내역 순서 검증
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
    @DisplayName("여러 유저의 포인트 독립적 관리 - 유저간 포인트 격리 확인")
    void multipleUsers_ShouldMaintainSeparatePoints() {
        // given: 여러 유저
        long user1Id = 1L;
        long user2Id = 2L;
        long user1ChargeAmount = 1000L;
        long user2ChargeAmount = 2000L;

        // when: 각 유저별로 포인트 충전
        pointController.charge(user1Id, user1ChargeAmount);
        pointController.charge(user2Id, user2ChargeAmount);

        // then: 각 유저의 포인트가 독립적으로 관리
        UserPoint user1Point = pointController.point(user1Id);
        UserPoint user2Point = pointController.point(user2Id);
        
        assertEquals(user1ChargeAmount, user1Point.point());
        assertEquals(user2ChargeAmount, user2Point.point());

        // 내역도 독립적으로 관리
        List<PointHistory> user1Histories = pointController.history(user1Id);
        List<PointHistory> user2Histories = pointController.history(user2Id);
        
        assertEquals(1, user1Histories.size());
        assertEquals(1, user2Histories.size());
        assertEquals(user1ChargeAmount, user1Histories.get(0).amount());
        assertEquals(user2ChargeAmount, user2Histories.get(0).amount());
    }
}