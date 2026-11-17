package com.likelion14th._Team.domain.university;

import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.domain.university.entity.University;
import com.likelion14th._Team.domain.course.entity.HomeCourse;
import com.likelion14th._Team.domain.coursecase.entity.CourseCase;
import com.likelion14th._Team.domain.returnreport.entity.*;
import com.likelion14th._Team.domain.university.service.UniversityCourseCaseService;
import com.likelion14th._Team.domain.university.dto.UniversityCourseCasesResponse.Item;
import com.likelion14th._Team.global.enums.*;
import com.likelion14th._Team.global.exception.CustomException;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest @Transactional
class UniversityCourseCaseIntegrationTests {
    @Autowired EntityManager em;
    @Autowired UniversityCourseCaseService service;
    University school() {
        var u=University.builder().name("case-test-"+UUID.randomUUID()).country("테스트").build();em.persist(u);return u;
    }
    CourseCase row(University school,String semester,String date,HomeCourse home) {
        String key=UUID.randomUUID().toString().replace("-","");
        var u=User.builder().schoolEmail(key+"@example.hufs.ac.kr").nickname(key.substring(0,12)).password("test-only").majorType(MajorType.PRIMARY).build();em.persist(u);
        var r=ReturnReport.builder().user(u).university(school).semester(semester).build();em.persist(r);
        var course=ReturnReportCourse.builder().returnReport(r).courseNameEn("Same course name").foreignCredit(new BigDecimal("5.0")).build();em.persist(course);
        var c=CourseCase.builder().returnReportCourse(course).homeCourse(home).recognitionType(home==null?CourseCategory.GENERAL:CourseCategory.MAJOR_REQUIRED)
            .recognizedCredit(new BigDecimal("3.0")).majorType(MajorType.MINOR).semester(semester).build();em.persist(c);em.flush();
        em.createNativeQuery("update course_case set created_at=:date where id=:id").setParameter("date",date).setParameter("id",c.getId()).executeUpdate();return c;
    }
    @Test void allAuthorsSchoolIsolationOrderingNullAndNoNPlusOne() {
        var u=school();
        var home=HomeCourse.builder().code("T"+UUID.randomUUID().toString().substring(0,8)).name("마케팅관리").credit(new BigDecimal("3.0")).category(CourseCategory.MAJOR_REQUIRED).baseYear("2026-2027").build();em.persist(home);
        var low=row(u,"2026-2","2026-07-03 00:00:00",home);
        var high=row(u,"2026-2","2026-07-03 00:00:00",null);
        var older=row(u,"2026-2","2026-07-01 00:00:00",null);
        var previous=row(u,"2025-2","2026-09-01 00:00:00",null);
        row(school(),"2027-1","2026-09-01 00:00:00",null);
        em.clear();var stats=em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();stats.setStatisticsEnabled(true);stats.clear();
        var result=service.cases(u.getId());
        assertEquals(List.of(high.getId(),low.getId(),older.getId(),previous.getId()),result.courseCases().stream().map(Item::courseCaseId).toList());
        assertEquals(2,stats.getPrepareStatementCount());
        assertNull(result.courseCases().getFirst().homeCourseName());
        assertEquals("마케팅관리",result.courseCases().get(1).homeCourseName());
        assertEquals(MajorType.MINOR,result.courseCases().getFirst().majorType());
        assertEquals(4,result.courseCases().size());
    }
    @Test void emptySchoolAndMissingSchool() {
        assertTrue(service.cases(school().getId()).courseCases().isEmpty());
        assertThrows(CustomException.class,()->service.cases(Long.MAX_VALUE));
    }
    @Test void generalWithLegacyHomeCourseStillReturnsNullName() {
        var home=HomeCourse.builder().name("본교 과목").build();
        var course=ReturnReportCourse.builder().courseNameEn("Foreign").build();
        var c=CourseCase.builder().returnReportCourse(course).homeCourse(home).recognitionType(CourseCategory.GENERAL).build();
        assertNull(Item.from(c).homeCourseName());
    }
}
