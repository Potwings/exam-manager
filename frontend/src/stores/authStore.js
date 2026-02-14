import { defineStore } from 'pinia'
import { ref } from 'vue'
import { loginExaminee } from '@/api'

export const useAuthStore = defineStore('auth', () => {
  const examinee = ref(null)

  async function login(name) {
    const { data } = await loginExaminee(name)
    examinee.value = data
    return data
  }

  function clear() {
    examinee.value = null
  }

  return { examinee, login, clear }
})
