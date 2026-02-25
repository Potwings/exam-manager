# ExamManager

기술 면접 필기 시험 관리 서비스. 관리자가 Web UI에서 시험 문제/채점기준을 등록하면 시험자가 접속하여 문제를 풀고, 제출 시 Ollama LLM으로 자동 채점한다.

## Tech Stack

- **Frontend**: Vue 3 + Vite 7 + Pinia + Vue Router + shadcn-vue (new-york style, Tailwind CSS v4)
- **Backend**: Spring Boot 3.5.7 (Java 17, Gradle 8.6)
- **Database**: MariaDB 10+ (로컬 설치, `exam_scorer` schema, 테스트: `exam_scorer_test`)
- **DB 드라이버**: `org.mariadb.jdbc:mariadb-java-client`
- **파일 파싱**: Apache POI 5.2.5 (docx)
- **마크다운**: markdown-it (문제 마크다운 렌더링) + @tailwindcss/typography (prose 스타일) + highlight.js (코드 블록 syntax highlighting)
- **아이콘**: lucide-vue-next
- **인증**: Spring Security 6 (세션 기반, BCrypt)
- **LLM 채점**: Ollama (gpt-oss:20b 모델, 로컬 `http://localhost:11434`)
- **코드 에디터**: Monaco Editor (`@guolao/vue-monaco-editor` + `monaco-editor`, 로컬 번들)
- **알림**: vue-sonner (Toast) + SSE (Server-Sent Events) + Browser Notification API

## Project Structure

```
exam-scorer/
├── frontend/                # Vue 3 SPA
│   └── src/
│       ├── api/             # Axios 인스턴스 + API 호출 함수
│       ├── assets/          # index.css (Tailwind + shadcn 테마)
│       ├── components/      # 커스텀 컴포넌트 (ProblemEditDialog 등)
│       │   └── ui/          # shadcn-vue 컴포넌트 (npx shadcn-vue로 관리)
│       ├── composables/     # Vue 컴포저블 (useNotifications)
│       ├── lib/             # utils.ts (cn 헬퍼), markdown.js (markdown-it 래퍼)
│       ├── stores/          # Pinia (authStore, examStore)
│       ├── views/
│       │   ├── admin/       # AdminLogin, ExamManage, ExamCreate, ExamDetail, ScoreBoard, ScoreDetail, AdminMembers, ChangePassword
│       │   └── exam/        # ExamLogin, ExamTake
│       └── router/          # Vue Router
├── backend/                 # Spring Boot
│   └── src/main/java/com/exammanager/
│       ├── config/          # SecurityConfig, WebConfig, OllamaProperties, CorsProperties, AdminInitializer, InitLoginFilter
│       ├── controller/      # AdminController, ExamController, ExamineeController, SubmissionController, ScoreController, AiAssistController, ExamSessionController, NotificationController
│       ├── service/         # ExamService, DocxParserService, GradingService, OllamaClient, SubmissionService, AiAssistService, AdminUserDetailsService, NotificationService
│       ├── repository/      # JPA Repositories (6개)
│       ├── entity/          # Admin, Exam, Problem, Answer, Examinee, Submission, ExamSession
│       └── dto/             # 요청/응답 DTO
│   └── src/main/resources/
│       └── data/            # seed.sql (초기 시드 데이터, 수동 실행용)
└── docs/                    # 샘플 docx 파일 (test_problem.docx, test_answer.docx)
```

## Commands

### Frontend
```bash
cd frontend
npm install --legacy-peer-deps   # 의존성 설치 (peer deps 충돌 시 필수)
npm run dev          # Vite 개발서버 (localhost:5173)
npm run build        # 프로덕션 빌드
```

### Backend
```bash
cd backend
./gradlew.bat compileJava    # 컴파일
./gradlew.bat bootRun        # Spring Boot 기동 (localhost:8080, MariaDB 필요)
./gradlew.bat test           # 테스트 (MariaDB exam_scorer_test 스키마, create-drop)
```

### Ollama
```bash
ollama run gemma3            # gemma3 모델 실행 (채점 시 필요)
# Ollama 미실행 시 → 폴백(equalsIgnoreCase 단순 비교) 동작
```

### shadcn-vue 컴포넌트 추가
```bash
cd frontend
npx shadcn-vue@latest add <component-name>
```

## Conventions

### Frontend
- **UI 컴포넌트**: 반드시 shadcn-vue 사용. `src/components/ui/`는 CLI로 관리하므로 직접 수정 지양
- **스타일링**: Tailwind CSS 유틸리티 클래스 사용. 커스텀 CSS 최소화
- **경로 alias**: `@/` → `src/` (tsconfig paths + vite alias)
- **상태관리**: Pinia Composition API 스타일 (`defineStore` + `ref`)
- **라우터**: `src/router/index.js`에 정의. views는 `admin/`, `exam/` 디렉토리로 구분
- **API 호출**: `src/api/index.js`의 axios 인스턴스 + named export 함수 사용. Vite proxy로 `/api` → `API_TARGET` (`.env`) 연결
- **코드 에디터**: Monaco Editor는 `main.js`에서 글로벌 플러그인으로 등록. 로컬 번들 로드 (`monaco-editor` + Web Worker 설정, CDN 의존성 없음)
- **헤더**: 좌상단 서비스명 "ExamManager" 표시. 관리자 미로그인 시 우측에 "관리자 로그인" 링크 표시 (`text-xs`, `text-muted-foreground/60`로 눈에 띄지 않게 처리) (`App.vue`)

