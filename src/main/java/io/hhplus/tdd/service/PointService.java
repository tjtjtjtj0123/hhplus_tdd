package io.hhplus.tdd.service;

import io.hhplus.tdd.common.constant.ErrorCode;
import io.hhplus.tdd.common.constant.PointPolicy;
import io.hhplus.tdd.common.exception.BusinessException;
import io.hhplus.tdd.domain.PointHistory;
import io.hhplus.tdd.domain.TransactionType;
import io.hhplus.tdd.domain.UserPoint;
import io.hhplus.tdd.repository.PointHistoryRepository;
import io.hhplus.tdd.repository.UserPointRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * 포인트 관련 비즈니스 로직을 처리하는 서비스 클래스
 * 동시성 제어를 위해 사용자별 세분화된 락(Lock)을 사용합니다.
 */
@Slf4j
@Service
public class PointService {

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;
    
    /**
     * 사용자별 락을 관리하는 ConcurrentHashMap
     * 각 사용자마다 독립적인 ReentrantLock을 할당하여 동시성 제어
     */
    private final ConcurrentHashMap<Long, ReentrantLock> userLocks = new ConcurrentHashMap<>();

    public PointService(UserPointRepository userPointRepository, PointHistoryRepository pointHistoryRepository) {
        this.userPointRepository = userPointRepository;
        this.pointHistoryRepository = pointHistoryRepository;
    }

    /**
     * 특정 사용자의 락을 가져옵니다. 없으면 새로 생성합니다.
     * 
     * @param userId 사용자 ID
     * @return 해당 사용자의 ReentrantLock
     */
    private ReentrantLock getUserLock(long userId) {
        return userLocks.computeIfAbsent(userId, k -> new ReentrantLock(true)); // fair lock 사용
    }

    /**
     * 사용자별 락을 사용하여 안전하게 작업을 실행합니다.
     * 
     * @param userId 사용자 ID
     * @param operation 실행할 작업
     * @param <T> 반환 타입
     * @return 작업 실행 결과
     */
    private <T> T executeWithUserLock(long userId, Supplier<T> operation) {
        ReentrantLock lock = getUserLock(userId);
        
        log.debug("사용자 {}의 락 획득 시도", userId);
        lock.lock();
        
        try {
            log.debug("사용자 {}의 락 획득 성공", userId);
            return operation.get();
        } finally {
            lock.unlock();
            log.debug("사용자 {}의 락 해제", userId);
        }
    }

    /**
     * 반환값이 없는 작업을 사용자별 락으로 안전하게 실행합니다.
     * 
     * @param userId 사용자 ID  
     * @param operation 실행할 작업
     */
    private void executeWithUserLock(long userId, Runnable operation) {
        executeWithUserLock(userId, () -> {
            operation.run();
            return null;
        });
    }

    /**
     * 사용자 ID 유효성 검증
     */
    private void validateUserId(long userId) {
        if (!PointPolicy.isValidUserId(userId)) {
            throw new BusinessException(ErrorCode.INVALID_USER_ID, 
                String.format("유효하지 않은 사용자 ID입니다. (범위: %d ~ %d)", 
                    PointPolicy.MIN_USER_ID, PointPolicy.MAX_USER_ID));
        }
    }

