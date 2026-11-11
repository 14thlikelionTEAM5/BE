package com.likelion14th._Team.domain.university.repository;

import com.likelion14th._Team.domain.university.entity.University;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UniversityRepository extends JpaRepository<University, Long> {

    /** 학교명 부분 일치 검색. %, _ 는 Spring Data가 이스케이프하며, 대소문자 구분은 DB collation을 따른다 */
    List<University> findByNameContainingOrderByNameAsc(String keyword, Pageable pageable);
}
