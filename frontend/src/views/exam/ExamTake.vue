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
      <!-- 헤더: 시험 제목 + 타이머 (sticky로 스크롤 시 상단 고정) -->
      <div
        class="sticky top-0 z-10 -mx-6 -mt-6 px-6 pt-6 pb-4 bg-background/95 backdrop-blur"
        :class="{ 'border-b': formattedTime !== null }"
      >
        <div class="flex items-start justify-between">
          <div>
            <h1 class="text-2xl font-bold tracking-tight">{{ examStore.currentExam?.title || '시험' }}</h1>
            <p class="text-muted-foreground">모든 문제에 답변한 후 제출하세요.</p>
          </div>
          <!-- 타이머 위젯 -->
          <div
            v-if="formattedTime !== null"
            class="flex flex-col items-center py-2.5 px-4 rounded-xl border-2 shadow-lg transition-colors duration-500 shrink-0"
            :class="timerBgClass"
          >
            <span class="text-[10px] font-medium tracking-widest uppercase opacity-70">남은 시간</span>
            <div class="flex items-center gap-1.5 mt-0.5">
              <Timer class="h-4 w-4" :class="{ 'animate-pulse': remainingSeconds <= 60 }" />
              <span class="font-mono text-2xl font-bold tabular-nums tracking-tight">{{ formattedTime }}</span>
            </div>
            <!-- 프로그레스 바 -->
            <div class="w-full h-1 rounded-full mt-2 overflow-hidden" :class="progressTrackClass">
              <div
                class="h-full rounded-full transition-all duration-1000 ease-linear"
                :class="progressBarClass"
                :style="{ width: progressPercent + '%' }"
              />
            </div>
          </div>
        </div>
      </div>

      <Card v-for="problem in examStore.problems" :key="problem.id" class="relative">
        <CardHeader>
          <CardTitle class="text-base">
            <Badge variant="outline" class="mr-2">Q{{ problem.problemNumber }}</Badge>
          </CardTitle>
        </CardHeader>
        <CardContent class="space-y-3">
          <!-- 문제 내용: contentType에 따라 렌더링 -->
          <div v-if="problem.contentType === 'MARKDOWN'" class="prose prose-sm max-w-none dark:prose-invert" v-html="renderMd(problem.content)"></div>
          <pre v-else class="whitespace-pre-wrap text-sm leading-relaxed">{{ problem.content }}</pre>

          <!-- 답안 입력: 코드 문제면 Monaco, 아니면 Textarea -->
          <div v-if="isCodeProblem(problem)" class="border rounded-md overflow-hidden">
            <div class="flex items-center justify-between bg-muted px-3 py-1.5">
              <span class="text-xs text-muted-foreground font-mono">Code Editor</span>
              <select
                v-model="languages[problem.id]"
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
                :value="answers[problem.id] || ''"
                @change="(val) => answers[problem.id] = val"
                :language="languages[problem.id] || 'java'"
                theme="vs-dark"
                :options="editorOptions"
              />
            </div>
          </div>

          <Textarea
            v-else
            v-model="answers[problem.id]"
            placeholder="답변을 작성하세요..."
            rows="3"
          />
        </CardContent>
      </Card>

      <div v-if="examStore.problems.length === 0" class="text-center py-12 text-muted-foreground">
        문제를 불러오는 중...
      </div>

      <div v-if="examStore.problems.length > 0" class="flex justify-end">
        <Button size="lg" @click="handleSubmit" :disabled="loading">
          {{ loading ? '제출 중...' : '답안 제출' }}
        </Button>
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
import { submitAnswers, createExamSession } from '@/api'
import { Card, CardHeader, CardTitle, CardContent, CardFooter } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Textarea } from '@/components/ui/textarea'
import { Timer } from 'lucide-vue-next'

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

// 타이머 관련 상태
const remainingSeconds = ref(null)
let timerId = null

const CODE_PROBLEM_NUMBERS = [9, 10, 11, 13, 14]

const editorOptions = {
  minimap: { enabled: false },
  fontSize: 14,
  lineNumbers: 'on',
  scrollBeyondLastLine: false,
  automaticLayout: true,
  tabSize: 4,
  wordWrap: 'on'
}

// MM:SS 포맷. 시간 제한 없으면 null 반환
const formattedTime = computed(() => {
  if (remainingSeconds.value === null) return null
  const mins = Math.floor(remainingSeconds.value / 60)
  const secs = remainingSeconds.value % 60
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
})

