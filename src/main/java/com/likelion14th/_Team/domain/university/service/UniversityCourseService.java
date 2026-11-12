package com.likelion14th._Team.domain.university.service;

import com.likelion14th._Team.domain.university.dto.UniversityCoursesResponse;
import com.likelion14th._Team.domain.university.repository.*;
import com.likelion14th._Team.global.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UniversityCourseService {
    private final UniversityRepository universities;
    private final UniversityCourseRepository repository;
    public UniversityCoursesResponse courses(Long userId, long universityId) {
        if (userId == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        if (!universities.existsById(universityId)) {
            throw new CustomException(ErrorCode.UNIVERSITY_NOT_FOUND, "존재하지 않는 학교입니다");
        }
        return new UniversityCoursesResponse(universityId, repository.courses(universityId));
    }
}
