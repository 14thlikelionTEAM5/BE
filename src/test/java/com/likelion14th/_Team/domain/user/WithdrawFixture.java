package com.likelion14th._Team.domain.user;

import com.likelion14th._Team.domain.admission.entity.AdmissionReport;
import com.likelion14th._Team.domain.bookmark.entity.Bookmark;
import com.likelion14th._Team.domain.coursecase.entity.CourseCase;
import com.likelion14th._Team.domain.returnreport.entity.ReturnReport;
import com.likelion14th._Team.domain.returnreport.entity.ReturnReportCourse;
import com.likelion14th._Team.domain.studyplan.entity.StudyPlan;
import com.likelion14th._Team.domain.studyplan.entity.StudyPlanItem;
import com.likelion14th._Team.domain.university.entity.University;
import com.likelion14th._Team.domain.user.entity.Consent;
import com.likelion14th._Team.domain.user.entity.EmailVerification;
import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.domain.user.entity.UserLanguageScore;
import com.likelion14th._Team.domain.user.entity.UserProfile;
import com.likelion14th._Team.global.enums.AdmissionResult;
import com.likelion14th._Team.global.enums.ConsentType;
import com.likelion14th._Team.global.enums.CourseCategory;
import com.likelion14th._Team.global.enums.LanguageTestType;
import com.likelion14th._Team.global.enums.MajorType;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 회원 탈퇴 통합 테스트용 데이터 도우미.
 * 모든 데이터는 PREFIX로 시작하는 이름을 써서 실제 데이터와 구분되고, 정리할 때 접두어로 찾는다.
 */
public class WithdrawFixture {

    public static final String PREFIX = "withdraw-it-";

    /** 트랜잭션이 끝난 뒤에도 쓸 수 있게 엔티티가 아니라 ID만 담는다 */
    public record Scenario(Long userId, String email, String nickname,
                           Long otherUserId, Long universityId,
                           Long courseId, Long otherItemId) {}

    private final EntityManager em;

    public WithdrawFixture(EntityManager em) {
        this.em = em;
    }

    /**
     * 탈퇴할 회원(me)이 모든 종류의 데이터를 가진 상태를 만든다.
     * 다른 회원(other)은 me의 귀국보고서 과목을 참고한 수강계획 항목(source_course_id)을 가진다.
     */
    public Scenario create() {
        String tag = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        University university = University.builder().name(PREFIX + tag).country("테스트").build();
        em.persist(university);

        User me = newUser(PREFIX + tag + "-me@test.com", "wd" + tag);
        User other = newUser(PREFIX + tag + "-other@test.com", "wo" + tag);

        em.persist(UserProfile.builder().gpa(new BigDecimal("3.50")).user(me).build());
        em.persist(UserLanguageScore.builder()
                .testType(LanguageTestType.IELTS).score(new BigDecimal("7.0")).user(me).build());
        em.persist(Consent.builder()
                .consentType(ConsentType.TERMS_OF_SERVICE).consentedAt(LocalDateTime.now()).user(me).build());
        em.persist(EmailVerification.builder()
                .schoolEmail(me.getSchoolEmail()).codeHash("hash")
                .expiresAt(LocalDateTime.now().plusMinutes(10)).lastSentAt(LocalDateTime.now()).build());

        // 내 수강계획 + 항목
        StudyPlan myPlan = StudyPlan.builder().user(me).university(university).name("내 계획").build();
        em.persist(myPlan);
        em.persist(StudyPlanItem.builder().studyPlan(myPlan).itemType("CUSTOM")
                .courseName("직접 입력 과목").foreignCredit(new BigDecimal("3.0")).build());

        // 내 귀국보고서 → 과목 → 인정 사례
        ReturnReport report = ReturnReport.builder().user(me).university(university).semester("9999-1").build();
        em.persist(report);
        ReturnReportCourse course = ReturnReportCourse.builder().returnReport(report)
                .courseNameEn("Withdraw Test Course").foreignCredit(new BigDecimal("3.0")).build();
        em.persist(course);
        em.persist(CourseCase.builder().returnReportCourse(course)
                .recognitionType(CourseCategory.MAJOR_ELECTIVE).recognizedCredit(new BigDecimal("3.0"))
                .majorType(MajorType.PRIMARY).semester("9999-1").build());

        em.persist(AdmissionReport.builder().university(university).user(me).semester("9999-1")
                .result(AdmissionResult.PASS).gpa(new BigDecimal("3.50")).majorType(MajorType.PRIMARY).build());
        em.persist(Bookmark.builder().user(me).university(university).build());

        // 다른 회원의 수강계획 항목이 내 과목을 참고 중
        StudyPlan otherPlan = StudyPlan.builder().user(other).university(university).name("남의 계획").build();
        em.persist(otherPlan);
        StudyPlanItem otherItem = StudyPlanItem.builder().studyPlan(otherPlan).itemType("REGISTERED")
                .courseName("참고한 과목").foreignCredit(new BigDecimal("3.0"))
                .sourceCourseId(course.getId()).build();
        em.persist(otherItem);

        em.flush();
        return new Scenario(me.getId(), me.getSchoolEmail(), me.getNickname(),
                other.getId(), university.getId(), course.getId(), otherItem.getId());
    }