// 전체 시간(초) — 프로그레스 바 계산용
const totalSeconds = ref(null)

// 타이머 배경 스타일: 평소 진한 배경, 5분 이하 amber, 1분 이하 red
const timerBgClass = computed(() => {
  if (remainingSeconds.value === null) return ''
  if (remainingSeconds.value <= 60) return 'bg-red-50/95 border-red-400 text-red-700 shadow-red-200/50'
  if (remainingSeconds.value <= 300) return 'bg-amber-50/95 border-amber-400 text-amber-700 shadow-amber-200/50'
  return 'bg-slate-900/95 border-slate-700 text-white shadow-slate-400/30'
})

// 프로그레스 바 트랙(배경) 색상
const progressTrackClass = computed(() => {
  if (remainingSeconds.value <= 60) return 'bg-red-200'
  if (remainingSeconds.value <= 300) return 'bg-amber-200'
  return 'bg-slate-700'
})

// 프로그레스 바 색상
const progressBarClass = computed(() => {
  if (remainingSeconds.value <= 60) return 'bg-red-500'
  if (remainingSeconds.value <= 300) return 'bg-amber-500'
  return 'bg-emerald-400'
})

// 남은 시간 비율 (%)
const progressPercent = computed(() => {
  if (totalSeconds.value === null || totalSeconds.value === 0) return 100
  return Math.max(0, (remainingSeconds.value / totalSeconds.value) * 100)
})

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
  return CODE_PROBLEM_NUMBERS.includes(problem.problemNumber)
}

function renderMd(text) {
  return renderMarkdown(text)
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
    // 공개 API(loadActiveExam)로 시험 데이터 로드
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

  examStore.problems.forEach(p => {
    if (isCodeProblem(p)) {
      if (p.problemNumber === 13) {
        languages[p.id] = 'sql'
      } else if (p.problemNumber === 11) {
        languages[p.id] = 'javascript'
      } else {
        languages[p.id] = 'java'
      }
    }
  })

  // localStorage에서 이전 답안 복원 (새로고침 대응)
  const storageKey = `exam_${examId}_answers`
  try {
    const saved = localStorage.getItem(storageKey)
    if (saved) {
      Object.assign(answers, JSON.parse(saved))
    }
  } catch { /* 파싱 실패 시 무시 */ }

  // 시간 제한이 있는 시험: 세션 생성 후 타이머 시작
  if (examStore.currentExam?.timeLimit) {
    try {
      const { data } = await createExamSession(authStore.examinee.id, examId)
      if (data.remainingSeconds !== null && data.remainingSeconds !== undefined) {
        if (data.remainingSeconds <= 0) {
          // 이미 시간 초과: 즉시 자동 제출
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

// 페이지 이탈 방지: 브라우저 새로고침/탭 닫기 시 확인 다이얼로그
function handleBeforeUnload(e) {
  if (!submitted.value) {
    e.preventDefault()
  }
}
window.addEventListener('beforeunload', handleBeforeUnload)

// 페이지 이탈 방지: Vue Router 이동 시 확인
onBeforeRouteLeave(() => {
  if (!submitted.value) {
    return confirm('변경사항이 저장되지 않을 수 있습니다.')
  }
})

onUnmounted(() => {
  stopTimer()
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

async function handleSubmit() {
  if (!authStore.examinee) {
    router.push('/exam/login')
    return
  }

  loading.value = true
  stopTimer()
  try {
    const answerList = examStore.problems.map(p => ({
      problemId: p.id,
      answer: answers[p.id] || ''
    }))

    await submitAnswers(
      authStore.examinee.id,
      route.params.examId,
      answerList
    )

    submitted.value = true
    // 제출 성공 시 localStorage 정리
    localStorage.removeItem(`exam_${route.params.examId}_answers`)
    authStore.clear()
  } catch (e) {
    if (e.response?.status === 409) {
      alert('이미 응시 완료한 시험입니다.')
      router.push('/exam/login')
    } else if (e.response?.status === 403) {
      // 서버 측에서 시간 초과 판정 — 세션 데이터 정리
      localStorage.removeItem(`exam_${route.params.examId}_answers`)
      authStore.clear()
      timeExpired.value = true
      submitted.value = true
    } else {
      // 시간 만료로 인한 자동 제출 실패 시에도 완료 처리 (사용자 혼란 방지)
      if (timeExpired.value) {
        localStorage.removeItem(`exam_${route.params.examId}_answers`)
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
