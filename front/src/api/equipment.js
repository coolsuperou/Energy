import request from './request'

/**
 * 获取设备列表
 * @param {number|string|undefined} workshopId 可选；不传则按后端规则（如车间用户仅本车间、巡检员等返回全部或筛选结果）
 */
export function getEquipments(workshopId) {
  const params = {}
  if (workshopId != null && workshopId !== '') {
    params.workshopId = workshopId
  }
  return request.get('/equipments', { params })
}

/**
 * 获取设备详情
 */
export function getEquipmentById(id) {
  return request.get(`/equipments/${id}`)
}

/**
 * 新增设备
 */
export function createEquipment(data) {
  return request.post('/equipments/create', data)
}

/**
 * 更新设备
 */
export function updateEquipment(id, data) {
  return request.put(`/equipments/${id}`, data)
}

/**
 * 删除设备
 */
export function deleteEquipment(id) {
  return request.delete(`/equipments/${id}`)
}
