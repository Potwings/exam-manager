# QA Test Sheet: ExamDetail 개별 문제 수정 기능

- **Date**: 2026-02-25
- **Tester**: Claude QA
- **Target URL**: http://localhost:5173
- **Feature**: ExamDetail 페이지에서 ProblemEditDialog를 통한 개별 문제 제자리 수정 (in-place PATCH)

## Test Cases

| ID | Category | Test Name | Precondition | Steps | Expected Result | Result | Note |
|----|----------|-----------|--------------|-------|-----------------|--------|------|
| TC-01-001 | UI 표시 | 독립 문제 편집 버튼 표시 | 시험 상세 페이지 진입 | 독립 문제 카드 확인 | 카드 헤더 우측에 SquarePen 편집 아이콘 버튼 표시 | Pass | Q1~Q12, Q14 각 카드 우측에 편집 아이콘 확인 |
| TC-01-002 | UI 표시 | 그룹 부모 편집 버튼 표시 | 그룹 문제가 있는 시험 상세 | 그룹 문제 카드 확인 | 그룹 부모 카드 헤더 우측에 편집 버튼 표시 | Pass | Q13 그룹 카드 헤더 우측에 편집 버튼 확인 |
| TC-01-003 | UI 표시 | 그룹 자식 편집 버튼 표시 | 그룹 문제가 있는 시험 상세 | 하위 문제 영역 확인 | 각 하위 문제 헤더 우측에 편집 버튼 표시 | Pass | Q13-1~Q13-5 각 하위 문제 우측에 편집 버튼 확인 |
| TC-01-004 | Dialog 열기 | 독립 문제 편집 Dialog 열기 | 시험 상세 페이지 | 독립 문제 편집 버튼 클릭 | Dialog 열림, "문제 수정 — Q{번호}" 제목, content/contentType/codeEditor/채점기준/배점 필드 모두 표시 | Pass | Q1 편집 Dialog: 제목 "문제 수정 — Q1", 모든 필드 표시 |
| TC-01-005 | Dialog 열기 | 그룹 부모 편집 Dialog 열기 | 그룹 문제가 있는 시험 상세 | 그룹 부모 편집 버튼 클릭 | Dialog 열림, "공통 지문을 수정합니다" 설명, 채점기준/배점/코드에디터 필드 숨김 | Pass | Q13 부모 Dialog: "공통 지문을 수정합니다" 설명, 채점기준/배점/코드에디터 숨김 확인 |
| TC-01-006 | Dialog 열기 | 그룹 자식 편집 Dialog 열기 | 그룹 문제가 있는 시험 상세 | 하위 문제 편집 버튼 클릭 | Dialog 열림, "Q{부모}-{자식}" 번호 표시, 모든 필드 표시 | Pass | Q13-1 Dialog: 제목 "문제 수정 — Q13-1", 모든 필드 표시 |
| TC-01-007 | 데이터 바인딩 | Dialog에 기존 데이터 로드 | 시험 상세 페이지 | 독립 문제 편집 버튼 클릭 | 문제 내용/채점기준/배점/contentType이 기존 값으로 채워짐 | Pass | Q1 Dialog: 기존 content, answerContent, score=8, TEXT 라디오 모두 정확히 로드 |
| TC-01-008 | 수정 저장 | 독립 문제 내용 수정 후 저장 | Dialog 열린 상태 | 문제 내용 변경 → 저장 클릭 | Dialog 닫힘, 페이지에 수정된 내용 즉시 반영 | Pass | Q1 내용에 "(수정됨)" 추가 후 저장 → 페이지 즉시 반영, 원복 완료 |
| TC-01-009 | 수정 저장 | 채점 기준 수정 후 저장 | Dialog 열린 상태 | 채점 기준 변경 → 저장 클릭 | Dialog 닫힘, 채점 기준 즉시 반영 | Pass | Q1 채점 기준에 "(수정됨)" 추가 후 저장 → 페이지 즉시 반영, 원복 완료 |
| TC-01-010 | 수정 저장 | 배점 수정 후 totalScore 갱신 | Dialog 열린 상태 | 배점 변경 → 저장 클릭 | Dialog 닫힘, 상단 totalScore 즉시 갱신 | Pass | Q1 배점 8→10 변경 → totalScore 100→102 갱신 확인, 원복 완료 |
| TC-01-011 | 유효성 검증 | 빈 내용 시 저장 버튼 비활성화 | Dialog 열린 상태 | 문제 내용을 모두 지움 | 저장 버튼 비활성화 (disabled) | Pass | 문제 내용 비움 → 저장 버튼 disabled 확인 |
| TC-01-012 | 유효성 검증 | 빈 채점 기준 시 저장 버튼 비활성화 | 독립 문제 Dialog | 채점 기준을 모두 지움 | 저장 버튼 비활성화 | Pass | Q13-1 채점 기준 비움 → 저장 버튼 disabled 확인 |
| TC-01-013 | 마크다운 | 마크다운 미리보기 토글 | 마크다운 타입 문제 Dialog | 미리보기 버튼 클릭 | 편집 textarea ↔ 마크다운 렌더링 토글 | Pass | Q9(마크다운) Dialog: 미리보기 클릭 → prose 렌더링 표시, 편집 클릭 → textarea 복귀 |
| TC-01-014 | 제출 결과 | 제출 결과 있는 시험 편집 가능 | 제출 결과 있는 시험 상세 | 문제 편집 버튼 클릭 → 수정 → 저장 | 409 없이 정상 저장, 페이지 반영 | Pass | 제출 결과 있는 시험(examId=11)에서 Q1 수정 → 409 없이 정상 저장 |
| TC-01-015 | Dialog 닫기 | 취소 버튼으로 Dialog 닫기 | Dialog 열린 상태 | 취소 버튼 클릭 | Dialog 닫힘, 변경사항 미반영 | Pass | 취소 클릭 → Dialog 닫힘, 페이지 변경 없음 확인 |

## Summary
- Total: 15
- Pass: 15
- Fail: 0
- Skip: 0
- Pass Rate: 100%