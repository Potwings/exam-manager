# QA Test Sheet: 마크다운 코드 블록 Syntax Highlighting

- **일자**: 2026-02-24
- **테스트 대상**: highlight.js 연동으로 MARKDOWN 타입 코드 블록 syntax highlighting
- **테스트 환경**: http://localhost:5173
- **사전 조건**: 백엔드 기동, 관리자 로그인, 테스트 시험(ID:15, MARKDOWN 코드블록 6문제) 생성

## Test Cases

| ID | 카테고리 | 테스트명 | 사전조건 | 테스트 단계 | 기대 결과 | 결과 | 비고 |
|----|----------|----------|----------|-------------|-----------|------|------|
| TC-01-001 | ExamDetail | Java 코드 블록 syntax highlighting | 관리자 로그인 | 1. /admin/exams/15 접속 2. Q1 Java 코드 블록 확인 | 다크 배경 + Java 키워드(public, class, String 등) 색상 구분 + 우상단 "java" 라벨 | Pass | 키워드 색상 구분 + 다크 배경 + 라벨 확인 |
| TC-01-002 | ExamDetail | SQL 코드 블록 syntax highlighting | 관리자 로그인 | 1. Q2 SQL 코드 블록 확인 | SQL 키워드(SELECT, FROM, LEFT JOIN 등) 색상 구분 + "sql" 라벨 | Pass | SELECT/FROM/WHERE 등 빨강 키워드 확인 |
| TC-01-003 | ExamDetail | Python 코드 블록 syntax highlighting | 관리자 로그인 | 1. Q3 Python 코드 블록 확인 | Python 키워드(def, if, return 등) 색상 구분 + "python" 라벨 | Pass | def/if/return/for/in 키워드 색상 확인 |
| TC-01-004 | ExamDetail | JavaScript 코드 블록 syntax highlighting | 관리자 로그인 | 1. Q4 JavaScript 코드 블록 확인 | JS 키워드(const, filter, map 등) 색상 구분 + "javascript" 라벨 | Pass | const/filter/map/console.log 색상 확인 |
| TC-01-005 | ExamDetail | 언어 미지정 코드 블록 | 관리자 로그인 | 1. Q5 언어 미지정 코드 블록 확인 | plain text 렌더링, 다크 배경 적용되지만 색상 구분 없음, 언어 라벨 미표시 | Pass | 다크 배경 + 단색 텍스트 + 라벨 없음 |
| TC-01-006 | ExamDetail | 인라인 코드 스타일 유지 | 관리자 로그인 | 1. Q5 인라인 코드 확인 | 기존 prose 인라인 코드 스타일 유지 (코드 블록과 별개) | Pass | 인라인 배경 스타일 유지 확인 |
| TC-01-007 | ExamDetail | TEXT 타입 문제 마크다운 미적용 | 관리자 로그인 | 1. Q6 TEXT 타입 문제 확인 | 코드 블록 마크다운 렌더링 없음, 원문 그대로 pre 출력 | Pass | 백틱 포함 원문 그대로 출력 |
| TC-01-008 | ExamTake | 수험자 시점 syntax highlighting | 시험 활성화 + 수험자 로그인 | 1. 시험 활성화 2. 수험자 로그인 3. 코드 블록 확인 | ExamDetail과 동일하게 syntax highlighting 적용 | Pass | Java/SQL 코드 블록 동일 렌더링 확인 |

## Screenshots

- `screenshot-tc01-001-java-highlight.png` — Java syntax highlighting (ExamDetail)
- `screenshot-tc01-002-sql-highlight.png` — SQL syntax highlighting (ExamDetail)
- `screenshot-tc01-003-python-highlight.png` — Python syntax highlighting (ExamDetail)
- `screenshot-tc01-004-js-highlight.png` — JavaScript syntax highlighting (ExamDetail)
- `screenshot-tc01-005-no-lang-inline.png` — 언어 미지정 + 인라인 코드 (ExamDetail)
- `screenshot-tc01-007-text-type.png` — TEXT 타입 원문 출력 (ExamDetail)
- `screenshot-tc01-008-examtake-java.png` — Java syntax highlighting (ExamTake)
- `screenshot-tc01-008-examtake-sql.png` — SQL syntax highlighting (ExamTake)

## Summary
- Total: 8
- Pass: 8
- Fail: 0
- Skip: 0
- Pass Rate: 100%
