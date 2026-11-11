# 협정교 조회 명세 보완 (#25, PR #26)

## 목록·상세 지원 가능 판정

2026-11-11 사용자 확인으로 목록·상세의 판정을 다음과 같이 통일한다.

| 조건 | status | reasonCodes | message |
| --- | --- | --- | --- |
| 선택한 학기 공고 없음 | NOT_ELIGIBLE | NO_CURRENT_AGREEMENT | 이번 학기 모집 없음 |
| 공고의 isOpen=false | NOT_ELIGIBLE | RECRUITMENT_STOPPED | 이번 학기 모집 없음 |
| 공고는 있으나 GPA·어학 등 필수 조건 확인 불가 | NEEDS_CHECK | 해당 미확인 사유 | 지원 조건을 확인해 주세요. |
| 확인된 GPA 또는 어학 조건 미달 | NOT_ELIGIBLE | GPA_BELOW_MIN / LANGUAGE_BELOW_MIN | 해당 미달 문구 |
| 확인 가능한 필수 조건 충족 | ELIGIBLE | 빈 배열 | null |

공고가 없을 때 상세의 기존 NEEDS_CHECK 규칙은 위 표로 대체한다.
상세 조회에서 semester를 생략하면 해당 학교의 최신 학기이므로, 목록과 같은 학기를 보려면 목록의 semester를 전달한다.

## 학기 변경 배지

- 직전 학기 데이터가 없으면 변경을 추정하지 않고 빈 배열을 반환한다.
- 현재 공고가 있고 직전 학기에는 해당 학교의 공고가 없으면 NEW다.
- 현재·직전 공고의 quota가 다르면 QUOTA_CHANGED다.
- 직전 공고는 isOpen=true이고 현재 공고는 false일 때만 RECRUITMENT_STOPPED다.
- 현재 공고가 없다는 사실만으로 모집 중단 배지를 붙이지 않는다.
- 따라서 기존 목록 예시의 '공고 없는 학교'는 semesterChanges=[]가 맞다. 표시 문구는 eligibility.message를 사용한다.

## 추가 확인 사항

- PRIMARY/DOUBLE/MINOR는 사용자 전공 구분이다. 사용자 확인으로 전공별 자격 제한은 적용하지 않는다. 수강 제한 원문도 자동 판정에 쓰지 않는다.
- 같은 학교의 계획은 사용자당 하나지만, 대표 studyPlanId는 여러 건이 존재할 경우 가장 작은 ID로 결정한다. 학기 조건은 적용하지 않는다.
- hasExperienceData는 ReturnReport 또는 AdmissionReport가 존재하면 true다. 현재 ERD의 course_case는 return_report_course를 필수 참조하고, 이 과목은 return_report를 필수 참조하므로 인정 사례가 있으면 귀국보고서도 존재한다. 독립 인정 사례를 허용하도록 ERD가 바뀌면 조회도 확장해야 한다.
- 국가 조회는 university 전체를 집계한다. DB 국가 정렬은 실제 한글 데이터로 검증한다.

## 협정 공고 시드 재실행 정책

agreement_2027_1.sql은 기존 학교 ID를 사용하고 없는 공고·어학 기준만 삽입한다. 기존 GPA·인원·비용·어학 기준은 수동 수정값을 보존한다. 학교 강의목록 URL은 NULL일 때만 채운다. 원문 변경 반영은 별도로 검토한 수정 SQL로 진행한다.

## 후속 개선

목록 검색 조건의 DB 이관, 프로필·점수·찜 조회 결합, 인증 후 사용자 재조회 제거, 컨트롤러 예외 처리 통합, 엔티티 enum 변환은 후속 리팩터링 대상으로 남긴다. 기존 응답 동작은 유지한다.
