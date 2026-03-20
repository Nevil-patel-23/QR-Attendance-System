package com.university.attendance.service;

import com.university.attendance.dto.request.LoginRequest;
import com.university.attendance.dto.response.AuthResponse;
import com.university.attendance.exception.ResourceNotFoundException;
import com.university.attendance.exception.ValidationException;
import com.university.attendance.models.Role;
import com.university.attendance.models.Student;
import com.university.attendance.models.Teacher;
import com.university.attendance.models.User;
import com.university.attendance.repository.StudentRepository;
import com.university.attendance.repository.TeacherRepository;
import com.university.attendance.repository.UserRepository;
import com.university.attendance.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
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

        // Resolve actual name based on role
        String name = resolveUserName(user);

        return AuthResponse.builder()
                .token(token)
                .role(user.getRole().name())
                .name(name)
                .prn(user.getPrn())
                .build();
    }

    private String resolveUserName(User user) {
        if (user.getRole() == Role.STUDENT) {
            Optional<Student> student = studentRepository.findByPrn(user.getPrn());
            if (student.isPresent()) {
                return student.get().getFirstName() + " " + student.get().getLastName();
            }
        } else if (user.getRole() == Role.TEACHER) {
            Optional<Teacher> teacher = teacherRepository.findByPrn(user.getPrn());
            if (teacher.isPresent()) {
                return teacher.get().getFirstName() + " " + teacher.get().getLastName();
            }
        }
        return "Admin";
    }
}
