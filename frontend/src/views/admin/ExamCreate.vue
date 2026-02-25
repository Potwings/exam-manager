<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold tracking-tight">{{ isEditMode ? '시험 수정' : '시험 생성' }}</h1>
      <p class="text-muted-foreground">{{ isEditMode ? '문제와 채점 기준을 수정합니다.' : '문제와 채점 기준을 입력하여 새 시험을 생성합니다.' }}</p>
    </div>

    <Card>
      <CardHeader>
        <CardTitle>기본 정보</CardTitle>
      </CardHeader>
      <CardContent>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div class="space-y-2">
            <Label for="title">시험 제목</Label>
            <Input id="title" v-model="title" placeholder="백엔드 개발자 필기시험" />
          </div>
          <div class="space-y-2">
            <Label for="timeLimit">시간 제한 (분)</Label>
            <Input id="timeLimit" type="number" v-model.number="timeLimit" placeholder="미입력 시 무제한" min="1" />
            <p class="text-xs text-muted-foreground">미입력 시 시간 제한 없음</p>
          </div>
        </div>
      </CardContent>
    </Card>

    <Card>
      <CardHeader>
        <div class="flex items-center justify-between">
          <CardTitle>문제 목록</CardTitle>
          <Button size="sm" variant="outline" @click="addProblem">
            <Plus class="h-4 w-4 mr-1" /> 문제 추가
          </Button>
        </div>
      </CardHeader>
      <CardContent class="space-y-4">
        <div
          v-for="(p, idx) in problemInputs"
          :key="p.id"
          class="border rounded-lg p-4 space-y-3"
        >
          <!-- 헤더: 번호 + 타입 + 그룹 토글 + 배점 + 삭제 -->
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-3">
              <span class="font-medium text-sm">문제 {{ p.problemNumber }}</span>
              <RadioGroup v-model="p.contentType" class="flex items-center gap-3">
                <div class="flex items-center gap-1">
                  <RadioGroupItem :id="'type-text-' + p.id" value="TEXT" />
                  <Label :for="'type-text-' + p.id" class="text-xs font-normal cursor-pointer">텍스트</Label>
                </div>
                <div class="flex items-center gap-1">
                  <RadioGroupItem :id="'type-md-' + p.id" value="MARKDOWN" />
                  <Label :for="'type-md-' + p.id" class="text-xs font-normal cursor-pointer">마크다운</Label>
                </div>
              </RadioGroup>
              <Button
                type="button"
                variant="outline"
                size="sm"
                class="h-6 px-2 text-xs gap-1"
                :class="p.isGroup
                  ? 'bg-blue-100 border-blue-300 text-blue-800 dark:bg-blue-900/30 dark:border-blue-700 dark:text-blue-200'
                  : ''"
                @click="toggleGroup(p)"
              >
                <Layers class="h-3 w-3" />
                그룹 문제
              </Button>
              <Button
                v-if="!p.isGroup"
                type="button"
                variant="outline"
                size="sm"
                class="h-6 px-2 text-xs gap-1"
                :class="p.codeEditor
                  ? 'bg-emerald-100 border-emerald-300 text-emerald-800 dark:bg-emerald-900/30 dark:border-emerald-700 dark:text-emerald-200'
                  : ''"
                @click="p.codeEditor = !p.codeEditor"
              >
                <Code class="h-3 w-3" />
                코드 에디터
              </Button>
            </div>
            <div class="flex items-center gap-2">
              <!-- 독립 문제일 때만 배점 표시 -->
              <div v-if="!p.isGroup" class="flex items-center gap-1">
                <Label :for="'score-' + p.id" class="text-sm text-muted-foreground">배점</Label>
                <Input
                  :id="'score-' + p.id"
                  type="number"
                  v-model.number="p.score"
                  min="1"
                  class="w-20 h-8"
                  :class="{ 'border-destructive': !p.score || p.score <= 0 }"
                />
                <span v-if="!p.score || p.score <= 0" class="text-xs text-destructive whitespace-nowrap">1점 이상 필요</span>
              </div>
              <!-- 그룹 문제일 때 자식 배점 합계 -->
              <span v-else class="text-sm text-muted-foreground">
                소계 {{ childrenScore(p) }}점
              </span>
              <Button
                v-if="aiAvailable && !p.isGroup"
                size="sm"
                variant="ghost"
                @click="openAiDialog(p)"
                title="AI 출제 도우미"
              >
                <Sparkles class="h-4 w-4 text-amber-500" />
              </Button>
              <Button
                size="sm"
                variant="ghost"
                @click="removeProblem(idx)"
                :disabled="problemInputs.length <= 1"
              >
                <Trash2 class="h-4 w-4 text-destructive" />
              </Button>
            </div>
          </div>

          <!-- 문제 내용 (공통 지문 or 독립 문제) -->
          <div class="space-y-1">
            <div class="flex items-center justify-between">
              <Label :for="'content-' + p.id" class="text-sm">{{ p.isGroup ? '공통 지문' : '문제 내용' }}</Label>
              <button
                v-if="p.contentType === 'MARKDOWN'"
                type="button"
                class="text-xs text-muted-foreground hover:text-foreground transition-colors"
                @click="togglePreview(p.id, 'content')"
              >
                {{ previewState[p.id]?.content ? '편집' : '미리보기' }}
              </button>
            </div>
            <div v-if="p.contentType === 'MARKDOWN' && previewState[p.id]?.content" class="border rounded-md p-3 min-h-[80px] bg-muted/30">
              <div class="prose prose-sm max-w-none dark:prose-invert" v-html="renderMd(p.content)"></div>
            </div>
            <Textarea
              v-else-if="p.contentType === 'MARKDOWN'"
              :id="'content-' + p.id"
              v-model="p.content"
              :placeholder="p.isGroup ? '공통 지문을 마크다운으로 입력하세요...' : '마크다운으로 문제를 입력하세요... (테이블: | 열1 | 열2 |)'"
              rows="5"
              class="font-mono text-sm"
            />
            <Textarea
              v-else
              :id="'content-' + p.id"
              v-model="p.content"
              :placeholder="p.isGroup ? '공통 지문을 입력하세요...' : '문제를 입력하세요...'"
              rows="4"
            />
          </div>

          <!-- 독립 문제: 채점 기준 -->
          <div v-if="!p.isGroup" class="space-y-1">
            <Label :for="'answer-' + p.id" class="text-sm">채점 기준 (정답/루브릭)</Label>
            <Textarea
              :id="'answer-' + p.id"
              v-model="p.answerContent"
              placeholder="채점 기준을 입력하세요..."
              rows="4"
            />
          </div>

          <!-- 그룹 문제: 하위 문제 섹션 -->
          <div v-if="p.isGroup" class="ml-4 border-l-2 border-blue-200 dark:border-blue-800 pl-4 space-y-3">
            <div class="flex items-center justify-between">
              <span class="text-sm font-medium text-blue-700 dark:text-blue-300">하위 문제</span>
              <Button size="sm" variant="outline" @click="addChild(p)">
                <Plus class="h-3 w-3 mr-1" /> 하위 문제 추가
              </Button>
            </div>

            <div
              v-for="(child, ci) in p.children"
              :key="child.id"
              class="border rounded-md p-3 space-y-2 bg-muted/20"
            >
              <!-- 하위 문제 헤더 -->
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-3">
                  <span class="text-sm font-medium">Q{{ p.problemNumber }}-{{ child.problemNumber }}</span>
                  <RadioGroup v-model="child.contentType" class="flex items-center gap-3">
                    <div class="flex items-center gap-1">
                      <RadioGroupItem :id="'type-text-' + child.id" value="TEXT" />
                      <Label :for="'type-text-' + child.id" class="text-xs font-normal cursor-pointer">텍스트</Label>
                    </div>
                    <div class="flex items-center gap-1">
                      <RadioGroupItem :id="'type-md-' + child.id" value="MARKDOWN" />
                      <Label :for="'type-md-' + child.id" class="text-xs font-normal cursor-pointer">마크다운</Label>
                    </div>
                  </RadioGroup>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    class="h-6 px-2 text-xs gap-1"
                    :class="child.codeEditor
                      ? 'bg-emerald-100 border-emerald-300 text-emerald-800 dark:bg-emerald-900/30 dark:border-emerald-700 dark:text-emerald-200'
                      : ''"
                    @click="child.codeEditor = !child.codeEditor"
                  >
                    <Code class="h-3 w-3" />
                    코드 에디터
                  </Button>
                </div>
                <div class="flex items-center gap-2">
                  <div class="flex items-center gap-1">
                    <Label :for="'score-' + child.id" class="text-sm text-muted-foreground">배점</Label>
                    <Input
                      :id="'score-' + child.id"
                      type="number"
                      v-model.number="child.score"
                      min="1"
                      class="w-20 h-8"
                      :class="{ 'border-destructive': !child.score || child.score <= 0 }"
                    />
                  </div>
                  <Button
                    v-if="aiAvailable"
                    size="sm"
                    variant="ghost"
                    @click="openAiDialog(child, p)"
                    title="AI 출제 도우미"
                  >
                    <Sparkles class="h-4 w-4 text-amber-500" />
                  </Button>
                  <Button
                    size="sm"
                    variant="ghost"
                    @click="removeChild(p, ci)"
                    :disabled="p.children.length <= 1"
                  >
                    <Trash2 class="h-3.5 w-3.5 text-destructive" />
                  </Button>
                </div>
              </div>

              <!-- 하위 문제 내용 -->
              <div class="space-y-1">
                <div class="flex items-center justify-between">
                  <Label :for="'content-' + child.id" class="text-sm">문제 내용</Label>
                  <button
                    v-if="child.contentType === 'MARKDOWN'"
                    type="button"
                    class="text-xs text-muted-foreground hover:text-foreground transition-colors"
                    @click="togglePreview(child.id, 'content')"
                  >
                    {{ previewState[child.id]?.content ? '편집' : '미리보기' }}
                  </button>
                </div>
                <div v-if="child.contentType === 'MARKDOWN' && previewState[child.id]?.content" class="border rounded-md p-3 min-h-[80px] bg-muted/30">
                  <div class="prose prose-sm max-w-none dark:prose-invert" v-html="renderMd(child.content)"></div>
                </div>
                <Textarea
                  v-else-if="child.contentType === 'MARKDOWN'"
                  :id="'content-' + child.id"
                  v-model="child.content"
                  placeholder="하위 문제를 마크다운으로 입력하세요..."
                  rows="3"
                  class="font-mono text-sm"
                />
                <Textarea
                  v-else
                  :id="'content-' + child.id"
                  v-model="child.content"
                  placeholder="하위 문제를 입력하세요..."
                  rows="3"
                />
              </div>

              <!-- 하위 채점 기준 -->
              <div class="space-y-1">
                <Label :for="'answer-' + child.id" class="text-sm">채점 기준 (정답/루브릭)</Label>
                <Textarea
                  :id="'answer-' + child.id"
                  v-model="child.answerContent"
                  placeholder="채점 기준을 입력하세요..."
                  rows="3"
                />
              </div>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>

    <!-- 마크다운 도움말 -->
    <Card v-if="hasMarkdownProblem">
      <CardHeader class="cursor-pointer py-3" @click="showHelp = !showHelp">
        <div class="flex items-center justify-between">
          <CardTitle class="text-sm font-medium">마크다운 문법 도움말</CardTitle>
          <ChevronDown class="h-4 w-4 text-muted-foreground transition-transform" :class="{ 'rotate-180': showHelp }" />
        </div>
      </CardHeader>
      <CardContent v-if="showHelp" class="pt-0">
        <div class="grid grid-cols-2 gap-4 text-sm">
          <div class="space-y-2">
            <p class="font-medium text-muted-foreground">입력</p>
            <pre class="bg-muted p-2 rounded text-xs font-mono whitespace-pre-wrap">**굵은 글씨**
