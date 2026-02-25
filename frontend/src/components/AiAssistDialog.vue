<template>
  <Dialog :open="open" @update:open="$emit('update:open', $event)">
    <DialogContent class="sm:max-w-3xl h-[85vh] !grid-rows-none !flex !flex-col gap-0 p-0 overflow-hidden">
      <DialogHeader class="px-6 pt-6 pb-2">
        <DialogTitle>AI 출제 도우미</DialogTitle>
        <DialogDescription>
          아이디어를 입력하면 AI가 문제와 채점 기준을 작성합니다.
        </DialogDescription>
      </DialogHeader>

      <!-- 대화 히스토리 -->
      <div class="flex-1 min-h-0 px-6">
      <ScrollArea ref="scrollAreaRef" class="h-full cursor-default">
        <div class="space-y-4 pb-4">
          <div v-for="(item, idx) in history" :key="idx" class="space-y-3">
            <!-- 관리자 요청 (우측 말풍선) -->
            <div class="flex justify-end">
              <div class="bg-primary text-primary-foreground rounded-lg px-4 py-2 max-w-[80%] text-sm">
                {{ item.instruction }}
              </div>
            </div>

            <!-- AI 결과 (좌측 카드) -->
            <div v-if="item.result" :id="'ai-result-' + idx" class="border rounded-lg p-4 space-y-3 max-w-[90%]">
              <div class="space-y-1">
                <p class="text-xs font-medium text-muted-foreground">문제 내용</p>
                <div v-if="item.result.contentType === 'MARKDOWN'" class="prose prose-sm max-w-none dark:prose-invert border rounded p-3 bg-muted/30" v-html="renderMd(item.result.problemContent)"></div>
                <pre v-else class="text-sm whitespace-pre-wrap border rounded p-3 bg-muted/30">{{ item.result.problemContent }}</pre>
              </div>
              <div class="space-y-1">
                <p class="text-xs font-medium text-muted-foreground">채점 기준</p>
                <pre class="text-sm whitespace-pre-wrap border rounded p-3 bg-muted/30">{{ item.result.answerContent }}</pre>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-xs text-muted-foreground">배점: {{ item.result.score }}점</span>
                <Button size="sm" @click="applyResult(item.result)">
                  적용
                </Button>
              </div>
            </div>
          </div>

          <!-- 로딩 -->
          <div v-if="loading" class="flex items-center gap-2 text-sm text-muted-foreground">
            <Loader2 class="h-4 w-4 animate-spin" />
            AI가 문제를 생성하고 있습니다...
          </div>

          <!-- 에러 -->
          <div v-if="error" class="text-sm text-destructive border border-destructive/50 rounded-lg p-3">
            {{ error }}
          </div>
        </div>
      </ScrollArea>
      </div>

      <!-- 부모 공통 지문 안내 배너 — 그룹 하위 문제에서 AI 도우미 사용 시 표시 -->
      <div v-if="parent?.content?.trim()" class="border-t px-6 pt-3">
        <div class="border border-blue-500/30 bg-blue-50 dark:bg-blue-950/20 rounded-lg px-3 py-2">
          <div class="flex items-center gap-2">
            <FileText class="h-4 w-4 text-blue-600 shrink-0" />
            <span class="text-xs font-medium text-blue-700 dark:text-blue-400">공통 지문(보기)이 프롬프트에 포함됩니다</span>
          </div>
          <p class="text-xs text-muted-foreground mt-1 truncate">{{ parent.content.trim().substring(0, 120) }}{{ parent.content.trim().length > 120 ? '...' : '' }}</p>
        </div>
      </div>

      <!-- 기존 문제 내용 안내 배너 — 입력 영역 바로 위에 표시 -->
      <div v-if="hasInitialContent" class="border-t px-6 pt-3">
        <div class="border border-amber-500/30 bg-amber-50 dark:bg-amber-950/20 rounded-lg p-3 space-y-2">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-2">
              <FileText class="h-4 w-4 text-amber-600" />
              <span class="text-xs font-medium text-amber-700 dark:text-amber-400">이전에 작성한 문제 내용포함</span>
            </div>
            <button
              type="button"
              class="text-muted-foreground hover:text-foreground transition-colors"
              title="기존 내용 제외"
              @click="dismissInitialContent"
            >
              <X class="h-3.5 w-3.5" />
            </button>
          </div>
          <div v-if="problem?.content?.trim()" class="space-y-1">
            <p class="text-xs text-muted-foreground">문제</p>
            <p class="text-xs truncate">{{ problem.content.trim().substring(0, 100) }}{{ problem.content.trim().length > 100 ? '...' : '' }}</p>
          </div>
          <div v-if="problem?.answerContent?.trim()" class="space-y-1">
            <p class="text-xs text-muted-foreground">채점 기준</p>
            <p class="text-xs truncate">{{ problem.answerContent.trim().substring(0, 100) }}{{ problem.answerContent.trim().length > 100 ? '...' : '' }}</p>
          </div>
        </div>
      </div>

      <!-- 입력 영역 -->
      <div class="border-t px-6 py-4 space-y-2">
        <Textarea
          ref="inputRef"
          v-model="instruction"
          :placeholder="history.length === 0 ? '출제 아이디어를 입력하세요... (예: Java 상속과 인터페이스 차이를 설명하는 문제)' : '개선 요청을 입력하세요...'"
          rows="2"
          class="resize-none"
          @keydown.ctrl.enter.prevent="handleGenerate"
          @keydown.meta.enter.prevent="handleGenerate"
        />
        <div class="flex items-center justify-between">
          <span class="text-xs text-muted-foreground">Ctrl+Enter로 전송</span>
          <Button
            size="sm"
            :disabled="!instruction.trim() || loading"
            @click="handleGenerate"
          >
            <Sparkles class="h-4 w-4 mr-1" />
            {{ history.length === 0 ? '생성' : '개선 요청' }}
          </Button>
        </div>
      </div>
    </DialogContent>
  </Dialog>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { generateAiAssist } from '@/api'
