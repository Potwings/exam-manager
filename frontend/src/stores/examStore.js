import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  fetchExams, fetchExam, fetchProblems, fetchScores,
  createExam as apiCreateExam, deleteExam as apiDeleteExam,
  activateExam as apiActivateExam, fetchActiveExam
} from '@/api'

export const useExamStore = defineStore('exam', () => {
  const exams = ref([])
  const currentExam = ref(null)
  const problems = ref([])
  const activeExam = ref(null)

  async function loadExams() {
    const { data } = await fetchExams()
    exams.value = data
  }

  async function loadExam(id) {
    const { data } = await fetchExam(id)
    currentExam.value = data
  }

  async function loadProblems(examId) {
    const { data } = await fetchProblems(examId)
    problems.value = data
  }

  async function loadScores(examId) {
    const { data } = await fetchScores(examId)
    return data
  }

  async function createExam(formData) {
    await apiCreateExam(formData)
    await loadExams()
  }

  async function deleteExam(id) {
    await apiDeleteExam(id)
    await loadExams()
  }

  async function activateExam(id) {
    await apiActivateExam(id)
    await loadExams()
  }

  async function loadActiveExam() {
    try {
      const { data } = await fetchActiveExam()
      activeExam.value = data || null
    } catch {
      activeExam.value = null
    }
  }

  function clear() {
    currentExam.value = null
    problems.value = []
  }

  return {
    exams, currentExam, problems, activeExam,
    loadExams, loadExam, loadProblems, loadScores,
    createExam, deleteExam, activateExam, loadActiveExam,
    clear
  }
})