*기울임*

| 열1 | 열2 | 열3 |
|-----|-----|-----|
| A   | B   | C   |

```java
public void main() {}
```

- 항목 1
- 항목 2</pre>
          </div>
          <div class="space-y-2">
            <p class="font-medium text-muted-foreground">결과</p>
            <div class="border p-2 rounded prose prose-sm max-w-none dark:prose-invert" v-html="helpPreview"></div>
          </div>
        </div>
      </CardContent>
    </Card>

    <div class="flex items-center gap-3">
      <Button @click="handleSubmit" :disabled="submitting || !canSubmit">
        {{ submitting ? (isEditMode ? '수정 중...' : '생성 중...') : (isEditMode ? '시험 수정' : '시험 생성') }}
      </Button>
      <span class="text-sm text-muted-foreground">
        {{ problemInputs.length }}문제 / 총 {{ totalScore }}점
      </span>
    </div>
    <p v-if="submitError" class="text-sm text-destructive">{{ submitError }}</p>

    <AiAssistDialog
      v-model:open="aiDialogOpen"
      :problem="aiTargetProblem"
      :parent="aiParentProblem"
      @apply="applyAiResult"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useExamStore } from '@/stores/examStore'
import { renderMarkdown } from '@/lib/markdown'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { Trash2, Plus, ChevronDown, Sparkles, Layers, Code } from 'lucide-vue-next'
import AiAssistDialog from '@/components/AiAssistDialog.vue'
import { checkAiStatus, fetchExam } from '@/api'

