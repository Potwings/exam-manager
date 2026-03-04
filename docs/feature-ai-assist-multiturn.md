# AI 출제 도우미 멀티턴 대화 개선

## 개요

AI 출제 도우미의 대화 방식을 **단건 요청**에서 **멀티턴 대화**로 개선하여, LLM이 이전 대화 전체를 맥락으로 인식하도록 변경했다.

### 변경 전 (단건)
```
system + user("[현재 문제]...[관리자 지시]...")
```
- 최신 1개 결과만 user prompt 텍스트에 `[현재 문제]`/`[현재 채점 기준]`으로 끼워넣음
- 3회 이상 대화 시 초기 맥락 유실

### 변경 후 (멀티턴)
```
system + user(1차) + assistant(1차) + user(2차) + assistant(2차) + user(3차)
```
- 전체 대화 히스토리를 LLM Chat API의 messages 배열에 user/assistant 역할로 전달
- LLM이 대화의 전체 흐름을 파악하여 더 정확한 개선 응답 생성

---

## 변경 파일 목록

| 파일 | 변경 유형 | 설명 |
|------|-----------|------|
| `backend/.../dto/ChatMessage.java` | **신규** | 멀티턴 대화 메시지 DTO (role + content) |
| `backend/.../service/LlmClient.java` | 수정 | `chat(List<ChatMessage>)` default 메서드 추가 |
| `backend/.../service/OllamaClient.java` | 수정 | 멀티턴 chat 구현 + `executeChat()` 공통 추출 |
| `backend/.../service/OpenAiClient.java` | 수정 | 멀티턴 chat 구현 + `executeChat()` 공통 추출 |
| `backend/.../dto/AiAssistRequest.java` | 수정 | `currentContent`/`currentAnswer` 제거, `conversationHistory` 추가, `@NotBlank` 검증 |
| `backend/.../service/AiAssistService.java` | 수정 | `buildMessages()` 멀티턴 조립, 보안 필터링 (role + content null/blank) |
| `backend/.../controller/AiAssistController.java` | 수정 | `@Valid` 추가 |
| `backend/.../service/AiAssistServiceTest.java` | **신규** | 15개 단위 테스트 |
| `frontend/src/components/AiAssistDialog.vue` | 수정 | 전체 대화 히스토리 `conversationHistory`로 전송 |
| `CLAUDE.md` | 수정 | 멀티턴 구조 문서화, DTO 테이블 업데이트, TODO 체크 |

---

## API 변경

### `POST /api/ai-assist/generate`

**Request Body:**
```json
{
  "instruction": "문제를 좀 더 어렵게 해줘",
  "parentContent": "공통 지문 (그룹 문제 시, optional)",
  "contentType": "TEXT",
  "score": 5,
  "conversationHistory": [
    { "role": "user", "content": "Java 상속 문제 만들어줘" },
    { "role": "assistant", "content": "{\"problemContent\":\"...\",\"answerContent\":\"...\",\"score\":5}" }
  ]
}
```

| 필드 | 변경 |
|------|------|
| `currentContent` | **제거** |
| `currentAnswer` | **제거** |
| `conversationHistory` | **추가** — `List<{role, content}>`, nullable |
| `instruction` | `@NotBlank` 검증 추가 |

**Response Body:** 변경 없음

---

## 백엔드 상세

### ChatMessage DTO
```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessage {
    private String role;
    private String content;
}
```

### LlmClient 인터페이스
```java
// 기존 (유지 — GradingService 하위 호환)
JsonNode chat(String systemPrompt, String userPrompt);

// 신규 (멀티턴 — AiAssistService에서 사용)
default JsonNode chat(List<ChatMessage> messages) {
    throw new UnsupportedOperationException("멀티턴 chat 미구현");
}
```

### OllamaClient / OpenAiClient
- 기존 `chat(String, String)` 내부 로직을 `executeChat(List<Map<String,String>>)` 공통 메서드로 추출
- 단건/멀티턴 양쪽에서 `executeChat()` 재사용
- 멀티턴: `ChatMessage` 리스트를 `List<Map<String,String>>`으로 변환하여 API body에 전달

### AiAssistService — 메시지 조립 순서
```
1) system    — SYSTEM_PROMPT (출제 전문가 역할 + JSON 출력 형식)
2) history   — conversationHistory (role 필터링: user/assistant만 허용)
3) user      — 현재 턴 (parentContent + contentType + score + instruction)
```