### Backend
- **패키지**: `com.exammanager` 하위 `config`, `controller`, `service`, `repository`, `entity`, `dto`
- **Lombok**: Entity에 `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder` 패턴
- **REST API**: `/api/` prefix. Controller → Service → Repository 계층 구조
- **JPA**: `ddl-auto: validate` (기본), `update` (dev 프로파일), `create-drop` (테스트). Entity에 `@PrePersist`로 createdAt 자동 설정
- **파일 업로드**: multipart max 10MB
- **DB 접속**: `jdbc:mariadb://127.0.0.1:3306/exam_scorer`
- **예외 처리**: 리소스 미발견 시 `ResponseStatusException(NOT_FOUND)` 사용 (HTTP 404 반환)
- **에러 핸들러 보안**: `@ExceptionHandler`에서 `e.getMessage()`를 클라이언트에 직접 반환 금지. 고정 메시지만 응답하고, 실제 에러는 `log.error()`/`log.warn()`으로 서버 로그에 기록. `Map.of()`는 null 값 시 NPE 발생하므로 주의

## 설정 파일 구조

### Backend (Spring Boot)
```
application.yml          — 운영 안전 기본값 (validate, show-sql: false, ${DB_USERNAME}/${DB_PASSWORD})
application-dev.yml      — 개발 오버라이드 (update, show-sql: true), profiles.include: local
application-local.yml    — DB 자격증명 + CORS 허용 origin (gitignored, **/application-local.yml)
```

- `application.yml`의 `spring.profiles.active: dev`로 로컬 개발 시 자동 적용
- 프로파일 로딩: `application.yml` → `application-dev.yml` → `application-local.yml`
- 테스트: `src/test/resources/application.yml` + `application-local.yml` (create-drop, MariaDB)
- 운영 배포 시: `SPRING_PROFILES_ACTIVE` 환경변수로 dev 비활성화, `DB_USERNAME`/`DB_PASSWORD` 환경변수 주입

### CORS 설정
- `CorsProperties.java` — `@ConfigurationProperties(prefix = "app.cors")`, `allowedOrigins` 리스트 바인딩
- `SecurityConfig`에서 `corsProperties.getAllowedOrigins()`로 동적 참조
- `application.yml`에 기본값 (`http://localhost:5173`), `application-local.yml`에서 개발자별 origin 오버라이드
- 리스트 오버라이드 시 **전체 교체** (append 아님) — local에서 오버라이드 시 기본값도 포함해야 함

### Frontend (Vite)
```
frontend/.env.example    — 환경변수 목록 안내 (git 포함)
frontend/.env            — 실제 값 (gitignored)
```

- `vite.config.js`에서 `loadEnv(mode, cwd, '')`로 `.env` 파일 로드 (세 번째 인자 `''`로 `VITE_` 접두사 없는 변수도 로드)
- `ALLOWED_HOSTS` — Vite dev server 허용 호스트 (쉼표 구분, 예: `exam.crinity.com`)
- `API_TARGET` — 백엔드 API 프록시 대상 (기본값: `http://localhost:8080`)

## LLM 채점 시스템

### 아키텍처 (비동기 채점)
```
SubmissionService.submitAnswers()  ← @Transactional
  → 재시험 방지: existsByExamineeIdAndProblemExamId() → 이미 있으면 409 CONFLICT
  → 중복 problemId 제거 (마지막 항목 유지)
  → 기존 제출 조회 (findByExamineeIdAndProblemId) → 있으면 업데이트, 없으면 새로 생성
  → 답안만 저장 (earnedScore/feedback/isCorrect = null 상태)
  → 즉시 응답 반환 (void)
  → TransactionSynchronization.afterCommit()
    → GradingService.gradeSubmissionsAsync()  ← @Async @Transactional (별도 스레드)
      → 해당 수험자의 미채점 submission 일괄 조회
      → 개별 submission마다:
        → GradingService.grade()
          → null 방어: answer null → 0점, maxScore ≤ 0 → 스킵, 빈 답안 → 0점
          → OllamaClient.isAvailable() 확인
          → gradeWithLlm(): 프롬프트 조립 → Ollama /api/chat 호출 → JSON 파싱
          → 실패 시 gradeFallback(): equalsIgnoreCase 단순 비교
```
- `@EnableAsync`로 Spring 비동기 실행 인프라 활성화 (`ExamManagerApplication.java`)
- `@Async`는 같은 클래스 내부 호출 시 AOP 프록시를 우회하므로, 반드시 다른 빈에서 호출해야 함 (SubmissionService → GradingService)
- `TransactionSynchronization.afterCommit()`: 트랜잭션 커밋 후 비동기 채점 트리거 (커밋 전 호출 시 비동기 스레드에서 저장 안 된 데이터 조회 불가)

### 설정 (`application.yml`)
```yaml
ollama:
  base-url: http://localhost:11434
  model: gemma3
  timeout: 120        # 초 단위
  enabled: true
```
- `OllamaProperties.java` — `@ConfigurationProperties(prefix = "ollama")` 바인딩
- `OllamaClient.java` — RestTemplate 기반 HTTP 클라이언트 (`/api/chat`, `format: json`, `stream: false`, `temperature: 0.1`)

### 프롬프트 구조 (`GradingService.java`)
- **System**: 엄격한 채점관 역할. 부분 점수 규칙, 필수 키워드(뉘앙스 일치 허용), 감점 규칙 준수 지시
- **User**: `[문제]` + `[채점 기준](배점)` + `[수험자 답안]`
- **그룹 자식 문제**: 부모 지문을 `[보기]` 태그로 감싸서 `[문제]` 앞에 포함 (`[보기]\n{부모지문}\n\n[문제]\n{자식내용}`)
- **응답**: `{"earnedScore": N, "feedback": "채점 근거"}`

### 채점 기준
- 관리자가 Web UI(`/admin/exams/create`)에서 문제별 채점 기준(루브릭)을 직접 입력
- `[필수 키워드]`: 핵심 개념 (정확 일치 아닌 의미/뉘앙스 유사 시 인정)
- `[감점 규칙]`: 키워드만 나열 시 부분 점수, 설명 없이 만점 불가 등
- 코드 결과 문제는 전부 맞춰야 점수 부여

