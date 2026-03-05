# 응시 현황 모니터링 (Exam Monitor)

## 개요
- **목적**: 관리자가 현재 활성 시험의 응시 현황을 실시간으로 모니터링하는 전용 페이지. 각 수험자의 응시 상태(응시 중/제출 완료), 남은 시간, 진입 시각 등을 한눈에 파악하여 시험 진행 상황을 관리한다.
- **대상 사용자**: 관리자 전용
- **우선순위**: P1 (중요) -- 시험 진행 중 관리자가 응시 현황을 실시간으로 확인할 수단이 현재 없음. 시험 감독 역할의 핵심 기능.

## 현재 상태

### 관련 기존 기능
1. **ExamSession 엔티티**: `examinee` + `exam` + `startedAt` (서버 시간 기준), `UNIQUE(examinee_id, exam_id)` -- 수험자가 시험에 진입하면 세션 레코드가 생성됨
2. **Submission 엔티티**: `examinee` + `problem` + `submittedAt` -- 제출 기록 존재 여부로 완료 판별 가능 (`existsByExamineeIdAndProblemExamId`)
3. **Exam.timeLimit**: nullable Integer (분 단위) -- null이면 시간 제한 없음
4. **ExamSessionController.calculateRemainingSeconds()**: `startedAt + timeLimit - now` 계산 로직 이미 존재
5. **ScoreBoard.vue**: 시험 선택 드롭다운 + Table 기반 수험자 목록 + 5초 폴링 패턴 -- 동일한 UI 패턴 재활용 가능
6. **SSE 알림 시스템**: `NotificationService` + `SseEmitter` -- 채점 완료/관리자 호출 이벤트 이미 구현됨
7. **SecurityConfig**: `/api/exam-sessions/**`는 permitAll, `/api/scores/**`는 authenticated

### 현재의 한계점
- 관리자가 시험 진행 중 누가 응시 중인지, 누가 제출을 완료했는지 확인할 수 없음
- 시간 제한 시험에서 수험자별 남은 시간을 관리자가 확인할 수 없음
- 관리자 호출(admin-call) 알림이 오더라도 해당 수험자가 어떤 상태인지(남은 시간 등) 즉시 파악 불가
- ExamSession 데이터가 존재하지만 관리자용 조회 API가 없어 활용되지 않고 있음

## 기능 요구사항

### 필수 (Must-have)
1. **시험별 응시 현황 조회**: 특정 시험의 모든 ExamSession을 조회하여 수험자 목록 표시
2. **상태 구분**: 각 수험자별 "응시 중" / "제출 완료" 상태를 Badge로 표시
3. **남은 시간 표시**: 시간 제한 시험의 경우 각 수험자의 남은 시간을 서버에서 계산하여 전달, 프론트엔드에서 카운트다운
4. **자동 갱신**: 10초 간격 폴링으로 새 수험자 진입, 제출 완료 등 변경사항 자동 반영
5. **헤더 네비게이션 추가**: "응시현황" 탭을 기존 네비게이션에 추가
6. **활성 시험 기본 선택**: 페이지 로드 시 활성(active) 시험을 자동 선택

### 선택 (Nice-to-have)
- 시간 초과 수험자 강조 표시 (시간 만료 후 미제출 상태)
- SSE 이벤트로 실시간 갱신 (신규 세션 생성, 제출 완료 시 push) -- 현재는 폴링으로 MVP 구현, 추후 SSE 확장 가능

## UX 설계

### 사용자 플로우

#### 플로우 A: 활성 시험 응시 현황 확인
1. 관리자가 헤더의 "응시현황" 탭 클릭 -> `/admin/monitor` 페이지로 이동
2. 활성 시험이 있으면 자동 선택되어 응시 현황 테이블 표시
3. 수험자가 시험에 진입하면 10초 이내에 목록에 새 행 추가
4. 수험자가 답안을 제출하면 10초 이내에 상태가 "응시 중" -> "제출 완료"로 변경

#### 플로우 B: 과거 시험 응시 현황 확인
1. 관리자가 상단 시험 선택 드롭다운에서 다른 시험 선택
2. 해당 시험의 모든 ExamSession 기록 조회 -- 전원 "제출 완료" 상태

