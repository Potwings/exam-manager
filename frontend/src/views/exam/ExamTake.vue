<template>
  <div class="space-y-6">
    <!-- 제출 완료 상태: 완료 메시지 Card 표시 -->
    <template v-if="submitted">
      <div class="flex items-center justify-center min-h-[40vh]">
        <Card class="w-full max-w-md text-center">
          <CardHeader>
            <CardTitle class="text-2xl">{{ timeExpired ? '시간 종료' : '제출 완료' }}</CardTitle>
          </CardHeader>
          <CardContent class="space-y-4">
            <p v-if="timeExpired" class="text-muted-foreground">시험 시간이 종료되어 답안이 자동 제출되었습니다.</p>
            <p v-else class="text-muted-foreground">답안이 정상적으로 제출되었습니다.</p>
          </CardContent>
          <CardFooter class="justify-center">
            <Button @click="router.push('/exam/login')">돌아가기</Button>
          </CardFooter>
        </Card>
      </div>
    </template>

    <!-- 세션 생성 실패: 시험 차단 + 재시도 -->
    <template v-else-if="sessionError">
      <div class="flex items-center justify-center min-h-[40vh]">
        <Card class="w-full max-w-md text-center">
          <CardHeader>
            <CardTitle class="text-2xl">세션 오류</CardTitle>
          </CardHeader>
          <CardContent class="space-y-4">
            <p class="text-muted-foreground">시험 세션을 생성하지 못했습니다. 네트워크 연결을 확인해주세요.</p>
          </CardContent>
          <CardFooter class="justify-center gap-2">
            <Button variant="outline" @click="router.push('/exam/login')">돌아가기</Button>
            <Button @click="retrySession">재시도</Button>
          </CardFooter>
        </Card>
      </div>
    </template>

    <!-- 시험 응시 상태 -->
    <template v-else>
      <!-- 헤더: 페이지 진행(좌) + 타이머·관리자 호출(우, xl 미만만 표시) (sticky) -->
      <div class="sticky top-0 z-10 -mx-6 -mt-6 px-6 py-2.5 bg-background/95 backdrop-blur border-b">
        <div class="flex items-center justify-between gap-3">
          <p v-if="pages.length > 0" class="text-sm text-muted-foreground whitespace-nowrap">
            문제 {{ currentPageIndex + 1 }} / {{ pages.length }} · 답변 완료 {{ answeredCount }}/{{ pages.length
            }}<template v-if="unansweredLabels.length > 5"> (미답변: {{ unansweredLabels.slice(0, 5).join(', ') }}... 총 {{ unansweredLabels.length }}개)</template
            ><template v-else-if="unansweredLabels.length > 3"> (미답변: {{ unansweredLabels.join(', ') }} 총 {{ unansweredLabels.length }}개)</template
            ><template v-else-if="unansweredLabels.length > 0"> (미답변: {{ unansweredLabels.join(', ') }})</template>
          </p>
          <p v-else class="text-sm text-muted-foreground">문제를 불러오는 중...</p>
          <!-- xl 미만: 상단 헤더에 도구 표시 / xl 이상: 우측 Teleport로 이동 -->
          <div class="flex items-center gap-2 shrink-0 xl:hidden">
            <Button variant="destructive" size="sm" @click="handleCallAdmin" :disabled="callCooldown > 0 || isCallingAdmin" class="text-xs h-7 px-2.5">
              {{ callCooldown > 0 ? `${callCooldown}초` : '관리자 호출' }}
            </Button>
            <div
              v-if="formattedTime !== null"
              class="flex items-center gap-1.5 py-1 px-2.5 rounded-lg border-2 transition-colors duration-500"
              :class="timerBgClass"
            >
              <Timer class="h-3.5 w-3.5" :class="{ 'animate-pulse': remainingSeconds <= 60 }" />
              <span class="font-mono text-base font-bold tabular-nums tracking-tight">{{ formattedTime }}</span>
              <div class="w-12 h-1 rounded-full overflow-hidden" :class="progressTrackClass">
                <div
                  class="h-full rounded-full transition-all duration-1000 ease-linear"
                  :class="progressBarClass"
                  :style="{ width: progressPercent + '%' }"
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 시험 제목 (스크롤 시 사라짐) -->
      <h1 class="text-2xl font-bold tracking-tight">{{ examStore.currentExam?.title || '시험' }}</h1>

      <!-- 단일 문제 페이지 렌더링 -->
      <template v-if="currentPage">
        <Card :key="currentPage.problemId">
          <CardHeader>
            <div class="flex items-center justify-between">
              <CardTitle class="text-base">
                <template v-if="currentPage.type === 'group-child'">
                  <Badge variant="outline" class="mr-2">Q{{ currentPage.parent.problemNumber }}</Badge>
                  <Badge variant="secondary" class="text-xs mr-2">그룹</Badge>
                  <Badge variant="outline">Q{{ currentPage.parent.problemNumber }}-{{ currentPage.problem.problemNumber }}</Badge>
                </template>
                <template v-else>
                  <Badge variant="outline" class="mr-2">Q{{ currentPage.problem.problemNumber }}</Badge>
                </template>
              </CardTitle>
              <span class="text-sm text-muted-foreground tabular-nums">{{ currentPageIndex + 1 }} / {{ pages.length }}</span>
            </div>
          </CardHeader>
          <CardContent class="space-y-4">
            <!-- 그룹 자식: 공통 지문 표시 -->
            <div v-if="currentPage.type === 'group-child'" class="border-l-2 border-blue-200 dark:border-blue-800 pl-4 pb-2">
              <p class="text-xs font-medium text-muted-foreground mb-2">공통 지문</p>
              <div v-if="currentPage.parent.contentType === 'MARKDOWN'" class="prose prose-sm max-w-none dark:prose-invert" v-html="renderMd(currentPage.parent.content)"></div>
              <pre v-else class="whitespace-pre-wrap text-sm leading-relaxed">{{ currentPage.parent.content }}</pre>
            </div>

            <!-- 문제 내용 -->
            <div>
              <div v-if="currentPage.problem.contentType === 'MARKDOWN'" class="prose prose-sm max-w-none dark:prose-invert" v-html="renderMd(currentPage.problem.content)"></div>
              <pre v-else class="whitespace-pre-wrap text-sm leading-relaxed">{{ currentPage.problem.content }}</pre>
            </div>

            <!-- 답안 입력: 코드 에디터 -->
            <div v-if="isCodeProblem(currentPage.problem)" :key="'editor-' + currentPage.problemId" class="border rounded-md overflow-hidden">
              <div class="flex items-center justify-between bg-muted px-3 py-1.5">
                <span class="text-xs text-muted-foreground font-mono">Code Editor</span>
                <select
                  v-model="languages[currentPage.problemId]"
                  class="text-xs bg-transparent border rounded px-1.5 py-0.5"
                >
                  <option value="java">Java</option>
                  <option value="javascript">JavaScript</option>
                  <option value="python">Python</option>
                  <option value="sql">SQL</option>
                </select>
              </div>
              <div style="height: 200px">
                <vue-monaco-editor
                  :key="'monaco-' + currentPage.problemId"
                  :value="answers[currentPage.problemId] || ''"
                  @change="(val) => answers[currentPage.problemId] = val"
                  :language="languages[currentPage.problemId] || 'java'"
                  theme="vs-dark"
                  :options="editorOptions"
                />
              </div>
            </div>

            <!-- 답안 입력: 텍스트 영역 -->
            <Textarea
              v-else
              :key="'textarea-' + currentPage.problemId"
              v-model="answers[currentPage.problemId]"
              placeholder="답변을 작성하세요..."
              rows="3"
            />
          </CardContent>
        </Card>
      </template>

      <div v-else-if="pages.length === 0" class="text-center py-12 text-muted-foreground">
        문제를 불러오는 중...
      </div>

      <!-- 우측 도구 모음: xl 이상에서 main 바깥 우측에 표시 (Teleport) -->
      <Teleport to="body">
        <div class="fixed z-30 hidden xl:flex flex-col items-center gap-2 w-28" style="left: calc(50% + 33rem); top: 6rem;">
          <!-- 타이머 위젯 -->
          <div
            v-if="formattedTime !== null"
            class="w-full flex flex-col items-center py-2 px-3 rounded-xl border-2 shadow-lg transition-colors duration-500"
            :class="timerBgClass"
          >
            <span class="text-[10px] font-medium tracking-widest uppercase opacity-70">남은 시간</span>
            <div class="flex items-center gap-1 mt-0.5">
              <Timer class="h-3.5 w-3.5" :class="{ 'animate-pulse': remainingSeconds <= 60 }" />
              <span class="font-mono text-lg font-bold tabular-nums tracking-tight">{{ formattedTime }}</span>
            </div>
            <div class="w-full h-1 rounded-full mt-1.5 overflow-hidden" :class="progressTrackClass">
              <div
                class="h-full rounded-full transition-all duration-1000 ease-linear"
                :class="progressBarClass"
                :style="{ width: progressPercent + '%' }"
              />
            </div>
          </div>
          <!-- 관리자 호출 -->
          <Button variant="destructive" size="sm" @click="handleCallAdmin" :disabled="callCooldown > 0 || isCallingAdmin" class="text-xs h-8 px-2 w-full">
            {{ callCooldown > 0 ? `${callCooldown}초` : '관리자 호출' }}
          </Button>
        </div>
      </Teleport>

      <!-- 네비게이션 바 (full-width, 하단 고정) -->
      <div v-if="pages.length > 0" class="sticky bottom-0 -mx-6 -mb-6 px-6 py-3 bg-background/95 backdrop-blur border-t">
        <div class="flex items-center justify-between gap-2">
          <!-- 이전 버튼 -->
          <Button variant="outline" size="sm" @click="goToPrevPage" :disabled="currentPageIndex === 0" class="shrink-0">
            <ChevronLeft class="h-4 w-4 mr-1" />
            이전
          </Button>

          <!-- 페이지 선택 (가운데) -->
          <Popover v-model:open="showPagePicker">
            <PopoverTrigger as-child>
              <Button variant="outline" size="sm" class="font-mono tabular-nums min-w-[5rem]">
                {{ currentPageIndex + 1 }} / {{ pages.length }}
              </Button>
            </PopoverTrigger>
            <PopoverContent class="w-auto p-3" align="center" side="top" :side-offset="8">
              <p class="text-xs text-muted-foreground mb-2">문제 번호를 선택하세요</p>
              <div class="grid gap-1.5" :style="{ gridTemplateColumns: `repeat(${Math.min(pages.length, 6)}, 1fr)` }">
                <button
                  v-for="(page, idx) in pages" :key="page.problemId"
                  class="h-8 min-w-[2.5rem] px-1.5 text-xs rounded-md border transition-colors inline-flex items-center justify-center gap-0.5"
                  :class="[
                    idx === currentPageIndex
                      ? 'bg-primary text-primary-foreground border-primary font-semibold'
                      : (answers[page.problemId] || '').trim()
                        ? 'bg-emerald-500/15 text-emerald-700 dark:text-emerald-400 border-emerald-500/30 hover:bg-emerald-500/25'
                        : 'bg-background text-muted-foreground border-input hover:bg-accent'
                  ]"
                  @click="goToPage(idx); showPagePicker = false"
                >
                  <Check v-if="(answers[page.problemId] || '').trim() && idx !== currentPageIndex" class="h-3 w-3 shrink-0" />
                  {{ page.label }}
                </button>
              </div>
            </PopoverContent>
          </Popover>

          <!-- 다음 + 제출 버튼 -->
          <div class="flex items-center gap-2 shrink-0">
            <Button variant="outline" size="sm" @click="goToNextPage" :disabled="currentPageIndex >= pages.length - 1">
              다음
              <ChevronRight class="h-4 w-4 ml-1" />
            </Button>
            <AlertDialog v-model:open="showSubmitDialog">
              <AlertDialogTrigger as-child>
                <Button size="sm" :disabled="loading">
                  {{ loading ? '제출 중...' : '답안 제출' }}
                </Button>
              </AlertDialogTrigger>
              <AlertDialogContent>
                <AlertDialogHeader>
                  <AlertDialogTitle>답안을 제출하시겠습니까?</AlertDialogTitle>
                  <AlertDialogDescription>
                    <span>답변 완료 {{ answeredCount }}/{{ pages.length }}문제</span>
                    <template v-if="unansweredLabels.length > 0">
                      <br><span class="text-orange-500">미답변 {{ unansweredLabels.length }}개: {{ unansweredLabels.length <= 5 ? unansweredLabels.join(', ') : unansweredLabels.slice(0, 5).join(', ') + '...' }}</span>
                    </template>
                    <br><span>제출 후에는 수정할 수 없습니다.</span>
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogAction @click="handleSubmit">제출</AlertDialogAction>
                  <AlertDialogCancel>취소</AlertDialogCancel>
                </AlertDialogFooter>
              </AlertDialogContent>
            </AlertDialog>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { renderMarkdown } from '@/lib/markdown'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { useExamStore } from '@/stores/examStore'