const router = useRouter()
const route = useRoute()
const examStore = useExamStore()

const isEditMode = computed(() => !!route.params.id)
const editId = computed(() => route.params.id)

const title = ref('')
const timeLimit = ref(null)
const problemInputs = ref([makeProblem(1)])
const submitting = ref(false)
const submitError = ref('')
const showHelp = ref(false)
const previewState = reactive({})

const aiAvailable = ref(false)
const aiDialogOpen = ref(false)
const aiTargetProblem = ref(null)
const aiParentProblem = ref(null)

onMounted(async () => {
  try {
    const res = await checkAiStatus()
    aiAvailable.value = res.data.available
  } catch {
    aiAvailable.value = false
  }

  // 수정 모드 또는 복제 모드: 기존 시험 데이터를 불러와 폼에 바인딩
  const sourceId = isEditMode.value ? editId.value : route.query.from
  if (sourceId) {
    try {
      const { data } = await fetchExam(sourceId)
      title.value = data.title
      timeLimit.value = data.timeLimit || null
      problemInputs.value = data.problems.map(p => {
        const hasChildren = p.children && p.children.length > 0
        return {
          id: generateId(),
          problemNumber: p.problemNumber,
          content: p.content || '',
          contentType: p.contentType || 'TEXT',
          codeEditor: !!p.codeEditor,
          answerContent: p.answerContent || '',
          score: p.score || 5,
          isGroup: hasChildren,
          children: hasChildren
            ? p.children.map(c => ({
                id: generateId(),
                problemNumber: c.problemNumber,
                content: c.content || '',
                contentType: c.contentType || 'TEXT',
                codeEditor: !!c.codeEditor,
                answerContent: c.answerContent || '',
                score: c.score || 5
              }))
            : []
        }
      })
    } catch (e) {
      submitError.value = '시험 데이터 로드 실패: ' + (e.response?.data?.message || e.message)
    }
  }
})

