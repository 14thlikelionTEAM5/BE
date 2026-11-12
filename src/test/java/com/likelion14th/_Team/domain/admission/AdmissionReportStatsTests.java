package com.likelion14th._Team.domain.admission;

import com.likelion14th._Team.domain.admission.dto.AdmissionReportFilter;
import com.likelion14th._Team.domain.admission.repository.AdmissionReportStatsRepository;
import com.likelion14th._Team.domain.admission.service.AdmissionReportStatsService;
import com.likelion14th._Team.domain.university.entity.University;
import com.likelion14th._Team.domain.user.entity.*;
import com.likelion14th._Team.global.enums.*;
import com.likelion14th._Team.global.exception.CustomException;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AdmissionReportStatsTests {
    @Autowired EntityManager em;
    @Autowired AdmissionReportStatsService service;
    @Autowired AdmissionReportStatsRepository repository;
    private University university;
    private User viewer;
    @BeforeEach void setup() {
        university=University.builder().name("stats-test-"+UUID.randomUUID()).country("테스트").build();
        em.persist(university);
        viewer=user();
        em.persist(UserProfile.builder().user(viewer).gpa(new BigDecimal("4.50")).build());
        em.persist(UserLanguageScore.builder().user(viewer).testType(LanguageTestType.IELTS).score(new BigDecimal("9.0")).build());
    }
    private User user() {
        String key=UUID.randomUUID().toString().replace("-", "");
        User user=User.builder().schoolEmail(key+"@example.hufs.ac.kr").nickname(key.substring(0,12))
                .majorType(MajorType.PRIMARY).password(UUID.randomUUID().toString()).build();
        em.persist(user); return user;
    }
    private void report(String semester,String major,String result,String gpa,String test,String score) {
        em.createNativeQuery("""
                insert into admission_report (university_id,user_id,semester,major_type,result,gpa,language_test_type,language_score,created_at,updated_at)
                values (:university,:user,:semester,:major,:result,:gpa,:test,:score,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """)
                .setParameter("university",university.getId()).setParameter("user",user().getId())
                .setParameter("semester",semester).setParameter("major",major).setParameter("result",result)
                .setParameter("gpa",new BigDecimal(gpa)).setParameter("test",test)
                .setParameter("score",score==null ? null : new BigDecimal(score)).executeUpdate();
    }
    @Test void aggregatesOnlyPassScoresAndDoesNotMixViewerOrTestTypes() {
        report("2026-1","PRIMARY","PASS","3.10","IELTS","6.5");
        report("2026-1","DOUBLE","PASS","4.00","TOEFL_IBT","100");
        report("2025-2","MINOR","PASS","3.80",null,null);
        report("2025-2","PRIMARY","FAIL","4.40","IELTS","9.0");
        var data=service.stats(viewer.getId(),new AdmissionReportFilter(university.getId(),null,null));
        assertEquals(4,data.totalCount()); assertEquals(3,data.passCount()); assertEquals(1,data.failCount());
        assertEquals(0,new BigDecimal("3.10").compareTo(data.gpa().min()));
        assertEquals(0,new BigDecimal("4.00").compareTo(data.gpa().max()));
        assertEquals(2,data.languageScores().size());
        assertEquals("TOEFL_IBT",data.languageScores().get(0).testType());
        assertEquals("IELTS",data.languageScores().get(1).testType());
        assertEquals(0,new BigDecimal("6.5").compareTo(data.languageScores().stream().filter(s -> s.testType().equals("IELTS")).findFirst().orElseThrow().max()));
        assertEquals(0,new BigDecimal("4.50").compareTo(data.myProfile().gpa()));
        assertEquals(1,data.myProfile().languageScores().size());
        assertEquals(java.util.List.of("2026-1","2025-2"),data.availableSemesters());
    }
    @Test void filtersCountsButNotAvailableSemesters() {
        report("2026-1","PRIMARY","PASS","3.10","IELTS",null);
        report("2025-2","DOUBLE","FAIL","3.80","IELTS","7.0");
        var data=service.stats(viewer.getId(),new AdmissionReportFilter(university.getId(),"2026-1","PRIMARY"));
        assertEquals(1,data.totalCount()); assertEquals(1,data.passCount());
        assertTrue(data.languageScores().isEmpty());
        assertEquals(2,data.availableSemesters().size());
        var other=service.stats(viewer.getId(),new AdmissionReportFilter(university.getId(),"2025-2","PRIMARY"));
        assertEquals(0,other.totalCount()); assertNull(other.gpa());
    }
    @Test void emptyReportsAndMissingProfileAreAllowed() {
        var data=service.stats(user().getId(),new AdmissionReportFilter(university.getId(),null,null));
        assertEquals(0,data.totalCount());assertNull(data.gpa());assertNull(data.myProfile().gpa());
        assertTrue(data.languageScores().isEmpty());assertTrue(data.myProfile().languageScores().isEmpty());
        assertTrue(data.availableSemesters().isEmpty());
    }
    @Test void rejectsMalformedFilters() {
        assertThrows(CustomException.class,() -> AdmissionReportFilter.from("0",null,null));
        assertThrows(CustomException.class,() -> AdmissionReportFilter.from("999999999999999999999",null,null));
        assertThrows(CustomException.class,() -> AdmissionReportFilter.from("1","2026-3",null));
        assertThrows(CustomException.class,() -> AdmissionReportFilter.from("1",null,"ALL"));
        assertNull(AdmissionReportFilter.from("1",null,null).semester());
    }
    @Test void listFiltersResultsAndSortsBySemesterThenRegistration() {
        report("2026-1","PRIMARY","PASS","3.10","IELTS","6.5");
        report("2026-1","DOUBLE","FAIL","3.20",null,null);
        report("2025-2","MINOR","PASS","3.30","TOEFL_IBT","100");
        em.createNativeQuery("update admission_report set created_at = '2099-01-01 00:00:00' where university_id = :id and gpa = 3.10")
                .setParameter("id",university.getId()).executeUpdate();
        var all=service.list(viewer.getId(),university.getId(),"ALL");
        assertEquals(3,all.totalCount());
        assertEquals(0,new BigDecimal("3.10").compareTo(all.admissionReports().get(0).gpa()));
        assertEquals(0,new BigDecimal("3.20").compareTo(all.admissionReports().get(1).gpa()));
        assertEquals("2025-2",all.admissionReports().get(2).semester());
        var fail=service.list(viewer.getId(),university.getId(),"FAIL");
        assertEquals(1,fail.totalCount());
        assertNull(fail.admissionReports().getFirst().languageTestType());
        assertNull(fail.admissionReports().getFirst().languageScore());
        assertEquals(2,service.list(viewer.getId(),university.getId(),"PASS").totalCount());
    }
    @Test void emptyListDoesNotRequireOwnReportAndUnknownSchoolIs404() {
        var data=service.list(viewer.getId(),university.getId(),"ALL");
        assertEquals(0,data.totalCount());assertTrue(data.admissionReports().isEmpty());
        var error=assertThrows(CustomException.class,() -> service.list(viewer.getId(),Long.MAX_VALUE,"ALL"));
        assertEquals("E404",error.getErrorCode().getCode());
    }
}
