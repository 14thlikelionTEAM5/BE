package com.likelion14th._Team.domain.studyplan.entity;

import com.likelion14th._Team.domain.university.entity.University;
import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.global.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "study_plan", uniqueConstraints = @UniqueConstraint(
        name = "uk_study_plan_user_university", columnNames = {"user_id", "university_id"}))
public class StudyPlan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    @Column(nullable = false, length = 100)
    private String name;

    @OneToMany(mappedBy = "studyPlan", cascade = CascadeType.REMOVE)
    @OrderBy("id ASC")
    private List<StudyPlanItem> items = new ArrayList<>();

    @Builder
    private StudyPlan(User user, University university, String name) {
        this.user = user;
        this.university = university;
        this.name = name;
    }
}
