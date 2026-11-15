package com.likelion14th._Team.domain.university.repository;

import com.likelion14th._Team.domain.coursecase.entity.CourseCase;
import com.likelion14th._Team.domain.returnreport.entity.ReturnReport;
import com.likelion14th._Team.domain.returnreport.entity.ReturnReportCourse;
import com.likelion14th._Team.domain.university.entity.University;
import com.likelion14th._Team.domain.university.repository.projection.UniversityCourseRow;
import com.likelion14th._Team.domain.university.service.UniversityCourseService;
import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.global.enums.CourseCategory;
import com.likelion14th._Team.global.enums.MajorType;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class UniversityCourseRepositoryTests {
    @Autowired EntityManager em;
    @Autowired UniversityCourseRepository repository;
    @Autowired UniversityCourseService service;

    private University school() {
        var school = University.builder().name("projection-" + UUID.randomUUID()).country("테스트").build();
        em.persist(school);
        return school;
    }

    private ReturnReport report(University school) {
        String key = UUID.randomUUID().toString().replace("-", "");
        var user = User.builder().schoolEmail(key + "@example.hufs.ac.kr").nickname(key.substring(0, 12))
                .majorType(MajorType.PRIMARY).password("test-only").build();
        em.persist(user);
        var report = ReturnReport.builder().user(user).university(school).semester("2026-1").build();
        em.persist(report);
        return report;
    }

    private ReturnReportCourse course(ReturnReport report, String name, String code, String credit) {
        var course = ReturnReportCourse.builder().returnReport(report).courseNameEn(name)
                .courseCode(code).foreignCredit(new BigDecimal(credit)).build();
        em.persist(course);
        return course;
    }

    @Test
    void projectsRawValuesKeepsRepeatedNamesAndSchoolBoundaryAndOrdering() {
        var school = school();
        var firstReport = report(school);
        var secondReport = report(school);
        var zulu = course(firstReport, "Zulu", "Z", "6.0");
        var alpha = course(firstReport, "Alpha", "A", "5.0");
        var repeated = course(secondReport, "Alpha", null, "2.5");
        var lower = course(secondReport, "alpha", "a2", "7.0");
        course(report(school()), "Another school", "OTHER", "3.0");
        em.persist(CourseCase.builder().returnReportCourse(alpha).recognitionType(CourseCategory.GENERAL)
                .recognizedCredit(new BigDecimal("3.0")).majorType(MajorType.PRIMARY).semester("2026-1").build());
        em.flush();
        em.clear();

        var rows = repository.courses(school.getId());
        assertEquals(List.of(alpha.getId(), repeated.getId(), lower.getId(), zulu.getId()),
                rows.stream().map(UniversityCourseRow::courseId).toList());
        assertEquals(new UniversityCourseRow(alpha.getId(), "A", "Alpha", new BigDecimal("5.0"), true), rows.get(0));
        assertEquals(new UniversityCourseRow(repeated.getId(), null, "Alpha", new BigDecimal("2.5"), false), rows.get(1));
        assertFalse(rows.get(2).hasRecognition());
        assertFalse(rows.get(3).hasRecognition());
        assertTrue(repository.courses(Long.MAX_VALUE).isEmpty());
    }

    @Test
    void manyCoursesUseOneProjectionQueryWithoutLoadingEntities() {
        var school = school();
        var report = report(school);
        Long viewerId = report.getUser().getId();
        for (int i = 0; i < 100; i++) {
            var course = course(report, String.format("Course %03d", i), null, "5.0");
            if (i % 2 == 0) {
                em.persist(CourseCase.builder().returnReportCourse(course).recognitionType(CourseCategory.GENERAL)
                        .recognizedCredit(new BigDecimal("3.0")).majorType(MajorType.PRIMARY).semester("2026-1").build());
            }
        }
        em.flush();
        em.clear();
        var stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        var rows = repository.courses(school.getId());
        assertEquals(100, rows.size());
        assertEquals(50, rows.stream().filter(UniversityCourseRow::hasRecognition).count());
        assertEquals(1, stats.getPrepareStatementCount());
        assertEquals(0, stats.getEntityLoadCount());
        stats.clear();
        assertEquals(100, service.courses(viewerId, school.getId()).courses().size());
        assertEquals(2, stats.getPrepareStatementCount());
        assertEquals(0, stats.getEntityLoadCount());
    }
}
