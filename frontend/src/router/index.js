import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

import ExamManage from '../views/admin/ExamManage.vue'
import ExamCreate from '../views/admin/ExamCreate.vue'
import ScoreBoard from '../views/admin/ScoreBoard.vue'
import ExamLogin from '../views/exam/ExamLogin.vue'
import ExamTake from '../views/exam/ExamTake.vue'
import ExamResult from '../views/exam/ExamResult.vue'

const routes = [
  { path: '/', redirect: '/exam/login' },
  { path: '/admin/exams', component: ExamManage },
  { path: '/admin/exams/create', component: ExamCreate },
  { path: '/admin/scores', component: ScoreBoard },
  { path: '/exam/login', component: ExamLogin },
  { path: '/exam/take/:examId', component: ExamTake, meta: { requiresExaminee: true } },
  { path: '/exam/result', component: ExamResult, meta: { requiresExaminee: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.meta.requiresExaminee) {
    const authStore = useAuthStore()
    if (!authStore.examinee) {
      return '/exam/login'
    }
  }
})

export default router