#### 플로우 C: 시간 제한 시험 모니터링
1. 시간 제한 시험의 응시 현황 확인
2. 각 수험자별 남은 시간이 "MM:SS" 형식으로 표시, 매초 클라이언트에서 카운트다운
3. 5분 이하: amber 색상, 1분 이하: red 색상 + 깜빡임 (기존 ExamTake.vue 타이머 패턴 동일)
4. 시간 만료: "시간 만료" Badge 표시 (red)

### 화면 구성

#### ExamMonitor 페이지 레이아웃

```
+------------------------------------------------------------------+
| 응시 현황                                                          |
| 현재 시험 응시 상태를 모니터링합니다.                                   |
+------------------------------------------------------------------+
| [Card]                                                           |
| ┌──────────────────────────────────────────────────────────────┐ |
| │ [현황]                              [시험 선택 드롭다운 ▼]     │ |
| ├──────────────────────────────────────────────────────────────┤ |
| │ 이름    | 생년월일   | 상태       | 남은 시간 | 시작 시각      │ |
| ├──────────────────────────────────────────────────────────────┤ |
| │ 홍길동  | 1990-01-01 | [응시 중]  | 23:45    | 14:30:15       │ |
| │ 김영희  | 1995-03-15 | [응시 중]  | 18:22    | 14:35:38       │ |
| │ 이철수  | 1988-07-20 | [제출완료] | -        | 14:25:00       │ |
| └──────────────────────────────────────────────────────────────┘ |
| ┌──────────────────────────────────────────────────────────────┐ |
| │ 요약: 총 3명 · 응시 중 2명 · 제출 완료 1명                      │ |
| └──────────────────────────────────────────────────────────────┘ |
+------------------------------------------------------------------+
```

### 컴포넌트 구성
- **Card + CardHeader + CardTitle + CardContent**: ScoreBoard와 동일한 카드 기반 레이아웃
- **Table + TableHeader + TableBody + TableRow + TableHead + TableCell**: 수험자 목록 테이블
- **Badge**: 상태 표시 (응시 중: default, 제출 완료: secondary, 시간 만료: destructive)
- **select**: 시험 선택 드롭다운 (ScoreBoard와 동일한 native select 패턴)
- **lucide-vue-next 아이콘**: `Monitor` (페이지 타이틀), `Clock` (시간 관련), `Users` (요약)

### 기존 UI 패턴과의 일관성 고려사항
- **페이지 헤더**: `text-2xl font-bold tracking-tight` 제목 + `text-muted-foreground` 설명 (ScoreBoard 패턴)
- **시험 선택 드롭다운**: ScoreBoard와 동일한 `<select>` + 스타일 클래스 재사용
- **테이블 레이아웃**: Card > Table 패턴 (ExamManage, ScoreBoard와 동일)
- **상태 Badge**: 기존 시스템의 Badge variant 패턴 유지 (active/inactive, 채점 중/완료)
- **빈 상태**: `text-center py-6 text-muted-foreground` (기존 패턴)
- **자동 폴링**: ScoreBoard의 `startPollingIfNeeded()` + `stopPolling()` 패턴 재활용

## 기술 구현 방안

### Backend

#### 새 API 엔드포인트
- `GET /api/exam-sessions/monitor?examId={examId}` -- 특정 시험의 응시 현황 조회 (Admin 전용)
- 새 Controller 메서드를 기존 `ExamSessionController`에 추가하지 않음 -- 기존 `ExamSessionController`는 수험자용(permitAll)이므로, 관리자 전용 모니터링 API는 별도 경로 구성 필요

#### SecurityConfig 고려사항
현재 `/api/exam-sessions/**`가 `permitAll`로 설정되어 있어 같은 경로 하위에 Admin 전용 엔드포인트를 추가하면 인증 없이 접근 가능한 보안 문제가 발생한다. 따라서 모니터링 API는 `/api/monitor/**` 경로로 분리하고 SecurityConfig에서 `authenticated`로 보호한다.

