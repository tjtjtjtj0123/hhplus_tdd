package io.hhplus.tdd.common.util;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * API 로깅을 위한 공통 유틸리티 클래스
 * 
 * 사용법:
 * return ApiLoggingUtil.executeWithLogging(
 *     "포인트 조회",
 *     "userId: " + id,
 *     () -> pointService.getUserPoint(id),
 *     result -> "point: " + result.point()
 * );
 */
@Slf4j
public class ApiLoggingUtil {

    /**
     * API 호출을 로깅과 함께 실행하는 공통 메서드
     * 
     * @param apiName API 이름 (예: "포인트 조회")
     * @param requestInfo 요청 정보 (예: "userId: 1")
     * @param operation 실행할 비즈니스 로직
     * @param responseFormatter 응답 정보 포맷터
     * @param <T> 반환 타입
     * @return 실행 결과
     */
    public static <T> T executeWithLogging(String apiName, String requestInfo, 
                                         Supplier<T> operation,
                                         Function<T, String> responseFormatter) {
        log.info("=== {} API 요청 === {}", apiName, requestInfo);
        
        try {
            T result = operation.get();
            log.info("=== {} API 성공 === {}, {}", apiName, requestInfo, responseFormatter.apply(result));
            return result;
        } catch (Exception e) {
            log.error("=== {} API 실패 === {}, error: {}", apiName, requestInfo, e.getMessage());
            throw e;
        }
    }

    /**
     * 응답 정보가 필요없는 간단한 API 로깅을 위한 메서드
     * 
     * @param apiName API 이름
     * @param requestInfo 요청 정보
     * @param operation 실행할 비즈니스 로직
     * @param <T> 반환 타입
     * @return 실행 결과
     */
    public static <T> T executeWithSimpleLogging(String apiName, String requestInfo, 
                                               Supplier<T> operation) {
        return executeWithLogging(
            apiName, 
            requestInfo, 
            operation, 
            result -> "success"
        );
    }

    /**
     * 반환값이 없는 void 메서드를 위한 로깅 메서드
     * 
     * @param apiName API 이름
     * @param requestInfo 요청 정보
     * @param operation 실행할 비즈니스 로직
     */
    public static void executeVoidWithLogging(String apiName, String requestInfo, 
                                            Runnable operation) {
        log.info("=== {} API 요청 === {}", apiName, requestInfo);
        
        try {
            operation.run();
            log.info("=== {} API 성공 === {}", apiName, requestInfo);
        } catch (Exception e) {
            log.error("=== {} API 실패 === {}, error: {}", apiName, requestInfo, e.getMessage());
            throw e;
        }
    }
}