import { submitAnswers, createExamSession, callAdmin } from '@/api'
import { Card, CardHeader, CardTitle, CardContent, CardFooter } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import {
  AlertDialog, AlertDialogTrigger, AlertDialogContent, AlertDialogHeader,
  AlertDialogTitle, AlertDialogDescription, AlertDialogFooter, AlertDialogCancel, AlertDialogAction
} from '@/components/ui/alert-dialog'
import { Popover, PopoverTrigger, PopoverContent } from '@/components/ui/popover'
import { Badge } from '@/components/ui/badge'
import { Textarea } from '@/components/ui/textarea'
import { Timer, ChevronLeft, ChevronRight, Check } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const examStore = useExamStore()

const answers = reactive({})
const languages = reactive({})
const loading = ref(false)
const submitted = ref(false)
const timeExpired = ref(false)
const sessionError = ref(false)
const callCooldown = ref(0)
const isCallingAdmin = ref(false)
const showSubmitDialog = ref(false)
const showPagePicker = ref(false)
let callCooldownTimer = null

// 타이머 관련 상태
const remainingSeconds = ref(null)
let timerId = null

// 페이지 탐색 상태
const currentPageIndex = ref(0)

const editorOptions = {
  minimap: { enabled: false },
  fontSize: 14,
  lineNumbers: 'on',
  scrollBeyondLastLine: false,
  automaticLayout: true,
  tabSize: 4,
  wordWrap: 'on'
}

