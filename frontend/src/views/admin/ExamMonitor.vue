<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold tracking-tight">응시 현황</h1>
      <p class="text-muted-foreground">현재 시험 응시 상태를 모니터링합니다.</p>
    </div>

    <Card>
      <CardHeader>
        <div class="flex items-center justify-between">
          <CardTitle>현황</CardTitle>
          <div class="w-64">
            <select
              v-model="selectedExamId"
              @change="handleExamChange"
              class="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
            >
              <option value="" disabled>시험 선택</option>
              <option v-for="exam in exams" :key="exam.id" :value="exam.id">
                {{ exam.title }} ({{ exam.problemCount }}문제)
              </option>
            </select>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <template v-if="selectedExamId">
          <Table v-if="sessions.length > 0">
            <TableHeader>
              <TableRow>
                <TableHead>이름</TableHead>
                <TableHead>생년월일</TableHead>
                <TableHead>상태</TableHead>
                <TableHead>남은 시간</TableHead>
                <TableHead>시작 시각</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-for="s in sessions" :key="s.examineeId">
                <TableCell class="font-medium">{{ s.examineeName }}</TableCell>
                <TableCell class="text-muted-foreground">{{ s.examineeBirthDate || '-' }}</TableCell>
                <TableCell>
                  <Badge :variant="statusVariant(s.status)">
                    {{ statusLabel(s.status) }}
                  </Badge>
                </TableCell>
                <TableCell>
                  <span
                    v-if="displayTime(s) !== null"
                    class="font-mono tabular-nums"
                    :class="timeColorClass(displayTime(s))"
                  >
                    {{ formatTime(displayTime(s)) }}
                  </span>
                  <span v-else class="text-muted-foreground">-</span>
                </TableCell>
                <TableCell class="text-muted-foreground">{{ formatStartedAt(s.startedAt) }}</TableCell>
              </TableRow>
            </TableBody>
          </Table>
          <p v-else class="text-center py-6 text-muted-foreground">
            응시자가 없습니다.
          </p>

          <!-- 요약 섹션 -->
          <div v-if="sessions.length > 0" class="mt-4 text-sm text-muted-foreground text-center">
            총 {{ sessions.length }}명 · 응시 중 {{ inProgressCount }}명 · 제출 완료 {{ submittedCount }}명
            <template v-if="expiredCount > 0"> · 시간 만료 {{ expiredCount }}명</template>
          </div>
        </template>
        <p v-else class="text-center py-6 text-muted-foreground">
          시험을 선택하세요.
        </p>
      </CardContent>
    </Card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { fetchExams, fetchMonitorSessions } from '@/api'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'

const exams = ref([])
const sessions = ref([])
const selectedExamId = ref('')

// 카운트다운을 위한 기준 시각: 서버 응답을 받은 시점의 Date.now()
// 서버에서 받은 remainingSeconds는 이 시점 기준이므로,
// 이후 매초 (Date.now() - fetchedAt) / 1000 만큼 차감하여 실시간 카운트다운 구현
let fetchedAt = null

// 매초 증가하는 tick 카운터: computed가 displayTime을 다시 계산하도록 반응성 트리거
const tick = ref(0)

let pollingTimer = null
let countdownTimer = null

// --- 시험 목록 로드 + 초기 선택 ---
onMounted(async () => {
  try {
    const { data } = await fetchExams()
    exams.value = data
    if (exams.value.length > 0) {
      const activeExam = exams.value.find(e => e.active)
      selectedExamId.value = activeExam ? activeExam.id : exams.value[0].id
      await loadSessions()
    }
  } catch {
    // 시험 목록 로드 실패 시 빈 상태 유지
  }
})

onUnmounted(() => {
  stopPolling()
  stopCountdown()
})

// --- 시험 변경 핸들러 ---
async function handleExamChange() {
  await loadSessions()
}

