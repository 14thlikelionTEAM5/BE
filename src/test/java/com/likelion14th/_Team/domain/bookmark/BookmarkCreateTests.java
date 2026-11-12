package com.likelion14th._Team.domain.bookmark;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.likelion14th._Team.domain.bookmark.repository.BookmarkRepository;
import com.likelion14th._Team.domain.university.entity.University;
import com.likelion14th._Team.domain.university.service.UniversityQueryService;
import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.global.enums.MajorType;
import com.likelion14th._Team.global.security.JwtProvider;
import jakarta.persistence.EntityManager;
import jakarta.servlet.Filter;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class BookmarkCreateTests {
    private static final String URL = "/api/v1/bookmarks";
    @Autowired EntityManager em;
    @Autowired BookmarkRepository bookmarks;
    @Autowired UniversityQueryService universities;
    @Autowired JwtProvider jwt;
    @Autowired WebApplicationContext context;
    @Autowired @Qualifier("springSecurityFilterChain") Filter security;
    @Value("${jwt.secret}") String signingKey;
    private MockMvc mvc;
    private User viewer;
    private University school;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
        viewer = user();
        school = University.builder().name("bookmark-" + UUID.randomUUID()).country("테스트").build();
        em.persist(school);
        em.flush();
    }

    private User user() {
        String key = UUID.randomUUID().toString().replace("-", "");
        User user = User.builder().schoolEmail(key + "@example.hufs.ac.kr")
                .nickname(key.substring(0, 12)).majorType(MajorType.PRIMARY)
                .password(UUID.randomUUID().toString()).build();
        em.persist(user);
        return user;
    }

    private String authorization(User user) {
        return "Bearer " + jwt.createAccessToken(user.getId());
    }

    private String body() {
        return "{\"universityId\":" + school.getId() + "}";
    }

    @Test
    void createsForAuthenticatedOwnerAndUpdatesUniversityBookmarkFlag() throws Exception {
        User other = user();
        assertFalse(universities.detail(viewer.getId(), school.getId(), null).isBookmarked());
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorization(viewer))
                        .content("{\"universityId\":" + school.getId() + ",\"userId\":" + other.getId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("S201"))
                .andExpect(jsonPath("$.message").value("찜 목록에 추가했습니다."))
                .andExpect(jsonPath("$.data.universityId").value(school.getId()))
                .andExpect(jsonPath("$.data.isBookmarked").value(true))
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
        assertTrue(bookmarks.existsByUserIdAndUniversityId(viewer.getId(), school.getId()));
        assertFalse(bookmarks.existsByUserIdAndUniversityId(other.getId(), school.getId()));
        assertTrue(universities.detail(viewer.getId(), school.getId(), null).isBookmarked());
    }

    @Test
    void duplicateIs409ButDifferentUserCanBookmarkSameSchool() throws Exception {
        User other = user();
        for (User owner : List.of(viewer, other)) {
            mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body())
                            .header("Authorization", authorization(owner)))
                    .andExpect(status().isCreated());
        }
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body())
                        .header("Authorization", authorization(viewer)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("E409"))
                .andExpect(jsonPath("$.message").value("이미 찜한 학교입니다"))
                .andExpect(jsonPath("$.data").doesNotExist());
        Number count = (Number) em.createNativeQuery(
                "select count(*) from bookmark where user_id=:user and university_id=:school")
                .setParameter("user", viewer.getId()).setParameter("school", school.getId()).getSingleResult();
        assertEquals(1, count.intValue());
    }

    @Test
    void invalidIdsReturnFieldErrorsWithoutSaving() throws Exception {
        for (String json : List.of("{}", "{\"universityId\":null}", "{\"universityId\":0}",
                "{\"universityId\":-1}", "{\"universityId\":1.5}", "{\"universityId\":\"4\"}",
                "{\"universityId\":true}", "{\"universityId\":9223372036854775808}")) {
            mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json)
                            .header("Authorization", authorization(viewer)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("E400"))
                    .andExpect(jsonPath("$.message").value("입력값이 올바르지 않습니다"))
                    .andExpect(jsonPath("$.errors[0].field").value("universityId"));
        }
        assertFalse(bookmarks.existsByUserIdAndUniversityId(viewer.getId(), school.getId()));
    }

    @Test
    void malformedOrMissingBodiesReturnFieldErrors() throws Exception {
        for (String json : List.of("", "null", "[]", "{", "\"text\"")) {
            mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json)
                            .header("Authorization", authorization(viewer)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("body"));
        }
    }

    @Test
    void missingSchoolIs404() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"universityId\":9223372036854775807}")
                        .header("Authorization", authorization(viewer)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("E404"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 학교입니다"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void authenticationIsCheckedBeforeRequestValidation() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
        String expired = new JwtProvider(signingKey, -60).createAccessToken(viewer.getId());
        for (String token : List.of("invalid", expired)) {
            mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("E401"))
                    .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
        }
    }
    @Test
    void swaggerDocuments201AndRequiredUniversityId() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/bookmarks'].post.responses['201']").exists())
                .andExpect(jsonPath("$.components.schemas.BookmarkCreateRequest.required[0]").value("universityId"));
    }

}
