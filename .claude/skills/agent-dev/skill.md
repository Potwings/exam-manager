---
name: agent-dev
description: "프로젝트 서브 에이전트 6개를 오케스트레이션하여 기능을 기획→구현→검증→리뷰→PR 문서화하는 워크플로우. 사용자가 기능 설명을 입력하면 feature-planner로 기획, backend-senior-dev + frontend-vue-engineer로 병렬 구현, qa-playwright-tester로 QA 테스트, code-reviewer로 코드 리뷰, pr-document-generator로 PR 문서 생성을 순차 실행한다."
user_invocable: true
---

# Agent-Dev: 에이전트 오케스트레이션 워크플로우

사용자가 `/agent-dev {기능 설명}`을 입력하면, 6개 서브 에이전트를 단계적으로 활용하여 기능을 체계적으로 구현하는 워크플로우입니다.

## 워크플로우 개요

```
Phase 1: 기획       → feature-planner
Phase 2: 구현       → backend-senior-dev (구현+테스트) + frontend-vue-engineer (병렬)
Phase 3: QA 테스트   → qa-playwright-tester
Phase 4: 코드 리뷰  → code-reviewer (구현+테스트 코드 전체)
Phase 5: 마무리      → 메인 (CLAUDE.md + 커밋) + pr-document-generator (PR 문서)
```

## 실행 지침

### Phase 0: 준비

1. 사용자의 기능 설명을 파악한다
2. **브랜치를 생성**한다:
   - 기능 설명에서 브랜치명을 도출한다 (예: `feature/ai-assist-multiturn`, `fix/examinee-login-validation`)
   - prefix는 작업 성격에 맞게 선택한다 (`feature/` 새 기능, `fix/` 버그수정/교정, `refactor/` 리팩토링)
   - `git checkout -b {prefix}/{기능명}` 으로 main에서 분기한다
   - 이후 모든 작업(Phase 1~5)은 이 브랜치에서 진행한다
3. **산출물 경로 규칙**: 모든 에이전트의 산출물은 `docs/{브랜치명}/` 하위에 저장한다
   - 브랜치명은 prefix(`feature/`, `fix/`, `refactor/` 등)를 제외한 부분
   - 예: `fix/examinee-login-validation` → `docs/examinee-login-validation/`
   - 기획서: `docs/{브랜치명}/plan.md`
   - 백엔드 변경 설명: `docs/{브랜치명}/changes-backend.md`
   - 코드 리뷰: `docs/{브랜치명}/review.md`
   - QA 결과: `docs/{브랜치명}/qa/`
3. `TaskCreate`로 전체 작업 목록을 생성한다:
   - Task: Phase 1 기획
   - Task: Phase 2 백엔드 구현 (테스트 포함)
   - Task: Phase 2 프론트엔드 구현
   - Task: Phase 3 QA 테스트
   - Task: Phase 4 코드 리뷰
   - Task: Phase 5 마무리
4. 의존성을 설정한다 (Phase 2는 Phase 1에 blocked, Phase 3는 Phase 2에 blocked, 등)

### Phase 1: 기획 (feature-planner)

**에이전트**: `subagent_type: "feature-planner"`

1. Agent 도구로 feature-planner를 실행한다:
   ```
   prompt: "다음 기능에 대한 상세 기획서를 작성해주세요: {사용자 기능 설명}

   기획서에 반드시 포함할 항목:
   - API 계약 (Method, Path, Request/Response Body)
   - [Backend]/[Frontend] 태그가 붙은 작업 분해
   - QA 수용 기준 (검증 가능한 조건 목록)
   - 의존성 순서 (병렬 가능 여부 표기)"
   ```

2. 기획서 결과를 사용자에게 보여주고 확인을 받는다:
   - `AskUserQuestion`으로 기획서 승인 여부를 확인
   - 수정 요청이 있으면 feature-planner를 `resume`하여 수정

3. **사용자 승인 후** Phase 2로 진행한다

### Phase 2: 구현 (backend + frontend 병렬)

**에이전트**:
- `subagent_type: "backend-senior-dev"`
- `subagent_type: "frontend-vue-engineer"`

1. 기획서에서 `[Backend]` / `[Frontend]` 태그 작업을 분리한다

