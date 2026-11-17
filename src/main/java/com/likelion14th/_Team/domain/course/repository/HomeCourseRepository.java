package com.likelion14th._Team.domain.course.repository;

import com.likelion14th._Team.domain.course.entity.HomeCourse;
import com.likelion14th._Team.global.enums.CourseCategory;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HomeCourseRepository extends JpaRepository<HomeCourse, Long> {

    /** 경영대학 개설 과목 중 구분이 같고 과목명에 검색어를 포함하는 과목. %, _ 는 Spring Data가 이스케이프한다 */
    List<HomeCourse> findByCategoryAndIsBusinessOfferedTrueAndNameContainingOrderByNameAsc(
            CourseCategory category, String keyword, Pageable pageable);
}
