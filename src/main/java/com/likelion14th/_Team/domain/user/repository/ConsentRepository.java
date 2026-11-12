package com.likelion14th._Team.domain.user.repository;

import com.likelion14th._Team.domain.user.entity.Consent;
import com.likelion14th._Team.global.enums.ConsentType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentRepository extends JpaRepository<Consent, Long> {

    Optional<Consent> findByUserIdAndConsentType(Long userId, ConsentType consentType);

    boolean existsByUserIdAndConsentType(Long userId, ConsentType consentType);
}