import { renderMarkdown } from '@/lib/markdown'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { Sparkles, Loader2, FileText, X } from 'lucide-vue-next'

const props = defineProps({
  open: Boolean,
  problem: Object,
  parent: Object
})

const emit = defineEmits(['update:open', 'apply'])

const instruction = ref('')
const history = ref([])
const loading = ref(false)
const error = ref('')

const scrollAreaRef = ref(null)
const latestResult = ref(null)
/** 다이얼로그 열릴 때 기존 문제 내용이 있었는지 여부 — UI에 안내 메시지 표시용 */
const hasInitialContent = ref(false)

function scrollToBottom() {
  nextTick(() => {
    const viewport = scrollAreaRef.value?.$el?.querySelector('[data-slot="scroll-area-viewport"]')
    if (viewport) {
      viewport.scrollTop = viewport.scrollHeight
    }
  })
}

/** AI 응답 카드의 시작 부분으로 스크롤 — 응답 전체를 위에서부터 읽을 수 있도록 함 */
function scrollToResult(idx) {
  nextTick(() => {
    const viewport = scrollAreaRef.value?.$el?.querySelector('[data-slot="scroll-area-viewport"]')
    const target = document.getElementById('ai-result-' + idx)
    if (viewport && target) {
      const offsetTop = target.offsetTop - viewport.offsetTop
      viewport.scrollTop = offsetTop - 8
    }
  })
}

// 다른 문제로 전환 시 히스토리 초기화
watch(() => props.problem?.id, () => {
  history.value = []
  latestResult.value = null
  hasInitialContent.value = false
  instruction.value = ''
  error.value = ''
})

// 다이얼로그가 열릴 때 초기화 + 기존 문제 내용 세팅
watch(() => props.open, (opened) => {
  if (!opened) {
    // 닫힐 때 대화 내역 초기화 — 다시 열면 깨끗한 상태에서 시작
    history.value = []
    latestResult.value = null
    hasInitialContent.value = false
    instruction.value = ''
    error.value = ''
    return
  }
  const p = props.problem
  const hasContent = p && (p.content?.trim() || p.answerContent?.trim())
  if (hasContent) {
    latestResult.value = {
      problemContent: p.content || '',
      answerContent: p.answerContent || '',
      score: p.score || 5,
      contentType: p.contentType || 'TEXT'
    }
    hasInitialContent.value = true
  }
})

/** 기존 문제 내용 제외 — 배너를 숨기고 latestResult 초기화하여 AI 요청에서 빠지게 함 */
function dismissInitialContent() {
  hasInitialContent.value = false
  if (history.value.length === 0) {
    latestResult.value = null
  }
}

function renderMd(text) {
  return renderMarkdown(text)
}

async function handleGenerate() {
  const text = instruction.value.trim()
  if (!text || loading.value) return

  const entry = { instruction: text, result: null }
  history.value.push(entry)
  instruction.value = ''
  loading.value = true
  error.value = ''
  scrollToBottom()

  try {
    const data = {
      instruction: text,
      contentType: props.problem?.contentType || 'TEXT',
      score: props.problem?.score || 5
    }

    // 그룹 문제의 부모 공통 지문이 있으면 포함 — AI가 보기를 맥락으로 인식하도록 함
    if (props.parent?.content?.trim()) {
      data.parentContent = props.parent.content.trim()
    }

    // 개선 요청: 최신 결과가 있으면 currentContent/currentAnswer 포함
    if (latestResult.value) {
      data.currentContent = latestResult.value.problemContent
      data.currentAnswer = latestResult.value.answerContent
    }

    const res = await generateAiAssist(data)
    entry.result = res.data
    latestResult.value = res.data
    hasInitialContent.value = false
  } catch (e) {
    if (e.response?.status === 503) {
      error.value = 'Ollama 서비스를 사용할 수 없습니다. Ollama가 실행 중인지 확인해주세요.'
    } else if (e.code === 'ECONNABORTED') {
      error.value = '요청 시간이 초과되었습니다. 다시 시도해주세요.'
    } else {
      error.value = e.response?.data?.error || e.message || 'AI 생성에 실패했습니다.'
    }
  } finally {
    loading.value = false
    scrollToResult(history.value.length - 1)
  }
}

function applyResult(result) {
  emit('apply', result)
  history.value = []
  latestResult.value = null
  hasInitialContent.value = false
  instruction.value = ''
  error.value = ''
  emit('update:open', false)
}
</script>
