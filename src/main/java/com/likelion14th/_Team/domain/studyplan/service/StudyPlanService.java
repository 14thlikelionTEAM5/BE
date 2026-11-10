package com.likelion14th._Team.domain.studyplan.service;

import com.likelion14th._Team.domain.course.entity.CreditPolicy;
import com.likelion14th._Team.domain.course.repository.CreditPolicyRepository;
import com.likelion14th._Team.domain.studyplan.dto.StudyPlanCreateRequest;
import com.likelion14th._Team.domain.studyplan.dto.StudyPlanCreateResponse;
import com.likelion14th._Team.domain.studyplan.dto.StudyPlanDeleteResponse;
import com.likelion14th._Team.domain.studyplan.dto.StudyPlanDetail;
import com.likelion14th._Team.domain.studyplan.dto.StudyPlanDetail.Calculation;
import com.likelion14th._Team.domain.studyplan.dto.StudyPlanDetail.Item;
import com.likelion14th._Team.domain.studyplan.dto.StudyPlanRenameResponse;
import com.likelion14th._Team.domain.studyplan.dto.StudyPlanSummary;
import com.likelion14th._Team.domain.studyplan.entity.StudyPlan;
import com.likelion14th._Team.domain.studyplan.entity.StudyPlanItem;
import com.likelion14th._Team.domain.studyplan.repository.StudyPlanRepository;
import com.likelion14th._Team.domain.university.entity.University;
import com.likelion14th._Team.domain.university.repository.UniversityRepository;
import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.domain.user.repository.UserRepository;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import com.likelion14th._Team.global.response.FieldErrorDetail;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@Service
@RequiredArgsConstructor
public class StudyPlanService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int MAX_PLANS = 5;

    private final StudyPlanRepository repository;
    private final ExpectedCreditCalculator creditCalculator;
    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final CreditPolicyRepository creditPolicyRepository;

    @Value("${app.study-plan.credit-policy-department:경영대학}")
    private String creditPolicyDepartment;

    @Transactional
    public StudyPlanCreateResponse create(Long userId, StudyPlanCreateRequest request) {
        // 같은 사용자의 생성 요청은 잠금 이후에 중복 여부와 현재 개수를 확인한다.
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTHENTICATION_REQUIRED));
        University university = universityRepository.findById(request.universityId())
                .orElseThrow(() -> new CustomException(ErrorCode.UNIVERSITY_NOT_FOUND));

        if (repository.existsByUserIdAndUniversityId(userId, university.getId())) {
            throw new CustomException(ErrorCode.STUDY_PLAN_UNIVERSITY_DUPLICATED);
        }
        if (repository.countByUserId(userId) >= MAX_PLANS) {
            throw new CustomException(ErrorCode.STUDY_PLAN_LIMIT_REACHED);
        }

        StudyPlan plan = repository.save(StudyPlan.builder()
                .user(user)
                .university(university)
                .name(request.name())
                .build());
        return new StudyPlanCreateResponse(plan.getId());
    }

    @Transactional(readOnly = true)
    public List<StudyPlanSummary> list(Long userId) {
        List<StudyPlan> plans = repository.findAllForUser(userId);
        if (plans.isEmpty()) {
            return List.of();
        }
        CreditPolicy policy = currentPolicy();
        return plans.stream()
                .map(plan -> summarize(plan, policy))
                .toList();
    }

    @Transactional(readOnly = true)
    public StudyPlanDetail detail(Long userId, Long studyPlanId) {
        StudyPlan plan = repository.findDetailById(studyPlanId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_PLAN_NOT_FOUND));
        if (!plan.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.STUDY_PLAN_FORBIDDEN);
        }

        University university = plan.getUniversity();
        CreditPolicy policy = currentPolicy();
        Calculation calculation = creditCalculator.calculation(university, policy);
        List<Item> items = plan.getItems().stream()
                .map(item -> detailItem(item, calculation, policy))
                .toList();
        BigDecimal total = items.isEmpty() ? new BigDecimal("0.0") : items.stream()
                .map(Item::expectedCredit)
                .filter(Objects::nonNull)
                .reduce(BigDecimal::add)
                .orElse(null);
        BigDecimal semesterLimit = policy == null ? null : BigDecimal.valueOf(policy.getMaxCreditPerSemester());
        return new StudyPlanDetail(plan.getId(), plan.getName(), university.getId(), university.getName(),
                calculation, items, total, semesterLimit,
                total != null && semesterLimit != null && total.compareTo(semesterLimit) > 0);
    }

    @Transactional
    public StudyPlanRenameResponse rename(Long userId, Long studyPlanId, JsonNode nameNode) {
        StudyPlan plan = repository.findById(studyPlanId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_PLAN_NOT_FOUND));
        if (!plan.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.STUDY_PLAN_FORBIDDEN);
        }
        if (nameNode == null || nameNode.isNull()) {
            throw invalidName("수강계획 이름을 입력해 주세요.");
        }
        if (!nameNode.isTextual()) {
            throw invalidName("수강계획 이름은 문자열이어야 합니다.");
        }
        String name = nameNode.stringValue();
        if (name.isBlank()) {
            throw invalidName("수강계획 이름을 공백으로만 입력할 수 없습니다.");
        }
        if (name.codePointCount(0, name.length()) > 100) {
            throw invalidName("수강계획 이름은 최대 100자까지 입력할 수 있습니다.");
        }

        LocalDateTime updatedAt = LocalDateTime.now(SEOUL).withNano(0);
        if (repository.updateName(studyPlanId, userId, name, updatedAt) == 0) {
            throw new CustomException(ErrorCode.STUDY_PLAN_NOT_FOUND);
        }
        return new StudyPlanRenameResponse(studyPlanId, name, updatedAt.atZone(SEOUL).toOffsetDateTime());
    }

    private CustomException invalidName(String reason) {
        return new CustomException(ErrorCode.INVALID_INPUT, "입력값이 올바르지 않습니다.",
                List.of(new FieldErrorDetail("name", reason)));
    }

    @Transactional
    public StudyPlanDeleteResponse delete(Long userId, Long studyPlanId) {
        StudyPlan plan = repository.findForDeleteById(studyPlanId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_PLAN_NOT_FOUND));
        if (!plan.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.STUDY_PLAN_FORBIDDEN);
        }
        repository.delete(plan);
        return new StudyPlanDeleteResponse(studyPlanId);
    }

    private Item detailItem(StudyPlanItem item, Calculation calculation, CreditPolicy policy) {
        Long homeCourseId = item.getHomeCourse() == null ? null : item.getHomeCourse().getId();
        BigDecimal expected = creditCalculator.expectedCredit(item, homeCourseId, calculation, policy);
        String type = switch (item.getItemType()) {
            case "CUSTOM" -> "DIRECT";
            case "REGISTERED" -> "REFERENCE";
            default -> item.getItemType();
        };
        return new Item(item.getId(), type, item.getCourseName(), item.getForeignCredit(),
                "DIRECT".equals(type) ? null : item.getSourceCourseId(), homeCourseId, item.getRecognitionType(),
                expected, expected != null && expected.compareTo(BigDecimal.valueOf(policy.getMaxCreditPerCourse())) > 0);
    }

    private CreditPolicy currentPolicy() {
        return creditPolicyRepository
                .findFirstByDepartmentNameAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        creditPolicyDepartment, LocalDate.now(SEOUL))
                .orElse(null);
    }

    private StudyPlanSummary summarize(StudyPlan plan, CreditPolicy policy) {
        University university = plan.getUniversity();
        return new StudyPlanSummary(
                plan.getId(),
                university.getId(),
                university.getName(),
                plan.getName(),
                plan.getItems().size(),
                creditCalculator.listTotal(university, plan.getItems(), policy),
                plan.getCreatedAt().atZone(SEOUL).toOffsetDateTime(),
                plan.getUpdatedAt().atZone(SEOUL).toOffsetDateTime());
    }
}
