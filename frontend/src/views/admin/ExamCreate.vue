<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold tracking-tight">시험 생성</h1>
      <p class="text-muted-foreground">문제와 채점 기준을 입력하여 새 시험을 생성합니다.</p>
    </div>

    <Card>
      <CardHeader>
        <CardTitle>기본 정보</CardTitle>
      </CardHeader>
      <CardContent>
        <div class="space-y-2">
          <Label for="title">시험 제목</Label>
          <Input id="title" v-model="title" placeholder="백엔드 개발자 필기시험" />
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
          <!-- 헤더: 번호 + 타입 + 배점 + 삭제 -->
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
            </div>
            <div class="flex items-center gap-2">
              <div class="flex items-center gap-1">
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

          <!-- 문제 내용 -->
          <div class="space-y-1">
            <div class="flex items-center justify-between">
              <Label :for="'content-' + p.id" class="text-sm">문제 내용</Label>
              <button
                v-if="p.contentType === 'MARKDOWN'"
                type="button"
                class="text-xs text-muted-foreground hover:text-foreground transition-colors"
                @click="togglePreview(p.id, 'content')"
              >
                {{ previewState[p.id]?.content ? '편집' : '미리보기' }}
              </button>
            </div>
            <!-- 마크다운 미리보기 -->
            <div v-if="p.contentType === 'MARKDOWN' && previewState[p.id]?.content" class="border rounded-md p-3 min-h-[80px] bg-muted/30">
              <div class="prose prose-sm max-w-none dark:prose-invert" v-html="renderMd(p.content)"></div>
            </div>
            <!-- 마크다운 편집 -->
            <Textarea
              v-else-if="p.contentType === 'MARKDOWN'"
              :id="'content-' + p.id"
              v-model="p.content"
              placeholder="마크다운으로 문제를 입력하세요... (테이블: | 열1 | 열2 |)"
              rows="5"
              class="font-mono text-sm"
            />
            <!-- 텍스트 편집 -->
            <Textarea
              v-else
              :id="'content-' + p.id"
              v-model="p.content"
              placeholder="문제를 입력하세요..."
              rows="4"
            />
          </div>

          <!-- 채점 기준 -->
          <div class="space-y-1">
            <Label :for="'answer-' + p.id" class="text-sm">채점 기준 (정답/루브릭)</Label>
            <Textarea
              :id="'answer-' + p.id"
              v-model="p.answerContent"
              placeholder="채점 기준을 입력하세요..."
              rows="4"
            />
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
      <Button @click="handleCreate" :disabled="creating || !canCreate">
        {{ creating ? '생성 중...' : '시험 생성' }}
      </Button>
      <span class="text-sm text-muted-foreground">
        {{ problemInputs.length }}문제 / 총 {{ totalScore }}점
      </span>
    </div>
    <p v-if="createError" class="text-sm text-destructive">{{ createError }}</p>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useExamStore } from '@/stores/examStore'
import { renderMarkdown } from '@/lib/markdown'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { Trash2, Plus, ChevronDown } from 'lucide-vue-next'

const router = useRouter()
const examStore = useExamStore()

const title = ref('')
const problemInputs = ref([makeProblem(1)])
const creating = ref(false)
const createError = ref('')
const showHelp = ref(false)
const previewState = reactive({})

const totalScore = computed(() => problemInputs.value.reduce((sum, p) => sum + (p.score || 0), 0))

const hasMarkdownProblem = computed(() => problemInputs.value.some(p => p.contentType === 'MARKDOWN'))

const canCreate = computed(() =>
  title.value.trim() &&
  problemInputs.value.length > 0 &&
  problemInputs.value.every(p => p.content.trim() && p.answerContent.trim() && p.score > 0)
)

const helpPreview = computed(() => renderMarkdown(
  '**굵은 글씨**\n*기울임*\n\n| 열1 | 열2 | 열3 |\n|-----|-----|-----|\n| A   | B   | C   |\n\n```java\npublic void main() {}\n```\n\n- 항목 1\n- 항목 2'
))

function makeProblem(num) {
  return { id: crypto.randomUUID(), problemNumber: num, content: '', contentType: 'TEXT', answerContent: '', score: 5 }
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

function togglePreview(id, field) {
  if (!previewState[id]) previewState[id] = {}
  previewState[id][field] = !previewState[id][field]
}

function renderMd(text) {
  return renderMarkdown(text)
}

async function handleCreate() {
  if (!canCreate.value) return
  creating.value = true
  createError.value = ''
  try {
    await examStore.createExam({
      title: title.value.trim(),
      problems: problemInputs.value.map(p => ({
        problemNumber: p.problemNumber,
        content: p.content.trim(),
        contentType: p.contentType,
        answerContent: p.answerContent.trim(),
        score: p.score
      }))
    })
    router.push('/admin/exams')
  } catch (e) {
    createError.value = '시험 생성 실패: ' + (e.response?.data?.message || e.message)
  } finally {
    creating.value = false
  }
}
</script>