function openAiDialog(problem, parentProblem = null) {
  aiTargetProblem.value = problem
  aiParentProblem.value = parentProblem
  aiDialogOpen.value = true
}

function applyAiResult(result) {
  if (!aiTargetProblem.value) return
  aiTargetProblem.value.content = result.problemContent
  aiTargetProblem.value.contentType = result.contentType
  aiTargetProblem.value.answerContent = result.answerContent
  aiTargetProblem.value.score = result.score
}

function childrenScore(p) {
  return p.children.reduce((sum, c) => sum + (c.score || 0), 0)
}

const totalScore = computed(() =>
  problemInputs.value.reduce((sum, p) => {
    if (p.isGroup) return sum + childrenScore(p)
    return sum + (p.score || 0)
  }, 0)
)

const hasMarkdownProblem = computed(() =>
  problemInputs.value.some(p =>
    p.contentType === 'MARKDOWN' ||
    (p.isGroup && p.children.some(c => c.contentType === 'MARKDOWN'))
  )
)

const canSubmit = computed(() => {
  if (!title.value.trim() || problemInputs.value.length === 0) return false
  return problemInputs.value.every(p => {
    if (p.isGroup) {
      return p.content.trim() &&
        p.children.length > 0 &&
        p.children.every(c => c.content.trim() && c.answerContent.trim() && c.score > 0)
    }
    return p.content.trim() && p.answerContent.trim() && p.score > 0
  })
})

