-- 실행 대상: 로컬 개발·시연 MySQL(team5-mysql)의 MYSQL_DATABASE. 운영 DB 실행 금지.
-- 이 파일을 실행하면 가상 사용자 최대 20명과 가상 스펙 최대 20건이 삽입된다.
-- 기존 사례와 함께 집계되므로 운영/실사용 통계용 DB에는 적용하지 않는다.
-- 2027-1 합격·탈락 스펙 전용 시연 데이터 (10개교 합격 10건 + 탈락 10건).
-- 모든 성적·결과는 가상 값이며 실제 합격 기준이나 귀국보고서에서 추출한 값이 아니다.
-- 개발·시연 DB에서만 수동 실행. 기존 대학 ID와 사용자 데이터는 변경하지 않는다.
-- 시연 사용자의 password는 무작위 잠금 값이며 로그인할 수 있는 공통 비밀번호를 만들지 않는다.
SET NAMES utf8mb4;
DROP TEMPORARY TABLE IF EXISTS passhufs_admission_demo;
CREATE TEMPORARY TABLE passhufs_admission_demo (
    sample_no CHAR(2) PRIMARY KEY, name VARCHAR(200), gpa DECIMAL(3,2), ielts DECIMAL(5,1)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
INSERT INTO passhufs_admission_demo VALUES
('01','University of Groningen',4.12,7.0),
('02','University of Mannheim',4.05,7.0),
('03','Texas State University',3.81,6.5),
('04','Florida International University',3.93,7.0),
('05','Ghent University',3.85,6.5),
('06','Queensland University of Technology',3.98,7.0),
('07','NHH Norwegian School of Economics',4.10,7.0),
('08','Goethe University Frankfurt am Main',3.89,6.5),
('09','Audencia Business School',3.96,7.0),
('10','EM Normandie Business School',3.75,6.5);

START TRANSACTION;
SET @admission_demo_now = CURRENT_TIMESTAMP;

INSERT INTO `user` (school_email,password,nickname,major_type,created_at,updated_at)
SELECT CONCAT('passhufs-demo',d.sample_no,'@example.hufs.ac.kr'),
       CONCAT('!',SHA2(UUID(),256)),CONCAT('demo27',d.sample_no),'PRIMARY',@admission_demo_now,@admission_demo_now
FROM passhufs_admission_demo d
JOIN university u ON u.name=d.name COLLATE utf8mb4_unicode_ci
WHERE NOT EXISTS (
    SELECT 1 FROM `user` s WHERE s.school_email=CONCAT('passhufs-demo',d.sample_no,'@example.hufs.ac.kr') COLLATE utf8mb4_unicode_ci
);

INSERT INTO admission_report (university_id,semester,result,language_test_type,language_score,gpa,
                              major_type,user_id,created_at,updated_at)
SELECT u.id,'2027-1','PASS','IELTS',d.ielts,
       d.gpa,'PRIMARY',s.id,@admission_demo_now,@admission_demo_now
FROM passhufs_admission_demo d
JOIN university u ON u.name=d.name COLLATE utf8mb4_unicode_ci
JOIN `user` s ON s.school_email=CONCAT('passhufs-demo',d.sample_no,'@example.hufs.ac.kr') COLLATE utf8mb4_unicode_ci
WHERE NOT EXISTS (
    SELECT 1 FROM admission_report a
    WHERE a.user_id=s.id AND a.university_id=u.id AND a.semester='2027-1' AND a.result='PASS'
);

INSERT INTO `user` (school_email,password,nickname,major_type,created_at,updated_at)
SELECT CONCAT('passhufs-fail',d.sample_no,'@example.hufs.ac.kr'),
       CONCAT('!',SHA2(UUID(),256)),CONCAT('fail27',d.sample_no),'PRIMARY',@admission_demo_now,@admission_demo_now
FROM passhufs_admission_demo d
JOIN university u ON u.name=d.name COLLATE utf8mb4_unicode_ci
WHERE NOT EXISTS (
    SELECT 1 FROM `user` s WHERE s.school_email=CONCAT('passhufs-fail',d.sample_no,'@example.hufs.ac.kr') COLLATE utf8mb4_unicode_ci
);

INSERT INTO admission_report (university_id,semester,result,language_test_type,language_score,gpa,
                              major_type,user_id,created_at,updated_at)
SELECT u.id,'2027-1','FAIL','IELTS',6.5,
       d.gpa-0.20,'PRIMARY',s.id,@admission_demo_now,@admission_demo_now
FROM passhufs_admission_demo d
JOIN university u ON u.name=d.name COLLATE utf8mb4_unicode_ci
JOIN `user` s ON s.school_email=CONCAT('passhufs-fail',d.sample_no,'@example.hufs.ac.kr') COLLATE utf8mb4_unicode_ci
WHERE NOT EXISTS (
    SELECT 1 FROM admission_report a
    WHERE a.user_id=s.id AND a.university_id=u.id AND a.semester='2027-1' AND a.result='FAIL'
);

COMMIT;
DROP TEMPORARY TABLE passhufs_admission_demo;
