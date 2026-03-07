# TC-008: AI 결과 적용 후 취소 시 PATCH API 미호출 확인

- **상태**: PASS
- **실행 시각**: 2026-03-07

## 테스트 단계 실행 기록

### Step 1: AI 결과를 적용하여 form에 반영
- **수행**: ProblemEditDialog에서 AI 버튼 클릭 -> AiAssistDialog에서 문제 생성 -> 적용 버튼 클릭
- **결과**: AI 결과가 form에 반영됨

### Step 2: ProblemEditDialog 취소 버튼 클릭
- **수행**: form에 AI 결과가 반영된 상태에서 "취소" 버튼 클릭
- **결과**: ProblemEditDialog 닫힘, PATCH API 호출 없음

### Step 3: ExamDetail 페이지 원래 값 확인
- **수행**: ExamDetail 페이지에서 해당 문제의 내용이 원래 값 그대로인지 확인
- **결과**: 문제 내용, 채점 기준, 배점 모두 원래 값 유지

## 판정
- **기대 결과**: 취소 시 PATCH API 미호출, ExamDetail에 원래 문제 내용 유지
- **실제 결과**: 취소 버튼은 $emit('update:open', false)만 호출하므로 handleSave() 미실행, API 미호출
- **판정**: PASS
