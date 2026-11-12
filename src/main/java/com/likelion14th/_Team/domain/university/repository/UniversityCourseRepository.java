package com.likelion14th._Team.domain.university.repository;

import com.likelion14th._Team.domain.university.dto.UniversityCoursesResponse.Course;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UniversityCourseRepository {
    private final EntityManager em;

    public List<Course> courses(long universityId) {
        // course_case의 엔티티 구현과 독립적으로 ERD의 외래키로 존재 여부만 조회한다.
        List<Object[]> rows = em.createNativeQuery("""
                select c.id, c.course_code, c.course_name_en, c.foreign_credit,
                       exists (select 1 from course_case k where k.return_report_course_id = c.id)
                from return_report_course c
                join return_report r on r.id = c.return_report_id
                where r.university_id = :universityId
                order by lower(c.course_name_en), c.id
                """, Object[].class).setParameter("universityId", universityId).getResultList();
        return rows.stream().map(row -> new Course(((Number) row[0]).longValue(),
                (String) row[1], (String) row[2], (BigDecimal) row[3],
                row[4] instanceof Boolean flag ? flag : ((Number) row[4]).intValue() != 0)).toList();
    }
}
