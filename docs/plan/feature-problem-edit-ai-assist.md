# ProblemEditDialog AI 출제 도우미 연동

## 개요
- **목적**: ExamDetail 페이지에서 개별 문제를 수정할 때 AI 출제 도우미를 활용하여 문제 내용과 채점 기준을 AI로 생성/개선할 수 있도록 한다.
- **대상 사용자**: 관리자
- **우선순위**: P2 (개선) -- 기존 기능(ProblemEditDialog + AiAssistDialog)을 조합하는 UX 개선

## 현재 상태

### 관련 기존 기능 분석

1. **ExamCreate.vue의 AI 출제 도우미** (완전 구현)
   - `checkAiStatus()` API로 LLM 가용 여부 확인 -> `aiAvailable` ref
   - 독립 문제 헤더에 Sparkles 아이콘 버튼 (조건: `aiAvailable && !p.isGroup`)
   - 그룹 자식 문제 헤더에 Sparkles 아이콘 버튼 (조건: `aiAvailable`)
   - `openAiDialog(problem, parentProblem?)` -> AiAssistDialog 열기
   - `applyAiResult(result)` -> `problemContent`, `answerContent`, `contentType`, `score`를 폼에 반영

2. **AiAssistDialog.vue** (재사용 가능 컴포넌트)
   - Props: `open`, `problem` (Object), `parent` (Object)
   - Emits: `update:open`, `apply(result)`
   - `problem`에서 기존 내용을 읽어 초기 assistant 메시지로 사용 (멀티턴 맥락)
   - `parent?.content`가 있으면 공통 지문 배너 표시 + 프롬프트에 포함
   - `apply` 이벤트 시 `{ problemContent, answerContent, contentType, score }` 전달

3. **ProblemEditDialog.vue** (현재 AI 기능 없음)
   - Props: `open`, `problem`, `examId`, `isGroupParent`, `parentProblemNumber`
   - Emits: `update:open`, `saved(updatedProblem)`
   - 그룹 부모: 콘텐츠 타입 + 문제 내용만 편집 (채점 기준/배점 숨김)
   - 독립/그룹 자식: 콘텐츠 타입, 코드 에디터, 문제 내용, 채점 기준, 배점 전 필드 편집
   - `form` ref로 편집 상태 관리 -> `handleSave()`로 PATCH API 호출

### 현재의 한계점
- ExamDetail 페이지에서 문제를 수정할 때 AI 도움을 받으려면 ExamCreate(수정 모드)로 이동해야 함
- 제출 결과가 있는 시험은 PUT 수정이 불가하므로 ExamCreate에서 AI를 사용할 수 없고, ProblemEditDialog(PATCH)만 사용 가능 -- 이때 AI 기능이 없음
- 즉, **제출 결과가 있는 시험의 문제 개선에 AI를 활용할 수 없는** 갭이 존재

## 기능 요구사항

### 필수 (Must-have)
1. ProblemEditDialog에 AI 출제 도우미 버튼(Sparkles 아이콘) 추가
2. 버튼 클릭 시 AiAssistDialog 열기
3. AI 생성 결과를 ProblemEditDialog의 `form`에 반영 (`apply` 이벤트 처리)
4. LLM 미가용 시 AI 버튼 숨김 (`checkAiStatus()` API 활용)
5. 그룹 부모 문제에서는 AI 버튼 미표시 (채점 기준/배점이 없으므로)
6. 그룹 자식 문제에서는 부모 공통 지문을 AiAssistDialog에 전달

### 선택 (Nice-to-have)
- 없음 (기존 컴포넌트 재사용이므로 범위가 명확)

## UX 설계

### 사용자 플로우 (step-by-step)
1. 관리자가 ExamDetail 페이지에서 문제의 편집(SquarePen) 아이콘 클릭
2. ProblemEditDialog가 열림
3. "문제 내용" Label 우측에 Sparkles 아이콘 버튼 표시 (LLM 가용 시만)
4. Sparkles 버튼 클릭 -> AiAssistDialog가 ProblemEditDialog 위에 열림
5. AI에게 문제 생성/개선 요청 (기존 문제 내용이 있으면 맥락으로 전달)
6. AI 결과 확인 후 "적용" 버튼 클릭
7. AiAssistDialog 닫히고 ProblemEditDialog의 form 필드에 결과 반영:
   - `form.content` <- `result.problemContent`
   - `form.answerContent` <- `result.answerContent`
   - `form.contentType` <- `result.contentType`
   - `form.score` <- `result.score`
