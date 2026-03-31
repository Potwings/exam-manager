# Code Reviewer Agent Memory

## 자주 발견되는 패턴

### Map.of() + null 위험 (Critical 빈도 높음)
- CLAUDE.md에 명시된 규칙: `Map.of()`는 null 값 시 NPE 발생
- `ChatMessage.content`가 null이면 `Map.of("role", role, "content", null)` → NPE
- 체크 포인트: LLM 클라이언트에서 외부 입력(DTO 필드)을 Map.of()에 직접 넣는 코드
- 수정 위치: 서비스 레이어의 필터링 로직에서 role + content 둘 다 null 검증 추가

### Bean Validation 누락 (Warning 빈도 높음)
- Controller에 `@Valid` 없으면 `@NotBlank` 어노테이션이 무효
- AiAssistRequest처럼 String 필드가 null이면 프롬프트에 "null" 문자열이 삽입됨
- 체크 포인트: 새 엔드포인트 추가 시 Controller `@Valid` + DTO 필드 Validation 쌍으로 확인

### 멀티턴 대화 히스토리 크기 제한 (Warning)
- 프론트엔드에서 전체 이력을 매 요청마다 전송하면 토큰/용량 기하급수 증가
- 최근 3~4턴만 슬라이싱하는 것이 일반적인 해결책
- 백엔드에서도 수신 메시지 수 상한 방어 계층 필요

### interface default + 예외 안티패턴 (Warning)
- `default` 메서드에서 UnsupportedOperationException을 던지면 사실상 추상 메서드
- 구현 누락을 컴파일 타임에 발견할 수 없어 런타임 장애로 이어짐
- 대안: 추상 메서드로 선언하거나 별도 인터페이스로 분리

## 보안 패턴 (양호 사례)
- conversationHistory에서 role 필터링으로 system prompt injection 차단: `ALLOWED_ROLES = Set.of("user", "assistant")`
- @ExceptionHandler에서 e.getMessage() 대신 고정 메시지 반환 패턴 프로젝트 전반 준수 확인됨

## 프로젝트 특화 위험 지점

### 동시성 위험
- `activateExam()`: 기존 비활성화 → 새 활성화 사이에 다른 요청이 끼면 2개 동시 활성 가능 (DB-level lock 미적용)
- `ExamSession` find-or-create: `DataIntegrityViolationException` 핸들링으로 방어 중 — 새 find-or-create 패턴 추가 시 동일 방어 필수

### 리소스/성능 위험
- `Problem.children` (@OneToMany EAGER): 자식이 불필요한 목록 조회에서도 항상 로드됨
- `LlmClient.chat()` 120초 타임아웃: `@Async` 스레드에서 호출하므로 기본 스레드풀(8개) 소진 위험 — 대량 동시 채점 시 주의
- `NotificationService`의 `CopyOnWriteArrayList<SseEmitter>`: 관리자 다수 접속 시 메모리/동시성 확인 필요
- `GradingService.gradeSubmissionsAsync()`: 트랜잭션 안에서 LLM 외부 호출 — DB 커넥션 장시간 점유

### 보안 위험
- Public 엔드포인트(`POST /api/submissions`, `/api/exam-sessions`)에서 examineeId를 요청 body에서 받음 — 서버 측 소유권 검증 없이 ID만으로 동작 (현재 아키텍처상 수험자는 세션 인증 미사용)
- `localStorage`에 수험자 정보/답안 저장 — XSS 시 노출 위험 (현재 마크다운 렌더링에 v-html 사용 중)
