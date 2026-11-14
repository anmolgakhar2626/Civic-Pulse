package com.smartcity.sla;

import com.smartcity.complaint.ComplaintService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled daemon task periodically evaluating active SLA compliance,
 * approaching deadlines, breach conditions, and multi-tier escalation progression.
 */
@Component
@ConditionalOnProperty(name = "app.sla.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SLAManager {

    private static final Logger log = LoggerFactory.getLogger(SLAManager.class);

    private final SLAService slaService;
    private final ComplaintService complaintService;

    public SLAManager(SLAService slaService, @Autowired(required = false) ComplaintService complaintService) {
        this.slaService = slaService;
        this.complaintService = complaintService;
    }

    /**
     * Periodically evaluates active SLA records with configurable interval.
     */
    @Scheduled(fixedDelayString = "${app.sla.scheduler.interval-ms:30000}", initialDelayString = "${app.sla.scheduler.initial-delay-ms:5000}")
    public void evaluateActiveSLAs() {
        log.debug("Running periodic SLA evaluation and escalation check...");
        try {
            int evaluated = slaService.evaluateAllActiveSLAs();
            if (evaluated > 0) {
                log.info("Periodic incident SLA escalation evaluation completed: {} active SLA record(s) processed.", evaluated);
            }
        } catch (Exception e) {
            log.error("Error during scheduled SLA evaluation: {}", e.getMessage(), e);
        }

        try {
            if (complaintService != null) {
                int evaluatedComplaints = complaintService.evaluateComplaintSlaDeadlines();
                if (evaluatedComplaints > 0) {
                    log.info("Periodic complaint SLA evaluation completed: {} overdue complaint(s) transitioned to BREACHED.", evaluatedComplaints);
                }
            }
        } catch (Exception e) {
            log.error("Error during scheduled complaint SLA evaluation: {}", e.getMessage(), e);
        }
    }
}