8. 관리자가 반영된 내용을 확인/수정 후 "저장" 클릭

### 화면 구성

#### AI 버튼 위치
- **배치**: "문제 내용" Label 우측에 Sparkles 아이콘 버튼 배치
- **근거**: AI 출제 도우미의 주요 산출물은 문제 내용(problemContent)이며, 채점 기준/배점/콘텐츠 타입은 문제 내용에 따라 함께 생성되는 부수 필드이다. 따라서 "문제 내용" 옆에 AI 버튼을 두는 것이 기능의 진입점으로서 가장 직관적이다.
- **기존 패턴 참고**: 현재 "문제 내용" Label 우측에는 마크다운 "미리보기/편집" 토글 버튼이 이미 존재한다 (`contentType === 'MARKDOWN'` 조건). AI 버튼은 이 토글 버튼 왼쪽에 배치하여, Label 행의 우측 액션 영역에 자연스럽게 추가한다.
- **조건**: `!isGroupParent && aiAvailable` 일 때만 표시
- **스타일**: `variant="ghost"`, `size="sm"`, Sparkles 아이콘 `text-amber-500` (ExamCreate.vue와 동일)
- **title**: "AI 출제 도우미"

```
┌─────────────────────────────────────┐
│ 문제 수정 -- Q5                      │
│ 문제 내용과 채점 기준을 수정합니다.     │
│                                     │
│ [콘텐츠 타입] ○ 텍스트  ○ 마크다운    │
│ [코드 에디터] [Java v]               │
│                                     │
│ 문제 내용             [✨] [미리보기] │  <-- AI 버튼 (문제 내용 Label 우측)
│ ┌─────────────────────────────────┐ │
│ │ (textarea)                      │ │
│ └─────────────────────────────────┘ │
│                                     │
│ 채점 기준                            │
│ ┌─────────────────────────────────┐ │
│ │ (textarea)                      │ │
│ └─────────────────────────────────┘ │
│                                     │
│ 배점                                 │
│ [___]                               │
│                                     │
│                    [취소]  [저장]     │
└─────────────────────────────────────┘
```

### 기존 UI 패턴과의 일관성 고려사항
- Sparkles 아이콘 색상 `text-amber-500`은 ExamCreate.vue와 동일
- `variant="ghost"`, `size="sm"` 버튼 스타일은 ExamCreate.vue의 AI 버튼과 동일
- AiAssistDialog에 전달하는 `problem` 객체 형태를 ExamCreate.vue와 맞춤 (`content`, `answerContent`, `contentType`, `score` 필드)

## 기술 구현 방안

### Frontend

#### ProblemEditDialog.vue 변경사항

1. **AI 가용 상태 확인**
   - `onMounted` 또는 `watch(open)` 시점에 `checkAiStatus()` 호출
   - `aiAvailable` ref로 상태 관리
   - Dialog가 열릴 때마다 매번 호출하지 않고, 컴포넌트 마운트 시 1회 확인 (ExamCreate.vue와 동일 패턴)

   > **설계 결정**: `onMounted`가 아닌 `watch(open)` + 최초 1회만 호출 방식을 사용한다. ProblemEditDialog는 ExamDetail 내에 항상 마운트되어 있는 `v-model:open` 패턴이므로 `onMounted`는 페이지 로드 시 1회 실행된다. 이 시점에 호출하면 된다. 다만, Dialog를 열기 전에 이미 상태를 알고 있어야 하므로 `onMounted`가 적절하다.

2. **AiAssistDialog 임포트 및 마운트**
   - `import AiAssistDialog from '@/components/AiAssistDialog.vue'`
   - ProblemEditDialog template 내 (Dialog 바깥 또는 안쪽에) AiAssistDialog 추가
   - `aiDialogOpen` ref로 열림/닫힘 제어

3. **AiAssistDialog에 전달할 `problem` 객체 구성**
   - AiAssistDialog는 `problem.content`, `problem.answerContent`, `problem.contentType`, `problem.score`를 읽음
   - ProblemEditDialog의 `form` ref 값을 그대로 전달하면 됨 (동일 필드명)
   - `aiProblem` computed로 `form` 값을 AiAssistDialog가 기대하는 형태로 매핑

