import { getUnreadReminderCount, getTimeoutReminders } from '@/api/oa/ai'
import request from '@/utils/request'

export function getDashboardSummary() {
  return request({
    url: '/oa/dashboard/summary',
    method: 'get'
  })
}

export { getUnreadReminderCount, getTimeoutReminders }
