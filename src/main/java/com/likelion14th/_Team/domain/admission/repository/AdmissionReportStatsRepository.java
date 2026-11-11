package com.likelion14th._Team.domain.admission.repository;

import com.likelion14th._Team.domain.admission.dto.AdmissionReportFilter;
import com.likelion14th._Team.domain.admission.dto.AdmissionReportListResponse;
import com.likelion14th._Team.domain.admission.dto.AdmissionStatsResponse.*;
import com.likelion14th._Team.domain.user.entity.UserLanguageScore;
import com.likelion14th._Team.global.enums.LanguageTestType;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class AdmissionReportStatsRepository {
    private final EntityManager em;

    public List<AdmissionReportListResponse.Item> reports(long universityId, String result) {
        var query = em.createQuery("""
                select new com.likelion14th._Team.domain.admission.dto.AdmissionReportListResponse$Item(
                    a.id, a.semester, a.result, a.majorType, a.gpa, a.languageTestType, a.languageScore)
                from AdmissionReport a where a.university.id = :universityId
                """ + (result.equals("ALL") ? "" : " and a.result = :result")
                + " order by a.semester desc, a.createdAt desc, a.id desc", AdmissionReportListResponse.Item.class)
                .setParameter("universityId", universityId);
        if (!result.equals("ALL")) query.setParameter("result", result);
        return query.getResultList();
    }
    private String conditions(AdmissionReportFilter filter) {
        return " where a.university.id = :universityId"
                + (filter.semester() == null ? "" : " and a.semester = :semester")
                + (filter.majorType() == null ? "" : " and a.majorType = :majorType");
    }
    private <T> TypedQuery<T> query(String select, AdmissionReportFilter filter, String extra, Class<T> type) {
        var q=em.createQuery(select + " from AdmissionReport a" + conditions(filter) + extra, type)
                .setParameter("universityId", filter.universityId());
        if (filter.semester() != null) q.setParameter("semester", filter.semester());
        if (filter.majorType() != null) q.setParameter("majorType", filter.majorType());
        return q;
    }
    public Map<String, Long> counts(AdmissionReportFilter filter) {
        Map<String, Long> counts = new HashMap<>();
        query("select a.result, count(a.id)", filter, " group by a.result", Object[].class)
                .getResultList().forEach(row -> counts.put((String)row[0], (Long)row[1]));
        return counts;
    }
    public GpaRange gpa(AdmissionReportFilter filter) {
        Object[] row=query("select min(a.gpa), max(a.gpa)",filter," and a.result = 'PASS'",Object[].class).getSingleResult();
        return row[0] == null ? null : new GpaRange(new BigDecimal("4.5"), (BigDecimal)row[0], (BigDecimal)row[1]);
    }
    public List<LanguageRange> languageScores(AdmissionReportFilter filter) {
        return query("select a.languageTestType, min(a.languageScore), max(a.languageScore)",filter,
                " and a.result = 'PASS' and a.languageTestType is not null and a.languageScore is not null group by a.languageTestType order by a.languageTestType",Object[].class)
                .getResultList().stream().map(row -> new LanguageRange((String)row[0],(BigDecimal)row[1],(BigDecimal)row[2]))
                .sorted(Comparator.comparingInt((LanguageRange score) -> testOrder(score.testType()))
                        .thenComparing(LanguageRange::testType)).toList();
    }
    public List<String> semesters(long universityId) {
        return em.createQuery("select distinct a.semester from AdmissionReport a where a.university.id = :id order by a.semester desc",String.class)
                .setParameter("id",universityId).getResultList();
    }
    public MyProfile myProfile(Long userId) {
        BigDecimal gpa=em.createQuery("select p.gpa from UserProfile p where p.user.id = :id",BigDecimal.class)
                .setParameter("id",userId).getResultStream().findFirst().orElse(null);
        var scores=em.createQuery("select s from UserLanguageScore s where s.user.id = :id order by s.testType",UserLanguageScore.class)
                .setParameter("id",userId).getResultList().stream()
                .sorted(Comparator.comparingInt(s -> s.getTestType().ordinal()))
                .map(s -> new MyLanguageScore(s.getTestType().name(),s.getScore())).toList();
        return new MyProfile(gpa,scores);
    }
    // 응답 정렬: TOEFL_IBT, TOEFL_NEW, TOEFL_ITP, IELTS. 기존 미정의 값은 마지막에 둔다.
    private int testOrder(String type) {
        try { return LanguageTestType.valueOf(type).ordinal(); }
        catch (IllegalArgumentException exception) { return Integer.MAX_VALUE; }
    }
}