const helpPreview = computed(() => renderMarkdown(
  '**굵은 글씨**\n*기울임*\n\n| 열1 | 열2 | 열3 |\n|-----|-----|-----|\n| A   | B   | C   |\n\n```java\npublic void main() {}\n```\n\n- 항목 1\n- 항목 2'
))

function generateId() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
    const r = Math.random() * 16 | 0
    return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16)
  })
}

function makeProblem(num) {
  return { id: generateId(), problemNumber: num, content: '', contentType: 'TEXT', codeEditor: false, answerContent: '', score: 5, isGroup: false, children: [] }
}

function makeChildProblem(num) {
  return { id: generateId(), problemNumber: num, content: '', contentType: 'TEXT', codeEditor: false, answerContent: '', score: 5 }
}

function addProblem() {
  const nextNum = problemInputs.value.length + 1
  problemInputs.value.push(makeProblem(nextNum))
}

function removeProblem(idx) {
  const removedId = problemInputs.value[idx]?.id
  problemInputs.value.splice(idx, 1)
  problemInputs.value.forEach((p, i) => { p.problemNumber = i + 1 })
  if (removedId) delete previewState[removedId]
}

function toggleGroup(p) {
  p.isGroup = !p.isGroup
  if (p.isGroup && p.children.length === 0) {
    p.children.push(makeChildProblem(1))
  }
}

function addChild(p) {
  const nextNum = p.children.length + 1
  p.children.push(makeChildProblem(nextNum))
}

function removeChild(p, ci) {
  p.children.splice(ci, 1)
  p.children.forEach((c, i) => { c.problemNumber = i + 1 })
}

function togglePreview(id, field) {
  if (!previewState[id]) previewState[id] = {}
  previewState[id][field] = !previewState[id][field]
}

function renderMd(text) {
  return renderMarkdown(text)
}

async function handleSubmit() {
  if (!canSubmit.value) return
  submitting.value = true
  submitError.value = ''
  const payload = {
    title: title.value.trim(),
    timeLimit: timeLimit.value && timeLimit.value > 0 ? timeLimit.value : null,
    problems: problemInputs.value.map(p => {
      const base = {
        problemNumber: p.problemNumber,
        content: p.content.trim(),
        contentType: p.contentType,
        codeEditor: !!p.codeEditor
      }
      if (p.isGroup) {
        // 그룹 문제: answerContent/score 없음, children 포함
        base.answerContent = null
        base.score = null
        base.codeEditor = false  // 그룹 부모는 코드 에디터 불필요
        base.children = p.children.map(c => ({
          problemNumber: c.problemNumber,
          content: c.content.trim(),
          contentType: c.contentType,
          codeEditor: !!c.codeEditor,
          answerContent: c.answerContent.trim(),
          score: c.score
        }))
      } else {
        base.answerContent = p.answerContent.trim()
        base.score = p.score
      }
      return base
    })
  }
  try {
    if (isEditMode.value) {
      await examStore.updateExam(editId.value, payload)
    } else {
      await examStore.createExam(payload)
    }
    router.push('/admin/exams')
  } catch (e) {
    submitError.value = '저장 실패: ' + (e.response?.data?.message || e.message)
  } finally {
    submitting.value = false
  }
}
</script>
