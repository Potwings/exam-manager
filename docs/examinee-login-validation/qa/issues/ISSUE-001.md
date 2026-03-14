# ISSUE-001: 로그인 시점 재시험 방지 쿼리 미동작

- **심각도**: Critical
- **관련 TC**: TC-004
- **재현 경로**:
  1. 활성 시험(ID:11)에 수험자(테스트유저/2000-01-01)로 로그인하여 답안 제출 완료
  2. `/exam/login`으로 이동
  3. 동일 수험자 정보(테스트유저/20000101) 입력 후 [시험 시작] 클릭
  4. 기대: 409 CONFLICT 에러 + "이미 응시 완료한 시험입니다." 메시지
  5. 실제: 200 OK + `/exam/take/11`로 이동 (시험 화면 진입)
- **기대 동작**: ExamineeService.loginOrCreate()에서 기존 수험자의 활성 시험 제출 기록이 있으면 409 CONFLICT를 반환해야 함
- **실제 동작**: 로그인 API(`POST /api/examinees/login`)가 200 OK를 반환하여 시험 화면 진입 허용

## 환경
- Browser: Chromium (Playwright MCP)
- Frontend: http://localhost:5173
- Backend: http://localhost:8080

## 상세 설명

### 원인 분석
`ExamineeService.loginOrCreate()` 메서드에서 사용하는 Spring Data JPA 쿼리 메서드:
```java
submissionRepository.existsByExamineeIdAndProblemExamActiveTrueAndProblemExamDeletedFalse(examinee.getId())
```

이 쿼리가 `false`를 반환하여 재시험 방지 검증을 통과시키고 있음.

### 검증 데이터
- 동일 수험자(examineeId: 98, 홍길동)에 대해:
  - `GET /api/scores/exam/11` -> 제출 기록 존재 (totalScore: 0)
  - `POST /api/submissions` (제출 재시도) -> 409 CONFLICT (기존 `existsByExamineeIdAndProblemExamId` 쿼리 정상 동작)
  - `POST /api/examinees/login` -> 200 OK (신규 쿼리 `existsByExamineeIdAndProblemExamActiveTrueAndProblemExamDeletedFalse` 미동작)
- 시험 상태: `active: true`, `deleted: false` 확인 완료

### 추정 원인
`existsByExamineeIdAndProblemExamActiveTrueAndProblemExamDeletedFalse` 쿼리 메서드명의 Spring Data JPA 파싱 문제:
- `Exam.active`와 `Exam.deleted` 필드가 `Boolean` (wrapper) 타입
- 경로 탐색: `Submission.problem` -> `Problem.exam` -> `Exam.active`/`Exam.deleted`
- 메서드명이 너무 길어서 파싱이 의도와 다르게 될 가능성 있음 (예: `ProblemExamActive`가 하나의 프로퍼티명으로 해석)

### 참고
- 프론트엔드 에러 핸들링은 정상 동작 확인됨 (Playwright route intercept로 409 mock 테스트 완료)
- 제출 시점의 기존 재시험 방지(`existsByExamineeIdAndProblemExamId`)는 정상 동작

## 제안 수정 방향
1. **@Query 어노테이션 사용**: 메서드명 자동 생성 대신 명시적 JPQL 작성
```java
@Query("SELECT COUNT(s) > 0 FROM Submission s WHERE s.examinee.id = :examineeId AND s.problem.exam.active = true AND s.problem.exam.deleted = false")
boolean existsByExamineeIdAndActiveExam(@Param("examineeId") Long examineeId);
```
2. 또는 `show-sql: true` 상태에서 생성되는 SQL 로그를 확인하여 쿼리가 의도대로 조립되는지 검증
