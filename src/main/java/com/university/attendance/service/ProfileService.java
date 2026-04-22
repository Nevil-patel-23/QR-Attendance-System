package com.university.attendance.service;

import com.university.attendance.dto.request.PasswordChangeRequest;
import com.university.attendance.dto.response.UserProfileResponse;
import com.university.attendance.models.Role;
import com.university.attendance.models.Student;
import com.university.attendance.models.Teacher;
import com.university.attendance.models.User;
import com.university.attendance.repository.StudentRepository;
import com.university.attendance.repository.TeacherRepository;
import com.university.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile() {
        // Identity MUST come entirely from the JWT context
        String prn = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByPrn(prn)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfileResponse.UserProfileResponseBuilder builder = UserProfileResponse.builder()
                .prn(user.getPrn())
                .role(user.getRole());

        if (user.getRole() == Role.STUDENT) {
            Student student = studentRepository.findByPrn(prn)
                    .orElseThrow(() -> new RuntimeException("Student profile not found"));
            builder.firstName(student.getFirstName())
                   .lastName(student.getLastName())
                   .fullName(student.getFirstName() + " " + student.getLastName())
                   .phone(student.getPhone())
                   .courseName(student.getCourse().getName())
                   .semesterLabel("Semester " + student.getCurrentSemester().getSemesterNumber())
                   .batchYear(student.getBatchYear());
        } else if (user.getRole() == Role.TEACHER) {
            Teacher teacher = teacherRepository.findByPrn(prn)
                    .orElseThrow(() -> new RuntimeException("Teacher profile not found"));
            builder.firstName(teacher.getFirstName())
                   .lastName(teacher.getLastName())
                   .fullName(teacher.getFirstName() + " " + teacher.getLastName())
                   .phone(teacher.getPhone())
                   .facultyName(teacher.getFaculty().getName());
        } else {
            builder.firstName("Admin")
                   .lastName("User")
                   .fullName("Admin User");
        }

        return builder.build();
    }

    @Transactional
    public void updatePassword(PasswordChangeRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        // Use context strictly
        String prn = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByPrn(prn)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect old password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
