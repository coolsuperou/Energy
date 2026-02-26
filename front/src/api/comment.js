import request from './request'

/**
 * 获取评论列表
 * @param {string} relatedType - 关联类型：application（用电申请）或 task（工单）
 * @param {number} relatedId - 关联业务ID
 */
export function getComments(relatedType, relatedId) {
  return request({
    url: '/comments',
    method: 'get',
    params: { relatedType, relatedId }
  })
}

/**
 * 发表评论
 * @param {Object} data - 评论数据
 * @param {string} data.relatedType - 关联类型：application 或 task
 * @param {number} data.relatedId - 关联业务ID
 * @param {string} data.content - 评论内容
 */
export function addComment(data) {
  return request({
    url: '/comments',
    method: 'post',
    data
  })
}
