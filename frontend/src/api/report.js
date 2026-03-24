import request from './request'

export function getReportList() {
  return request({ url: '/report/list', method: 'get' })
}

export function getReportById(id) {
  return request({ url: `/report/${id}`, method: 'get' })
}

export function addReport(data) {
  return request({ url: '/report', method: 'post', data })
}

export function updateReport(id, data) {
  return request({ url: `/report/${id}`, method: 'put', data })
}

export function deleteReport(id) {
  return request({ url: `/report/${id}`, method: 'delete' })
}

export function previewReportData(sql) {
  return request({ url: '/report/preview', method: 'post', data: { sql } })
}
