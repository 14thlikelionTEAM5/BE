package com.likelion14th._Team.domain.course.repository;

import com.likelion14th._Team.domain.course.entity.CreditPolicy;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditPolicyRepository extends JpaRepository<CreditPolicy, Long> {

    Optional<CreditPolicy> findFirstByDepartmentNameAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
            String departmentName, LocalDate date);
}
