package com.user.resource;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AppTokenExchangeStore {
    private static final long CODE_LIFETIME_NANOS = Duration.ofMinutes(2).toNanos();

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, Entry> tokensByCode = new ConcurrentHashMap<>();

    public String issue(String token) {
        long now = System.nanoTime();
        tokensByCode.entrySet().removeIf(entry -> entry.getValue().expiresAtNanos() - now <= 0);

        byte[] random = new byte[32];
        String code;
        do {
            secureRandom.nextBytes(random);
            code = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        } while (tokensByCode.putIfAbsent(code, new Entry(token, now + CODE_LIFETIME_NANOS)) != null);

        return code;
    }

    public Optional<String> consume(String code) {
        Entry entry = tokensByCode.remove(code);
        if (entry == null || entry.expiresAtNanos() - System.nanoTime() <= 0) {
            return Optional.empty();
        }
        return Optional.of(entry.token());
    }

    private record Entry(String token, long expiresAtNanos) {
    }
}
