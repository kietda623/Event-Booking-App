package com.eventbooking.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryRateLimiter {
    private final Clock clock = Clock.systemUTC();
    private final Map<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();

    public boolean tryAcquire(String key, int limit, Duration window) {
        Instant now = Instant.now(clock);
        Deque<Instant> bucket = attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (bucket) {
            prune(bucket, now.minus(window));
            if (bucket.size() >= limit) {
                return false;
            }
            bucket.addLast(now);
            return true;
        }
    }

    public boolean isAtLimit(String key, int limit, Duration window) {
        Instant now = Instant.now(clock);
        Deque<Instant> bucket = attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (bucket) {
            prune(bucket, now.minus(window));
            return bucket.size() >= limit;
        }
    }

    public void record(String key, Duration window) {
        Instant now = Instant.now(clock);
        Deque<Instant> bucket = attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (bucket) {
            prune(bucket, now.minus(window));
            bucket.addLast(now);
        }
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    private void prune(Deque<Instant> bucket, Instant cutoff) {
        while (!bucket.isEmpty() && bucket.peekFirst().isBefore(cutoff)) {
            bucket.removeFirst();
        }
    }
}
