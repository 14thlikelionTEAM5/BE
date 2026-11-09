package com.likelion14th._Team.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * created_at / updated_at 자동 기록을 켠다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
