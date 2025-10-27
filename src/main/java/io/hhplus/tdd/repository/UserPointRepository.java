package io.hhplus.tdd.repository;

import io.hhplus.tdd.domain.UserPoint;

/**
 * 사용자 포인트 데이터 접근을 담당하는 Repository 인터페이스
 */
public interface UserPointRepository {
    
    /**
     * 특정 사용자의 포인트 정보를 조회합니다.
     * 
     * @param id 사용자 ID
     * @return 사용자 포인트 정보
     */
    UserPoint selectById(Long id);
    
    /**
     * 사용자의 포인트 정보를 저장하거나 업데이트합니다.
     * 
     * @param id 사용자 ID
     * @param amount 포인트 금액
     * @return 저장/업데이트된 사용자 포인트 정보
     */
    UserPoint insertOrUpdate(long id, long amount);
}