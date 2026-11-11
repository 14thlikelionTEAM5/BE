package com.likelion14th._Team.domain.course.service;

import com.likelion14th._Team.domain.course.dto.HomeCourseSearchResponse;
import com.likelion14th._Team.domain.course.repository.HomeCourseRepository;
import com.likelion14th._Team.global.enums.CourseCategory;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import com.likelion14th._Team.global.response.FieldErrorDetail;
import com.likelion14th._Team.global.util.SearchKeyword;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeCourseService {

    static final int SEARCH_LIMIT = 10;
    static final String RECOGNITION_TYPE_FIELD = "recognitionType";

    /** 본교 과목 검색은 전필·전선만 받는다. 교양(GENERAL)은 과목을 고르지 않으므로 400 */
    private static final List<CourseCategory> SEARCHABLE_TYPES =
            List.of(CourseCategory.MAJOR_REQUIRED, CourseCategory.MAJOR_ELECTIVE);

    private final HomeCourseRepository homeCourseRepository;

    /** 인정 구분과 과목명으로 경영대학 개설 과목 검색. 이름순 최대 10건, 없으면 빈 목록 */
    public List<HomeCourseSearchResponse> search(String rawKeyword, String rawRecognitionType) {
        List<FieldErrorDetail> errors = new ArrayList<>();
        FieldErrorDetail keywordError = SearchKeyword.check(rawKeyword);
        if (keywordError != null) {
            errors.add(keywordError);
        }
        CourseCategory recognitionType = parseRecognitionType(rawRecognitionType, errors);
        if (!errors.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.getMessage(), errors);
        }

        String keyword = rawKeyword.trim();
        try {
            return homeCourseRepository
                    .findByCategoryAndIsBusinessOfferedTrueAndNameContainingOrderByNameAsc(
                            recognitionType, keyword, PageRequest.of(0, SEARCH_LIMIT))
                    .stream()
                    .map(HomeCourseSearchResponse::from)
                    .toList();
        } catch (RuntimeException e) {
            log.error("본교 과목 검색 실패", e);
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "본교 과목 검색 중 오류가 발생했습니다.");
        }
    }

    private CourseCategory parseRecognitionType(String raw, List<FieldErrorDetail> errors) {
        if (raw == null || raw.isBlank()) {
            errors.add(new FieldErrorDetail(RECOGNITION_TYPE_FIELD, "인정 구분을 입력해 주세요"));
            return null;
        }
        for (CourseCategory type : SEARCHABLE_TYPES) {
            if (type.name().equals(raw)) {
                return type;
            }
        }
        errors.add(new FieldErrorDetail(RECOGNITION_TYPE_FIELD, "MAJOR_REQUIRED 또는 MAJOR_ELECTIVE만 사용할 수 있습니다"));
        return null;
    }
}
