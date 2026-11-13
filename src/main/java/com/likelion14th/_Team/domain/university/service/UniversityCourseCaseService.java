package com.likelion14th._Team.domain.university.service;

import com.likelion14th._Team.domain.coursecase.repository.CourseCaseRepository;
import com.likelion14th._Team.domain.university.repository.UniversityRepository;
import com.likelion14th._Team.domain.university.dto.UniversityCourseCasesResponse;
import com.likelion14th._Team.domain.university.dto.UniversityCourseCasesResponse.Item;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UniversityCourseCaseService {
    private final UniversityRepository universities;
    private final CourseCaseRepository cases;

    @Transactional(readOnly = true)
    public UniversityCourseCasesResponse cases(long universityId) {
        if (!universities.existsById(universityId)) {
            throw new CustomException(ErrorCode.UNIVERSITY_NOT_FOUND, "존재하지 않는 학교입니다");
        }
        return new UniversityCourseCasesResponse(universityId,
                cases.findAllByUniversityIdWithCourses(universityId).stream().map(Item::from).toList());
    }
}
