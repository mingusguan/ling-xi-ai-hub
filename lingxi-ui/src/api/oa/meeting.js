import request from '@/utils/request'

export function listMeetingRoom(params) {
  return request({ url: '/oa/meeting/room/list', method: 'get', params })
}

export function saveMeetingRoom(data) {
  return request({ url: '/oa/meeting/room', method: 'post', data })
}

export function delMeetingRoom(roomId) {
  return request({ url: `/oa/meeting/room/${roomId}`, method: 'delete' })
}

export function listMeetingReservation(params) {
  return request({ url: '/oa/meeting/reservation/list', method: 'get', params })
}

export function saveMeetingReservation(data) {
  return request({ url: '/oa/meeting/reservation', method: 'post', data })
}

export function cancelMeetingReservation(reservationId) {
  return request({ url: `/oa/meeting/reservation/${reservationId}/cancel`, method: 'put' })
}
