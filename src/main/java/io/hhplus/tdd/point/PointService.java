package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 포인트 관련 비즈니스 로직을 처리하는 서비스 클래스
 */
@Service
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    /**
     * 특정 유저의 포인트를 조회합니다.
     * 
     * @param userId 조회할 유저 ID
     * @return 유저의 포인트 정보
     */
    public UserPoint getUserPoint(long userId) {
        return userPointTable.selectById(userId);
    }

    /**
     * 특정 유저의 포인트 충전/사용 내역을 조회합니다.
     * 
     * @param userId 조회할 유저 ID
     * @return 포인트 변경 내역 리스트
     */
    public List<PointHistory> getUserPointHistories(long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }

    /**
     * 특정 유저의 포인트를 충전합니다.
     * 
     * @param userId 충전할 유저 ID
     * @param amount 충전할 포인트 금액 (양수)
     * @return 충전 후 유저의 포인트 정보
     * @throws IllegalArgumentException 충전 금액이 0 이하인 경우
     */
    public UserPoint chargeUserPoint(long userId, long amount) {
        // 충전 금액 유효성 검증
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }

        // 현재 포인트 조회
        UserPoint currentPoint = userPointTable.selectById(userId);
        
        // 새로운 포인트 계산
        long newPointAmount = currentPoint.point() + amount;
        
        // 포인트 업데이트
        UserPoint updatedPoint = userPointTable.insertOrUpdate(userId, newPointAmount);
        
        // 포인트 변경 내역 기록
        pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, updatedPoint.updateMillis());
        
        return updatedPoint;
    }

    /**
     * 특정 유저의 포인트를 사용합니다.
     * 
     * @param userId 포인트를 사용할 유저 ID
     * @param amount 사용할 포인트 금액 (양수)
     * @return 사용 후 유저의 포인트 정보
     * @throws IllegalArgumentException 사용 금액이 0 이하인 경우
     * @throws IllegalStateException 잔고가 부족한 경우
     */
    public UserPoint useUserPoint(long userId, long amount) {
        // 사용 금액 유효성 검증
        if (amount <= 0) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다.");
        }

        // 현재 포인트 조회
        UserPoint currentPoint = userPointTable.selectById(userId);
        
        // 잔고 확인
        if (currentPoint.point() < amount) {
            throw new IllegalStateException("포인트 잔고가 부족합니다. 현재 잔고: " + currentPoint.point() + ", 사용 요청: " + amount);
        }
        
        // 새로운 포인트 계산
        long newPointAmount = currentPoint.point() - amount;
        
        // 포인트 업데이트
        UserPoint updatedPoint = userPointTable.insertOrUpdate(userId, newPointAmount);
        
        // 포인트 변경 내역 기록
        pointHistoryTable.insert(userId, amount, TransactionType.USE, updatedPoint.updateMillis());
        
        return updatedPoint;
    }
}