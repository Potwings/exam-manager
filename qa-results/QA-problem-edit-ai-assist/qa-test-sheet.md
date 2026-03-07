# QA Test Sheet: ProblemEditDialog AI 출제 도우미 기능

- **테스트 일시**: 2026-03-07
- **테스트 대상**: ProblemEditDialog에서 AI 출제 도우미 기능 추가 (ExamDetail 개별 문제 편집 시 AI 문제/채점기준 생성)
- **테스트 환경**: Chromium (Playwright MCP), Frontend http://localhost:5173, Backend http://localhost:8080

## 테스트 케이스

### TC-001: 독립 문제 편집 Dialog에서 AI 버튼 표시 확인
- **분류**: 기능
- **우선순위**: P0-Critical
- **사전 조건**: 관리자 로그인, LLM 가용 상태, 독립 문제가 있는 시험 존재
- **테스트 단계**:
  1. 관리자 로그인 (admin/admin123)
  2. 시험 관리 -> 시험 상세 페이지 이동
  3. 독립 문제의 편집(연필) 아이콘 클릭
  4. ProblemEditDialog에서 "문제 내용" 레이블 우측에 Sparkles(AI) 아이콘 버튼 존재 확인
- **기대 결과**: "문제 내용" 레이블 우측에 amber 색상의 Sparkles 아이콘 버튼이 표시됨
- **실제 결과**: AI 출제 도우미 버튼(title="AI 출제 도우미")이 "문제 내용" 레이블 우측에 정상 표시됨
- **상태**: ✅ 통과
- **비고**: 스크린샷 TC-001/screenshot.png

### TC-002: AI 버튼 클릭 시 AiAssistDialog 열림 확인
- **분류**: 기능
- **우선순위**: P0-Critical
- **사전 조건**: TC-001 완료 (ProblemEditDialog 열린 상태)
- **테스트 단계**:
  1. AI 버튼(Sparkles) 클릭
  2. AiAssistDialog가 열리는지 확인
  3. 입력 필드와 UI 요소가 정상 표시되는지 확인
- **기대 결과**: AiAssistDialog가 ProblemEditDialog 위에 중첩으로 열림, 입력 필드 정상 표시
- **실제 결과**: AiAssistDialog가 ProblemEditDialog 위에 중첩으로 정상 열림, 입력 textarea 및 생성 버튼 정상 표시
- **상태**: ✅ 통과
- **비고**: 스크린샷 TC-002/screenshot.png

### TC-003: AI 결과 적용 후 ProblemEditDialog 폼 반영 확인
- **분류**: 기능
- **우선순위**: P0-Critical
- **사전 조건**: TC-002 완료 (AiAssistDialog 열린 상태)
- **테스트 단계**:
  1. AiAssistDialog에서 문제 생성 요청 입력 (예: "Java 상속 관련 문제 만들어줘")
  2. 생성 버튼 클릭
  3. 결과 생성 대기
  4. "적용" 버튼 클릭
  5. ProblemEditDialog의 문제 내용/채점 기준/콘텐츠 타입/배점 필드에 AI 결과가 반영되었는지 확인
- **기대 결과**: AI가 생성한 문제 내용, 채점 기준, 콘텐츠 타입, 배점이 ProblemEditDialog의 form 필드에 정확히 반영됨
- **실제 결과**: applyAiResult()에 의해 content, answerContent, contentType, score 모두 form에 정확히 반영됨
- **상태**: ✅ 통과
- **비고**: LLM 응답 약 180초 소요. 스크린샷 TC-003/screenshot-ai-result.png, TC-003/screenshot-form-applied.png

### TC-004: AI 결과 적용 후 저장 시 PATCH API 정상 호출
- **분류**: 기능
- **우선순위**: P0-Critical
- **사전 조건**: TC-003 완료 (AI 결과가 form에 반영된 상태)
- **테스트 단계**:
  1. AI 결과가 반영된 상태에서 "저장" 버튼 클릭
  2. PATCH API 호출 확인
  3. ExamDetail 페이지에서 변경 내용 반영 확인
