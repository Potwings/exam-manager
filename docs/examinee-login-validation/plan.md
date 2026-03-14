# ExamineeService 분리 및 로그인 시 재시험 방지

## 개요
- **목적**: ExamineeController의 Repository 직접 호출 패턴을 Service 계층으로 분리하여 프로젝트 아키텍처 일관성을 확보하고, 로그인 시점에서 재시험 방지 검증을 추가하여 이미 응시한 수험자가 문제를 열람하는 보안 문제를 차단한다.
- **대상 사용자**: 수험자 (로그인 플로우) / 관리자 (간접적으로 보안 강화 수혜)
- **우선순위**: P1 (중요) -- 아키텍처 일관성 + 보안 문제 해결

## 현재 상태

### 관련 기존 기능 분석

**1) ExamineeController (코드에서 확인)**
- `ExamineeRepository`를 직접 주입받아 `findByNameAndBirthDate()` + `save()` 로직을 Controller에서 직접 수행
- `DataIntegrityViolationException` 동시성 처리도 Controller에 구현되어 있음
- 프로젝트의 다른 모든 Controller는 Service 계층을 거침 (ExamController->ExamService, SubmissionController->SubmissionService 등)

**2) 재시험 방지 현황**
- 현재 `SubmissionService.submitAnswers()` 시작 시에만 `existsByExamineeIdAndProblemExamId()` 검증 (제출 시점)
- 로그인 시점에는 검증 없음 -> 이미 응시한 수험자가 다시 로그인하면 `ExamTake.vue`에서 문제를 열람 가능
- 수험자 로그인 API(`POST /api/examinees/login`)는 `ExamineeResponse`만 반환하며 시험 제출 이력을 확인하지 않음

**3) 관련 Repository 메서드**
- `ExamineeRepository.findByNameAndBirthDate(name, birthDate)` -- find-or-create에 사용
- `SubmissionRepository.existsByExamineeIdAndProblemExamId(examineeId, examId)` -- 제출 이력 확인 (이미 존재)
- `ExamRepository.findByActiveTrueAndDeletedFalse()` -- 활성 시험 조회 (이미 존재)

### 현재의 한계점
1. **아키텍처 불일관**: ExamineeController만 Repository를 직접 사용하여 Controller->Service->Repository 계층 구조를 위반
2. **보안 허점**: 응시 완료 수험자가 재로그인으로 문제 열람 가능 (답안 재제출은 차단되지만 문제 유출 위험)
3. **테스트 어려움**: 비즈니스 로직이 Controller에 있어 단위 테스트 시 Spring MVC 전체를 올려야 함

## 기능 요구사항

### 필수 (Must-have)
1. **ExamineeService 생성**: find-or-create + 동시성 처리 로직을 Service 계층으로 이동
2. **로그인 시 재시험 방지**: 활성 시험(active exam)에 대해 이미 제출 기록이 있으면 `409 CONFLICT` 반환
3. **ExamineeController 리팩토링**: Repository 직접 의존 제거, Service에 위임
4. **프론트엔드 409 처리**: ExamLogin.vue에서 409 응답 시 사용자 친화적 에러 메시지 표시 (기존 `alert()` 대신 인라인 에러)
5. **기존 제출 시점 검증 유지**: SubmissionService의 재시험 방지 로직은 그대로 유지 (이중 방어)

### 선택 (Nice-to-have)
- 로그인 시 응시 완료 정보를 응답에 포함하여 프론트엔드에서 상태 기반 UI 분기 (현 기획에서는 409로 단순 차단)

## UX 설계

### 사용자 플로우

#### 플로우 A: 정상 로그인 (변경 없음)
1. 수험자가 `/exam/login`에서 이름 + 생년월일 입력
2. [시험 시작] 버튼 클릭
3. `POST /api/examinees/login` 호출 -> 200 OK + ExamineeResponse 반환
4. `/exam/take/{examId}`로 이동

#### 플로우 B: 재시험 방지 (신규)
1. 수험자가 `/exam/login`에서 이름 + 생년월일 입력
2. [시험 시작] 버튼 클릭
3. `POST /api/examinees/login` 호출 -> **409 CONFLICT** 반환
4. 로그인 폼 하단에 에러 메시지 표시: "이미 응시 완료한 시험입니다."
5. 페이지 이동 없음, 에러 메시지가 지속 표시됨
6. 다른 정보로 재입력 시 에러 메시지 자동 해제

### 화면 구성

