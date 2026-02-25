<template>
  <Dialog :open="open" @update:open="$emit('update:open', $event)">
    <DialogContent class="sm:max-w-2xl max-h-[85vh] overflow-y-auto">
      <DialogHeader>
        <DialogTitle>
          문제 수정 — Q{{ displayNumber }}
        </DialogTitle>
        <DialogDescription>
          {{ isGroupParent ? '공통 지문을 수정합니다.' : '문제 내용과 채점 기준을 수정합니다.' }}
        </DialogDescription>
      </DialogHeader>

      <div class="space-y-4 py-2">
        <!-- 콘텐츠 타입 -->
        <div class="space-y-2">
          <Label>콘텐츠 타입</Label>
          <RadioGroup v-model="form.contentType" class="flex gap-4">
            <div class="flex items-center gap-2">
              <RadioGroupItem value="TEXT" id="edit-type-text" />
              <Label for="edit-type-text" class="font-normal cursor-pointer">텍스트</Label>
            </div>
            <div class="flex items-center gap-2">
              <RadioGroupItem value="MARKDOWN" id="edit-type-md" />
              <Label for="edit-type-md" class="font-normal cursor-pointer">마크다운</Label>
            </div>
          </RadioGroup>
        </div>

        <!-- 코드 에디터 토글 (그룹 부모에서는 숨김) -->
        <div v-if="!isGroupParent" class="flex items-center gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            :class="form.codeEditor
              ? 'bg-emerald-100 border-emerald-300 text-emerald-700 dark:bg-emerald-900/30 dark:border-emerald-700 dark:text-emerald-300'
              : ''"
            @click="form.codeEditor = !form.codeEditor"
          >
            <Code class="h-3.5 w-3.5 mr-1" />
            코드 에디터
          </Button>
        </div>

        <!-- 문제 내용 -->
        <div class="space-y-2">
          <div class="flex items-center justify-between">
            <Label>{{ isGroupParent ? '공통 지문' : '문제 내용' }}</Label>
            <Button
              v-if="form.contentType === 'MARKDOWN'"
              type="button"
              variant="ghost"
              size="sm"
              class="h-7 text-xs"
              @click="previewContent = !previewContent"
            >
              {{ previewContent ? '편집' : '미리보기' }}
            </Button>
          </div>
          <div v-if="previewContent && form.contentType === 'MARKDOWN'"
               class="prose prose-sm max-w-none dark:prose-invert border rounded-md p-3 bg-muted/30 min-h-[120px]"
               v-html="renderMd(form.content)">
          </div>
          <Textarea
            v-else
            v-model="form.content"
            :rows="6"
            class="font-mono text-sm"
            placeholder="문제 내용을 입력하세요"
          />
        </div>

        <!-- 채점 기준 (그룹 부모에서는 숨김) -->
        <div v-if="!isGroupParent" class="space-y-2">
          <Label>채점 기준</Label>
          <Textarea
            v-model="form.answerContent"
            :rows="4"
            class="font-mono text-sm"
            placeholder="채점 기준을 입력하세요"
          />
        </div>

        <!-- 배점 (그룹 부모에서는 숨김) -->
        <div v-if="!isGroupParent" class="space-y-2">
          <Label>배점</Label>
          <Input
            v-model.number="form.score"
            type="number"
            min="1"
            class="w-32"
          />
        </div>
      </div>

      <DialogFooter>
        <Button variant="outline" @click="$emit('update:open', false)">취소</Button>
        <Button :disabled="!canSave || saving" @click="handleSave">
          <Loader2 v-if="saving" class="h-4 w-4 mr-1 animate-spin" />
          저장
        </Button>
      </DialogFooter>

      <p v-if="saveError" class="text-sm text-destructive mt-2">{{ saveError }}</p>
    </DialogContent>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { updateProblem } from '@/api'
import { renderMarkdown } from '@/lib/markdown'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Code, Loader2 } from 'lucide-vue-next'

const props = defineProps({
  open: Boolean,
  problem: Object,
  examId: [String, Number],
  isGroupParent: Boolean,
  parentProblemNumber: Number
})

const emit = defineEmits(['update:open', 'saved'])

const form = ref({
  content: '',
  contentType: 'TEXT',
  codeEditor: false,
  answerContent: '',
  score: null
})

const previewContent = ref(false)
const saving = ref(false)
const saveError = ref('')

const displayNumber = computed(() => {
  if (!props.problem) return ''
  if (props.parentProblemNumber != null) {
    return `${props.parentProblemNumber}-${props.problem.problemNumber}`
  }
  return String(props.problem.problemNumber)
})

const canSave = computed(() => {
  if (!form.value.content?.trim()) return false
  if (!props.isGroupParent) {
    if (!form.value.answerContent?.trim()) return false
    if (!form.value.score || form.value.score <= 0) return false
  }
  return true
})

// Dialog 열릴 때 problem 데이터를 form에 복사
watch(() => props.open, (opened) => {
  if (opened && props.problem) {
    form.value = {
      content: props.problem.content || '',
      contentType: props.problem.contentType || 'TEXT',
      codeEditor: Boolean(props.problem.codeEditor),
      answerContent: props.problem.answerContent || '',
      score: props.problem.score ?? null
    }
    previewContent.value = false
    saveError.value = ''
  }
})

function renderMd(text) {
  return renderMarkdown(text)
}

async function handleSave() {
  if (!canSave.value || saving.value) return

  saving.value = true
  saveError.value = ''

  try {
    const payload = {
      content: form.value.content,
      contentType: form.value.contentType,
      codeEditor: form.value.codeEditor
    }
    if (!props.isGroupParent) {
      payload.answerContent = form.value.answerContent
      payload.score = form.value.score
    }

    const { data } = await updateProblem(props.examId, props.problem.id, payload)
    emit('saved', data)
    emit('update:open', false)
  } catch (e) {
    saveError.value = e.response?.data?.message || e.response?.data?.error || '저장에 실패했습니다.'
  } finally {
    saving.value = false
  }
}
</script>
