# ExamManager

기술 면접 필기 시험 관리 서비스. 관리자가 Web UI에서 시험 문제/채점기준을 등록하면 시험자가 접속하여 문제를 풀고, 제출 시 Ollama LLM으로 자동 채점한다.

## Tech Stack

- **Frontend**: Vue 3 + Vite 7 + Pinia + Vue Router + shadcn-vue (new-york style, Tailwind CSS v4)
- **Backend**: Spring Boot 3.2.4 (Java 17, Gradle 8.6)
- **Database**: MariaDB 10+ (로컬 설치, `exam_scorer` schema, 테스트: `exam_scorer_test`)
- **DB 드라이버**: `org.mariadb.jdbc:mariadb-java-client`
- **파일 파싱**: Apache POI 5.2.5 (docx)
- **마크다운**: markdown-it (문제 마크다운 렌더링) + @tailwindcss/typography (prose 스타일)
- **아이콘**: lucide-vue-next
- **LLM 채점**: Ollama (gemma3 모델, 로컬 `http://localhost:11434`)
- **코드 에디터**: Monaco Editor (`@guolao/vue-monaco-editor`, CDN 로드)

## Project Structure

```
exam-scorer/
├── frontend/                # Vue 3 SPA
│   └── src/
│       ├── api/             # Axios 인스턴스 + API 호출 함수
│       ├── assets/          # index.css (Tailwind + shadcn 테마)
│       ├── components/ui/   # shadcn-vue 컴포넌트 (npx shadcn-vue로 관리)
│       ├── lib/             # utils.ts (cn 헬퍼), markdown.js (markdown-it 래퍼)
│       ├── stores/          # Pinia (authStore, examStore)
│       ├── views/
│       │   ├── admin/       # ExamManage, ExamCreate, ScoreBoard
│       │   └── exam/        # ExamLogin, ExamTake, ExamResult
│       └── router/          # Vue Router
├── backend/                 # Spring Boot
│   └── src/main/java/com/exammanager/
│       ├── config/          # WebConfig (CORS), OllamaProperties
│       ├── controller/      # ExamController, ExamineeController, SubmissionController, ScoreController
│       ├── service/         # ExamService, DocxParserService, GradingService, OllamaClient, SubmissionService
│       ├── repository/      # JPA Repositories (4개)
│       ├── entity/          # Exam, Problem, Answer, Examinee, Submission
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
- **API 호출**: `src/api/index.js`의 axios 인스턴스 + named export 함수 사용. Vite proxy로 `/api` → `localhost:8080` 연결
- **코드 에디터**: Monaco Editor는 `main.js`에서 글로벌 플러그인으로 등록. CDN(`jsdelivr`)에서 로드
- **헤더**: 좌상단 서비스명 "ExamManager" 표시 (`App.vue`)

### Backend
- **패키지**: `com.exammanager` 하위 `config`, `controller`, `service`, `repository`, `entity`, `dto`
- **Lombok**: Entity에 `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder` 패턴
- **REST API**: `/api/` prefix. Controller → Service → Repository 계층 구조
- **JPA**: `ddl-auto: update` (운영), `create-drop` (테스트). Entity에 `@PrePersist`로 createdAt 자동 설정
- **파일 업로드**: multipart max 10MB
- **DB 접속**: `jdbc:mariadb://127.0.0.1:3306/exam_scorer`

## LLM 채점 시스템

### 아키텍처
```
SubmissionService.submitAnswers()
  → GradingService.grade()
    → OllamaClient.isAvailable() 확인
    → gradeWithLlm(): 프롬프트 조립 → Ollama /api/chat 호출 → JSON 파싱
    → 실패 시 gradeFallback(): equalsIgnoreCase 단순 비교
```

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

### 시험 생성
- 관리자가 `/admin/exams/create`에서 시험 제목 + 문제/채점기준/배점을 직접 입력
- `POST /api/exams` → `ExamCreateRequest` JSON → `ExamService.createExam()`
- docx 업로드 생성도 별도 유지: `POST /api/exams/upload` (추후 UI 연결 예정)

### 시험 활성화
- 동시에 1개만 활성 가능 — `PATCH /api/exams/{id}/activate`
- 기존 활성 시험 자동 비활성화 → 새 시험 활성화
- 수험자는 `/exam/login`에서 활성 시험만 자동 표시 (`GET /api/exams/active`)

### 소프트 삭제
- `DELETE /api/exams/{id}` → `deleted=true`, `active=false` 설정
- DB 데이터 보존, 관리자 목록에서만 숨김 (`findByDeletedFalse()`)

## DB Schema (Entities)

