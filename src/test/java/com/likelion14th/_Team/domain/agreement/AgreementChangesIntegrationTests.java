package com.likelion14th._Team.domain.agreement;

import com.likelion14th._Team.domain.agreement.service.AgreementChangesService;
import com.likelion14th._Team.domain.university.entity.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class AgreementChangesIntegrationTests {
    @Autowired EntityManager em;
    @Autowired AgreementChangesService service;
    University school() {
        var school=University.builder().name("changes-test-"+UUID.randomUUID()).country("테스트").build();
        em.persist(school);return school;
    }
    void announcement(University school,String semester,boolean open,Integer quota,String date) {
        em.persist(Agreement.builder().university(school).semester(semester).isOpen(open).quota(quota)
            .announcedAt(LocalDate.parse(date)).importedAt(LocalDateTime.now()).build());
    }
    @Test void databaseSelectsLatestTwoAvailableSemestersAndAggregatesWithoutLoadingUniversities() {
        var changed=school();var closed=school();var added=school();
        announcement(changed,"9998-2",true,2,"2026-01-01");
        announcement(changed,"9999-1",true,4,"2026-07-01");
        announcement(closed,"9998-2",true,1,"2026-01-01");
        announcement(added,"9999-1",true,2,"2026-07-03");
        em.flush();em.clear();
        var result=service.changes();
        assertEquals("9999-1",result.semester());assertEquals("9998-2",result.previousSemester());
        assertEquals(1L,result.newCount());assertEquals(1L,result.quotaChangedCount());assertEquals(1L,result.closedCount());
        assertEquals(LocalDate.parse("2026-07-03"),result.standardDate());
    }
}
