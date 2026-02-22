<template>
  <div class="flex items-center justify-center min-h-[60vh]">
    <Card class="w-full max-w-md">
      <CardHeader class="text-center">
        <CardTitle class="text-2xl">관리자 로그인</CardTitle>
        <CardDescription>관리자 계정으로 로그인하세요.</CardDescription>
      </CardHeader>
      <CardContent class="space-y-4">
        <div class="space-y-2">
          <Label for="username">아이디</Label>
          <Input id="username" v-model="username" placeholder="admin" @keyup.enter="handleLogin" />
        </div>
        <div class="space-y-2">
          <Label for="password">비밀번호</Label>
          <Input id="password" v-model="password" type="password" placeholder="비밀번호" @keyup.enter="handleLogin" />
        </div>
        <p v-if="error" class="text-sm text-destructive">{{ error }}</p>
      </CardContent>
      <CardFooter>
        <Button class="w-full" @click="handleLogin" :disabled="loading || !username.trim() || !password">
          {{ loading ? '로그인 중...' : '로그인' }}
        </Button>
      </CardFooter>
    </Card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

const router = useRouter()
const authStore = useAuthStore()

const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

async function handleLogin() {
  if (!username.value.trim() || !password.value) return
  loading.value = true
  error.value = ''
  try {
    const data = await authStore.loginAdmin(username.value, password.value)
    router.push(data.initLogin ? '/admin/change-password' : '/admin/scores')
  } catch (e) {
    error.value = e.response?.data?.message || '로그인에 실패했습니다'
  } finally {
    loading.value = false
  }
}
</script>