#### ExamLogin.vue 변경점
- 기존 `alert()` 기반 에러 처리를 인라인 에러 메시지로 교체
- 에러 메시지 위치: [시험 시작] 버튼 위 (CardFooter 내)
- 스타일: `text-sm text-destructive text-center` (기존 폼 검증 에러와 동일 패턴)
- 409 응답 시: "이미 응시 완료한 시험입니다." 고정 메시지
- 기타 에러: "로그인에 실패했습니다. 다시 시도해주세요." 고정 메시지
- 입력 변경 시 에러 메시지 초기화 (`watch([name, birthDate])`)

### 기존 UI 패턴과의 일관성
- 에러 메시지 스타일: `text-destructive` (기존 AdminLogin.vue와 유사)
- 서버 에러 메시지를 클라이언트에 직접 노출하지 않음 (CLAUDE.md 에러 핸들러 보안 규칙 준수)
- 고정 메시지만 사용하여 서버 내부 상태 노출 방지

## 기술 구현 방안

### Backend

#### 1) ExamineeService 생성
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamineeService {
    private final ExamineeRepository examineeRepository;
    private final SubmissionRepository submissionRepository;

    @Transactional
    public Examinee loginOrCreate(String name, LocalDate birthDate) {
        Optional<Examinee> existing = examineeRepository.findByNameAndBirthDate(name, birthDate);

        if (existing.isPresent()) {
            // 기존 수험자 → 활성 시험에 대한 재시험 방지 검증
            Examinee examinee = existing.get();
            if (submissionRepository.existsByExamineeIdAndProblemExamActiveTrueAndProblemExamDeletedFalse(
                    examinee.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "이미 응시 완료한 시험입니다");
            }
            return examinee;
        }

        // 신규 수험자 → 제출 기록이 있을 수 없으므로 검증 불필요, 바로 생성
        try {
            return examineeRepository.save(
                Examinee.builder().name(name).birthDate(birthDate).build());
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 중복 insert 실패 시, 이미 생성된 레코드를 재조회
            log.warn("수험자 동시 로그인 충돌 후 재조회: name={}", name);
            return examineeRepository
                .findByNameAndBirthDate(name, birthDate)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "수험자 조회 실패"));
        }
    }
}
```

**설계 결정 사항:**
- **기존 수험자 / 신규 수험자 분기**: `Optional.isPresent()`로 분기하여, 기존 수험자만 재시험 검증 수행. 신규 수험자는 제출 기록이 있을 수 없으므로 검증 스킵 후 바로 생성
- **단일 쿼리 재시험 검증**: `existsByExamineeIdAndProblemExamActiveTrueAndProblemExamDeletedFalse()` — JPA derived query로 Submission → Problem → Exam 조인을 한 번에 수행. 활성 시험 조회 + 제출 기록 확인을 별도 쿼리로 분리하지 않음
- **ExamRepository 의존 불필요**: 단일 쿼리로 활성 시험 조건을 포함하므로 ExamineeService는 `ExamineeRepository` + `SubmissionRepository`만 주입
- **쿼리 최적화**: 신규 수험자 2회(find + create), 기존 수험자 2회(find + 제출확인). 원안 대비 1~2회 절감
- `@Transactional` 적용: find-then-write 패턴 + 동시성 처리를 위해 트랜잭션 경계 필요
- **SubmissionRepository에 신규 메서드 1개 추가**: `existsByExamineeIdAndProblemExamActiveTrueAndProblemExamDeletedFalse(Long examineeId)`

#### 2) ExamineeController 리팩토링
```java
@Slf4j
@RestController
@RequestMapping("/api/examinees")
@RequiredArgsConstructor
public class ExamineeController {
    private final ExamineeService examineeService;  // Repository -> Service로 변경

