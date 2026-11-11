package com.likelion14th._Team.domain.university.repository;

import com.likelion14th._Team.domain.university.entity.*;
import com.likelion14th._Team.domain.university.dto.UniversityCountriesResponse.Country;
import com.likelion14th._Team.domain.user.entity.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class UniversityQueryRepository {
    private final EntityManager em;

    public Optional<University> university(Long id) {
        return Optional.ofNullable(em.find(University.class, id));
    }
    public List<Agreement> schoolAgreements(Long universityId) {
        return em.createQuery("select a from Agreement a where a.university.id = :id order by a.semester desc", Agreement.class)
                .setParameter("id", universityId).getResultList();
    }
    public List<AgreementLanguageRequirement> agreementRequirements(Long agreementId) {
        return em.createQuery("select r from AgreementLanguageRequirement r where r.agreement.id = :id", AgreementLanguageRequirement.class)
                .setParameter("id", agreementId).getResultList();
    }
    public Optional<Long> studyPlanId(Long userId, Long universityId) {
        return em.createQuery("select p.id from StudyPlan p where p.user.id = :userId and p.university.id = :universityId order by p.id", Long.class)
                .setParameter("userId", userId).setParameter("universityId", universityId)
                .setMaxResults(1).getResultStream().findFirst();
    }

    public List<Country> countries() {
        return em.createQuery("""
                select new com.likelion14th._Team.domain.university.dto.UniversityCountriesResponse$Country(u.country, count(u.id))
                from University u group by u.country order by u.country
                """, Country.class)
                .getResultList();
    }

    public List<University> universities() {
        return em.createQuery("select u from University u", University.class).getResultList();
    }
    public List<String> semesters() {
        return em.createQuery("select distinct a.semester from Agreement a order by a.semester desc", String.class)
                .getResultList();
    }
    public List<Agreement> agreements(List<String> semesters) {
        if (semesters.isEmpty()) return List.of();
        return em.createQuery("select a from Agreement a join fetch a.university where a.semester in :semesters", Agreement.class)
                .setParameter("semesters", semesters).getResultList();
    }
    public List<AgreementLanguageRequirement> requirements(String semester) {
        if (semester == null) return List.of();
        return em.createQuery("select r from AgreementLanguageRequirement r join fetch r.agreement a where a.semester = :semester", AgreementLanguageRequirement.class)
                .setParameter("semester", semester).getResultList();
    }
    public User user(Long id) { return em.find(User.class, id); }
    public Optional<UserProfile> profile(Long id) {
        return em.createQuery("select p from UserProfile p where p.user.id = :id", UserProfile.class)
                .setParameter("id", id).getResultStream().findFirst();
    }
    public List<UserLanguageScore> scores(Long id) {
        return em.createQuery("select s from UserLanguageScore s where s.user.id = :id", UserLanguageScore.class)
                .setParameter("id", id).getResultList();
    }
    public Set<Long> bookmarks(Long id) {
        return new HashSet<>(em.createQuery("select b.university.id from Bookmark b where b.user.id = :id", Long.class)
                .setParameter("id", id).getResultList());
    }
    public Set<Long> experienceUniversities() {
        Set<Long> ids = new HashSet<>(em.createQuery("select distinct r.university.id from ReturnReport r", Long.class).getResultList());
        ids.addAll(em.createQuery("select distinct a.university.id from AdmissionReport a", Long.class).getResultList());
        // 인정 사례는 원본 귀국보고서 과목을 필수 참조하므로 귀국보고서 집합에 포함된다.
        return ids;
    }
}
