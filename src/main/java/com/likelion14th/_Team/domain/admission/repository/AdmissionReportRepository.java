package com.likelion14th._Team.domain.admission.repository;

import com.likelion14th._Team.domain.admission.entity.AdmissionReport;
import com.likelion14th._Team.global.enums.AdmissionResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdmissionReportRepository extends JpaRepository<AdmissionReport, Long> {

    /** 저장 시 중복 확인: 내 건 중 같은 학교·학기·결과가 있는지 */
    boolean existsByUserIdAndUniversityIdAndSemesterAndResult(
            Long userId, Long universityId, String semester, AdmissionResult result);

    /** 수정 시 중복 확인: 자기 자신은 제외하고 확인 */
    boolean existsByUserIdAndUniversityIdAndSemesterAndResultAndIdNot(
            Long userId, Long universityId, String semester, AdmissionResult result, Long id);
}