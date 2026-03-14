# Backend 변경 설명 — ExamineeService 분리 및 로그인 재시험 방지

## 변경 개요
ExamineeController에서 Repository를 직접 호출하던 로직을 ExamineeService로 분리하고, 수험자 로그인 시 활성 시험에 대한 재시험 방지 검증을 추가했다.

## 변경 파일

### 1. SubmissionRepository.java (수정)
**변경**: `existsByExamineeIdAndProblemExamActiveTrueAndProblemExamDeletedFalse(Long examineeId)` 메서드 추가

**왜**: 로그인 시점에는 특정 examId를 모르므로, "현재 활성(active=true) + 미삭제(deleted=false) 시험에 제출한 적이 있는지"를 한 번에 확인해야 한다. 기존 `existsByExamineeIdAndProblemExamId`는 examId를 알아야 사용 가능하므로 새 메서드가 필요했다.

**동작 원리**: JPA derived query로 `Submission → Problem → Exam`을 자동 조인한다. Spring Data JPA가 메서드 이름을 파싱하여 다음과 같은 JPQL을 생성한다:
```sql
SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
FROM Submission s
WHERE s.examinee.id = ?1
  AND s.problem.exam.active = true
  AND s.problem.exam.deleted = false
```

### 2. ExamineeService.java (신규)
**왜**: Controller에서 Repository를 직접 호출하는 것은 계층 구조(Controller → Service → Repository) 위반이다. 비즈니스 로직(find-or-create + 동시성 처리 + 재시험 검증)을 Service 레이어로 옮겨 단일 책임 원칙을 따른다.

**동작 원리**:
1. 이름+생년월일로 기존 수험자 조회
2. **기존 수험자인 경우**: 활성 시험에 제출 기록이 있는지 검증 → 있으면 409 CONFLICT
3. **신규 수험자인 경우**: 바로 save. 제출 기록이 있을 수 없으므로 검증을 건너뛴다 (불필요한 DB 쿼리 방지)
4. **동시성 처리**: save 시 DataIntegrityViolationException 발생하면 재조회 (UNIQUE 제약조건 충돌)

**설계 결정**:
- `@Transactional`을 서비스 메서드에 선언: 조회 → 검증 → 저장이 하나의 트랜잭션 안에서 실행되어 일관성 보장
- 기존 SubmissionService의 제출 시점 재시험 방지 로직은 유지: 이중 방어 전략 (로그인 시 + 제출 시 모두 검증)
- 신규 수험자는 DB 조회 자체를 하지 않음: Optional.empty()로 분기되므로 submissionRepository 호출이 발생하지 않는다

### 3. ExamineeController.java (수정)
**변경**: `ExamineeRepository` 의존 제거 → `ExamineeService` 주입으로 교체

**왜**: Controller는 HTTP 요청/응답 매핑만 담당해야 한다. 비즈니스 로직(find-or-create, 재시험 검증)은 Service에 위임한다.

**동작**: request에서 name/birthDate를 추출하여 Service에 전달하고, 반환된 Entity를 DTO로 변환하여 응답한다.

### 4. ExamineeServiceTest.java (신규)
Mockito 기반 단위 테스트 5개:

| 테스트 | 검증 내용 |
|--------|-----------|
| 기존 수험자 정상 로그인 | 미응시 수험자가 그대로 반환되고, save가 호출되지 않음 |
| 신규 수험자 생성 | save 호출되고, 재시험 검증(submissionRepository)이 호출되지 않음 |
| 동시성 충돌 후 재조회 | DataIntegrityViolationException 발생 시 findByNameAndBirthDate 재조회 |
| 재시험 방지 409 | 활성 시험 제출 기록이 있으면 ResponseStatusException(409) 발생 |
| 재조회 실패 500 | 동시성 충돌 후 재조회에서도 못 찾으면 ResponseStatusException(500) 발생 |

## 기존 코드와의 관계
- SubmissionService의 `submitAnswers()` 내 `existsByExamineeIdAndProblemExamId` 재시험 방지 로직은 그대로 유지 → 이중 방어
- ExamineeController의 기존 find-or-create + DataIntegrityViolationException 처리 패턴을 그대로 Service로 이동
- 기존 API 계약(POST /api/examinees/login)은 변경 없음, 409 응답이 새로 추가됨

## 주의사항
- JPA derived query 메서드명이 매우 긴데(`existsByExamineeIdAndProblemExamActiveTrueAndProblemExamDeletedFalse`), 이는 3단계 연관(Submission → Problem → Exam)을 탐색하기 때문. `@Query`로 JPQL을 직접 작성할 수도 있지만, derived query가 컴파일 타임에 검증되는 장점이 있어 유지했다.
- 로그인 시 재시험 검증은 "활성 시험"만 대상: 시험이 비활성화되면 같은 수험자가 다시 로그인 가능
