# QA Test Sheet: 코드 리뷰 파인딩 수정 검증

- **Date**: 2026-02-23
- **Tester**: Claude (automated)
- **Target URL**: http://localhost:5173
- **Scope**: 코드 리뷰 파인딩 수정 11건 (Critical 2건 + Important 9건) 검증

## Test Cases

| ID | Category | Test Name | Precondition | Steps | Expected Result | Result | Note |
|----|----------|-----------|-------------|-------|----------------|--------|------|
| TC-01-001 | Form | ExamLogin 버튼 비활성 - 빈 폼 | 활성 시험 존재 | 1) /exam/login 접속 2) 아무 입력 없이 버튼 확인 | 시험 시작 버튼이 disabled 상태 | Pass | Important #8 |
| TC-01-002 | Form | ExamLogin 버튼 비활성 - 이름만 입력 | 활성 시험 존재 | 1) /exam/login 접속 2) 이름만 입력 3) 버튼 확인 | 버튼이 disabled 상태 유지 | Pass | Important #8 |
| TC-01-003 | Form | ExamLogin 버튼 활성 - 모든 필드 유효 | 활성 시험 존재 | 1) /exam/login 접속 2) 이름+생년월일(8자리) 입력 3) 버튼 확인 | 버튼이 enabled 상태 | Pass | Important #8 |
| TC-01-004 | Form | ExamLogin 중복 클릭 방지 | 활성 시험 존재 | 1) /exam/login 접속 2) 이름+생년월일 입력 3) 시험 시작 클릭 4) 로딩 중 재클릭 시도 | 로딩 중 버튼 disabled, 중복 요청 차단 | Pass | Important #9. API 빠른 응답으로 로딩 상태 짧음. :disabled 바인딩 + 코드 가드 확인 |
| TC-01-005 | Error | authStore localStorage 손상 데이터 방어 | 없음 | 1) 콘솔에서 localStorage에 잘못된 JSON 저장 2) 페이지 새로고침 | 앱 크래시 없이 정상 로드, 손상 데이터 자동 제거 | Pass | Important #7. 손상 데이터 null로 자동 제거됨 |
| TC-01-006 | Form | ExamCreate timeLimit 0 검증 | 관리자 로그인 | 1) POST /api/exams에 timeLimit:0으로 직접 API 호출 | 서버에서 400 에러 반환 | Pass | Important #5. @Min(1) 검증 동작 확인 |
| TC-01-007 | Form | ExamCreate timeLimit null 허용 | 관리자 로그인 | 1) POST /api/exams에 timeLimit:null로 API 호출 | 시험 정상 생성 (시간 제한 없음) | Pass | Important #5. null 허용 확인 |
| TC-01-008 | Display | ExamTake 프로그레스 바 비율 정확성 | 시간 제한 시험, 수험자 로그인 | 1) /exam/take/{examId} 접속 2) 프로그레스 바 width 확인 | totalSeconds=timeLimit*60 기준 비율 표시 | Pass | Important #11. 97.56% (1756/1800) 확인 |
| TC-01-009 | Error | ExamTake 세션 에러 UI 표시 | 시간 제한 시험 활성 | 1) exam-sessions API 500 인터셉트 2) ExamTake 접속 | "세션 오류" Card + 재시도/돌아가기 버튼, 문제 미노출 | Pass | Critical #3. 스크린샷: screenshot-tc01-009-session-error.png |
| TC-01-010 | Auth | ExamTake 403 에러 시 cleanup | 시간 초과 상태 | 1) submissions API 403 인터셉트 2) 타이머 만료로 자동 제출 3) localStorage 확인 | 답안+수험자 데이터 localStorage에서 삭제됨 | Pass | Important #10. examinee=null, answers=null 확인 |
| TC-01-011 | Error | SubmissionService 세션 없을 시 FORBIDDEN | 시간 제한 시험, DB에 세션 없음 | 1) 세션 미생성 수험자로 POST /api/submissions 직접 호출 | 403 FORBIDDEN 응답 | Pass | Critical #2. orElseThrow 동작 확인 |

## Summary

- **Total**: 11
- **Pass**: 11
- **Fail**: 0
- **Skip**: 0
- **Pass Rate**: 100%

## Issues

없음
