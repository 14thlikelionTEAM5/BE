package com.likelion14th._Team.domain.university;

import com.likelion14th._Team.domain.university.entity.University;
import com.likelion14th._Team.domain.university.service.UniversityCourseService;
import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.global.enums.MajorType;
import com.likelion14th._Team.global.exception.CustomException;
import jakarta.persistence.EntityManager;
import jakarta.servlet.Filter;
import com.likelion14th._Team.global.security.JwtProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class UniversityCourseTests {
    @Autowired EntityManager em;
    @Autowired UniversityCourseService service;
    @Autowired WebApplicationContext context;
    @Autowired @Qualifier("springSecurityFilterChain") Filter security;
    @Autowired JwtProvider jwt;
    private MockMvc mvc;
    private University university;
    private User viewer;
    @BeforeEach void setup() {
        university=school();viewer=user();
        mvc=MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    }
    private University school() {
        var u=University.builder().name("courses-test-"+UUID.randomUUID()).country("테스트").build();
        em.persist(u);return u;
    }
    private User user() {
        String key=UUID.randomUUID().toString().replace("-", "");
        var u=User.builder().schoolEmail(key+"@example.hufs.ac.kr").nickname(key.substring(0,12))
                .majorType(MajorType.PRIMARY).password(UUID.randomUUID().toString()).build();
        em.persist(u);return u;
    }
    private long report(University school) {
        em.createNativeQuery("""
                insert into return_report(user_id,university_id,semester,created_at,updated_at)
                values (:user,:university,'2026-1',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """).setParameter("user",user().getId()).setParameter("university",school.getId()).executeUpdate();
        return ((Number)em.createNativeQuery("select LAST_INSERT_ID()").getSingleResult()).longValue();
    }
    private long course(long report, String name, String code) {
        em.createNativeQuery("""
                insert into return_report_course(return_report_id,course_code,course_name_en,foreign_credit,created_at,updated_at)
                values (:report,:code,:name,5.0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """).setParameter("report",report).setParameter("code",code).setParameter("name",name).executeUpdate();
        return ((Number)em.createNativeQuery("select LAST_INSERT_ID()").getSingleResult()).longValue();
    }
    @Test void retainsRepeatedCoursesAcrossReportsAndReturnsRecognitionExistence() {
        long first=report(university),second=report(university);
        long zulu=course(first,"Zulu",null);
        long alpha=course(first,"Alpha","A001");
        long repeated=course(second,"Alpha","A001");
        em.createNativeQuery("""
                insert into course_case(return_report_course_id,recognition_type,recognized_credit,major_type,semester,created_at,updated_at)
                values (:course,'GENERAL',3.0,'PRIMARY','2026-1',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """).setParameter("course",alpha).executeUpdate();
        course(report(school()),"Other School Course","OTHER");
        var courses=service.courses(viewer.getId(),university.getId()).courses();
        assertEquals(3,courses.size());
        assertEquals(alpha,courses.get(0).courseId());assertTrue(courses.get(0).hasRecognition());
        assertEquals(repeated,courses.get(1).courseId());assertFalse(courses.get(1).hasRecognition());
        assertEquals(zulu,courses.get(2).courseId());assertNull(courses.get(2).courseCode());
        assertFalse(courses.get(2).hasRecognition());
        assertEquals(0,new java.math.BigDecimal("5.0").compareTo(courses.get(0).foreignCredit()));
    }
    @Test void noReportsIsEmptyAndMissingSchoolIs404() {
        assertTrue(service.courses(viewer.getId(),university.getId()).courses().isEmpty());
        var error=assertThrows(CustomException.class,() -> service.courses(viewer.getId(),Long.MAX_VALUE));
        assertEquals("E404",error.getErrorCode().getCode());
    }
    @Test void reportWithNoCoursesIsEmptyAndAnonymousIsRejected() {
        report(university);
        assertTrue(service.courses(viewer.getId(),university.getId()).courses().isEmpty());
        var error=assertThrows(CustomException.class,() -> service.courses(null,university.getId()));
        assertEquals("E401",error.getErrorCode().getCode());
    }
    @Test void httpValidatesAuthenticationAndPathAndMissingSchool() throws Exception {
        String path="/api/v1/universities/"+university.getId()+"/courses";
        mvc.perform(get(path)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E401"));
        mvc.perform(get(path).header("Authorization","Bearer invalid"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("E401"));
        String token="Bearer "+jwt.createAccessToken(viewer.getId());
        for(String id : new String[]{"0","-1","abc","1.2","9223372036854775808"}) {
            mvc.perform(get("/api/v1/universities/"+id+"/courses").header("Authorization",token))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andExpect(jsonPath("$.errors[0].field").value("universityId"));
        }
        mvc.perform(get("/api/v1/universities/"+Long.MAX_VALUE+"/courses").header("Authorization",token))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("E404"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 학교입니다"));
    }
    @Test void httpReturnsCoursesAndEmptyArrayAndSwaggerDocumentsRoute() throws Exception {
        String path="/api/v1/universities/"+university.getId()+"/courses";
        String token="Bearer "+jwt.createAccessToken(viewer.getId());
        mvc.perform(get(path).header("Authorization",token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courses").isEmpty());
        long id=course(report(university),"Sample Course",null);
        mvc.perform(get(path).header("Authorization",token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S200"))
                .andExpect(jsonPath("$.message").value("수강 과목 목록 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.universityId").value(university.getId()))
                .andExpect(jsonPath("$.data.courses[0].courseId").value(id))
                .andExpect(jsonPath("$.data.courses[0].courseCode").doesNotExist())
                .andExpect(jsonPath("$.data.courses[0].courseName").value("Sample Course"))
                .andExpect(jsonPath("$.data.courses[0].foreignCredit").value(5.0))
                .andExpect(jsonPath("$.data.courses[0].hasRecognition").value(false));
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/universities/{universityId}/courses'].get").exists());
    }

}
