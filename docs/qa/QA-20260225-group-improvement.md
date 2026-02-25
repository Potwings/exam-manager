# QA Test Sheet: ExamCreate 그룹 문제 개선

- **Date**: 2026-02-25
- **Tester**: Claude (automated)
- **Target URL**: http://localhost:5173
- **Scope**: 그룹 문제 AI 출제 도우미 공통지문 포함 + 하위 문제 마크다운 미리보기

## Test Cases

| ID | Category | Test Name | Precondition | Steps | Expected Result | Result | Note |
|----|----------|-----------|-------------|-------|----------------|--------|------|
| TC-01-001 | Form | 하위 문제 MARKDOWN 선택 시 미리보기 버튼 표시 | 관리자 로그인, 시험 생성 페이지 | 1) 문제 추가 2) 그룹 문제 토글 ON 3) 하위 문제 contentType을 MARKDOWN으로 변경 | 하위 문제 "문제 내용" 라벨 우측에 "미리보기" 버튼 표시 | Pass | |
| TC-01-002 | Form | 하위 문제 TEXT 선택 시 미리보기 버튼 미표시 | 관리자 로그인, 시험 생성 페이지 | 1) 문제 추가 2) 그룹 문제 토글 ON 3) 하위 문제 contentType이 TEXT인 상태 확인 | 하위 문제 "문제 내용" 라벨 우측에 미리보기 버튼 없음 | Pass | |
| TC-01-003 | Display | 하위 문제 마크다운 미리보기 렌더링 | TC-01-001 상태 | 1) 하위 문제 textarea에 마크다운 입력 (`**굵은글씨**`, 테이블 등) 2) 미리보기 버튼 클릭 | 마크다운이 HTML로 렌더링되어 표시, 버튼 텍스트 "편집"으로 변경 | Pass | 굵은글씨 + 테이블 정상 렌더링 |
| TC-01-004 | Form | 미리보기↔편집 토글 전환 | TC-01-003 상태 (미리보기 모드) | 1) "편집" 버튼 클릭 | textarea로 전환, 기존 마크다운 텍스트 유지, 버튼 "미리보기"로 변경 | Pass | |
| TC-01-005 | Display | AI 출제 도우미 하위 문제에서 공통지문 배너 표시 | 관리자 로그인, 그룹 문제 생성, 부모에 공통지문 입력 | 1) 하위 문제의 AI 출제 버튼(Sparkles) 클릭 | AI Dialog에 공통지문 안내 배너 표시 | Pass | 아이콘+안내문구+지문내용 모두 표시 |
| TC-01-006 | Display | AI 출제 도우미 독립 문제에서 공통지문 배너 미표시 | 관리자 로그인, 독립 문제 존재 | 1) 독립 문제의 AI 출제 버튼 클릭 | AI Dialog에 공통지문 안내 배너 없음 | Pass | 입력란+생성버튼만 표시 |

## Summary

- **Total**: 6
- **Pass**: 6
- **Fail**: 0
- **Skip**: 0
- **Pass Rate**: 100%

## Issues

| Test Case ID | Issue | Severity | Report |
|-------------|-------|----------|--------|