// --- 세션 데이터 로드 ---
async function loadSessions() {
  stopPolling()
  stopCountdown()

  if (!selectedExamId.value) {
    sessions.value = []
    return
  }

  const examId = selectedExamId.value
  try {
    const { data } = await fetchMonitorSessions(examId)
    // 응답 수신 시점에 시험이 변경되었으면 stale 응답이므로 무시
    if (examId !== selectedExamId.value) return

    sessions.value = data
    fetchedAt = Date.now()
    tick.value = 0
    startCountdownIfNeeded()
    startPollingIfNeeded()
  } catch {
    sessions.value = []
  }
}

// --- 폴링: 10초 간격, IN_PROGRESS가 있을 때만 ---
function startPollingIfNeeded() {
  const hasInProgress = sessions.value.some(s => s.status === 'IN_PROGRESS')
  if (hasInProgress && !pollingTimer) {
    pollingTimer = setInterval(async () => {
      const examId = selectedExamId.value
      try {
        const { data } = await fetchMonitorSessions(examId)
        // 응답 수신 시점에 시험이 변경되었으면 stale 응답이므로 무시
        if (examId !== selectedExamId.value) return

        sessions.value = data
        fetchedAt = Date.now()
        tick.value = 0

        const stillInProgress = data.some(s => s.status === 'IN_PROGRESS')
        if (!stillInProgress) {
          stopPolling()
          stopCountdown()
        }
      } catch {
        // 폴링 실패 시 기존 데이터 유지
      }
    }, 10000)
  }
}

function stopPolling() {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

// --- 카운트다운: 1초 간격으로 tick 증가 ---
// tick이 증가하면 displayTime computed가 재계산되어 UI가 갱신됨
function startCountdownIfNeeded() {
  const hasInProgress = sessions.value.some(s => s.status === 'IN_PROGRESS')
  if (hasInProgress && !countdownTimer) {
    countdownTimer = setInterval(() => {
      tick.value++
    }, 1000)
  }
}

function stopCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

// --- 남은 시간 계산 ---
// tick ref에 의존하므로 tick이 변경될 때마다 반응적으로 재평가됨
// remainingSeconds가 null(시간 제한 없음)이거나 제출 완료/시간 만료인 경우 null 반환
function displayTime(session) {
  // tick.value를 참조하여 Vue 반응성 시스템에 의존성 등록
  // eslint-disable-next-line no-unused-expressions
  tick.value

  if (session.status !== 'IN_PROGRESS' || session.remainingSeconds == null || fetchedAt == null) {
    return null
  }

  const elapsedSeconds = Math.floor((Date.now() - fetchedAt) / 1000)
  const remaining = session.remainingSeconds - elapsedSeconds
  return Math.max(0, remaining)
}

// --- 상태 Badge ---
function statusVariant(status) {
  switch (status) {
    case 'IN_PROGRESS': return 'default'
    case 'SUBMITTED': return 'secondary'
    case 'TIME_EXPIRED': return 'destructive'
    default: return 'outline'
  }
}

function statusLabel(status) {
  switch (status) {
    case 'IN_PROGRESS': return '응시 중'
    case 'SUBMITTED': return '제출 완료'
    case 'TIME_EXPIRED': return '시간 만료'
    default: return status
  }
}

// --- 시간 색상 클래스 ---
// ExamTake.vue의 타이머 색상 규칙과 동일: 5분 이하 amber, 1분 이하 red + pulse
function timeColorClass(seconds) {
  if (seconds == null) return ''
  if (seconds <= 60) return 'text-red-600 animate-pulse'
  if (seconds <= 300) return 'text-amber-600'
  return ''
}

// --- 시간 포맷: MM:SS ---
function formatTime(seconds) {
  if (seconds == null) return '-'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

// --- 시작 시각 포맷: HH:mm:ss ---
function formatStartedAt(dt) {
  if (!dt) return '-'
  const d = new Date(dt)
  return d.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false })
}

// --- 요약 집계 ---
const inProgressCount = computed(() => sessions.value.filter(s => s.status === 'IN_PROGRESS').length)
const submittedCount = computed(() => sessions.value.filter(s => s.status === 'SUBMITTED').length)
const expiredCount = computed(() => sessions.value.filter(s => s.status === 'TIME_EXPIRED').length)
</script>
