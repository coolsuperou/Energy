/**
 * AI 流式对话（SSE）
 * @param {string} message 用户消息
 * @param {function} onMessage 收到内容片段的回调
 * @param {function} onDone 完成回调
 * @param {function} onError 错误回调
 * @returns {EventSource} 可用于关闭连接
 */
export function aiChatStream(message, onMessage, onDone, onError) {
  const url = `/api/ai/chat/stream?message=${encodeURIComponent(message)}`
  const es = new EventSource(url, { withCredentials: true })

  es.addEventListener('message', (e) => {
    onMessage && onMessage(e.data)
  })

  es.addEventListener('done', () => {
    onDone && onDone()
    es.close()
  })

  es.addEventListener('error', (e) => {
    if (e.data) {
      onError && onError(e.data)
    } else {
      onError && onError('连接中断')
    }
    es.close()
  })

  es.onerror = () => {
    es.close()
  }

  return es
}

/**
 * AI 流式能耗分析（SSE）
 * @param {string} type 分析类型：overview(能耗总览)、peak-valley(峰谷分析)、anomaly(异常检测)、saving(节能建议)、forecast(趋势预测)
 * @param {string} question 用户额外问题（可选）
 * @param {function} onMessage 收到内容片段的回调
 * @param {function} onDone 完成回调
 * @param {function} onError 错误回调
 * @returns {EventSource} 可用于关闭连接
 */
export function aiAnalyzeEnergyStream(type, question, onMessage, onDone, onError) {
  const params = new URLSearchParams()
  if (type) {
    params.append('type', type)
  }
  if (question) {
    params.append('question', question)
  }
  const queryString = params.toString()
  const url = `/api/ai/analyze-energy/stream${queryString ? '?' + queryString : ''}`
  
  const es = new EventSource(url, { withCredentials: true })

  es.addEventListener('message', (e) => {
    onMessage && onMessage(e.data)
  })

  es.addEventListener('done', () => {
    onDone && onDone()
    es.close()
  })

  es.addEventListener('error', (e) => {
    if (e.data) {
      onError && onError(e.data)
    } else {
      onError && onError('连接中断')
    }
    es.close()
  })

  es.onerror = () => {
    es.close()
  }

  return es
}

/**
 * 快速指令类型常量
 */
export const AI_ANALYSIS_TYPES = {
  OVERVIEW: 'overview',       // 全厂用电概览分析
  PEAK_VALLEY: 'peak-valley', // 峰谷电价优化建议
  ANOMALY: 'anomaly',         // 异常用电检测
  SAVING: 'saving',           // 节能降耗建议
  FORECAST: 'forecast'        // 用电趋势预测
}
