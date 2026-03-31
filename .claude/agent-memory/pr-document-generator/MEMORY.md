# PR Document Generator Memory

## PR 본문 이미지 규칙
- GitHub PR 본문에서 이미지는 **상대 경로로 렌더링되지 않음**
- 반드시 `raw.githubusercontent.com` 절대 URL 사용
- 형식: `https://raw.githubusercontent.com/Potwings/exam-manager/{브랜치명}/{파일경로}`
- 예: `![스크린샷](https://raw.githubusercontent.com/Potwings/exam-manager/feature/exam-monitor/qa-results/QA-exam-monitor/TC-001/screenshot.png)`
- PR 문서 파일(`docs/pr/PR-*.md`)에서도 동일하게 절대 URL 사용할 것