4. **AiAssistDialog에 전달할 `parent` 객체 구성**
   - 그룹 자식 문제일 때 부모 공통 지문을 전달해야 함
   - **문제**: 현재 ProblemEditDialog는 `parentProblemNumber`만 받고 부모 `content`는 받지 않음
   - **해결**: ProblemEditDialog에 새 prop `parentContent` (String) 추가
   - ExamDetail.vue에서 그룹 자식 문제의 편집 시 부모의 `content`를 함께 전달

5. **apply 이벤트 처리**
   - AiAssistDialog의 `apply` 이벤트로 받은 결과를 `form`에 반영:
     ```js
     function applyAiResult(result) {
       form.value.content = result.problemContent
       form.value.answerContent = result.answerContent
       form.value.contentType = result.contentType
       form.value.score = result.score
     }
     ```

#### ExamDetail.vue 변경사항

1. **ProblemEditDialog에 `parentContent` prop 전달**
   - `openEditDialog(child, false, problem.problemNumber)` 호출 시 부모의 `content`도 `editTarget`에 저장
   - `editTarget = { problem, isGroupParent, parentProblemNumber, parentContent }`
   - ProblemEditDialog에 `:parent-content="editTarget?.parentContent"` 바인딩 추가

### Backend
- **변경 없음**: 기존 `GET /api/ai-assist/status` 및 `POST /api/ai-assist/generate` API를 그대로 사용

### 고려사항

#### 기존 기능과의 호환성
- ProblemEditDialog의 기존 Props/Emits에 `parentContent` 1개만 추가 -- 기존 호출부(ExamDetail.vue)에서 해당 prop을 전달하지 않으면 `undefined`로 동작하므로 하위 호환됨
- AiAssistDialog는 변경 없이 그대로 재사용

#### 보안
- AI 관련 API(`/api/ai-assist/**`)는 이미 authenticated 보호 -- 추가 보안 조치 불필요
- ProblemEditDialog 자체가 Admin 페이지(ExamDetail) 내에서만 사용됨

#### 성능 영향
- `checkAiStatus()` API 호출 1회 추가 (ProblemEditDialog 마운트 시) -- 경량 GET 요청, 무시할 수준
- AiAssistDialog 컴포넌트 lazy import 미필요 (이미 ExamCreate에서도 eager import 패턴)

#### 에러 처리
- `checkAiStatus()` 실패 시 `aiAvailable = false` (AI 버튼 숨김, ExamCreate.vue와 동일)
- AI 생성 실패는 AiAssistDialog 내부에서 처리됨 (기존 로직 그대로)

#### Dialog 중첩 (Nested Dialog)
- ProblemEditDialog(Dialog) 위에 AiAssistDialog(Dialog)가 열리는 중첩 구조
- shadcn-vue/Radix UI Dialog는 중첩을 지원함 -- 자동으로 focus trap과 overlay가 관리됨
- AiAssistDialog를 ProblemEditDialog의 `<DialogContent>` **바깥**, `<Dialog>` **안쪽**에 배치하여 중첩 동작 보장

## API 계약 (Backend <-> Frontend)

기존 API를 그대로 재사용하며, 변경 사항 없음.

| Method | Path | Request Body | Response Body | 비고 |
|--------|------|-------------|---------------|------|
| GET | `/api/ai-assist/status` | - | `{ available: boolean }` | 기존 API 재사용 |
| POST | `/api/ai-assist/generate` | `AiAssistRequest` | `AiAssistResponse` | 기존 API 재사용 |
| PATCH | `/api/exams/{examId}/problems/{problemId}` | `ProblemUpdateRequest` | `ProblemResponse` | 기존 API (AI 적용 후 저장) |

**Backend 변경 없음** -- 신규 엔드포인트, DTO, Entity, 스키마 변경이 필요하지 않습니다.

## 작업 분해 (Task Breakdown)

모든 작업이 프론트엔드 전용이며, 백엔드 변경이 없습니다.