| Entity | Table | 설명 |
|--------|-------|------|
| Exam | exams | 시험 (title, problemFileName, answerFileName, **deleted**, **active**) |
| Problem | problems | 문제 (problemNumber, content, **contentType**) → Exam N:1 |
| Answer | answers | 정답/채점기준 (content, score) → Problem 1:1 |
| Examinee | examinees | 시험자 (name) — email 필드 제거됨 |
| Submission | submissions | 제출 답안 (submittedAnswer, isCorrect, earnedScore, **feedback**) → Examinee, Problem N:1 |

## API Endpoints

| Method | Path | Controller | 설명 |
|--------|------|------------|------|
| GET | `/api/exams` | ExamController | 시험 목록 — 삭제 안 된 것만 (ExamResponse) |
| GET | `/api/exams/{id}` | ExamController | 시험 상세 + 문제 목록 (ExamDetailResponse) |
| GET | `/api/exams/{id}/problems` | ExamController | 시험별 문제 목록 (ProblemResponse) |
| GET | `/api/exams/active` | ExamController | 현재 활성 시험 1개 (수험자용, ExamDetailResponse) |
| POST | `/api/exams` | ExamController | 시험 생성 — Web UI JSON (ExamCreateRequest) |
| POST | `/api/exams/upload` | ExamController | 시험 생성 — docx 업로드 (multipart, 추후 UI 연결 예정) |
| DELETE | `/api/exams/{id}` | ExamController | 시험 소프트 삭제 (deleted=true) |
| PATCH | `/api/exams/{id}/activate` | ExamController | 시험 활성화 (동시 1개만) |
| POST | `/api/examinees/login` | ExamineeController | 시험자 등록/로그인 (name만 사용) |
| POST | `/api/submissions` | SubmissionController | 답안 제출 + LLM 자동 채점 (타임아웃 5분) |
| GET | `/api/submissions/result` | SubmissionController | 채점 결과 조회 (query: examineeId, examId) |
| GET | `/api/scores/exam/{examId}` | ScoreController | 시험별 점수 집계 (ScoreSummaryResponse) |

## DTO

| 클래스 | 용도 |
|--------|------|
| ExamCreateRequest | 시험 생성 요청 (title, problems[{problemNumber, content, **contentType**, answerContent, score}]) |
| ExamResponse | 시험 목록 응답 (id, title, problemCount, totalScore, **active**, createdAt) |
| ExamDetailResponse | 시험 상세 응답 (problems 포함) |
| ProblemResponse | 문제 응답 (id, problemNumber, content, **contentType**) |
| ExamineeLoginRequest | 로그인 요청 (name) |
| ExamineeResponse | 시험자 응답 (id, name) |
| SubmissionRequest | 답안 제출 요청 (examineeId, examId, answers[]) |
| SubmissionResultResponse | 채점 결과 응답 (totalScore, maxScore, submissions[{..., **feedback**}]) |
| ScoreSummaryResponse | 점수 집계 응답 (examineeName, totalScore, maxScore, submittedAt) |

## Routes (Frontend)

| Path | Component | 설명 |
|------|-----------|------|
| `/admin/exams` | ExamManage | 시험 목록 관리 (활성화 토글, 소프트 삭제) |
| `/admin/exams/create` | ExamCreate | 시험 생성 — 문제/채점기준/배점 직접 입력 |
| `/admin/scores` | ScoreBoard | 채점 결과 대시보드 (시험 선택 드롭다운) |
| `/exam/login` | ExamLogin | 시험자 로그인 — 활성 시험 자동 로드 (이름만 입력) |
| `/exam/take/:examId` | ExamTake | 시험 응시 — 코드 문제(Q9~Q11, Q13~Q14)는 Monaco Editor, 나머지는 Textarea |
| `/exam/result` | ExamResult | 결과 확인 — 피드백 컬럼, 부분 정답 Badge (정답/부분 정답/오답) |

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

## Monaco Editor (코드 에디터)

- **적용 문제**: Q9, Q10, Q11, Q13, Q14 (`CODE_PROBLEM_NUMBERS` 배열로 관리)
- **기본 언어**: Q13 → SQL, Q11 → JavaScript, 나머지 → Java
- **수험자가 언어 변경 가능**: 드롭다운 (Java / JavaScript / Python / SQL)
- **설정**: VS Code 다크 테마, minimap 비활성화, fontSize 14, wordWrap on
- **CDN**: `https://cdn.jsdelivr.net/npm/monaco-editor@0.52.2/min/vs`

## TODO (미구현)

### Phase 3 — 고도화
- [ ] 관리자 인증/권한 분리
- [ ] 서비스/컨트롤러 단위 테스트 추가
- [ ] 에러 핸들링 및 입력 검증 강화
- [ ] 채점 결과 상세 보기 (관리자가 개별 수험자 답안+피드백 확인)
- [ ] docx 업로드 시험 생성 UI 연결 (`POST /api/exams/upload` 엔드포인트 준비됨)
