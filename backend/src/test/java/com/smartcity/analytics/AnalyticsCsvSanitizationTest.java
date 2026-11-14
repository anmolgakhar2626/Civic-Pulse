package com.smartcity.analytics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CSV Formula Injection Sanitization Tests (CWE-1236)")
class AnalyticsCsvSanitizationTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "=cmd|' /C calc'!A0",
            "+1+2+cmd|' /C calc'!A0",
            "-2+3+4",
            "@SUM(1,2)",
            "\tTAB_INJECTION",
            "\rRETURN_INJECTION"
    })
    @DisplayName("Formula injection characters at the start of cells are prepended with single quote")
    void testSanitizeFormulaInjection(String dangerousPayload) {
        String sanitized = AnalyticsServiceImpl.sanitizeCsvField(dangerousPayload);
        // Field must start with ' or "' (if quoted due to commas/newlines)
        assertThat(sanitized).matches("^\"?'[\\s\\S]*");
        assertThat(sanitized).contains(dangerousPayload.replace("\"", "\"\""));
    }

    @Test
    @DisplayName("Safe alphanumeric strings remain unquoted if without commas or newlines")
    void testSafeStringWithoutSpecialCharacters() {
        String input = "Water Leakage Sector 17";
        String sanitized = AnalyticsServiceImpl.sanitizeCsvField(input);
        assertThat(sanitized).isEqualTo("Water Leakage Sector 17");
    }

    @Test
    @DisplayName("Strings with commas or double quotes are properly escaped with double quotes")
    void testCommaAndQuotesEscaping() {
        String input = "Water Main, Burst at \"Sector 17\"";
        String sanitized = AnalyticsServiceImpl.sanitizeCsvField(input);
        // Expecting: "Water Main, Burst at ""Sector 17"""
        assertThat(sanitized).isEqualTo("\"Water Main, Burst at \"\"Sector 17\"\"\"");
    }

    @Test
    @DisplayName("Null value returns empty string safely")
    void testNullValueHandling() {
        String sanitized = AnalyticsServiceImpl.sanitizeCsvField(null);
        assertThat(sanitized).isEqualTo("");
    }
}
