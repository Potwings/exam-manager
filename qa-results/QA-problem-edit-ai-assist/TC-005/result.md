# TC-005: 그룹 부모 문제 편집 Dialog에서 AI 버튼 미표시 확인

- **상태**: PASS
- **실행 시각**: 2026-03-07

## 테스트 단계 실행 기록

### Step 1: 그룹 부모 문제 편집 아이콘 클릭
- **수행**: ExamDetail에서 Q13(그룹 부모, 공통 지문) 문제의 편집 아이콘 클릭
- **결과**: ProblemEditDialog 열림, "문제 수정 -- Q13", 설명: "공통 지문을 수정합니다."

### Step 2: AI 버튼 존재 여부 확인
- **수행**: Dialog 스냅샷에서 title="AI 출제 도우미" 버튼 존재 여부 검색
- **결과**: AI 버튼 없음 확인 (v-if="!isGroupParent && aiAvailable" 조건에 의해 숨김)
- **스크린샷**: TC-005/screenshot.png

## 판정
- **기대 결과**: 그룹 부모 문제 편집 Dialog에서는 AI 버튼이 표시되지 않음
- **실제 결과**: isGroupParent=true이므로 AI 버튼 미표시 확인
- **판정**: PASS
