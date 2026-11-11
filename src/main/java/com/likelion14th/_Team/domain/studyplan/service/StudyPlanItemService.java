package com.likelion14th._Team.domain.studyplan.service;

import com.likelion14th._Team.domain.course.entity.HomeCourse;
import com.likelion14th._Team.domain.course.repository.HomeCourseRepository;
import com.likelion14th._Team.domain.studyplan.dto.StudyPlanItemCreateRequest;
import com.likelion14th._Team.domain.studyplan.dto.StudyPlanItemCreateResponse;
import com.likelion14th._Team.domain.studyplan.dto.StudyPlanItemDeleteResponse;
import com.likelion14th._Team.domain.studyplan.dto.StudyPlanItemUpdateRequest;
import com.likelion14th._Team.domain.studyplan.dto.StudyPlanItemUpdateResponse;
import com.likelion14th._Team.domain.studyplan.entity.StudyPlan;
import com.likelion14th._Team.domain.studyplan.entity.StudyPlanItem;
import com.likelion14th._Team.domain.studyplan.repository.StudyPlanItemRepository;
import com.likelion14th._Team.domain.studyplan.repository.StudyPlanRepository;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@Service
@RequiredArgsConstructor
public class StudyPlanItemService {

    private final StudyPlanRepository planRepository;
    private final StudyPlanItemRepository itemRepository;
    private final HomeCourseRepository homeCourseRepository;

    @Transactional
    public StudyPlanItemDeleteResponse delete(Long userId, Long studyPlanId, Long itemId) {
        StudyPlan plan = planRepository.findForDeleteById(studyPlanId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_PLAN_NOT_FOUND));
        if (!plan.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.STUDY_PLAN_FORBIDDEN);
        }
        StudyPlanItem item = itemRepository.findByIdAndStudyPlanId(itemId, studyPlanId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_PLAN_ITEM_NOT_FOUND));
        itemRepository.delete(item);
        planRepository.touch(studyPlanId, LocalDateTime.now(ZoneId.of("Asia/Seoul")));
        return new StudyPlanItemDeleteResponse(itemId);
    }

    @Transactional
    public StudyPlanItemCreateResponse create(Long userId, Long studyPlanId, JsonNode body) {
        // 계획 잠금을 공유해 원본 과목 중복 추가와 계획 삭제를 직렬화한다.
        StudyPlan plan = planRepository.findForDeleteById(studyPlanId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_PLAN_NOT_FOUND));
        if (!plan.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.STUDY_PLAN_FORBIDDEN);
        }
        StudyPlanItemCreateRequest request = StudyPlanItemCreateRequest.from(body);
        if (request.sourceCourseId() != null) {
            Long sourceUniversityId = itemRepository.findSourceUniversityId(request.sourceCourseId())
                    .orElseThrow(() -> new CustomException(ErrorCode.SOURCE_COURSE_NOT_FOUND));
            if (!sourceUniversityId.equals(plan.getUniversity().getId())) {
                throw new CustomException(ErrorCode.INVALID_INPUT,
                        "원본 과목의 대학이 수강계획 대상 대학과 일치하지 않습니다.");
            }
        }
        HomeCourse home = request.homeCourseId() == null ? null : homeCourseRepository.findById(request.homeCourseId())
                .orElseThrow(() -> new CustomException(ErrorCode.HOME_COURSE_NOT_FOUND));
        if (request.sourceCourseId() != null
                && itemRepository.existsByStudyPlanIdAndSourceCourseId(studyPlanId, request.sourceCourseId())) {
            throw new CustomException(ErrorCode.STUDY_PLAN_ITEM_DUPLICATED);
        }
        StudyPlanItem item = itemRepository.save(StudyPlanItem.builder()
                .studyPlan(plan).itemType(request.itemType()).courseName(request.courseName())
                .foreignCredit(request.foreignCredit()).sourceCourseId(request.sourceCourseId())
                .homeCourse(home).recognitionType(request.recognitionType()).build());
        planRepository.touch(studyPlanId, LocalDateTime.now(ZoneId.of("Asia/Seoul")));
        return new StudyPlanItemCreateResponse(item.getId());
    }

    @Transactional
    public StudyPlanItemUpdateResponse update(Long userId, Long studyPlanId, Long itemId, JsonNode body) {
        StudyPlan plan = planRepository.findForDeleteById(studyPlanId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_PLAN_NOT_FOUND));
        if (!plan.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.STUDY_PLAN_FORBIDDEN);
        }
        StudyPlanItem item = itemRepository.findByIdAndStudyPlanId(itemId, studyPlanId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_PLAN_ITEM_NOT_FOUND));
        StudyPlanItemUpdateRequest request = StudyPlanItemUpdateRequest.from(body);
        BigDecimal credit = request.changeForeignCredit() ? request.foreignCredit() : item.getForeignCredit();
        HomeCourse home = item.getHomeCourse();
        if (request.changeHomeCourse()) {
            home = request.homeCourseId() == null ? null : homeCourseRepository.findById(request.homeCourseId())
                    .orElseThrow(() -> new CustomException(ErrorCode.HOME_COURSE_NOT_FOUND));
        }
        LocalDateTime updatedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul")).withNano(0);
        itemRepository.updateCredits(studyPlanId, itemId, credit, home, updatedAt);
        planRepository.touch(studyPlanId, updatedAt);
        return new StudyPlanItemUpdateResponse(item.getId(), item.getResponseItemType(), item.getCourseName(), credit,
                item.getSourceCourseId(), home == null ? null : home.getId(), item.getRecognitionType(),
                updatedAt.atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime());
    }
}
