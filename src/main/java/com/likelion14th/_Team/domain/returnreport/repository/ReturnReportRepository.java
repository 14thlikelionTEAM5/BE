package com.likelion14th._Team.domain.returnreport.repository;

import com.likelion14th._Team.domain.returnreport.entity.ReturnReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReturnReportRepository extends JpaRepository<ReturnReport, Long> {

    boolean existsByUserIdAndSemester(Long userId, String semester);

    /** 학점 인정 결과 등록 전 확인: 내 해당 학교·학기 귀국보고서가 있는지 */
    boolean existsByUserIdAndUniversityIdAndSemester(Long userId, Long universityId, String semester);
}