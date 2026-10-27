package com.vocr.web;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.function.LongSupplier;

/** Bounded, process-local admission. No maps keyed by untrusted IPs, paths or credentials. */
final class GuestRequestLimits
{
    record Policy(int requestsPerSecond, int requestBurst, int creationsPerMinute, int creationBurst,
                  int sessionPerSecond, int sessionBurst, int replaysPerMinute, int replayBurst,
                  int concurrentRequests, int concurrentCreations, int concurrentReplays, int sessionUsers)
    {
        Policy {
            for (int value : new int[]{requestsPerSecond, requestBurst, creationsPerMinute, creationBurst,
                    sessionPerSecond, sessionBurst, replaysPerMinute, replayBurst, concurrentRequests,
                    concurrentCreations, concurrentReplays, sessionUsers})
                if (value <= 0) throw new IllegalArgumentException("请求限额必须为正数");
        }
        static Policy production() { return new Policy(250, 500, 100, 20, 100, 200, 8, 8, 8, 2, 2, 2); }
    }

    static final class Rejected extends RuntimeException
    {
        final int retrySeconds;
        Rejected(int seconds) { super("request admission rejected"); retrySeconds = seconds; }
    }

    static final class Bucket
    {
        private final LongSupplier clock;
        private final int rate;
        private final int capacity;
        private final long period;
        private long last;
        private double tokens;
        Bucket(LongSupplier clock, int rate, int capacity, Duration period) {
            this.clock = clock; this.rate = rate; this.capacity = capacity;
            this.period = period.toNanos(); last = clock.getAsLong(); tokens = capacity;
        }
        synchronized void take() {
            long now = clock.getAsLong(), elapsed = now - last;
            if (elapsed > 0) {
                tokens = Math.min(capacity, tokens + (double) elapsed / period * rate);
                last = now;
            }
            if (tokens < 1) throw new Rejected(Math.max(1,
                    (int) Math.ceil((1 - tokens) * period / rate / 1_000_000_000d)));
            tokens--;
        }
    }

    final Policy policy;
    private final Semaphore requests, creations, replays;
    private final Bucket requestRate, creationRate;

    GuestRequestLimits(Policy policy, LongSupplier clock) {
        this.policy = policy;
        requests = new Semaphore(policy.concurrentRequests());
        creations = new Semaphore(policy.concurrentCreations());
        replays = new Semaphore(policy.concurrentReplays());
        requestRate = new Bucket(clock, policy.requestsPerSecond(), policy.requestBurst(), Duration.ofSeconds(1));
        creationRate = new Bucket(clock, policy.creationsPerMinute(), policy.creationBurst(), Duration.ofMinutes(1));
    }

    final class Lease implements AutoCloseable {
        private boolean request, creation, replay, closed;
        synchronized void restrictRoute(boolean create, boolean readReplay) {
            if (closed) throw new IllegalStateException("request lease is closed");
            if (create && !creation) {
                creationRate.take();
                if (!creations.tryAcquire()) throw new Rejected(1);
                creation = true;
            }
            if (readReplay && !replay) {
                if (!replays.tryAcquire()) throw new Rejected(1);
                replay = true;
            }
        }
        @Override public synchronized void close() {
            if (closed) return;
            closed = true;
            if (replay) replays.release();
            if (creation) creations.release();
            if (request) requests.release();
        }
    }

    Lease enter(boolean create, boolean replay) {
        Lease lease = new Lease();
        try {
            if (!requests.tryAcquire()) throw new Rejected(1);
            lease.request = true;
            requestRate.take();
            lease.restrictRoute(create, replay);
            return lease;
        } catch (RuntimeException error) { lease.close(); throw error; }
    }
}