#### MonitorController (신규)
```java
@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class MonitorController {
    @GetMapping("/sessions")
    public List<ExamSessionMonitorResponse> getSessions(@RequestParam Long examId) { ... }
}
```

#### ExamSessionMonitorResponse (신규 DTO)
```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamSessionMonitorResponse {
    private Long examineeId;
    private String examineeName;
    private LocalDate examineeBirthDate;
    private String status;          // "IN_PROGRESS" | "SUBMITTED" | "TIME_EXPIRED"
    private Long remainingSeconds;  // null이면 시간 제한 없음
    private LocalDateTime startedAt;
}
```

#### MonitorService (신규)
```java
@Service
@RequiredArgsConstructor
public class MonitorService {
    private final ExamSessionRepository examSessionRepository;
    private final SubmissionRepository submissionRepository;
    private final ExamService examService;

    public List<ExamSessionMonitorResponse> getSessionsByExam(Long examId) {
        Exam exam = examService.findById(examId);
        List<ExamSession> sessions = examSessionRepository.findByExamId(examId);

        return sessions.stream().map(session -> {
            boolean submitted = submissionRepository
                .existsByExamineeIdAndProblemExamId(session.getExaminee().getId(), examId);

            String status;
            Long remainingSeconds = null;

            if (submitted) {
                status = "SUBMITTED";
            } else if (exam.getTimeLimit() != null) {
                long remaining = calculateRemainingSeconds(session, exam);
                remainingSeconds = remaining;
                status = remaining <= 0 ? "TIME_EXPIRED" : "IN_PROGRESS";
            } else {
                status = "IN_PROGRESS";
            }

            return ExamSessionMonitorResponse.builder()
                .examineeId(session.getExaminee().getId())
                .examineeName(session.getExaminee().getName())
                .examineeBirthDate(session.getExaminee().getBirthDate())
                .status(status)
                .remainingSeconds(remainingSeconds)
                .startedAt(session.getStartedAt())
                .build();
        }).sorted(Comparator.comparing(ExamSessionMonitorResponse::getStartedAt).reversed())
          .toList();
    }
}
```

#### Repository 변경
`ExamSessionRepository`에 메서드 추가:
```java
List<ExamSession> findByExamId(Long examId);
```

#### SecurityConfig 변경
```java
// 기존 authenticated 블록에 추가 (순서: /api/exam-sessions/** (permitAll) 아래)
.requestMatchers("/api/monitor/**").authenticated()
```
주의: `/api/monitor/**`는 기존 `anyRequest().permitAll()` 이전에 위치해야 하며, 기존 패턴의 순서를 깨뜨리지 않는 위치에 삽입한다.

### Frontend

#### 새 컴포넌트: `ExamMonitor.vue`
- 위치: `frontend/src/views/admin/ExamMonitor.vue`
- ScoreBoard.vue를 참고하여 동일한 레이아웃 패턴 적용

#### Router 변경 (`router/index.js`)
```javascript
{ path: '/admin/monitor', component: ExamMonitor, meta: { requiresAdmin: true } }
```

#### API 함수 추가 (`api/index.js`)
```javascript
export function fetchMonitorSessions(examId) {
  return api.get('/monitor/sessions', { params: { examId } })
}
```

#### App.vue 헤더 네비게이션 변경
기존 3개 탭(채점결과, 시험관리, 계정관리)에 "응시현황" 탭 추가:
```html
<Button variant="ghost" size="sm" as-child>
  <router-link to="/admin/monitor">응시현황</router-link>
</Button>
```
배치 순서: 채점결과 | **응시현황** | 시험관리 | 계정관리 -- 채점결과 바로 옆에 배치하여 시험 운영 관련 탭끼리 묶음.

#### 카운트다운 로직
- 서버에서 받은 `remainingSeconds`를 기준 시각(`Date.now()`)과 함께 저장
- `setInterval(1000)`으로 매초 클라이언트에서 경과 시간 차감하여 표시
- 폴링(10초)으로 서버 데이터 수신 시 기준 시각 + remainingSeconds 동기화
- 음수 방지: `Math.max(0, ...)`
- 포맷: `MM:SS` (monospace, `tabular-nums` 클래스)

