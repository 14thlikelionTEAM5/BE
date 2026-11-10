package com.likelion14th._Team.domain.user.repository;

import com.likelion14th._Team.domain.user.entity.Consent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentRepository extends JpaRepository<Consent, Long> {
}