# QA Summary: 수험자 로그인 재시험 방지

- **테스트 일시**: 2026-03-14
- **총 테스트 케이스**: 8개
- **통과**: 7개
- **실패**: 1개
- **부분통과**: 0개
- **전체 통과율**: 87.5%

## 주요 발견 사항
1. **[Critical] 백엔드 재시험 방지 쿼리 미동작**: ExamineeService의 `existsByExamineeIdAndProblemExamActiveTrueAndProblemExamDeletedFalse` Spring Data JPA 쿼리 메서드가 제출 기록이 있는 수험자에 대해 `false`를 반환하여, 로그인 시점 재시험 방지가 동작하지 않음
2. **프론트엔드 에러 핸들링 정상**: 409 응답 수신 시 에러 메시지 표시, 입력 변경 시 자동 소멸, 고정 메시지 사용 등 모든 프론트엔드 동작은 정상 (Playwright route intercept로 검증)
3. **기존 기능 회귀 없음**: 미응시 수험자 로그인, 신규 수험자 생성, 활성 시험 없을 때 버튼 비활성화 등 기존 기능 정상 동작

## 이슈 목록
| 이슈 ID | 제목 | 심각도 | 관련 TC |
|---------|------|--------|---------|
| ISSUE-001 | 로그인 시점 재시험 방지 쿼리 미동작 | Critical | TC-004 |

## 최종 판정
**FAIL** - P0-Critical 테스트 케이스(TC-004: 재시험 방지)가 실패. 핵심 신규 기능이 동작하지 않음.

## 권장 사항
1. `existsByExamineeIdAndProblemExamActiveTrueAndProblemExamDeletedFalse` 메서드명 대신 `@Query` 어노테이션으로 명시적 JPQL을 작성하여 쿼리 파싱 문제를 해결할 것
2. `show-sql: true` 로그에서 실제 생성되는 SQL을 확인하여 원인 특정할 것
3. 수정 후 재테스트 필요 (TC-004 ~ TC-008)
4. 참고: 제출 시점의 기존 재시험 방지(`SubmissionService.existsByExamineeIdAndProblemExamId`)는 정상 동작하므로 2중 방어선은 유지되나, 로그인 시점 차단이 UX상 더 중요
