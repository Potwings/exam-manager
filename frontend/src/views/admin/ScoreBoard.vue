<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold tracking-tight">채점 결과</h1>
      <p class="text-muted-foreground">시험 채점 결과를 확인합니다.</p>
    </div>

    <Card>
      <CardHeader>
        <div class="flex items-center justify-between">
          <CardTitle>결과</CardTitle>
          <div class="w-64">
            <select
              v-model="selectedExamId"
              @change="loadResults"
              class="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
            >
              <option value="" disabled>시험 선택</option>
              <option v-for="exam in examStore.exams" :key="exam.id" :value="exam.id">
                {{ exam.title }} ({{ exam.problemCount }}문제)
              </option>
            </select>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <Table v-if="selectedExamId">
          <TableHeader>
            <TableRow>
              <TableHead>이름</TableHead>
              <TableHead>생년월일</TableHead>
              <TableHead>점수</TableHead>
              <TableHead>제출일</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-for="r in results" :key="r.examineeId">
              <TableCell class="font-medium">{{ r.examineeName }}</TableCell>
              <TableCell class="text-muted-foreground">{{ r.examineeBirthDate || '-' }}</TableCell>
              <TableCell>
                <Badge :variant="r.totalScore >= r.maxScore * 0.6 ? 'default' : 'destructive'">
                  {{ r.totalScore }} / {{ r.maxScore }}
                </Badge>
              </TableCell>
              <TableCell class="text-muted-foreground">{{ formatDate(r.submittedAt) }}</TableCell>
            </TableRow>
          </TableBody>
        </Table>
        <p v-if="!selectedExamId" class="text-center py-6 text-muted-foreground">
          시험을 선택하세요.
        </p>
        <p v-else-if="results.length === 0" class="text-center py-6 text-muted-foreground">
          제출된 답안이 없습니다.
        </p>
      </CardContent>
    </Card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useExamStore } from '@/stores/examStore'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'

const examStore = useExamStore()
const results = ref([])
const selectedExamId = ref('')

onMounted(async () => {
  await examStore.loadExams()
  if (examStore.exams.length > 0) {
    selectedExamId.value = examStore.exams[0].id
    await loadResults()
  }
})

async function loadResults() {
  if (!selectedExamId.value) {
    results.value = []
    return
  }
  results.value = await examStore.loadScores(selectedExamId.value)
}

function formatDate(dt) {
  if (!dt) return ''
  return new Date(dt).toLocaleString('ko-KR')
}
</script>
