-- #96: 기존 user_language_score의 총점은 보존하고 영역별 점수 네 컬럼을 추가한다.
-- 대상: 사용할 DB를 선택한 MySQL 8.x 세션. 앱 배포 전 실행 가능하며 재실행해도 기존 값을 덮어쓰지 않는다.
-- 기존 성적은 네 컬럼 모두 NULL: 영역별 기준이 있는 학교는 확인 필요로 유지한다.
-- API: languageScores[].sectionScores = {reading, listening, speaking, writing}
-- IELTS 0~9(0.5 단위), TOEFL_IBT 0~30(정수), TOEFL_NEW 1~6(0.5 단위).
-- sectionScores 생략 허용, 객체를 보내면 네 영역 모두 필수. TOEFL_ITP는 이 객체를 받지 않는다.
-- 학교 응답 languageRequirements[]에 minSectionScore, conditionNote를 함께 반환한다.
-- ERD: user_language_score에 reading_score/listening_score/speaking_score/writing_score DECIMAL(3,1) NULL 추가.
-- 지원 판정: LANGUAGE_SECTION_SCORE_NOT_PROVIDED(확인 필요), LANGUAGE_SECTION_BELOW_MIN(지원 불가).
-- 별도 추가 조건은 LANGUAGE_DETAIL_NEEDS_CHECK 유지. 한 시험으로 모든 조건을 충족하면 지원 가능.
-- 이 스크립트는 수동 배포용이다. 현재 프로젝트에 자동 마이그레이션 도구는 적용되어 있지 않다.

SET @section_column_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'user_language_score' AND column_name = 'reading_score');
SET @section_ddl = IF(@section_column_exists = 0,
    'ALTER TABLE user_language_score ADD COLUMN reading_score DECIMAL(3,1) NULL', 'SELECT 1');
PREPARE section_migration FROM @section_ddl;
EXECUTE section_migration;
DEALLOCATE PREPARE section_migration;

SET @section_column_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'user_language_score' AND column_name = 'listening_score');
SET @section_ddl = IF(@section_column_exists = 0,
    'ALTER TABLE user_language_score ADD COLUMN listening_score DECIMAL(3,1) NULL', 'SELECT 1');
PREPARE section_migration FROM @section_ddl;
EXECUTE section_migration;
DEALLOCATE PREPARE section_migration;

SET @section_column_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'user_language_score' AND column_name = 'speaking_score');
SET @section_ddl = IF(@section_column_exists = 0,
    'ALTER TABLE user_language_score ADD COLUMN speaking_score DECIMAL(3,1) NULL', 'SELECT 1');
PREPARE section_migration FROM @section_ddl;
EXECUTE section_migration;
DEALLOCATE PREPARE section_migration;

SET @section_column_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'user_language_score' AND column_name = 'writing_score');
SET @section_ddl = IF(@section_column_exists = 0,
    'ALTER TABLE user_language_score ADD COLUMN writing_score DECIMAL(3,1) NULL', 'SELECT 1');
PREPARE section_migration FROM @section_ddl;
EXECUTE section_migration;
DEALLOCATE PREPARE section_migration;