// 문제를 페이지 단위로 변환 (1문제 = 1페이지, 그룹 자식은 개별 페이지)
const pages = computed(() => {
  const result = []
  for (const p of examStore.problems) {
    if (p.children && p.children.length > 0) {
      for (const child of p.children) {
        result.push({
          type: 'group-child',
          parent: p,
          problem: child,
          problemId: child.id,
          label: `${p.problemNumber}-${child.problemNumber}`
        })
      }
    } else {
      result.push({
        type: 'independent',
        parent: null,
        problem: p,
        problemId: p.id,
        label: `${p.problemNumber}`
      })
    }
  }
  return result
})

// 현재 페이지 객체 (bounds 보호)
const currentPage = computed(() => {
  const idx = currentPageIndex.value
  if (idx >= 0 && idx < pages.value.length) return pages.value[idx]
  return pages.value[0] || null
})

// 답변 완료된 페이지 수
const answeredCount = computed(() => {
  return pages.value.filter(p => (answers[p.problemId] || '').trim() !== '').length
})

// 미답변 문제 번호 목록 (라벨 배열)
const unansweredLabels = computed(() => {
  return pages.value
    .filter(p => (answers[p.problemId] || '').trim() === '')
    .map(p => p.label)
})

// 답안 입력 가능한 문제만 추출 (독립 문제 + 그룹의 하위 문제)
const answerableProblems = computed(() => {
  const result = []
  for (const p of examStore.problems) {
    if (p.children && p.children.length > 0) {
      result.push(...p.children)
    } else {
      result.push(p)
    }
  }
  return result
})

