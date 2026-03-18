package com.university.attendance.service;

import com.university.attendance.dto.request.LoginRequest;
import com.university.attendance.dto.response.AuthResponse;
import com.university.attendance.exception.ResourceNotFoundException;
import com.university.attendance.exception.ValidationException;
import com.university.attendance.models.User;
import com.university.attendance.repository.UserRepository;
import com.university.attendance.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByPrn(request.getPrn())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with PRN: " + request.getPrn()));

        if (!user.getIsActive()) {
            throw new ValidationException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ValidationException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user);
        
        // Return role name as string for now, to be replaced by actual profile lookup later
        String roleStr = user.getRole().name();
        String name = roleStr.substring(0, 1).toUpperCase() + roleStr.substring(1).toLowerCase();

        return AuthResponse.builder()
                .token(token)
                .role(roleStr)
                .name(name)
                .prn(user.getPrn())
                .build();
    }
}