#### 색상 규칙 (기존 ExamTake.vue 타이머 패턴)
- 5분 초과: 기본 텍스트 색상 (`text-foreground`)
- 5분 이하: `text-amber-600`
- 1분 이하: `text-red-600 animate-pulse`
- 시간 만료 (0초): `Badge variant="destructive"` "시간 만료"

#### 요약 섹션
테이블 하단에 응시 현황 요약:
```
총 {N}명 · 응시 중 {N}명 · 제출 완료 {N}명 [· 시간 만료 {N}명]
```
시간 만료 항목은 시간 제한 시험이고 만료자가 있을 때만 표시.

### 고려사항

#### 기존 기능과의 호환성
- `ExamSessionRepository.findByExamId()` 추가는 기존 `findByExamineeIdAndExamId()`에 영향 없음
- `/api/monitor/**` 신규 경로이므로 기존 엔드포인트와 충돌 없음
- 시간 제한 없는 시험도 ExamSession이 생성되지 않는 경우가 있음 (ExamSessionController에서 timeLimit null이면 세션 미생성). 이 경우 해당 수험자는 monitor 목록에 표시되지 않으므로, 시간 제한 없는 시험에서는 "세션 없는 수험자"도 표시해야 함 -> **Submission 기반 보조 조회** 필요

#### 시간 제한 없는 시험의 응시자 표시 (중요)
현재 `ExamSessionController.createSession()`은 `timeLimit == null`이면 ExamSession을 생성하지 않고 바로 `remainingSeconds: null`을 반환한다. 따라서 시간 제한 없는 시험에서는 ExamSession 테이블에 레코드가 없어 모니터링 불가.

**해결 방안**: Submission 데이터를 보조적으로 사용한다.
- ExamSession이 있는 수험자: 세션 기반으로 상태 판별 (응시 중/제출 완료/시간 만료)
- ExamSession이 없지만 Submission이 있는 수험자: "제출 완료" 상태로 표시 (시간 제한 없는 시험에서 제출한 경우)
- 둘 다 없는 수험자: 표시 안 함 (시험에 진입하지 않은 수험자)

즉, 시간 제한 없는 시험에서는 **제출 완료된 수험자만** 목록에 표시된다. 응시 중인 수험자는 ExamSession 레코드가 없어 표시가 불가능한데, 이는 현재 시스템의 구조적 제약이다. 이 제약을 해결하려면 ExamSession 생성 로직을 변경해야 하지만, 기존 수험자 플로우에 영향을 줄 수 있으므로 MVP에서는 현행 유지하고 다음 단계에서 개선한다.

**MVP 이후 개선안**: `ExamSessionController.createSession()`에서 timeLimit 무관하게 항상 ExamSession을 생성하도록 변경. 이렇게 하면 시간 제한 없는 시험에서도 "응시 중" 수험자를 표시할 수 있다. 이 변경은 기존 기능에 부작용이 없다 (ExamSession은 시간 계산 외에 다른 곳에서 참조되지 않으며, 중복 생성은 UNIQUE 제약조건으로 방지됨).

=> **결론: MVP에서 바로 ExamSession 항상 생성으로 변경한다.** 변경 범위가 작고 부작용이 없으며, 이를 하지 않으면 시간 제한 없는 시험의 모니터링이 사실상 불가능하여 기능 가치가 크게 떨어지기 때문이다.

#### ExamSessionController 변경 (MVP 포함)
```java
@PostMapping
public ExamSessionResponse createSession(@Valid @RequestBody ExamSessionRequest request) {
    Exam exam = examService.findById(request.getExamId());
    Examinee examinee = examineeRepository.findById(request.getExamineeId())
            .orElseThrow(() -> ...);

    // 항상 세션 생성 (find-or-create)
    ExamSession session = examSessionRepository.findByExamineeIdAndExamId(
            request.getExamineeId(), request.getExamId()
    ).orElse(null);

    if (session == null) {
        try {
            session = ExamSession.builder()
                    .examinee(examinee)
                    .exam(exam)
                    .build();
            session = examSessionRepository.save(session);
        } catch (DataIntegrityViolationException e) {
            session = examSessionRepository.findByExamineeIdAndExamId(
                    request.getExamineeId(), request.getExamId()
            ).orElseThrow(() -> ...);
        }
    }

    // timeLimit 없으면 remainingSeconds null 반환 (기존 동작 유지)
    if (exam.getTimeLimit() == null) {
        return ExamSessionResponse.builder().remainingSeconds(null).build();
    }

    long remaining = calculateRemainingSeconds(session, exam);
    return ExamSessionResponse.builder().remainingSeconds(remaining).build();
}
```
기존과 차이: `timeLimit == null` 체크를 세션 생성 이후로 이동. 응답은 기존과 동일하므로 수험자 UI에 영향 없음.

