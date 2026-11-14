package com.smartcity.priority;

public enum PublicExposure {
    VERY_HIGH(4),
    HIGH(3),
    MEDIUM(2),
    LOW(1);

    private final int rank;

    PublicExposure(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    public static PublicExposure fromString(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return PublicExposure.valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
