<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold tracking-tight">관리자 관리</h1>
        <p class="text-muted-foreground">관리자 계정을 등록하고 관리합니다.</p>
      </div>
      <Dialog v-model:open="dialogOpen">
        <DialogTrigger as-child>
          <Button>
            <UserPlus class="h-4 w-4 mr-2" />
            관리자 등록
          </Button>
        </DialogTrigger>
        <DialogContent class="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>관리자 등록</DialogTitle>
            <DialogDescription>새 관리자 계정을 생성합니다.</DialogDescription>
          </DialogHeader>
          <div class="space-y-4 py-4">
            <div class="space-y-2">
              <Label for="reg-username">아이디</Label>
              <Input id="reg-username" v-model="form.username" placeholder="아이디 입력" @keyup.enter="handleRegister" />
            </div>
            <div class="space-y-2">
              <Label for="reg-password">비밀번호</Label>
              <Input id="reg-password" v-model="form.password" type="password" placeholder="4자 이상" @keyup.enter="handleRegister" />
            </div>
            <p v-if="formError" class="text-sm text-destructive">{{ formError }}</p>
          </div>
          <DialogFooter>
            <Button variant="outline" @click="dialogOpen = false">취소</Button>
            <Button @click="handleRegister" :disabled="registering || !form.username.trim() || form.password.length < 4">
              {{ registering ? '등록 중...' : '등록' }}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>

    <Card>
      <CardContent class="pt-6">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>아이디</TableHead>
              <TableHead>역할</TableHead>
              <TableHead>최초 로그인</TableHead>
              <TableHead>생성일</TableHead>
              <TableHead class="w-20"></TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-for="a in admins" :key="a.id">
              <TableCell class="font-medium">{{ a.username }}</TableCell>
              <TableCell>
                <Badge variant="secondary">{{ a.role }}</Badge>
              </TableCell>
              <TableCell>
                <Badge v-if="a.initLogin" variant="outline">비밀번호 변경 필요</Badge>
                <span v-else class="text-muted-foreground text-sm">완료</span>
              </TableCell>
              <TableCell class="text-muted-foreground">{{ formatDate(a.createdAt) }}</TableCell>
              <TableCell>
                <Button
                  variant="ghost"
                  size="icon"
                  :disabled="a.id === authStore.admin?.id"
                  @click="handleDelete(a)"
                >
                  <Trash2 class="h-4 w-4" />
                </Button>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
        <p v-if="admins.length === 0" class="text-center py-6 text-muted-foreground">
          등록된 관리자가 없습니다.
        </p>
      </CardContent>
    </Card>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { useAuthStore } from '@/stores/authStore'
import { fetchAdminList, adminRegister, deleteAdmin } from '@/api'
import { Card, CardContent } from '@/components/ui/card'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Dialog, DialogContent, DialogDescription, DialogFooter,
  DialogHeader, DialogTitle, DialogTrigger
} from '@/components/ui/dialog'
import { UserPlus, Trash2 } from 'lucide-vue-next'

const authStore = useAuthStore()
const admins = ref([])
const dialogOpen = ref(false)
const registering = ref(false)
const formError = ref('')
const form = reactive({ username: '', password: '' })

onMounted(loadAdmins)

async function loadAdmins() {
  const { data } = await fetchAdminList()
  admins.value = data
}

async function handleRegister() {
  if (!form.username.trim() || form.password.length < 4) return
  registering.value = true
  formError.value = ''
  try {
    await adminRegister(form.username, form.password)
    dialogOpen.value = false
    form.username = ''
    form.password = ''
    await loadAdmins()
  } catch (e) {
    formError.value = e.response?.data?.message || '등록에 실패했습니다'
  } finally {
    registering.value = false
  }
}

async function handleDelete(admin) {
  if (!confirm(`"${admin.username}" 관리자를 삭제하시겠습니까?`)) return
  try {
    await deleteAdmin(admin.id)
    await loadAdmins()
  } catch (e) {
    alert(e.response?.data?.message || '삭제에 실패했습니다')
  }
}

function formatDate(dt) {
  if (!dt) return ''
  return new Date(dt).toLocaleString('ko-KR')
}
</script>
