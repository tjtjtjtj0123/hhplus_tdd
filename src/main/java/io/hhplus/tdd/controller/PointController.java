package io.hhplus.tdd.controller;

import io.hhplus.tdd.common.util.ApiLoggingUtil;
import io.hhplus.tdd.domain.PointHistory;
import io.hhplus.tdd.domain.UserPoint;
import io.hhplus.tdd.service.PointService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/point")
public class PointController {
    
    private final PointService pointService;
    
    public PointController(PointService pointService) {
        this.pointService = pointService;
    }

    /**
     * 특정 유저의 포인트를 조회하는 기능
     */
    @GetMapping("{id}")
    public UserPoint point(
            @PathVariable long id
    ) {
        return ApiLoggingUtil.executeWithLogging(
            "포인트 조회",
            "userId: " + id,
            () -> pointService.getUserPoint(id),
            result -> "point: " + result.point()
        );
    }

    /**
     * 특정 유저의 포인트 충전/이용 내역을 조회하는 기능
     */
    @GetMapping("{id}/histories")
    public List<PointHistory> history(
            @PathVariable long id
    ) {
        return ApiLoggingUtil.executeWithLogging(
            "포인트 내역 조회",
            "userId: " + id,
            () -> pointService.getUserPointHistories(id),
            result -> "historyCount: " + result.size()
        );
    }

    /**
     * 특정 유저의 포인트를 충전하는 기능
     */
    @PatchMapping("{id}/charge")
    public UserPoint charge(
            @PathVariable long id,
            @RequestBody long amount
    ) {
        return ApiLoggingUtil.executeWithLogging(
            "포인트 충전",
            String.format("userId: %d, chargeAmount: %d", id, amount),
            () -> pointService.chargeUserPoint(id, amount),
            result -> String.format("beforePoint: %d, afterPoint: %d, chargedAmount: %d", 
                    result.point() - amount, result.point(), amount)
        );
    }

    /**
     * 특정 유저의 포인트를 사용하는 기능
     */
    @PatchMapping("{id}/use")
    public UserPoint use(
            @PathVariable long id,
            @RequestBody long amount
    ) {
        return ApiLoggingUtil.executeWithLogging(
            "포인트 사용",
            String.format("userId: %d, useAmount: %d", id, amount),
            () -> pointService.useUserPoint(id, amount),
            result -> String.format("beforePoint: %d, afterPoint: %d, usedAmount: %d", 
                    result.point() + amount, result.point(), amount)
        );
    }
}