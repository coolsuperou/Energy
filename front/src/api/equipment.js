import request from './request'

/**
 * 获取设备列表
 */
export function getEquipments(workshopId) {
  return request.get('/equipments', { params: { workshopId } })
}

/**
 * 获取设备详情
 */
export function getEquipmentById(id) {
  return request.get(`/equipments/${id}`)
}
