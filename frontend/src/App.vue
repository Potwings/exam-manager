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
            <Popover v-if="!authStore.admin.initLogin" @update:open="(open) => { if (open) markAllRead() }">
              <PopoverTrigger as-child>
                <Button variant="ghost" size="icon" class="relative">
                  <Bell class="h-4 w-4" />
                  <Badge
                    v-if="unreadCount > 0"
                    variant="destructive"
                    class="absolute top-0 right-0 h-4 min-w-4 px-1 text-[10px] leading-none flex items-center justify-center"
                  >
                    {{ unreadCount > 9 ? '9+' : unreadCount }}
                  </Badge>
                </Button>
              </PopoverTrigger>
              <PopoverContent align="end" :side-offset="8" class="w-96 p-0">
                <div class="px-4 py-3 border-b flex items-center justify-between">
                  <h4 class="text-sm font-semibold">알림</h4>
                  <span v-if="notifications.length > 0" class="text-xs text-muted-foreground">{{ notifications.length }}개</span>
                </div>
                <ScrollArea class="max-h-96">
                  <div v-if="notifications.length === 0" class="px-4 py-12 text-center">
                    <Bell class="h-8 w-8 text-muted-foreground/30 mx-auto mb-2" />
                    <p class="text-sm text-muted-foreground">알림이 없습니다</p>
                  </div>
                  <div v-else>
                    <div
                      v-for="item in notifications"
                      :key="item.id"
                      class="flex items-start gap-3 px-4 py-3 border-b last:border-b-0 hover:bg-muted/50 transition-colors"
                      :class="item.type === 'admin-call' ? 'border-l-2 border-l-amber-400' : 'border-l-2 border-l-emerald-400'"
                    >
                      <div class="mt-0.5 shrink-0">
                        <CheckCircle v-if="item.type === 'grading-complete'" class="h-4 w-4 text-emerald-500" />
                        <AlertTriangle v-else class="h-4 w-4 text-amber-500" />
                      </div>
                      <div class="flex-1 min-w-0">
                        <div class="flex items-center justify-between gap-2">
                          <p class="text-sm font-medium">{{ item.title }}</p>
                          <span class="text-[11px] text-muted-foreground shrink-0">{{ formatTime(item.timestamp) }}</span>
                        </div>
                        <p class="text-xs text-muted-foreground mt-0.5">{{ item.message }}</p>
                      </div>
                    </div>
                  </div>
                </ScrollArea>
              </PopoverContent>
            </Popover>
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
import { Badge } from '@/components/ui/badge'
import { Separator } from '@/components/ui/separator'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Toaster } from '@/components/ui/sonner'
import { LogOut, Bell, CheckCircle, AlertTriangle } from 'lucide-vue-next'

const router = useRouter()
const authStore = useAuthStore()
const { connect, disconnect, requestPermission, notifications, unreadCount, markAllRead } = useNotifications()

function formatTime(date) {
  const diff = new Date() - date
  if (diff < 60000) return '방금 전'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}분 전`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}시간 전`
  return date.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' })
}

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
