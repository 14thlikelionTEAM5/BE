package com.likelion14th._Team.domain.returnreport.repository;

import com.likelion14th._Team.domain.returnreport.entity.ReturnReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReturnReportRepository extends JpaRepository<ReturnReport, Long> {

    boolean existsByUserIdAndSemester(Long userId, String semester);
}
