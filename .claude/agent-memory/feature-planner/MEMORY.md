# Feature Planner Memory

## 프로젝트 아키텍처 핵심 패턴

### 비동기 채점 패턴
- `@Transactional` 메서드에서 데이터 저장 -> `TransactionSynchronization.afterCommit()`로 비동기 트리거
- `@Async` + `@Transactional`은 별도 빈(GradingService)에서 호출해야 AOP 프록시 동작
- 채점 완료 후 `NotificationService.notifyGradingComplete()` SSE 알림

### 채점 중 상태 UI 패턴
- `earnedScore == null`이면 "채점 중" (amber Badge + Loader2 스피너)
- ScoreBoard/ScoreDetail 5초 폴링 -> 완료 감지 시 자동 중단
- `startPollingIfNeeded()` + `stopPolling()` 패턴

### SecurityConfig 엔드포인트 보호 주의점
- `POST /api/submissions`만 permitAll (수험자 제출)
- `/api/submissions/**`는 authenticated -> 재채점 등 신규 엔드포인트 자동 보호
- `/api/exam-sessions/**`는 permitAll -> 같은 하위에 Admin 전용 추가하면 보안 문제
- 순서가 중요: 구체적 매칭이 먼저, 와일드카드가 나중

### ScoreDetail.vue 구조
- `groupedSubmissions` computed: 독립/그룹 문제 분류
- `EditForm` 인라인 컴포넌트 (render function 기반)
- 편집 상태: `editingId` ref로 관리
- 기존 import: lucide-vue-next 아이콘은 개별 import

### API 모듈 패턴
- `src/api/index.js`에 named export 함수
- axios 인스턴스 `api` 사용, baseURL `/api`
- timeout 기본 10000ms, 채점 관련은 30000ms

### ExamSession 생성 동작 (중요)
- 기존: timeLimit == null이면 ExamSession 미생성 -> 시간 제한 없는 시험에서 세션 레코드 없음
- 모니터링 기능 추가 시 반드시 ExamSession 항상 생성으로 변경 필요
- 변경해도 수험자 UI 영향 없음 (응답 형식 동일, remainingSeconds null 유지)

### 관리자 전용 API 경로 분리 원칙
- `/api/exam-sessions/**`가 permitAll이므로, 관리자 전용 기능은 같은 경로 하위에 추가 금지
- 별도 경로 (예: `/api/monitor/**`)로 분리하고 SecurityConfig에서 authenticated 보호

## 기획서 작성 시 확인사항
- 기존 코드의 실제 구현을 반드시 읽고 확인 (CLAUDE.md 설명과 다를 수 있음)
- SecurityConfig 규칙 순서 검증 필수
- GradingService.gradeSubmissionsAsync()는 Answer 유무만 체크하고 채점 수행 (earnedScore null 여부 무관)
- 기존 기획서 형식 참고: `docs/feature-regrade.md` 패턴

## UI 패턴 레퍼런스
- 관리자 페이지 헤더: `text-2xl font-bold tracking-tight` + `text-muted-foreground` 설명
- 시험 선택 드롭다운: ScoreBoard.vue의 native `<select>` + shadcn 스타일 클래스
- 테이블 레이아웃: Card > Table (ExamManage, ScoreBoard 공통)
- 빈 상태: `text-center py-6 text-muted-foreground`
- 헤더 네비게이션: App.vue에서 `authStore.admin && !authStore.admin.initLogin` 조건으로 표시