    @PostMapping("/login")
    public ResponseEntity<ExamineeResponse> login(
            @Valid @RequestBody ExamineeLoginRequest request) {
        Examinee examinee = examineeService.loginOrCreate(
            request.getName(), request.getBirthDate());
        return ResponseEntity.ok(ExamineeResponse.from(examinee));
    }
}
```

**변경 사항:**
- `ExamineeRepository` 의존 제거 -> `ExamineeService` 의존으로 교체
- find-or-create + 동시성 처리 + 재시험 검증 로직 모두 Service로 위임
- Controller는 DTO 변환 + HTTP 응답만 담당

#### 3) SecurityConfig 변경 불필요
- `/api/examinees/**`는 이미 `permitAll()` 설정 (line 47)
- 기존 로그인 엔드포인트 경로 변경 없음

### Frontend

#### 1) ExamLogin.vue 변경
- 기존 `alert()` 호출을 인라인 에러 메시지(`loginError` ref)로 교체
- 409 응답 분기 처리 추가
- 입력 변경 시 에러 메시지 자동 초기화

```javascript
const loginError = ref('')

// 입력 변경 시 서버 에러 초기화
watch([name, birthDate], () => {
  loginError.value = ''
})

async function handleLogin() {
  // ... 기존 검증 로직 ...
  loginLoading.value = true
  loginError.value = ''
  try {
    // ... 기존 로그인 로직 ...
  } catch (e) {
    if (e.response?.status === 409) {
      loginError.value = '이미 응시 완료한 시험입니다.'
    } else {
      loginError.value = '로그인에 실패했습니다. 다시 시도해주세요.'
    }
  } finally {
    loginLoading.value = false
  }
}
```

```html
<!-- CardFooter 내, Button 위에 배치 -->
<p v-if="loginError" class="text-sm text-destructive text-center">
  {{ loginError }}
</p>
```

### 고려사항

#### 기존 기능과의 호환성
- **API 경로 변경 없음**: `POST /api/examinees/login` 동일
- **정상 응답 형식 변경 없음**: `ExamineeResponse` (id, name, birthDate) 그대로
- **SubmissionService 이중 방어 유지**: 제출 시점의 `existsByExamineeIdAndProblemExamId()` 검증은 그대로 유지. 로그인 검증 우회 시나리오(API 직접 호출 등)에 대한 안전장치
- **활성 시험 없을 때**: 재시험 검증 스킵 -> 기존처럼 로그인만 수행 (프론트엔드에서 활성 시험 없으면 버튼 비활성화이므로 실질적 영향 없음)

#### 보안
- 서버 에러 메시지를 클라이언트에 직접 노출하지 않음 (프론트엔드에서 고정 메시지 사용)
- `/api/examinees/**` permitAll 규칙 유지 (수험자 로그인은 인증 불필요)
- 409 응답의 `message` 필드는 Spring의 `ResponseStatusException` 기본 동작으로 반환되나, 프론트엔드에서 무시하고 고정 메시지 사용

#### 성능 영향
- 기존 수험자: `findByNameAndBirthDate()` + `existsByExamineeIdAndProblemExam...()` = 쿼리 2회
- 신규 수험자: `findByNameAndBirthDate()` + `save()` = 쿼리 2회 (재시험 검증 스킵)
- 두 쿼리 모두 인덱스 기반 단건 조회이므로 성능 영향 미미

#### 에러 처리
- `ResponseStatusException(CONFLICT)` 사용 -> Spring 기본 에러 응답 형식으로 반환
- `ResponseStatusException(INTERNAL_SERVER_ERROR)` -- 동시성 재조회 실패 시 (기존 로직 유지)

#### 동시성
- 기존 `DataIntegrityViolationException` catch + 재조회 패턴 그대로 Service로 이동
- Examinee 테이블의 `UNIQUE(name, birth_date)` 제약 조건이 동시 insert를 방어

## API 계약 (Backend <-> Frontend)

### 기존 API 변경사항

| Method | Path | Request Body | Response Body | 변경 내용 |
|--------|------|-------------|---------------|-----------|
| POST | `/api/examinees/login` | `{"name": "홍길동", "birthDate": "2001-01-01"}` | 200: `{"id": 1, "name": "홍길동", "birthDate": "2001-01-01"}` | **응답 형식 변경 없음** |
| | | | **409**: 활성 시험에 대해 이미 제출 기록이 있는 경우 | **신규 에러 응답 추가** |

### 에러 응답

| 상황 | Status | 프론트엔드 표시 메시지 |
|------|--------|----------------------|
| 활성 시험에 대해 이미 응시 완료 | 409 CONFLICT | "이미 응시 완료한 시험입니다." |
| 동시 로그인 충돌 후 재조회 실패 | 500 INTERNAL_SERVER_ERROR | "로그인에 실패했습니다. 다시 시도해주세요." |
| 유효성 검증 실패 (name/birthDate 누락) | 400 BAD_REQUEST | "로그인에 실패했습니다. 다시 시도해주세요." |

### 신규 API 없음
- 기존 `POST /api/examinees/login` 엔드포인트에 재시험 방지 로직만 추가
- Request/Response DTO 변경 없음

## 작업 분해 (Task Breakdown)

1. `[Backend]` **ExamineeService 생성** -- `com.exammanager.service.ExamineeService` 클래스 생성. 기존/신규 수험자 분기 + 기존 수험자 재시험 검증 + 신규 수험자 생성(동시성 처리) 로직 구현. `ExamineeRepository`, `SubmissionRepository` 주입. SubmissionRepository에 `existsByExamineeIdAndProblemExamActiveTrueAndProblemExamDeletedFalse()` 메서드 추가.
2. `[Backend]` **ExamineeController 리팩토링** -- `ExamineeRepository` 의존 제거, `ExamineeService` 주입으로 교체. `login()` 메서드를 Service 위임으로 단순화. (🔗 1 완료 후)
3. `[Backend]` **단위 테스트 작성** -- ExamineeService 테스트: 정상 로그인, 신규 생성, 동시성 충돌 재조회, 활성 시험 재시험 방지(409), 활성 시험 없을 때 검증 스킵. (🔗 1 완료 후)
4. `[Frontend]` **ExamLogin.vue 에러 처리 개선** -- `alert()` 제거, `loginError` ref 추가, 409 분기 처리, 인라인 에러 메시지 표시, 입력 변경 시 자동 초기화. (⚡ 1, 2와 병렬 가능 -- API 경로/응답 형식 변경 없으므로 프론트엔드 독립 작업)

### 의존성 그래프
```
1 (ExamineeService) ──> 2 (Controller 리팩토링) ──> 3 (단위 테스트)

4 (Frontend ExamLogin) ──────── ⚡ 1, 2와 병렬 가능
```
- 작업 1 -> 2는 순차 (Controller가 Service에 의존)
- 작업 3은 작업 1 완료 후 (Service 테스트)
- 작업 4는 백엔드와 **완전 병렬 가능** (API 계약 변경 없음, 409 응답 코드만 추가)

## QA 수용 기준 (Acceptance Criteria)

### 정상 로그인 (기존 기능 회귀 없음)
- [ ] 활성 시험이 있고 미응시 수험자가 이름+생년월일 입력 후 [시험 시작] 클릭 시 정상 로그인되어 `/exam/take/{examId}`로 이동한다
- [ ] 신규 수험자(이름+생년월일 조합이 DB에 없음)가 로그인 시 Examinee 레코드가 생성되고 정상 로그인된다
- [ ] 활성 시험이 없을 때 [시험 시작] 버튼이 비활성화 상태이며 클릭할 수 없다

### 재시험 방지 (로그인 시점)
- [ ] 활성 시험에 대해 이미 제출 기록이 있는 수험자가 로그인 시도하면 페이지 이동 없이 "이미 응시 완료한 시험입니다." 에러 메시지가 표시된다
- [ ] 에러 메시지가 [시험 시작] 버튼 위에 빨간색(text-destructive)으로 표시된다
- [ ] 에러 표시 후 이름 또는 생년월일 입력을 변경하면 에러 메시지가 자동으로 사라진다
- [ ] 에러 표시 후 다른 수험자 정보(미응시자)로 입력 변경 후 [시험 시작] 클릭 시 정상 로그인된다

### 이중 방어 (제출 시점 검증 유지)
- [ ] 로그인 검증을 우회(API 직접 호출)하여 이미 응시한 시험에 답안 제출 시도하면 409 CONFLICT가 반환된다

### 에러 처리
- [ ] 서버 연결 실패 등 네트워크 에러 시 "로그인에 실패했습니다. 다시 시도해주세요." 메시지가 표시된다 (alert가 아닌 인라인 메시지)
- [ ] 서버 에러 메시지(ResponseStatusException의 reason)가 사용자에게 직접 노출되지 않는다

### 아키텍처 검증
- [ ] ExamineeController에 `ExamineeRepository` import가 없다 (Service만 의존)
- [ ] ExamineeService가 `@Service` 애노테이션을 가지며, `@Transactional` 이 적용되어 있다

## 리스크 및 의존성

### 리스크
1. **시험 전환 시 엣지 케이스**: 수험자가 시험A를 응시 완료 후, 관리자가 시험B를 활성화하면 해당 수험자는 시험B에 정상 로그인 가능. 이는 의도된 동작 (활성 시험 기준 검증).
2. **활성 시험 없는 상태에서 로그인**: 프론트엔드에서 버튼이 비활성화되어 있으므로 API 호출 자체가 발생하지 않음. 다만 API 직접 호출 시 재시험 검증 없이 로그인됨 (활성 시험이 없으므로 실질적 보안 위험 없음).

### 의존성
- `SubmissionRepository`에 `existsByExamineeIdAndProblemExamActiveTrueAndProblemExamDeletedFalse()` 신규 메서드 추가 필요
- 기존 `ExamineeRepository` 메서드 변경 없음
- `ExamRepository` 의존 불필요 (단일 쿼리로 활성 시험 조건 포함)
- 기존 프론트엔드 API 함수 (`loginExaminee`) 변경 없음