#### 보안 (인증/권한)
- `/api/monitor/**` 경로를 SecurityConfig에서 `.authenticated()`로 보호
- 기존 `/api/exam-sessions/**` (permitAll)과 완전히 분리된 경로

#### 성능 영향
- `ExamSession.findByExamId()`: exam_id 인덱스로 효율적 조회 (FK 컬럼이므로 자동 인덱스)
- `existsByExamineeIdAndProblemExamId()`: 기존 메서드 재활용 (EXISTS 쿼리로 효율적)
- 10초 폴링: ScoreBoard의 5초 폴링보다 느슨하여 서버 부하 낮음
- 세션 수: 시험당 수십~수백 명 수준이므로 성능 문제 없음

#### 에러 처리
- 존재하지 않는 examId 요청 시 404 반환 (`examService.findById()`)
- 세션이 없는 시험: 빈 배열 반환 + 프론트엔드에서 "응시자가 없습니다" 표시

## API 계약 (Backend <-> Frontend)

| Method | Path | Request | Response | 비고 |
|--------|------|---------|----------|------|
| GET | `/api/monitor/sessions?examId={examId}` | Query: examId (Long, 필수) | `ExamSessionMonitorResponse[]` | Admin 전용. 시험의 전체 세션 목록 + 상태 + 남은 시간 |

### ExamSessionMonitorResponse 상세

```json
[
  {
    "examineeId": 1,
    "examineeName": "홍길동",
    "examineeBirthDate": "1990-01-01",
    "status": "IN_PROGRESS",
    "remainingSeconds": 1425,
    "startedAt": "2026-03-04T14:30:15"
  },
  {
    "examineeId": 2,
    "examineeName": "김영희",
    "examineeBirthDate": "1995-03-15",
    "status": "SUBMITTED",
    "remainingSeconds": null,
    "startedAt": "2026-03-04T14:25:00"
  },
  {
    "examineeId": 3,
    "examineeName": "이철수",
    "examineeBirthDate": "1988-07-20",
    "status": "TIME_EXPIRED",
    "remainingSeconds": 0,
    "startedAt": "2026-03-04T13:50:00"
  }
]
```

### status 값 정의
| 값 | 의미 | 조건 |
|----|------|------|
| `IN_PROGRESS` | 응시 중 | ExamSession 존재 + Submission 미존재 + (시간 미만료 또는 시간 제한 없음) |
| `SUBMITTED` | 제출 완료 | Submission 존재 |
| `TIME_EXPIRED` | 시간 만료 | ExamSession 존재 + Submission 미존재 + 시간 제한 있음 + remainingSeconds <= 0 |

### remainingSeconds 규칙
| 시험 유형 | 상태 | remainingSeconds |
|-----------|------|-----------------|
| 시간 제한 있음 | 응시 중 | 양수 (서버 계산값) |
| 시간 제한 있음 | 시간 만료 | 0 |
| 시간 제한 있음 | 제출 완료 | null |
| 시간 제한 없음 | 응시 중 | null |
| 시간 제한 없음 | 제출 완료 | null |

## 작업 분해 (Task Breakdown)

### Phase 1: Backend 기반 작업

1. `[Backend]` **ExamSessionRepository에 findByExamId 메서드 추가** -- `List<ExamSession> findByExamId(Long examId)` 쿼리 메서드 추가. JPA 네이밍 컨벤션으로 자동 구현.

