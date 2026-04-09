import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getPendingApplications } from '@/api/application'
import { getUnreadCount } from '@/api/notification'
import { getAllFeedbacks } from '@/api/feedback'
import { getTasks } from '@/api/task'

/**
 * 能源调度员侧栏气泡：待审批申请、未读消息、待处理反馈、待处理工单
 */
export const useDispatcherNavStore = defineStore('dispatcherNav', () => {
  const pendingApplicationCount = ref(0)
  const unreadCount = ref(0)
  const pendingFeedbackCount = ref(0)
  const pendingTaskCount = ref(0)

  async function refreshAll() {
    try {
      const res = await getPendingApplications({ page: 1, size: 1 })
      if (res && res.code === 200 && res.data) {
        pendingApplicationCount.value =
          res.data.total ?? (res.data.records || res.data || []).length
      }
    } catch (e) {
      console.warn('加载待审批数量失败', e)
    }

    try {
      const res = await getUnreadCount()
      if (res && res.code === 200) {
        unreadCount.value = res.data || 0
      }
    } catch (e) {
      console.warn('加载未读消息数量失败', e)
      unreadCount.value = 0
    }

    try {
      const res = await getAllFeedbacks({ page: 1, size: 1, status: 'pending' })
      if (res && res.code === 200 && res.data) {
        pendingFeedbackCount.value = res.data.total || 0
      }
    } catch (e) {
      console.warn('加载待处理反馈数量失败', e)
    }

    try {
      const res = await getTasks({ page: 1, size: 1, status: 'PENDING' })
      if (res && res.code === 200 && res.data) {
        pendingTaskCount.value = res.data.total || 0
      }
    } catch (e) {
      console.warn('加载待处理工单数量失败', e)
    }
  }

  return {
    pendingApplicationCount,
    unreadCount,
    pendingFeedbackCount,
    pendingTaskCount,
    refreshAll
  }
})
