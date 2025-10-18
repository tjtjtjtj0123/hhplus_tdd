package io.hhplus.tdd.repository;

import io.hhplus.tdd.domain.PointHistory;
import io.hhplus.tdd.domain.TransactionType;

import java.util.List;

/**
 * 포인트 변경 내역 데이터 접근을 담당하는 Repository 인터페이스
 */
public interface PointHistoryRepository {
    
    /**
     * 포인트 변경 내역을 저장합니다.
     * 
     * @param userId 사용자 ID
     * @param amount 변경 금액
     * @param type 변경 유형 (충전/사용)
     * @param updateMillis 변경 시간
     * @return 저장된 포인트 변경 내역
     */
    PointHistory insert(long userId, long amount, TransactionType type, long updateMillis);
    
    /**
     * 특정 사용자의 모든 포인트 변경 내역을 조회합니다.
     * 
     * @param userId 사용자 ID
     * @return 포인트 변경 내역 리스트
     */
    List<PointHistory> selectAllByUserId(long userId);
}