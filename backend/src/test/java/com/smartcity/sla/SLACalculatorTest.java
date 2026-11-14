package com.smartcity.sla;

import com.smartcity.priority.PriorityTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SLA Calculator Unit Tests")
class SLACalculatorTest {

    private SLACalculator calculator;
    private final Instant baseTime = Instant.parse("2026-10-04T10:00:00Z");

    @BeforeEach
    void setUp() {
        calculator = new SLACalculator();
    }

    @Test
    @DisplayName("1. CRITICAL tier maps to exactly 30 minutes")
    void testCriticalTierMapping() {
        int minutes = calculator.getTargetMinutes(PriorityTier.CRITICAL);
        assertEquals(30, minutes, "CRITICAL priority must map to 30 minutes");
    }

    @Test
    @DisplayName("2. HIGH tier maps to exactly 240 minutes (4 hours)")
    void testHighTierMapping() {
        int minutes = calculator.getTargetMinutes(PriorityTier.HIGH);
        assertEquals(240, minutes, "HIGH priority must map to 240 minutes (4 hours)");
    }

    @Test
    @DisplayName("3. MEDIUM tier maps to exactly 1440 minutes (24 hours)")
    void testMediumTierMapping() {
        int minutes = calculator.getTargetMinutes(PriorityTier.MEDIUM);
        assertEquals(1440, minutes, "MEDIUM priority must map to 1440 minutes (24 hours)");
    }

    @Test
    @DisplayName("4. LOW tier maps to exactly 4320 minutes (72 hours)")
    void testLowTierMapping() {
        int minutes = calculator.getTargetMinutes(PriorityTier.LOW);
        assertEquals(4320, minutes, "LOW priority must map to 4320 minutes (72 hours)");
    }

    @Test
    @DisplayName("5. Correct deadline calculation using java.time.Instant")
    void testDeadlineCalculation() {
        // Incident created at 10:00, CRITICAL (30m) -> Deadline 10:30
        Instant deadlineCritical = calculator.calculateDeadline(baseTime, 30);
        assertEquals(baseTime.plus(30, ChronoUnit.MINUTES), deadlineCritical);
        assertEquals(Instant.parse("2026-10-04T10:30:00Z"), deadlineCritical);

        // Incident created at 10:00, HIGH (240m) -> Deadline 14:00
        Instant deadlineHigh = calculator.calculateDeadline(baseTime, 240);
        assertEquals(baseTime.plus(4, ChronoUnit.HOURS), deadlineHigh);
        assertEquals(Instant.parse("2026-10-04T14:00:00Z"), deadlineHigh);
    }

    @Test
    @DisplayName("6. ON_TRACK status when more than 25% SLA time remains")
    void testOnTrackStatus() {
        Instant startedAt = baseTime;
        Instant deadlineAt = startedAt.plus(60, ChronoUnit.MINUTES); // 60 min total

        // 30 min passed, 30 min remaining (50% remaining > 25%)
        Instant now = startedAt.plus(30, ChronoUnit.MINUTES);
        SLAStatus status = calculator.calculateStatus(startedAt, deadlineAt, null, now);
        assertEquals(SLAStatus.ON_TRACK, status);

        // 40 min passed, 20 min remaining (33.3% remaining > 25%)
        now = startedAt.plus(40, ChronoUnit.MINUTES);
        status = calculator.calculateStatus(startedAt, deadlineAt, null, now);
        assertEquals(SLAStatus.ON_TRACK, status);
    }

    @Test
    @DisplayName("7. AT_RISK status when 25% or less SLA time remains before deadline")
    void testAtRiskStatus() {
        Instant startedAt = baseTime;
        Instant deadlineAt = startedAt.plus(60, ChronoUnit.MINUTES); // 60 min total, 25% is 15 min

        // Exactly 15 min remaining (25% remaining)
        Instant now = startedAt.plus(45, ChronoUnit.MINUTES);
        SLAStatus status = calculator.calculateStatus(startedAt, deadlineAt, null, now);
        assertEquals(SLAStatus.AT_RISK, status);

        // 10 min remaining (< 25% remaining)
        now = startedAt.plus(50, ChronoUnit.MINUTES);
        status = calculator.calculateStatus(startedAt, deadlineAt, null, now);
        assertEquals(SLAStatus.AT_RISK, status);
    }