    /** 같은 메일·닉네임으로 다시 가입한 상황을 만들 때 쓴다. 중복이면 flush에서 예외가 난다 */
    public Long signUp(String email, String nickname) {
        User user = newUser(email, nickname);
        em.flush();
        return user.getId();
    }

    /** 남아 있는 행이 있는 테이블 이름 목록. 탈퇴가 끝났다면 비어 있어야 한다 */
    public List<String> nonEmptyTables(Scenario s) {
        Map<String, Long> rows = new LinkedHashMap<>();
        Long id = s.userId();
        rows.put("user", count("select count(x) from User x where x.id = :id", id));
        rows.put("user_profile", count("select count(x) from UserProfile x where x.user.id = :id", id));
        rows.put("user_language_score", count("select count(x) from UserLanguageScore x where x.user.id = :id", id));
        rows.put("consent", count("select count(x) from Consent x where x.user.id = :id", id));
        rows.put("study_plan", count("select count(x) from StudyPlan x where x.user.id = :id", id));
        rows.put("study_plan_item", count("select count(x) from StudyPlanItem x where x.studyPlan.user.id = :id", id));
        rows.put("return_report", count("select count(x) from ReturnReport x where x.user.id = :id", id));
        rows.put("return_report_course",
                count("select count(x) from ReturnReportCourse x where x.returnReport.user.id = :id", id));
        rows.put("course_case",
                count("select count(x) from CourseCase x where x.returnReportCourse.returnReport.user.id = :id", id));
        rows.put("admission_report", count("select count(x) from AdmissionReport x where x.user.id = :id", id));
        rows.put("bookmark", count("select count(x) from Bookmark x where x.user.id = :id", id));
        rows.put("email_verification", em.createQuery(
                        "select count(x) from EmailVerification x where x.schoolEmail = :email", Long.class)
                .setParameter("email", s.email()).getSingleResult());

        List<String> remaining = new ArrayList<>();
        rows.forEach((table, count) -> {
            if (count > 0) {
                remaining.add(table + "=" + count);
            }
        });
        return remaining;
    }

    /** 다른 회원 항목의 source_course_id를 DB에서 직접 읽는다 (영속성 컨텍스트 캐시를 거치지 않는다) */
    public Long sourceCourseIdOf(Long itemId) {
        return em.createQuery("select i.sourceCourseId from StudyPlanItem i where i.id = :id", Long.class)
                .setParameter("id", itemId).getSingleResult();
    }

    public boolean itemExists(Long itemId) {
        return count("select count(i) from StudyPlanItem i where i.id = :id", itemId) > 0;
    }

    private User newUser(String email, String nickname) {
        User user = User.builder().schoolEmail(email).password("encoded-password")
                .majorType(MajorType.PRIMARY).nickname(nickname).build();
        em.persist(user);
        return user;
    }

    private long count(String jpql, Long id) {
        return em.createQuery(jpql, Long.class).setParameter("id", id).getSingleResult();
    }
}