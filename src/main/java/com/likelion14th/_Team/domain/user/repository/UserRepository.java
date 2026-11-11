package com.likelion14th._Team.domain.user.repository;

import com.likelion14th._Team.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsBySchoolEmail(String schoolEmail);

    /** 닉네임은 대소문자 구분 없이 중복 불가 */
    boolean existsByNicknameIgnoreCase(String nickname);

    Optional<User> findBySchoolEmail(String schoolEmail);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where user.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}
