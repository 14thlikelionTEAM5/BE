-- 재실행 정책: 기존 협정 공고·어학 기준은 덮어쓰지 않는다. 변경된 원문을 반영하려면 별도 검토된 수정 SQL을 사용한다.
-- 2027-1 공식 협정 공고: 상세-자격요건등 시트의 대상 10개교.
-- 기존 university.name으로 연결하며 대학 ID를 재생성하지 않는다.
-- 사용자·귀국보고서·합격/탈락·수강계획 샘플은 생성하지 않는다.
SET NAMES utf8mb4;

DROP TEMPORARY TABLE IF EXISTS passhufs_notice_2027_1;
CREATE TEMPORARY TABLE passhufs_notice_2027_1 (
 name VARCHAR(200) PRIMARY KEY, quota INT, business_only BOOLEAN, is_open BOOLEAN
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
INSERT INTO passhufs_notice_2027_1 VALUES
('University of Groningen',2,0,1),
('University of Mannheim',2,0,1),
('Texas State University',3,0,1),
('Florida International University',NULL,0,1),
('Ghent University',2,0,1),
('Queensland University of Technology',1,0,1),
('NHH Norwegian School of Economics',NULL,1,0),
('Goethe University Frankfurt am Main',3,1,1),
('Audencia Business School',3,1,1),
('EM Normandie Business School',5,1,1);

START TRANSACTION;
SET @demo_now = CURRENT_TIMESTAMP;

-- 공식 모집 엑셀 sheet1의 상세 조건. sheet2 요약본과 다른 경우 상세 원문을 사용한다.
-- FIU의 2~3명처럼 정수 한 칸으로 표현하지 못하는 선발인원은 NULL과 원문을 함께 보존한다.
DROP TEMPORARY TABLE IF EXISTS passhufs_notice_details;
CREATE TEMPORARY TABLE passhufs_notice_details (
    name VARCHAR(200) PRIMARY KEY, min_gpa DECIMAL(3,2), quota INT, is_open BOOLEAN,
    cost_summary TEXT, benefit_summary TEXT, course_restriction TEXT, remark_summary TEXT,
    course_list_url VARCHAR(500)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
INSERT INTO passhufs_notice_details VALUES
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

UPDATE university u JOIN passhufs_notice_details d ON u.name=d.name COLLATE utf8mb4_unicode_ci
SET u.course_list_url=d.course_list_url
WHERE u.course_list_url IS NULL AND d.course_list_url IS NOT NULL;

-- 기존 공고는 수동 수정값을 보존한다. 신규 공고만 상세 정보와 함께 삽입한다.
INSERT INTO agreement (university_id,semester,quota,exchange_period,is_business_only,is_open,
                       min_gpa,cost_summary,benefit_summary,course_restriction,remark_summary,
                       source_url,announced_at,imported_at,created_at,updated_at)
SELECT u.id,'2027-1',details.quota,'1학기',d.business_only,details.is_open,
       details.min_gpa,details.cost_summary,details.benefit_summary,details.course_restriction,
       details.remark_summary,'https://oia.hufs.ac.kr/bbs/oia/3431/262695/artclView.do',
       '2026-07-03',@demo_now,@demo_now,@demo_now
FROM passhufs_notice_2027_1 d
JOIN university u ON u.name=d.name COLLATE utf8mb4_unicode_ci
JOIN passhufs_notice_details details ON details.name=d.name
WHERE NOT EXISTS (SELECT 1 FROM agreement a WHERE a.university_id=u.id AND a.semester='2027-1');

DROP TEMPORARY TABLE IF EXISTS passhufs_notice_languages;
CREATE TEMPORARY TABLE passhufs_notice_languages (
    name VARCHAR(200), test_type VARCHAR(20), min_score DECIMAL(5,1),
    min_section_score DECIMAL(3,1), condition_note VARCHAR(100)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
INSERT INTO passhufs_notice_languages VALUES
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
FROM passhufs_notice_languages d JOIN university u ON u.name=d.name COLLATE utf8mb4_unicode_ci
JOIN agreement a ON a.university_id=u.id AND a.semester='2027-1'
WHERE NOT EXISTS (
    SELECT 1 FROM agreement_language_requirement l
    WHERE l.agreement_id=a.id AND l.test_type=d.test_type COLLATE utf8mb4_unicode_ci
);

DROP TEMPORARY TABLE passhufs_notice_languages;
DROP TEMPORARY TABLE passhufs_notice_details;

COMMIT;
DROP TEMPORARY TABLE passhufs_notice_2027_1;
