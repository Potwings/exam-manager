# QA Playwright Tester - Agent Memory

## 프로젝트 기본 정보
- Frontend: http://localhost:5173 (Vue 3 + Vite)
- Backend: http://localhost:8080 (Spring Boot 3.5)
- 관리자 기본 계정: admin / admin123 (initLogin=false 상태이면 비밀번호 변경 화면 미표시)
- 관리자 로그인 경로: /admin/login → 성공 시 /admin/scores 리다이렉트

## 테스트 경험 패턴

### 로그인 패턴
- admin/admin123 로그인 성공 → /admin/scores 자동 이동 (initLogin=false 시)
- initLogin=true 시 /admin/change-password 강제 이동 → 비밀번호 변경 후 정상 사용

### ScoreBoard (/admin/scores)
- 활성 시험이 있으면 드롭다운 자동 선택됨
- `r.regrading` 필드로 "재채점 중" Badge 조건부 표시 (파란색 + Loader2 스피너)
- 5초 폴링: `results.value.some(r => !r.gradingComplete || r.regrading)` 조건 시 자동 폴링

### ScoreDetail (/admin/scores/:examId/:examineeId)
- 그룹 문제(부모-자식) 구조: `parentProblemId` 기준으로 그룹핑하여 표시
- 개별 재채점 버튼 조건: `v-if="s.earnedScore !== null && !s.regrading && editingId !== s.id"`
- 재채점 중 문제 카드: "재채점 중" Badge(파란색) + "재채점이 진행 중입니다." 안내 텍스트
- isAnyRegrading=true 시 "전체 재채점" 버튼 자동 비활성화
- 채점 기준(answerContent): 읽기 모드에서 Collapsible 접이식(기본 접힘), 편집 모드에서 blue 박스 항상 표시
- answerContent null 방어: `v-if="s.answerContent"` 조건으로 미표시

### Toast 알림 검증 방법
- vue-sonner Toast가 짧게 표시 후 자동 소멸 → 스냅샷 타이밍 캡처 어려움
- 대안: 소스 코드 grep으로 toast.xxx() 호출 코드 확인 (간접 검증)
- 또는 JavaScript evaluate로 API 직접 호출 후 응답 상태 코드로 검증

### API 에러 케이스 검증 방법
- Playwright evaluate로 fetch() 직접 호출 가능
- credentials: 'include'로 세션 쿠키 자동 포함
- 예: 이미 재채점 중인 submission에 연속 호출 → 첫 번째 200, 두 번째 400 확인

### 폴링 기반 상태 변경 테스트
- LLM 폴백 채점(equalsIgnoreCase)이 즉시 완료되므로 "채점 중"/"재채점 중" Badge 표시 시간이 매우 짧음
- 스냅샷 XML에서 Badge 텍스트(`img + text: 재채점 중`) 또는 [disabled] 속성으로 상태 전환 확인

### AI 출제 도우미 버튼 탐색 방법
- `document.querySelector('button[title="AI 출제 도우미"]')` — Dialog 닫기 전에는 스냅샷에 ref 미노출될 수 있음. Dialog 한 번 열고 닫은 뒤 ref 확인 권장.
- LLM 가용 여부: GET /api/ai-assist/status → `{"available":true}` 확인 필수

### 네트워크 요청 인터셉트 패턴
- `browser_run_code`에서 `page.on('request', ...)` 등록 → 클릭 → `waitForTimeout` 순서
- POST body: `req.postData()` → `JSON.parse()`

### Vue reactive proxy === 비교 버그 패턴 (중요)
- Vue `ref([])` 배열에 push한 객체는 Vue reactive proxy로 래핑됨
- `for...of` 루프에서 꺼낸 `item === plainObject` (strict equality)는 항상 false
- 즉 break 조건이 의도대로 동작하지 않아 현재 항목이 계속 포함되는 버그 발생 가능
- 수정 방법: `import { toRaw } from 'vue'` 후 `toRaw(item) === entry` 비교