### 시드 데이터 (seed.sql)
- 기존 하드코딩(DataInitializer.java) 데이터를 `backend/src/main/resources/data/seed.sql`로 추출
- 자동 실행 아님 — 초기 데이터 필요 시 수동 실행: `mysql -u root -p exam_scorer < seed.sql`
- DataInitializer.java는 삭제됨

### 폴백
Ollama 미실행/오류 시 → `equalsIgnoreCase` 단순 비교 + feedback "오답 (단순 비교 채점)"

## 시험 관리 시스템

### 그룹 문제 (꼬리 문제)
하나의 공통 지문(보기)에 여러 하위 문제가 엮인 출제 형식:
```
Q5. [보기] 다음 테이블 구조를 보고 아래 물음에 답하시오. (부모 - 지문만, 답안/배점 없음)
  ├── Q5-1. INSERT 문을 작성하시오. (하위 - 답안/배점 있음)
  ├── Q5-2. UPDATE 문을 작성하시오. (하위 - 답안/배점 있음)
  └── Q5-3. SELECT 문을 작성하시오. (하위 - 답안/배점 있음)
```

#### Entity 구조 (`Problem.java`)
- `@ManyToOne parent` (LAZY) + `@OneToMany children` (EAGER, orphanRemoval, `@OrderBy("problemNumber ASC")`)
- 부모 문제: `answerContent=null`, `score=null`, Answer 미생성 (지문 전용)
- 자식 문제: 각각 독립적인 답안/배점/채점
- 기존 독립 문제: `parent=null`, `children=[]` → 완전 하위 호환

#### 관리자 UI (`ExamCreate.vue`)
- 각 문제에 "그룹 문제" 토글 버튼 (`isGroup`)
- `isGroup=true` 시: 배점/채점기준 숨김, 하위 문제 섹션 표시 (border-l 인덴트)
- 하위 문제 추가/삭제 버튼, 번호: 부모번호-자식번호 (Q5-1)
- 검증: 그룹 문제는 지문 필수 + 하위 1개 이상 + 각 하위 답안/배점 유효
- 총점: 그룹 문제는 자식 배점 합산
- 수정/복제 모드: `p.children.length > 0`으로 `isGroup` 자동 판별
- 하위 문제 MARKDOWN 타입 선택 시 미리보기/편집 토글 지원 (독립 문제와 동일 패턴)

#### 응시자 UI (`ExamTake.vue`)
- 그룹 문제: 부모 지문 Card + 하위 문제별 답안 입력 (border-l 인덴트)
- `answerableProblems` computed: 독립 문제 + 그룹 자식 문제만 추출하여 답안 수집
- `answers[child.id]`로 하위 문제별 독립 입력

#### 시험 상세 (`ExamDetail.vue`)
- 그룹 문제: "그룹" Badge + 공통 지문 + 하위 문제 카드 (인덴트, 개별 배점/채점기준)

#### 채점 결과 (`ScoreDetail.vue`)
- `groupedSubmissions` computed: `parentProblemId`로 제출 결과 그룹핑
- 그룹: 부모 지문 한 번 표시 + 하위 제출 결과 각각 표시
- 독립: 기존 Card 유지

#### LLM 채점 (`GradingService.java`)
- 자식 문제 채점 시 부모 지문을 `[보기]` 태그로 프롬프트에 포함
- 부모는 Answer 없어 채점 대상 자체에서 제외

### 시험 생성
- 관리자가 `/admin/exams/create`에서 시험 제목 + 문제/채점기준/배점을 직접 입력
- `POST /api/exams` → `ExamCreateRequest` JSON → `ExamService.createExam()`
- docx 업로드 생성도 별도 유지: `POST /api/exams/upload` (추후 UI 연결 예정)

### 시험 수정
- 제출 결과가 없는 시험만 수정 가능 — `PUT /api/exams/{id}`
- 제출 결과가 있는 시험 수정 시도 시 409 CONFLICT 반환 → 복제 유도
- 수정 시 기존 문제 전체 삭제(`orphanRemoval`) 후 재생성 (`clear()` + `flush()` + INSERT)
- 삭제된 시험(`deleted=true`) 수정 불가 (400 Bad Request)
- ExamCreate 컴포넌트를 생성/수정/복제 겸용으로 사용
  - `/admin/exams/create` — 새 시험 생성
  - `/admin/exams/:id/edit` — 기존 시험 수정
  - `/admin/exams/create?from=:id` — 기존 시험 복제하여 새 시험 생성

### 시험 조회
- `/admin/exams/:id` — ExamDetail 상세 페이지 (개별 문제 편집 가능)
- 수정 버튼 클릭 시: 제출 결과 없으면 수정 페이지로, 있으면 복제 안내 배너 노출
- 목록에서 행 클릭으로 상세 페이지 이동

### 개별 문제 수정 (in-place PATCH)
- `PATCH /api/exams/{examId}/problems/{problemId}` — Problem ID를 보존하며 제자리 수정
- **Submission FK 안전**: Problem ID가 변경되지 않으므로 제출 결과가 있는 시험도 수정 가능 (PUT 전체 수정과 달리 409 미발생)
- `ExamService.updateProblem()` — 문제 조회 → 소속 시험 검증 → 삭제된 시험 가드(`deleted=true` → 400) → 필드 업데이트 → `applyAnswer()` 호출
- `applyAnswer()` — Answer 엔티티 생성/업데이트 공통 헬퍼 (createExam·updateProblem 양쪽에서 사용)

#### ProblemEditDialog.vue
- shadcn Dialog 기반 편집 컴포넌트 (`src/components/ProblemEditDialog.vue`)
- Props: `open`, `problem`, `examId`, `isGroupParent`, `parentProblemNumber`
- Emits: `update:open`, `saved(updatedProblem)`
- **그룹 부모 모드**: 콘텐츠 타입 + 문제 내용만 표시 (채점기준/배점/코드에디터 숨김), 설명 "공통 지문을 수정합니다"
- **독립/그룹 자식 모드**: 콘텐츠 타입, 코드 에디터 토글, 문제 내용, 채점 기준, 배점 전 필드 표시
- **displayNumber**: 그룹 자식은 `Q{부모}-{자식}` 형식 (예: Q13-1)
- **canSave 검증**: content 필수, 비그룹은 answerContent + score > 0 필수
- **마크다운 미리보기**: contentType=MARKDOWN 시 편집/미리보기 토글 (renderMarkdown 사용)
- `watch(open)` — Dialog 열릴 때 problem 데이터를 form으로 복사, 미리보기/에러 상태 초기화

