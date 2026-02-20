<template>
  <div class="space-y-6">
    <!-- 제출 완료 상태: 완료 메시지 Card 표시 -->
    <template v-if="submitted">
      <div class="flex items-center justify-center min-h-[40vh]">
        <Card class="w-full max-w-md text-center">
          <CardHeader>
            <CardTitle class="text-2xl">제출 완료</CardTitle>
          </CardHeader>
          <CardContent class="space-y-4">
            <p class="text-muted-foreground">답안이 정상적으로 제출되었습니다.</p>
          </CardContent>
          <CardFooter class="justify-center">
            <Button @click="router.push('/exam/login')">돌아가기</Button>
          </CardFooter>
        </Card>
      </div>
    </template>

    <!-- 시험 응시 상태 -->
    <template v-else>
      <div>
        <h1 class="text-2xl font-bold tracking-tight">{{ examStore.currentExam?.title || '시험' }}</h1>
        <p class="text-muted-foreground">모든 문제에 답변한 후 제출하세요.</p>
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
            <vue-monaco-editor
              :value="answers[problem.id] || ''"
              @change="(val) => answers[problem.id] = val"
              :language="languages[problem.id] || 'java'"
              theme="vs-dark"
              :height="200"
              :options="editorOptions"
            />
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
import { ref, reactive, onMounted } from 'vue'
import { renderMarkdown } from '@/lib/markdown'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { useExamStore } from '@/stores/examStore'
import { submitAnswers } from '@/api'
import { Card, CardHeader, CardTitle, CardContent, CardFooter } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Textarea } from '@/components/ui/textarea'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const examStore = useExamStore()

const answers = reactive({})
const languages = reactive({})
const loading = ref(false)
const submitted = ref(false)

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
})

async function handleSubmit() {
  if (!authStore.examinee) {
    router.push('/exam/login')
    return
  }

  loading.value = true
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
  } catch (e) {
    if (e.response?.status === 409) {
      alert('이미 응시 완료한 시험입니다.')
      router.push('/exam/login')
    } else {
      alert('제출 실패: ' + (e.response?.data?.message || e.message))
    }
  } finally {
    loading.value = false
  }
}
</script>
