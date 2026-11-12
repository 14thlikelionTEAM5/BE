package com.likelion14th._Team.domain.agreement.repository;

import com.likelion14th._Team.domain.university.entity.Agreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface AgreementChangesRepository extends JpaRepository<Agreement, Long> {
    @Query("select distinct a.semester from Agreement a order by a.semester desc")
    List<String> latestSemesters(Pageable pageable);

    @Query("select a from Agreement a where a.semester in :semesters")
    List<Agreement> forSemesters(@Param("semesters") List<String> semesters);
}
