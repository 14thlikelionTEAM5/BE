package com.likelion14th._Team.domain.university.repository;

import com.likelion14th._Team.domain.university.entity.University;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UniversityRepository extends JpaRepository<University, Long> {

    /** 목록과 동일하게 대소문자·악센트를 무시하며, 검색어의 특수문자는 이스케이프한다. */
    default List<University> searchByName(String keyword, Pageable pageable) {
        return searchByNamePattern(UniversityNamePattern.contains(keyword), pageable);
    }

    @Query("""
            select u from University u
            where collate(u.name as utf8mb4_0900_ai_ci) like :pattern escape '!'
            order by collate(u.name as utf8mb4_0900_ai_ci), u.id
            """)
    List<University> searchByNamePattern(@Param("pattern") String pattern, Pageable pageable);
}