2. **의존성이 없는 작업은 병렬 실행**:
   - 두 에이전트를 **동일 메시지에서 동시에** Agent 도구로 호출한다
   - 각 에이전트에게 기획서의 해당 섹션 + API 계약을 전달한다

   ```
   # backend-senior-dev prompt:
   "다음 기획서의 [Backend] 작업을 구현해주세요:
   {기획서 중 Backend 관련 부분}

   API 계약:
   {API 계약 테이블}

   구현 요구사항:
   1. 기능 코드 구현
   2. Service 레이어 단위 테스트 작성 (핵심 비즈니스 로직 + 엣지 케이스)
   3. ./gradlew.bat test 로 기존 + 신규 테스트 전체 통과 확인
   4. 구현 후 변경된 파일 목록과 실제 API 응답 형태를 보고해주세요."

   # frontend-vue-engineer prompt:
   "다음 기획서의 [Frontend] 작업을 구현해주세요:
   {기획서 중 Frontend 관련 부분}

   API 계약:
   {API 계약 테이블}

   구현 후 변경된 파일 목록과 주요 UI 검증 포인트를 보고해주세요."
   ```

3. **의존성이 있는 작업은 순차 실행**:
   - 예: 백엔드 API가 먼저 완성되어야 프론트엔드 연동이 가능한 경우
   - backend-senior-dev 완료 후 frontend-vue-engineer 실행

4. 양쪽 구현 결과를 취합한다:
   - 변경된 파일 목록
   - API 응답 형태 확인 (backend → frontend 일관성)
   - 테스트 통과 여부 확인

### Phase 3: QA 테스트 (qa-playwright-tester)

**에이전트**: `subagent_type: "qa-playwright-tester"`

구현 완료 후 기능이 실제로 동작하는지 브라우저에서 먼저 검증한다. 동작하지 않는 코드를 리뷰하는 것은 낭비이므로 코드 리뷰보다 먼저 실행한다.

1. `/qa-testing` 스킬을 참조하여 QA 테스트를 실행한다:

   ```
   prompt: "다음 기능에 대한 QA 테스트를 진행해주세요:
   {기능 설명}

   QA 수용 기준 (기획서에서 추출):
   {QA 수용 기준 목록}

   구현된 변경사항:
   - Backend: {변경 파일 목록}
   - Frontend: {변경 파일 목록}

   테스트 환경:
   - Frontend: http://localhost:5173
   - Backend: http://localhost:8080"
   ```

2. QA 결과 처리:
   - **PASS**: Phase 4(코드 리뷰)로 진행
   - **CONDITIONAL PASS**: 사용자에게 보고하고 진행 여부 확인
   - **FAIL**: 실패 항목을 구현 에이전트에게 전달하여 수정 후 재테스트

### Phase 4: 코드 리뷰 (code-reviewer)

**에이전트**: `subagent_type: "code-reviewer"`

QA를 통과하여 동작이 검증된 코드를 대상으로 컨벤션, 보안, 성능을 리뷰한다.

1. 변경된 파일 목록을 code-reviewer에게 전달한다:

   ```
   prompt: "다음 파일들의 코드 변경사항을 리뷰해주세요:
   {변경된 파일 목록 (구현 + 테스트 코드 모두)}

   기능 설명: {기능 설명}

   리뷰 범위:
   1. CLAUDE.md 컨벤션 준수 여부
   2. 보안 취약점 (XSS, SQL Injection, 에러 메시지 노출)
   3. 성능 이슈 (N+1 쿼리, 불필요한 리렌더링)
   4. 테스트 코드 품질 (커버리지, 엣지 케이스, 테스트 격리)"
   ```

2. 리뷰 결과 처리:
   - **Critical 이슈**: 해당 파일을 직접 수정하고 code-reviewer를 `resume`하여 재리뷰
   - **Warning 이슈**: 사용자에게 보고하고 수정 여부를 확인
   - **Info 이슈**: 사용자에게 참고사항으로 보고
   - **LGTM**: Phase 5(마무리)로 진행

3. Critical/Warning 수정 후 재리뷰 사이클:
   - 수정 → code-reviewer resume → 결과 확인
   - 모든 Critical이 해소될 때까지 반복 (최대 3회)

4. 코드 리뷰 수정 후 QA 재테스트:
   - UI 동작에 영향을 주는 수정이 있었다면 qa-playwright-tester로 재테스트 진행
   - 백엔드 내부 로직만 변경된 경우(리팩토링, 성능 개선 등)는 단위 테스트 통과 확인으로 갈음

### Phase 5: 마무리

메인 에이전트가 직접 수행한다 (서브 에이전트 미사용):

#### 5-1. 서비스 종료

QA 테스트 등에서 구동한 개발 서버를 종료한다:
   1. `netstat -ano`로 5173(Vite), 8080(Spring Boot) 포트 LISTENING 프로세스를 확인한다
   2. 해당 PID를 `taskkill //PID {pid} //F`로 종료한다
   3. 리스닝 중인 서비스가 없으면 이 단계를 건너뛴다

