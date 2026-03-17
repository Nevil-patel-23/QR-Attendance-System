package com.university.attendance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Provides the BCrypt password encoder as a Spring bean.
 *
 * Why a separate file?
 * Spring Security's SecurityConfig will need a PasswordEncoder bean.
 * If we define it inside SecurityConfig, we can get circular dependency
 * errors (SecurityConfig → UserDetailsService → PasswordEncoder → SecurityConfig).
 * Putting it in its own @Configuration class avoids that entirely.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
