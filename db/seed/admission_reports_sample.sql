-- PassHUFS 시연용 샘플 합격·탈락 스펙 (2026-2). 실제 합격 데이터가 아니라 시연을 위해 만든 샘플 값이다.
-- 운영 DB에 실행할 수 있다. 협정교 상세의 합격 스펙 탭이 비지 않도록 10개교에 합격 2건, 탈락 1건씩 넣는다.
-- 점수는 2026-2 협정 공고의 학점·어학 기준(agreement_2026_2.sql)을 충족하는 범위에서 구성했다.
-- 로그인할 수 없는 시드 전용 계정(passhufs-sample01 ~ 30)만 생성하며 비밀번호/토큰은 포함하지 않는다.
-- 재실행 시 이미 있는 사용자와 스펙은 덮어쓰지 않고 누락된 것만 삽입한다.
-- admission_reports_2027_1.sql(가상 데이터, 운영 실행 금지)과는 별개다.
SET NAMES utf8mb4;

DROP TEMPORARY TABLE IF EXISTS passhufs_admission_sample;
CREATE TEMPORARY TABLE passhufs_admission_sample (
    sample_no CHAR(2) PRIMARY KEY, name VARCHAR(200), result VARCHAR(10),
    test_type VARCHAR(20), score DECIMAL(5,1), gpa DECIMAL(3,2), major_type VARCHAR(20)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
INSERT INTO passhufs_admission_sample VALUES
                                          ('01','University of Groningen','PASS','IELTS',7.0,4.12,'PRIMARY'),
                                          ('02','University of Groningen','PASS','TOEFL_IBT',98,3.95,'DOUBLE'),
                                          ('03','University of Groningen','FAIL','IELTS',6.5,3.62,'PRIMARY'),
                                          ('04','University of Mannheim','PASS','IELTS',7.0,4.05,'PRIMARY'),
                                          ('05','University of Mannheim','PASS','TOEFL_ITP',600,3.88,'PRIMARY'),
                                          ('06','University of Mannheim','FAIL','TOEFL_IBT',86,3.55,'MINOR'),
                                          ('07','Texas State University','PASS','TOEFL_IBT',95,3.82,'PRIMARY'),
                                          ('08','Texas State University','PASS','IELTS',7.0,4.01,'DOUBLE'),
                                          ('09','Texas State University','FAIL','TOEFL_ITP',575,3.52,'PRIMARY'),
                                          ('10','Florida International University','PASS','IELTS',7.0,3.96,'PRIMARY'),
                                          ('11','Florida International University','PASS','TOEFL_IBT',92,3.91,'PRIMARY'),
                                          ('12','Florida International University','FAIL','IELTS',6.5,3.45,'DOUBLE'),
                                          ('13','Ghent University','PASS','TOEFL_IBT',100,3.97,'PRIMARY'),
                                          ('14','Ghent University','PASS','IELTS',7.0,3.86,'MINOR'),
                                          ('15','Ghent University','FAIL','IELTS',6.5,3.50,'PRIMARY'),
                                          ('16','Queensland University of Technology','PASS','IELTS',7.0,4.08,'PRIMARY'),
                                          ('17','Queensland University of Technology','PASS','IELTS',7.5,3.92,'DOUBLE'),
                                          ('18','Queensland University of Technology','FAIL','IELTS',6.5,3.40,'PRIMARY'),
                                          ('19','NHH Norwegian School of Economics','PASS','TOEFL_IBT',100,4.15,'PRIMARY'),
                                          ('20','NHH Norwegian School of Economics','PASS','IELTS',7.0,4.03,'PRIMARY'),
                                          ('21','NHH Norwegian School of Economics','FAIL','TOEFL_ITP',560,3.60,'DOUBLE'),
                                          ('22','Goethe University Frankfurt am Main','PASS','IELTS',7.0,3.99,'PRIMARY'),
                                          ('23','Goethe University Frankfurt am Main','PASS','TOEFL_IBT',96,4.10,'PRIMARY'),
                                          ('24','Goethe University Frankfurt am Main','FAIL','TOEFL_IBT',87,3.58,'PRIMARY'),
                                          ('25','Audencia Business School','PASS','TOEFL_IBT',98,3.89,'DOUBLE'),
                                          ('26','Audencia Business School','PASS','TOEFL_ITP',590,3.84,'PRIMARY'),
                                          ('27','Audencia Business School','FAIL','IELTS',6.5,3.55,'PRIMARY'),
                                          ('28','EM Normandie Business School','PASS','IELTS',6.5,3.78,'PRIMARY'),
                                          ('29','EM Normandie Business School','PASS','TOEFL_IBT',90,3.94,'PRIMARY'),
                                          ('30','EM Normandie Business School','FAIL','IELTS',6.0,3.42,'MINOR');

START TRANSACTION;
SET @sample_now = CURRENT_TIMESTAMP;

INSERT INTO `user` (school_email,password,nickname,major_type,created_at,updated_at)
SELECT CONCAT('passhufs-sample',d.sample_no,'@example.hufs.ac.kr'),
       CONCAT('!',SHA2(UUID(),256)),CONCAT('sample',d.sample_no),d.major_type,@sample_now,@sample_now
FROM passhufs_admission_sample d
         JOIN university u ON u.name=d.name COLLATE utf8mb4_unicode_ci
WHERE NOT EXISTS (
    SELECT 1 FROM `user` s
    WHERE s.school_email=CONCAT('passhufs-sample',d.sample_no,'@example.hufs.ac.kr') COLLATE utf8mb4_unicode_ci
);

INSERT INTO admission_report (university_id,semester,result,language_test_type,language_score,gpa,
                              major_type,user_id,created_at,updated_at)
SELECT u.id,'2026-2',d.result,d.test_type,d.score,d.gpa,d.major_type,s.id,@sample_now,@sample_now
FROM passhufs_admission_sample d
         JOIN university u ON u.name=d.name COLLATE utf8mb4_unicode_ci
         JOIN `user` s ON s.school_email=CONCAT('passhufs-sample',d.sample_no,'@example.hufs.ac.kr') COLLATE utf8mb4_unicode_ci
WHERE NOT EXISTS (
    SELECT 1 FROM admission_report a
    WHERE a.user_id=s.id AND a.university_id=u.id AND a.semester='2026-2'
      AND a.result=d.result COLLATE utf8mb4_unicode_ci
);

COMMIT;
DROP TEMPORARY TABLE passhufs_admission_sample;