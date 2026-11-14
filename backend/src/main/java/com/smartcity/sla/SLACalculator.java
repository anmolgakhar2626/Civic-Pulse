package com.smartcity.sla;

import com.smartcity.priority.PriorityTier;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class SLACalculator {

    public int getTargetMinutes(PriorityTier tier) {
        if (tier == null) {
            throw new IllegalArgumentException("PriorityTier cannot be null for SLA calculation");
        }
        return switch (tier) {
            case CRITICAL -> 30;
            case HIGH -> 240;    // 4 hours
            case MEDIUM -> 1440;  // 24 hours
            case LOW -> 4320;     // 72 hours
        };
    }

    public Instant calculateDeadline(Instant startedAt, int targetMinutes) {
        if (startedAt == null) {
            throw new IllegalArgumentException("startedAt cannot be null");
        }
        return startedAt.plus(targetMinutes, ChronoUnit.MINUTES);
    }

    public SLAStatus calculateStatus(Instant startedAt, Instant deadlineAt, Instant resolvedAt, Instant now) {
        if (resolvedAt != null) {
            return SLAStatus.RESOLVED;
        }

        if (deadlineAt == null || now == null) {
            return SLAStatus.ON_TRACK;
        }

        if (now.isAfter(deadlineAt)) {
            return SLAStatus.BREACHED;
        }

        if (startedAt == null) {
            startedAt = now;
        }

        long totalMillis = Duration.between(startedAt, deadlineAt).toMillis();
        long remainingMillis = Duration.between(now, deadlineAt).toMillis();

        if (totalMillis <= 0 || remainingMillis <= 0) {
            return SLAStatus.BREACHED;
        }

        double ratio = (double) remainingMillis / (double) totalMillis;
        // AT_RISK / WARNING: 25% or less of SLA time remains but deadline has not passed
        if (ratio <= 0.25) {
            return SLAStatus.AT_RISK;
        }

        // ON_TRACK: More than 25% of SLA time remains
        return SLAStatus.ON_TRACK;
    }

    public long calculateRemainingMinutes(Instant deadlineAt, Instant now) {
        if (deadlineAt == null || now == null) {
            return 0L;
        }
        if (now.isBefore(deadlineAt)) {
            return Duration.between(now, deadlineAt).toMinutes();
        } else {
            return -Duration.between(deadlineAt, now).toMinutes();
        }
    }

    public EscalationLevel calculateEscalationLevel(PriorityTier tier, SLAStatus status, EscalationLevel previousLevel) {
        if (status != null && status.isTerminal()) {
            return EscalationLevel.NONE;
        }
        if (status == SLAStatus.ON_TRACK) {
            return EscalationLevel.NONE;
        }

        if (status != null && status.isWarning()) {
            return EscalationLevel.LEVEL_1;
        }

        if (status == SLAStatus.BREACHED) {
            if (tier == PriorityTier.CRITICAL) {
                if (previousLevel == EscalationLevel.LEVEL_2 || previousLevel == EscalationLevel.LEVEL_3) {
                    return EscalationLevel.LEVEL_3;
                }
                return EscalationLevel.LEVEL_2;
            }
            return EscalationLevel.LEVEL_2;
        }

        return previousLevel != null ? previousLevel : EscalationLevel.NONE;
    }

    public EscalationLevel calculateEscalationLevel(PriorityTier tier, SLAStatus status, boolean stillUnresolvedAfterBreach) {
        if (status != null && status.isTerminal()) {
            return EscalationLevel.NONE;
        }
        if (status == SLAStatus.ON_TRACK) {
            return EscalationLevel.NONE;
        }
        if (status != null && status.isWarning()) {
            return EscalationLevel.LEVEL_1;
        }
        if (status == SLAStatus.BREACHED) {
            return stillUnresolvedAfterBreach ? EscalationLevel.LEVEL_3 : EscalationLevel.LEVEL_2;
        }
        return EscalationLevel.NONE;
    }
}
