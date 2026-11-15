package com.likelion14th._Team.domain.university.service;

import com.likelion14th._Team.domain.university.dto.UniversityCoursesResponse;
import com.likelion14th._Team.domain.university.repository.UniversityCourseRepository;
import com.likelion14th._Team.domain.university.repository.UniversityRepository;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
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
        var courses = repository.courses(universityId).stream()
                .map(row -> new UniversityCoursesResponse.Course(row.courseId(), row.courseCode(),
                        row.courseName(), row.foreignCredit(), row.hasRecognition()))
                .toList();
        return new UniversityCoursesResponse(universityId, courses);
    }
}
