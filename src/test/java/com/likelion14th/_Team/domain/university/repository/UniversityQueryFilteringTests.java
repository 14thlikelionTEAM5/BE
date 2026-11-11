package com.likelion14th._Team.domain.university.repository;

import com.likelion14th._Team.domain.university.dto.*;
import com.likelion14th._Team.domain.university.entity.*;
import com.likelion14th._Team.domain.university.service.UniversityQueryService;
import com.likelion14th._Team.domain.user.entity.*;
import com.likelion14th._Team.domain.bookmark.entity.Bookmark;
import com.likelion14th._Team.global.enums.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UniversityQueryFilteringTests {
    @Autowired EntityManager em;
    @Autowired UniversityQueryService service;
    private User viewer;
    private University alpha, accent, beta;
    private String country, key;
    @BeforeEach void setup() {
        key=UUID.randomUUID().toString().replace("-", "");country="FilterCountry-"+key;
        viewer=User.builder().schoolEmail(key+"@example.hufs.ac.kr").nickname(key.substring(0,12))
                .majorType(MajorType.PRIMARY).password(UUID.randomUUID().toString()).build();
        em.persist(viewer);
        em.persist(UserProfile.builder().user(viewer).gpa(new BigDecimal("3.80")).build());
        em.persist(UserLanguageScore.builder().user(viewer).testType(LanguageTestType.IELTS).score(new BigDecimal("7.0")).build());
        alpha=school("Alpha_100%\\"+key,country);
        accent=school("École "+key,country);
        beta=school("Beta "+key,country);
        school("CaseDifferent "+key,country.toLowerCase(Locale.ROOT));
        school("Trailing "+key,country+" ");
        agreement(alpha,"2099-1",true,1);
        var current=agreement(alpha,"2099-2",true,2);
        requirement(current,LanguageTestType.IELTS,"6.5");
        requirement(current,LanguageTestType.TOEFL_IBT,"90");
        requirement(agreement(beta,"2099-2",false,0),LanguageTestType.IELTS,"6.5");
        agreement(accent,"2099-1",true,1);
        em.persist(Bookmark.builder().user(viewer).university(alpha).build());
        em.createNativeQuery("""
                insert into return_report(user_id,university_id,semester,created_at,updated_at)
                values (:user,:school,'2099-1',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """).setParameter("user",viewer.getId()).setParameter("school",beta.getId()).executeUpdate();
        em.createNativeQuery("""
                insert into admission_report(user_id,university_id,semester,result,major_type,gpa,created_at,updated_at)
                values (:user,:school,'2099-1','FAIL','PRIMARY',3.5,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """).setParameter("user",viewer.getId()).setParameter("school",accent.getId()).executeUpdate();
        em.flush();em.clear();
    }
    private University school(String name,String country) {
        var u=University.builder().name(name).country(country).build();em.persist(u);return u;
    }
    private Agreement agreement(University university,String semester,boolean open,int quota) {
        var a=Agreement.builder().university(university).semester(semester).isOpen(open).quota(quota)
                .minGpa(new BigDecimal("3.00")).costSummary("official-cost")
                .announcedAt(LocalDate.of(2099,1,1)).importedAt(LocalDateTime.of(2099,1,1,0,0)).build();
        em.persist(a);return a;
    }
    private void requirement(Agreement agreement,LanguageTestType type,String score) {
        em.persist(AgreementLanguageRequirement.builder().agreement(agreement).testType(type)
                .minScore(new BigDecimal(score)).conditionNote(score).build());
    }
    private UniversityListResponse list(String keyword,String language,String eligible) {
        return service.list(viewer.getId(),UniversitySearch.from(keyword,country,language,eligible,null));
    }
    @Test void exactCountryAndLiteralCaseInsensitiveSubstringArePreserved() {
        assertEquals(3,list(null,null,null).totalCount());
        for(String literal:List.of("ALPHA","%","_","\\")) {
            var data=list(literal,null,null);assertEquals(1,data.totalCount(),literal);
            assertEquals(alpha.getId(),data.universities().getFirst().universityId());
        }
        assertEquals(0,list("ecole",null,null).totalCount());
        assertEquals(1,list("éCOLE",null,null).totalCount());
    }
    @Test void languageFilterUsesCurrentAgreementButReturnsAllItsRequirements() {
        var data=list(null,"IELTS",null);assertEquals(2,data.totalCount());
        var first=data.universities().getFirst();
        assertEquals(alpha.getId(),first.universityId());assertEquals(2,first.languageRequirements().size());
        assertEquals(1,list(null,"TOEFL_IBT",null).totalCount());
        assertEquals(1,list(null,"IELTS","true").totalCount());
    }
    @Test void announcementsEligibilityBadgesBookmarksAndExperienceFlagsAreUnchanged() {
        var data=list(null,null,null);
        assertEquals("2099-2",data.semester());
        var a=data.universities().stream().filter(s -> s.universityId().equals(alpha.getId())).findFirst().orElseThrow();
        assertEquals("ELIGIBLE",a.eligibility().status());assertTrue(a.isBookmarked());assertFalse(a.hasExperienceData());
        assertEquals(List.of("QUOTA_CHANGED"),a.semesterChanges());assertEquals("official-cost",a.costSummary());
        var missing=data.universities().stream().filter(s -> s.universityId().equals(accent.getId())).findFirst().orElseThrow();
        assertNull(missing.agreementId());assertNull(missing.languageRequirements());assertTrue(missing.hasExperienceData());
        assertEquals(List.of("RECRUITMENT_STOPPED"),missing.semesterChanges());
        var closed=data.universities().stream().filter(s -> s.universityId().equals(beta.getId())).findFirst().orElseThrow();
        assertEquals("NOT_ELIGIBLE",closed.eligibility().status());assertTrue(closed.hasExperienceData());assertFalse(closed.isBookmarked());
        assertEquals(List.of("NEW","RECRUITMENT_STOPPED"),closed.semesterChanges());
    }
    @Test void absentProfileStillAllowsListAndNeedsCheckWhenRequirementsAreKnown() {
        var absent=service.list(Long.MAX_VALUE,UniversitySearch.from("Alpha",country,null,null,null));
        assertEquals("NEEDS_CHECK",absent.universities().getFirst().eligibility().status());
    }
}
