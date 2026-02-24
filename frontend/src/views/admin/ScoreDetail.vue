<template>
  <div class="space-y-6">
    <div>
      <div class="flex items-center gap-2 mb-1">
        <Button variant="ghost" size="sm" @click="router.push('/admin/scores')">
          <ArrowLeft class="h-4 w-4 mr-1" /> 목록
        </Button>
      </div>
      <div v-if="result">
        <h1 class="text-2xl font-bold tracking-tight">{{ examineeName }} 채점 상세</h1>
        <p class="text-muted-foreground">
          총점:
          <Badge :variant="result.totalScore >= result.maxScore * 0.6 ? 'default' : 'destructive'">
            {{ result.totalScore }} / {{ result.maxScore }}
          </Badge>
        </p>
      </div>
      <div v-else-if="loading">
        <h1 class="text-2xl font-bold tracking-tight">채점 상세</h1>
      </div>
    </div>

    <div v-if="loading" class="flex items-center justify-center py-12">
      <Loader2 class="h-6 w-6 animate-spin text-muted-foreground" />
      <span class="ml-2 text-muted-foreground">불러오는 중...</span>
    </div>

    <p v-else-if="loadError" class="text-sm text-destructive">{{ loadError }}</p>

    <template v-else-if="result">
      <template v-for="item in groupedSubmissions" :key="item.key">
        <!-- 그룹 문제: 부모 지문 + 하위 제출 결과 -->
        <Card v-if="item.type === 'group'">
          <CardHeader class="pb-3">
            <CardTitle class="text-base">
              문제 {{ item.parentProblemNumber }}
              <Badge variant="secondary" class="ml-2 text-xs">그룹</Badge>
            </CardTitle>
          </CardHeader>
          <CardContent class="space-y-4">
            <!-- 부모 지문 -->
            <div>
              <p class="text-sm font-medium text-muted-foreground mb-1">공통 지문</p>
              <div v-if="item.parentProblemContentType === 'MARKDOWN'" class="prose prose-sm dark:prose-invert max-w-none bg-muted rounded-md p-3" v-html="renderMarkdown(item.parentProblemContent)" />
              <pre v-else class="text-sm bg-muted rounded-md p-3 whitespace-pre-wrap break-words">{{ item.parentProblemContent }}</pre>
            </div>

            <!-- 하위 문제 제출 결과 -->
            <div class="ml-4 border-l-2 border-blue-200 dark:border-blue-800 pl-4 space-y-4">
              <div v-for="s in item.children" :key="s.id" class="border rounded-md p-3 space-y-3 bg-muted/10">
                <div class="flex items-center justify-between">
                  <span class="text-sm font-medium">Q{{ item.parentProblemNumber }}-{{ s.problemNumber }}</span>
                  <div class="flex items-center gap-2">
                    <Badge v-if="s.earnedScore == null" variant="outline" class="text-amber-600 border-amber-300">
                      <Loader2 class="h-3 w-3 animate-spin mr-1" /> 채점 중
                    </Badge>
                    <Badge v-else :variant="s.earnedScore >= s.maxScore ? 'default' : s.earnedScore > 0 ? 'secondary' : 'destructive'">
                      {{ s.earnedScore }} / {{ s.maxScore }}점
                    </Badge>
                    <Button
                      v-if="editingId !== s.id"
                      variant="ghost"
                      size="sm"
                      @click="startEdit(s)"
                    >
                      <Pencil class="h-3.5 w-3.5" />
                    </Button>
                  </div>
                </div>

                <!-- 하위 문제 내용 -->
                <div v-if="s.problemContent">
                  <p class="text-sm font-medium text-muted-foreground mb-1">문제</p>
                  <div v-if="s.problemContentType === 'MARKDOWN'" class="prose prose-sm dark:prose-invert max-w-none bg-muted rounded-md p-3" v-html="renderMarkdown(s.problemContent)" />
                  <pre v-else class="text-sm bg-muted rounded-md p-3 whitespace-pre-wrap break-words">{{ s.problemContent }}</pre>
                </div>
                <Separator v-if="s.problemContent" />

                <!-- 제출 답안 -->
                <div>
                  <p class="text-sm font-medium text-muted-foreground mb-1">제출 답안</p>
                  <pre v-if="!s.annotatedAnswer" class="text-sm bg-muted rounded-md p-3 whitespace-pre-wrap break-words">{{ s.submittedAnswer || '(미작성)' }}</pre>
                  <div v-else class="text-sm bg-muted rounded-md p-3 whitespace-pre-wrap break-words font-mono">
                    <template v-for="(part, idx) in parseAnnotatedAnswer(s.annotatedAnswer)" :key="idx">
                      <span v-if="part.status === 'correct'" class="bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-200 rounded px-0.5">{{ part.text }}</span>
                      <span v-else-if="part.status === 'incorrect'" class="bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-200 rounded px-0.5">{{ part.text }}</span>
                      <span v-else-if="part.status === 'partial'" class="bg-orange-100 dark:bg-orange-900/30 text-orange-800 dark:text-orange-200 rounded px-0.5">{{ part.text }}</span>
                      <span v-else>{{ part.text }}</span>
                    </template>
                  </div>
                </div>
                <Separator />

                <!-- 읽기/편집 모드 (하위 문제) -->
                <template v-if="editingId !== s.id">
                  <div>
                    <p class="text-sm font-medium text-muted-foreground mb-1">채점 사유</p>
                    <p v-if="s.earnedScore == null" class="text-sm text-amber-600 flex items-center gap-1">
                      <Loader2 class="h-3 w-3 animate-spin" /> 채점이 완료되면 피드백이 표시됩니다.
                    </p>
                    <p v-else class="text-sm leading-relaxed">{{ s.feedback || '(피드백 없음)' }}</p>
                  </div>
                </template>
                <template v-else>
                  <EditForm :s="s" :editForm="editForm" :saving="saving" :editError="editError"
                    @save="saveEdit(s)" @cancel="cancelEdit" @marker="applyMarker" />
                </template>
              </div>
            </div>
          </CardContent>
        </Card>

        <!-- 독립 문제 -->
        <Card v-else>
          <CardHeader class="pb-3">
            <div class="flex items-center justify-between">
              <CardTitle class="text-base">문제 {{ item.submission.problemNumber }}</CardTitle>
              <div class="flex items-center gap-2">
                <Badge v-if="item.submission.earnedScore == null" variant="outline" class="text-amber-600 border-amber-300">
                  <Loader2 class="h-3 w-3 animate-spin mr-1" /> 채점 중
                </Badge>
                <Badge v-else :variant="item.submission.earnedScore >= item.submission.maxScore ? 'default' : item.submission.earnedScore > 0 ? 'secondary' : 'destructive'">
                  {{ item.submission.earnedScore }} / {{ item.submission.maxScore }}점
                </Badge>
                <Button
                  v-if="editingId !== item.submission.id"
                  variant="ghost"
                  size="sm"
                  @click="startEdit(item.submission)"
                >
                  <Pencil class="h-3.5 w-3.5" />
                </Button>
              </div>
            </div>
          </CardHeader>
          <CardContent class="space-y-3">
            <div v-if="item.submission.problemContent">
              <p class="text-sm font-medium text-muted-foreground mb-1">문제</p>
              <div v-if="item.submission.problemContentType === 'MARKDOWN'" class="prose prose-sm dark:prose-invert max-w-none bg-muted rounded-md p-3" v-html="renderMarkdown(item.submission.problemContent)" />
              <pre v-else class="text-sm bg-muted rounded-md p-3 whitespace-pre-wrap break-words">{{ item.submission.problemContent }}</pre>
            </div>
            <Separator v-if="item.submission.problemContent" />
            <div>
              <p class="text-sm font-medium text-muted-foreground mb-1">제출 답안</p>
              <pre v-if="!item.submission.annotatedAnswer" class="text-sm bg-muted rounded-md p-3 whitespace-pre-wrap break-words">{{ item.submission.submittedAnswer || '(미작성)' }}</pre>
              <div v-else class="text-sm bg-muted rounded-md p-3 whitespace-pre-wrap break-words font-mono">
                <template v-for="(part, idx) in parseAnnotatedAnswer(item.submission.annotatedAnswer)" :key="idx">
                  <span v-if="part.status === 'correct'" class="bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-200 rounded px-0.5">{{ part.text }}</span>
                  <span v-else-if="part.status === 'incorrect'" class="bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-200 rounded px-0.5">{{ part.text }}</span>
                  <span v-else-if="part.status === 'partial'" class="bg-orange-100 dark:bg-orange-900/30 text-orange-800 dark:text-orange-200 rounded px-0.5">{{ part.text }}</span>
                  <span v-else>{{ part.text }}</span>
                </template>
              </div>
            </div>
            <Separator />

            <template v-if="editingId !== item.submission.id">
              <div>
                <p class="text-sm font-medium text-muted-foreground mb-1">채점 사유</p>
                <p v-if="item.submission.earnedScore == null" class="text-sm text-amber-600 flex items-center gap-1">
                  <Loader2 class="h-3 w-3 animate-spin" /> 채점이 완료되면 피드백이 표시됩니다.
                </p>
                <p v-else class="text-sm leading-relaxed">{{ item.submission.feedback || '(피드백 없음)' }}</p>
              </div>
            </template>
            <template v-else>
              <EditForm :s="item.submission" :editForm="editForm" :saving="saving" :editError="editError"
                @save="saveEdit(item.submission)" @cancel="cancelEdit" @marker="applyMarker" />
            </template>
          </CardContent>
        </Card>
      </template>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { fetchResult, updateSubmission } from '@/api'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Separator } from '@/components/ui/separator'