#### ExamDetail.vue 연동
- 각 문제 카드 헤더에 SquarePen 편집 아이콘 버튼 (독립/그룹 부모/그룹 자식 3곳, `aria-label`로 문제 번호 포함한 접근성 레이블 제공)
- `openEditDialog(problem, isGroupParent, parentProblemNumber)` — 편집 대상 설정 + Dialog 열기
- `handleProblemSaved(updated)` — 응답으로 받은 문제를 기존 배열에서 찾아 `Object.assign`으로 즉시 반영 (top-level + children 재귀 탐색)
- `totalScore` computed — 문제 배점 합산을 동적 계산 (편집 후 즉시 갱신)

### 답안 노출 제어
- `ProblemResponse.from(problem)` — 답안 미포함 (수험자용)
- `ProblemResponse.from(problem, true)` — 답안 포함 (관리자용)
- `ExamDetailResponse.from(exam)` — 답안 미포함 (수험자: `/api/exams/active`)
- `ExamDetailResponse.from(exam, hasSubmissions)` — 답안 포함 (관리자: `/api/exams/{id}`)

### 시험 활성화
- 동시에 1개만 활성 가능 — `PATCH /api/exams/{id}/activate`
- 기존 활성 시험 자동 비활성화 → 새 시험 활성화
- 삭제된 시험(`deleted=true`)은 활성화 불가 (400 Bad Request)
- 수험자는 `/exam/login`에서 활성 시험만 자동 표시 (`GET /api/exams/active`)

### 소프트 삭제
- `DELETE /api/exams/{id}` → `deleted=true`, `active=false` 설정
- DB 데이터 보존, 관리자 목록에서만 숨김 (`findByDeletedFalse()`)

### 시험 시간 제한
- `Exam.timeLimit` — nullable `Integer`, 분 단위 (null = 무제한)
- 시험 생성/수정 시 관리자가 선택적으로 설정 (`ExamCreate.vue`)
- 시험 목록에 Clock 아이콘 + `{N}분` 표시, 없으면 `-` (`ExamManage.vue`)
- 수험자 로그인 시 `· 제한시간 {N}분` 조건부 표시 (`ExamLogin.vue`)

#### ExamSession (서버 기반 시간 관리)
- `ExamSession` 엔티티: `examinee` + `exam` + `startedAt` (서버 시간 기준)
- `UNIQUE(examinee_id, exam_id)` — 동일 수험자의 중복 세션 방지
- `POST /api/exam-sessions` — find-or-create 패턴 (DataIntegrityViolationException 처리)
  - timeLimit 없으면 `{ remainingSeconds: null }` 반환 (세션 미생성)
  - 있으면 `startedAt + timeLimit - now` 계산 → 남은 초 반환
- 새로고침 시에도 서버 기준 시간이 이어짐 (클라이언트 조작 불가)

#### 타이머 UI (`ExamTake.vue`)
- **sticky 헤더**: 시험 제목 + 타이머 위젯이 `sticky top-0`으로 스크롤 시 상단 고정
- **카운트다운**: `setInterval` 1초 간격, `MM:SS` monospace 포맷 (`tabular-nums`)
- **색상 변화**: 평소 다크(slate-900) → 5분 이하 amber → 1분 이하 red + animate-pulse
- **프로그레스 바**: 전체 시간 대비 남은 시간 비율, 색상 연동
- **자동 제출**: 0초 도달 시 `handleSubmit()` 자동 호출 + "시간 종료" 메시지
- **시간 제한 없는 시험**: 타이머 위젯 미표시 (`v-if="formattedTime !== null"`)

#### 서버 측 시간 초과 검증 (`SubmissionService`)
- timeLimit 있는 시험 제출 시 `startedAt + timeLimit + 1분(grace) < now` 검증
- 초과 시 `403 FORBIDDEN` ("시험 시간이 종료되었습니다")
- 1분 여유시간: 네트워크 지연 고려

## DB Schema (Entities)

| Entity | Table | 설명 |
|--------|-------|------|
| Admin | admins | 관리자 (username:`UNIQUE`, password:`BCrypt`, role, **initLogin**) |
| Exam | exams | 시험 (title, problemFileName, answerFileName, **deleted**, **active**, **timeLimit**) |
| Problem | problems | 문제 (problemNumber, content, **contentType**, **codeEditor**, **parent_id**) → Exam N:1, 자기참조 부모-자식 |
| Answer | answers | 정답/채점기준 (content, score:`int`) → Problem 1:1 |
| Examinee | examinees | 시험자 (name, **birthDate**) |
| Submission | submissions | 제출 답안 (submittedAnswer, isCorrect, earnedScore, **feedback**, **annotatedAnswer**) → Examinee, Problem N:1 |
| ExamSession | exam_sessions | 시험 세션 (startedAt) → Examinee, Exam N:1. `UNIQUE(examinee_id, exam_id)` |

## API Endpoints

