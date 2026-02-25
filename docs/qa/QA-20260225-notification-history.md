# QA Test Sheet: 알림 이력 (Notification Queue)

- **Date**: 2026-02-25
- **Tester**: Claude (automated)
- **Target URL**: http://localhost:5173
- **Scope**: 프론트엔드 알림 이력 — 벨 아이콘, Popover 패널, 알림 적재, 읽음 처리

## Test Cases

| ID | Category | Test Name | Precondition | Steps | Expected Result | Result | Note |
|----|----------|-----------|-------------|-------|----------------|--------|------|
| TC-01-001 | Display | 관리자 로그인 후 벨 아이콘 표시 | 관리자 로그인 | 1) 관리자 로그인 2) 헤더 확인 | username 우측에 벨 아이콘 표시 | Pass | |
| TC-01-002 | Display | 벨 클릭 시 빈 알림 패널 표시 | TC-01-001 상태 | 1) 벨 아이콘 클릭 | Popover 열림, "알림이 없습니다" 표시 | Pass | |
| TC-01-003 | Display | 미로그인 시 벨 아이콘 미표시 | 미로그인 상태 | 1) /exam/login 접속 2) 헤더 확인 | 벨 아이콘 없음, "관리자 로그인" 링크만 표시 | Pass | |
| TC-01-004 | Form | 관리자 호출 시 알림 적재 + 미확인 카운트 | 관리자 로그인 상태 | 1) 수험자가 관리자 호출 전송 2) 벨 아이콘 확인 | 벨 아이콘에 미확인 카운트 Badge 표시, BellDot 아이콘 전환 | Pass | call-admin API로 트리거 |
| TC-01-005 | Display | 알림 패널에서 이력 확인 | TC-01-004 후 | 1) 벨 아이콘 클릭 | 알림 목록에 "관리자 호출" 항목 표시 (주황 아이콘 + 메시지 + 시간) | Pass | AlertTriangle 아이콘 + "방금 전" 시간 표시 |
| TC-01-006 | Form | 패널 열면 미확인 카운트 초기화 | TC-01-004 후 (카운트 > 0) | 1) 벨 아이콘 클릭 (패널 열기) | 미확인 카운트 Badge 사라짐, Bell 아이콘으로 전환 | Pass | 패널 open 시 markAllRead 자동 호출 |

## Summary

- **Total**: 6
- **Pass**: 6
- **Fail**: 0
- **Skip**: 0
- **Pass Rate**: 100%

## Issues

| Test Case ID | Issue | Severity | Report |
|-------------|-------|----------|--------|
