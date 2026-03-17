package com.university.attendance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Central application configuration.
 *
 * Reads custom properties from application.properties (which in turn
 * pulls values from .env via system properties set in the main class).
 *
 * Any service that needs these values can inject this class directly
 * instead of scattering @Value annotations across the codebase.
 */
@Configuration
@Getter
@Slf4j
public class AppConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiry.ms}")
    private long jwtExpiryMs;

    @Value("${qr.expiry.seconds}")
    private int qrExpirySeconds;

    @Value("${app.minimum.attendance.percent:75}")
    private int minimumAttendancePercent;

    /**
     * Runs once after Spring injects all values.
     * Logs a confirmation so you can verify .env loaded correctly.
     */
    @PostConstruct
    public void init() {
        log.info("=== AppConfig loaded ===");
        log.info("JWT expiry          : {} ms", jwtExpiryMs);
        log.info("QR expiry           : {} seconds", qrExpirySeconds);
        log.info("Min attendance      : {}%", minimumAttendancePercent);
    }
}
