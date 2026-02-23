<template>
  <div class="flex items-center justify-center min-h-[60vh]">
    <Card class="w-full max-w-md">
      <CardHeader class="text-center">
        <CardTitle class="text-2xl">시험 응시</CardTitle>
        <CardDescription>정보를 입력하고 시험을 시작하세요.</CardDescription>
      </CardHeader>
      <CardContent class="space-y-4">
        <form autocomplete="off" class="space-y-4" @submit.prevent="handleLogin">
          <div class="space-y-2">
            <Label for="name">이름</Label>
            <Input
              id="name"
              v-model="name"
              placeholder="홍길동"
              @blur="nameTouched = true"
              :class="{ 'border-destructive': nameTouched && nameError }"
            />
            <p v-if="nameTouched && nameError" class="text-sm text-destructive">{{ nameError }}</p>
          </div>
          <div class="space-y-2">
            <Label for="birthDate">생년월일</Label>
            <Input
              id="birthDate"
              v-model="birthDate"
              placeholder="ex) 20010101"
              maxlength="8"
              @blur="birthDateTouched = true"
              :class="{ 'border-destructive': birthDateTouched && birthDateError }"
            />
            <p v-if="birthDateTouched && birthDateError" class="text-sm text-destructive">{{ birthDateError }}</p>
          </div>
          <button type="submit" class="sr-only" tabindex="-1" />
        </form>
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
        <Button
          class="w-full"
          :class="{ 'opacity-50': !examStore.activeExam || !canSubmit }"
          :aria-disabled="loginLoading || !examStore.activeExam || !canSubmit"
          @click="handleLogin"
        >
          {{ loginLoading ? '로그인 중...' : '시험 시작' }}
        </Button>
      </CardFooter>
    </Card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
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
const birthDate = ref('')
const loading = ref(true)
const loginLoading = ref(false)
const nameTouched = ref(false)
const birthDateTouched = ref(false)

// 8자리 숫자가 실제 유효한 날짜(윤년/월별 일수 포함)인지 검증
function isValidDate(dateStr) {
  if (!/^\d{8}$/.test(dateStr)) return false
  const y = parseInt(dateStr.slice(0, 4), 10)
  const m = parseInt(dateStr.slice(4, 6), 10)
  const d = parseInt(dateStr.slice(6, 8), 10)
  const date = new Date(y, m - 1, d)
  return date.getFullYear() === y && date.getMonth() === m - 1 && date.getDate() === d
}

const nameError = computed(() => {
  if (!name.value.trim()) return '이름을 입력해주세요'
  return ''
})

const birthDateError = computed(() => {
  if (!birthDate.value || birthDate.value.length < 8) return '생년월일 8자리를 입력해주세요 (예: 20010101)'
  if (!isValidDate(birthDate.value)) return '올바른 날짜 형식이 아닙니다'
  return ''
})

const canSubmit = computed(() => !nameError.value && !birthDateError.value)

onMounted(async () => {
  await examStore.loadActiveExam()
  loading.value = false
})

async function handleLogin() {
  // 제출 시도 시 모든 필드를 touched 처리하여 에러 메시지 노출
  nameTouched.value = true
  birthDateTouched.value = true
  if (!canSubmit.value || !examStore.activeExam) return

  loginLoading.value = true
  try {
    const raw = birthDate.value
    const formatted = `${raw.slice(0, 4)}-${raw.slice(4, 6)}-${raw.slice(6, 8)}`
    await authStore.login(name.value, formatted)
    router.push(`/exam/take/${examStore.activeExam.id}`)
  } catch (e) {
    alert('로그인 실패: ' + (e.response?.data?.message || e.message))
  } finally {
    loginLoading.value = false
  }
}
</script>
