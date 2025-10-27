# hhplus_tdd
항해 99 - TDD 기본 다지기 과제

## 프로젝트 개요
포인트 충전/사용 기능을 제공하는 REST API 서버입니다.
TDD(Test-Driven Development) 방식으로 개발되었으며, 동시성 제어를 통한 데이터 정합성 보장에 중점을 두고 있습니다.

## 주요 기능
- 사용자 포인트 조회
- 포인트 충전
- 포인트 사용  
- 포인트 사용 내역 조회

## 테스트 구조

### 📁 Controller Layer 테스트
**`src/test/java/io/hhplus/tdd/controller/`**
- `PointControllerTest.java` - `PointController.java` 테스트
  - 포인트 조회 API 테스트
  - 포인트 충전 API 테스트
  - 포인트 사용 API 테스트
  - 포인트 내역 조회 API 테스트
  - 복합 시나리오 통합 테스트

### 📁 Service Layer 테스트
**`src/test/java/io/hhplus/tdd/service/`**
- `PointServiceTest.java` - `PointService.java` 테스트
  - 포인트 조회 비즈니스 로직 테스트
  - 포인트 충전 비즈니스 로직 테스트
  - 포인트 사용 비즈니스 로직 테스트
  - 예외 상황 처리 테스트

### 📁 Integration 테스트
**`src/test/java/io/hhplus/tdd/integration/`**
- `ConcurrencyControlTest.java` - 동시성 제어 통합 테스트
  - 동시 포인트 충전 데이터 정합성 검증
  - 동시 포인트 사용 시 잔고 초과 방지 검증
  - 충전/사용 복합 시나리오 정합성 검증
  - 다중 사용자 독립적 처리 검증

## 동시성 제어 분석 보고서

### 1. 문제 상황 분석

#### 1.1 Race Condition 발생 시나리오
포인트 시스템에서 동시성 문제가 발생할 수 있는 주요 시나리오는 다음과 같습니다:

```java
// 시나리오: 동일 사용자가 동시에 포인트 사용을 요청하는 경우
// Thread 1: 1000 포인트에서 500 포인트 사용
// Thread 2: 1000 포인트에서 300 포인트 사용

// Race Condition 발생 시:
// 1. Thread 1이 현재 포인트 1000을 조회
// 2. Thread 2도 동시에 현재 포인트 1000을 조회  
// 3. Thread 1이 1000 - 500 = 500으로 업데이트
// 4. Thread 2가 1000 - 300 = 700으로 업데이트
// 결과: 실제로는 200 포인트가 남아야 하지만 700 포인트가 남게 됨
```

#### 1.2 데이터 불일치 문제점
- **Lost Update**: 한 트랜잭션의 업데이트가 다른 트랜잭션에 의해 덮어쓰여지는 문제
- **Dirty Read**: 커밋되지 않은 데이터를 읽는 문제
- **Phantom Read**: 같은 쿼리를 실행했을 때 결과가 달라지는 문제

### 2. 동시성 제어 방식 비교 분석

#### 2.1 Java Synchronized 키워드
```java
public synchronized UserPoint chargeUserPoint(long userId, long amount) {
    // 메서드 전체에 락 적용
}
```

**장점:**
- 구현이 간단하고 직관적
- JVM 레벨에서 지원하여 안정적
- 데드락 가능성이 상대적으로 낮음

**단점:**
- 메서드 전체에 락이 걸려 성능 저하 가능
- 사용자별 세분화된 락 제어가 어려움
- 확장성 제한

#### 2.2 ReentrantLock (java.util.concurrent)
```java
private final ReentrantLock lock = new ReentrantLock();

public UserPoint chargeUserPoint(long userId, long amount) {
    lock.lock();
    try {
        // 비즈니스 로직
    } finally {
        lock.unlock();
    }
}
```

**장점:**
- 더 세밀한 락 제어 가능
- tryLock()으로 타임아웃 설정 가능
- 공정성(fairness) 설정 가능

