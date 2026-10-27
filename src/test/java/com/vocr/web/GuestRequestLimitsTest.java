package com.vocr.web;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class GuestRequestLimitsTest
{
    @Test void tokenBoundaryAndRetry_doNotResetOnRejectedCalls() {
        AtomicLong clock = new AtomicLong();
        var bucket = new GuestRequestLimits.Bucket(clock::get, 1, 2, Duration.ofSeconds(1));
        bucket.take(); bucket.take();
        assertEquals(1, assertThrows(GuestRequestLimits.Rejected.class, bucket::take).retrySeconds);
        clock.set(999_999_999); assertThrows(GuestRequestLimits.Rejected.class, bucket::take);
        clock.set(1_000_000_000); bucket.take();
        assertThrows(GuestRequestLimits.Rejected.class, bucket::take);
        clock.set(60_000_000_000L); bucket.take(); bucket.take();
        assertThrows(GuestRequestLimits.Rejected.class, bucket::take); // Cap, no unlimited saved credit.
    }

    @Test void monotonicWrapAndBackwardsClock_doNotManufactureTokens() {
        AtomicLong clock = new AtomicLong(Long.MAX_VALUE - 500_000_000);
        var bucket = new GuestRequestLimits.Bucket(clock::get, 1, 1, Duration.ofSeconds(1));
        bucket.take(); clock.addAndGet(1_000_000_000); bucket.take();
        clock.addAndGet(-1); assertThrows(GuestRequestLimits.Rejected.class, bucket::take);
    }

    private GuestRequestLimits limits(int requests, int creates, int replays) {
        return new GuestRequestLimits(new GuestRequestLimits.Policy(100,100,100,100,100,100,8,8,
                requests,creates,replays,2), System::nanoTime);
    }

    @Test void globalAndCreationLeases_areNonBlockingAndIdempotentlyReleased() {
        var limits = limits(2,1,1);
        var create = limits.enter(true,false);
        assertThrows(GuestRequestLimits.Rejected.class, () -> limits.enter(true,false));
        var read = limits.enter(false,false); // Failed create did not leak its global permit.
        assertThrows(GuestRequestLimits.Rejected.class, () -> limits.enter(false,false));
        create.close(); create.close();
        var second = limits.enter(true,false);
        assertThrows(GuestRequestLimits.Rejected.class, () -> limits.enter(false,false));
        second.close(); read.close();
        limits.enter(true,false).close();
    }

    @Test void replayAdmission_isHeldUntilTheWholeHttpHandlerFinishes() {
        var limits = limits(3,2,1);
        var replay = limits.enter(false,true);
        assertThrows(GuestRequestLimits.Rejected.class, () -> limits.enter(false,true));
        limits.enter(false,false).close();
        replay.close(); limits.enter(false,true).close();
    }

    @Test void invalidPoliciesAreRefusedAndProductionValuesAreExplicit() {
        assertThrows(IllegalArgumentException.class, () -> new GuestRequestLimits.Policy(0,1,1,1,1,1,1,1,1,1,1,1));
        var p = GuestRequestLimits.Policy.production();
        assertEquals(8,p.concurrentRequests()); assertEquals(2,p.sessionUsers());
        assertEquals(2,p.concurrentCreations()); assertEquals(2,p.concurrentReplays());
    }
}