2. `[Backend]` **ExamSessionMonitorResponse DTO 생성** -- `dto/ExamSessionMonitorResponse.java` 신규 파일. Lombok `@Builder` 패턴. 필드: examineeId, examineeName, examineeBirthDate, status(String), remainingSeconds(Long), startedAt(LocalDateTime).

3. `[Backend]` **MonitorService 생성** -- `service/MonitorService.java` 신규 파일. `getSessionsByExam(Long examId)` 메서드 구현. ExamSession 조회 + Submission 존재 여부 판별 + 남은 시간 계산 + 상태 결정 로직.

4. `[Backend]` **MonitorController 생성** -- `controller/MonitorController.java` 신규 파일. `GET /api/monitor/sessions` 엔드포인트. `@RequestParam Long examId` 파라미터.

5. `[Backend]` **SecurityConfig에 /api/monitor/** 경로 보호 추가** -- `.requestMatchers("/api/monitor/**").authenticated()` 를 기존 authenticated 블록에 추가. `/api/scores/**` 라인 바로 아래에 배치.

6. `[Backend]` **ExamSessionController.createSession() 수정** -- timeLimit null 체크를 세션 생성 이후로 이동. 세션은 항상 생성하되, 응답의 remainingSeconds는 기존 로직 유지. 기존 수험자 UI 동작에 영향 없음.

7. `[Backend]` **단위 테스트 작성** -- MonitorService의 상태 판별 로직 테스트 (응시 중, 제출 완료, 시간 만료 3가지 케이스 + 시간 제한 없는 시험 케이스).

### Phase 2: Frontend 작업 (Phase 1의 1~6 완료 후)

8. `[Frontend]` **API 함수 추가** -- `api/index.js`에 `fetchMonitorSessions(examId)` 함수 추가. (Phase 1과 병렬 가능 -- mock 응답으로 개발 가능하나, API 계약이 확정되었으므로 Phase 1 완료 후 통합이 안전)

9. `[Frontend]` **ExamMonitor.vue 생성** -- `views/admin/ExamMonitor.vue` 신규 파일. ScoreBoard.vue 구조를 기반으로 시험 선택 + 응시 현황 테이블 + 카운트다운 + 요약 섹션 구현.

10. `[Frontend]` **Router에 /admin/monitor 경로 추가** -- `router/index.js`에 ExamMonitor 컴포넌트 import + 라우트 추가. `meta: { requiresAdmin: true }`.

11. `[Frontend]` **App.vue 헤더 네비게이션에 "응시현황" 탭 추가** -- 채점결과와 시험관리 사이에 배치.

### 의존성 정리
- 1~6: 순차적 (Repository -> DTO -> Service -> Controller -> Security -> Session 수정)
- 7: 1~6 완료 후
- 8: 독립적 (API 함수만 추가)
- 9: 8 완료 후 (API 함수 사용)
- 10, 11: 9와 병렬 가능하나 순차 구현이 자연스러움
- **8~11은 1~6 완료 후 진행** (백엔드 API 필요)

## QA 수용 기준 (Acceptance Criteria)

### 페이지 접근 및 인증
- [ ] `/admin/monitor` 페이지에 관리자 로그인 없이 접근 시 `/admin/login`으로 리다이렉트
- [ ] 관리자 로그인 후 `/admin/monitor` 페이지에 정상 접근 가능
- [ ] 헤더 네비게이션에 "응시현황" 탭이 표시됨 (로그인 + initLogin=false 상태)
- [ ] initLogin=true 상태에서는 "응시현황" 탭이 숨겨짐

### 시험 선택 및 데이터 로드
- [ ] 페이지 로드 시 시험 목록이 드롭다운에 표시됨
- [ ] 활성(active) 시험이 있으면 자동 선택됨
- [ ] 활성 시험이 없으면 첫 번째 시험이 선택됨
- [ ] 시험 변경 시 해당 시험의 응시 현황으로 데이터가 갱신됨
- [ ] 응시자가 없는 시험 선택 시 "응시자가 없습니다" 메시지 표시

### 응시 상태 표시
- [ ] ExamSession이 존재하고 Submission이 없는 수험자: "응시 중" Badge 표시 (default variant)
- [ ] Submission이 존재하는 수험자: "제출 완료" Badge 표시 (secondary variant)
- [ ] 시간 제한 시험에서 시간 만료 + 미제출 수험자: "시간 만료" Badge 표시 (destructive variant)
- [ ] 각 수험자의 이름, 생년월일, 시작 시각이 정확히 표시됨

### 남은 시간 표시 (시간 제한 시험)
- [ ] 시간 제한 있는 시험의 응시 중 수험자에게 남은 시간이 MM:SS 형식으로 표시됨
- [ ] 남은 시간이 매초 감소함 (클라이언트 카운트다운)
- [ ] 5분 이하: amber 색상으로 변경됨
- [ ] 1분 이하: red 색상 + 깜빡임(animate-pulse)
- [ ] 시간 제한 없는 시험: 남은 시간 열에 "-" 표시
- [ ] 제출 완료 수험자: 남은 시간 열에 "-" 표시

### 자동 갱신 (폴링)
- [ ] 10초 간격으로 서버 데이터 자동 갱신
- [ ] 새 수험자 진입 시 10초 이내에 목록에 추가됨
- [ ] 수험자 제출 완료 시 10초 이내에 상태가 변경됨
- [ ] 폴링 시 카운트다운이 서버 데이터와 동기화됨 (드리프트 보정)
- [ ] 페이지 이탈(unmounted) 시 폴링 타이머와 카운트다운 타이머가 정리됨

### 요약 섹션
- [ ] 테이블 하단에 "총 N명 / 응시 중 N명 / 제출 완료 N명" 요약 표시
- [ ] 시간 만료자가 있으면 "시간 만료 N명" 추가 표시

### ExamSession 항상 생성 변경
- [ ] 시간 제한 없는 시험에서도 수험자 로그인 후 ExamSession 레코드가 생성됨
- [ ] 시간 제한 없는 시험에서 수험자의 응답(remainingSeconds)이 기존과 동일하게 null
- [ ] 시간 제한 없는 시험에서 응시 중 수험자가 모니터 페이지에 표시됨
- [ ] 기존 시간 제한 시험의 수험자 플로우에 변경 없음 (타이머 정상 동작)

### 보안
- [ ] `GET /api/monitor/sessions` 엔드포인트에 미인증 접근 시 401 반환
- [ ] 관리자 인증 후 정상 응답 반환

### 엣지 케이스
- [ ] 시험이 하나도 없을 때 빈 드롭다운 + 안내 메시지 표시
- [ ] 동일 수험자가 같은 시험에 중복 세션을 생성할 수 없음 (UNIQUE 제약조건)
- [ ] 삭제된 시험(deleted=true)은 시험 목록에 표시되지 않음

## 리스크 및 의존성

### 리스크
1. **ExamSession 항상 생성 변경**: 기존 수험자 플로우에 영향을 줄 수 있음. 변경 범위는 작지만 기존 테스트 및 수동 검증 필요. -> **완화**: 응답 형식이 동일하므로 수험자 UI에 영향 없음. ExamTake.vue는 remainingSeconds null이면 타이머 미표시 (기존 동작 유지).
2. **폴링 주기**: 10초가 실시간 모니터링에 충분한지. -> **완화**: 시험 감독 용도로 10초는 적절함. 더 빠른 갱신이 필요하면 추후 SSE 확장.
3. **카운트다운 드리프트**: 클라이언트 시계와 서버 시계 차이로 남은 시간 오차 발생 가능. -> **완화**: 10초마다 폴링 시 서버 값으로 보정. 표시 목적이므로 수초 오차는 허용.

### 의존성
- **ExamSession 항상 생성 변경 (작업 6)** 은 모니터링 기능의 핵심 전제. 이 변경 없이는 시간 제한 없는 시험의 응시 중 수험자를 표시할 수 없음.
- 프론트엔드 작업(8~11)은 백엔드 API(1~6) 완료에 의존.
- shadcn-vue 기존 컴포넌트(Card, Table, Badge, Button)만 사용하므로 새 컴포넌트 설치 불필요.
