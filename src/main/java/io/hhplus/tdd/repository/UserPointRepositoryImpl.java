package io.hhplus.tdd.repository;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.domain.UserPoint;
import org.springframework.stereotype.Repository;

/**
 * UserPointRepository의 구현체
 * UserPointTable을 사용하여 실제 데이터 접근 로직을 처리합니다.
 */
@Repository
public class UserPointRepositoryImpl implements UserPointRepository {

    private final UserPointTable userPointTable;

    public UserPointRepositoryImpl(UserPointTable userPointTable) {
        this.userPointTable = userPointTable;
    }

    @Override
    public UserPoint selectById(Long id) {
        return userPointTable.selectById(id);
    }

    @Override
    public UserPoint insertOrUpdate(long id, long amount) {
        return userPointTable.insertOrUpdate(id, amount);
    }
}