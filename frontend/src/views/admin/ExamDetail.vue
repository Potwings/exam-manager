<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <div class="flex items-center gap-2 mb-1">
          <Button variant="ghost" size="sm" @click="router.push('/admin/exams')">
            <ArrowLeft class="h-4 w-4 mr-1" /> 목록
          </Button>
        </div>
        <h1 class="text-2xl font-bold tracking-tight">{{ exam?.title }}</h1>
        <p class="text-muted-foreground">
          {{ exam?.problems?.length || 0 }}문제 / 총 {{ totalScore }}점
          <Badge v-if="exam" class="ml-2" :variant="exam.hasSubmissions ? 'secondary' : 'outline'">
            {{ exam.hasSubmissions ? '제출 결과 있음' : '제출 결과 없음' }}
          </Badge>
        </p>
      </div>
      <Button @click="handleEdit">
        <Pencil class="h-4 w-4 mr-1" /> 수정하기
      </Button>
    </div>

    <!-- 제출 결과 있을 때 수정 버튼 클릭 시 노출되는 안내 -->
    <div v-if="showCloneNotice" class="rounded-lg border border-amber-200 bg-amber-50 dark:border-amber-900 dark:bg-amber-950 p-4">
      <p class="text-sm text-amber-800 dark:text-amber-200 mb-3">
        이 시험에 제출 결과가 있어 직접 수정할 수 없습니다. 기존 시험 내용을 복제하여 <strong>새 시험을 생성</strong>할 수 있습니다.
      </p>
      <div class="flex gap-2">
        <Button size="sm" @click="router.push(`/admin/exams/create?from=${examId}`)">
          <Copy class="h-4 w-4 mr-1" /> 복제하여 새 시험 만들기
        </Button>
        <Button size="sm" variant="ghost" @click="showCloneNotice = false">닫기</Button>
      </div>
    </div>

    <!-- 문제 목록 -->
    <template v-for="problem in exam?.problems" :key="problem.id">
      <!-- 그룹 문제 -->
      <Card v-if="problem.children && problem.children.length > 0">
        <CardHeader>
          <div class="flex items-center justify-between">
            <CardTitle class="text-base">
              <Badge variant="outline" class="mr-2">Q{{ problem.problemNumber }}</Badge>
              <Badge variant="secondary" class="text-xs">그룹</Badge>
            </CardTitle>
            <div class="flex items-center gap-1">
              <Badge v-if="problem.contentType === 'MARKDOWN'" variant="outline" class="text-xs">마크다운</Badge>
              <Button variant="ghost" size="sm" class="h-7 w-7 p-0" :aria-label="`Q${problem.problemNumber} 공통 지문 수정`" @click="openEditDialog(problem, true)">
                <SquarePen class="h-3.5 w-3.5 text-muted-foreground" />
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent class="space-y-4">
          <!-- 부모 지문 (그룹 문제) -->
          <div>
            <p class="text-sm font-medium text-muted-foreground mb-1">공통 지문</p>
            <div v-if="problem.contentType === 'MARKDOWN'" class="prose prose-sm max-w-none dark:prose-invert border rounded-md p-3 bg-muted/30" v-html="renderMd(problem.content)"></div>
            <pre v-else class="whitespace-pre-wrap text-sm leading-relaxed border rounded-md p-3 bg-muted/30">{{ problem.content }}</pre>
          </div>

          <!-- 하위 문제들 -->
          <div class="ml-4 border-l-2 border-blue-200 dark:border-blue-800 pl-4 space-y-4">
            <div v-for="child in problem.children" :key="child.id" class="border rounded-md p-3 space-y-3 bg-muted/10">
              <div class="flex items-center justify-between">
                <span class="text-sm font-medium">Q{{ problem.problemNumber }}-{{ child.problemNumber }}</span>
                <div class="flex items-center gap-1">
                  <Badge variant="secondary" class="text-xs">{{ child.score }}점</Badge>
                  <Badge v-if="child.codeEditor" variant="outline" class="text-xs text-emerald-700 border-emerald-300 dark:text-emerald-300 dark:border-emerald-700">코드 에디터</Badge>
                  <Button variant="ghost" size="sm" class="h-7 w-7 p-0" :aria-label="`Q${problem.problemNumber}-${child.problemNumber} 문제 수정`" @click="openEditDialog(child, false, problem.problemNumber)">
                    <SquarePen class="h-3.5 w-3.5 text-muted-foreground" />
                  </Button>
                </div>
              </div>
              <div>
                <p class="text-sm font-medium text-muted-foreground mb-1">문제 내용</p>
                <div v-if="child.contentType === 'MARKDOWN'" class="prose prose-sm max-w-none dark:prose-invert border rounded-md p-3 bg-muted/30" v-html="renderMd(child.content)"></div>
                <pre v-else class="whitespace-pre-wrap text-sm leading-relaxed border rounded-md p-3 bg-muted/30">{{ child.content }}</pre>
              </div>
              <div>
                <p class="text-sm font-medium text-muted-foreground mb-1">채점 기준</p>
                <pre class="whitespace-pre-wrap text-sm leading-relaxed border rounded-md p-3 bg-muted/30">{{ child.answerContent }}</pre>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      <!-- 독립 문제 -->
      <Card v-else>
        <CardHeader>
          <div class="flex items-center justify-between">
            <CardTitle class="text-base">
              <Badge variant="outline" class="mr-2">Q{{ problem.problemNumber }}</Badge>
              <Badge variant="secondary" class="text-xs">{{ problem.score }}점</Badge>
              <Badge v-if="problem.codeEditor" variant="outline" class="text-xs ml-1 text-emerald-700 border-emerald-300 dark:text-emerald-300 dark:border-emerald-700">코드 에디터</Badge>
            </CardTitle>
            <div class="flex items-center gap-1">
              <Badge v-if="problem.contentType === 'MARKDOWN'" variant="outline" class="text-xs">마크다운</Badge>
              <Button variant="ghost" size="sm" class="h-7 w-7 p-0" :aria-label="`Q${problem.problemNumber} 문제 수정`" @click="openEditDialog(problem)">
                <SquarePen class="h-3.5 w-3.5 text-muted-foreground" />
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent class="space-y-4">
          <div>
            <p class="text-sm font-medium text-muted-foreground mb-1">문제 내용</p>
            <div v-if="problem.contentType === 'MARKDOWN'" class="prose prose-sm max-w-none dark:prose-invert border rounded-md p-3 bg-muted/30" v-html="renderMd(problem.content)"></div>
            <pre v-else class="whitespace-pre-wrap text-sm leading-relaxed border rounded-md p-3 bg-muted/30">{{ problem.content }}</pre>
          </div>
          <div>
            <p class="text-sm font-medium text-muted-foreground mb-1">채점 기준</p>
            <pre class="whitespace-pre-wrap text-sm leading-relaxed border rounded-md p-3 bg-muted/30">{{ problem.answerContent }}</pre>
          </div>
        </CardContent>
      </Card>
    </template>

    <p v-if="loadError" class="text-sm text-destructive">{{ loadError }}</p>

    <!-- 문제 수정 Dialog -->
    <ProblemEditDialog
      v-model:open="editDialogOpen"
      :problem="editTarget?.problem"
      :exam-id="examId"
      :is-group-parent="editTarget?.isGroupParent ?? false"
      :parent-problem-number="editTarget?.parentProblemNumber"
      @saved="handleProblemSaved"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { renderMarkdown } from '@/lib/markdown'
