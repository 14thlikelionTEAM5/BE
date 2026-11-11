-- PassHUFS 2027-1 시연 전용. 대상 DB를 선택한 후 MySQL 8에서 실행한다.
-- 기존 앱 테이블은 Hibernate 또는 프로젝트 스키마로 먼저 생성한다.
-- 아래 네 테이블은 PassHUFS.sql 설계에 맞춰 앱이 아직 생성하지 않는 경우에만 만든다.
-- 대학 메타데이터는 첨부 78개교 조사표의 국가·인증·학점 체계 열을 사용한다.
-- 성적, 보고서, 과목과 인정 사례는 모두 가상의 시연 데이터다.
-- 2026-1 Texas State 실보고서와 2027-1 시연 데이터는 구분한다.
-- 대상 10개교가 모두 경영 과목을 수강할 수 있다는 의미는 아니다. 공식 수강 제한을 확인한다.
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


-- 공식 모집 엑셀 sheet1의 상세 조건. sheet2 요약본과 다른 경우 상세 원문을 사용한다.
-- FIU의 2~3명처럼 정수 한 칸으로 표현하지 못하는 선발인원은 NULL과 원문을 함께 보존한다.
DROP TEMPORARY TABLE IF EXISTS passhufs_demo_details;
CREATE TEMPORARY TABLE passhufs_demo_details (
    name VARCHAR(200) PRIMARY KEY, min_gpa DECIMAL(3,2), quota INT, is_open BOOLEAN,
    cost_summary TEXT, benefit_summary TEXT, course_restriction TEXT, remark_summary TEXT,
    course_list_url VARCHAR(500)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
INSERT INTO passhufs_demo_details VALUES
('University of Groningen',3.0,2,1,'• 숙소: 450 ~ 650/월\n• 식비: 200 ~ 300/월\n• 보험: 100 ~ 120/월\n','•  외국대학 등록금 면제','• 지원가능 학부: Faculty of Arts, Faculty of Philosophy, Faculty of Religious Studies, University College Fryslan, University College Groningen \n• 본인이 지원한 학부 내에서만 수강 가능','선발인원 원문 2\n• 각종안내: https://www.rug.nl/education/exchange/\n• 숙소(기숙사 제공하지 않음): https://www.rug.nl/education/bachelor/international-students/study-in-the-netherlands-groningen/accommodation\n• 비용: https://www.rug.nl/education/bachelor/international-students/financial-matters/living-expenses\n• Resident Permit 진행시 1학기 생활비(약 5,500유로)를 RUG에 미리 송금해야 함',NULL),
('University of Mannheim',3.0,2,1,'€ 970 ~ 1,000/월\n(거주비 포함/\n출처: Factsheet)\n• 숙소: 400~/월\n• 보험: 125/월\n• 교통비: 190~/학기','•  외국대학 등록금 면제','• 지원가능 학부: School of Humanities \n• School of Humanities 내에서 50% 이상의 수업을 수강하는 경우, University Wide Electives에서 한 두과목 정도 추가 수강 가능 (기타 학부는 교수의 허가가 있을 경우에만 수강 가능)','선발인원 원문 2\n• 각종안내: https://www.uni-mannheim.de/en/academics/coming-to-mannheim/exchange-students/\n• 강의목록: https://www.uni-mannheim.de/en/academics/coming-to-mannheim/exchange-students/courses/course-catalog/\n• 숙소(가을학기 파견학생의 경우 특히 기숙사 배정 어려움, 외부숙소): https://www.uni-mannheim.de/en/academics/coming-to-mannheim/exchange-students/housing/','https://www.uni-mannheim.de/en/academics/coming-to-mannheim/exchange-students/courses/course-catalog/'),
('Texas State University',3.4,3,1,'$10,200~/학기\n(출처-Factsheet)','• 외국대학 등록금 면제','• Mass Communication, Business Administration, Marketing, Forensics 전공 인기\n• 기관토플 제출 시 어학성적 자격요건을 충족했더라도 영어권 교환선발 면접에서 영어실력이 부족하다고 판단될 시에는 선발 불가','선발인원 원문 3\n• 각종안내: https://www.educationabroad.txst.edu/\n• 강의목록: https://mycatalog.txstate.edu/courses/\n• 숙소(기숙사 신청): https://www.reslife.txst.edu/\n','https://mycatalog.txstate.edu/courses/'),
('Florida International University',3.38,NULL,1,'US$ 10,662 ~/학기\n(출처-Factsheet)\n• 기숙사: 4,222~ /학기\n• 밀플랜: 2,028~ /학기\n• 교재비 외: 675~ /학기\n• 보험: 1,212~ /학기\n• 기타: 2,500~ /학기','• 외국대학 등록금 면제','• 수강불가: College of Medicine, College of Law\n• 수강제한: Finance, Accounting, Communication (Advertising, Journalism, Public Relations), Nursing, Nutrition and Dietetics','선발인원 원문 2~3\n• 각종안내(강의목록 포함): https://abroad.fiu.edu/index.cfm?FuseAction=Programs.ViewProgramAngular&id=10203\n• 숙소(기숙사 선착순 배정): https://housing.fiu.edu/\n• Exchange/Education Abroad Office: https://studyabroad.fiu.edu/','https://abroad.fiu.edu/index.cfm?FuseAction=Programs.ViewProgramAngular&id=10203'),
('Ghent University',3.0,2,1,'• 교재: 500~/년\n(출처-Factsheet)','• 외국대학 등록금 면제','• 지원가능 학부: Faculty of Arts and Philosophy (Course code A로 시작)\n• 본인 소속학부에서 최소 50% 이상 수강해야함\n','선발인원 원문 2\n• 강의목록: https://studiekiezer.ugent.be/en/zoek\n  - Exchange Programme으로 조회 \n  - 교과목 정보에서 initial competences를 반드시 확인해야함\n• 숙소(기숙사 선착순 배정): https://www.ugent.be/en/facilities/housing/exchange.htm\n• 비용: https://www.ugent.be/en/facilities','https://studiekiezer.ugent.be/en/zoek'),
('Queensland University of Technology',3.0,1,1,'A$1,660~3,545/월','• 외국대학 등록금 면제','• 수강불가: Health disciplines – Podiatry, Paramedic Science, Optometry and Vision Science, Medical Imaging, Radiation Therapy, Pharmacy and PG Psychology\n• Term 1: 2월 - 7월, Term2: 7월 - 11월\n','선발인원 원문 1\n• 개설전공: https://www.qut.edu.au/study/applying/study-abroad-and-exchange/what-can-i-study\n• 각종안내: https://www.qut.edu.au/study/applying/study-abroad-and-exchange/exchange\n• 숙소: https://www.qut.edu.au/study/student-life/living-in-brisbane/accommodation-in-brisbane\n• 비용: https://www.qut.edu.au/study/international/study-abroad-program/costs',NULL),
('NHH Norwegian School of Economics',3.0,NULL,0,'NOK11,000~/월','•  외국대학 등록금 면제','• 학부과정 수업만 선택 가능하며 석사과정 수강 희망시 개별 승인 필요','선발인원 원문 -\n• 각종안내(숙소포함) : https://www.nhh.no/en/for-students/international-opportunities/exchange/incoming-exchange/\n• 강의목록: https://www.nhh.no/en/for-students/courses2/bachelor-courses-exchange/\n• 비용: https://www.studybergen.com/why-study-in-bergen/studying-in-norway/cost-of-living/','https://www.nhh.no/en/for-students/courses2/bachelor-courses-exchange/'),
('Goethe University Frankfurt am Main',3.0,3,1,'• 생활비(숙소,식비 등): €800~1,000/월\n• Semester Contribution: €360/학기\n(출처-Factsheet)','•  외국대학 등록금 면제','• Faculty of Economics and Business 학부수업만 선택 가능','선발인원 원문 3\n• 각종안내(강의목록 포함): https://www.goethe-university-frankfurt.de/122913225/Before_your_Mobility_to_Frankfurt\n• 숙소(기숙사 선착순 배정 또는 외부숙소)\n• 비용: https://www.goethe-university-frankfurt.de/122919146/During_your_stay_in_Frankfurt#Finances','https://www.goethe-university-frankfurt.de/122913225/Before_your_Mobility_to_Frankfurt'),
('Audencia Business School',3.0,3,1,'https://www.audencia.com/en/student-experience/everyday-life/international-students','•  외국대학 등록금 면제\n','• 3학년 이상 파견 가능\n• Atlantic Campus (Grande Ecole Programme)\n• 경영전공자가 이닌 경우 City Campus (Bachelor Program)에서 수강해야함\n• 2027학년도 1학기 이후 매년 가을학기에만 교환학생 선발 예정\n','선발인원 원문 3\n• 각종안내(강의목록, Factsheet 포함): https://apply.exchangestudents.audencia.com/_portal/incoming-students\n• 최초 수강신청 이후 변경 불가\n• 숙소(외부숙소 학생 스스로 찾아야함)\n• 비용: https://www.expatistan.com/cost-of-living/nantes','https://apply.exchangestudents.audencia.com/_portal/incoming-students'),
('EM Normandie Business School',3.3,5,1,'€ 800 ~ 1,300\n/월\n(Le Havre 기준\n출처-Information sheet)','•  외국대학 등록금 면제','• 지원가능 학부: International Management(BIM), Business Administraion(BBA)\n• 석사 과정은 원칙적으로 수강 불가하나 한국의 4학년에 해당하는 프랑스 그랑제꼴 과정 Master 1년 과정 수강을 희망하는 4학년 교환학생인 경우 학과와 검토 후 승인여부 결정\n• 2027학년도 1학기 이후 매년 봄학기에만 교환학생 선발 예정','선발인원 원문 5\n• 학부과정은 Le Havre 캠퍼스만 선택 가능 \n• 각종안내(강의목록, Factsheet, Welcome guide 포함): https://www.em-normandie.com/en/exchange-programmes\n• 숙소(외부숙소 학생 스스로 찾아야함) \n- Campus France website 방문하여 추가 정보 확인 필요, 주정부 운영 CROUS와 연계가능','https://www.em-normandie.com/en/exchange-programmes');

UPDATE university u JOIN passhufs_demo_details d ON u.name=d.name COLLATE utf8mb4_unicode_ci
SET u.course_list_url=d.course_list_url;

UPDATE agreement a JOIN university u ON u.id=a.university_id
JOIN passhufs_demo_details d ON u.name=d.name COLLATE utf8mb4_unicode_ci
SET a.min_gpa=d.min_gpa,a.quota=d.quota,a.is_open=d.is_open,
    a.cost_summary=d.cost_summary,a.benefit_summary=d.benefit_summary,
    a.course_restriction=d.course_restriction,a.remark_summary=d.remark_summary,
    a.source_url='https://oia.hufs.ac.kr/bbs/oia/3431/262695/artclView.do'
WHERE a.semester='2027-1';

DROP TEMPORARY TABLE IF EXISTS passhufs_demo_languages;
CREATE TEMPORARY TABLE passhufs_demo_languages (
    name VARCHAR(200), test_type VARCHAR(20), min_score DECIMAL(5,1),
    min_section_score DECIMAL(3,1), condition_note VARCHAR(100)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
INSERT INTO passhufs_demo_languages VALUES
('University of Groningen','TOEFL_IBT',90,21,'90 (영역별 21이상)'),
('University of Groningen','TOEFL_NEW',4.5,4.0,'4.5 (영역별 4.0이상)'),
('University of Groningen','IELTS',6.5,6.0,'6.5 (영역별 6.0이상)'),
('University of Mannheim','TOEFL_IBT',85,NULL,'85'),
('University of Mannheim','TOEFL_NEW',4.5,NULL,'4.5'),
('University of Mannheim','TOEFL_ITP',570,NULL,'570'),
('University of Mannheim','IELTS',6.5,NULL,'6.5'),
('Texas State University','TOEFL_IBT',80,19,'80 (영역별 19 이상)'),
('Texas State University','TOEFL_NEW',4.5,NULL,'4.5'),
('Texas State University','TOEFL_ITP',570,NULL,'570'),
('Texas State University','IELTS',6.5,6.0,'6.5 (영역별 6.0이상)'),
('Florida International University','TOEFL_IBT',80,NULL,'80'),
('Florida International University','TOEFL_NEW',4.5,NULL,'4.5'),
('Florida International University','IELTS',6.5,NULL,'6.5'),
('Ghent University','TOEFL_IBT',90,NULL,'90'),
('Ghent University','TOEFL_NEW',5,NULL,'5'),
('Ghent University','IELTS',6.5,6.0,'6.5 (영역별 6.0이상)'),
('Queensland University of Technology','IELTS',6.5,6.0,'6.5 (영역별 6.0이상)'),
('NHH Norwegian School of Economics','TOEFL_IBT',80,NULL,'80'),
('NHH Norwegian School of Economics','TOEFL_NEW',4,NULL,'4'),
('NHH Norwegian School of Economics','TOEFL_ITP',550,NULL,'550'),
('NHH Norwegian School of Economics','IELTS',6,NULL,'6'),
('Goethe University Frankfurt am Main','TOEFL_IBT',87,NULL,'87'),
('Goethe University Frankfurt am Main','TOEFL_NEW',4.5,NULL,'4.5'),
('Goethe University Frankfurt am Main','IELTS',6.5,NULL,'6.5'),
('Audencia Business School','TOEFL_IBT',85,NULL,'85'),
('Audencia Business School','TOEFL_NEW',4.5,NULL,'4.5'),
('Audencia Business School','TOEFL_ITP',570,NULL,'570'),
('Audencia Business School','IELTS',6.5,NULL,'6.5'),
('EM Normandie Business School','TOEFL_IBT',83,NULL,'83'),
('EM Normandie Business School','TOEFL_NEW',4,NULL,'4'),
('EM Normandie Business School','IELTS',6,NULL,'6');

INSERT INTO agreement_language_requirement
    (agreement_id,test_type,min_score,min_section_score,condition_note,created_at,updated_at)
SELECT a.id,d.test_type,d.min_score,d.min_section_score,d.condition_note,@demo_now,@demo_now
FROM passhufs_demo_languages d JOIN university u ON u.name=d.name COLLATE utf8mb4_unicode_ci
JOIN agreement a ON a.university_id=u.id AND a.semester='2027-1'
WHERE NOT EXISTS (
    SELECT 1 FROM agreement_language_requirement l
    WHERE l.agreement_id=a.id AND l.test_type=d.test_type COLLATE utf8mb4_unicode_ci
);

UPDATE agreement_language_requirement l JOIN agreement a ON a.id=l.agreement_id
JOIN university u ON u.id=a.university_id
JOIN passhufs_demo_languages d ON u.name=d.name COLLATE utf8mb4_unicode_ci
    AND l.test_type=d.test_type COLLATE utf8mb4_unicode_ci
SET l.min_score=d.min_score,l.min_section_score=d.min_section_score,l.condition_note=d.condition_note
WHERE a.semester='2027-1';

DROP TEMPORARY TABLE passhufs_demo_languages;
DROP TEMPORARY TABLE passhufs_demo_details;

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


-- 실제 보고서 원문을 받기 전까지 사용하는 가상 비용·후기. 모든 금액은 시연값이다.
UPDATE return_report r JOIN `user` s ON s.id=r.user_id
JOIN passhufs_demo_2027_1 d ON s.school_email=CONCAT('passhufs-demo',d.no,'@example.hufs.ac.kr') COLLATE utf8mb4_unicode_ci
JOIN university u ON u.id=r.university_id AND u.name=d.name COLLATE utf8mb4_unicode_ci
SET r.cost_flight='[가상 시연] 왕복 약 180만원',
    r.cost_insurance='[가상 시연] 약 40만원',
    r.cost_living_monthly='[가상 시연] 월 약 120만원',
    r.cost_total='[가상 시연] 한 학기 약 1000만원',
    r.visa_duration='[가상 시연] 약 4주',
    r.visa_type='[가상 시연] 학생 체류 자격, 실제 요건 확인 필요',
    r.insurance_provider='[가상 시연] 교환교 안내 보험',
    r.review_summary=CONCAT('[가상 시연] ',u.name,'의 수강 신청 안내를 확인하고 숙소를 준비했습니다. 실제 경험담이 아닙니다.')
WHERE r.semester='2027-1';

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


-- 귀국보고서에는 지원 당시 GPA·어학 성적이나 탈락 결과가 없으므로 별도 가상 계정을 사용한다.
-- 실제 합격 기준이나 합격 확률을 뜻하지 않는다.
INSERT INTO `user` (school_email,password,nickname,major_type,created_at,updated_at)
SELECT CONCAT('passhufs-fail',d.no,'@example.hufs.ac.kr'),
       '$2b$12$4t7at76yue0DOtICsHHR7.ElrJVMXl0ynu5v6zWGoTzZ/FXjI/BNC',
       CONCAT('fail27',d.no),'PRIMARY',@demo_now,@demo_now
FROM passhufs_demo_2027_1 d
WHERE NOT EXISTS (
    SELECT 1 FROM `user` s
    WHERE s.school_email=CONCAT('passhufs-fail',d.no,'@example.hufs.ac.kr') COLLATE utf8mb4_unicode_ci
);

INSERT INTO admission_report (university_id,semester,result,language_test_type,language_score,gpa,
                              major_type,user_id,created_at,updated_at)
SELECT u.id,'2027-1','FAIL','IELTS',6.5,d.gpa-0.20,'PRIMARY',s.id,@demo_now,@demo_now
FROM passhufs_demo_2027_1 d JOIN university u ON u.name=d.name COLLATE utf8mb4_unicode_ci
JOIN `user` s ON s.school_email=CONCAT('passhufs-fail',d.no,'@example.hufs.ac.kr') COLLATE utf8mb4_unicode_ci
WHERE NOT EXISTS (
    SELECT 1 FROM admission_report a
    WHERE a.user_id=s.id AND a.university_id=u.id AND a.semester='2027-1' AND a.result='FAIL'
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


-- 국가 목록 조회 예시. 대학 테이블의 국가를 중복 제거한다.
SELECT DISTINCT u.country
FROM university u JOIN passhufs_demo_2027_1 d ON u.name=d.name COLLATE utf8mb4_unicode_ci
JOIN agreement a ON a.university_id=u.id AND a.semester='2027-1'
ORDER BY u.country;

-- 목록·상세 조회 예시. 어학 요건은 agreement_language_requirement를 추가 조회한다.
SELECT u.id,u.name,u.country,u.is_aacsb,u.is_equis,u.credit_system,u.course_list_url,
       a.semester,a.quota,a.min_gpa,a.is_open,a.exchange_period,a.cost_summary,a.benefit_summary,
       a.course_restriction,a.remark_summary,a.source_url
FROM university u JOIN passhufs_demo_2027_1 d ON u.name=d.name COLLATE utf8mb4_unicode_ci
JOIN agreement a ON a.university_id=u.id AND a.semester='2027-1'
ORDER BY d.no;

-- 합격·탈락 스펙 분포의 원천은 admission_report다.
SELECT u.id,u.name,a.result,COUNT(*) AS sample_count,
       MIN(a.gpa) AS min_gpa,MAX(a.gpa) AS max_gpa
FROM university u JOIN passhufs_demo_2027_1 d ON u.name=d.name COLLATE utf8mb4_unicode_ci
JOIN admission_report a ON a.university_id=u.id AND a.semester='2027-1'
GROUP BY u.id,u.name,a.result ORDER BY u.id,a.result;

DROP TEMPORARY TABLE passhufs_demo_2027_1;
