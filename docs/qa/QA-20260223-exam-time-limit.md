# QA Test Sheet: 시험 시간 제한 + 자동 제출

- **Date**: 2026-02-23
- **Tester**: Claude
- **Target URL**: http://localhost:5173
- **Feature**: 시험 시간 제한(timeLimit) 설정, 타이머 카운트다운, 자동 제출

## Test Cases

| ID | Category | Test Name | Pre-condition | Steps | Expected Result | Result | Notes |
|----|----------|-----------|---------------|-------|-----------------|--------|-------|
| TC-01-001 | ExamCreate | 시간 제한 입력 필드 표시 | 관리자 로그인 | 1. /admin/exams/create 접속 2. 기본 정보 Card 확인 | 시험 제목 옆에 "시간 제한 (분)" 입력 필드가 2열 grid로 표시됨 | **Pass** | "미입력 시 시간 제한 없음" 안내 텍스트도 정상 표시 |
| TC-01-002 | ExamCreate | 시간 제한 30분 설정 시험 생성 | 관리자 로그인 | 1. 시험 제목 입력 2. 시간 제한 30 입력 3. 문제/채점기준 입력 4. 시험 생성 | 시험이 정상 생성되고 목록 페이지로 이동 | **Pass** | "QA 시간제한 v2" (ID 7) 생성 확인 |
| TC-01-003 | ExamManage | 시간 컬럼 표시 (제한 있음) | TC-01-002 완료 | 1. /admin/exams 목록 확인 | "시간" 컬럼에 Clock 아이콘 + "30분" 표시 | **Pass** | Clock 아이콘 + "30분" 정상 표시 |
| TC-01-004 | ExamManage | 시간 컬럼 표시 (제한 없음) | 시간 제한 없는 시험 존재 | 1. /admin/exams 목록 확인 | 시간 제한 없는 시험의 시간 컬럼에 "-" 표시 | **Pass** | ID 3~6 모두 "-" 표시 |
| TC-01-005 | ExamLogin | 제한시간 표시 | 시간 제한 있는 시험 활성화 | 1. /exam/login 접속 2. 활성 시험 정보 확인 | "제한시간 30분" 텍스트 표시 | **Pass** | "시험: QA 시간제한 v2(1문제) · 제한시간 30분" 정상 표시 |
| TC-01-006 | ExamTake | 타이머 카운트다운 표시 | 시험 로그인 완료 + 시간 제한 시험 | 1. 시험 응시 페이지 진입 | sticky 타이머 바에 MM:SS 형식 카운트다운 표시 | **Pass** | "29:58" → 카운트다운 정상 동작. Timer 아이콘 + monospace 표시 |
| TC-01-007 | ExamTake | 시간 제한 없는 시험 타이머 미표시 | 시간 제한 없는 시험 활성화 | 1. 시험 응시 페이지 진입 | 타이머 바가 표시되지 않음 | **Pass** | 타이머 요소 전혀 없음 확인 |

## Screenshots

- TC-01-006 타이머 표시: `docs/qa/screenshot-tc01-006-timer.png`
- TC-01-007 타이머 미표시: `docs/qa/screenshot-tc01-007-no-timer.png`

## Notes

- 구 서버(재시작 전)에서 생성한 ID 6 시험은 timeLimit이 DB에 저장되지 않음 (당시 코드 미반영). 서버 재시작 후 생성한 ID 7부터 정상 저장됨.

## Summary
- Total: 7
- Pass: 7
- Fail: 0
- Skip: 0
- Pass Rate: 100%
