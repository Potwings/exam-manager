# QA Test Sheet: ExamLogin UX 개선

- **작성일**: 2026-02-23
- **테스트 대상**: ExamLogin 페이지 (`/exam/login`)
- **테스트 범위**: autocomplete 방지, Enter 키 제출, Tab 키 이동, 버튼 클릭 시 유효성 검증
- **사전조건**: 활성 시험이 존재해야 함, 프론트엔드/백엔드 실행 중

## Test Cases

| ID | Category | Test Name | Pre-condition | Steps | Expected Result | Result | Note |
|----|----------|-----------|---------------|-------|-----------------|--------|------|
| TC-01-001 | Autocomplete | form autocomplete="off" 적용 확인 | 페이지 접속 | 1. /exam/login 접속 2. 이름/생년월일 input의 부모 form 확인 | form 태그에 autocomplete="off" 속성이 존재 | Pass | form 내 input 2개 확인 |
| TC-01-002 | Enter 키 | 유효한 입력 후 Enter 키로 제출 | 활성 시험 존재 | 1. 이름 입력 2. 생년월일 입력 3. 생년월일 input에서 Enter 키 입력 | 로그인 처리 → /exam/take 페이지로 이동 | Pass | /exam/take/5로 정상 이동 |
| TC-01-003 | Enter 키 | 빈 입력 상태에서 Enter 키 | 활성 시험 존재 | 1. 아무것도 입력하지 않음 2. 이름 input 클릭 후 Enter 키 입력 | 에러 메시지 노출 (이름, 생년월일) | Pass | 두 에러 메시지 모두 노출 |
| TC-01-004 | Tab 키 | 생년월일 → 시험 시작 버튼 Tab 이동 (유효 입력) | 활성 시험 존재 | 1. 이름 입력 2. 생년월일 입력 3. Tab 키 입력 | 시험 시작 버튼에 포커스 이동 | Pass | 버튼에 [active] 확인 |
| TC-01-005 | Tab 키 | 생년월일 → 시험 시작 버튼 Tab 이동 (빈 입력) | 활성 시험 존재 | 1. 아무것도 입력하지 않음 2. 이름 input 클릭 → Tab → Tab | 시험 시작 버튼에 포커스 이동 (disabled 상태에서도) | Pass | aria-disabled로 포커스 가능 |
| TC-01-006 | 버튼 | 빈 입력 상태에서 버튼 클릭 시 에러 표시 | 활성 시험 존재 | 1. 아무것도 입력하지 않음 2. 시험 시작 버튼 클릭 | 이름/생년월일 에러 메시지 노출, 로그인 진행하지 않음 | Pass | JS evaluate로 클릭 테스트 |
| TC-01-007 | 버튼 | 이름만 입력 후 버튼 클릭 시 생년월일 에러 표시 | 활성 시험 존재 | 1. 이름만 입력 2. 시험 시작 버튼 클릭 | 생년월일 에러 메시지 노출 | Pass | 이름 에러 없음, 생년월일만 노출 |
| TC-01-008 | 버튼 | 버튼 비활성 시각 스타일 확인 | 활성 시험 존재 | 1. 빈 입력 상태에서 버튼 확인 | 버튼이 반투명(opacity-50) 스타일로 표시 | Pass | opacity: 0.5, aria-disabled: true |

## Summary
- Total: 8
- Pass: 8
- Fail: 0
- Skip: 0
- Pass Rate: 100%