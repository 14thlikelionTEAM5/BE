package com.likelion14th._Team.domain.studyplan.repository;

import com.likelion14th._Team.domain.studyplan.entity.StudyPlan;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {

    boolean existsByUserIdAndUniversityId(Long userId, Long universityId);

    long countByUserId(Long userId);

    @Modifying
    @Query("""
            update StudyPlan plan set plan.name = :name, plan.updatedAt = :updatedAt
            where plan.id = :id and plan.user.id = :userId
            """)
    int updateName(@Param("id") Long id, @Param("userId") Long userId,
                   @Param("name") String name, @Param("updatedAt") LocalDateTime updatedAt);

    @EntityGraph(attributePaths = {"university", "items", "items.homeCourse"})
    Optional<StudyPlan> findDetailById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StudyPlan> findForDeleteById(Long id);

    @Modifying
    @Query("update StudyPlan plan set plan.updatedAt = :updatedAt where plan.id = :id")
    void touch(@Param("id") Long id, @Param("updatedAt") LocalDateTime updatedAt);

    @Query("""
            select plan from StudyPlan plan
            join fetch plan.university
            left join fetch plan.items
            where plan.user.id = :userId
            order by plan.updatedAt desc, plan.id desc
            """)
    List<StudyPlan> findAllForUser(@Param("userId") Long userId);

    /** 회원 탈퇴: 항목(study_plan_item)을 먼저 지운 뒤 호출 */
    @Modifying(flushAutomatically = true)
    @Query("delete from StudyPlan plan where plan.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
