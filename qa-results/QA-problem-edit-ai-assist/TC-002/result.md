# TC-002: AI 버튼 클릭 시 AiAssistDialog 열림 확인

- **상태**: PASS
- **실행 시각**: 2026-03-07

## 테스트 단계 실행 기록

### Step 1: AI 버튼 클릭
- **수행**: ProblemEditDialog에서 title="AI 출제 도우미" 버튼 클릭
- **결과**: AiAssistDialog가 ProblemEditDialog 위에 중첩으로 열림

### Step 2: AiAssistDialog UI 확인
- **수행**: 스냅샷에서 Dialog 내 입력 필드, 생성 버튼 등 UI 요소 확인
- **결과**: 입력 textarea, 생성 버튼 등 정상 표시
- **스크린샷**: TC-002/screenshot.png

## 판정
- **기대 결과**: AiAssistDialog가 ProblemEditDialog 위에 중첩으로 열림, 입력 필드 정상 표시
- **실제 결과**: Dialog 중첩 정상, 입력 필드 및 UI 요소 모두 정상 표시
- **판정**: PASS
