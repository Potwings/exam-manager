# Backend Senior Dev Memory

## 프로젝트 구조
- Backend: `backend/src/main/java/com/exammanager/` (config, controller, service, repository, entity, dto)
- 테스트: `backend/src/test/java/com/exammanager/` (Mockito 단위 테스트 + SpringBoot 통합 테스트)
- 테스트 DB: `exam_scorer_test` (create-drop)

## 테스트 패턴
- Mockito `@ExtendWith(MockitoExtension.class)` + `@InjectMocks` 패턴 사용
- `TransactionSynchronizationManager` 사용하는 서비스 테스트 시:
  - `@BeforeEach`에서 `TransactionSynchronizationManager.initSynchronization()` 호출 필수
  - `@AfterEach`에서 `TransactionSynchronizationManager.clearSynchronization()` 호출 필수

## Entity 주의사항
- Lombok `@Builder`와 필드 초기화 값 함께 쓸 때 `@Builder.Default` 필수
  - 예: `@Builder.Default @Column(nullable = false) private Boolean regrading = false;`
  - 없으면 Builder 생성 시 초기화 값 무시되어 null 발생

## 비동기 채점 패턴
- `SubmissionService` (트랜잭션) → `afterCommit` → `GradingService` (@Async, 별도 빈)
- @Async는 같은 클래스 내부 호출 시 AOP 프록시 우회하므로 반드시 다른 빈에서 호출
- 재채점도 동일 패턴: `regrading=true` 설정 → afterCommit → GradingService.regradeXxxAsync()

## SecurityConfig 보호 규칙 요약
- `POST /api/submissions`만 `.permitAll()`, 나머지 `/api/submissions/**`는 `.authenticated()`
- 재채점 엔드포인트는 별도 SecurityConfig 변경 없이 자동으로 Admin 인증 필요

## API 응답에서 Map.of() 주의
- `Map.of()`는 null 값 시 NPE 발생
- null 가능성 있으면 `HashMap`으로 수동 구성

## LlmClient 멀티턴 구조 (2026-03)
- `LlmClient` 인터페이스: `chat(String, String)` (단일턴, GradingService) + `chat(List<ChatMessage>)` (멀티턴, AiAssistService)
- 멀티턴은 `default` 메서드로 선언 → 기존 코드 하위 호환 유지
- OllamaClient, OpenAiClient 모두 `executeChat(List<Map>)` 공통 메서드 추출 패턴
- AiAssistService: system + 필터링된 history + current user 메시지 조립
- 보안: conversationHistory에서 "user"/"assistant" role만 허용, "system" 주입 차단