import { fetchExam } from '@/api'
import ProblemEditDialog from '@/components/ProblemEditDialog.vue'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { ArrowLeft, Pencil, Copy, SquarePen } from 'lucide-vue-next'

const router = useRouter()
const route = useRoute()
const examId = route.params.id

const exam = ref(null)
const loadError = ref('')
const showCloneNotice = ref(false)

// 문제 수정 Dialog 상태
const editDialogOpen = ref(false)
const editTarget = ref(null)

// totalScore를 problems 기반으로 동적 계산 — 문제 수정 후 배점 변경 즉시 반영
const totalScore = computed(() => {
  if (!exam.value?.problems) return 0
  let sum = 0
  for (const p of exam.value.problems) {
    if (p.children && p.children.length > 0) {
      for (const c of p.children) {
        sum += c.score || 0
      }
    } else {
      sum += p.score || 0
    }
  }
  return sum
})

onMounted(async () => {
  try {
    const { data } = await fetchExam(examId)
    exam.value = data
  } catch (e) {
    loadError.value = '시험 데이터 로드 실패: ' + (e.response?.data?.message || e.message)
  }
})

function handleEdit() {
  if (exam.value?.hasSubmissions) {
    showCloneNotice.value = true
  } else {
    router.push(`/admin/exams/${examId}/edit`)
  }
}

function openEditDialog(problem, isGroupParent = false, parentProblemNumber = null) {
  editTarget.value = { problem, isGroupParent, parentProblemNumber }
  editDialogOpen.value = true
}

function handleProblemSaved(updated) {
  if (!exam.value?.problems) return

  // 최상위 문제에서 검색
  const topIdx = exam.value.problems.findIndex(p => p.id === updated.id)
  if (topIdx !== -1) {
    Object.assign(exam.value.problems[topIdx], updated)
    return
  }

  // 그룹 자식 문제에서 검색
  for (const p of exam.value.problems) {
    if (p.children) {
      const childIdx = p.children.findIndex(c => c.id === updated.id)
      if (childIdx !== -1) {
        Object.assign(p.children[childIdx], updated)
        return
      }
    }
  }
}

function renderMd(text) {
  return renderMarkdown(text)
}
</script>
