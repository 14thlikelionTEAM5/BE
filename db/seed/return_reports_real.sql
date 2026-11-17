-- 사용자 제공 귀국보고서 캡처·본문을 직접 구조화하고 AI로 요약한 시연용 데이터. 종정시 연동 데이터가 아니다. 원본: db/seed/source/return_reports.json
-- 조회 시 AI를 호출하지 않는다. 기존 과목 모음용 보고서와 별도 레코드로 저장한다.
-- 로그인 불가능한 시드 전용 계정만 생성하며, 비밀번호/토큰/인증 코드는 포함하지 않는다.
-- 재실행 시 기존 사용자 및 보고서는 덮어쓰지 않고 누락된 데이터만 삽입한다.
-- NHH는 semester가 확정된 경우에만 적재한다. QUT는 원문 미확보로 제외한다.
SET NAMES utf8mb4;
DROP TEMPORARY TABLE IF EXISTS passhufs_real_reports;
CREATE TEMPORARY TABLE passhufs_real_reports (
 sample_no INT, university_name VARCHAR(200), semester VARCHAR(10),
 cost_flight VARCHAR(200), cost_insurance VARCHAR(200), cost_living_monthly VARCHAR(200), cost_total VARCHAR(200),
 housing_type VARCHAR(20), visa_duration VARCHAR(100), visa_type VARCHAR(100), insurance_provider VARCHAR(200),
 review_summary TEXT, source_submitted_at DATE, source_updated_at DATETIME
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
INSERT INTO passhufs_real_reports VALUES
('3','Texas State University','2026-1','약 200만원','총비용에 포함 (필수)','매달 150-200만원 정도','약 8000달러','DORM','1주','F비자','외국대학교','학부수업으로 3학점 과목 4개를 수강했다. Cypress의 개별 방과 화장실을 사용하는 3인실에 거주했으며, 교환교 보험 가입은 필수였다. 문학 수업과 Cultural Anthropology 수업 경험, writing center 및 교통편에 대한 후기를 제공했다.','2026-06-05','2026-06-05 13:06:12'),
('1','University of Groningen','2026-1','For round trip you can spend around 500-600','Swiss care 70 euro per semester','Transportation- bike 15 euro per month','7500 euro per semester','SELF','4-5 weeks','학생 비자','Swiss care','2월부터 6월까지 수학하며 5학점 과목 6개를 수강했다. 수업은 소규모 세미나와 조별 과제 중심이었다. 비자와 생활비 증명을 위한 송금, 숙소 계약 및 네덜란드 은행 계좌 개설에 관한 주의사항을 제공했다.','2026-09-04','2026-09-04 17:09:52'),
('2','University of Mannheim','2026-1','약 120만원','한달에 약 141유로','달에 약 100-200만원(여행비용)','기숙사비(390x6) 수업료 100, 식비 및 생활비 약 500-600유로','DORM','2주','학생비자','TK 공보험 (Expatrio를 통해 가입)','1월 초순부터 6월 중순까지 수학하며 School of Humanities에서 세미나 중심의 수업을 수강했다. Ulmenweg 기숙사에서 개인 방과 공용 주방·화장실을 사용하는 플랫 형태로 생활했다. 학교 안내 이메일, 수강 및 시험 신청, 거주지 등록과 귀국 전 해지 절차 확인을 권장했다.','2026-09-26','2026-09-26 23:09:33'),
('4','Florida International University','2026-1','왕복 약 350만원','ISO 보험 월 별 $75, 국내 유학생 보험 약 20만원','달마다 250~300만원','기숙사비+밀플랜 75블럭 약 1000만원','DORM','1주','J-1 비자','ISO FIU 전용 플랜 및 국내 유학생 보험','1월 초순부터 4월 하순까지 수학하며 커뮤니케이션 등 3학점 과목 5개를 수강했다. 희망 교과목 리스트를 제출하면 FIU 측에서 수강신청을 진행하며 학기 초 변경 기간을 활용할 수 있다고 설명했다. Tamiami Hall 거주, ISO 보험 및 국내 유학생 보험 가입 경험을 공유했고, 프로젝트와 발표 중심 수업 및 높은 생활비를 언급했다.','2026-05-28','2026-05-28 18:05:08'),
('5','Ghent University','2026-1','왕복 160만원','약 15만원','첫달 정착 비용 약 150유로 / 여행비 약 3000유로','약 1500만원 (매달 Blocked Account 1050유로/ 기숙사비 561유로)','DORM','3주','학생비자 (D비자)','국내보험업체','학부수업으로 5 ECTS 과목 4개를 수강했으며 기말시험과 공통 평가 기준에 대비할 것을 권장했다. Home Göttingen에서 개인 방·화장실과 공용 주방을 이용했다. 기숙사 신청을 위해 입학 지원서를 빨리 작성하고, 비자 및 현지 거주증 절차를 미리 준비할 것을 안내했다. ESN과 기숙사 프로그램을 활용한 교류 경험을 공유했다.','2026-11-07','2026-11-07 23:11:13'),
('7','NHH Norwegian School of Economics','2026-1','Bergen행 비행기 약 100만 원(출국편 편도)','약 20만원','여행 약 1천5백만원','기숙사비 달 90-100만원/식비 25-50만원','DORM','2주','학생 비자','국내보험업체','1월 초순부터 6월 중순까지 수학하며 영어로 진행되는 과목을 수강했다. Hatleberg 기숙사와 Sammen 신청 경로, 공항에서의 대중교통·공항버스·우버 이용 경험을 공유했다. 학기 초 환영 행사와 교내 동아리를 소개했으며, 식비 절약을 위해 현지 마트 이용을 권장했다.','2026-06-30','2026-09-01 13:09:02'),
('8','Goethe University Frankfurt am Main','2026-1','왕복 약 200만원','매달 약 146유로','매달 992유로(엑스파트리오)','약 1000만원+여행비','DORM','3개월','학생 비자','외국대학교','정규학기 전 DIA 독일어 과정과 버디 프로그램을 추천한다. 대형 lecture와 tutorial로 수업을 진행하며 lecture는 별도 수강신청을 하지 않는다. apartment형 기숙사에 만족했고 프랑크푸르트의 교통과 여행 접근성이 좋다고 평가했다.','2026-08-11','2026-08-12 16:08:52'),
('9','Audencia Business School','2025-2','170','20','약 월 200만원씩,+여행비','대략 2000만원 소요','SELF','2주','학생비자-D','국내보험업체','팀플·발표·그룹 과제가 많고 라이팅과 리스닝 역량이 필요하다고 설명한다. 입학 허가 후 안내 메일을 받아 정해진 기간 구글폼으로 수강신청하며 시간표가 매주 바뀔 수 있다. 학교 근처 셰어하우스에서 생활했고 국제학생회와 교내 동아리 활동을 소개했다.','2026-02-24','2026-02-24 17:02:34'),
('10','EM Normandie Business School','2025-1','왕복 약 120만원','약 20만원',NULL,'약 1500만원','SELF','4주','학생 비자','국내보험업체','르아브르 캠퍼스에서 수학했으며 팀플과 발표가 많았다고 설명한다. Services Marketing 수업을 추천했고 Port Management 수업은 어려웠다고 평가했다. 학교 근처 사설 기숙사에 거주했으며 학교 직원들이 친절하고 학기 중 짧은 방학을 활용해 여행했다고 작성했다.','2025-09-07','2025-09-07 20:09:49');

START TRANSACTION;
INSERT INTO `user` (school_email,password,nickname,major_type,created_at,updated_at)
SELECT CONCAT('passhufs-report-',LPAD(d.sample_no,2,'0'),'@example.hufs.ac.kr'),
       CONCAT('!',SHA2(UUID(),256)),CONCAT('report',LPAD(d.sample_no,2,'0')),'PRIMARY',NOW(),NOW()
FROM passhufs_real_reports d
JOIN university u ON u.name=d.university_name COLLATE utf8mb4_unicode_ci
WHERE d.semester IS NOT NULL AND NOT EXISTS (
 SELECT 1 FROM `user` s WHERE s.school_email=CONCAT('passhufs-report-',LPAD(d.sample_no,2,'0'),'@example.hufs.ac.kr') COLLATE utf8mb4_unicode_ci
);

INSERT INTO return_report (user_id,university_id,semester,cost_flight,cost_insurance,cost_living_monthly,cost_total,
 housing_type,visa_duration,visa_type,insurance_provider,review_summary,created_at,updated_at)
SELECT s.id,u.id,d.semester,d.cost_flight,d.cost_insurance,d.cost_living_monthly,d.cost_total,
 d.housing_type,d.visa_duration,d.visa_type,d.insurance_provider,d.review_summary,
 d.source_submitted_at,COALESCE(d.source_updated_at,d.source_submitted_at)
FROM passhufs_real_reports d
JOIN university u ON u.name=d.university_name COLLATE utf8mb4_unicode_ci
JOIN `user` s ON s.school_email=CONCAT('passhufs-report-',LPAD(d.sample_no,2,'0'),'@example.hufs.ac.kr') COLLATE utf8mb4_unicode_ci
WHERE d.semester IS NOT NULL AND NOT EXISTS (
 SELECT 1 FROM return_report r WHERE r.user_id=s.id AND r.semester=d.semester
);
-- 이전 시드의 잘린 값만 보정한다. 사용자 수정값 및 다른 보고서는 덮어쓰지 않는다.
UPDATE return_report r
JOIN `user` s ON s.id=r.user_id
JOIN university u ON u.id=r.university_id
SET r.cost_total='약 1500만원 (매달 Blocked Account 1050유로/ 기숙사비 561유로)'
WHERE s.school_email='passhufs-report-05@example.hufs.ac.kr'
  AND u.name='Ghent University' AND r.semester='2026-1'
  AND r.cost_total IN ('약 1500만원 (매달 Blocked Account 1050유로/ 기', '약 1500만원');
COMMIT;
DROP TEMPORARY TABLE passhufs_real_reports;
