-- PassHUFS 2027-1 시연 전용. 대상 DB를 선택한 후 MySQL 8에서 실행한다.
-- 기존 앱 테이블은 Hibernate 또는 프로젝트 스키마로 먼저 생성한다.
-- 아래 네 테이블은 PassHUFS.sql 설계에 맞춰 앱이 아직 생성하지 않는 경우에만 만든다.
-- 대학 메타데이터는 첨부 78개교 조사표의 국가·인증·학점 체계 열을 사용한다.
-- 성적, 보고서, 과목과 인정 사례는 모두 가상의 시연 데이터다.
-- 로컬 시연 계정 passhufs-demo01@example.hufs.ac.kr ~ passhufs-demo10@example.hufs.ac.kr
-- 시연 계정 자격 증명은 팀 비공개 채널에서 관리한다.
-- 이 파일은 개발·시연 DB에서만 수동 실행하며 배포 DB에는 실행하지 않는다.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS return_report (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT NOT NULL,
    university_id BIGINT NOT NULL,
    semester VARCHAR(10) NOT NULL,
    cost_flight VARCHAR(200),
    cost_insurance VARCHAR(200),
    cost_living_monthly VARCHAR(200),
    cost_total VARCHAR(200),
    housing_type VARCHAR(20),
    visa_duration VARCHAR(100),
    visa_type VARCHAR(100),
    insurance_provider VARCHAR(200),
    review_summary TEXT,
    UNIQUE (user_id, semester),
    FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE,
    FOREIGN KEY (university_id) REFERENCES university (id)
);

