package io.hhplus.tdd.integration;

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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 동시성 제어에 대한 통합 테스트 클래스
 * 
 * 여러 스레드가 동시에 포인트 충전/사용 요청을 했을 때
 * 데이터 정합성이 보장되는지 검증합니다.
 */
class ConcurrencyControlTest {

    private PointService pointService;
    private UserPointRepository userPointRepository;
    private PointHistoryRepository pointHistoryRepository;
    
    // 테스트에 사용할 스레드 풀
    private ExecutorService executorService;

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
        
        // 테스트용 스레드 풀 생성 (CPU 코어 수의 2배)
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    }

    @Test
    @DisplayName("동시 포인트 충전 - 모든 충전이 정확히 반영되어야 함")
    void concurrentCharge_ShouldMaintainDataConsistency() throws InterruptedException {
        // given: 동시 충전 테스트를 위한 설정
        long userId = 1L;
        int threadCount = 10;
        long chargeAmountPerThread = 100L;
        long expectedTotalAmount = threadCount * chargeAmountPerThread;
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        List<Exception> exceptions = new ArrayList<>();

        // when: 여러 스레드가 동시에 포인트 충전
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await(); // 모든 스레드가 동시에 시작하도록 대기
                    pointService.chargeUserPoint(userId, chargeAmountPerThread);
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 모든 스레드 동시 시작
        boolean completed = endLatch.await(10, TimeUnit.SECONDS);

        // then: 모든 충전이 완료되고 총 포인트가 정확해야 함
        assertTrue(completed, "모든 스레드가 시간 내에 완료되어야 함");
        assertTrue(exceptions.isEmpty(), "예외가 발생하지 않아야 함: " + exceptions);
        
        UserPoint finalPoint = pointService.getUserPoint(userId);
        assertEquals(expectedTotalAmount, finalPoint.point(), 
            "최종 포인트는 모든 충전 금액의 합과 같아야 함");
        
        // 히스토리 검증
        List<PointHistory> histories = pointService.getUserPointHistories(userId);
        assertEquals(threadCount, histories.size(), "모든 충전 내역이 기록되어야 함");
        
        long totalChargedAmount = histories.stream()
            .filter(h -> h.type() == TransactionType.CHARGE)
            .mapToLong(PointHistory::amount)
            .sum();
        assertEquals(expectedTotalAmount, totalChargedAmount, "충전 내역의 합이 일치해야 함");
    }

    @Test
    @DisplayName("동시 포인트 사용 - 잔고를 초과하지 않아야 함")
    void concurrentUse_ShouldNotExceedBalance() throws InterruptedException {
        // given: 초기 포인트 설정 및 동시 사용 테스트 준비
        long userId = 2L;
        long initialPoint = 1000L;
        int threadCount = 20;
        long useAmountPerThread = 100L;
        
        // 초기 포인트 충전
        pointService.chargeUserPoint(userId, initialPoint);
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Exception> exceptions = new ArrayList<>();

        // when: 여러 스레드가 동시에 포인트 사용 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    pointService.useUserPoint(userId, useAmountPerThread);
                    successCount.incrementAndGet();
                } catch (IllegalStateException e) {
                    // 잔고 부족으로 인한 실패는 정상적인 상황
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(10, TimeUnit.SECONDS);

        // then: 잔고를 초과하지 않고 정확한 계산이 이루어져야 함
        assertTrue(completed, "모든 스레드가 시간 내에 완료되어야 함");
        assertTrue(exceptions.isEmpty(), "예상치 못한 예외가 발생하지 않아야 함: " + exceptions);
        
        UserPoint finalPoint = pointService.getUserPoint(userId);
        assertTrue(finalPoint.point() >= 0, "최종 포인트는 음수가 될 수 없음");
        
        // 성공한 사용 횟수 * 사용 금액 + 최종 잔고 = 초기 포인트
        long expectedFinalPoint = initialPoint - (successCount.get() * useAmountPerThread);
        assertEquals(expectedFinalPoint, finalPoint.point(), 
            "최종 포인트 = 초기 포인트 - (성공한 사용 횟수 * 사용 금액)");
        
        System.out.println("성공한 사용: " + successCount.get() + "회");
        System.out.println("실패한 사용: " + failureCount.get() + "회");
        System.out.println("최종 포인트: " + finalPoint.point());
    }

    @Test
    @DisplayName("동시 충전과 사용 - 복합 시나리오에서 데이터 정합성 보장")
    void concurrentChargeAndUse_ShouldMaintainConsistency() throws InterruptedException {
        // given: 복합 시나리오 테스트 설정
        long userId = 3L;
        long initialPoint = 500L;
        int chargeThreadCount = 5;
        int useThreadCount = 3;
        long chargeAmount = 200L;
        long useAmount = 150L;
        
        // 초기 포인트 설정
        pointService.chargeUserPoint(userId, initialPoint);
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(chargeThreadCount + useThreadCount);
        AtomicInteger chargeSuccessCount = new AtomicInteger(0);
        AtomicInteger useSuccessCount = new AtomicInteger(0);
        List<Exception> exceptions = new ArrayList<>();

        // when: 충전과 사용을 동시에 실행
        // 충전 스레드들
        for (int i = 0; i < chargeThreadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    pointService.chargeUserPoint(userId, chargeAmount);
                    chargeSuccessCount.incrementAndGet();
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        // 사용 스레드들
        for (int i = 0; i < useThreadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    pointService.useUserPoint(userId, useAmount);
                    useSuccessCount.incrementAndGet();
                } catch (IllegalStateException e) {
                    // 잔고 부족은 정상적인 실패
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(15, TimeUnit.SECONDS);

        // then: 모든 연산의 결과가 정확해야 함
        assertTrue(completed, "모든 스레드가 시간 내에 완료되어야 함");
        assertTrue(exceptions.isEmpty(), "예상치 못한 예외가 발생하지 않아야 함: " + exceptions);
        
        UserPoint finalPoint = pointService.getUserPoint(userId);
        long expectedPoint = initialPoint + (chargeSuccessCount.get() * chargeAmount) - (useSuccessCount.get() * useAmount);
        
        assertEquals(expectedPoint, finalPoint.point(), 
            "최종 포인트 = 초기 포인트 + 총 충전 - 총 사용");
        
        // 히스토리 검증
        List<PointHistory> histories = pointService.getUserPointHistories(userId);
        long chargeHistoryCount = histories.stream()
            .filter(h -> h.type() == TransactionType.CHARGE)
            .count();
        long useHistoryCount = histories.stream()
            .filter(h -> h.type() == TransactionType.USE)
            .count();
            
        assertEquals(chargeSuccessCount.get() + 1, chargeHistoryCount, "충전 히스토리 개수 (초기 충전 포함)");
        assertEquals(useSuccessCount.get(), useHistoryCount, "사용 히스토리 개수");
        
        System.out.println("충전 성공: " + chargeSuccessCount.get() + "회");
        System.out.println("사용 성공: " + useSuccessCount.get() + "회");
        System.out.println("최종 포인트: " + finalPoint.point());
    }

    @Test
    @DisplayName("다중 사용자 동시 요청 - 사용자간 간섭 없이 독립적 처리")
    void multipleUsersConcurrentRequests_ShouldBeIndependent() throws InterruptedException {
        // given: 여러 사용자가 동시에 요청하는 시나리오
        int userCount = 5;
        int requestsPerUser = 10;
        long chargeAmount = 100L;
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(userCount * requestsPerUser);
        List<Exception> exceptions = new ArrayList<>();

        // when: 여러 사용자가 동시에 포인트 충전
        for (long userId = 1; userId <= userCount; userId++) {
            final long currentUserId = userId;
            
            for (int j = 0; j < requestsPerUser; j++) {
                executorService.submit(() -> {
                    try {
                        startLatch.await();
                        pointService.chargeUserPoint(currentUserId, chargeAmount);
                    } catch (Exception e) {
                        synchronized (exceptions) {
                            exceptions.add(e);
                        }
                    } finally {
                        endLatch.countDown();
                    }
                });
            }
        }

        startLatch.countDown();
        boolean completed = endLatch.await(15, TimeUnit.SECONDS);

        // then: 모든 사용자의 포인트가 독립적으로 정확히 계산되어야 함
        assertTrue(completed, "모든 스레드가 시간 내에 완료되어야 함");
        assertTrue(exceptions.isEmpty(), "예외가 발생하지 않아야 함: " + exceptions);
        
        for (long userId = 1; userId <= userCount; userId++) {
            UserPoint userPoint = pointService.getUserPoint(userId);
            long expectedPoint = requestsPerUser * chargeAmount;
            assertEquals(expectedPoint, userPoint.point(), 
                "사용자 " + userId + "의 포인트가 정확해야 함");
            
            List<PointHistory> histories = pointService.getUserPointHistories(userId);
            assertEquals(requestsPerUser, histories.size(), 
                "사용자 " + userId + "의 히스토리 개수가 정확해야 함");
        }
    }



    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        // 테스트 완료 후 스레드 풀 정리
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}