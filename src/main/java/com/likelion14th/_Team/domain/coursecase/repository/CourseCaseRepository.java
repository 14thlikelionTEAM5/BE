package com.likelion14th._Team.domain.coursecase.repository;

import com.likelion14th._Team.domain.coursecase.entity.CourseCase;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseCaseRepository extends JpaRepository<CourseCase, Long> {

    boolean existsByReturnReportCourseId(Long returnReportCourseId);

    /** 수정 시 자기 자신을 뺀 중복 확인 */
    boolean existsByReturnReportCourseIdAndIdNot(Long returnReportCourseId, Long id);

    /** 귀국보고서 수정·삭제 때 과목 행을 지우기 전에 딸린 인정 사례를 먼저 지운다 */
    @Modifying(flushAutomatically = true)
    @Query("delete from CourseCase c where c.returnReportCourse.id in :courseIds")
    int deleteByReturnReportCourseIds(@Param("courseIds") Collection<Long> courseIds);
}