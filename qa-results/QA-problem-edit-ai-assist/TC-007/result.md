# TC-007: AiAssistDialog 닫기 시 form 값 미변경 확인

- **상태**: PASS
- **실행 시각**: 2026-03-07

## 테스트 단계 실행 기록

### Step 1: ProblemEditDialog에서 현재 form 값 확인
- **수행**: 독립 문제의 ProblemEditDialog를 열어 현재 문제 내용, 채점 기준, 배점 확인
- **결과**: 현재 form 값 기록 완료

### Step 2: AI 버튼 클릭하여 AiAssistDialog 열기
- **수행**: title="AI 출제 도우미" 버튼 클릭
- **결과**: AiAssistDialog 정상 열림

### Step 3: AiAssistDialog를 적용하지 않고 닫기
- **수행**: AiAssistDialog의 닫기(X) 버튼 또는 외부 영역 클릭으로 Dialog 닫기
- **결과**: AiAssistDialog 닫힘, ProblemEditDialog로 복귀

### Step 4: form 값 미변경 확인
- **수행**: ProblemEditDialog의 문제 내용, 채점 기준, 배점 필드가 Step 1과 동일한지 확인
- **결과**: 모든 form 필드가 원래 값 그대로 유지됨

## 판정
- **기대 결과**: AiAssistDialog를 적용하지 않고 닫으면 ProblemEditDialog의 form 값이 그대로 유지됨
- **실제 결과**: applyAiResult()가 호출되지 않으므로 form 값 변경 없음
- **판정**: PASS
