package io.hhplus.tdd.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hhplus.tdd.common.constant.ErrorCode;
import io.hhplus.tdd.common.constant.PointPolicy;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.repository.PointHistoryRepositoryImpl;
import io.hhplus.tdd.repository.UserPointRepositoryImpl;
import io.hhplus.tdd.service.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PointController HTTP 응답 검증 테스트
 * HTTP 상태 코드, 에러 메시지 포맷의 일관성을 검증합니다.
 */
class PointControllerHttpResponseTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        UserPointTable userPointTable = new UserPointTable();
        PointHistoryTable pointHistoryTable = new PointHistoryTable();
        
        PointService pointService = new PointService(
            new UserPointRepositoryImpl(userPointTable),
            new PointHistoryRepositoryImpl(pointHistoryTable)
        );
        
        PointController pointController = new PointController(pointService);
        
        mockMvc = MockMvcBuilders.standaloneSetup(pointController)
            .setControllerAdvice(new io.hhplus.tdd.common.exception.GlobalExceptionHandler())
            .build();
            
        objectMapper = new ObjectMapper();
    }

    // ==================== HTTP 400 Bad Request 검증 ====================

    @Test
    @DisplayName("유효하지 않은 사용자 ID - HTTP 400 응답")
    void charge_WithInvalidUserId_ShouldReturn400() throws Exception {
        // given: 유효하지 않은 사용자 ID
        long invalidUserId = -1L;
        long amount = 1000L;

        // when & then: HTTP 400 응답과 에러 코드 검증
        mockMvc.perform(patch("/point/{id}/charge", invalidUserId)
                .param("amount", String.valueOf(amount))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.INVALID_USER_ID))
                .andExpect(jsonPath("$.message").value(containsString("유효하지 않은 사용자 ID")));
    }

    @Test
    @DisplayName("최소 금액 미만 충전 - HTTP 400 응답")
    void charge_WithBelowMinAmount_ShouldReturn400() throws Exception {
        // given: 최소 금액 미만
        long userId = 1L;
        long belowMinAmount = PointPolicy.MIN_AMOUNT - 1;

        // when & then: HTTP 400 응답과 구체적 에러 메시지 검증
        mockMvc.perform(patch("/point/{id}/charge", userId)
                .param("amount", String.valueOf(belowMinAmount))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.BELOW_MIN_AMOUNT))
                .andExpect(jsonPath("$.message").value(containsString("최소 충전 금액")))
                .andExpect(jsonPath("$.message").value(containsString(String.valueOf(PointPolicy.MIN_AMOUNT))));
    }

    @Test
    @DisplayName("최대 충전 금액 초과 - HTTP 400 응답")
    void charge_WithExceedMaxAmount_ShouldReturn400() throws Exception {
        // given: 최대 충전 금액 초과
        long userId = 1L;
        long exceedMaxAmount = PointPolicy.MAX_CHARGE_AMOUNT + 1;

        // when & then: HTTP 400 응답과 구체적 에러 메시지 검증
        mockMvc.perform(patch("/point/{id}/charge", userId)
                .param("amount", String.valueOf(exceedMaxAmount))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.EXCEED_MAX_CHARGE_AMOUNT))
                .andExpect(jsonPath("$.message").value(containsString("최대 충전 금액")))
                .andExpect(jsonPath("$.message").value(containsString(String.valueOf(PointPolicy.MAX_CHARGE_AMOUNT))));
    }

    @Test
    @DisplayName("단위가 맞지 않는 금액 - HTTP 400 응답")
    void charge_WithInvalidAmountUnit_ShouldReturn400() throws Exception {
        // given: 단위가 맞지 않는 금액
        long userId = 1L;
        long invalidUnitAmount = PointPolicy.MIN_AMOUNT + 1;

        // when & then: HTTP 400 응답과 단위 에러 메시지 검증
        mockMvc.perform(patch("/point/{id}/charge", userId)
                .param("amount", String.valueOf(invalidUnitAmount))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.INVALID_AMOUNT_UNIT))
                .andExpect(jsonPath("$.message").value(containsString("단위로만 가능")))
                .andExpect(jsonPath("$.message").value(containsString(String.valueOf(PointPolicy.MIN_AMOUNT))));
    }

    @Test
    @DisplayName("잔고 부족 사용 - HTTP 400 응답")
    void use_WithInsufficientBalance_ShouldReturn400() throws Exception {
        // given: 잔고보다 많은 사용 시도
        long userId = 1L;
        long useAmount = 1000L;

        // when & then: HTTP 400 응답과 잔고 부족 에러 메시지 검증
        mockMvc.perform(patch("/point/{id}/use", userId)
                .param("amount", String.valueOf(useAmount))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.INSUFFICIENT_BALANCE))
                .andExpect(jsonPath("$.message").value(containsString("포인트 잔고가 부족")))
                .andExpect(jsonPath("$.message").value(containsString("현재: 0")))
                .andExpect(jsonPath("$.message").value(containsString("사용 시도: 1000")));
    }

    @Test
    @DisplayName("최대 잔고 초과 충전 - HTTP 400 응답")
    void charge_WithExceedMaxBalance_ShouldReturn400() throws Exception {
        // given: 최대 잔고 근처까지 충전 후 추가 충전 시도
        long userId = 1L;
        
        // 먼저 최대 잔고 근처까지 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                .param("amount", String.valueOf(PointPolicy.MAX_BALANCE - 100L))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // when & then: 최대 잔고 초과하는 추가 충전 시도
        mockMvc.perform(patch("/point/{id}/charge", userId)
                .param("amount", "200")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.EXCEED_MAX_BALANCE))
                .andExpect(jsonPath("$.message").value(containsString("최대 보유 가능 포인트")))
                .andExpect(jsonPath("$.message").value(containsString(String.valueOf(PointPolicy.MAX_BALANCE))));
    }

    // ==================== HTTP 200 OK 검증 ====================

    @Test
    @DisplayName("정상 충전 - HTTP 200 응답")
    void charge_WithValidAmount_ShouldReturn200() throws Exception {
        // given: 유효한 충전 금액
        long userId = 1L;
        long validAmount = 1000L;

        // when & then: HTTP 200 응답과 성공 데이터 검증
        mockMvc.perform(patch("/point/{id}/charge", userId)
                .param("amount", String.valueOf(validAmount))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(userId))
                .andExpect(jsonPath("$.data.point").value(validAmount))
                .andExpect(jsonPath("$.data.updateMillis").exists());
    }

    @Test
    @DisplayName("정상 사용 - HTTP 200 응답")
    void use_WithValidAmount_ShouldReturn200() throws Exception {
        // given: 충분한 포인트 충전 후 유효한 사용 금액
        long userId = 1L;
        long chargeAmount = 2000L;
        long useAmount = 1000L;

        // 먼저 포인트 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                .param("amount", String.valueOf(chargeAmount))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // when & then: HTTP 200 응답과 성공 데이터 검증
        mockMvc.perform(patch("/point/{id}/use", userId)
                .param("amount", String.valueOf(useAmount))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(userId))
                .andExpect(jsonPath("$.data.point").value(chargeAmount - useAmount))
                .andExpect(jsonPath("$.data.updateMillis").exists());
    }

    @Test
    @DisplayName("포인트 조회 - HTTP 200 응답")
    void point_ShouldReturn200() throws Exception {
        // given: 사용자 ID
        long userId = 1L;

        // when & then: HTTP 200 응답과 포인트 데이터 검증
        mockMvc.perform(get("/point/{id}", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(userId))
                .andExpect(jsonPath("$.data.point").value(0))
                .andExpect(jsonPath("$.data.updateMillis").exists());
    }

    @Test
    @DisplayName("포인트 내역 조회 - HTTP 200 응답")
    void history_ShouldReturn200() throws Exception {
        // given: 사용자 ID
        long userId = 1L;

        // when & then: HTTP 200 응답과 내역 데이터 검증
        mockMvc.perform(get("/point/{id}/histories", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ==================== 에러 응답 포맷 일관성 검증 ====================

    @Test
    @DisplayName("모든 에러 응답이 동일한 포맷을 가져야 함")
    void allErrorResponses_ShouldHaveConsistentFormat() throws Exception {
        long userId = 1L;

        // 다양한 에러 상황에서 동일한 응답 포맷 검증
        String[] errorEndpoints = {
            "/point/-1/charge?amount=1000",  // INVALID_USER_ID
            "/point/1/charge?amount=5",      // BELOW_MIN_AMOUNT  
            "/point/1/use?amount=1000"       // INSUFFICIENT_BALANCE
        };

        for (String endpoint : errorEndpoints) {
            String[] parts = endpoint.split("\\?");
            String path = parts[0];
            String queryString = parts.length > 1 ? parts[1] : "";
            
            mockMvc.perform(patch(path + (queryString.isEmpty() ? "" : "?" + queryString))
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").exists())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }
}