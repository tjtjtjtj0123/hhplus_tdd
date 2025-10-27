### **커밋 설명**
<!-- 
좋은 피드백을 받기 위해 가장 중요한 것은 커밋입니다.
코드를 작성할 때 커밋을 작업 단위로 잘 쪼개주세요!

예시)
동시성 처리 : c83845
동시성 테스트 코드 : d93ji3
-->

**주요 커밋 내역:**

1. **초기 프로젝트 설정** : `32b83b3`, `321dc68`
   - GitHub 저장소 초기화 및 기본 프로젝트 구조 생성
   - Gradle 빌드 설정 및 Spring Boot 기본 템플릿 구성

2. **TDD 기반 포인트 관리 시스템 구현** : `ab8f08e`
   - **핵심 기능 구현**: 포인트 조회, 충전, 사용, 내역 조회 API 개발 (772+ lines)
   - **테스트 우선 개발**: PointControllerTest (252 lines), PointServiceTest (269 lines) 작성
   - **비즈니스 로직**: PointService 클래스에 핵심 포인트 관리 로직 구현 (106 lines)
   - **기본 예외 처리**: 입력값 검증 및 잔고 부족 시 예외 처리

3. **Repository-Service-Controller 아키텍처 구조 개선** : `50a34d0`
   - **계층 분리**: point 패키지를 controller/domain/service/repository로 분리 (284+ lines 변경)
   - **Repository 패턴 도입**: UserPointRepository, PointHistoryRepository 인터페이스 및 구현체 생성
   - **의존성 주입**: 각 계층 간 의존성을 인터페이스 기반으로 개선
   - **테스트 구조 개선**: 계층별 책임에 맞는 테스트 코드 리팩토링

4. **예외 처리 및 공통 유틸리티 구조 개선** : `28eacbb`
   - **전역 예외 처리**: GlobalExceptionHandler 및 BusinessException 구조 구축 (495+ lines)
   - **공통 응답 포맷**: ApiResponse, ErrorResponse 표준화
   - **유틸리티 클래스**: ValidationUtil, ApiLoggingUtil 추가
   - **에러 코드 체계**: ErrorCode enum으로 에러 메시지 중앙 관리
   - **로깅 설정**: application.yml에 상세 로깅 설정 추가

5. **동시성 제어 및 테스트 구조 최적화** : `0d7e188`
   - **동시성 제어 구현**: ConcurrentHashMap + ReentrantLock 기반 사용자별 세분화된 락 (915+ lines 변경)
   - **테스트 구조 재편**: controller/service/integration 패키지별 테스트 파일 재구성
   - **동시성 테스트**: ConcurrencyControlTest 클래스로 멀티스레드 환경 테스트 (320 lines)
   - **포괄적 문서화**: README.md에 동시성 제어 분석 보고서 및 테스트 구조 가이드 (206+ lines)
   - **모니터링 기능**: 락 상태 모니터링 메서드 추가

### **과제 셀프 피드백**
**좋았던 부분:**
- **체계적인 TDD 접근**: 테스트를 먼저 작성하고 구현하는 방식으로 안정적인 코드 품질 확보
- **동시성 문제 심화 분석**: 단순한 synchronized 적용이 아닌 사용자별 세분화된 락으로 성능과 안정성 모두 고려
- **계층별 테스트 전략**: Controller/Service/Integration 테스트를 분리하여 각 계층의 책임에 맞는 테스트 작성
- **포괄적인 문서화**: 동시성 제어 방식 비교 분석부터 성능 영향도까지 체계적으로 정리

**애매하거나 도전적이었던 부분:**
- **동시성 제어 방식 선택**: synchronized, ReentrantLock, 사용자별 락, DB 락 등 다양한 방식 중 최적해 선택의 어려움
- **테스트 시나리오 설계**: 동시성 상황을 재현하는 멀티스레드 테스트 작성의 복잡성
- **성능과 안정성 트레이드오프**: 사용자별 락 도입으로 인한 메모리 사용량 증가와 성능 향상 간의 균형점 찾기

### 기술적 성장
**새로 학습한 개념:**
- **ConcurrentHashMap의 computeIfAbsent 활용**: 원자적 연산으로 락 객체 생성 및 관리
- **ReentrantLock의 Fair Lock**: 요청 순서 보장을 통한 공정한 락 획득 메커니즘
- **CountDownLatch를 활용한 동시성 테스트**: 여러 스레드의 동시 시작과 완료 대기 패턴
- **ExecutorService 기반 스레드 풀 관리**: 테스트 환경에서의 효율적인 스레드 리소스 관리

**기존 지식의 재발견/심화:**
- **TDD의 실질적 효과**: 테스트 우선 개발로 인한 설계 품질 향상과 리팩토링 안정성 확보
- **Race Condition 시나리오 분석**: Lost Update, Dirty Read 등 동시성 문제의 구체적 발생 상황 이해
- **아키텍처 계층 분리의 중요성**: Repository-Service-Controller 패턴의 테스트 용이성과 유지보수성

**구현 과정에서의 기술적 도전과 해결:**
- **동시성 테스트 안정성 확보**: 
  - 문제: 멀티스레드 테스트의 비결정적 결과
  - 해결: CountDownLatch로 스레드 동기화, AtomicInteger로 안전한 카운팅

- **사용자별 락 메모리 관리**:
  - 문제: 사용자 증가에 따른 락 객체 메모리 누수 우려
  - 해결: 모니터링 메서드 추가, 추후 WeakReference 또는 TTL 기반 정리 로직 고려

- **성능과 정확성의 균형**:
  - 문제: 전역 락의 성능 저하 vs 세분화된 락의 복잡성
  - 해결: 사용자별 독립적 락으로 동시성 향상과 데이터 정합성 모두 달성