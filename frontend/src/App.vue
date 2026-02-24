<template>
  <div class="min-h-screen bg-background">
    <header class="border-b">
      <div class="max-w-5xl mx-auto flex items-center gap-6 px-6 h-14">
        <router-link to="/exam/login" class="font-bold text-lg tracking-tight">
          ExamManager
        </router-link>
        <Separator orientation="vertical" class="h-6" />
        <nav class="flex items-center gap-1">
          <template v-if="authStore.admin && !authStore.admin.initLogin">
            <Button variant="ghost" size="sm" as-child>
              <router-link to="/admin/scores">채점결과</router-link>
            </Button>
            <Button variant="ghost" size="sm" as-child>
              <router-link to="/admin/exams">시험관리</router-link>
            </Button>
            <Button variant="ghost" size="sm" as-child>
              <router-link to="/admin/members">계정관리</router-link>
            </Button>
          </template>
        </nav>
        <div class="ml-auto flex items-center gap-2">
          <template v-if="authStore.admin">
            <span class="text-sm text-muted-foreground">{{ authStore.admin.username }}</span>
            <Button variant="ghost" size="icon" @click="handleLogout">
              <LogOut class="h-4 w-4" />
            </Button>
          </template>
          <router-link v-else to="/admin/login" class="text-xs text-muted-foreground/60 hover:text-muted-foreground transition-colors">
            관리자 로그인
          </router-link>
        </div>
      </div>
    </header>
    <main class="max-w-5xl mx-auto px-6 py-8">
      <router-view />
    </main>
    <Toaster />
  </div>
</template>

<script setup>
import { onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { useNotifications } from '@/composables/useNotifications'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { Toaster } from '@/components/ui/sonner'
import { LogOut } from 'lucide-vue-next'

const router = useRouter()
const authStore = useAuthStore()
const { connect, disconnect, requestPermission } = useNotifications()

onMounted(async () => {
  await authStore.checkAdmin()
  if (authStore.admin) {
    connect()
    requestPermission()
  }
})

// 로그인/로그아웃 시 SSE 연결 자동 관리
watch(() => authStore.admin, (admin) => {
  if (admin) {
    connect()
    requestPermission()
  } else {
    disconnect()
  }
})

async function handleLogout() {
  try {
    disconnect()
    await authStore.logoutAdmin()
  } finally {
    router.push('/admin/login')
  }
}
</script>
