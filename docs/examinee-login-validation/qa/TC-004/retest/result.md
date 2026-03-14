# TC-004 재테스트: 재시험 방지 - 응시 완료 수험자 로그인 차단

- **상태**: FAIL
- **실행 시각**: 2026-03-14 23:20
- **수정 내용**: SubmissionRepository의 derived query를 @Query JPQL로 교체

## 테스트 데이터
- 수험자: 테스트유저 (생년월일: 2000-01-01, examineeId: 100)
- 시험: Java 기반 Web 개발자 기술 시험 (examId: 11, active: true, deleted: false)
- submission 수: 18개 (채점 완료)

## 테스트 단계 실행 기록

### Step 1: 응시 완료 수험자 정보 입력
- **수행**: /exam/login 페이지에서 이름 "테스트유저", 생년월일 "20000101" 입력
- **결과**: 입력 완료, "시험 시작" 버튼 활성화

### Step 2: 시험 시작 버튼 클릭
- **수행**: "시험 시작" 버튼 클릭
- **기대 결과**: 409 에러 → "이미 응시 완료한 시험입니다." 에러 메시지 표시, 페이지 이동 없음
- **실제 결과**: 200 OK 반환 → /exam/take/11 페이지로 정상 이동 (시험 응시 화면 표시)
- **스크린샷**: retest-exam-take-page.png

### Step 3: API 직접 호출 검증
- **수행**: POST /api/examinees/login (name: 테스트유저, birthDate: 2000-01-01) 직접 호출
- **결과**: HTTP 200 반환, examineeId: 100 응답
- **예상**: HTTP 409 반환되어야 함

### Step 4: 데이터 정합성 확인
- **수행**: GET /api/submissions/result?examineeId=100&examId=11 조회
- **결과**: submission 18개 존재 확인 (채점 완료 상태)
- **수행**: GET /api/exams 조회 → examId=11의 active=true 확인

## 판정
- **기대 결과**: 응시 완료 수험자 로그인 시 409 CONFLICT → "이미 응시 완료한 시험입니다." 에러 메시지 표시
- **실제 결과**: 200 OK 반환 → 시험 페이지로 정상 진입
- **판정**: FAIL

## 원인 분석

JPQL 쿼리가 `false`를 반환하고 있음:
```java
@Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Submission s " +
       "WHERE s.examinee.id = :examineeId AND s.problem.exam.active = true AND s.problem.exam.deleted = false")
boolean existsByExamineeIdAndActiveExam(@Param("examineeId") Long examineeId);
```

데이터 확인 결과:
- examineeId=100은 examId=11에 대해 submission 18개 존재
- examId=11은 active=true, deleted=false (목록 API에서 확인)
- 그럼에도 쿼리가 false 반환 → **JPQL 조인 경로 또는 쿼리 실행에 문제가 있을 가능성**

가능한 원인:
1. 서버 재시작이 제대로 되지 않아 변경된 코드가 반영되지 않았을 수 있음
2. JPQL에서 `s.problem.exam` 경로 탐색 시 그룹 자식 문제의 parent 관계로 인한 조인 문제
3. Hibernate 쿼리 캐시 또는 프록시 관련 이슈

**추가 디버깅 필요**: 서버 로그에서 실제 생성된 SQL 쿼리를 확인하여 WHERE 조건이 올바르게 변환되는지 검증 필요
