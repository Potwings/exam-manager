# QA 시트: 코드 에디터 기본 언어 설정

- **테스트 일시**: 2026-02-25
- **테스트 대상**: 관리자 문제별 코드 에디터 기본 언어 설정 + 수험자 응시 시 기본 언어 적용
- **테스트 환경**: http://localhost:5173 (Chrome, Playwright MCP)
- **사전 조건**: 관리자 로그인 가능, 백엔드 실행 중

## 테스트 케이스

| ID | 카테고리 | 테스트명 | 기대 결과 | 결과 | 비고 |
|----|---------|---------|----------|------|------|
| TC-01-001 | 시험 생성 | 코드 에디터 ON 시 언어 드롭다운 표시 | 코드 에디터 버튼 옆에 언어 select 표시 | **Pass** | Java 기본 선택 상태 |
| TC-01-002 | 시험 생성 | 언어 드롭다운 옵션 확인 | Java, JavaScript, Python, SQL 4개 옵션 | **Pass** | 4개 옵션 모두 확인 |
| TC-01-003 | 시험 생성 | 코드 에디터 OFF 시 드롭다운 숨김 | 언어 select 사라짐 | **Pass** | combobox 제거 확인 |
| TC-01-004 | 시험 생성 | SQL 언어로 시험 생성 | 시험 저장 성공, API 응답에 codeLanguage=sql | **Pass** | 시험 ID=17, API에서 codeLanguage:"sql" 확인 |
| TC-01-005 | 시험 생성 | 그룹 하위 문제 언어 드롭다운 | 하위 문제에도 코드 에디터 ON 시 언어 드롭다운 표시 | **Pass** | Q1-1 하위 문제에 4개 옵션 표시 |
| TC-01-006 | 시험 수정 | 수정 모드에서 저장된 언어 로드 | SQL이 선택된 상태로 로드 | **Pass** | /admin/exams/17/edit에서 SQL [selected] 확인 |
| TC-01-007 | 문제 편집 | ProblemEditDialog 언어 드롭다운 | Dialog에 언어 드롭다운 + 저장된 언어 표시 | **Pass** | Dialog combobox에 SQL [selected] 확인 |
| TC-01-008 | 응시 화면 | 수험자 Monaco Editor 기본 언어 | 언어 드롭다운이 SQL로 표시 | **Pass** | Monaco Editor 언어 combobox SQL [selected] 확인 |

## Summary
- Total: 8
- Pass: 8
- Fail: 0
- Skip: 0
- Pass Rate: 100%