package com.likelion14th._Team.domain.bookmark;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.likelion14th._Team.domain.bookmark.repository.BookmarkRepository;
import com.likelion14th._Team.domain.bookmark.service.BookmarkService;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class BookmarkDeleteTests {
    @Autowired EntityManager em;
    @Autowired BookmarkService service;
    @Autowired BookmarkRepository bookmarks;
    @Autowired UniversityQueryService universities;
    @Autowired JwtProvider jwt;
    @Autowired WebApplicationContext context;
    @Autowired @Qualifier("springSecurityFilterChain") Filter security;
    private MockMvc mvc;
    private User viewer;
    private University school;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
        viewer = user();
        school = school();
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

    private University school() {
        University school = University.builder().name("bookmark-delete-" + UUID.randomUUID())
                .country("테스트").build();
        em.persist(school);
        return school;
    }

    private String authorization() {
        return "Bearer " + jwt.createAccessToken(viewer.getId());
    }

    private String url(long id) {
        return "/api/v1/bookmarks/" + id;
    }

    @Test
    void deletesOnlyOwnSelectedBookmarkAndClearsUniversityFlag() throws Exception {
        User other = user();
        University otherSchool = school();
        service.create(viewer.getId(), school.getId());
        service.create(other.getId(), school.getId());
        service.create(viewer.getId(), otherSchool.getId());
        mvc.perform(delete(url(school.getId())).header("Authorization", authorization()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S200"))
                .andExpect(jsonPath("$.message").value("찜을 해제했습니다."))
                .andExpect(jsonPath("$.data.universityId").value(school.getId()))
                .andExpect(jsonPath("$.data.isBookmarked").value(false))
                .andExpect(jsonPath("$.errors").doesNotExist());
        assertFalse(bookmarks.existsByUserIdAndUniversityId(viewer.getId(), school.getId()));
        assertTrue(bookmarks.existsByUserIdAndUniversityId(other.getId(), school.getId()));
        assertTrue(bookmarks.existsByUserIdAndUniversityId(viewer.getId(), otherSchool.getId()));
        assertNotNull(em.find(University.class, school.getId()));
        assertFalse(universities.detail(viewer.getId(), school.getId(), null).isBookmarked());
    }

    @Test
    void repeatedDeleteIs404AndReaddingCreatesNewRow() throws Exception {
        service.create(viewer.getId(), school.getId());
        long before = bookmarkId();
        mvc.perform(delete(url(school.getId())).header("Authorization", authorization()))
                .andExpect(status().isOk());
        mvc.perform(delete(url(school.getId())).header("Authorization", authorization()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("E404"))
                .andExpect(jsonPath("$.message").value("찜하지 않은 학교입니다"));
        service.create(viewer.getId(), school.getId());
        assertNotEquals(before, bookmarkId());
    }

    private long bookmarkId() {
        return ((Number) em.createNativeQuery("select id from bookmark where user_id=:user and university_id=:school")
                .setParameter("user", viewer.getId()).setParameter("school", school.getId()).getSingleResult()).longValue();
    }

    @Test
    void missingSchoolAndAnotherUsersBookmarkAre404() throws Exception {
        mvc.perform(delete(url(Long.MAX_VALUE)).header("Authorization", authorization()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("존재하지 않는 학교입니다"));
        User other = user();
        service.create(other.getId(), school.getId());
        mvc.perform(delete(url(school.getId())).header("Authorization", authorization()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("찜하지 않은 학교입니다"))
                .andExpect(jsonPath("$.data").doesNotExist());
        assertTrue(bookmarks.existsByUserIdAndUniversityId(other.getId(), school.getId()));
    }

    @Test
    void invalidPathsReturn400WithFieldErrors() throws Exception {
        for (String id : List.of("0", "-1", "abc", "1.5", "9223372036854775808")) {
            mvc.perform(delete("/api/v1/bookmarks/" + id).header("Authorization", authorization()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("E400"))
                    .andExpect(jsonPath("$.message").value("입력값이 올바르지 않습니다"))
                    .andExpect(jsonPath("$.errors[0].field").value("universityId"));
        }
    }

    @Test
    void invalidAuthenticationCannotDeleteBookmark() throws Exception {
        service.create(viewer.getId(), school.getId());
        mvc.perform(delete(url(school.getId())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
        mvc.perform(delete(url(school.getId())).header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        assertTrue(bookmarks.existsByUserIdAndUniversityId(viewer.getId(), school.getId()));
    }
}