    /**
     * 충전 금액 정책 검증
     */
    private void validateChargeAmount(long amount) {
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_POINT_AMOUNT, "충전 금액은 0보다 커야 합니다.");
        }
        
        if (amount < PointPolicy.MIN_AMOUNT) {
            throw new BusinessException(ErrorCode.BELOW_MIN_AMOUNT, 
                String.format("최소 충전 금액은 %d 포인트입니다.", PointPolicy.MIN_AMOUNT));
        }
        
        if (amount > PointPolicy.MAX_CHARGE_AMOUNT) {
            throw new BusinessException(ErrorCode.EXCEED_MAX_CHARGE_AMOUNT, 
                String.format("최대 충전 금액은 %d 포인트입니다.", PointPolicy.MAX_CHARGE_AMOUNT));
        }
        
        if (!PointPolicy.isValidAmountUnit(amount)) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT_UNIT, 
                String.format("충전 금액은 %d 포인트 단위로만 가능합니다.", PointPolicy.MIN_AMOUNT));
        }
    }

    /**
     * 사용 금액 정책 검증
     */
    private void validateUseAmount(long amount) {
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_POINT_AMOUNT, "사용 금액은 0보다 커야 합니다.");
        }
        
        if (amount < PointPolicy.MIN_AMOUNT) {
            throw new BusinessException(ErrorCode.BELOW_MIN_AMOUNT, 
                String.format("최소 사용 금액은 %d 포인트입니다.", PointPolicy.MIN_AMOUNT));
        }
        
        if (amount > PointPolicy.MAX_USE_AMOUNT) {
            throw new BusinessException(ErrorCode.EXCEED_MAX_USE_AMOUNT, 
                String.format("최대 사용 금액은 %d 포인트입니다.", PointPolicy.MAX_USE_AMOUNT));
        }
        
        if (!PointPolicy.isValidAmountUnit(amount)) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT_UNIT, 
                String.format("사용 금액은 %d 포인트 단위로만 가능합니다.", PointPolicy.MIN_AMOUNT));
        }
    }

    /**
     * 충전 후 잔고 초과 검증
     */
    private void validateBalanceAfterCharge(long currentBalance, long chargeAmount) {
        if (!PointPolicy.isValidBalanceAfterCharge(currentBalance, chargeAmount)) {
            throw new BusinessException(ErrorCode.EXCEED_MAX_BALANCE, 
                String.format("최대 보유 가능 포인트는 %d 포인트입니다. (현재: %d, 충전 시도: %d)", 
                    PointPolicy.MAX_BALANCE, currentBalance, chargeAmount));
        }
    }

    /**
     * 잔고 부족 검증
     */
    private void validateSufficientBalance(long currentBalance, long useAmount) {
        if (currentBalance < useAmount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE, 
                String.format("포인트 잔고가 부족합니다. (현재: %d, 사용 시도: %d)", currentBalance, useAmount));
        }
    }

    /**
     * 특정 유저의 포인트를 조회합니다.
     * 
     * @param userId 조회할 유저 ID
     * @return 유저의 포인트 정보
     */
    public UserPoint getUserPoint(long userId) {
        return userPointRepository.selectById(userId);
    }

    /**
     * 특정 유저의 포인트 충전/사용 내역을 조회합니다.
     * 
     * @param userId 조회할 유저 ID
     * @return 포인트 변경 내역 리스트
     */
    public List<PointHistory> getUserPointHistories(long userId) {
        return pointHistoryRepository.selectAllByUserId(userId);
    }

    /**
     * 특정 유저의 포인트를 충전합니다.
     * 동시성 제어를 위해 사용자별 락을 사용하여 안전하게 처리합니다.
     * 
     * @param userId 충전할 유저 ID
     * @param amount 충전할 포인트 금액 (양수)
     * @return 충전 후 유저의 포인트 정보
     * @throws IllegalArgumentException 충전 금액이 0 이하인 경우
     */
    public UserPoint chargeUserPoint(long userId, long amount) {
        // 사용자 ID 검증
        validateUserId(userId);
        
        // 충전 금액 정책 검증
        validateChargeAmount(amount);

        return executeWithUserLock(userId, () -> {
            log.info("포인트 충전 시작 - 사용자: {}, 충전금액: {}", userId, amount);
            
            // 현재 포인트 조회
            UserPoint currentPoint = userPointRepository.selectById(userId);
            log.debug("현재 포인트: {}", currentPoint.point());
            
            // 충전 후 최대 잔고 초과 검증
            validateBalanceAfterCharge(currentPoint.point(), amount);
            
            // 새로운 포인트 계산
            long newPointAmount = currentPoint.point() + amount;
            
            // 포인트 업데이트
            UserPoint updatedPoint = userPointRepository.insertOrUpdate(userId, newPointAmount);
            
            // 포인트 변경 내역 기록
            pointHistoryRepository.insert(userId, amount, TransactionType.CHARGE, updatedPoint.updateMillis());
            
            log.info("포인트 충전 완료 - 사용자: {}, 이전: {}, 이후: {}", userId, currentPoint.point(), updatedPoint.point());
            return updatedPoint;
        });
    }

    /**
     * 특정 유저의 포인트를 사용합니다.
     * 동시성 제어를 위해 사용자별 락을 사용하여 안전하게 처리합니다.
     * 
     * @param userId 포인트를 사용할 유저 ID
     * @param amount 사용할 포인트 금액 (양수)
     * @return 사용 후 유저의 포인트 정보
     * @throws IllegalArgumentException 사용 금액이 0 이하인 경우
     * @throws IllegalStateException 잔고가 부족한 경우
     */
    public UserPoint useUserPoint(long userId, long amount) {
        // 사용자 ID 검증
        validateUserId(userId);
        
        // 사용 금액 정책 검증
        validateUseAmount(amount);

        return executeWithUserLock(userId, () -> {
            log.info("포인트 사용 시작 - 사용자: {}, 사용금액: {}", userId, amount);
            
            // 현재 포인트 조회
            UserPoint currentPoint = userPointRepository.selectById(userId);
            log.debug("현재 포인트: {}", currentPoint.point());
            
            // 잔고 부족 검증
            validateSufficientBalance(currentPoint.point(), amount);
            
            // 새로운 포인트 계산
            long newPointAmount = currentPoint.point() - amount;
            
            // 포인트 업데이트
            UserPoint updatedPoint = userPointRepository.insertOrUpdate(userId, newPointAmount);
            
            // 포인트 변경 내역 기록
            pointHistoryRepository.insert(userId, amount, TransactionType.USE, updatedPoint.updateMillis());
            
            log.info("포인트 사용 완료 - 사용자: {}, 이전: {}, 이후: {}", userId, currentPoint.point(), updatedPoint.point());
            return updatedPoint;
        });
    }

    /**
     * 현재 사용중인 락의 개수를 반환합니다. (모니터링 용도)
     * 
     * @return 사용중인 락의 개수
     */
    public int getActiveLockCount() {
        return userLocks.size();
    }

    /**
     * 지정된 사용자의 락 대기 큐 길이를 반환합니다. (모니터링 용도)
     * 
     * @param userId 사용자 ID
     * @return 해당 사용자 락의 대기 큐 길이
     */
    public int getUserLockQueueLength(long userId) {
        ReentrantLock lock = userLocks.get(userId);
        return lock != null ? lock.getQueueLength() : 0;
    }
}