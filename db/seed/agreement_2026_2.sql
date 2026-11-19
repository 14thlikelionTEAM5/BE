-- 2026-2 협정 공고 시드 (2026학년도 2학기 영어권 모집 엑셀 기준, 시연 대상 10개교).
-- 2027-1과 비교한 "이번 학기 변경 안내"(신규/인원 변경/모집 중단)를 계산하기 위한 이전 학기 데이터다.
-- 기존 university.name으로 연결하며 대학 ID를 재생성하지 않는다. 사용자·귀국보고서 등은 만들지 않는다.
-- 재실행 시 이미 있는 2026-2 협정과 어학 기준은 덮어쓰지 않고 누락된 것만 삽입한다.
-- 선발인원이 '1~2'처럼 정수 한 칸으로 표현되지 않는 경우(FIU)는 NULL로 둔다.
SET NAMES utf8mb4;

START TRANSACTION;
SET @seed_now = CURRENT_TIMESTAMP;

DROP TEMPORARY TABLE IF EXISTS passhufs_notice_2026_2;
CREATE TEMPORARY TABLE passhufs_notice_2026_2 (
    name VARCHAR(200) PRIMARY KEY, min_gpa DECIMAL(3,2), quota INT, business_only BOOLEAN,
    cost_summary TEXT, benefit_summary TEXT, course_restriction TEXT, remark_summary TEXT
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
INSERT INTO passhufs_notice_2026_2 VALUES
                                       ('University of Groningen',3.0,2,0,'• 숙소: 350 ~ 500/월\n• 식비: 150 ~ 250/월\n• 보험: 100 ~ 120/월','•  외국대학 등록금 면제','• 지원가능 학부: Faculty of Arts, Faculty of Philosophy, Faculty of Religious Studies, University College Fryslan, University College Groningen \n• 본인이 지원한 학부 내에서만 수강 가능','• 각종안내: https://www.rug.nl/education/exchange/\n• 숙소(기숙사 제공하지 않음): https://www.rug.nl/education/bachelor/international-students/study-in-the-netherlands-groningen/accommodation\n• 비용: https://www.rug.nl/education/bachelor/international-students/financial-matters/living-expenses\n• Resident Permit 진행시 1학기 생활비(약 5,500유로)를 RUG에 미리 송금해야 함'),
                                       ('University of Mannheim',3.0,2,0,'€ 970 ~ 1,000/월\n(거주비 포함/\n출처: Factsheet)\n• 숙소: 400~/월\n• 보험: 125/월\n• 교통비: 190~/학기','•  외국대학 등록금 면제','• 지원가능 학부: School of Humanities \n• School of Humanities 내에서 50% 이상의 수업을 수강하는 경우, University Wide Electives에서 한 두과목 정도 추가 수강 가능 (기타 학부는 교수의 허가가 있을 경우에만 수강 가능)','• 각종안내: https://www.uni-mannheim.de/en/academics/coming-to-mannheim/exchange-students/\n• 강의목록: https://www.uni-mannheim.de/en/academics/coming-to-mannheim/exchange-students/courses/course-catalog/\n• 숙소(가을학기 파견학생의 경우 특히 기숙사 배정 어려움, 외부숙소): https://www.uni-mannheim.de/en/academics/coming-to-mannheim/exchange-students/housing/'),
                                       ('Texas State University',3.4,2,0,'$8,000~/학기','• 외국대학 등록금 면제','• Mass Communication, Business Administration, Marketing, Forensics 전공 인기\n• 기관토플 제출 시 어학성적 자격요건을 충족했더라도 영어권 교환선발 면접에서 영어실력이 부족하다고 판단될 시에는 선발 불가','• 각종안내: https://www.educationabroad.txst.edu/\n• 강의목록: https://ssb-prod.ec.txstate.edu/PROD/bwckschd.p_disp_dyn_sched\n• 숙소(기숙사 신청): https://www.reslife.txst.edu/'),
                                       ('Florida International University',3.38,NULL,0,'US$ 10,662 ~/학기\n(출처-Factsheet)\n• 기숙사: 4,222~ /학기\n• 밀플랜: 2,028~ /학기\n• 교재비 외: 675~ /학기\n• 보험: 1,212~ /학기\n• 기타: 2,500~ /학기','• 외국대학 등록금 면제','• 수강불가: College of Medicine, College of Law\n• 수강제한: Finance, Accounting, Communication (Advertising, Journalism, Public Relations), Nursing, Nutrition and Dietetics','• 각종안내(강의목록 포함): https://abroad.fiu.edu/index.cfm?FuseAction=Programs.ViewProgramAngular&id=10203\n• 숙소(기숙사 선착순 배정): https://housing.fiu.edu/\n• Exchange/Education Abroad Office: https://studyabroad.fiu.edu/'),
                                       ('Ghent University',3.0,1,0,NULL,'• 외국대학 등록금 면제','• 지원가능 학부: Faculty of Arts and Philosophy (Course code A로 시작)\n• 본인 소속학부에서 최소 50% 이상 수강해야함','• 강의목록: https://studiekiezer.ugent.be/en/zoek\n  - Exchange Programme으로 조회 \n  - 교과목 정보에서 initial competences를 반드시 확인해야함\n• 숙소(기숙사 선착순 배정): https://www.ugent.be/en/facilities/housing/exchange.htm\n• 비용: https://www.ugent.be/en/facilities'),
                                       ('Queensland University of Technology',3.0,1,0,'• 정착금: $2,500\n• 생활비(숙소 포함): $500~/주\nhttps://www.qut.edu.au/study/international/apply-for-study-abroad#Costs','• 외국대학 등록금 면제','• 수강불가: Health disciplines – Podiatry, Paramedic Science, Optometry and Vision Science, Medical Imaging, Radiation Therapy, Pharmacy and PG Psychology\n• Term 1: 2월 - 7월, Term2: 7월 - 11월','• 개설전공: https://www.qut.edu.au/study/applying/study-abroad-and-exchange/what-can-i-study\n• 각종안내: https://www.qut.edu.au/study/applying/study-abroad-and-exchange/exchange\n• 숙소: https://www.qut.edu.au/study/student-life/living-in-brisbane/accommodation-in-brisbane'),
                                       ('NHH Norwegian School of Economics',3.0,2,1,'NOK11,000~/월','•  외국대학 등록금 면제','• 학부과정 수업만 선택 가능하며 석사과정 수강 희망시 개별 승인 필요','• 각종안내(숙소포함) : https://www.nhh.no/en/for-students/international-opportunities/exchange/incoming-exchange/\n• 강의목록: https://www.nhh.no/en/for-students/courses2/bachelor-courses-exchange/\n• 비용: https://www.studybergen.com/why-study-in-bergen/studying-in-norway/cost-of-living/'),
                                       ('Goethe University Frankfurt am Main',3.0,2,1,'• 생활비(숙소,식비 등): 800~/월\n• 보험: 120/월\n• Semester Contribution: 326/학기','•  외국대학 등록금 면제','• Faculty of Economics and Business 학부수업만 선택 가능','• 각종안내(강의목록 포함): https://www.goethe-university-frankfurt.de/122913225/Before_your_Mobility_to_Frankfurt\n• 숙소(기숙사 선착순 배정 또는 외부숙소)\n• 비용: https://www.goethe-university-frankfurt.de/122919146/During_your_stay_in_Frankfurt#Finances'),
                                       ('Audencia Business School',3.0,3,1,'https://www.audencia.com/en/student-experience/everyday-life/international-students','•  외국대학 등록금 면제','• 3학년 이상 파견 가능\n• Atlantic Campus (Grande Ecole Programme)\n• 경영전공자가 이닌 경우 City Campus (Bachelor Program)에서 수강해야함','• 각종안내(강의목록, Factsheet 포함): https://apply.exchangestudents.audencia.com/_portal/\n• 최초 수강신청 이후 변경 불가\n• 숙소(외부숙소 학생 스스로 찾아야함)\n• 비용: https://www.expatistan.com/cost-of-living/nantes'),
                                       ('EM Normandie Business School',3.3,5,1,'€ 800 ~ 1,300\n/월\n(Le Havre 기준\n출처-Information sheet)','•  외국대학 등록금 면제','• 지원가능 학부: International Management(BIM), Business Administraion(BBA)\n• 석사 과정은 원칙적으로 수강 불가하나 한국의 4학년에 해당하는 프랑스 그랑제꼴 과정 Master 1년 과정 수강을 희망하는 4학년 교환학생인 경우 학과와 검토 후 승인여부 결정','• 학부과정은 Le Havre 캠퍼스만 선택 가능 \n• 각종안내(강의목록, Factsheet, Welcome guide 포함): https://www.em-normandie.com/en/exchange-programmes\n• 숙소(외부숙소 학생 스스로 찾아야함) \n- Campus France website 방문하여 추가 정보 확인 필요, 주정부 운영 CROUS와 연계가능');

INSERT INTO agreement (university_id,semester,quota,exchange_period,is_business_only,is_open,
                       min_gpa,cost_summary,benefit_summary,course_restriction,remark_summary,
                       source_url,announced_at,imported_at,created_at,updated_at)
SELECT u.id,'2026-2',d.quota,'1학기',d.business_only,1,
       d.min_gpa,d.cost_summary,d.benefit_summary,d.course_restriction,d.remark_summary,
       NULL,'2026-01-08',@seed_now,@seed_now,@seed_now
FROM passhufs_notice_2026_2 d
         JOIN university u ON u.name=d.name COLLATE utf8mb4_unicode_ci
WHERE NOT EXISTS (SELECT 1 FROM agreement a WHERE a.university_id=u.id AND a.semester='2026-2');

DROP TEMPORARY TABLE IF EXISTS passhufs_notice_languages_2026_2;
CREATE TEMPORARY TABLE passhufs_notice_languages_2026_2 (
    name VARCHAR(200), test_type VARCHAR(20), min_score DECIMAL(5,1),
    min_section_score DECIMAL(3,1), condition_note VARCHAR(100)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
INSERT INTO passhufs_notice_languages_2026_2 VALUES
                                                 ('University of Groningen','TOEFL_IBT',90,21,'90 (영역별 21이상)'),
                                                 ('University of Groningen','IELTS',6.5,6.0,'6.5 (영역별 6.0이상)'),
                                                 ('University of Mannheim','TOEFL_IBT',85,NULL,'85'),
                                                 ('University of Mannheim','TOEFL_ITP',570,NULL,'570'),
                                                 ('University of Mannheim','IELTS',6.5,6.0,'6.5 (영역별 6.0이상)'),
                                                 ('Texas State University','TOEFL_IBT',80,19,'80 (영역별 19이상)'),
                                                 ('Texas State University','TOEFL_ITP',570,NULL,'570'),
                                                 ('Texas State University','IELTS',6.5,6.0,'6.5 (영역별 6.0이상)'),
                                                 ('Florida International University','TOEFL_IBT',80,NULL,'80'),
                                                 ('Florida International University','IELTS',6.5,6.0,'6.5 (영역별 6.0이상)'),
                                                 ('Ghent University','TOEFL_IBT',90,NULL,'90'),
                                                 ('Ghent University','IELTS',6.5,6.0,'6.5 (영역별 6.0이상)'),
                                                 ('Queensland University of Technology','IELTS',6.5,6.0,'6.5 (영역별 6.0이상)'),
                                                 ('NHH Norwegian School of Economics','TOEFL_IBT',80,NULL,'80'),
                                                 ('NHH Norwegian School of Economics','TOEFL_ITP',550,NULL,'550'),
                                                 ('NHH Norwegian School of Economics','IELTS',6,NULL,'6'),
                                                 ('Goethe University Frankfurt am Main','TOEFL_IBT',85,NULL,'85'),
                                                 ('Goethe University Frankfurt am Main','IELTS',6.5,6.0,'6.5 (영역별 6.0이상)'),
                                                 ('Audencia Business School','TOEFL_IBT',89,NULL,'89'),
                                                 ('Audencia Business School','TOEFL_ITP',570,NULL,'570'),
                                                 ('Audencia Business School','IELTS',6.5,6.0,'6.5 (영역별 6.0이상)'),
                                                 ('EM Normandie Business School','TOEFL_IBT',80,NULL,'80'),
                                                 ('EM Normandie Business School','IELTS',6,5.5,'6 (영역별 5.5이상)');

INSERT INTO agreement_language_requirement
(agreement_id,test_type,min_score,min_section_score,condition_note,created_at,updated_at)
SELECT a.id,d.test_type,d.min_score,d.min_section_score,d.condition_note,@seed_now,@seed_now
FROM passhufs_notice_languages_2026_2 d
         JOIN university u ON u.name=d.name COLLATE utf8mb4_unicode_ci
         JOIN agreement a ON a.university_id=u.id AND a.semester='2026-2'
WHERE NOT EXISTS (
    SELECT 1 FROM agreement_language_requirement l
    WHERE l.agreement_id=a.id AND l.test_type=d.test_type COLLATE utf8mb4_unicode_ci
);

DROP TEMPORARY TABLE passhufs_notice_languages_2026_2;
DROP TEMPORARY TABLE passhufs_notice_2026_2;

COMMIT;