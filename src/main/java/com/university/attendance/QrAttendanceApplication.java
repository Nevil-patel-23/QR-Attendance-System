package com.university.attendance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Entry point for the QR-Based University Attendance System.
 *
 * What this class does:
 * 1. Loads secrets from the .env file BEFORE Spring starts,
 *    so ${DB_URL}, ${JWT_SECRET} etc. are available in application.properties.
 * 2. Enables @Scheduled methods (needed for the background job
 *    that inserts ABSENT records after a QR session expires).
 * 3. Boots the entire Spring application.
 */
@SpringBootApplication
@EnableScheduling
public class QrAttendanceApplication {

    public static void main(String[] args) {

        // Load .env file and push every variable into Java system properties.
        // Spring Boot reads system properties, so ${DB_URL} in
        // application.properties will resolve to whatever is in .env.
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()   // don't crash if .env is absent (e.g. in production)
                .load();

        dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
        );

        SpringApplication.run(QrAttendanceApplication.class, args);
    }
}
