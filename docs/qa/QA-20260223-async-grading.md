# QA Test Sheet: 제출 채점 비동기 전환

- **Date**: 2026-02-23
- **Tester**: Claude (automated)
- **Target URL**: http://localhost:5173
- **Scope**: 답안 제출 → 즉시 응답 확인, 비동기 채점 완료 확인, 채점 중 UI, 자동 폴링, 재시험 방지

## Test Cases

| ID | Category | Test Name | Precondition | Steps | Expected Result | Result | Note |
|----|----------|-----------|-------------|-------|----------------|--------|------|
| TC-01-001 | Form | 답안 제출 시 즉시 응답 | 활성 시험 존재, 수험자 로그인 완료 | 1) 시험 응시 페이지 접속 2) 각 문제에 답안 입력 3) 제출 버튼 클릭 | 5초 이내 "제출 완료" 메시지 표시 | Pass | 0.3초 만에 응답 |
| TC-01-002 | Display | 제출 완료 후 UI 상태 | TC-01-001 완료 | 제출 완료 화면 확인 | "제출 완료" 카드 표시, 점수/피드백 미노출 | Pass | |
| TC-01-003 | Error | 재시험 방지 동작 | TC-01-001에서 이미 제출 완료 | 1) 같은 수험자로 다시 로그인 2) 답안 제출 | 409 에러 메시지 ("이미 응시 완료한 시험입니다") | Pass | alert 후 /exam/login 리다이렉트 |
| TC-01-004 | Data | 관리자 채점 결과 조회 (비동기 완료 후) | 비동기 채점 완료 대기 | 1) 관리자 로그인 2) 채점 결과 상세 확인 | 점수/피드백 정상 표시, 폴링으로 자동 반영 | Pass | 15/100, LLM 피드백 정상 |
| TC-01-005 | Display | 채점 미완료 상태 "채점 중" 표시 | 제출 직후 (비동기 채점 진행 중) | 1) 관리자 채점 결과 목록 확인 2) 상세 페이지 확인 | 목록: "채점 중" Badge, 상세: 각 문제별 "채점 중" + 안내 메시지 | Pass | amber 스피너 + 안내문구 |

## Summary

- **Total**: 5
- **Pass**: 5
- **Fail**: 0
- **Skip**: 0
- **Pass Rate**: 100%

## Issues

| Test Case ID | Issue | Severity | Report |
|-------------|-------|----------|--------|
| (없음) | - | - | - |

## Notes

- 초기 구현에서 `@Async` 메서드를 `@Transactional` 내부에서 직접 호출하여 트랜잭션 커밋 전에 비동기 스레드가 실행되는 문제 발견 → `TransactionSynchronization.afterCommit()`으로 수정하여 해결
- "채점 중" UI 표시 기능 추가 (ScoreBoard + ScoreDetail)
- 채점 완료 시 자동 반영을 위한 5초 간격 폴링 추가 (채점 완료 시 자동 중단)
