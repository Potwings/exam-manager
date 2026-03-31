# Frontend Vue Engineer Memory

## ScoreDetail.vue 구조
- 독립 문제(`type=single`)와 그룹 문제(`type=group`)를 `groupedSubmissions` computed로 분리
- 그룹: 부모 지문 + `item.children` 배열 / 독립: `item.submission` 단일 객체
- 인라인 편집 폼은 `EditForm` render function 컴포넌트 (h() 기반, `<script setup>` 내부 정의)
- 편집/재채점/채점 중 3가지 상태에 따라 버튼 표시 조건이 다름

## ScoreBoard.vue 구조
- `examStore.loadScores(examId)` 사용하여 결과 로드
- 폴링: `startPollingIfNeeded()` / `stopPolling()` 패턴 (5초 간격, 조건 미충족 시 자동 중단)

## AlertDialog 패턴
- shadcn-vue AlertDialog: `v-model:open` + 별도 Content/Header/Footer 구조
- ExamTake.vue에서는 `AlertDialogTrigger as-child` 패턴 사용
- ScoreDetail.vue에서는 프로그래매틱 open 제어 (`regradeDialogOpen` ref)
- `AlertDialogAction`에 `@click.prevent`로 기본 닫힘 동작 방지 후 수동 닫힘

## 폴링 패턴
- `let pollingTimer = null` (ref 아닌 일반 변수)
- `onUnmounted(() => stopPolling())` cleanup
- 조건: `earnedScore == null` (초회 채점) 또는 `regrading === true` (재채점)

## API 함수 패턴
- `src/api/index.js`에 named export
- POST 본문 없는 경우: `api.post('/path')` (빈 body)
- POST 본문 있는 경우: `api.post('/path', { data })`

## Collapsible 패턴
- shadcn-vue Collapsible: `Collapsible`, `CollapsibleTrigger`, `CollapsibleContent` 3개 컴포넌트
- 내부 reka-ui CollapsibleRoot가 slot props로 `{ open: boolean }` 노출 → `v-slot="{ open }"`으로 접힘/펼침 상태 참조 가능
- 별도 ref 불필요 — Collapsible의 내부 상태 활용 (각 인스턴스별 독립적)
- 아이콘 토글: `<ChevronUp v-if="open" />` / `<ChevronDown v-else />`

## EditForm render function 확장
- props 배열에 새 prop 추가: `props: ['s', 'editForm', 'saving', 'editError', 'answerContent']`
- h() 함수 내 조건부 렌더링: `props.answerContent ? h('div', ...) : null`
- blue info 스타일: `border-blue-200 dark:border-blue-800 bg-blue-50 dark:bg-blue-950/30`

## Badge 색상 규칙
- 채점 중 (초회): amber 계열 (`text-amber-600 border-amber-300`)
- 재채점 중: blue 계열 (`text-blue-600 border-blue-300`)
- 점수 Badge: default(높음) / secondary(부분) / destructive(0점)

## ExamMonitor.vue 구조
- ScoreBoard.vue 패턴 기반: 시험 선택 드롭다운 + 테이블 + 폴링
- 카운트다운: `fetchedAt`(Date.now()) + `tick` ref(매초 증가) 조합으로 클라이언트 카운트다운 구현
- `displayTime()` 함수에서 `tick.value` 참조하여 Vue 반응성 트리거
- 폴링: 10초 간격 (ScoreBoard는 5초), IN_PROGRESS 수험자 있을 때만
- 카운트다운 타이머 + 폴링 타이머 2개 별도 관리 (`countdownTimer`, `pollingTimer`)
- 상태 Badge: IN_PROGRESS(default) / SUBMITTED(secondary) / TIME_EXPIRED(destructive)
- 라우트: `/admin/monitor` (requiresAdmin: true)
- 헤더 탭 순서: 채점결과 | 응시현황 | 시험관리 | 계정관리

## 시험 선택 드롭다운 패턴
- ScoreBoard/ExamMonitor 공통: native `<select>` + shadcn 스타일 클래스
- active 시험 자동 선택 → 없으면 첫 번째 시험
- ScoreBoard는 `examStore.loadExams()` 사용, ExamMonitor는 직접 `fetchExams()` 호출
- `@change` 이벤트로 데이터 로드 트리거
