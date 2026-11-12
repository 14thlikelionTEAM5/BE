package com.likelion14th._Team.domain.user.repository;

import com.likelion14th._Team.domain.user.entity.UserLanguageScore;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserLanguageScoreRepository extends JpaRepository<UserLanguageScore, Long> {

    /** 등록한 순서대로 내려준다 */
    List<UserLanguageScore> findByUserIdOrderByIdAsc(Long userId);

    /** 교체 시 insert보다 먼저 실행되도록 flush한 뒤 지운다 (uk_user_language_score_user_test 충돌 방지) */
    @Modifying(flushAutomatically = true)
    @Query("delete from UserLanguageScore s where s.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}