<template>
  <div class="flex items-center justify-center min-h-[60vh]">
    <Card class="w-full max-w-md">
      <CardHeader class="text-center">
        <CardTitle class="text-2xl">비밀번호 변경</CardTitle>
        <CardDescription>보안을 위해 비밀번호를 변경해주세요.</CardDescription>
      </CardHeader>
      <CardContent class="space-y-4">
        <div class="space-y-2">
          <Label for="current-password">현재 비밀번호</Label>
          <Input id="current-password" v-model="currentPassword" type="password" placeholder="현재 비밀번호" @keyup.enter="handleChange" />
        </div>
        <div class="space-y-2">
          <Label for="new-password">새 비밀번호</Label>
          <Input id="new-password" v-model="newPassword" type="password" placeholder="4자 이상" @keyup.enter="handleChange" />
        </div>
        <div class="space-y-2">
          <Label for="confirm-password">새 비밀번호 확인</Label>
          <Input id="confirm-password" v-model="confirmPassword" type="password" placeholder="새 비밀번호 재입력" @keyup.enter="handleChange" />
        </div>
        <p v-if="error" class="text-sm text-destructive">{{ error }}</p>
      </CardContent>
      <CardFooter>
        <Button class="w-full" @click="handleChange" :disabled="loading || !isValid">
          {{ loading ? '변경 중...' : '비밀번호 변경' }}
        </Button>
      </CardFooter>
    </Card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { changePassword } from '@/api'
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

const router = useRouter()
const authStore = useAuthStore()

const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const error = ref('')

const isValid = computed(() =>
  currentPassword.value &&
  newPassword.value.length >= 4 &&
  newPassword.value === confirmPassword.value
)

async function handleChange() {
  if (!isValid.value) return

  if (newPassword.value !== confirmPassword.value) {
    error.value = '새 비밀번호가 일치하지 않습니다'
    return
  }

  loading.value = true
  error.value = ''
  try {
    await changePassword(currentPassword.value, newPassword.value)
    authStore.admin = null
    window.alert('비밀번호가 변경되었습니다. 새 비밀번호로 다시 로그인해주세요.')
    router.push('/admin/login')
  } catch (e) {
    error.value = e.response?.data?.message || '비밀번호 변경에 실패했습니다'
  } finally {
    loading.value = false
  }
}
</script>
