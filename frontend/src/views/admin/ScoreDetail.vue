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
      <Card v-for="s in result.submissions" :key="s.id">
        <CardHeader class="pb-3">
          <div class="flex items-center justify-between">
            <CardTitle class="text-base">문제 {{ s.problemNumber }}</CardTitle>
            <div class="flex items-center gap-2">
              <Badge :variant="s.earnedScore >= s.maxScore ? 'default' : s.earnedScore > 0 ? 'secondary' : 'destructive'">
                {{ s.earnedScore ?? 0 }} / {{ s.maxScore }}점
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
        </CardHeader>
        <CardContent class="space-y-3">
          <div v-if="s.problemContent">
            <p class="text-sm font-medium text-muted-foreground mb-1">문제</p>
            <div v-if="s.problemContentType === 'MARKDOWN'" class="prose prose-sm dark:prose-invert max-w-none bg-muted rounded-md p-3" v-html="renderMarkdown(s.problemContent)" />
            <pre v-else class="text-sm bg-muted rounded-md p-3 whitespace-pre-wrap break-words">{{ s.problemContent }}</pre>
          </div>
          <Separator v-if="s.problemContent" />
          <div>
            <p class="text-sm font-medium text-muted-foreground mb-1">제출 답안</p>
            <pre v-if="!s.annotatedAnswer" class="text-sm bg-muted rounded-md p-3 whitespace-pre-wrap break-words">{{ s.submittedAnswer || '(미작성)' }}</pre>
            <div v-else class="text-sm bg-muted rounded-md p-3 whitespace-pre-wrap break-words font-mono">
              <template v-for="(part, idx) in parseAnnotatedAnswer(s.annotatedAnswer)" :key="idx">
                <span
                  v-if="part.status === 'correct'"
                  class="bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-200 rounded px-0.5"
                >{{ part.text }}</span>
                <span
                  v-else-if="part.status === 'incorrect'"
                  class="bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-200 rounded px-0.5"
                >{{ part.text }}</span>
                <span
                  v-else-if="part.status === 'partial'"
                  class="bg-orange-100 dark:bg-orange-900/30 text-orange-800 dark:text-orange-200 rounded px-0.5"
                >{{ part.text }}</span>
                <span v-else>{{ part.text }}</span>
              </template>
            </div>
          </div>
          <Separator />

          <!-- 읽기 모드 -->
          <template v-if="editingId !== s.id">
            <div>
              <p class="text-sm font-medium text-muted-foreground mb-1">채점 사유</p>
              <p class="text-sm leading-relaxed">{{ s.feedback || '(피드백 없음)' }}</p>
            </div>
          </template>

          <!-- 편집 모드 -->
          <template v-else>
            <div class="space-y-3">
              <div>
                <label class="text-sm font-medium text-muted-foreground mb-1 block">득점 (최대 {{ s.maxScore }}점)</label>
                <Input
                  type="number"
                  v-model.number="editForm.earnedScore"
                  :min="0"
                  :max="s.maxScore"
                  class="w-32"
                />
              </div>
              <div>
                <label class="text-sm font-medium text-muted-foreground mb-1 block">채점 사유</label>
                <Textarea
                  v-model="editForm.feedback"
                  rows="3"
                />
              </div>
              <div>
                <label class="text-sm font-medium text-muted-foreground mb-1 block">답안 서식 (색상 마커)</label>
                <p class="text-xs text-muted-foreground mb-2">텍스트를 선택한 후 버튼을 클릭하여 마커를 적용하세요</p>
                <div class="flex items-center gap-2 mb-2">
                  <span class="text-xs text-muted-foreground">마커:</span>
                  <Button size="sm" variant="outline" @click="applyMarker('정답')"
                    class="h-7 text-xs bg-green-100/50 hover:bg-green-200/50 text-green-800 dark:bg-green-900/30 dark:hover:bg-green-900/50 dark:text-green-200 border-green-300 dark:border-green-700">정답</Button>
                  <Button size="sm" variant="outline" @click="applyMarker('오답')"
                    class="h-7 text-xs bg-red-100/50 hover:bg-red-200/50 text-red-800 dark:bg-red-900/30 dark:hover:bg-red-900/50 dark:text-red-200 border-red-300 dark:border-red-700">오답</Button>
                  <Button size="sm" variant="outline" @click="applyMarker('부분')"
                    class="h-7 text-xs bg-orange-100/50 hover:bg-orange-200/50 text-orange-800 dark:bg-orange-900/30 dark:hover:bg-orange-900/50 dark:text-orange-200 border-orange-300 dark:border-orange-700">부분</Button>
                </div>
                <Textarea
                  id="annotated-answer-textarea"
                  v-model="editForm.annotatedAnswer"
                  rows="4"
                  class="font-mono text-xs"
                  :placeholder="s.submittedAnswer || ''"
                />
                <div v-if="editForm.annotatedAnswer" class="mt-2">
                  <p class="text-xs text-muted-foreground mb-1">미리보기</p>
                  <div class="text-sm bg-muted rounded-md p-3 whitespace-pre-wrap break-words font-mono">
                    <template v-for="(part, idx) in parseAnnotatedAnswer(editForm.annotatedAnswer)" :key="idx">
                      <span v-if="part.status === 'correct'" class="bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-200 rounded px-0.5">{{ part.text }}</span>
                      <span v-else-if="part.status === 'incorrect'" class="bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-200 rounded px-0.5">{{ part.text }}</span>
                      <span v-else-if="part.status === 'partial'" class="bg-orange-100 dark:bg-orange-900/30 text-orange-800 dark:text-orange-200 rounded px-0.5">{{ part.text }}</span>
                      <span v-else>{{ part.text }}</span>
                    </template>
                  </div>
                </div>
              </div>
              <div class="flex gap-2">
                <Button size="sm" @click="saveEdit(s)" :disabled="saving">
                  {{ saving ? '저장 중...' : '저장' }}
                </Button>
                <Button size="sm" variant="outline" @click="cancelEdit" :disabled="saving">
                  취소
                </Button>
              </div>
              <p v-if="editError" class="text-sm text-destructive">{{ editError }}</p>
            </div>
          </template>
        </CardContent>
      </Card>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
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

