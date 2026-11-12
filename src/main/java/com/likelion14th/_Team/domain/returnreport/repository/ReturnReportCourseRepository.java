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

    /** 내 귀국보고서들의 수강 교과목을 한 번에 조회 (서비스에서 보고서별로 묶는다) */
    @Query("select c from ReturnReportCourse c join fetch c.returnReport r "
            + "where r.user.id = :userId order by c.id asc")
    List<ReturnReportCourse> findAllByUserId(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true)
    @Query("delete from ReturnReportCourse course where course.returnReport.id = :returnReportId")
    int deleteAllByReturnReportId(@Param("returnReportId") Long returnReportId);
}
