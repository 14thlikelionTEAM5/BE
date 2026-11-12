package com.likelion14th._Team.domain.university.repository;

import com.likelion14th._Team.domain.university.dto.UniversitySearch;
import com.likelion14th._Team.domain.university.entity.University;
import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.domain.university.service.UniversityQueryService;
import com.likelion14th._Team.global.enums.MajorType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.util.Arrays;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.likelion14th._Team.domain.university.entity.Agreement;
import com.likelion14th._Team.domain.university.entity.AgreementLanguageRequirement;
import com.likelion14th._Team.global.enums.LanguageTestType;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties="spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class UniversityQueryPerformanceTests {
    @Autowired EntityManager em;
    @Autowired EntityManagerFactory emf;
    @Autowired UniversityQueryService service;
    @Test void selectiveSearchMeasuresBoundedDatabaseReads() {
        String key=UUID.randomUUID().toString().replace("-", "");
        User viewer=User.builder().schoolEmail(key+"@example.hufs.ac.kr").nickname(key.substring(0,12))
                .majorType(MajorType.PRIMARY).password(UUID.randomUUID().toString()).build();
        em.persist(viewer);
        String wanted="QueryTarget-"+key;
        String country="Benchmark-"+key;
        for(int i=0;i<100;i++) {
            em.persist(University.builder().name(i==0?wanted:"QueryOther-"+key+"-"+i).country(country).build());
        }
        em.flush(); em.clear();
        var statistics=emf.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        var result=service.list(viewer.getId(),UniversitySearch.from(wanted,country,null,null,null));
        assertEquals(1,result.totalCount());
        assertEquals(wanted,result.universities().getFirst().name());
        long rows=0;
        for(String query:statistics.getQueries()) rows+=statistics.getQueryStatistics(query).getExecutionRowCount();
        assertTrue(statistics.getPrepareStatementCount() <= 5);
        assertEquals(1,statistics.getEntityLoadCount());
        System.out.printf("UNIVERSITY_QUERY_MEASUREMENT queries=%d rows=%d entities=%d%n",
                statistics.getPrepareStatementCount(),rows,statistics.getEntityLoadCount());
        em.clear(); statistics.clear();
        assertEquals(100,service.list(viewer.getId(),UniversitySearch.from(null,country,null,null,null)).totalCount());
        assertTrue(statistics.getPrepareStatementCount() <= 5, "query count must not grow with school count");
        assertEquals(100,statistics.getEntityLoadCount());
    }
    @Test void mixedAgreementsAndRequirementsKeepReadsBounded() {
        String key=UUID.randomUUID().toString().replace("-", "");
        User viewer=User.builder().schoolEmail(key+"@example.hufs.ac.kr").nickname(key.substring(0,12))
                .majorType(MajorType.PRIMARY).password(UUID.randomUUID().toString()).build();
        em.persist(viewer);
        String country="Mixed-"+key;
        for(int i=0;i<100;i++) {
            University school=University.builder().name("MixedSchool-"+key+"-"+i).country(country).build();
            em.persist(school);
            if(i<10) {
                var agreement=Agreement.builder()
                        .university(school).semester("9999-1").quota(2)
                        .minGpa(new BigDecimal("3.0"))
                        .announcedAt(LocalDate.now()).importedAt(LocalDateTime.now()).build();
                em.persist(agreement);
                em.persist(AgreementLanguageRequirement.builder()
                        .agreement(agreement).testType(LanguageTestType.IELTS)
                        .minScore(new BigDecimal("6.5")).build());
            }
        }
        em.flush(); em.clear();
        var statistics=emf.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        var result=service.list(viewer.getId(),UniversitySearch.from(null,country,null,null,null));
        assertEquals(100,result.totalCount());
        assertEquals(10,result.universities().stream().filter(s -> s.agreementId()!=null).count());
        assertEquals(10,result.universities().stream().filter(s -> s.languageRequirements()!=null)
                .mapToLong(s -> s.languageRequirements().size()).sum());
        assertTrue(statistics.getPrepareStatementCount() <= 5, "agreements and requirements must use bounded batch queries");
        assertTrue(Arrays.stream(statistics.getQueries()).anyMatch(q -> q.contains("Agreement a")));
        assertTrue(Arrays.stream(statistics.getQueries()).anyMatch(q -> q.contains("AgreementLanguageRequirement")));
        System.out.printf("UNIVERSITY_MIXED_MEASUREMENT queries=%d entities=%d%n",
                statistics.getPrepareStatementCount(),statistics.getEntityLoadCount());
    }

}