### 보안 필터링
- `ALLOWED_ROLES = Set.of("user", "assistant")` — system 역할 주입 차단
- content가 null 또는 blank인 메시지 필터링 — `Map.of()` NPE 방지

---

## 프론트엔드 상세

### 핵심 변경: `handleGenerate()`

**기존:**
```javascript
if (latestResult.value) {
  data.currentContent = latestResult.value.problemContent
  data.currentAnswer = latestResult.value.answerContent
}
```

**변경:**
```javascript
const conversationHistory = []

// 1) 기존 문제 → 초기 assistant 메시지
if (initialContent.value) {
  conversationHistory.push({
    role: 'assistant',
    content: JSON.stringify(initialContent.value)
  })
}

// 2) 이전 대화 → user/assistant 쌍
const previousItems = history.value.slice(0, -1)
for (const item of previousItems) {
  conversationHistory.push({ role: 'user', content: item.instruction })
  if (item.result) {
    conversationHistory.push({
      role: 'assistant',
      content: JSON.stringify(item.result)
    })
  }
}

if (conversationHistory.length > 0) {
  data.conversationHistory = conversationHistory
}
```

### 새로운 ref: `initialContent`
- Dialog 열릴 때 기존 문제 내용을 저장
- 첫 요청 시 assistant 메시지로 conversationHistory 앞에 추가
- dismiss(X 버튼) 시 null로 초기화되어 이후 요청에서 제외
- `latestResult`와 분리 — latestResult는 UI 표시 전용

### QA에서 발견된 버그 수정
- **문제**: `history.value.push(entry)` 후 Vue reactive proxy로 감싸져 `item === entry` 비교가 실패
- **수정**: `history.value.slice(0, -1)`로 마지막 항목 제외 (인덱스 기반 접근)

---

## 테스트

### 단위 테스트 (AiAssistServiceTest — 15개)

| 테스트 | 검증 내용 |
|--------|-----------|
| 대화이력 없으면 system+user만 구성 | null history |
| 빈 대화이력이면 system+user만 구성 | empty list |
| 대화이력 있으면 system→history→user 순서 | 멀티턴 조립 |
| system 역할 필터링 | system prompt injection 차단 |
| null 역할 필터링 | 잘못된 메시지 방어 |
| function/tool 역할 필터링 | 허용되지 않는 역할 |
| **content null/blank 필터링** | Map.of() NPE 방지 |
| parentContent 있으면 [보기] 태그 | 그룹 문제 지문 |
| parentContent 없으면 [보기] 미포함 | null 방어 |
| parentContent 빈 문자열이면 미포함 | blank 방어 |
| 멀티턴 chat 호출 + 응답 파싱 | 정상 플로우 |
| 대화이력과 함께 멀티턴 chat 호출 | history 포함 플로우 |
| AI 비가용 시 예외 | 서비스 비가용 방어 |
| AI 응답 null 시 예외 | LLM 실패 방어 |
| score 미포함 시 요청 score 사용 | 폴백 로직 |

### 빌드 결과
- `./gradlew.bat test` — **BUILD SUCCESSFUL** (전체 통과)

---

## 코드 리뷰 결과

| 등급 | 건수 | 내용 | 조치 |
|------|------|------|------|
| Critical | 1 | ChatMessage content null → Map.of() NPE | **수정 완료** — content null/blank 필터링 추가 |
| Warning | 1 | instruction null 시 프롬프트에 "null" 삽입 | **수정 완료** — @NotBlank + @Valid 추가 |
| Warning | 1 | LlmClient default 메서드가 예외를 던지는 구조 | 보류 — 구현체 2개뿐이므로 현재로 충분 |
| Warning | 1 | 대화 히스토리 크기 제한 없음 | 보류 — Dialog 닫으면 초기화, 긴 대화 드뭄 |
| Info | 1 | ChatMessage @Builder 누락 | **수정 완료** |
| Info | 1 | 빈 @BeforeEach 메서드 | **수정 완료** — 제거 |

---

## 하위 호환성

- `GradingService`의 기존 `chat(String, String)` 호출 — **영향 없음** (메서드 시그니처 유지)
- `conversationHistory`가 null/빈 배열 → 기존과 동일한 단건 동작
- 기존 프론트엔드에서 `currentContent`/`currentAnswer` 전송해도 서버에서 무시 (필드 제거로 역직렬화 시 자동 무시)
