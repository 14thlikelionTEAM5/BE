package com.likelion14th._Team.domain.university.repository;

import com.likelion14th._Team.domain.university.entity.*;
import com.likelion14th._Team.domain.university.dto.UniversitySearch;
import com.likelion14th._Team.global.enums.LanguageTestType;
import java.math.BigDecimal;
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

    public record SchoolRow(University university, boolean bookmarked, boolean experienceData) {}
    public record ApplicantRow(BigDecimal gpa, LanguageTestType testType, BigDecimal score) {}

    public List<SchoolRow> schools(Long userId, UniversitySearch search, String semester) {
        StringBuilder hql = new StringBuilder("""
                select new com.likelion14th._Team.domain.university.repository.UniversityQueryRepository$SchoolRow(
                    u,
                    case when exists (select b.id from Bookmark b where b.user.id = :userId and b.university.id = u.id)
                         then true else false end,
                    case when exists (select r.id from ReturnReport r where r.university.id = u.id)
                           or exists (select a.id from AdmissionReport a where a.university.id = u.id)
                         then true else false end)
                from University u where 1 = 1
                """);
        boolean keyword = search.keyword() != null && !search.keyword().isEmpty();
        boolean country = search.country() != null && !search.country().isBlank();
        // LOCATE treats %, _ and backslash literally. Binary collation preserves the former Java accent/case distinction.
        if (keyword) hql.append(" and locate(:keyword, collate(lower(u.name) as utf8mb4_bin)) > 0");
        if (country) hql.append(" and collate(u.country as utf8mb4_bin) = :country and length(u.country) = length(:country)");
        if (search.languageTest() != null) hql.append("""
                 and exists (select requirement.id from AgreementLanguageRequirement requirement
                             where requirement.agreement.university.id = u.id
                               and requirement.agreement.semester = :semester
                               and requirement.testType = :testType)
                """);
        var query = em.createQuery(hql.toString(), SchoolRow.class).setParameter("userId", userId);
        if (keyword) query.setParameter("keyword", search.keyword().toLowerCase(Locale.ROOT));
        if (country) query.setParameter("country", search.country());
        if (search.languageTest() != null) query.setParameter("semester", semester).setParameter("testType", search.languageTest());
        return query.getResultList();
    }
    public List<String> semesters() {
        return em.createQuery("select distinct a.semester from Agreement a order by a.semester desc", String.class)
                .setMaxResults(2).getResultList();
    }
    public List<Agreement> agreementsForSchools(List<String> semesters, List<Long> universityIds) {
        if (semesters.isEmpty() || universityIds.isEmpty()) return List.of();
        return em.createQuery("select a from Agreement a where a.semester in :semesters and a.university.id in :ids", Agreement.class)
                .setParameter("semesters", semesters).setParameter("ids", universityIds).getResultList();
    }
    public List<AgreementLanguageRequirement> requirementsForAgreements(List<Long> agreementIds) {
        if (agreementIds.isEmpty()) return List.of();
        return em.createQuery("select r from AgreementLanguageRequirement r where r.agreement.id in :ids", AgreementLanguageRequirement.class)
                .setParameter("ids", agreementIds).getResultList();
    }
    public List<ApplicantRow> applicant(Long userId) {
        return em.createQuery("""
                select new com.likelion14th._Team.domain.university.repository.UniversityQueryRepository$ApplicantRow(
                    p.gpa, score.testType, score.score)
                from User u
                left join UserProfile p on p.user.id = u.id
                left join UserLanguageScore score on score.user.id = u.id
                where u.id = :userId
                """, ApplicantRow.class).setParameter("userId", userId).getResultList();
    }
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
}
