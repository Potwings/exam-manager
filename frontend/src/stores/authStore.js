import { defineStore } from 'pinia'
import { ref } from 'vue'
import { loginExaminee, adminLogin, adminLogout, adminMe } from '@/api'

export const useAuthStore = defineStore('auth', () => {
  // localStorage에서 수험자 인증 정보 복원 (새로고침 대응)
  const saved = localStorage.getItem('examinee')
  let parsedExaminee = null
  if (saved) {
    try {
      parsedExaminee = JSON.parse(saved)
    } catch {
      localStorage.removeItem('examinee')
    }
  }
  const examinee = ref(parsedExaminee)
  const admin = ref(null)
  const adminLoading = ref(true)

  async function login(name, birthDate) {
    const { data } = await loginExaminee(name, birthDate)
    examinee.value = data
    try {
      localStorage.setItem('examinee', JSON.stringify(data))
    } catch { /* private browsing / quota exceeded — 인메모리 세션 유지 */ }
    return data
  }

  function clear() {
    examinee.value = null
    localStorage.removeItem('examinee')
  }

  async function loginAdmin(username, password) {
    const { data } = await adminLogin(username, password)
    admin.value = data
    return data
  }

  async function logoutAdmin() {
    await adminLogout()
    admin.value = null
  }

  async function checkAdmin() {
    try {
      const { data } = await adminMe()
      admin.value = data
    } catch {
      admin.value = null
    } finally {
      adminLoading.value = false
    }
  }

  return { examinee, admin, adminLoading, login, clear, loginAdmin, logoutAdmin, checkAdmin }
})
