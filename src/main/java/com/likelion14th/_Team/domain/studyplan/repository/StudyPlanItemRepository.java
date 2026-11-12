package com.likelion14th._Team.domain.studyplan.repository;

import com.likelion14th._Team.domain.studyplan.entity.StudyPlanItem;
import java.util.Collection;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

public interface StudyPlanItemRepository extends JpaRepository<StudyPlanItem, Long> {

    boolean existsByStudyPlanIdAndSourceCourseId(Long studyPlanId, Long sourceCourseId);

    Optional<StudyPlanItem> findByIdAndStudyPlanId(Long id, Long studyPlanId);

    @Modifying
    @Query("""
            update StudyPlanItem item
            set item.foreignCredit = :credit, item.homeCourse = :homeCourse, item.updatedAt = :updatedAt
            where item.id = :itemId and item.studyPlan.id = :studyPlanId
            """)
    int updateCredits(@Param("studyPlanId") Long studyPlanId, @Param("itemId") Long itemId,
                      @Param("credit") BigDecimal credit,
                      @Param("homeCourse") com.likelion14th._Team.domain.course.entity.HomeCourse homeCourse,
                      @Param("updatedAt") LocalDateTime updatedAt);

    @Query("""
            select course.returnReport.university.id from ReturnReportCourse course
            where course.id = :sourceCourseId
            """)
    Optional<Long> findSourceUniversityId(@Param("sourceCourseId") Long sourceCourseId);

    /** 귀국보고서 과목이 삭제될 때 수강계획 항목의 원본 연결만 끊는다 (항목 자체와 이름·학점은 유지) */
    @Modifying(flushAutomatically = true)
    @Query("update StudyPlanItem item set item.sourceCourseId = null where item.sourceCourseId in :courseIds")
    int clearSourceCourse(@Param("courseIds") Collection<Long> courseIds);

    /** 회원 탈퇴: 내 수강계획에 속한 항목을 모두 삭제 */
    @Modifying(flushAutomatically = true)
    @Query("delete from StudyPlanItem item where item.studyPlan.id in "
            + "(select plan.id from StudyPlan plan where plan.user.id = :userId)")
    int deleteAllByStudyPlanUserId(@Param("userId") Long userId);
}