| Method | Path | Controller | 설명 |
|--------|------|------------|------|
| GET | `/api/exams` | ExamController | 시험 목록 — 삭제 안 된 것만 (ExamResponse) |
| GET | `/api/exams/{id}` | ExamController | 시험 상세 + 문제 목록 (ExamDetailResponse) |
| GET | `/api/exams/{id}/problems` | ExamController | 시험별 문제 목록 (ProblemResponse) |
| GET | `/api/exams/active` | ExamController | 현재 활성 시험 1개 (수험자용, ExamDetailResponse) |
| POST | `/api/exams` | ExamController | 시험 생성 — Web UI JSON (ExamCreateRequest) |
| POST | `/api/exams/upload` | ExamController | 시험 생성 — docx 업로드 (multipart, 추후 UI 연결 예정) |
| PUT | `/api/exams/{id}` | ExamController | 시험 수정 — 제출 결과 없을 때만 (409 CONFLICT) |
| PATCH | `/api/exams/{examId}/problems/{problemId}` | ExamController | 개별 문제 제자리 수정 (Problem ID 보존) — **Admin** |
| DELETE | `/api/exams/{id}` | ExamController | 시험 소프트 삭제 (deleted=true) |
| PATCH | `/api/exams/{id}/activate` | ExamController | 시험 활성화 (동시 1개만) |
| POST | `/api/admin/login` | AdminController | 관리자 로그인 (세션 생성) — **Public** |
| POST | `/api/admin/logout` | AdminController | 관리자 로그아웃 (세션 무효화) |
| GET | `/api/admin/me` | AdminController | 현재 세션 관리자 정보 — **Public** |
| POST | `/api/admin/register` | AdminController | 관리자 등록 — **Admin** |
| GET | `/api/admin/list` | AdminController | 관리자 목록 조회 — **Admin** |
| DELETE | `/api/admin/{id}` | AdminController | 관리자 삭제 (자기 자신 불가) — **Admin** |
| PATCH | `/api/admin/change-password` | AdminController | 비밀번호 변경 + initLogin 해제 — **Admin** |
| POST | `/api/examinees/login` | ExamineeController | 시험자 로그인 — 이름+생년월일 find-or-create — **Public** |
| POST | `/api/submissions` | SubmissionController | 답안 제출 (즉시 저장 + 비동기 LLM 채점, 재시험 방지) — **Public** |
| GET | `/api/submissions/result` | SubmissionController | 채점 결과 조회 — **Admin** |
| PATCH | `/api/submissions/{id}` | SubmissionController | 채점 결과 수정 (득점/피드백/답안서식) — **Admin** |
| GET | `/api/scores/exam/{examId}` | ScoreController | 시험별 점수 집계 — **Admin** |
| GET | `/api/ai-assist/status` | AiAssistController | AI 출제 도우미 사용 가능 여부 확인 — **Admin** |
| POST | `/api/ai-assist/generate` | AiAssistController | AI 문제/채점기준 자동 생성 — **Admin** |
| POST | `/api/exam-sessions` | ExamSessionController | 시험 세션 생성/조회 (find-or-create, 남은 시간 반환) — **Public** |
| GET | `/api/exam-sessions/remaining` | ExamSessionController | 남은 시간 조회 (새로고침용) — **Public** |
| GET | `/api/notifications/stream` | NotificationController | SSE 알림 스트림 (채점 완료/관리자 호출 이벤트) — **Admin** |
| POST | `/api/notifications/call-admin` | NotificationController | 수험자 → 관리자 호출 알림 전송 — **Public** |

## DTO

| 클래스 | 용도 |
|--------|------|
| ExamCreateRequest | 시험 생성/수정 요청 (title, **timeLimit**, problems[{problemNumber, content, **contentType**, **codeEditor**, answerContent, score, **children**}]) |
| ProblemUpdateRequest | 개별 문제 수정 요청 (@NotBlank content, contentType, codeEditor, answerContent, score) — 그룹 부모는 content만, 독립/자식은 전 필드 |
| ExamResponse | 시험 목록 응답 (id, title, problemCount, totalScore, **active**, **timeLimit**, createdAt) — problemCount는 최상위 문제만 카운트 |
| ExamDetailResponse | 시험 상세 응답 (problems, **hasSubmissions**, **timeLimit** 포함) — problems는 최상위만 필터 (자식은 재귀 포함) |
| ProblemResponse | 문제 응답 (id, problemNumber, content, **contentType**, **codeEditor**, answerContent?, score?, **children**) — 답안은 관리자용만 포함, children 재귀 매핑 |
| AiAssistRequest | AI 출제 요청 (topic, difficulty, **parentContent** 등) |
| AiAssistResponse | AI 출제 응답 (problemContent, answerContent, contentType, score) |
| AdminLoginRequest | 관리자 로그인 요청 (username, password) |
| AdminRegisterRequest | 관리자 등록 요청 (username, password) |
| ChangePasswordRequest | 비밀번호 변경 요청 (currentPassword, newPassword) |
| AdminResponse | 관리자 응답 (id, username, role, **initLogin**, **createdAt**) |
| ExamineeLoginRequest | 로그인 요청 (name, **birthDate**) |
| ExamineeResponse | 시험자 응답 (id, name, **birthDate**) |
| SubmissionRequest | 답안 제출 요청 (examineeId, examId, answers[]) |
| SubmissionUpdateRequest | 채점 결과 수정 요청 (earnedScore, feedback, **annotatedAnswer**) |
| SubmissionResultResponse | 채점 결과 응답 (totalScore, maxScore, submissions[{..., **feedback**, **parentProblemId**, **parentProblemNumber**, **parentProblemContent**, **parentProblemContentType**}]) |
| ScoreSummaryResponse | 점수 집계 응답 (examineeName, **examineeBirthDate**, totalScore, maxScore, **gradingComplete**, submittedAt) |
| ExamSessionRequest | 시험 세션 생성 요청 (examineeId, examId) |
| ExamSessionResponse | 시험 세션 응답 (remainingSeconds — null이면 시간 제한 없음) |
| AdminCallRequest | 관리자 호출 요청 (examineeId, examId, examineeName) |

## Routes (Frontend)

