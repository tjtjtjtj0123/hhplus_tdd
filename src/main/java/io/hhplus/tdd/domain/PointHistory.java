package io.hhplus.tdd.domain;

public record PointHistory(
        long id,
        long userId,
        long amount,
        TransactionType type,
        long updateMillis
) {
}