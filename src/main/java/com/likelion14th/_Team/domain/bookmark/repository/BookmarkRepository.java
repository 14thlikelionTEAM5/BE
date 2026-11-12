package com.likelion14th._Team.domain.bookmark.repository;

import com.likelion14th._Team.domain.bookmark.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    boolean existsByUserIdAndUniversityId(Long userId, Long universityId);
    @Query("select b from Bookmark b join fetch b.university where b.user.id = :userId order by b.createdAt desc, b.id desc")
    List<Bookmark> findAllForUser(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true)
    @Query("delete from Bookmark b where b.user.id = :userId and b.university.id = :universityId")
    int deleteForUser(@Param("userId") Long userId, @Param("universityId") Long universityId);

    /** 회원 탈퇴 */
    @Modifying(flushAutomatically = true)
    @Query("delete from Bookmark b where b.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}

