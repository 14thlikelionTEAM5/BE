package com.likelion14th._Team.domain.returnreport.repository;

import com.likelion14th._Team.domain.returnreport.entity.ReturnReportCourse;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReturnReportCourseRepository extends JpaRepository<ReturnReportCourse, Long> {

    List<ReturnReportCourse> findByReturnReportIdOrderByIdAsc(Long returnReportId);

    @Query("select course.id from ReturnReportCourse course where course.returnReport.id = :returnReportId")
    List<Long> findIdsByReturnReportId(@Param("returnReportId") Long returnReportId);

    @Modifying(flushAutomatically = true)
    @Query("delete from ReturnReportCourse course where course.returnReport.id = :returnReportId")
    int deleteAllByReturnReportId(@Param("returnReportId") Long returnReportId);
}
