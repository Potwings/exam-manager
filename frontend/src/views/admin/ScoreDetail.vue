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
            <Badge :variant="s.earnedScore >= s.maxScore ? 'default' : s.earnedScore > 0 ? 'secondary' : 'destructive'">
              {{ s.earnedScore ?? 0 }} / {{ s.maxScore }}점
            </Badge>
          </div>
        </CardHeader>
        <CardContent class="space-y-3">
          <div>
            <p class="text-sm font-medium text-muted-foreground mb-1">제출 답안</p>
            <pre class="text-sm bg-muted rounded-md p-3 whitespace-pre-wrap break-words">{{ s.submittedAnswer || '(미작성)' }}</pre>
          </div>
          <Separator />
          <div>
            <p class="text-sm font-medium text-muted-foreground mb-1">AI 채점 사유</p>
            <p class="text-sm leading-relaxed">{{ s.feedback || '(피드백 없음)' }}</p>
          </div>
        </CardContent>
      </Card>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { fetchResult } from '@/api'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Separator } from '@/components/ui/separator'
import { ArrowLeft, Loader2 } from 'lucide-vue-next'

const router = useRouter()
const route = useRoute()
const examId = route.params.examId
const examineeId = route.params.examineeId

const result = ref(null)
const loading = ref(true)
const loadError = ref('')
const examineeName = ref('')

onMounted(async () => {
  try {
    const { data } = await fetchResult(examineeId, examId)
    result.value = data
    // submissions 배열에서 수험자 이름을 가져올 수 없으므로, query param으로 전달받음
    examineeName.value = route.query.name || '수험자'
  } catch (e) {
    loadError.value = '채점 결과를 불러올 수 없습니다.'
  } finally {
    loading.value = false
  }
})
</script>
