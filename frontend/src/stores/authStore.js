import { defineStore } from 'pinia'
import { ref } from 'vue'
import { loginExaminee, adminLogin, adminLogout, adminMe } from '@/api'

export const useAuthStore = defineStore('auth', () => {
  const examinee = ref(null)
  const admin = ref(null)
  const adminLoading = ref(true)

  async function login(name, birthDate) {
    const { data } = await loginExaminee(name, birthDate)
    examinee.value = data
    return data
  }

  function clear() {
    examinee.value = null
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
