package com.payflow.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletCacheService {

    private final StringRedisTemplate redisTemplate;

    private static final long TTL_SECONDS = 30;

    private String getKey(UUID walletId) {
        return "wallet:balance:" + walletId;
    }

    public Optional<BigDecimal> getBalanceFromCache(UUID walletId) {

        String value = redisTemplate.opsForValue().get(getKey(walletId));

        if (value == null) {
            log.info("Cache MISS: {}", getKey(walletId));
            return Optional.empty();
        }

        log.info("Cache HIT: {}", getKey(walletId));
        return Optional.of(new BigDecimal(value));
    }

    public void saveBalanceToCache(UUID walletId, BigDecimal balance) {

        redisTemplate.opsForValue().set(
                getKey(walletId),
                balance.toPlainString(),
                TTL_SECONDS,
                TimeUnit.SECONDS
        );

        log.info("Cached balance: {}", getKey(walletId));
    }

    public void invalidateBalanceCache(UUID walletId) {

        redisTemplate.delete(getKey(walletId));

        log.info("Cache invalidated: {}", getKey(walletId));
    }
}