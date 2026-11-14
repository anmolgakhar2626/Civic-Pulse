package com.smartcity.priority;

public enum PriorityTier {
    CRITICAL(1, 30, "<= 30 minutes", 4),
    HIGH(2, 240, "2–4 hours", 3),
    MEDIUM(3, 1440, "12–24 hours", 2),
    LOW(4, 4320, "48–72 hours", 1);

    private final int tierNumber;
    private final int targetSlaMinutes;
    private final String targetSlaCategory;
    private final int precedenceRank;

    PriorityTier(int tierNumber, int targetSlaMinutes, String targetSlaCategory, int precedenceRank) {
        this.tierNumber = tierNumber;
        this.targetSlaMinutes = targetSlaMinutes;
        this.targetSlaCategory = targetSlaCategory;
        this.precedenceRank = precedenceRank;
    }

    public int getTierNumber() {
        return tierNumber;
    }

    public int getTargetSlaMinutes() {
        return targetSlaMinutes;
    }

    public String getTargetSlaCategory() {
        return targetSlaCategory;
    }

    public int getPrecedenceRank() {
        return precedenceRank;
    }

    public boolean isHigherThan(PriorityTier other) {
        if (other == null) {
            return true;
        }
        return this.precedenceRank > other.precedenceRank;
    }

    public static PriorityTier fromString(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String clean = text.trim().toUpperCase();
        for (PriorityTier tier : values()) {
            if (tier.name().equals(clean) || ("TIER_" + tier.tierNumber).equals(clean) || ("TIER" + tier.tierNumber).equals(clean)) {
                return tier;
            }
        }
        return null;
    }
}
