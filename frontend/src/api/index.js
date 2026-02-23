import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  withCredentials: true
})

// 401 응답 인터셉터: admin 페이지에서 세션 만료 시 로그인 페이지로 리다이렉트
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (
      error.response?.status === 401 &&
      window.location.pathname.startsWith('/admin') &&
      !error.config.url.includes('/admin/me') &&
      !error.config.url.includes('/admin/login')
    ) {
      window.location.href = '/admin/login'
      // 리다이렉트 중 caller의 catch 핸들러가 에러 UI를 표시하지 않도록 resolve되지 않는 promise 반환
      return new Promise(() => {})
    }
    return Promise.reject(error)
  }
)

// ===== Admin =====

export function adminLogin(username, password) {
  return api.post('/admin/login', { username, password })
}

export function adminLogout() {
  return api.post('/admin/logout')
}

export function adminMe() {
  return api.get('/admin/me')
}

export function adminRegister(username, password) {
  return api.post('/admin/register', { username, password })
}

export function fetchAdminList() {
  return api.get('/admin/list')
}

export function deleteAdmin(id) {
  return api.delete(`/admin/${id}`)
}

export function changePassword(currentPassword, newPassword) {
  return api.patch('/admin/change-password', { currentPassword, newPassword })
}

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

export function updateExam(id, data) {
  return api.put(`/exams/${id}`, data)
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

export function loginExaminee(name, birthDate) {
  return api.post('/examinees/login', { name, birthDate })
}

// ===== Submission =====

export function submitAnswers(examineeId, examId, answers) {
  return api.post('/submissions', { examineeId, examId, answers }, { timeout: 30000 })
}

export function fetchResult(examineeId, examId) {
  return api.get('/submissions/result', { params: { examineeId, examId } })
}

export function updateSubmission(id, data) {
  return api.patch(`/submissions/${id}`, data)
}

// ===== Score =====

export function fetchScores(examId) {
  return api.get(`/scores/exam/${examId}`)
}

// ===== AI Assist =====

export function checkAiStatus() {
  return api.get('/ai-assist/status')
}

export function generateAiAssist(data) {
  return api.post('/ai-assist/generate', data, { timeout: 180000 })
}

export default api