| Path | Component | 설명 |
|------|-----------|------|
| `/admin/login` | AdminLogin | 관리자 로그인 (아이디/비밀번호) |
| `/admin/change-password` | ChangePassword | 비밀번호 변경 (최초 로그인 시 강제) — **Admin 가드** |
| `/admin/members` | AdminMembers | 관리자 계정 관리 (등록/삭제) — **Admin 가드** |
| `/admin/exams` | ExamManage | 시험 목록 관리 — **Admin 가드** |
| `/admin/exams/create` | ExamCreate | 시험 생성 — **Admin 가드** |
| `/admin/exams/:id` | ExamDetail | 시험 상세 조회 — **Admin 가드** |
| `/admin/exams/:id/edit` | ExamCreate | 시험 수정 — **Admin 가드** |
| `/admin/scores` | ScoreBoard | 채점 결과 대시보드 — **Admin 가드** |
| `/admin/scores/:examId/:examineeId` | ScoreDetail | 수험자별 채점 상세 — **Admin 가드** |
| `/exam/login` | ExamLogin | 시험자 로그인 — 이름 + 생년월일 입력 |
| `/exam/take/:examId` | ExamTake | 시험 응시 — **Examinee 가드**, 제출 후 완료 메시지 표시 |

## 문제 콘텐츠 타입

### contentType 필드
- Problem 엔티티에 `contentType` 필드 (`TEXT` | `MARKDOWN`, 기본값 `TEXT`)
- 시험 생성 시(`ExamCreate.vue`) 문제별로 타입 선택 가능

### 렌더링 분기 (`ExamTake.vue`)
- **TEXT**: `<pre>` 태그로 원문 그대로 출력 (일반 텍스트 문제)
- **MARKDOWN**: `prose` 클래스 + `v-html`로 마크다운 렌더링 (테이블, 코드블록 등 복잡한 서식)

### 마크다운 지원 (`src/lib/markdown.js`)
- `markdown-it` 라이브러리 래퍼 (`renderMarkdown()` 함수)
- `@tailwindcss/typography` 플러그인으로 prose 스타일 적용
- Tailwind CSS v4에서는 `@plugin "@tailwindcss/typography"` 지시자 사용 (`@import` 아님)
- 활용 예시: SQL 문제의 테이블 구조, 코드 포함 문제 등

### 코드 블록 Syntax Highlighting (`highlight.js`)
- markdown-it `highlight` 콜백으로 highlight.js 연동 (`src/lib/markdown.js`)
- **지원 언어**: Java, JavaScript (js alias 포함), Python, SQL — core + 개별 언어만 import (경량 번들)
- **테마**: `github-dark.css` — Monaco Editor(vs-dark)와 시각적 일관성 유지
- **언어 라벨**: 코드 블록 우상단에 언어명 표시 (`.code-lang-label`, absolute 포지셔닝)
- **CSS 셀렉터**: `code[class*="language-"]` 사용 — markdown-it이 `<code class="language-{lang}">`으로 래핑하므로 `.hljs` 클래스 아닌 `language-*` 클래스로 매칭
- **언어 미지정** 코드 블록: highlight 콜백이 빈 문자열 반환 → markdown-it 기본 렌더링 (plain text)
- **인라인 코드**: highlight 콜백 미적용 → 기존 prose 인라인 스타일 유지
- 컴포넌트 수정 없이 `renderMarkdown()` 사용하는 모든 곳에 자동 적용 (ExamTake, ExamDetail, ScoreDetail)

## Monaco Editor (코드 에디터)

- **적용 기준**: 문제별 `codeEditor` 필드 (`Boolean`, 기본값 `false`) — 관리자가 시험 생성/수정 시 문제마다 개별 설정
- **관리자 UI** (`ExamCreate.vue`): 독립 문제·하위 문제 헤더에 "코드 에디터" 토글 버튼 (초록색 강조)
- **응시자 UI** (`ExamTake.vue`): `problem.codeEditor === true`이면 Monaco Editor, 아니면 textarea 표시
- **시험 상세** (`ExamDetail.vue`): `codeEditor=true` 문제에 점수 옆 "코드 에디터" Badge 표시
- **기본 언어**: Java (수험자가 드롭다운으로 Java / JavaScript / Python / SQL 변경 가능)
- **설정**: VS Code 다크 테마, minimap 비활성화, fontSize 14, wordWrap on
- **로컬 번들**: `monaco-editor` 패키지 + Web Worker(editor/json/typescript) 직접 로드. 외부 CDN 의존성 없음

## 인증/권한 체계

### 관리자 인증 (Spring Security 세션 기반)
- `SecurityConfig.java` — 필터 체인 + CORS (`CorsProperties`에서 허용 origin 주입, `allowCredentials: true`)
- `Admin` 엔티티 + `AdminRepository` + `AdminUserDetailsService` (UserDetailsService 구현)
- `AdminInitializer` — 앱 기동 시 `admins` 테이블 비어있으면 기본 계정 생성 (`admin/admin123`)
- `AdminController` — 프로그래매틱 인증 (`/api/admin/login`, `/api/admin/logout`, `/api/admin/me`)
- 미인증 시 401 반환 (`HttpStatusEntryPoint`), 로그인 폼 리다이렉트 안 함
- 세션 정책: `IF_REQUIRED` + `maximumSessions(1)`

### 최초 로그인 비밀번호 변경 (InitLoginFilter)
- `Admin.initLogin` 필드 (`boolean`, 기본값 `true`) — 신규 관리자는 `true`, 비밀번호 변경 완료 후 `false`
- `InitLoginFilter` (`OncePerRequestFilter`) — `initLogin=true`인 관리자가 `/api/**` 요청 시 403 차단
  - 허용 경로: `/api/admin/change-password`, `/api/admin/me`, `/api/admin/logout`
- 프론트엔드 라우터 가드 — `initLogin=true`이면 `/admin/change-password` 외 admin 페이지 접근 차단
- 헤더 탭(Manage/Scores/Members) — `initLogin=true`이면 숨김 처리 (`App.vue`)
- 로그인 후 리다이렉트: `initLogin=true` → `/admin/change-password`, `false` → `/admin/scores`