function applyMarker(type) {
  const textarea = document.getElementById('annotated-answer-textarea')
  if (!textarea) return

  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  if (start === end) return

  const selected = textarea.value.slice(start, end)

  // 같은 마커 → 해제 (토글), 다른 마커 → 교체, 마커 없음 → 적용
  const sameMarkerRegex = new RegExp(`^\\[${type}\\]([\\s\\S]*)\\[\\/${type}\\]$`)
  const anyMarkerRegex = /^\[(정답|오답|부분)\]([\s\S]*)\[\/\1\]$/
  const sameMatch = selected.match(sameMarkerRegex)
  const anyMatch = selected.match(anyMarkerRegex)

  let replacement
  if (sameMatch) {
    // 같은 마커가 적용되어 있으면 해제 (내부 텍스트만 남김)
    replacement = sameMatch[1]
  } else if (anyMatch) {
    // 다른 마커가 적용되어 있으면 교체 (기존 마커 제거 후 새 마커 적용)
    replacement = `[${type}]${anyMatch[2]}[/${type}]`
  } else {
    // 마커 없으면 새로 적용
    replacement = `[${type}]${selected}[/${type}]`
  }

  textarea.focus()
  textarea.setSelectionRange(start, end)
  const success = document.execCommand('insertText', false, replacement)

  if (!success || textarea.value === editForm.annotatedAnswer) {
    // execCommand 실패 시 수동 문자열 조합 폴백
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
  // 닫는 태그가 있는 쌍과, 닫는 태그 없이 여는 태그만 있는 경우 모두 매칭
  // 1) [정답]...[/정답] — 닫는 태그 있는 완전한 쌍
  // 2) [정답]...(?=[오답]|[부분]|[정답]|$) — 닫는 태그 없이 다음 여는 태그 또는 끝까지
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
  } catch (e) {
    loadError.value = '채점 결과를 불러올 수 없습니다.'
  } finally {
    loading.value = false
  }
})

function startEdit(s) {
  editingId.value = s.id
  editForm.earnedScore = s.earnedScore ?? 0
  editForm.feedback = s.feedback || ''
  // annotatedAnswer가 비어있으면 원래 제출 답안을 기본값으로 채워서
  // 관리자가 빈 상태에서 시작하지 않고 바로 마커를 적용할 수 있게 함
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

  // 득점 범위 검증 (0 ~ maxScore)
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

    // 로컬 데이터 반영
    const oldScore = s.earnedScore ?? 0
    s.earnedScore = editForm.earnedScore
    s.feedback = editForm.feedback
    s.annotatedAnswer = editForm.annotatedAnswer || null

    // 총점 재계산
    result.value.totalScore += (editForm.earnedScore - oldScore)

    editingId.value = null
  } catch (e) {
    editError.value = '저장에 실패했습니다.'
  } finally {
    saving.value = false
  }
}
</script>
