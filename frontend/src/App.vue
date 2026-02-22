<template>
  <div class="min-h-screen bg-background">
    <header class="border-b">
      <div class="max-w-5xl mx-auto flex items-center gap-6 px-6 h-14">
        <router-link to="/" class="font-bold text-lg tracking-tight">
          ExamManager
        </router-link>
        <Separator orientation="vertical" class="h-6" />
        <nav class="flex items-center gap-1">
          <template v-if="authStore.admin && !authStore.admin.initLogin">
            <Button variant="ghost" size="sm" as-child>
              <router-link to="/admin/exams">Manage</router-link>
            </Button>
            <Button variant="ghost" size="sm" as-child>
              <router-link to="/admin/scores">Scores</router-link>
            </Button>
            <Button variant="ghost" size="sm" as-child>
              <router-link to="/admin/members">Members</router-link>
            </Button>
          </template>
          <Button variant="ghost" size="sm" as-child>
            <router-link to="/exam/login">Exam</router-link>
          </Button>
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
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { LogOut } from 'lucide-vue-next'

const router = useRouter()
const authStore = useAuthStore()

onMounted(() => {
  authStore.checkAdmin()
})

async function handleLogout() {
  await authStore.logoutAdmin()
  router.push('/admin/login')
}
</script>
