package com.likelion14th._Team.domain.admission.repository;

import com.likelion14th._Team.domain.admission.entity.AdmissionReport;
import com.likelion14th._Team.global.enums.AdmissionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

public interface AdmissionReportRepository extends JpaRepository<AdmissionReport, Long> {

    /** 저장 시 중복 확인: 내 건 중 같은 학교·학기·결과가 있는지 */
    boolean existsByUserIdAndUniversityIdAndSemesterAndResult(
            Long userId, Long universityId, String semester, AdmissionResult result);

    /** 수정 시 중복 확인: 자기 자신은 제외하고 확인 */
    boolean existsByUserIdAndUniversityIdAndSemesterAndResultAndIdNot(
            Long userId, Long universityId, String semester, AdmissionResult result, Long id);
    /** 내 등록 내역 조회: 내 건을 학기 최신순으로 */
    @Query("select a from AdmissionReport a join fetch a.university "
            + "where a.user.id = :userId order by a.semester desc, a.id desc")
    List<AdmissionReport> findAllByUserIdWithUniversity(@Param("userId") Long userId);

    /** 회원 탈퇴 */
    @Modifying(flushAutomatically = true)
    @Query("delete from AdmissionReport a where a.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}