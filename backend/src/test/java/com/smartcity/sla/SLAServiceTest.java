package com.smartcity.sla;

import com.smartcity.incident.Incident;
import com.smartcity.incident.IncidentRepository;
import com.smartcity.incident.IncidentSource;
import com.smartcity.incident.IncidentStatus;
import com.smartcity.priority.PriorityTier;
import com.smartcity.sla.dto.SLAResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SLA Service Unit Tests")
class SLAServiceTest {

    @Mock
    private SLARepository slaRepository;

    @Mock
    private IncidentRepository incidentRepository;

    private SLACalculator slaCalculator;
    private SLAServiceImpl slaService;

    private final Instant baseTime = Instant.parse("2026-10-04T10:00:00Z");
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        slaCalculator = new SLACalculator();
        slaService = new SLAServiceImpl(slaRepository, incidentRepository, slaCalculator);
        fixedClock = Clock.fixed(baseTime, ZoneOffset.UTC);
        slaService.setClock(fixedClock);
    }

    private Incident createMockIncident(PriorityTier tier) {
        Incident incident = new Incident();
        incident.setId(UUID.randomUUID());
        incident.setIncidentNumber("INC-1024");
        incident.setIncidentType("FIRE_HAZARD");
        incident.setDescription("Live exposed power line sparking");
        incident.setSource(IncidentSource.IOT);
        incident.setLatitude(30.7333);
        incident.setLongitude(76.7794);
        incident.setReportedAt(baseTime);
        incident.setDetectedAt(baseTime);
        incident.setStatus(IncidentStatus.PRIORITIZED);
        incident.setTier(tier);
        return incident;
    }

    @Test
    @DisplayName("11. SLA cannot be created without priority - throws proper validation error")
    void testCannotCreateSLAWithoutPriority() {
        Incident unprioritizedIncident = createMockIncident(null);
        unprioritizedIncident.setTier(null);
        unprioritizedIncident.setPriority(null);
        unprioritizedIncident.setAssignedTier(null);

        when(incidentRepository.findById(unprioritizedIncident.getId()))
                .thenReturn(Optional.of(unprioritizedIncident));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                slaService.createSLAForIncident(unprioritizedIncident.getId()));

        assertEquals("Incident must be prioritized before SLA creation.", exception.getMessage());
        verify(slaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create SLA successfully for prioritized CRITICAL incident (30m target)")
    void testCreateSLASuccessForCriticalIncident() {
        Incident incident = createMockIncident(PriorityTier.CRITICAL);
        when(incidentRepository.findById(incident.getId())).thenReturn(Optional.of(incident));
        when(slaRepository.findByIncidentId(incident.getId())).thenReturn(Optional.empty());
        when(slaRepository.save(any(SLARecord.class))).thenAnswer(invocation -> {
            SLARecord saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        SLAResponse response = slaService.createSLAForIncident(incident.getId());

        assertNotNull(response);
        assertEquals("INC-1024", response.getIncidentId());
        assertEquals(PriorityTier.CRITICAL, response.getPriorityTier());
        assertEquals(30, response.getTargetMinutes());
        assertEquals(baseTime, response.getStartedAt());
        assertEquals(baseTime.plus(30, ChronoUnit.MINUTES), response.getDeadlineAt());
        assertEquals(SLAStatus.ON_TRACK, response.getStatus());
        assertEquals(30L, response.getRemainingMinutes());
        assertEquals(EscalationLevel.NONE, response.getEscalationLevel());

        // Verify incident was synchronized with SLA deadline and target minutes
        assertEquals(30, incident.getSlaMinutes());
        assertEquals(baseTime.plus(30, ChronoUnit.MINUTES), incident.getSlaDeadline());
        verify(incidentRepository).save(incident);
        verify(slaRepository).save(any(SLARecord.class));
    }

    @Test
    @DisplayName("13. Scheduled SLA evaluation updates remaining time, status, detects breach, and escalates")
    void testScheduledSLAEvaluationLifecycle() {
        Incident incident = createMockIncident(PriorityTier.CRITICAL);
        Instant startedAt = baseTime;
        Instant deadlineAt = startedAt.plus(30, ChronoUnit.MINUTES); // 10:30:00

        SLARecord slaRecord = new SLARecord(incident, PriorityTier.CRITICAL, 30, startedAt, deadlineAt);
        slaRecord.setId(UUID.randomUUID());
        slaRecord.setStatus(SLAStatus.ON_TRACK);
        slaRecord.setEscalationLevel(EscalationLevel.NONE);

        when(slaRepository.findAllActiveSLAs()).thenReturn(List.of(slaRecord));
        when(slaRepository.save(any(SLARecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // Step 1: At 10:24 (6 minutes remaining out of 30 = 20% remaining <= 25%) -> AT_RISK & LEVEL_1
        slaService.setClock(Clock.fixed(startedAt.plus(24, ChronoUnit.MINUTES), ZoneOffset.UTC));
        int count1 = slaService.evaluateAllActiveSLAs();

        assertEquals(1, count1);
        assertEquals(SLAStatus.AT_RISK, slaRecord.getStatus());
        assertEquals(6L, slaRecord.getRemainingMinutes());
        assertEquals(EscalationLevel.LEVEL_1, slaRecord.getEscalationLevel());
        assertFalse(slaRecord.getBreached());

        // Step 2: At 10:31 (1 minute past deadline) -> BREACHED & LEVEL_2
        slaService.setClock(Clock.fixed(deadlineAt.plus(1, ChronoUnit.MINUTES), ZoneOffset.UTC));
        int count2 = slaService.evaluateAllActiveSLAs();

        assertEquals(1, count2);
        assertEquals(SLAStatus.BREACHED, slaRecord.getStatus());
        assertEquals(-1L, slaRecord.getRemainingMinutes());
        assertTrue(slaRecord.getBreached());
        assertNotNull(slaRecord.getBreachedAt());
        assertEquals(EscalationLevel.LEVEL_2, slaRecord.getEscalationLevel());

        // Step 3: At 10:35 (CRITICAL incident remains unresolved after breach) -> LEVEL_3
        slaService.setClock(Clock.fixed(deadlineAt.plus(5, ChronoUnit.MINUTES), ZoneOffset.UTC));
        int count3 = slaService.evaluateAllActiveSLAs();

        assertEquals(1, count3);
        assertEquals(SLAStatus.BREACHED, slaRecord.getStatus());
        assertEquals(-5L, slaRecord.getRemainingMinutes());
        assertEquals(EscalationLevel.LEVEL_3, slaRecord.getEscalationLevel());
    }

    @Test
    @DisplayName("12. Resolved incident sets SLA to RESOLVED and scheduled evaluator cannot revert to BREACHED")
    void testResolvedIncidentIntegrationAndImmutability() {
        Incident incident = createMockIncident(PriorityTier.CRITICAL);
        Instant startedAt = baseTime;
        Instant deadlineAt = startedAt.plus(30, ChronoUnit.MINUTES);

        SLARecord slaRecord = new SLARecord(incident, PriorityTier.CRITICAL, 30, startedAt, deadlineAt);
        slaRecord.setId(UUID.randomUUID());
        slaRecord.setStatus(SLAStatus.BREACHED);
        slaRecord.setBreached(true);
        slaRecord.setEscalationLevel(EscalationLevel.LEVEL_2);

        when(slaRepository.findByIncidentId(incident.getId())).thenReturn(Optional.of(slaRecord));
        when(slaRepository.save(any(SLARecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // Mark incident resolved at 10:35 (after breach)
        Instant resolvedAt = deadlineAt.plus(5, ChronoUnit.MINUTES);
        SLAResponse resolvedResponse = slaService.markIncidentResolved(incident.getId(), resolvedAt);

        assertNotNull(resolvedResponse);
        assertEquals(SLAStatus.RESOLVED, resolvedResponse.getStatus());
        assertEquals(resolvedAt, resolvedResponse.getResolvedAt());
        assertEquals(EscalationLevel.NONE, resolvedResponse.getEscalationLevel());

        // Now run scheduled evaluation 2 hours later!
        slaService.setClock(Clock.fixed(deadlineAt.plus(2, ChronoUnit.HOURS), ZoneOffset.UTC));
        when(slaRepository.findAllActiveSLAs()).thenReturn(List.of(slaRecord));

        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolutionTime(resolvedAt);

        slaService.evaluateAllActiveSLAs();

        // SLA must remain RESOLVED and NEVER revert back to BREACHED
        assertEquals(SLAStatus.RESOLVED, slaRecord.getStatus());
        assertEquals(resolvedAt, slaRecord.getResolvedAt());
        assertEquals(EscalationLevel.NONE, slaRecord.getEscalationLevel());
    }
}
