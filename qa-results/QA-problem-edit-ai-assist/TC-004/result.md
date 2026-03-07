# TC-004: AI 결과 적용 후 저장 시 PATCH API 정상 호출

- **상태**: PASS
- **실행 시각**: 2026-03-07

## 테스트 단계 실행 기록

### Step 1: 저장 버튼 클릭
- **수행**: AI 결과가 반영된 ProblemEditDialog에서 "저장" 버튼 클릭
- **결과**: PATCH /api/exams/11/problems/{problemId} API 호출 성공, Dialog 자동 닫힘

### Step 2: ExamDetail 페이지 반영 확인
- **수행**: ExamDetail 페이지에서 Q1 문제의 변경 내용 확인
- **결과**:
  - Q1 헤딩: "Q1 8점" -> "Q1 5점" 으로 변경 확인
  - Q1 문제 내용: Java 상속 관련 문제로 변경 확인
  - Q1 채점 기준: 상속 관련 루브릭으로 변경 확인
  - 총점: "14문제 / 총 100점" -> "14문제 / 총 97점" 으로 변경 확인

## 판정
- **기대 결과**: PATCH /api/exams/{examId}/problems/{problemId} 호출 성공, ExamDetail에 변경 내용 즉시 반영
- **실제 결과**: PATCH API 정상 호출, handleProblemSaved의 Object.assign으로 즉시 반영, totalScore computed도 동적 갱신
- **판정**: PASS

## 비고
- TC-004 실행으로 Q1 문제 데이터가 영구 변경됨 (OOP 4가지 특징 8점 -> Java 상속 5점, 총점 100->97)
- 테스트 데이터 아티팩트로서 후속 테스트 시 주의 필요