### 엔드포인트 보호 규칙
| 분류 | 경로 | 접근 |
|------|------|------|
| Public | `GET /api/exams/active`, `POST /api/examinees/**`, `POST /api/submissions`, `/api/exam-sessions/**` | permitAll |
| Public | `/api/admin/login`, `/api/admin/me` | permitAll |
| Public | `POST /api/notifications/call-admin` | permitAll |
| Admin | `GET/POST/PUT/DELETE/PATCH /api/exams/**` (active 제외) | authenticated |
| Admin | `GET/PATCH /api/submissions/**` (POST 제외), `/api/scores/**`, `/api/ai-assist/**` | authenticated |
| Admin | `/api/notifications/**` (call-admin 제외) | authenticated |

### 수험자 인증 (이름 + 생년월일)
- `Examinee` 엔티티에 `birthDate` (LocalDate) 필드 추가
- `findByNameAndBirthDate()` — 이름+생년월일 동일하면 기존 레코드 재사용 (find-or-create)
- 동일 수험자의 재시험 방지에 활용 (같은 examineeId로 이미 제출 기록 존재 시 409)

### 재시험 방지
- `SubmissionService.submitAnswers()` 시작 시 `existsByExamineeIdAndProblemExamId()` 검증
- 이미 제출 기록이 있으면 `409 CONFLICT` ("이미 응시 완료한 시험입니다")
- 프론트엔드에서 409 응답 시 에러 메시지 표시 + `/exam/login`으로 이동

### 프론트엔드 인증 상태 (authStore)
- `admin` ref — 관리자 세션 정보, `adminLoading` ref — 세션 확인 완료 전 가드 방지
- `checkAdmin()` — `GET /api/admin/me`로 세션 복원 (페이지 새로고침 대응)
- `examinee` ref — localStorage 연동: 로그인 시 저장, 스토어 초기화 시 복원, `clear()` 시 삭제
- axios `withCredentials: true` — 세션 쿠키(JSESSIONID) 자동 포함
- 401 인터셉터 — admin 페이지에서 세션 만료 시 `/admin/login`으로 리다이렉트

### 응시 중 답안 유지 (localStorage)
- **수험자 인증**: `localStorage.examinee` — 로그인 시 저장, 새로고침/브라우저 재시작 시 복원, 제출 후 삭제
- **답안 자동 저장**: `localStorage.exam_{examId}_answers` — `watch(answers, ..., { deep: true })`로 변경 시마다 저장
- **답안 복원**: `onMounted`에서 문제 로드 후 `Object.assign(answers, saved)`로 복원
- **정리**: `handleSubmit()` 성공 시 답안 키 삭제 + `authStore.clear()`로 수험자 키 삭제
- **타이머**: 서버 기반 ExamSession이므로 별도 저장 불필요 (새로고침 시 서버에서 남은 시간 재계산)
- **페이지 이탈 방지**: `beforeunload`(브라우저 새로고침/탭 닫기) + `onBeforeRouteLeave`(Vue Router 이동) — 제출 완료 전에만 확인 다이얼로그 표시, 제출 후 해제

### 라우터 가드
- `/admin/*` (login 제외) — `meta.requiresAdmin: true`, `checkAdmin()` await 후 인증 확인
- `/exam/take/:examId` — `meta.requiresExaminee: true`, `authStore.examinee` 확인
- `initLogin` 가드 — `initLogin=true`이면 `/admin/change-password` 외 admin 페이지 접근 시 리다이렉트
- `App.vue` — Manage, Scores, Members 링크는 `authStore.admin && !initLogin` 일 때만 표시. 미로그인 시 "관리자 로그인" 링크 표시

### 답안 제출 결과 관리자 전용화
- `POST /api/submissions` — 답안 즉시 저장 후 성공 메시지만 반환, 채점은 `@Async`로 백그라운드 실행
- `ExamTake.vue` — 제출 후 "제출 완료" Card 표시 (결과 페이지 이동 제거), 시간 만료 시 "시간 종료" 표시
- `GET /api/submissions/result` — 관리자 인증 필수 (채점 결과 조회)

### 채점 중 상태 표시 + 자동 폴링
- 비동기 채점 미완료 시 `earnedScore = null` → "채점 중" Badge (amber 색상 + 스피너) 표시
- `ScoreSummaryResponse.gradingComplete` — 전체 submission의 `earnedScore != null` 여부
- `ScoreBoard.vue` — 목록에서 "채점 중" Badge 표시, 5초 간격 폴링으로 자동 반영
- `ScoreDetail.vue` — 문제별 "채점 중" Badge + "채점이 완료되면 피드백이 표시됩니다" 안내, 5초 간격 폴링
- 폴링은 채점 중 항목이 있을 때만 시작, 모두 완료되면 자동 중단 (`onUnmounted`에서 cleanup)

### 알림 시스템 (SSE + Toast + Browser Notification)

#### 아키텍처
```
1) 채점 완료 알림 (서버 → 관리자)
GradingService.gradeSubmissionsAsync() 완료
  → NotificationService.notifyGradingComplete()
    → SseEmitter.send("grading-complete") (모든 연결된 관리자에게 push)
      → 프론트엔드 EventSource 수신
        → 탭 활성: toast.success('채점 완료')
        → 탭 비활성: Browser Notification API

2) 관리자 호출 (수험자 → 서버 → 관리자)
ExamTake.vue "관리자 호출" 버튼 클릭
  → POST /api/notifications/call-admin (permitAll)
    → NotificationService.notifyAdminCall()
      → SseEmitter.send("admin-call") (모든 연결된 관리자에게 push)
        → 프론트엔드 EventSource 수신
          → 탭 활성: toast.warning('관리자 호출')
          → 탭 비활성: Browser Notification API
```