import { ArrowLeft, Loader2, Pencil } from 'lucide-vue-next'
import { renderMarkdown } from '@/lib/markdown.js'

// 인라인 편집 폼 컴포넌트
const EditForm = {
  props: ['s', 'editForm', 'saving', 'editError'],
  emits: ['save', 'cancel', 'marker'],
  setup(props, { emit }) {
    return () => h('div', { class: 'space-y-3' }, [
      h('div', [
        h('label', { class: 'text-sm font-medium text-muted-foreground mb-1 block' }, `득점 (최대 ${props.s.maxScore}점)`),
        h(Input, { type: 'number', modelValue: props.editForm.earnedScore, 'onUpdate:modelValue': v => props.editForm.earnedScore = v, min: 0, max: props.s.maxScore, class: 'w-32' })
      ]),
      h('div', [
        h('label', { class: 'text-sm font-medium text-muted-foreground mb-1 block' }, '채점 사유'),
        h(Textarea, { modelValue: props.editForm.feedback, 'onUpdate:modelValue': v => props.editForm.feedback = v, rows: 3 })
      ]),
      h('div', [
        h('label', { class: 'text-sm font-medium text-muted-foreground mb-1 block' }, '답안 서식 (색상 마커)'),
        h('p', { class: 'text-xs text-muted-foreground mb-2' }, '텍스트를 선택한 후 버튼을 클릭하여 마커를 적용하세요'),
        h('div', { class: 'flex items-center gap-2 mb-2' }, [
          h('span', { class: 'text-xs text-muted-foreground' }, '마커:'),
          h(Button, { size: 'sm', variant: 'outline', onClick: () => emit('marker', '정답'), class: 'h-7 text-xs bg-green-100/50 hover:bg-green-200/50 text-green-800 dark:bg-green-900/30 dark:hover:bg-green-900/50 dark:text-green-200 border-green-300 dark:border-green-700' }, () => '정답'),
          h(Button, { size: 'sm', variant: 'outline', onClick: () => emit('marker', '오답'), class: 'h-7 text-xs bg-red-100/50 hover:bg-red-200/50 text-red-800 dark:bg-red-900/30 dark:hover:bg-red-900/50 dark:text-red-200 border-red-300 dark:border-red-700' }, () => '오답'),
          h(Button, { size: 'sm', variant: 'outline', onClick: () => emit('marker', '부분'), class: 'h-7 text-xs bg-orange-100/50 hover:bg-orange-200/50 text-orange-800 dark:bg-orange-900/30 dark:hover:bg-orange-900/50 dark:text-orange-200 border-orange-300 dark:border-orange-700' }, () => '부분')
        ]),
        h(Textarea, { id: 'annotated-answer-textarea', modelValue: props.editForm.annotatedAnswer, 'onUpdate:modelValue': v => props.editForm.annotatedAnswer = v, rows: 4, class: 'font-mono text-xs', placeholder: props.s.submittedAnswer || '' }),
        props.editForm.annotatedAnswer
          ? h('div', { class: 'mt-2' }, [
              h('p', { class: 'text-xs text-muted-foreground mb-1' }, '미리보기'),
              h('div', { class: 'text-sm bg-muted rounded-md p-3 whitespace-pre-wrap break-words font-mono' },
                parseAnnotatedAnswer(props.editForm.annotatedAnswer).map((part, idx) =>
                  h('span', {
                    key: idx,
                    class: part.status === 'correct' ? 'bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-200 rounded px-0.5'
                      : part.status === 'incorrect' ? 'bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-200 rounded px-0.5'
                      : part.status === 'partial' ? 'bg-orange-100 dark:bg-orange-900/30 text-orange-800 dark:text-orange-200 rounded px-0.5'
                      : ''
                  }, part.text)
                )
              )
            ])
          : null
      ]),
      h('div', { class: 'flex gap-2' }, [
        h(Button, { size: 'sm', onClick: () => emit('save'), disabled: props.saving }, () => props.saving ? '저장 중...' : '저장'),
        h(Button, { size: 'sm', variant: 'outline', onClick: () => emit('cancel'), disabled: props.saving }, () => '취소')
      ]),
      props.editError ? h('p', { class: 'text-sm text-destructive' }, props.editError) : null
    ])
  }
}

