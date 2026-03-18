package com.university.attendance.repository;

import com.university.attendance.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByPrn(String prn);
    Optional<User> findByPrnAndIsActiveTrue(String prn);
}