## 완료된 QA 세션

### QA-regrade-feature (2026-03-02)
- 대상: AI 채점 결과 재채점 기능 (ScoreDetail 개별/전체 재채점 + ScoreBoard 연동)
- TC 8개 모두 PASS (100% 통과율)
- 결과 경로: qa-results/QA-regrade-feature/
- 이슈: 없음
- 판정: PASS (커밋 가능)

### QA-ai-assist-multiturn (2026-03-03)
- 대상: AiAssistDialog 멀티턴 대화 conversationHistory 전송
- TC 7개 실행 (TC-08 스킵), 5 PASS / 2 부분통과
- 결과 경로: qa-results/QA-ai-assist-multiturn/
- 이슈: ISSUE-001 — for 루프 break 미동작 (Vue reactive proxy 원인), conversationHistory에 현재 instruction 중복 포함 (Medium)
- 판정: CONDITIONAL PASS (ISSUE-001 수정 후 커밋 권장)

### QA-score-detail-answer-criteria (2026-03-04)
- 대상: ScoreDetail 채점 기준(answerContent) 표시 기능 (읽기 Collapsible + 편집 blue 박스)
- TC 9개 모두 PASS (100% 통과율)
- 결과 경로: qa-results/QA-score-detail-answer-criteria/
- 이슈: 없음
- 판정: PASS (커밋 가능)
- 주의: 백엔드 코드 변경 후 서버 재시작 필요 (이전 빌드에서 answerContent 미반환 확인)

### QA-problem-edit-ai-assist (2026-03-07)
- 대상: ProblemEditDialog AI 출제 도우미 기능 (ExamDetail 개별 문제 편집 시 AI 생성)
- TC 8개 모두 PASS (100% 통과율)
- 결과 경로: qa-results/QA-problem-edit-ai-assist/
- 이슈: 없음
- 판정: PASS (커밋 가능)
- 주의: TC-004에서 Q1 데이터 영구 변경됨 (시드 데이터 복원 필요할 수 있음)

### QA-examinee-login-validation (2026-03-14)
- 대상: ExamineeService 로직 분리 + 로그인 시 재시험 방지 검증
- TC 8개: 7 PASS / 1 FAIL (87.5%)
- 결과 경로: qa-results/QA-examinee-login-validation/
- 이슈: ISSUE-001 — existsByExamineeIdAndProblemExamActiveTrueAndProblemExamDeletedFalse 쿼리 미동작 (Critical)
- 판정: FAIL (백엔드 쿼리 수정 후 재테스트 필요)
- 프론트엔드 에러 핸들링은 Playwright route intercept로 mock 409 검증 완료 (정상)

### Playwright route intercept 패턴 (프론트엔드 mock 테스트)
- `page.route('**/api/path', route => route.fulfill({status: 409, ...}))` 으로 특정 API 응답을 강제
- 테스트 후 반드시 `page.unroute('**/api/path')` 로 해제
- 백엔드 버그로 실제 에러 응답을 받을 수 없을 때 프론트엔드 에러 핸들링을 독립적으로 검증하는 데 유용

### Spring Data JPA 긴 메서드명 주의
- `existsByExamineeIdAndProblemExamActiveTrueAndProblemExamDeletedFalse` 같은 긴 경로 탐색 메서드는 파싱이 의도와 다를 수 있음
- 복잡한 조건은 `@Query` JPQL로 명시적 작성 권장

## 알려진 주의사항
- 서버 미기동 상태에서 테스트 시작 시 즉시 중단하고 서버 기동 요청
- **백엔드 코드 변경 후 반드시 서버 재시작 확인** — API 응답에 새 필드 누락 시 이전 빌드 실행 중일 가능성 높음. Object.keys()로 응답 키 확인 필수.
- 데이터가 없는 시험 선택 시 수험자 목록이 비어 테스트 불가 → 활성 시험 있는지 먼저 확인
- 그룹 부모 문제는 submission 없음 → 재채점/채점 대상 제외 (정상 동작)
