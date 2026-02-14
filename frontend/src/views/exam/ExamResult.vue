<template>
  <div class="space-y-6">
    <div class="flex items-center justify-center min-h-[30vh]">
      <Card class="w-full max-w-lg text-center">
        <CardHeader>
          <CardTitle class="text-2xl">시험 결과</CardTitle>
          <CardDescription>{{ result?.examTitle }}</CardDescription>
        </CardHeader>
        <CardContent class="space-y-4">
          <div class="text-5xl font-bold">
            {{ result?.totalScore ?? 0 }}
            <span class="text-xl text-muted-foreground">/ {{ result?.maxScore ?? 0 }}</span>
          </div>
          <p class="text-muted-foreground">{{ result?.examineeName }}</p>
        </CardContent>
        <CardFooter class="justify-center">
          <Button variant="outline" @click="$router.push('/exam/login')">돌아가기</Button>
        </CardFooter>
      </Card>
    </div>

    <Card v-if="result?.submissions?.length">
      <CardHeader>
        <CardTitle>문제별 결과</CardTitle>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead class="w-[60px]">번호</TableHead>
              <TableHead>제출 답안</TableHead>
              <TableHead class="w-[80px]">정답 여부</TableHead>
              <TableHead class="w-[80px]">점수</TableHead>
              <TableHead>피드백</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-for="s in result.submissions" :key="s.id">
              <TableCell>Q{{ s.problemNumber }}</TableCell>
              <TableCell class="max-w-xs">
                <div class="whitespace-pre-wrap break-words">{{ s.submittedAnswer || '-' }}</div>
              </TableCell>
              <TableCell>
                <Badge :variant="getBadgeVariant(s)">
                  {{ getBadgeLabel(s) }}
                </Badge>
              </TableCell>
              <TableCell>{{ s.earnedScore }} / {{ s.maxScore }}</TableCell>
              <TableCell class="max-w-sm">
                <div class="text-sm text-muted-foreground whitespace-pre-wrap break-words">{{ s.feedback || '-' }}</div>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { fetchResult } from '@/api'
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'

const route = useRoute()
const result = ref(null)

function getBadgeVariant(s) {
  if (s.earnedScore === s.maxScore) return 'default'
  if (s.earnedScore > 0) return 'secondary'
  return 'destructive'
}

function getBadgeLabel(s) {
  if (s.earnedScore === s.maxScore) return '정답'
  if (s.earnedScore > 0) return '부분 정답'
  return '오답'
}

onMounted(async () => {
  const { examineeId, examId } = route.query
  if (examineeId && examId) {
    const { data } = await fetchResult(examineeId, examId)
    result.value = data
  }
})
</script>