const router = useRouter()
const route = useRoute()
const examId = route.params.examId
const examineeId = route.params.examineeId

const result = ref(null)
const loading = ref(true)
const loadError = ref('')
const examineeName = ref('')

// 편집 상태
const editingId = ref(null)
const editForm = reactive({ earnedScore: 0, feedback: '', annotatedAnswer: '' })
const saving = ref(false)
const editError = ref('')

let pollingTimer = null

// 제출 결과를 그룹핑: 독립 문제 vs 그룹 문제(부모 지문 + 하위 문제)
const groupedSubmissions = computed(() => {
  if (!result.value?.submissions) return []
  const items = []
  const groupMap = new Map()

  for (const s of result.value.submissions) {
    if (s.parentProblemId) {
      if (!groupMap.has(s.parentProblemId)) {
        const group = {
          type: 'group',
          key: `group-${s.parentProblemId}`,
          parentProblemId: s.parentProblemId,
          parentProblemNumber: s.parentProblemNumber,
          parentProblemContent: s.parentProblemContent,
          parentProblemContentType: s.parentProblemContentType,
          children: []
        }
        groupMap.set(s.parentProblemId, group)
        items.push(group)
      }
      groupMap.get(s.parentProblemId).children.push(s)
    } else {
      items.push({ type: 'single', key: `single-${s.id}`, submission: s })
    }
  }

  return items
})

