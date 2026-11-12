-- 사용자 제공 9개교 과목 자료 124건. QUT는 자료 미확보로 포함하지 않는다.
-- 원본/중복 처리/미확인 사항: db/seed/source/university_courses.json
-- 과목은 실제 제공 자료이며, 연결 보고서는 여러 자료를 묶은 가상 시연 컨테이너다.
-- 보고서의 2027-1은 시연용 학기이며 개별 과목의 실제 수학 학기를 뜻하지 않는다.
-- 실제 인정 사례나 인정 학점을 만들지 않는다. 외국 학점은 원문 그대로 보존한다.
-- NHH 동일 코드의 4/7학점 기록은 미확정 상태로 각각 보존한다.
-- 기존 대학 ID·기존 사용자·기존 보고서·수강계획은 변경하지 않는다.
-- 샘플 계정에는 무작위 로그인 불가 값만 생성하며 공통 비밀번호를 저장하지 않는다.
-- 개발·시연 DB에서만 수동 실행한다. 같은 파일을 재실행해도 중복 삽입하지 않는다.
SET NAMES utf8mb4;
DROP TEMPORARY TABLE IF EXISTS passhufs_course_demo_schools;
DROP TEMPORARY TABLE IF EXISTS passhufs_course_demo_items;
CREATE TEMPORARY TABLE passhufs_course_demo_schools (
    sample_no INT PRIMARY KEY, name VARCHAR(200) NOT NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE TEMPORARY TABLE passhufs_course_demo_items (
    sample_no INT NOT NULL, course_code VARCHAR(50), course_name_en VARCHAR(200) NOT NULL,
    foreign_credit DECIMAL(3,1) NOT NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
INSERT INTO passhufs_course_demo_schools VALUES
(1,'University of Groningen'),
(2,'University of Mannheim'),
(3,'Texas State University'),
(4,'Florida International University'),
(5,'Ghent University'),
(6,'NHH Norwegian School of Economics'),
(7,'Goethe University Frankfurt am Main'),
(8,'Audencia Business School'),
(9,'EM Normandie Business School');

INSERT INTO passhufs_course_demo_items VALUES
(1,'UCG1HU05','Music in Culture',5.0),
(1,'LXX029P05','Foodways in the Middle East: Heritage, Culture and Memory',5.0),
(1,'UCG2SS09','Decision making',5.0),
(1,'UCG2AC02','Philosophy of Science',5.0),
(1,'UCG2HU03','Knowledge is power',5.0),
(1,'UCG2SS02','Criminal law',5.0),
(1,'LBU018B05','Dutch Dynamics',5.0),
(1,'LBU048B05','Race and Region in Dutch Culture',5.0),
(1,'LBU058B05','Dutch Dynamics : Current Topics',5.0),
(1,NULL,'Dutch for University of Groningen  students, PhDs` and postdocs A1>A2',1.0),
(1,'UCG2HU07','World Literature',5.0),
(1,'UCG1SS09','Policy Making in the EU',5.0),
(1,'UCG2SS10','International Marketing',5.0),
(2,'ANG 422','Fat American Studies: Representations of (Un)Ideal Bodies in US Film & Media/Overlap with ANG 425',6.0),
(2,'MKW411','Public Opinion on Social Media Bans: International Perspectives',8.0),
(2,'2601','IWA A1.1',6.0),
(2,NULL,'IDV 401 International Cultural Studies Lecture',5.0),
(2,NULL,'IDV 401 Tutorial for the ICS Lecture',1.0),
(2,NULL,'IDV 402 Communication Across Cultures',6.0),
(2,NULL,'IDV 402 Sciencefluencing 101: Cultural Studies from Theory to Practice',6.0),
(2,'MAN 352','Human Resource Management',6.0),
(2,'Seminar ANG 303','Linguistics: Form and Function / Phonology and Poetry',6.0),
(2,'Seminar ANG 313','Literary Studies US / African American Theater&Culture in the 20th century',6.0),
(2,'Seminar ANG 313','Literary Studies US / US-American Women Writers',6.0),
(2,'2111','General German Language Course B1.1',3.0),
(2,'ANG 421','Compute Games People Play - Virtual Selves and Environments',4.0),
(2,'ANG 601','Language and Marketing 2',7.0),
(2,'42051','IWA A2.2',6.0),
(2,'10704-ANG-MA-0-P','ANG 621 Exercise: Area Studies: Economy, Business, Society',4.0),
(2,'18000-GES-BA-0-P','World History of State and Law',4.0),
(2,'817-VWL-BA-0-P','Law and Economics',6.0),
(2,'386-VWL-BA-0-P','Behavioral Economics',6.0),
(3,'ENG 3329','STUDIES IN MYTHOLOGY',3.0),
(3,'ENG 3331','BLACK LITERATURE',3.0),
(3,'ANTH 1312','CULTRL ANTHROPLGY',3.0),
(3,'ENG 3344','CHICANA/O/X NARRATIVE',3.0),
(3,'ENG 3386','Adolescent Literature',3.0),
(3,'PSY 3331','Social Psychology',3.0),
(3,'FR 1410','Beginning FR 1',4.0),
(3,'DAN 1280','Ballet 1',2.0),
(3,'ENG 2330','WORLD LIT FALL 25',3.0),
(3,'ENG 4310','Modern English Syntax',3.0),
(3,'ENG 3311','Writing About Music',3.0),
(3,'BA 2310','Intro to Global Business',3.0),
(3,'34961','German Translation',3.0),
(3,'33759','Basic Political Ideas',3.0),
(3,'31158','Fundamentals of CS',3.0),
(3,'41501','Prog for Scientists & Engineer',3.0),
(3,'ARTC1301','COMM DESIGN FOUNDATION I',3.0),
(3,'MU 2310','GUITAR CLASS I',3.0),
(3,'PS 3351','INTRO INTERNATIONAL STUDIES',3.0),
(3,'TH 1353','FILM PRODUCTION PRACTICES',3.0),
(4,'ASN 3153','Topics in Korean Culture',3.0),
(4,'COM 2417','Com in Film',3.0),
(4,'COM 3120','Organizational Comm',3.0),
(4,'COM 3230','Crisis Comm',3.0),
(4,'COM 3461','Intercultural Comm',3.0),
(4,'ECO 3101','Intermediate Microeconomics',3.0),
(4,'ECO 3106','Behavioral Economics',3.0),
(4,'ECO 3203','Intermediate Macroeconomics',3.0),
(4,'ECO 3223','Money&Banking',3.0),
(4,'MAR 4233','Social Media Marketing',3.0),
(4,'MAR 4354','Marketing yourself in the Global Marketplace',3.0),
(4,'LEI 3001','Leisure and Recreation in America',3.0),
(4,'HFT 3770','Cruise Line Operations&Management',3.0),
(5,'A005583','Art Policy and Museum Studies',5.0),
(5,'A005637','Music and Social Action',5.0),
(5,'A005761','English Literature: Historical Period',5.0),
(5,'A005759','English Linguistics: Syntax and Semantics',5.0),
(5,'A005959','Dutch as a Foreign Language: CEFR A2',6.0),
(5,'A005960','Dutch as a Foreign Language: CEFR B1',6.0),
(5,'F710161','European Enterprise Policy',3.0),
(5,'A006715','History of the Ancient Near East',5.0),
(5,'A003001','Academic English',3.0),
(5,'A005547','Dutch for Exchange Students',3.0),
(5,'F000798','Introduction to Global Economic History',5.0),
(5,'A003403','Sexual and Relational Ethics',5.0),
(6,'FOR16','Sustainable investments',4.0),
(6,'NOR10','Norwegian Language for Foreign Students level I (A1)',4.0),
(6,'SOL11','Managing Projects in the Digital Age',4.0),
(6,'FOR11','Business Taxation',4.0),
(6,'FOR11','Business Taxation',7.0),
(6,'NOR10','Norwegian Language for Foreign Students level I (A1)',7.0),
(6,'SOL11','Managing Projects in the Digital Age',7.0),
(6,'FOR16','Sustainable investments',7.0),
(6,'FOR16-V26','Sustainable Investment',4.0),
(6,'ACC413-V26','Sustainability & ESG Reporting',4.0),
(6,'NOR10-V26','Norwegian Language for Foreign Students',4.0),
(7,NULL,'DIA - Deutsch Intensivkurs für Austauschstudierende',3.0),
(7,'WPMF_INFI','International Finance',6.0),
(7,'WPME_MECC','Macroeconomics of Climate Change',6.0),
(7,'BAES1/L1','The Postmodern Condition',5.0),
(7,NULL,'American Literature and History',4.0),
(7,'wpmm_mare','Market Research',6.0),
(7,'PWIN','Wirtschaftsinformatik 2',6.0),
(7,'WPMF_LFIN','Leadership in Financial Industry',6.0),
(7,'WPMF_MCAI','Managment Control and Incentives',6.0),
(8,'STR402','Business Simulation',3.0),
(8,'ENT402','Entrepreneurial Project and Business plan',4.0),
(8,'FIN406','Financial Analysis',5.0),
(8,'FIN467','Financial Modeling',3.0),
(8,'CLE400','French',4.0),
(8,'FIN460','Introduction to International Project Finance',3.0),
(8,'MAPP_F_CGC401_EN_2025','Management Control',5.0),
(8,'MAPP_F_STR402_EN_2025','Business Simulation',3.0),
(8,'MAPP_F_FIN406_EN_2025','Financial Analysis',5.0),
(8,'MAPP_F_MKT404_EN_2025','Customer experience and branding',4.0),
(8,'MAPP_F_CLE450_2025','European business environment and culture',5.0),
(8,'MAPP_F_SPO401_2025','Sports Class',2.0),
(8,'MAPP_F_FIN465_2025','Data analytics fundamentals',3.0),
(8,'MAPP_F_MSI464_2025','Cyber security',3.0),
(8,'MAPP_S_CLE450_2024','European Business Environment and Culture',5.0),
(8,'MAPP_S_FIN460_2024','Introduction to international Project Finance',3.0),
(8,'MAPP_S_FIN465_2024','Data analytics fundamentals',3.0),
(8,'MAPP_S_MKT404_EN_2024','Customer experience and branding',4.0),
(8,'MAPP_S_STR401_EN_2024','Strategic management',5.0),
(8,'SPRING_CLE400_2024','French (Spring 2025)',4.0),
(8,'MAPP_S_CLE467_2024','Managing across cultures',3.0),
(8,'MAPP_S_MKT460_2024','Digital Marketing & Social Networks',3.0),
(8,'MAPP_S_STR402_EN_2024','Business simulation',3.0),
(9,'2425_MKT_1_EN_009','Services Marketing',5.0),
(9,'2425_SCM_1_EN_007','Port Management',5.0),
(9,'2425_DFC_1_EN_010','Internet Law and Cybersecurity',5.0),
(9,'2425_MKT_1_EN_036','Financial And Gaming Services',5.0);

START TRANSACTION;
SET @course_demo_now = CURRENT_TIMESTAMP;
INSERT INTO `user` (school_email,password,nickname,major_type,created_at,updated_at)
SELECT CONCAT('passhufs-courses-',LPAD(d.sample_no,2,'0'),'@example.hufs.ac.kr'),
       CONCAT('!',SHA2(UUID(),256)),CONCAT('courses',LPAD(d.sample_no,2,'0')),'PRIMARY',@course_demo_now,@course_demo_now
FROM passhufs_course_demo_schools d
JOIN university u ON u.name=d.name COLLATE utf8mb4_unicode_ci
WHERE NOT EXISTS (
    SELECT 1 FROM `user` s
    WHERE s.school_email=CONCAT('passhufs-courses-',LPAD(d.sample_no,2,'0'),'@example.hufs.ac.kr') COLLATE utf8mb4_unicode_ci
);

INSERT INTO return_report (user_id,university_id,semester,review_summary,created_at,updated_at)
SELECT s.id,u.id,'2027-1',
       '[시연용 과목 모음] 여러 귀국보고서의 과목 자료를 중복 정리한 컨테이너입니다. 실제 개인의 귀국보고서가 아닙니다. 2027-1은 시연용 학기이며 원본 수학 학기는 source JSON을 참고합니다. NHH의 4/7학점 차이는 원문대로 보존하며 인정 사실을 뜻하지 않습니다.',
       @course_demo_now,@course_demo_now
FROM passhufs_course_demo_schools d
JOIN university u ON u.name=d.name COLLATE utf8mb4_unicode_ci
JOIN `user` s ON s.school_email=CONCAT('passhufs-courses-',LPAD(d.sample_no,2,'0'),'@example.hufs.ac.kr') COLLATE utf8mb4_unicode_ci
WHERE NOT EXISTS (SELECT 1 FROM return_report r WHERE r.user_id=s.id AND r.semester='2027-1');

INSERT INTO return_report_course (return_report_id,course_code,course_name_en,foreign_credit,created_at,updated_at)
SELECT r.id,c.course_code,c.course_name_en,c.foreign_credit,@course_demo_now,@course_demo_now
FROM passhufs_course_demo_items c
JOIN passhufs_course_demo_schools d ON d.sample_no=c.sample_no
JOIN university u ON u.name=d.name COLLATE utf8mb4_unicode_ci
JOIN `user` s ON s.school_email=CONCAT('passhufs-courses-',LPAD(d.sample_no,2,'0'),'@example.hufs.ac.kr') COLLATE utf8mb4_unicode_ci
JOIN return_report r ON r.user_id=s.id AND r.university_id=u.id AND r.semester='2027-1'
WHERE NOT EXISTS (
    SELECT 1 FROM return_report_course existing
    WHERE existing.return_report_id=r.id
      AND existing.course_code <=> c.course_code COLLATE utf8mb4_unicode_ci
      AND existing.course_name_en=c.course_name_en COLLATE utf8mb4_unicode_ci
      AND existing.foreign_credit=c.foreign_credit
);
COMMIT;
DROP TEMPORARY TABLE passhufs_course_demo_items;
DROP TEMPORARY TABLE passhufs_course_demo_schools;