// MM:SS 포맷. 시간 제한 없으면 null 반환
const formattedTime = computed(() => {
  if (remainingSeconds.value === null) return null
  const mins = Math.floor(remainingSeconds.value / 60)
  const secs = remainingSeconds.value % 60
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
})

// 전체 시간(초) — 프로그레스 바 계산용
const totalSeconds = ref(null)

const timerBgClass = computed(() => {
  if (remainingSeconds.value === null) return ''
  if (remainingSeconds.value <= 60) return 'bg-red-50/95 border-red-400 text-red-700 shadow-red-200/50'
  if (remainingSeconds.value <= 300) return 'bg-amber-50/95 border-amber-400 text-amber-700 shadow-amber-200/50'
  return 'bg-slate-900/95 border-slate-700 text-white shadow-slate-400/30'
})

const progressTrackClass = computed(() => {
  if (remainingSeconds.value <= 60) return 'bg-red-200'
  if (remainingSeconds.value <= 300) return 'bg-amber-200'
  return 'bg-slate-700'
})

const progressBarClass = computed(() => {
  if (remainingSeconds.value <= 60) return 'bg-red-500'
  if (remainingSeconds.value <= 300) return 'bg-amber-500'
  return 'bg-emerald-400'
})

const progressPercent = computed(() => {
  if (totalSeconds.value === null || totalSeconds.value === 0) return 100
  return Math.max(0, (remainingSeconds.value / totalSeconds.value) * 100)
})

