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
          {{ exam?.problems?.length || 0 }}문제 / 총 {{ exam?.totalScore || 0 }}점
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
    <Card v-for="problem in exam?.problems" :key="problem.id">
      <CardHeader>
        <div class="flex items-center justify-between">
          <CardTitle class="text-base">
            <Badge variant="outline" class="mr-2">Q{{ problem.problemNumber }}</Badge>
            <Badge variant="secondary" class="text-xs">{{ problem.score }}점</Badge>
          </CardTitle>
          <Badge v-if="problem.contentType === 'MARKDOWN'" variant="outline" class="text-xs">마크다운</Badge>
        </div>
      </CardHeader>
      <CardContent class="space-y-4">
        <!-- 문제 내용 -->
        <div>
          <p class="text-sm font-medium text-muted-foreground mb-1">문제 내용</p>
          <div v-if="problem.contentType === 'MARKDOWN'" class="prose prose-sm max-w-none dark:prose-invert border rounded-md p-3 bg-muted/30" v-html="renderMd(problem.content)"></div>
          <pre v-else class="whitespace-pre-wrap text-sm leading-relaxed border rounded-md p-3 bg-muted/30">{{ problem.content }}</pre>
        </div>

        <!-- 채점 기준 -->
        <div>
          <p class="text-sm font-medium text-muted-foreground mb-1">채점 기준</p>
          <pre class="whitespace-pre-wrap text-sm leading-relaxed border rounded-md p-3 bg-muted/30">{{ problem.answerContent }}</pre>
        </div>
      </CardContent>
    </Card>

    <p v-if="loadError" class="text-sm text-destructive">{{ loadError }}</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { renderMarkdown } from '@/lib/markdown'
import { fetchExam } from '@/api'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { ArrowLeft, Pencil, Copy } from 'lucide-vue-next'

const router = useRouter()
const route = useRoute()
const examId = route.params.id

const exam = ref(null)
const loadError = ref('')
const showCloneNotice = ref(false)

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

function renderMd(text) {
  return renderMarkdown(text)
}
</script>
