<template>
  <div class="flex items-center justify-center min-h-[60vh]">
    <Card class="w-full max-w-md">
      <CardHeader class="text-center">
        <CardTitle class="text-2xl">시험 응시</CardTitle>
        <CardDescription>정보를 입력하고 시험을 시작하세요.</CardDescription>
      </CardHeader>
      <CardContent class="space-y-4">
        <div class="space-y-2">
          <Label for="name">이름</Label>
          <Input id="name" v-model="name" placeholder="홍길동" />
        </div>
        <div v-if="examStore.activeExam" class="p-3 bg-muted rounded-md text-sm">
          <span class="text-muted-foreground">시험: </span>
          <span class="font-medium">{{ examStore.activeExam.title }}</span>
          <span class="text-muted-foreground ml-1">({{ examStore.activeExam.problems.length }}문제)</span>
        </div>
        <p v-else-if="!loading" class="text-center py-3 text-muted-foreground text-sm">
          현재 진행 중인 시험이 없습니다.
        </p>
      </CardContent>
      <CardFooter>
        <Button class="w-full" @click="handleLogin" :disabled="loginLoading || !examStore.activeExam || !name.trim()">
          {{ loginLoading ? '로그인 중...' : '시험 시작' }}
        </Button>
      </CardFooter>
    </Card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { useExamStore } from '@/stores/examStore'
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

const router = useRouter()
const authStore = useAuthStore()
const examStore = useExamStore()

const name = ref('')
const loading = ref(true)
const loginLoading = ref(false)

onMounted(async () => {
  await examStore.loadActiveExam()
  loading.value = false
})

async function handleLogin() {
  if (!name.value.trim() || !examStore.activeExam) return
  loginLoading.value = true
  try {
    await authStore.login(name.value)
    router.push(`/exam/take/${examStore.activeExam.id}`)
  } catch (e) {
    alert('로그인 실패: ' + (e.response?.data?.message || e.message))
  } finally {
    loginLoading.value = false
  }
}
</script>