function applyMarker(type) {
  const textarea = document.getElementById('annotated-answer-textarea')
  if (!textarea) return

  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  if (start === end) return

  const selected = textarea.value.slice(start, end)

  const sameMarkerRegex = new RegExp(`^\\[${type}\\]([\\s\\S]*)\\[\\/${type}\\]$`)
  const anyMarkerRegex = /^\[(정답|오답|부분)\]([\s\S]*)\[\/\1\]$/
  const sameMatch = selected.match(sameMarkerRegex)
  const anyMatch = selected.match(anyMarkerRegex)

  let replacement
  if (sameMatch) {
    replacement = sameMatch[1]
  } else if (anyMatch) {
    replacement = `[${type}]${anyMatch[2]}[/${type}]`
  } else {
    replacement = `[${type}]${selected}[/${type}]`
  }

  textarea.focus()
  textarea.setSelectionRange(start, end)
  const success = document.execCommand('insertText', false, replacement)

  if (!success || textarea.value === editForm.annotatedAnswer) {
    const before = textarea.value.slice(0, start)
    const after = textarea.value.slice(end)
    textarea.value = before + replacement + after
  }

  editForm.annotatedAnswer = textarea.value

  nextTick(() => {
    textarea.focus()
    textarea.setSelectionRange(start, start + replacement.length)
  })
}

