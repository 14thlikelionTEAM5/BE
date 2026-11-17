package com.likelion14th._Team.domain.university.repository;

import com.likelion14th._Team.domain.university.repository.projection.UniversityCourseRow;
import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UniversityCourseRepository {
    private final EntityManager em;

    public List<UniversityCourseRow> courses(long universityId) {
        // Hibernate HQL의 SELECT EXISTS로 인정 사례 존재 여부만 조회한다.
        // 과목 이름을 합치거나 인정 사례를 외부 조인하지 않아 보고서별 행 수가 유지된다.
        return em.createQuery("""
                select new com.likelion14th._Team.domain.university.repository.projection.UniversityCourseRow(
                    c.id, c.courseCode, c.courseNameEn, c.foreignCredit,
                    exists (select 1 from CourseCase k where k.returnReportCourse.id = c.id))
                from ReturnReportCourse c
                join c.returnReport r
                where r.university.id = :universityId
                order by lower(c.courseNameEn), c.id
                """, UniversityCourseRow.class)
                .setParameter("universityId", universityId)
                .getResultList();
    }
}
