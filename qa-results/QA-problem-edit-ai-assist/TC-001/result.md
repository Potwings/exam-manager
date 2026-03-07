# TC-001: 독립 문제 편집 Dialog에서 AI 버튼 표시 확인

- **상태**: PASS
- **실행 시각**: 2026-03-07

## 테스트 단계 실행 기록

### Step 1: 관리자 로그인
- **수행**: http://localhost:5173/admin/login 접속 후 admin/admin123 입력하여 로그인
- **결과**: 로그인 성공, /admin/scores 페이지로 리다이렉트

### Step 2: 시험 상세 페이지 이동
- **수행**: /admin/exams 이동 후 시험 목록에서 시험 상세 페이지(examId=11) 이동
- **결과**: ExamDetail 페이지 정상 로드, 14문제 목록 표시

### Step 3: 독립 문제의 편집 아이콘 클릭
- **수행**: Q1 문제의 편집(SquarePen) 아이콘 클릭
- **결과**: ProblemEditDialog 정상 열림

### Step 4: AI 버튼 존재 확인
- **수행**: Dialog 스냅샷에서 "문제 내용" 레이블 우측의 Sparkles 아이콘 버튼 확인
- **결과**: title="AI 출제 도우미" 버튼이 표시됨
- **스크린샷**: TC-001/screenshot.png

## 판정
- **기대 결과**: "문제 내용" 레이블 우측에 amber 색상의 Sparkles 아이콘 버튼이 표시됨
- **실제 결과**: AI 출제 도우미 버튼이 정상 표시됨
- **판정**: PASS
