# QA Test Sheet: AI 출제 도우미 그룹 문제 공통지문 포함

- **Date**: 2026-02-24
- **Tester**: Claude (automated)
- **Target URL**: http://localhost:5173
- **Scope**: AI 출제 도우미에서 그룹 문제 하위 문제 사용 시 부모 공통 지문 포함 여부 검증

## Test Cases

| ID | Category | Test Name | Precondition | Steps | Expected Result | Result | Note |
|----|----------|-----------|-------------|-------|----------------|--------|------|
| TC-01-001 | Display | 독립 문제 AI 다이얼로그에 공통지문 배너 미표시 | 관리자 로그인, 시험 생성 페이지 | 1) 시험 생성 접속 2) 독립 문제의 AI 버튼 클릭 3) 다이얼로그 확인 | 파란색 공통지문 배너가 표시되지 않음 | Pass | |
| TC-01-002 | Display | 그룹 문제 하위 AI 다이얼로그에 공통지문 배너 표시 | 관리자 로그인, 시험 생성 페이지, 그룹 문제 설정 완료 | 1) 그룹 문제 토글 2) 부모 공통지문 입력 3) 하위 문제 AI 버튼 클릭 | 파란색 배너에 "공통 지문(보기)이 프롬프트에 포함됩니다" 메시지 + 공통지문 미리보기 표시 | Pass | |
| TC-01-003 | Display | 공통지문 미입력 시 배너 미표시 | 관리자 로그인, 시험 생성 페이지, 그룹 문제 설정 | 1) 그룹 문제 토글 2) 공통지문 비워둠 3) 하위 문제 AI 버튼 클릭 | 파란색 공통지문 배너가 표시되지 않음 | Pass | |
| TC-01-004 | Data | 하위 문제 AI 생성 시 parentContent API 전송 | 관리자 로그인, 그룹 문제 + 공통지문 입력 완료 | 1) 하위 문제 AI 버튼 클릭 2) 지시 입력 후 생성 3) 네트워크 요청 확인 | POST /api/ai-assist/generate 요청에 parentContent 필드 포함 | Pass | 요청 필드: instruction, contentType, score, parentContent |
| TC-01-005 | Data | 독립 문제 AI 생성 시 parentContent 미전송 | 관리자 로그인, 독립 문제 | 1) 독립 문제 AI 버튼 클릭 2) 지시 입력 후 생성 3) 네트워크 요청 확인 | POST /api/ai-assist/generate 요청에 parentContent 필드 없음 | Pass | 요청 필드: instruction, contentType, score, currentContent, currentAnswer |
| TC-01-006 | Display | 공통지문 120자 이상 시 말줄임 표시 | 관리자 로그인, 그룹 문제 | 1) 120자 이상 공통지문 입력 2) 하위 문제 AI 버튼 클릭 | 배너에서 120자까지만 표시 + '...' 말줄임 처리 | Pass | |
| TC-01-007 | Display | 기존 문제 내용 배너와 공통지문 배너 동시 표시 | 관리자 로그인, 그룹 문제 + 하위 문제에 기존 내용 있음 | 1) 하위 문제에 내용/채점기준 입력 2) AI 버튼 클릭 | 파란색 공통지문 배너 + 노란색 기존 내용 배너 모두 표시 | Pass | 스크린샷: tc-01-007-both-banners.png |

## Summary

- **Total**: 7
- **Pass**: 7
- **Fail**: 0
- **Skip**: 0
- **Pass Rate**: 100%

## Issues

| Test Case ID | Issue | Severity | Report |
|-------------|-------|----------|--------|
| - | 이슈 없음 | - | - |