    @Test
    @DisplayName("8. BREACHED status when current time > deadline and unresolved")
    void testBreachedStatus() {
        Instant startedAt = baseTime;
        Instant deadlineAt = startedAt.plus(30, ChronoUnit.MINUTES);

        // 1 second after deadline
        Instant now = deadlineAt.plus(1, ChronoUnit.SECONDS);
        SLAStatus status = calculator.calculateStatus(startedAt, deadlineAt, null, now);
        assertEquals(SLAStatus.BREACHED, status);

        // 10 minutes after deadline
        now = deadlineAt.plus(10, ChronoUnit.MINUTES);
        status = calculator.calculateStatus(startedAt, deadlineAt, null, now);
        assertEquals(SLAStatus.BREACHED, status);
    }

    @Test
    @DisplayName("9. RESOLVED status when resolvedAt is populated")
    void testResolvedStatus() {
        Instant startedAt = baseTime;
        Instant deadlineAt = startedAt.plus(30, ChronoUnit.MINUTES);
        Instant resolvedAt = startedAt.plus(15, ChronoUnit.MINUTES);
        Instant now = startedAt.plus(20, ChronoUnit.MINUTES);

        SLAStatus status = calculator.calculateStatus(startedAt, deadlineAt, resolvedAt, now);
        assertEquals(SLAStatus.RESOLVED, status);
    }

    @Test
    @DisplayName("10. Deterministic escalation levels progression for CRITICAL incidents")
    void testCriticalEscalationProgression() {
        // ON_TRACK -> NONE
        assertEquals(EscalationLevel.NONE,
                calculator.calculateEscalationLevel(PriorityTier.CRITICAL, SLAStatus.ON_TRACK, EscalationLevel.NONE));

        // CRITICAL + AT_RISK -> LEVEL_1
        assertEquals(EscalationLevel.LEVEL_1,
                calculator.calculateEscalationLevel(PriorityTier.CRITICAL, SLAStatus.AT_RISK, EscalationLevel.NONE));

        // CRITICAL + BREACHED (first occurrence) -> LEVEL_2
        assertEquals(EscalationLevel.LEVEL_2,
                calculator.calculateEscalationLevel(PriorityTier.CRITICAL, SLAStatus.BREACHED, EscalationLevel.LEVEL_1));

        // CRITICAL + BREACHED + still unresolved -> LEVEL_3
        assertEquals(EscalationLevel.LEVEL_3,
                calculator.calculateEscalationLevel(PriorityTier.CRITICAL, SLAStatus.BREACHED, EscalationLevel.LEVEL_2));

        // CRITICAL + BREACHED with stillUnresolved flag -> LEVEL_3
        assertEquals(EscalationLevel.LEVEL_3,
                calculator.calculateEscalationLevel(PriorityTier.CRITICAL, SLAStatus.BREACHED, true));

        // RESOLVED -> NONE
        assertEquals(EscalationLevel.NONE,
                calculator.calculateEscalationLevel(PriorityTier.CRITICAL, SLAStatus.RESOLVED, EscalationLevel.LEVEL_3));
    }

    @Test
    @DisplayName("12. Resolved SLA cannot become breached even if current time is past deadline")
    void testResolvedSlaCannotBecomeBreached() {
        Instant startedAt = baseTime;
        Instant deadlineAt = startedAt.plus(30, ChronoUnit.MINUTES);
        Instant resolvedAt = startedAt.plus(20, ChronoUnit.MINUTES); // Resolved on time

        // Current time is now 2 hours after deadline!
        Instant farFuture = deadlineAt.plus(2, ChronoUnit.HOURS);
        SLAStatus status = calculator.calculateStatus(startedAt, deadlineAt, resolvedAt, farFuture);
        assertEquals(SLAStatus.RESOLVED, status, "Resolved SLA must never revert to BREACHED");

        // Resolved after breach: resolvedAt is past deadline
        Instant lateResolvedAt = deadlineAt.plus(15, ChronoUnit.MINUTES);
        status = calculator.calculateStatus(startedAt, deadlineAt, lateResolvedAt, farFuture);
        assertEquals(SLAStatus.RESOLVED, status, "SLA resolved after deadline must be marked RESOLVED");
    }

    @Test
    @DisplayName("14. Remaining minutes calculation (positive before deadline, negative after)")
    void testRemainingMinutesCalculation() {
        Instant deadlineAt = baseTime.plus(30, ChronoUnit.MINUTES);

        // 10 minutes remaining
        Instant now = baseTime.plus(20, ChronoUnit.MINUTES);
        assertEquals(10L, calculator.calculateRemainingMinutes(deadlineAt, now));

        // Exactly at deadline
        now = deadlineAt;
        assertEquals(0L, calculator.calculateRemainingMinutes(deadlineAt, now));

        // 15 minutes past deadline
        now = deadlineAt.plus(15, ChronoUnit.MINUTES);
        assertEquals(-15L, calculator.calculateRemainingMinutes(deadlineAt, now));
    }
}
