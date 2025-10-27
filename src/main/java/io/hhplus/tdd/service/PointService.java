package io.hhplus.tdd.service;

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
        // 충전 금액 유효성 검증
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }

        return executeWithUserLock(userId, () -> {
            log.info("포인트 충전 시작 - 사용자: {}, 충전금액: {}", userId, amount);
            
            // 현재 포인트 조회
            UserPoint currentPoint = userPointRepository.selectById(userId);
            log.debug("현재 포인트: {}", currentPoint.point());
            
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
        // 사용 금액 유효성 검증
        if (amount <= 0) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다.");
        }

        return executeWithUserLock(userId, () -> {
            log.info("포인트 사용 시작 - 사용자: {}, 사용금액: {}", userId, amount);
            
            // 현재 포인트 조회
            UserPoint currentPoint = userPointRepository.selectById(userId);
            log.debug("현재 포인트: {}", currentPoint.point());
            
            // 잔고 확인
            if (currentPoint.point() < amount) {
                log.warn("포인트 잔고 부족 - 사용자: {}, 현재: {}, 요청: {}", userId, currentPoint.point(), amount);
                throw new IllegalStateException("포인트 잔고가 부족합니다. 현재 잔고: " + currentPoint.point() + ", 사용 요청: " + amount);
            }
            
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