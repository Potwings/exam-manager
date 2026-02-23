# QA Test Sheet: 응시 중 답안 유지 (브라우저 새로고침/종료 대응)

- **Date**: 2026-02-23
- **Tester**: Claude QA
- **Target URL**: http://localhost:5173
- **Feature**: localStorage를 통한 수험자 인증 및 답안 영속화

## Test Cases

| ID | Category | Test Name | Pre-condition | Steps | Expected Result | Result | Note |
|----|----------|-----------|---------------|-------|-----------------|--------|------|
| TC-01-001 | 인증 유지 | 수험자 로그인 후 새로고침 시 인증 유지 | 활성 시험 존재 | 1. /exam/login 접속 2. 이름+생년월일 입력 후 시험 시작 3. ExamTake 페이지 확인 4. 브라우저 새로고침(F5) | ExamTake 페이지 유지 (/exam/login으로 리다이렉트되지 않음) | Pass | 새로고침 후 /exam/take/8 유지 |
| TC-01-002 | 인증 유지 | localStorage에 examinee 저장 확인 | TC-01-001 완료 | 1. 수험자 로그인 완료 2. DevTools > Application > localStorage 확인 | 'examinee' 키에 수험자 정보(id, name, birthDate) JSON 저장됨 | Pass | `{"id":30,"name":"QA테스터","birthDate":"1990-01-01"}` 확인 |
| TC-01-003 | 답안 저장 | 텍스트 답안 입력 후 새로고침 시 유지 | ExamTake 페이지 진입 | 1. Q1 텍스트 답안 입력 ("테스트 답안 A") 2. Q2 텍스트 답안 입력 ("테스트 답안 B") 3. 브라우저 새로고침 | Q1, Q2에 입력한 답안이 그대로 복원됨 | Pass | Q1, Q2 텍스트 답안 정확히 복원됨 |
| TC-01-004 | 답안 저장 | 코드 에디터 답안 입력 후 새로고침 시 유지 | ExamTake 페이지 진입 (코드 문제 포함) | 1. 코드 문제(Q9~Q14)에 코드 입력 2. 브라우저 새로고침 | 코드 에디터에 입력한 답안이 복원됨 | Pass | Q9 Monaco 에디터에 코드 정상 복원 확인 |
| TC-01-005 | 답안 저장 | localStorage에 답안 저장 확인 | 답안 입력 상태 | 1. 답안 입력 2. DevTools > localStorage 확인 | 'exam_{examId}_answers' 키에 답안 JSON 저장됨 | Pass | `exam_8_answers` 키에 Q1(25), Q2(26), Q9(33) 답안 JSON 저장 |
| TC-01-006 | 타이머 | 새로고침 후 타이머 이어감 확인 | 시간 제한 시험 응시 중 | 1. 시험 시작 후 타이머 확인 2. 새로고침 3. 타이머 확인 | 서버 기준 남은 시간으로 타이머가 이어서 카운트다운 | Pass | 29:57 → 새로고침 → 29:33 (서버 기준 이어서 카운트다운) |
| TC-01-007 | 정리 | 답안 제출 후 localStorage 정리 확인 | 답안 입력 상태 | 1. 답안 입력 후 제출 2. DevTools > localStorage 확인 | 'exam_{examId}_answers' 키와 'examinee' 키 모두 삭제됨 | Pass | 두 키 모두 null 확인 |
| TC-01-008 | 정리 | 제출 완료 후 돌아가기 시 로그인 페이지 표시 | 제출 완료 상태 | 1. 제출 완료 화면에서 "돌아가기" 클릭 | /exam/login 페이지로 이동, 로그인 폼 표시 | Pass | /exam/login 이동, 시험 응시 폼 정상 표시 |
| TC-01-009 | 브라우저 재시작 | 브라우저 탭 종료 후 URL 직접 접속 시 인증+답안 유지 | 답안 입력 상태에서 브라우저 탭 종료 | 1. 시험 로그인 + Q1,Q2 텍스트, Q9 코드 입력 2. 브라우저 탭 완전 종료 3. 새 탭에서 /exam/take/8 직접 접속 | 인증 유지 + 텍스트/코드 답안 모두 복원 + 타이머 서버 기준 이어감 | Pass | Q1,Q2 텍스트, Q9 코드(2줄) 모두 정확히 복원. 타이머 29:17로 이어감 |

## Summary
- Total: 9
- Pass: 9
- Fail: 0
- Skip: 0
- Pass Rate: 100%
