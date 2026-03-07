# TC-006: 그룹 자식 문제 편집 Dialog에서 AI 버튼 표시 확인

- **상태**: PASS
- **실행 시각**: 2026-03-07

## 테스트 단계 실행 기록

### Step 1: 그룹 자식 문제 편집 아이콘 클릭
- **수행**: ExamDetail에서 Q13-1(그룹 자식) 문제의 편집 아이콘 클릭
- **결과**: ProblemEditDialog 열림, "문제 수정 -- Q13-1"

### Step 2: AI 버튼 존재 확인
- **수행**: Dialog 스냅샷에서 title="AI 출제 도우미" 버튼 존재 여부 확인
- **결과**: AI 버튼 정상 표시됨 (isGroupParent=false이므로 v-if 조건 통과)
- **스크린샷**: TC-006/screenshot.png

### Step 3: AI 버튼 클릭 시 AiAssistDialog 열림 확인
- **수행**: AI 버튼 클릭
- **결과**: AiAssistDialog 열림, 부모 공통 지문 안내 배너 표시 확인 (parentContent prop 전달 정상)

## 판정
- **기대 결과**: 그룹 자식 문제 편집 Dialog에서 AI 버튼이 표시되고 클릭 시 AiAssistDialog가 열림
- **실제 결과**: AI 버튼 표시 + AiAssistDialog 정상 열림 + 부모 지문 배너 표시
- **판정**: PASS
