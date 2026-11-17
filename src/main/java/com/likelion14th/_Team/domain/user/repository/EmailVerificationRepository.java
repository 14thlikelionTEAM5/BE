package com.likelion14th._Team.domain.user.repository;

import com.likelion14th._Team.domain.user.entity.EmailVerification;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findBySchoolEmail(String schoolEmail);
}