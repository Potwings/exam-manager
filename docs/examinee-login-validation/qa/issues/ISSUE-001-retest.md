# ISSUE-001 재테스트: 응시 완료 수험자 재시험 방지 여전히 미동작

- **심각도**: Critical
- **관련 TC**: TC-004 (재테스트)
- **재현 경로**:
  1. 활성 시험(examId=11)에 대해 응시 완료한 수험자 확인 (테스트유저, 2000-01-01)
  2. /exam/login 페이지에서 해당 수험자 정보 입력
  3. "시험 시작" 클릭
  4. 시험 페이지(/exam/take/11)로 정상 진입됨 (차단되지 않음)
- **기대 동작**: HTTP 409 CONFLICT 반환 → "이미 응시 완료한 시험입니다." 에러 메시지 표시
- **실제 동작**: HTTP 200 OK 반환 → 시험 페이지로 진입

## 상세 설명

SubmissionRepository의 `existsByExamineeIdAndActiveExam` JPQL 쿼리가 수정되었으나 여전히 `false`를 반환하고 있습니다.

### 수정된 JPQL
```java
@Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Submission s " +
       "WHERE s.examinee.id = :examineeId AND s.problem.exam.active = true AND s.problem.exam.deleted = false")
boolean existsByExamineeIdAndActiveExam(@Param("examineeId") Long examineeId);
```

### 검증된 데이터
- examineeId=100 → examId=11에 submission 18개 존재 (GET /api/submissions/result 확인)
- examId=11 → active=true (GET /api/exams 목록 확인)
- examId=11 → deleted=false (목록에 표시되므로 findByDeletedFalse 통과)

### 가능한 원인
1. **서버 미반영**: 코드 변경 후 서버가 실제로 재시작되지 않았거나, 이전 버전으로 실행 중일 수 있음
2. **JPQL 조인 경로 문제**: 그룹 문제(parent-child)의 `s.problem.exam` 경로 탐색에서 자식 문제의 경우 `problem.exam`이 null이거나 다르게 매핑될 수 있음
3. **derived query 잔존**: 기존 `existsByExamineeIdAndProblemExamId` 메서드가 여전히 19번 줄에 존재하지만, ExamineeService는 32번 줄에서 `existsByExamineeIdAndActiveExam`을 호출하고 있어 혼동은 아님

## 제안 수정 방향

1. **서버 재시작 확인**: `./gradlew.bat bootRun` 로그에서 JPQL 쿼리가 실제로 실행되는지 확인
2. **SQL 로그 확인**: `show-sql: true` 설정으로 Hibernate가 생성하는 실제 SQL 확인
3. **대안 JPQL**: 조인을 명시적으로 작성하여 경로 탐색 문제 회피
```java
@Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Submission s " +
       "JOIN s.problem p JOIN p.exam e " +
       "WHERE s.examinee.id = :examineeId AND e.active = true AND e.deleted = false")
```
