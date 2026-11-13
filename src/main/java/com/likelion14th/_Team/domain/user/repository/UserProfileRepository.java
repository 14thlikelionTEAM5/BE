package com.likelion14th._Team.domain.user.repository;

import com.likelion14th._Team.domain.user.entity.UserProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByUserId(Long userId);

    /** 회원 탈퇴 */
    @Modifying(flushAutomatically = true)
    @Query("delete from UserProfile p where p.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}