CREATE TABLE IF NOT EXISTS return_report_course (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    return_report_id BIGINT NOT NULL,
    course_code VARCHAR(50),
    course_name_en VARCHAR(200) NOT NULL,
    foreign_credit DECIMAL(3,1) NOT NULL,
    satisfaction INT,
    FOREIGN KEY (return_report_id) REFERENCES return_report (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS course_case (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    return_report_course_id BIGINT NOT NULL UNIQUE,
    home_course_id BIGINT,
    recognition_type VARCHAR(20) NOT NULL,
    recognized_credit DECIMAL(3,1) NOT NULL,
    major_type VARCHAR(20) NOT NULL,
    semester VARCHAR(10) NOT NULL,
    FOREIGN KEY (return_report_course_id) REFERENCES return_report_course (id) ON DELETE CASCADE,
    FOREIGN KEY (home_course_id) REFERENCES home_course (id)
);

CREATE TABLE IF NOT EXISTS admission_report (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    university_id BIGINT NOT NULL,
    semester VARCHAR(10) NOT NULL,
    result VARCHAR(10) NOT NULL,
    language_test_type VARCHAR(20),
    language_score DECIMAL(5,1),
    gpa DECIMAL(3,2) NOT NULL,
    major_type VARCHAR(20) NOT NULL,
    user_id BIGINT NOT NULL,
    UNIQUE (user_id, university_id, semester, result),
    FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE,
    FOREIGN KEY (university_id) REFERENCES university (id)
);

DROP TEMPORARY TABLE IF EXISTS passhufs_demo_2027_1;
CREATE TEMPORARY TABLE passhufs_demo_2027_1 (
    no CHAR(2) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    country VARCHAR(100) NOT NULL,
    aacsb BOOLEAN,
    equis BOOLEAN,
    credit_system VARCHAR(20) NOT NULL,
    quota INT,
    business_only BOOLEAN NOT NULL,
    is_open BOOLEAN NOT NULL,
    course_name VARCHAR(200) NOT NULL,
    foreign_credit DECIMAL(3,1) NOT NULL,
    gpa DECIMAL(3,2) NOT NULL,
    ielts DECIMAL(5,1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO passhufs_demo_2027_1 VALUES
    ('01','University of Groningen','네덜란드',1,1,'ECTS',2,0,1,'International Business',8,4.12,7.0),
    ('02','University of Mannheim','독일',1,1,'ECTS',2,0,1,'Business Strategy',5,4.05,7.0),
    ('03','Texas State University','미국',1,NULL,'US_STYLE',3,0,1,'Marketing Management',3,3.81,6.5),
    ('04','Florida International University','미국',1,NULL,'US_STYLE',NULL,0,1,'Financial Management',3,3.93,7.0),
    ('05','Ghent University','벨기에',1,NULL,'ECTS',2,0,1,'Operations Management',5,3.85,6.5),
    ('06','Queensland University of Technology','호주',1,1,'OWN',1,0,1,'Business Analytics',12,3.98,7.0),
    ('07','NHH Norwegian School of Economics','노르웨이',1,1,'ECTS',NULL,1,0,'Managerial Economics',5,4.10,7.0),
    ('08','Goethe University Frankfurt am Main','독일',1,NULL,'ECTS',3,1,1,'International Finance',5,3.89,6.5),
    ('09','Audencia Business School','프랑스',1,1,'ECTS',3,1,1,'Sustainable Management',5,3.96,7.0),
    ('10','EM Normandie Business School','프랑스',1,1,'ECTS',5,1,1,'Global Supply Chain Management',5,3.75,6.5);

START TRANSACTION;
SET @demo_now = CURRENT_TIMESTAMP;

INSERT INTO university (name,country,is_aacsb,is_equis,credit_system,created_at,updated_at)
SELECT d.name,d.country,d.aacsb,d.equis,d.credit_system,@demo_now,@demo_now
FROM passhufs_demo_2027_1 d
WHERE NOT EXISTS (SELECT 1 FROM university u WHERE u.name=d.name COLLATE utf8mb4_unicode_ci);

UPDATE university u JOIN passhufs_demo_2027_1 d ON d.name COLLATE utf8mb4_unicode_ci=u.name
SET u.country=d.country, u.is_aacsb=d.aacsb, u.is_equis=COALESCE(d.equis,u.is_equis),
    u.credit_system=d.credit_system, u.updated_at=@demo_now
WHERE NOT (u.country <=> d.country COLLATE utf8mb4_unicode_ci AND u.is_aacsb <=> d.aacsb
           AND u.is_equis <=> COALESCE(d.equis,u.is_equis) AND u.credit_system <=> d.credit_system COLLATE utf8mb4_unicode_ci);

-- 2026-07-03 공지용 명단의 기준일. 모집 여부와 시연용 가상 합격 데이터는 별개다.
INSERT INTO agreement (university_id,semester,quota,exchange_period,is_business_only,is_open,
                       announced_at,imported_at,created_at,updated_at)
SELECT u.id,'2027-1',d.quota,'1학기',d.business_only,d.is_open,'2026-07-03',@demo_now,@demo_now,@demo_now
FROM passhufs_demo_2027_1 d JOIN university u ON u.name=d.name COLLATE utf8mb4_unicode_ci
WHERE NOT EXISTS (
    SELECT 1 FROM agreement a WHERE a.university_id=u.id AND a.semester='2027-1'
);

INSERT INTO credit_policy (department_name,effective_from,max_credit_per_course,max_credit_per_semester,
                           ects_rate,allow_major_required,allow_major_required_minor,only_business_courses,
                           standard_date,created_at,updated_at)
SELECT '경영대학','2024-03-21',3,18,0.60,0,1,1,'2024-03-21',@demo_now,@demo_now
WHERE NOT EXISTS (
    SELECT 1 FROM credit_policy WHERE department_name='경영대학' AND effective_from='2024-03-21'
);

-- 기존 시연 DB에 같은 시행일의 테스트 정책이 있으면 시연 기준을 적용한다.
UPDATE credit_policy
SET ects_rate=0.60, max_credit_per_course=3, max_credit_per_semester=18, updated_at=@demo_now
WHERE department_name='경영대학' AND effective_from='2024-03-21'
  AND (ects_rate<>0.60 OR max_credit_per_course<>3 OR max_credit_per_semester<>18);

INSERT INTO home_course (code,name,credit,category,is_business_offered,base_year,created_at,updated_at)
SELECT 'DEMO-BIZ-2027','[시연] 국제경영 주제연구',3,'MAJOR_ELECTIVE',1,'2026-2027',@demo_now,@demo_now
WHERE NOT EXISTS (SELECT 1 FROM home_course WHERE code='DEMO-BIZ-2027');

INSERT INTO `user` (school_email,password,nickname,major_type,created_at,updated_at)
SELECT CONCAT('passhufs-demo',d.no,'@example.hufs.ac.kr'),
       '$2b$12$4t7at76yue0DOtICsHHR7.ElrJVMXl0ynu5v6zWGoTzZ/FXjI/BNC',
       CONCAT('demo27',d.no),'PRIMARY',@demo_now,@demo_now
FROM passhufs_demo_2027_1 d
WHERE NOT EXISTS (
    SELECT 1 FROM `user` s WHERE s.school_email=CONCAT('passhufs-demo',d.no,'@example.hufs.ac.kr') COLLATE utf8mb4_unicode_ci
);

INSERT INTO return_report (user_id,university_id,semester,housing_type,review_summary,created_at,updated_at)
SELECT s.id,u.id,'2027-1','DORM',
       '[시연] 수강 신청 전에 강의계획서와 과목 개설 학기를 확인했습니다.',@demo_now,@demo_now
FROM passhufs_demo_2027_1 d
JOIN university u ON u.name=d.name COLLATE utf8mb4_unicode_ci
JOIN `user` s ON s.school_email=CONCAT('passhufs-demo',d.no,'@example.hufs.ac.kr') COLLATE utf8mb4_unicode_ci
WHERE NOT EXISTS (
    SELECT 1 FROM return_report r WHERE r.user_id=s.id AND r.semester='2027-1'
);

INSERT INTO return_report_course (return_report_id,course_code,course_name_en,foreign_credit,
                                  satisfaction,created_at,updated_at)
SELECT r.id,CONCAT('DEMO-2027-',d.no),d.course_name,d.foreign_credit,4,@demo_now,@demo_now
FROM passhufs_demo_2027_1 d
JOIN university u ON u.name=d.name COLLATE utf8mb4_unicode_ci
JOIN `user` s ON s.school_email=CONCAT('passhufs-demo',d.no,'@example.hufs.ac.kr') COLLATE utf8mb4_unicode_ci
JOIN return_report r ON r.user_id=s.id AND r.university_id=u.id AND r.semester='2027-1'
WHERE NOT EXISTS (
    SELECT 1 FROM return_report_course c WHERE c.return_report_id=r.id AND c.course_code=CONCAT('DEMO-2027-',d.no) COLLATE utf8mb4_unicode_ci
);

INSERT INTO course_case (return_report_course_id,home_course_id,recognition_type,recognized_credit,
                         major_type,semester,created_at,updated_at)
SELECT c.id,h.id,'MAJOR_ELECTIVE',3.0,'PRIMARY','2027-1',@demo_now,@demo_now
FROM passhufs_demo_2027_1 d
JOIN university u ON u.name=d.name COLLATE utf8mb4_unicode_ci
JOIN `user` s ON s.school_email=CONCAT('passhufs-demo',d.no,'@example.hufs.ac.kr') COLLATE utf8mb4_unicode_ci
JOIN return_report r ON r.user_id=s.id AND r.university_id=u.id AND r.semester='2027-1'
JOIN return_report_course c ON c.return_report_id=r.id AND c.course_code=CONCAT('DEMO-2027-',d.no) COLLATE utf8mb4_unicode_ci
JOIN home_course h ON h.code='DEMO-BIZ-2027'
WHERE NOT EXISTS (SELECT 1 FROM course_case k WHERE k.return_report_course_id=c.id);

INSERT INTO admission_report (university_id,semester,result,language_test_type,language_score,gpa,
                              major_type,user_id,created_at,updated_at)
SELECT u.id,'2027-1','PASS','IELTS',d.ielts,d.gpa,'PRIMARY',s.id,@demo_now,@demo_now
FROM passhufs_demo_2027_1 d
JOIN university u ON u.name=d.name COLLATE utf8mb4_unicode_ci
JOIN `user` s ON s.school_email=CONCAT('passhufs-demo',d.no,'@example.hufs.ac.kr') COLLATE utf8mb4_unicode_ci
WHERE NOT EXISTS (
    SELECT 1 FROM admission_report a
    WHERE a.user_id=s.id AND a.university_id=u.id AND a.semester='2027-1' AND a.result='PASS'
);

INSERT INTO study_plan (user_id,university_id,name,created_at,updated_at)
SELECT s.id,u.id,'[시연] 2027-1 교환학생 수강계획',@demo_now,@demo_now
FROM passhufs_demo_2027_1 d
JOIN university u ON u.name=d.name COLLATE utf8mb4_unicode_ci
JOIN `user` s ON s.school_email=CONCAT('passhufs-demo',d.no,'@example.hufs.ac.kr') COLLATE utf8mb4_unicode_ci
WHERE NOT EXISTS (SELECT 1 FROM study_plan p WHERE p.user_id=s.id AND p.university_id=u.id);

INSERT INTO study_plan_item (study_plan_id,item_type,course_name,foreign_credit,source_course_id,
                             home_course_id,recognition_type,created_at,updated_at)
SELECT p.id,'REGISTERED',c.course_name_en,c.foreign_credit,c.id,h.id,'MAJOR_ELECTIVE',@demo_now,@demo_now
FROM passhufs_demo_2027_1 d
JOIN university u ON u.name=d.name COLLATE utf8mb4_unicode_ci
JOIN `user` s ON s.school_email=CONCAT('passhufs-demo',d.no,'@example.hufs.ac.kr') COLLATE utf8mb4_unicode_ci
JOIN return_report r ON r.user_id=s.id AND r.university_id=u.id AND r.semester='2027-1'
JOIN return_report_course c ON c.return_report_id=r.id AND c.course_code=CONCAT('DEMO-2027-',d.no) COLLATE utf8mb4_unicode_ci
JOIN study_plan p ON p.user_id=s.id AND p.university_id=u.id
JOIN home_course h ON h.code='DEMO-BIZ-2027'
WHERE NOT EXISTS (
    SELECT 1 FROM study_plan_item i WHERE i.study_plan_id=p.id AND i.source_course_id=c.id
);

-- Groningen에서 8 ECTS 과목 네 개로 4.8 × 4 = 19.2학점 경고를 시연한다.
INSERT INTO study_plan_item (study_plan_id,item_type,course_name,foreign_credit,recognition_type,created_at,updated_at)
SELECT p.id,'CUSTOM',extra.name,8.0,'GENERAL',@demo_now,@demo_now
FROM study_plan p
JOIN `user` s ON s.id=p.user_id AND s.school_email='passhufs-demo01@example.hufs.ac.kr'
JOIN university u ON u.id=p.university_id AND u.name='University of Groningen'
CROSS JOIN (
    SELECT '[시연] Intercultural Communication' AS name
    UNION ALL SELECT '[시연] European Society'
    UNION ALL SELECT '[시연] Global Studies'
) extra
WHERE NOT EXISTS (
    SELECT 1 FROM study_plan_item i WHERE i.study_plan_id=p.id AND i.item_type='CUSTOM' AND i.course_name=extra.name COLLATE utf8mb4_unicode_ci
);

COMMIT;

SELECT u.id AS university_id,u.name,u.country,u.credit_system,s.school_email,p.id AS study_plan_id,
       CASE
           WHEN (u.is_aacsb=1 OR u.is_equis=1) AND u.credit_system='ECTS' THEN 'A'
           WHEN (u.is_aacsb=1 OR u.is_equis=1) AND u.credit_system='US_STYLE' THEN 'B'
           ELSE 'C'
       END AS demo_rule
FROM passhufs_demo_2027_1 d
JOIN university u ON u.name=d.name COLLATE utf8mb4_unicode_ci
JOIN `user` s ON s.school_email=CONCAT('passhufs-demo',d.no,'@example.hufs.ac.kr') COLLATE utf8mb4_unicode_ci
JOIN study_plan p ON p.user_id=s.id AND p.university_id=u.id
ORDER BY d.no;

DROP TEMPORARY TABLE passhufs_demo_2027_1;
