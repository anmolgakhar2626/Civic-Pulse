package com.smartcity.sla.dto;

public class SlaOverviewResponse {

    private long totalActive;
    private long onTrackCount;
    private long warningCount;
    private long breachedCount;
    private long completedCount;

    public SlaOverviewResponse() {}

    public SlaOverviewResponse(long totalActive, long onTrackCount, long warningCount, long breachedCount, long completedCount) {
        this.totalActive = totalActive;
        this.onTrackCount = onTrackCount;
        this.warningCount = warningCount;
        this.breachedCount = breachedCount;
        this.completedCount = completedCount;
    }

    public long getTotalActive() { return totalActive; }
    public void setTotalActive(long totalActive) { this.totalActive = totalActive; }

    public long getOnTrackCount() { return onTrackCount; }
    public void setOnTrackCount(long onTrackCount) { this.onTrackCount = onTrackCount; }

    public long getWarningCount() { return warningCount; }
    public void setWarningCount(long warningCount) { this.warningCount = warningCount; }

    public long getBreachedCount() { return breachedCount; }
    public void setBreachedCount(long breachedCount) { this.breachedCount = breachedCount; }

    public long getCompletedCount() { return completedCount; }
    public void setCompletedCount(long completedCount) { this.completedCount = completedCount; }
}

