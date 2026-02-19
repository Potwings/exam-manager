<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold tracking-tight">시험 관리</h1>
        <p class="text-muted-foreground">등록된 시험을 관리합니다.</p>
      </div>
      <Button as-child>
        <router-link to="/admin/exams/create">
          <Plus class="h-4 w-4 mr-1" /> 시험 생성
        </router-link>
      </Button>
    </div>

    <Card>
      <CardHeader>
        <CardTitle>시험 목록</CardTitle>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>ID</TableHead>
              <TableHead>제목</TableHead>
              <TableHead>문제 수</TableHead>
              <TableHead>총점</TableHead>
              <TableHead>상태</TableHead>
              <TableHead>생성일</TableHead>
              <TableHead class="text-right">관리</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-for="exam in examStore.exams" :key="exam.id" class="cursor-pointer" @click="router.push(`/admin/exams/${exam.id}`)">
              <TableCell>{{ exam.id }}</TableCell>
              <TableCell class="font-medium">{{ exam.title }}</TableCell>
              <TableCell>
                <Badge variant="secondary">{{ exam.problemCount }}문제</Badge>
              </TableCell>
              <TableCell>{{ exam.totalScore }}점</TableCell>
              <TableCell>
                <Badge :variant="exam.active ? 'default' : 'outline'">
                  {{ exam.active ? '활성' : '비활성' }}
                </Badge>
              </TableCell>
              <TableCell class="text-muted-foreground">{{ formatDate(exam.createdAt) }}</TableCell>
              <TableCell class="text-right space-x-2">
                <Button
                  size="sm"
                  :variant="exam.active ? 'secondary' : 'default'"
                  @click.stop="handleActivate(exam.id)"
                  :disabled="exam.active"
                >
                  {{ exam.active ? '활성 중' : '활성화' }}
                </Button>
                <Button
                  size="sm"
                  variant="destructive"
                  @click.stop="handleDelete(exam.id, exam.title)"
                >
                  <Trash2 class="h-4 w-4" />
                </Button>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
        <p v-if="examStore.exams.length === 0" class="text-center py-6 text-muted-foreground">
          등록된 시험이 없습니다.
        </p>
      </CardContent>
    </Card>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useExamStore } from '@/stores/examStore'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Trash2, Plus } from 'lucide-vue-next'

const router = useRouter()
const examStore = useExamStore()

onMounted(() => examStore.loadExams())

async function handleActivate(id) {
  try {
    await examStore.activateExam(id)
  } catch (e) {
    alert('활성화 실패: ' + (e.response?.data?.message || e.message))
  }
}

async function handleDelete(id, examTitle) {
  if (!confirm(`"${examTitle}" 시험을 삭제하시겠습니까?`)) return
  try {
    await examStore.deleteExam(id)
  } catch (e) {
    alert('삭제 실패: ' + (e.response?.data?.message || e.message))
  }
}

function formatDate(dt) {
  if (!dt) return ''
  return new Date(dt).toLocaleDateString('ko-KR')
}
</script>
