package com.likelion14th._Team.domain.studyplan.repository;

import com.likelion14th._Team.domain.studyplan.entity.StudyPlanItem;
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
}