function parseAnnotatedAnswer(text) {
  if (!text) return []
  const parts = []
  const statusMap = { '정답': 'correct', '오답': 'incorrect', '부분': 'partial' }
  const regex = /\[(정답|오답|부분)\]([\s\S]*?)(?:\[\/\1\]|(?=\[(?:정답|오답|부분)\])|$)/g
  let lastIndex = 0
  let match
  while ((match = regex.exec(text)) !== null) {
    if (match[0].length === 0) break
    if (match.index > lastIndex) {
      parts.push({ status: 'plain', text: text.slice(lastIndex, match.index) })
    }
    parts.push({ status: statusMap[match[1]], text: match[2] })
    lastIndex = regex.lastIndex
  }
  if (lastIndex < text.length) {
    parts.push({ status: 'plain', text: text.slice(lastIndex) })
  }
  return parts
}

onMounted(async () => {
  try {
    const { data } = await fetchResult(examineeId, examId)
    result.value = data
    examineeName.value = data.examineeName || '수험자'
    startPollingIfNeeded()
  } catch (e) {
    loadError.value = '채점 결과를 불러올 수 없습니다.'
  } finally {
    loading.value = false
  }
})

function startPollingIfNeeded() {
  const hasGrading = result.value?.submissions?.some(s => s.earnedScore == null)
  if (hasGrading && !pollingTimer) {
    pollingTimer = setInterval(async () => {
      try {
        const { data } = await fetchResult(examineeId, examId)
        result.value = data
        const stillGrading = data.submissions?.some(s => s.earnedScore == null)
        if (!stillGrading) stopPolling()
      } catch (_) { /* 무시 */ }
    }, 5000)
  }
}

function stopPolling() {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

onUnmounted(() => stopPolling())

function startEdit(s) {
  editingId.value = s.id
  editForm.earnedScore = s.earnedScore ?? 0
  editForm.feedback = s.feedback || ''
  editForm.annotatedAnswer = s.annotatedAnswer || s.submittedAnswer || ''
  editError.value = ''
}

function cancelEdit() {
  editingId.value = null
  editError.value = ''
}

async function saveEdit(s) {
  saving.value = true
  editError.value = ''

  const score = Number(editForm.earnedScore)
  if (isNaN(score) || score < 0 || score > s.maxScore) {
    editError.value = `득점은 0 이상 ${s.maxScore} 이하여야 합니다.`
    saving.value = false
    return
  }
  editForm.earnedScore = score

  try {
    await updateSubmission(s.id, {
      earnedScore: editForm.earnedScore,
      feedback: editForm.feedback,
      annotatedAnswer: editForm.annotatedAnswer || null
    })

    const oldScore = s.earnedScore ?? 0
    s.earnedScore = editForm.earnedScore
    s.feedback = editForm.feedback
    s.annotatedAnswer = editForm.annotatedAnswer || null

    result.value.totalScore += (editForm.earnedScore - oldScore)

    editingId.value = null
  } catch (e) {
    editError.value = '저장에 실패했습니다.'
  } finally {
    saving.value = false
  }
}
</script>
