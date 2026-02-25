import { ref } from 'vue'
import { toast } from 'vue-sonner'

let eventSource = null
let reconnectTimer = null
let shouldReconnect = false
const connected = ref(false)

const MAX_NOTIFICATIONS = 20
const notifications = ref([])
const unreadCount = ref(0)

function handleGradingComplete(event) {
  try {
    const data = JSON.parse(event.data)
    const { examineeName, totalScore, maxScore } = data
    if (!examineeName || totalScore == null || maxScore == null) return
    const message = `${examineeName}: ${totalScore}점 / ${maxScore}점`

    if (document.visibilityState === 'visible') {
      toast.success('채점 완료', { description: message })
    } else {
      showBrowserNotification('채점 완료', message)
    }

    addNotification({ type: 'grading-complete', title: '채점 완료', message, data })
  } catch {
    console.warn('채점 완료 알림 파싱 실패:', event.data)
  }
}

function handleAdminCall(event) {
  try {
    const data = JSON.parse(event.data)
    const { examineeName } = data
    if (!examineeName) return
    const message = `${examineeName}님이 도움을 요청했습니다`

    if (document.visibilityState === 'visible') {
      toast.warning('관리자 호출', { description: message })
    } else {
      showBrowserNotification('관리자 호출', message)
    }

    addNotification({ type: 'admin-call', title: '관리자 호출', message, data })
  } catch {
    console.warn('관리자 호출 알림 파싱 실패:', event.data)
  }
}

function showBrowserNotification(title, body) {
  if (!('Notification' in window) || Notification.permission !== 'granted') {
    return
  }
  new Notification(title, { body, icon: '/favicon.ico' })
}

export function requestPermission() {
  if ('Notification' in window && Notification.permission === 'default') {
    Notification.requestPermission()
  }
}

export function connect() {
  if (eventSource) {
    return
  }

  shouldReconnect = true
  eventSource = new EventSource('/api/notifications/stream', { withCredentials: true })

  eventSource.addEventListener('grading-complete', handleGradingComplete)
  eventSource.addEventListener('admin-call', handleAdminCall)

  eventSource.onopen = () => {
    connected.value = true
  }

  eventSource.onerror = () => {
    connected.value = false
    cleanup()
    // 의도적 disconnect가 아닌 경우에만 3초 후 자동 재연결
    if (shouldReconnect) {
      reconnectTimer = setTimeout(() => {
        connect()
      }, 3000)
    }
  }
}

export function disconnect() {
  shouldReconnect = false
  clearTimeout(reconnectTimer)
  reconnectTimer = null
  cleanup()
  clearNotifications()
}

function cleanup() {
  if (eventSource) {
    eventSource.removeEventListener('grading-complete', handleGradingComplete)
    eventSource.removeEventListener('admin-call', handleAdminCall)
    eventSource.close()
    eventSource = null
    connected.value = false
  }
}

function addNotification({ type, title, message, data }) {
  notifications.value.unshift({
    id: Date.now().toString() + '-' + Math.random().toString(36).slice(2, 7),
    type,
    title,
    message,
    timestamp: new Date(),
    data
  })
  if (notifications.value.length > MAX_NOTIFICATIONS) {
    notifications.value = notifications.value.slice(0, MAX_NOTIFICATIONS)
  }
  unreadCount.value++
}

function markAllRead() {
  unreadCount.value = 0
}

function clearNotifications() {
  notifications.value = []
  unreadCount.value = 0
}

export function useNotifications() {
  return { connected, connect, disconnect, requestPermission, notifications, unreadCount, markAllRead }
}
