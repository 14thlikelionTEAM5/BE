package com.likelion14th._Team.domain.returnreport.repository;

import com.likelion14th._Team.domain.returnreport.entity.ReturnReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReturnReportRepository extends JpaRepository<ReturnReport, Long> {

    boolean existsByUserIdAndSemester(Long userId, String semester);

    /** 학점 인정 결과 등록 전 확인: 내 해당 학교·학기 귀국보고서가 있는지 */
    boolean existsByUserIdAndUniversityIdAndSemester(Long userId, Long universityId, String semester);

    /** 내 등록 내역 조회: 내 건을 학기 최신순으로 */
    @Query("select r from ReturnReport r join fetch r.university "
            + "where r.user.id = :userId order by r.semester desc, r.id desc")
    List<ReturnReport> findAllByUserIdWithUniversity(@Param("userId") Long userId);
}