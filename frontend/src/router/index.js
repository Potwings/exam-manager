import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

import AdminLogin from '../views/admin/AdminLogin.vue'
import ExamManage from '../views/admin/ExamManage.vue'
import ExamCreate from '../views/admin/ExamCreate.vue'
import ExamDetail from '../views/admin/ExamDetail.vue'
import ScoreBoard from '../views/admin/ScoreBoard.vue'
import ExamLogin from '../views/exam/ExamLogin.vue'
import ExamTake from '../views/exam/ExamTake.vue'

const routes = [
  { path: '/', redirect: '/exam/login' },
  { path: '/admin/login', component: AdminLogin },
  { path: '/admin/exams', component: ExamManage, meta: { requiresAdmin: true } },
  { path: '/admin/exams/create', component: ExamCreate, meta: { requiresAdmin: true } },
  { path: '/admin/exams/:id/edit', component: ExamCreate, meta: { requiresAdmin: true } },
  { path: '/admin/exams/:id', component: ExamDetail, meta: { requiresAdmin: true } },
  { path: '/admin/scores', component: ScoreBoard, meta: { requiresAdmin: true } },
  { path: '/exam/login', component: ExamLogin },
  { path: '/exam/take/:examId', component: ExamTake, meta: { requiresExaminee: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAdmin) {
    // 앱 시작 직후 세션 확인이 아직 미완료이면 checkAdmin()을 대기
    if (authStore.adminLoading) {
      await authStore.checkAdmin()
    }
    if (!authStore.admin) {
      return '/admin/login'
    }
  }

  if (to.meta.requiresExaminee) {
    if (!authStore.examinee) {
      return '/exam/login'
    }
  }
})

export default router
