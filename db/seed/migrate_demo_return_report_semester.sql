-- PassHUFS 시연 귀국보고서 학기 이관(2027-1 → 2026-1). 대상 DB를 선택한 후 MySQL 8에서 실행한다.
-- 수정 전 demo_2027_1.sql로 이미 시드한 DB에만 사용한다.
-- 귀국보고서는 수학을 마친 학기에만 존재하므로 시연 귀국보고서와 인정 사례를 2026-1로 옮긴다.
-- 대상은 passhufs-demo%@example.hufs.ac.kr 시연 계정의 데이터뿐이다. 실보고서와 다른 계정은 바꾸지 않는다.
-- admission_report와 agreement의 2027-1은 지원·협정 학기이므로 UPDATE하지 않는다.
-- 이 스크립트를 먼저 실행한 뒤 수정된 demo_2027_1.sql을 실행한다. 순서가 반대면 보고서가 중복 생성된다.
-- 이 파일은 개발·시연 DB에서만 수동 실행하며 배포 DB에는 실행하지 않는다.

SET NAMES utf8mb4;

-- 사전 확인. return_report에는 (user_id, semester) 유니크 제약이 있다.
-- 결과가 한 건이라도 나오면 시연 계정에 이미 2026-1 보고서가 있으므로 아래 UPDATE를 실행하지 않고 원인을 먼저 확인한다.
SELECT s.school_email,r.id AS return_report_id,r.university_id,r.semester
FROM return_report r JOIN `user` s ON s.id=r.user_id
WHERE s.school_email LIKE 'passhufs-demo%@example.hufs.ac.kr' AND r.semester='2026-1';

START TRANSACTION;
SET @migrate_now = CURRENT_TIMESTAMP;

-- 시연 계정 귀국보고서에 딸린 인정 사례의 학기를 옮긴다.
UPDATE course_case k
JOIN return_report_course c ON c.id=k.return_report_course_id
JOIN return_report r ON r.id=c.return_report_id
JOIN `user` s ON s.id=r.user_id
SET k.semester='2026-1', k.updated_at=@migrate_now
WHERE s.school_email LIKE 'passhufs-demo%@example.hufs.ac.kr'
  AND r.semester='2027-1' AND k.semester='2027-1';

-- 시연 계정 귀국보고서의 학기를 옮긴다.
UPDATE return_report r
JOIN `user` s ON s.id=r.user_id
SET r.semester='2026-1', r.updated_at=@migrate_now
WHERE s.school_email LIKE 'passhufs-demo%@example.hufs.ac.kr' AND r.semester='2027-1';

COMMIT;

-- 이관 결과 확인. 시연 계정의 귀국보고서와 인정 사례에 2027-1이 남지 않아야 한다.
SELECT 'return_report' AS target,r.semester,COUNT(*) AS row_count
FROM return_report r JOIN `user` s ON s.id=r.user_id
WHERE s.school_email LIKE 'passhufs-demo%@example.hufs.ac.kr'
GROUP BY r.semester
UNION ALL
SELECT 'course_case',k.semester,COUNT(*)
FROM course_case k
JOIN return_report_course c ON c.id=k.return_report_course_id
JOIN return_report r ON r.id=c.return_report_id
JOIN `user` s ON s.id=r.user_id
WHERE s.school_email LIKE 'passhufs-demo%@example.hufs.ac.kr'
GROUP BY k.semester;