1. `[Frontend]` **ProblemEditDialog에 AI 가용 상태 확인 추가** -- `checkAiStatus()` 호출 + `aiAvailable` ref 관리
2. `[Frontend]` **ProblemEditDialog에 `parentContent` prop 추가** -- 그룹 자식 문제의 부모 지문 전달용
3. `[Frontend]` **ProblemEditDialog에 AiAssistDialog 연동** -- import, template 추가, `aiDialogOpen` ref, `aiProblem` computed, `applyAiResult` 핸들러
4. `[Frontend]` **ProblemEditDialog에 AI 버튼(Sparkles) 추가** -- "문제 내용" Label 우측 배치 (미리보기 토글 왼쪽), 조건부 표시 (`!isGroupParent && aiAvailable`)
5. `[Frontend]` **ExamDetail.vue에서 `parentContent` 전달** -- `openEditDialog()` 호출 시 부모 `content`를 `editTarget`에 포함, ProblemEditDialog에 바인딩

> 작업 1~5는 모두 프론트엔드이며 순차 의존성이 있으므로 **순서대로 진행**합니다.
> 백엔드 작업 없음 -- 프론트엔드 단독 작업 가능.

## QA 수용 기준 (Acceptance Criteria)

### 기본 동작
- [ ] ExamDetail 페이지에서 독립 문제의 편집 Dialog를 열면, LLM 가용 시 "문제 내용" Label 우측에 Sparkles(AI) 아이콘 버튼이 표시된다
- [ ] AI 버튼 클릭 시 AiAssistDialog가 열린다
- [ ] AiAssistDialog에서 문제 생성 요청 후 "적용" 클릭 시, ProblemEditDialog의 문제 내용/채점 기준/콘텐츠 타입/배점 필드에 AI 결과가 반영된다
- [ ] AI 결과 적용 후 "저장" 클릭 시 PATCH API가 정상 호출되고, ExamDetail 페이지에 변경 내용이 반영된다

### 그룹 문제 처리
- [ ] 그룹 부모 문제(공통 지문)의 편집 Dialog에서는 AI 버튼이 표시되지 않는다
- [ ] 그룹 자식 문제의 편집 Dialog에서 AI 버튼이 표시된다
- [ ] 그룹 자식 문제에서 AI 버튼 클릭 시, AiAssistDialog에 "공통 지문(보기)이 프롬프트에 포함됩니다" 배너가 표시된다

### LLM 미가용 시
- [ ] LLM 미가용 상태(`/api/ai-assist/status` -> `available: false`)에서는 ProblemEditDialog에 AI 버튼이 표시되지 않는다

### 기존 문제 내용 맥락 전달
- [ ] 이미 문제 내용이 있는 상태에서 AI 버튼을 클릭하면, AiAssistDialog에 "이전에 작성한 문제 내용 포함" 배너가 표시된다
- [ ] 배너의 X 버튼으로 기존 내용을 제외하고 새로 생성할 수 있다

### 엣지 케이스
- [ ] AiAssistDialog에서 "적용"하지 않고 닫으면(ESC/외부 클릭) ProblemEditDialog의 form 값이 변경되지 않는다
- [ ] AI 결과 적용 후에도 "취소" 버튼으로 ProblemEditDialog를 닫으면 PATCH API가 호출되지 않는다 (저장 전 취소 가능)
- [ ] AI 결과로 contentType이 변경된 경우 (TEXT -> MARKDOWN 또는 반대), ProblemEditDialog의 콘텐츠 타입 라디오 버튼이 올바르게 업데이트된다

### UI 일관성
- [ ] AI 버튼의 아이콘 색상(`text-amber-500`)과 스타일이 ExamCreate.vue의 AI 버튼과 동일하다
- [ ] ProblemEditDialog와 AiAssistDialog의 중첩 Dialog가 정상 동작한다 (focus trap, overlay, ESC 키 순서)

## 리스크 및 의존성

| 항목 | 설명 | 대응 |
|------|------|------|
| Dialog 중첩 렌더링 | shadcn-vue Dialog 중첩 시 z-index/overlay 이슈 가능 | Radix UI 기본 중첩 지원으로 대부분 문제 없음. QA에서 확인 |
| `parentContent` prop 추가 | ExamDetail 외에 ProblemEditDialog를 사용하는 곳이 있는지 확인 필요 | 현재 ExamDetail.vue에서만 사용 (Grep 확인 완료). optional prop이므로 하위 호환 |
| LLM 가용 여부 API 타이밍 | ProblemEditDialog 마운트 시 1회 호출 -- Dialog 열기 전에 상태 확정 필요 | `onMounted`에서 호출하므로 페이지 로드 시 이미 확인 완료 |
