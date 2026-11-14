package com.smartcity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

@SpringBootApplication
@EnableScheduling
public class CivicPulseApplication {

    private static final Logger log = LoggerFactory.getLogger(CivicPulseApplication.class);

    public static void main(String[] args) {
        loadDotEnvIfPresent();
        SpringApplication.run(CivicPulseApplication.class, args);
    }

    private static void loadDotEnvIfPresent() {
        File[] candidates = new File[] {
                new File(".env"),
                new File("../.env"),
                new File(System.getProperty("user.dir"), ".env"),
                new File(System.getProperty("user.dir"), "../.env")
        };

        for (File envFile : candidates) {
            if (envFile.exists() && envFile.isFile()) {
                try {
                    List<String> lines = Files.readAllLines(envFile.toPath());
                    int loadedCount = 0;
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                            continue;
                        }
                        int eqIdx = trimmed.indexOf('=');
                        String key = trimmed.substring(0, eqIdx).trim();
                        String value = trimmed.substring(eqIdx + 1).trim();

                        if ((value.startsWith("\"") && value.endsWith("\"")) ||
                            (value.startsWith("'") && value.endsWith("'"))) {
                            value = value.substring(1, value.length() - 1);
                        }

                        if (System.getenv(key) == null && System.getProperty(key) == null) {
                            System.setProperty(key, value);
                            loadedCount++;
                        }
                    }
                    log.info("Loaded {} environment properties from local {}", loadedCount, envFile.getAbsolutePath());
                    break;
                } catch (Exception e) {
                    log.warn("Failed to parse .env file at {}: {}", envFile.getAbsolutePath(), e.getMessage());
                }
            }
        }
    }
}
