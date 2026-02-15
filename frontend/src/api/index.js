import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// ===== Exam =====

export function fetchExams() {
  return api.get('/exams')
}

export function fetchExam(id) {
  return api.get(`/exams/${id}`)
}

export function fetchProblems(examId) {
  return api.get(`/exams/${examId}/problems`)
}

export function createExam(data) {
  return api.post('/exams', data)
}

export function deleteExam(id) {
  return api.delete(`/exams/${id}`)
}

export function activateExam(id) {
  return api.patch(`/exams/${id}/activate`)
}

export function fetchActiveExam() {
  return api.get('/exams/active')
}

// ===== Examinee =====

export function loginExaminee(name) {
  return api.post('/examinees/login', { name })
}

// ===== Submission =====

export function submitAnswers(examineeId, examId, answers) {
  return api.post('/submissions', { examineeId, examId, answers }, { timeout: 300000 })
}

export function fetchResult(examineeId, examId) {
  return api.get('/submissions/result', { params: { examineeId, examId } })
}

// ===== Score =====

export function fetchScores(examId) {
  return api.get(`/scores/exam/${examId}`)
}

export default api
