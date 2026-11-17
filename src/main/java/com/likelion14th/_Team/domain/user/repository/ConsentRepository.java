package com.likelion14th._Team.domain.user.repository;

import com.likelion14th._Team.domain.user.entity.Consent;
import com.likelion14th._Team.global.enums.ConsentType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsentRepository extends JpaRepository<Consent, Long> {

    Optional<Consent> findByUserIdAndConsentType(Long userId, ConsentType consentType);

    boolean existsByUserIdAndConsentType(Long userId, ConsentType consentType);

    /** 회원 탈퇴 */
    @Modifying(flushAutomatically = true)
    @Query("delete from Consent c where c.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
