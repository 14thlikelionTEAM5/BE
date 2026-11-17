package com.likelion14th._Team.domain.university;

import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.domain.university.entity.University;
import com.likelion14th._Team.domain.returnreport.entity.ReturnReport;
import com.likelion14th._Team.domain.university.service.UniversityReturnReportService;
import com.likelion14th._Team.global.enums.MajorType;
import com.likelion14th._Team.global.exception.CustomException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class UniversityReturnReportIntegrationTests {
    @Autowired EntityManager em;
    @Autowired UniversityReturnReportService service;
    University school() {
        var u=University.builder().name("reports-test-"+UUID.randomUUID()).country("테스트").build();em.persist(u);return u;
    }
    ReturnReport report(University school,String semester,String date) {
        String key=UUID.randomUUID().toString().replace("-","");
        var user=User.builder().schoolEmail(key+"@example.hufs.ac.kr").nickname(key.substring(0,12)).password("test-only").majorType(MajorType.PRIMARY).build();em.persist(user);
        var r=ReturnReport.builder().user(user).university(school).semester(semester).costFlight("왕복 $1,200").reviewSummary("저장된 요약").build();em.persist(r);em.flush();
        em.createNativeQuery("update return_report set created_at=:date where id=:id")
            .setParameter("date",date).setParameter("id",r.getId()).executeUpdate();return r;
    }
    @Test void exactSchoolSemesterCreatedAtAndIdOrderingAndRawValues() {
        var u=school();
        var low=report(u,"2026-2","2026-07-03 00:00:00");
        var high=report(u,"2026-2","2026-07-03 00:00:00");
        var older=report(u,"2026-2","2026-07-01 00:00:00");
        var previous=report(u,"2025-2","2026-09-01 00:00:00");
        report(school(),"2027-1","2026-09-01 00:00:00");
        em.clear();
        var result=service.reports(u.getId());
        assertEquals(4,result.totalCount());
        assertEquals(List.of(high.getId(),low.getId(),older.getId(),previous.getId()),result.returnReports().stream().map(x->x.returnReportId()).toList());
        assertEquals("왕복 $1,200",result.returnReports().getFirst().costFlight());
        assertNull(result.returnReports().getFirst().visaType());
        assertEquals("저장된 요약",result.returnReports().getFirst().reviewSummary());
    }
    @Test void schoolWithoutReportsHasEmptyArray() {
        var u=school();var result=service.reports(u.getId());assertEquals(0,result.totalCount());assertTrue(result.returnReports().isEmpty());
    }
    @Test void nonexistentSchoolIs404() {
        assertThrows(CustomException.class,()->service.reports(Long.MAX_VALUE));
    }
}