// 페이지 네비게이션 함수
function goToNextPage() {
  if (currentPageIndex.value < pages.value.length - 1) {
    currentPageIndex.value++
    scrollToTop()
  }
}

function goToPrevPage() {
  if (currentPageIndex.value > 0) {
    currentPageIndex.value--
    scrollToTop()
  }
}

function goToPage(index) {
  if (index >= 0 && index < pages.value.length) {
    currentPageIndex.value = index
    scrollToTop()
  }
}

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function startTimer(seconds) {
  remainingSeconds.value = seconds
  if (totalSeconds.value === null) totalSeconds.value = examStore.currentExam.timeLimit * 60
  timerId = setInterval(() => {
    if (remainingSeconds.value <= 0) {
      stopTimer()
      timeExpired.value = true
      handleSubmit()
      return
    }
    remainingSeconds.value--
  }, 1000)
}

function stopTimer() {
  if (timerId) {
    clearInterval(timerId)
    timerId = null
  }
}

function isCodeProblem(problem) {
  return !!problem.codeEditor
}

function renderMd(text) {
  return renderMarkdown(text)
}

function initLanguages(problems) {
  for (const p of problems) {
    if (p.children && p.children.length > 0) {
      for (const child of p.children) {
        if (isCodeProblem(child)) {
          languages[child.id] = child.codeLanguage || 'java'
        }
      }
    } else {
      if (isCodeProblem(p)) {
        languages[p.id] = p.codeLanguage || 'java'
      }
    }
  }
}

onMounted(async () => {
  if (!authStore.examinee?.id) {
    router.push('/exam/login')
    return
  }

  const examId = route.params.examId
  if (!examId) {
    router.push('/exam/login')
    return
  }

  try {
    if (!examStore.activeExam || String(examStore.activeExam.id) !== String(examId)) {
      await examStore.loadActiveExam()
    }
    if (examStore.activeExam && String(examStore.activeExam.id) === String(examId)) {
      examStore.currentExam = examStore.activeExam
      examStore.problems = examStore.activeExam.problems || []
    } else {
      alert('시험을 찾을 수 없습니다.')
      router.push('/exam/login')
      return
    }
  } catch (e) {
    alert('문제를 불러오지 못했습니다: ' + (e.response?.data?.message || e.message))
    router.push('/exam/login')
    return
  }

  initLanguages(examStore.problems)

  // localStorage에서 이전 답안 복원 (새로고침 대응)
  const storageKey = `exam_${examId}_answers`
  try {
    const saved = localStorage.getItem(storageKey)
    if (saved) {
      Object.assign(answers, JSON.parse(saved))
    }
  } catch { /* 파싱 실패 시 무시 */ }

  // localStorage에서 페이지 위치 복원 (새로고침 대응)
  const pageKey = `exam_${examId}_page`
  try {
    const savedPage = localStorage.getItem(pageKey)
    if (savedPage !== null) {
      const pageIdx = parseInt(savedPage, 10)
      if (!isNaN(pageIdx) && pageIdx >= 0 && pageIdx < pages.value.length) {
        currentPageIndex.value = pageIdx
      }
    }
  } catch { /* 파싱 실패 시 무시 */ }

  // 시간 제한이 있는 시험: 세션 생성 후 타이머 시작
  if (examStore.currentExam?.timeLimit) {
    try {
      const { data } = await createExamSession(authStore.examinee.id, examId)
      if (data.remainingSeconds !== null && data.remainingSeconds !== undefined) {
        if (data.remainingSeconds <= 0) {
          timeExpired.value = true
          handleSubmit()
        } else {
          startTimer(data.remainingSeconds)
        }
      }
    } catch (e) {
      console.error('세션 생성 실패:', e)
      sessionError.value = true
    }
  }
})