**단점:**
- 명시적인 unlock() 처리 필요
- synchronized보다 복잡한 구현

#### 2.3 사용자별 세분화된 락 (ConcurrentHashMap + Lock)
```java
private final ConcurrentHashMap<Long, ReentrantLock> userLocks = new ConcurrentHashMap<>();

private ReentrantLock getUserLock(long userId) {
    return userLocks.computeIfAbsent(userId, k -> new ReentrantLock());
}
```

**장점:**
- 사용자별 독립적인 락으로 성능 최적화
- 다른 사용자 요청에 영향 없음
- 확장성이 좋음

**단점:**
- 메모리 사용량 증가 (사용자별 락 객체)
- 락 객체 정리 로직 필요
- 구현 복잡도 증가

#### 2.4 데이터베이스 레벨 락
```sql
-- 비관적 락 (Pessimistic Lock)
SELECT * FROM user_point WHERE user_id = ? FOR UPDATE;

-- 낙관적 락 (Optimistic Lock)  
UPDATE user_point SET point = ?, version = version + 1 
WHERE user_id = ? AND version = ?;
```

**장점:**
- 분산 환경에서도 동작
- 데이터베이스 트랜잭션과 연계
- 여러 애플리케이션 인스턴스 간 동시성 제어

**단점:**
- 데이터베이스 성능에 의존
- 네트워크 지연 시간 증가
- 데드락 가능성

### 3. 선택한 해결방안: 사용자별 세분화된 락

#### 3.1 선택 근거
1. **성능 최적화**: 사용자별 독립적인 락으로 다른 사용자에게 영향 없음
2. **확장성**: 사용자 수 증가에 대응 가능한 구조
3. **메모리 인-메모리 처리**: 현재 메모리 기반 데이터 구조에 적합
4. **구현 복잡도**: 합리적인 수준의 복잡도로 관리 가능

#### 3.2 구현 전략
```java
@Service
public class PointService {
    private final ConcurrentHashMap<Long, ReentrantLock> userLocks = new ConcurrentHashMap<>();
    
    private void executeWithUserLock(long userId, Runnable operation) {
        ReentrantLock lock = userLocks.computeIfAbsent(userId, k -> new ReentrantLock());
        lock.lock();
        try {
            operation.run();
        } finally {
            lock.unlock();
        }
    }
}
```

#### 3.3 고려사항
- **메모리 관리**: 사용하지 않는 락 객체 정리 메커니즘 필요
- **공정성**: 요청 순서 보장을 위한 fair lock 사용 검토
- **모니터링**: 락 대기 시간 및 경합 상황 모니터링

### 4. 성능 영향도 분석

#### 4.1 예상 성능 지표
- **처리량(Throughput)**: 사용자별 락으로 인한 처리량 향상 예상
- **응답시간(Response Time)**: 락 대기로 인한 소폭 증가 예상  
- **CPU 사용률**: 락 관리 오버헤드로 인한 소폭 증가
- **메모리 사용률**: 사용자별 락 객체로 인한 증가

#### 4.2 성능 최적화 방안
- 락 타임아웃 설정으로 무한 대기 방지
- 주기적인 사용하지 않는 락 객체 정리
- 락 경합 상황 모니터링 및 알림

### 5. 테스트 전략

#### 5.1 단위 테스트
- 단일 사용자 시나리오 테스트
- 예외 상황 처리 테스트

#### 5.2 동시성 테스트  
- 다중 스레드 환경에서 데이터 정합성 검증
- 락 대기 시간 측정
- 데드락 발생 여부 확인

#### 5.3 성능 테스트
- 부하 테스트를 통한 처리량 측정
- 메모리 사용량 모니터링
- 락 경합 상황 분석

### 6. 결론
사용자별 세분화된 락 방식을 통해 포인트 시스템의 동시성 문제를 해결하고, 성능과 확장성을 동시에 확보할 수 있는 구조를 구현했습니다. 이를 통해 다중 사용자 환경에서도 데이터 정합성을 보장하면서 시스템 성능을 최적화할 수 있습니다.
