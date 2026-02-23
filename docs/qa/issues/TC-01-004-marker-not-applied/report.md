# Issue Report: 마커 버튼 클릭 시 텍스트에 마커가 적용되지 않음

## 기본 정보
- **Test Case ID**: TC-01-004, TC-01-005, TC-01-006
- **심각도**: High
- **발견일**: 2026-02-23
- **페이지**: `/admin/scores/:examId/:examineeId` (ScoreDetail.vue)

## 증상
편집 모드에서 Textarea의 텍스트를 선택한 후 정답/오답/부분 마커 버튼을 클릭해도 `[정답]...[/정답]` 등의 마커 태그가 텍스트에 삽입되지 않음.

## 원인 분석

### 근본 원인
`applyMarker()` 함수에서 `annotatedAnswerInput.value?.$el`이 `null`을 반환하여 함수 첫 줄의 `if (!textarea) return`에서 early return됨.

### 상세
```javascript
function applyMarker(type) {
  const textarea = annotatedAnswerInput.value?.$el  // ← null 반환
  if (!textarea) return  // ← 여기서 early return
  ...
}
```

- `annotatedAnswerInput`은 `ref(null)`로 선언되어 template의 `ref="annotatedAnswerInput"`과 바인딩됨
- shadcn-vue의 `Textarea` 컴포넌트에서 `$el`이 실제 `<textarea>` DOM 요소를 가리켜야 하나, 런타임에서 `null`을 반환
- Vue의 `vnode.el`을 통한 확인에서는 `<textarea>` 태그를 정상적으로 가리키나, Vue의 `$el` 접근 경로(`annotatedAnswerInput.value?.$el`)에서 참조가 끊어진 것으로 추정

### 검증 결과
- `textarea.selectionStart/End`는 blur 후에도 정상 유지됨 (0, 11)
- `mousedown` 시점에서 selection 유지 확인
- `click` 시점에서도 selection 값 유지 확인 (start=0, end=11)
- **문제는 selection이 아닌 ref 바인딩**: `annotatedAnswerInput.value?.$el === null`

## 재현 단계
1. 관리자 로그인 → 채점결과 → 수험자 클릭 → 채점 상세 진입
2. 아무 문제 카드에서 연필 아이콘 클릭 (편집 모드 진입)
3. 답안 서식 Textarea에서 텍스트 드래그 선택
4. 정답/오답/부분 버튼 클릭
5. **Expected**: 선택한 텍스트가 `[정답]...[/정답]`로 감싸짐
6. **Actual**: 아무 변화 없음

## 수정 제안
`annotatedAnswerInput.value?.$el` 대신 직접 DOM querySelector 또는 `$el.querySelector('textarea')` 등으로 textarea 요소에 접근하도록 수정 필요.

## 스크린샷
- [screenshot-001.png](screenshot-001.png) — 텍스트 선택 후 마커 미적용 상태
- [screenshot-002-preview.png](screenshot-002-preview.png) — 수동 마커 입력 시 미리보기는 정상 동작