async function retrySession() {
  sessionError.value = false
  try {
    const examId = route.params.examId
    const { data } = await createExamSession(authStore.examinee.id, examId)
    if (data.remainingSeconds !== null && data.remainingSeconds !== undefined) {
      if (data.remainingSeconds <= 0) {
        timeExpired.value = true
        handleSubmit()
      } else {
        startTimer(data.remainingSeconds)
      }
    }
  } catch (e) {
    console.error('세션 재시도 실패:', e)
    sessionError.value = true
  }
}

// answers 변경 시 localStorage에 자동 저장 (새로고침 대응)
watch(answers, (val) => {
  const examId = route.params.examId
  if (examId) {
    localStorage.setItem(`exam_${examId}_answers`, JSON.stringify(val))
  }
}, { deep: true })

// currentPageIndex 변경 시 localStorage에 저장 (새로고침 대응)
watch(currentPageIndex, (val) => {
  const examId = route.params.examId
  if (examId) {
    localStorage.setItem(`exam_${examId}_page`, String(val))
  }
})

// 페이지 이탈 방지: 브라우저 새로고침/탭 닫기 시 확인 다이얼로그
function handleBeforeUnload(e) {
  if (!submitted.value && !sessionError.value) {
    e.preventDefault()
    e.returnValue = ''
  }
}
window.addEventListener('beforeunload', handleBeforeUnload)

// 페이지 이탈 방지: Vue Router 이동 시 확인
onBeforeRouteLeave(() => {
  if (!submitted.value && !sessionError.value) {
    return confirm('변경사항이 저장되지 않을 수 있습니다.')
  }
})

onUnmounted(() => {
  stopTimer()
  clearInterval(callCooldownTimer)
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

async function handleCallAdmin() {
  if (callCooldown.value > 0 || isCallingAdmin.value || !authStore.examinee) return

  isCallingAdmin.value = true
  try {
    await callAdmin(
      authStore.examinee.id,
      route.params.examId,
      authStore.examinee.name
    )
    // 30초 쿨다운 시작 — 스팸 방지
    callCooldown.value = 30
    callCooldownTimer = setInterval(() => {
      callCooldown.value--
      if (callCooldown.value <= 0) {
        clearInterval(callCooldownTimer)
        callCooldownTimer = null
      }
    }, 1000)
  } catch {
    alert('관리자 호출에 실패했습니다. 잠시 후 다시 시도해주세요.')
  } finally {
    isCallingAdmin.value = false
  }
}

async function handleSubmit() {
  if (!authStore.examinee) {
    router.push('/exam/login')
    return
  }

  loading.value = true
  stopTimer()
  try {
    // 답안 입력 가능한 문제만 제출 (독립 문제 + 그룹 하위 문제)
    const answerList = answerableProblems.value.map(p => ({
      problemId: p.id,
      answer: answers[p.id] || ''
    }))

    await submitAnswers(
      authStore.examinee.id,
      route.params.examId,
      answerList
    )

    submitted.value = true
    localStorage.removeItem(`exam_${route.params.examId}_answers`)
    localStorage.removeItem(`exam_${route.params.examId}_page`)
    authStore.clear()
  } catch (e) {
    if (e.response?.status === 409) {
      alert('이미 응시 완료한 시험입니다.')
      router.push('/exam/login')
    } else if (e.response?.status === 403) {
      localStorage.removeItem(`exam_${route.params.examId}_answers`)
      localStorage.removeItem(`exam_${route.params.examId}_page`)
      authStore.clear()
      timeExpired.value = true
      submitted.value = true
    } else {
      if (timeExpired.value) {
        localStorage.removeItem(`exam_${route.params.examId}_answers`)
        localStorage.removeItem(`exam_${route.params.examId}_page`)
        authStore.clear()
        submitted.value = true
      } else {
        alert('제출 실패: ' + (e.response?.data?.message || e.message))
      }
    }
  } finally {
    loading.value = false
  }
}
</script>
