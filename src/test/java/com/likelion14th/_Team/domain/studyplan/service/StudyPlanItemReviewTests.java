package com.likelion14th._Team.domain.studyplan.service;

import com.likelion14th._Team.domain.course.repository.HomeCourseRepository;
import com.likelion14th._Team.domain.studyplan.dto.StudyPlanItemCreateRequest;
import com.likelion14th._Team.domain.studyplan.entity.StudyPlan;
import com.likelion14th._Team.domain.studyplan.entity.StudyPlanItem;
import com.likelion14th._Team.domain.studyplan.repository.StudyPlanItemRepository;
import com.likelion14th._Team.domain.studyplan.repository.StudyPlanRepository;
import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.global.exception.CustomException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudyPlanItemReviewTests {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void recognitionTypeRejectsTypoButAcceptsNullAndKnownValues() {
        for (String value : new String[]{"\"GENERL\"", "\"\"", "\"GENERAL \"", "123"}) {
            CustomException exception = assertThrows(CustomException.class,
                    () -> StudyPlanItemCreateRequest.from(mapper.readTree(body(value))));
            assertEquals("recognitionType", exception.getErrors().getFirst().field());
        }
        assertNull(StudyPlanItemCreateRequest.from(mapper.readTree(body("null"))).recognitionType());
        for (String value : new String[]{"GENERAL", "MAJOR_REQUIRED", "MAJOR_ELECTIVE"}) {
            assertEquals(value, StudyPlanItemCreateRequest.from(mapper.readTree(body("\"" + value + "\"")))
                    .recognitionType());
        }
    }

    @Test
    void responseTypesUseSharedMapping() {
        assertEquals("DIRECT", StudyPlanItem.builder().itemType("CUSTOM").build().getResponseItemType());
        assertEquals("REFERENCE", StudyPlanItem.builder().itemType("REGISTERED").build().getResponseItemType());
    }

    @Test
    void deletingItemAlsoUpdatesPlanTimestamp() {
        StudyPlanRepository plans = mock(StudyPlanRepository.class);
        StudyPlanItemRepository items = mock(StudyPlanItemRepository.class);
        HomeCourseRepository homes = mock(HomeCourseRepository.class);
        StudyPlan plan = mock(StudyPlan.class);
        User user = mock(User.class);
        StudyPlanItem item = mock(StudyPlanItem.class);
        when(user.getId()).thenReturn(1L);
        when(plan.getUser()).thenReturn(user);
        when(plans.findForDeleteById(2L)).thenReturn(Optional.of(plan));
        when(items.findByIdAndStudyPlanId(3L, 2L)).thenReturn(Optional.of(item));

        assertEquals(3L, new StudyPlanItemService(plans, items, homes).delete(1L, 2L, 3L).itemId());

        verify(items).delete(item);
        verify(plans).touch(eq(2L), any(LocalDateTime.class));
    }

    private String body(String recognition) {
        return "{\"itemType\":\"CUSTOM\",\"courseName\":\"Test\",\"foreignCredit\":3,"
                + "\"recognitionType\":" + recognition + "}";
    }
}