#### 5-2. CLAUDE.md 업데이트

`revise-claude-md` 스킬을 참조하여 누락 없이 업데이트:
   - 새 API 엔드포인트 → `API Endpoints` 테이블에 추가
   - 새 Entity/DTO → `DB Schema`, `DTO` 테이블에 추가
   - 새 Route → `Routes` 테이블에 추가
   - TODO 항목 체크 또는 새 항목 추가
   - 관련 섹션 설명 업데이트 (해당 시)

#### 5-3. 커밋 준비

   1. `git diff --stat`으로 실제 변경된 파일 목록을 확인한다
   2. 변경 사항 요약을 사용자에게 보여준다 (파일 수, 주요 변경 내용)
   3. `AskUserQuestion`으로 커밋 여부를 확인한다
   4. 사용자 승인 시 커밋을 진행한다
      - 커밋 메시지: 기술 범위 + 기능 변화 형식
   5. `git push -u origin feature/{기능명}` 으로 원격에 브랜치를 push한다

#### 5-4. PR 문서 생성 + PR 생성 (pr-document-generator)

**에이전트**: `subagent_type: "pr-document-generator"`

커밋 및 push 완료 후, PR 문서를 자동 생성하고 feature → main PR을 만든다:

   1. `AskUserQuestion`으로 PR 생성 여부를 확인한다
   2. 사용자 승인 시 pr-document-generator 에이전트를 실행한다:
      ```
      prompt: "다음 작업에 대한 PR을 생성해주세요 (별도 문서 파일 없이 PR body에 직접 작성):

      브랜치: feature/{기능명} → main
      기능 설명: {기능 설명}

      변경된 파일:
      - Backend: {변경 파일 목록}
      - Frontend: {변경 파일 목록}

      주요 변경사항:
      {기획서 또는 코드 리뷰 결과에서 추출한 주요 변경 요약}

      UI 시각적 변경 여부: {있음/없음}
      (시각적 변경이 있는 경우에만 before/after 스크린샷을 캡처해주세요.
       내부 로직만 변경된 경우 스크린샷은 생략합니다.)"
      ```
   3. 에이전트가 `gh pr create --base main`으로 PR body에 직접 내용을 작성하여 PR을 생성한다
   4. PR URL을 사용자에게 반환한다

## 에이전트별 호출 규칙

| 에이전트              | subagent_type           | model  | 실행 조건                        |
|-----------------------|-------------------------|--------|----------------------------------|
| feature-planner       | `feature-planner`       | opus   | Phase 1 시작 시                  |
| backend-senior-dev    | `backend-senior-dev`    | opus   | Phase 2, [Backend] 작업 존재 시  |
| frontend-vue-engineer | `frontend-vue-engineer` | opus   | Phase 2, [Frontend] 작업 존재 시 |
| qa-playwright-tester  | `qa-playwright-tester`  | opus | Phase 3, 웹 UI 변경 존재 시      |
| code-reviewer         | `code-reviewer`         | opus | Phase 4, 코드 변경 존재 시       |
| pr-document-generator | `pr-document-generator` | opus   | Phase 5, PR 생성 승인 시         |

## 사용자 승인 시점

다음 시점에서 반드시 `AskUserQuestion`으로 사용자 확인을 받는다:

1. **Phase 1 완료 후**: 기획서 승인
2. **Phase 3 CONDITIONAL PASS**: QA 진행 여부 결정
3. **Phase 4 Warning 이슈**: 수정 여부 결정
4. **Phase 5 커밋 전**: 커밋 승인

## 에러 처리

- **에이전트 실패**: 에러 내용을 사용자에게 보고하고 재시도 여부를 확인
- **서버 미가동**: Phase 3 시작 전 localhost:5173 / localhost:8080 접근 가능 여부 확인
- **Phase 스킵**: Backend-only 또는 Frontend-only 기능일 경우 해당 에이전트만 실행
- **QA 반복 실패**: 3회 이상 실패 시 사용자에게 수동 개입 요청

## 참조 스킬

- `/qa-testing`: QA 테스트 실행 시 참조
- `/simplify`: 코드 리뷰 후 리팩토링 필요 시 참조
- `coderabbit:code-review`: 외부 코드 리뷰 도구 (보조 참조용)

## 출력 스타일

- 각 Phase 시작/완료 시 진행 상황을 사용자에게 보고한다
- `TaskUpdate`로 작업 상태를 실시간 갱신한다
- 에이전트 실행 결과는 핵심 요약만 사용자에게 전달한다 (전체 출력은 필요 시 별도 제공)
- 한국어로 소통하되, 코드/경로/기술 용어는 영어 그대로 사용한다
