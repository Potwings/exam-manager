# Code Review Report

## Summary
- **Files reviewed**: 5
- **Issues found**: 4 (Critical: 1, Warning: 2, Info: 1)
- **Verdict**: Changes Requested

## Issues

### [Critical] 에러 메시지가 클라이언트에 노출될 수 있음
- **File**: `backend/src/main/java/com/exammanager/service/ExamineeService.java:49`
- **Pillar**: Security
- **Description**: `DataIntegrityViolationException` 처리 후 재조회 실패 시 `ResponseStatusException`에 `"수험자 조회 실패"` 메시지를 전달하고 있다. 이 메시지 자체는 고정 문자열이므로 큰 문제는 아니나, `HttpStatus.INTERNAL_SERVER_ERROR`(500) 응답은 Spring Boot 기본 에러 핸들러를 통해 `message` 필드에 reason이 포함될 수 있다. 더 중요한 문제는 49행의 `ResponseStatusException`에 내부 구현 상세("수험자 조회 실패")가 reason으로 전달된다는 점이다. CLAUDE.md 컨벤션에 따르면 에러는 고정 메시지만 응답하고 실제 에러는 `log.error()`로 서버 로그에 기록해야 한다.
- **Suggestion**: 500 에러 시 reason에 구현 상세를 넣지 말고, 로그로 기록 후 일반적인 메시지만 반환한다.
```java
} catch (DataIntegrityViolationException e) {
    log.warn("수험자 동시 로그인 충돌 후 재조회: name={}", name);
    return examineeRepository
            .findByNameAndBirthDate(name, birthDate)
            .orElseThrow(() -> {
                log.error("동시성 충돌 후 수험자 재조회 실패: name={}", name);
                return new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR);
            });
}
```

### [Warning] 로그인 시 재시험 검증과 제출 시 재시험 검증의 범위 불일치
- **File**: `backend/src/main/java/com/exammanager/service/ExamineeService.java:32`
- **Pillar**: Convention (설계 일관성)
- **Description**: 로그인 단계에서는 `existsByExamineeIdAndActiveExam()`으로 **현재 활성 시험**에 대한 제출 여부를 검증하고, 제출 단계(`SubmissionService:48`)에서는 `existsByExamineeIdAndProblemExamId()`로 **특정 시험 ID**에 대한 제출 여부를 검증한다. 두 검증의 범위가 다르다.
  - 로그인 검증: 활성 시험이 바뀌면 이전 시험 제출 기록이 무시됨 (의도된 동작일 수 있음)
  - 제출 검증: 특정 시험 ID 기반이므로 활성 여부와 무관하게 방어

  이 차이가 의도된 것인지 확인 필요. 활성 시험이 A에서 B로 변경된 직후 수험자가 로그인하면, A 시험에 대한 제출 기록이 있어도 로그인은 통과하지만 B 시험 URL로 이동하게 되므로 문제없을 수 있다. 다만, **활성 시험이 변경되지 않은 상태에서 deleted=false 조건**이 JPQL에 포함되어 있으므로, 삭제된 시험의 제출 기록은 무시된다는 점도 확인이 필요하다.
- **Suggestion**: 의도된 설계라면 JPQL 쿼리에 대한 주석으로 "활성 시험 기준 검증" 의도를 명시하면 유지보수에 도움이 된다. `SubmissionRepository`의 쿼리 메서드에 JavaDoc 추가를 권장한다.

### [Warning] `@Transactional` 범위 내에서 조회 전용 경로 존재
- **File**: `backend/src/main/java/com/exammanager/service/ExamineeService.java:25`
- **Pillar**: Performance
- **Description**: `loginOrCreate()` 메서드 전체가 `@Transactional`로 감싸져 있다. 기존 수험자가 로그인하는 경우(29~36행) 조회만 수행하므로 DB 커넥션을 write 모드로 점유할 필요가 없다. 다만 신규 수험자 생성 경로에서는 write가 필요하므로 메서드 분리가 번거로울 수 있다. 현재 구조에서 성능 임팩트는 미미하지만, 트래픽이 높아지면 read-only 분리가 도움이 된다.
- **Suggestion**: 현재 수준에서는 허용 가능. 추후 트래픽 증가 시 조회 전용 경로를 별도 `@Transactional(readOnly = true)` 메서드로 분리하는 것을 고려한다.

### [Info] 테스트에서 409 응답의 reason 메시지 검증 누락
- **File**: `backend/src/test/java/com/exammanager/service/ExamineeServiceTest.java:116-122`
- **Pillar**: Convention (테스트 품질)
- **Description**: `loginOrCreate_existingExamineeWithSubmission_throwsConflict` 테스트에서 HTTP 상태 코드(409)만 검증하고 있다. reason 메시지("이미 응시 완료한 시험입니다")까지 검증하면 의도치 않은 메시지 변경을 조기에 감지할 수 있다.
- **Suggestion**:
```java
assertThat(rse.getReason()).isEqualTo("이미 응시 완료한 시험입니다");
```

## Positive Observations
- **서비스 계층 분리**: Controller에서 Repository 직접 호출을 Service로 분리한 것은 컨벤션(Controller -> Service -> Repository)에 부합하는 좋은 리팩토링이다.
- **동시성 방어**: `DataIntegrityViolationException` 처리로 find-or-create 레이스 컨디션을 방어하는 패턴이 CLAUDE.md의 동시성 제어 가이드라인과 일치한다.
- **테스트 커버리지**: 정상 경로(기존/신규), 동시성 충돌, 재시험 방지, 재조회 실패 등 5가지 시나리오를 모두 커버하고 있다. 특히 `verify(submissionRepository, never()).existsByExamineeIdAndActiveExam(anyLong())`로 신규 수험자에게 불필요한 검증이 호출되지 않음을 확인한 점이 좋다.
- **프론트엔드 UX**: 409 에러를 인라인 메시지로 표시하고, `watch([name, birthDate])`로 입력 변경 시 자동 초기화하는 패턴이 깔끔하다. 서버 에러 메시지를 그대로 노출하지 않고 프론트엔드에서 고정 메시지를 사용하는 점도 보안 컨벤션에 부합한다.
- **SecurityConfig 일관성**: `/api/examinees/**`가 이미 `permitAll()`로 설정되어 있어, 변경 없이 기존 보안 규칙이 유지된다.