#### 백엔드 (SSE)
- `NotificationService`: `CopyOnWriteArrayList<SseEmitter>` 스레드 안전 관리
  - `createEmitter()`: 30분 타임아웃 SseEmitter 생성 + onCompletion/onTimeout/onError 자동 정리
  - `notifyGradingComplete()`: 모든 emitter에 `grading-complete` 이벤트 JSON 전송, IOException 시 해당 emitter 제거
  - `notifyAdminCall()`: 모든 emitter에 `admin-call` 이벤트 JSON 전송 (수험자 이름, 시험 ID 포함)
- `NotificationController`:
  - `GET /api/notifications/stream` — SSE 스트림 (`text/event-stream`) — **Admin**
  - `POST /api/notifications/call-admin` — 관리자 호출 요청 (`AdminCallRequest`) — **Public**
- `GradingService`: 채점 루프 완료 후 총점 계산 + 수험자 이름 조회 + 알림 전송 (try-catch로 실패 격리)

#### 프론트엔드 (SSE 수신)
- `useNotifications.js` 컴포저블: EventSource SSE 연결 관리
  - `connect()`: SSE 연결 생성 + `shouldReconnect=true`
  - `disconnect()`: `shouldReconnect=false` + 타이머 정리 + EventSource 종료
  - `onerror`: `shouldReconnect` 플래그 확인 후 3초 딜레이 자동 재연결 (의도적 disconnect 시 재연결 방지)
  - `grading-complete` 이벤트: 탭 활성 → `toast.success()`, 탭 비활성 → `new Notification()`
  - `admin-call` 이벤트: 탭 활성 → `toast.warning()`, 탭 비활성 → `new Notification()`
  - `requestPermission()`: 관리자 로그인 시 브라우저 알림 권한 요청
- `App.vue`: `<Toaster />` 마운트 + `watch(authStore.admin)` → 로그인 시 connect, 로그아웃 시 disconnect

#### 관리자 호출 UI (`ExamTake.vue`)
- sticky 헤더 우측 타이머 좌측에 "관리자 호출" 버튼 배치 (`variant="destructive"`)
- 30초 쿨다운: 호출 후 버튼 비활성화 + "호출 (N초)" 카운트다운 표시 (스팸 방지)
- 제출 완료 후 호출 버튼 미표시 (submitted 상태에서는 완료 Card만 표시)

### 채점 답안 마커 시스템 (ScoreDetail.vue)
- 편집 모드에서 정답(초록)/오답(빨강)/부분(주황) 마커 버튼 제공
- 마커 문법: `[정답]텍스트[/정답]`, `[오답]텍스트[/오답]`, `[부분]텍스트[/부분]`
- 토글: 같은 마커 재클릭 시 해제, 다른 마커 클릭 시 교체
- `execCommand('insertText')` 기반으로 Ctrl+Z 되돌리기 지원, 적용 후 선택 유지
- `annotatedAnswer` 비어있으면 `submittedAnswer`로 자동 채움
- `parseAnnotatedAnswer()` — 마커 텍스트를 파싱하여 색상 span으로 렌더링 (미리보기 + 읽기 모드)
- `PATCH /api/submissions/{id}` — `annotatedAnswer` 필드로 저장

## AI 출제 도우미

- 시험 생성/수정 시 문제별 AI 자동 생성 기능 (Sparkles 아이콘 버튼)
- Ollama 연동: `AiAssistService` → `OllamaClient` → gemma3 모델
- `GET /api/ai-assist/status` — Ollama 사용 가능 여부 확인 (버튼 표시 제어)
- `POST /api/ai-assist/generate` — 주제/난이도 기반 문제+채점기준 생성
- `AiAssistDialog.vue` — shadcn Dialog + ScrollArea로 결과 표시, 적용 버튼으로 폼에 반영
- **그룹 문제 공통지문**: 하위 문제에서 AI 요청 시 `AiAssistRequest.parentContent`로 부모 지문 전달 → `AiAssistService`에서 `[보기]` 태그로 프롬프트에 포함, Dialog에 공통지문 안내 배너 표시
- Ollama 미실행 시 AI 버튼 자체가 숨김 처리됨

## TODO (미구현)

### Phase 3 — 고도화
- [x] 관리자 인증/권한 분리
- [x] 관리자 계정 관리 (등록/목록/삭제 + 최초 로그인 비밀번호 변경)
- [x] 답안 제출 비동기 채점 전환 (@Async + afterCommit + 채점 중 UI + 자동 폴링)
- [ ] 서비스/컨트롤러 단위 테스트 추가
- [x] 채점 결과 상세 보기 (관리자가 개별 수험자 답안+피드백 확인) — ScoreDetail.vue 별도 페이지
- [x] 채점 결과 첨삭 기능 (관리자가 득점/피드백 인라인 수정) — PATCH /api/submissions/{id}
- [x] 채점 답안 마커 툴바 (정답/오답/부분 색상 마커 적용·토글·교체, Ctrl+Z 지원)
- [x] 관리자 로그인 페이지 접근 개선 — 헤더 우측에 "관리자 로그인" 링크 추가 (미로그인 시만 표시)
- [x] 시험 시간 제한 + 카운트다운 타이머 + 자동 제출 (ExamSession 서버 기반)
- [x] 응시 중 답안 유지 — localStorage로 수험자 인증/답안 영속화 (새로고침/브라우저 재시작 대응)
- [x] 채점 완료 알림 — SSE + vue-sonner Toast + Browser Notification API (관리자 실시간 알림)
- [x] 관리자 호출 — 수험자가 시험 중 관리자에게 도움 요청 (SSE admin-call 이벤트 + 30초 쿨다운)
- [x] 그룹 문제(꼬리 문제) — 부모-자식 문제 구조 (생성/수정/복제/응시/채점/결과 표시)
- [x] 마크다운 코드 블록 syntax highlighting — highlight.js (github-dark 테마, Java/JS/Python/SQL)
- [x] ExamDetail 개별 문제 수정 — ProblemEditDialog + in-place PATCH (Problem ID 보존, Submission FK 안전)
- [ ] docx 업로드 시험 생성 UI 연결 (`POST /api/exams/upload` 엔드포인트 준비됨)