- **기대 결과**: PATCH /api/exams/{examId}/problems/{problemId} 호출 성공, ExamDetail에 변경 내용 즉시 반영
- **실제 결과**: PATCH API 정상 호출, Q1 배점 8->5점, 총점 100->97점으로 변경, ExamDetail 즉시 반영
- **상태**: ✅ 통과
- **비고**: 테스트로 Q1 데이터 영구 변경됨

### TC-005: 그룹 부모 문제 편집 Dialog에서 AI 버튼 미표시 확인
- **분류**: 기능
- **우선순위**: P0-Critical
- **사전 조건**: 그룹 문제가 있는 시험 존재
- **테스트 단계**:
  1. ExamDetail에서 그룹 부모 문제(공통 지문)의 편집 아이콘 클릭
  2. ProblemEditDialog에서 AI 버튼 존재 여부 확인
- **기대 결과**: 그룹 부모 문제 편집 Dialog에서는 AI 버튼이 표시되지 않음
- **실제 결과**: Q13 그룹 부모 편집 Dialog에서 AI 버튼 미표시 확인 (isGroupParent=true)
- **상태**: ✅ 통과
- **비고**: `v-if="!isGroupParent && aiAvailable"` 조건 정상 동작. 스크린샷 TC-005/screenshot.png

### TC-006: 그룹 자식 문제 편집 Dialog에서 AI 버튼 표시 확인
- **분류**: 기능
- **우선순위**: P1-High
- **사전 조건**: 그룹 문제가 있는 시험 존재
- **테스트 단계**:
  1. ExamDetail에서 그룹 자식 문제의 편집 아이콘 클릭
  2. ProblemEditDialog에서 AI 버튼 존재 확인
  3. AI 버튼 클릭 시 AiAssistDialog가 열리는지 확인
- **기대 결과**: 그룹 자식 문제 편집 Dialog에서 AI 버튼이 표시되고 클릭 시 AiAssistDialog가 열림
- **실제 결과**: Q13-1 편집 Dialog에서 AI 버튼 표시, 클릭 시 AiAssistDialog 열림 + 부모 공통지문 배너 표시
- **상태**: ✅ 통과
- **비고**: parentContent prop 전달 정상. 스크린샷 TC-006/screenshot.png

### TC-007: AiAssistDialog 닫기 시 form 값 미변경 확인
- **분류**: 엣지 케이스
- **우선순위**: P1-High
- **사전 조건**: ProblemEditDialog 열린 상태, AI 버튼 클릭 가능
- **테스트 단계**:
  1. ProblemEditDialog에서 현재 form 값 확인 (문제 내용, 채점 기준 등)
  2. AI 버튼 클릭하여 AiAssistDialog 열기
  3. AiAssistDialog를 "적용"하지 않고 닫기 (닫기 버튼 또는 외부 클릭)
  4. ProblemEditDialog의 form 값이 변경되지 않았는지 확인
- **기대 결과**: AiAssistDialog를 적용하지 않고 닫으면 ProblemEditDialog의 form 값이 그대로 유지됨
- **실제 결과**: AiAssistDialog 닫기 후 ProblemEditDialog form 값 변경 없음 확인
- **상태**: ✅ 통과
- **비고**:

### TC-008: AI 결과 적용 후 취소 시 PATCH API 미호출 확인
- **분류**: 엣지 케이스
- **우선순위**: P1-High
- **사전 조건**: AI 결과가 form에 반영된 상태
- **테스트 단계**:
  1. AI 결과를 적용하여 form에 반영된 상태
  2. ProblemEditDialog의 "취소" 버튼 클릭
  3. PATCH API가 호출되지 않는지 확인
  4. ExamDetail 페이지에서 문제 내용이 원래 값 그대로인지 확인
- **기대 결과**: 취소 시 PATCH API 미호출, ExamDetail에 원래 문제 내용 유지
- **실제 결과**: 취소 버튼 클릭 시 handleSave() 미실행, PATCH API 미호출, 원래 값 유지
- **상태**: ✅ 통과
- **비고**:
