<template>
  <div class="ai-page">
    <!-- 顶部标题 -->
    <div class="ai-page-header">
      <div class="ai-page-title">
        <i class="bi bi-robot"></i>
        <span>AI 智能分析助手</span>
      </div>
      <div class="ai-page-desc">基于讯飞星火大模型，为您提供专业的电能数据分析与节能建议</div>
    </div>

    <div class="ai-main">
      <!-- 左侧：对话区域 -->
      <div class="chat-panel">
        <div class="chat-messages" ref="messagesRef">
          <!-- 欢迎消息 -->
          <div v-if="messages.length === 0" class="welcome-area">
            <div class="welcome-icon">🤖</div>
            <h3>你好，我是 AI 能耗分析助手</h3>
            <p>我可以帮你分析工厂电能数据、发现异常用电、提供节能优化建议。试试下面的快捷操作吧：</p>
            <div class="quick-actions">
              <button class="quick-btn" @click="quickAnalyze('overview')">
                <i class="bi bi-speedometer2"></i>
                <span>全厂用电概览</span>
              </button>
              <button class="quick-btn" @click="quickAnalyze('peak-valley')">
                <i class="bi bi-graph-up-arrow"></i>
                <span>峰谷电价优化</span>
              </button>
              <button class="quick-btn" @click="quickAnalyze('anomaly')">
                <i class="bi bi-exclamation-triangle"></i>
                <span>异常用电检测</span>
              </button>
              <button class="quick-btn" @click="quickAnalyze('saving')">
                <i class="bi bi-lightbulb"></i>
                <span>节能降耗建议</span>
              </button>
              <button class="quick-btn" @click="quickAnalyze('forecast')">
                <i class="bi bi-bar-chart-line"></i>
                <span>用电趋势预测</span>
              </button>
            </div>
          </div>

          <!-- 消息列表 -->
          <div v-for="(msg, index) in messages" :key="index" :class="['message', msg.role]">
            <div class="message-avatar">
              <i :class="msg.role === 'user' ? 'bi bi-person-fill' : 'bi bi-robot'"></i>
            </div>
            <div class="message-content">
              <div class="message-text" v-html="renderMarkdown(msg.content)"></div>
              <div class="message-time">{{ msg.time }}</div>
            </div>
          </div>

          <!-- 加载中 -->
          <div v-if="loading && messages.length > 0 && messages[messages.length - 1].role === 'assistant' && !messages[messages.length - 1].content" class="message assistant">
            <div class="message-avatar"><i class="bi bi-robot"></i></div>
            <div class="message-content">
              <div class="typing-indicator"><span></span><span></span><span></span></div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="chat-input-area">
          <div class="input-wrapper">
            <textarea v-model="inputMessage" @keydown.enter.exact.prevent="sendMessage"
              placeholder="输入你的问题，按 Enter 发送..." rows="1" :disabled="loading" ref="inputRef"></textarea>
            <button class="send-btn" @click="sendMessage" :disabled="loading || !inputMessage.trim()">
              <i class="bi bi-send-fill"></i>
            </button>
          </div>
        </div>
      </div>

      <!-- 右侧：快捷功能面板 -->
      <div class="side-panel">
        <!-- 快速指令卡片 -->
        <div class="side-card">
          <div class="side-card-title"><i class="bi bi-lightning-charge-fill"></i> 快速指令</div>
          <div class="quick-command-list">
            <button class="command-btn" @click="quickAnalyze('overview')" :disabled="loading">
              <i class="bi bi-speedometer2"></i>
              <div class="command-info">
                <div class="command-name">全厂用电概览</div>
                <div class="command-desc">各车间用电量、费用汇总分析</div>
              </div>
            </button>
            <button class="command-btn" @click="quickAnalyze('peak-valley')" :disabled="loading">
              <i class="bi bi-graph-up-arrow"></i>
              <div class="command-info">
                <div class="command-name">峰谷电价优化</div>
                <div class="command-desc">峰/平/谷用电占比及错峰建议</div>
              </div>
            </button>
            <button class="command-btn" @click="quickAnalyze('anomaly')" :disabled="loading">
              <i class="bi bi-exclamation-triangle"></i>
              <div class="command-info">
                <div class="command-name">异常用电检测</div>
                <div class="command-desc">功率波动、超限情况检测</div>
              </div>
            </button>
            <button class="command-btn" @click="quickAnalyze('saving')" :disabled="loading">
              <i class="bi bi-lightbulb"></i>
              <div class="command-info">
                <div class="command-name">节能降耗建议</div>
                <div class="command-desc">基于数据的降本增效建议</div>
              </div>
            </button>
            <button class="command-btn" @click="quickAnalyze('forecast')" :disabled="loading">
              <i class="bi bi-bar-chart-line"></i>
              <div class="command-info">
                <div class="command-name">用电趋势预测</div>
                <div class="command-desc">未来用电趋势及预警</div>
              </div>
            </button>
          </div>
        </div>

        <!-- 常用问题 -->
        <div class="side-card">
          <div class="side-card-title"><i class="bi bi-chat-dots-fill"></i> 常用问题</div>
          <div class="faq-list">
            <button class="faq-btn" v-for="(q, i) in faqList" :key="i" @click="askQuestion(q)" :disabled="loading">{{ q }}</button>
          </div>
        </div>

        <!-- 使用说明 -->
        <div class="side-card">
          <div class="side-card-title"><i class="bi bi-info-circle-fill"></i> 使用说明</div>
          <ul class="tips-list">
            <li>点击快速指令自动获取实时数据分析</li>
            <li>支持自然语言提问</li>
            <li>可以询问能耗趋势、异常检测等</li>
            <li>分析结果仅供参考</li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import { aiChatStream, aiAnalyzeEnergyStream } from '@/api/ai'

// 图表实例缓存：DOM元素 -> { instance, hash }
const chartCache = new WeakMap()

const messages = ref([])
const inputMessage = ref('')
const loading = ref(false)
const messagesRef = ref(null)
const inputRef = ref(null)
let currentEs = null

const analysisTypeNames = {
  'overview': '全厂用电概览分析',
  'peak-valley': '峰谷电价优化分析',
  'anomaly': '异常用电检测',
  'saving': '节能降耗建议',
  'forecast': '用电趋势预测'
}

const faqList = [
  '今日各车间用电量排名如何？',
  '哪个时段用电量最高？',
  '本月能耗与上月相比有何变化？',
  '如何降低峰时段的用电成本？',
  '哪些车间存在用电异常？'
]

function getNow() {
  const d = new Date()
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  })
}

// base64 编码（兼容中文）
function b64encode(str) {
  return btoa(unescape(encodeURIComponent(str)))
}
function b64decode(str) {
  return decodeURIComponent(escape(atob(str)))
}

function renderMarkdown(text) {
  if (!text) return ''
  let out = text
  // 1. 提取已闭合的 ```echarts ... ``` 代码块，替换为图表占位 div
  out = out.replace(/```echarts\s*\n([\s\S]*?)```/g, (m, json) => {
    const trimmed = json.trim()
    const b64 = b64encode(trimmed)
    return `<div class="ai-chart-block" data-option="${b64}" data-hash="${b64.length}-${b64.slice(0,12)}"></div>`
  })
  // 2. 流式过程中尚未闭合的 ```echarts 块：用占位提示替换，避免显示原始 JSON
  if (/```echarts[\s\S]*$/.test(out)) {
    out = out.replace(/```echarts[\s\S]*$/, '<div class="ai-chart-loading"><i class="bi bi-bar-chart-line"></i> 图表生成中...</div>')
  }
  return out
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/\n/g, '<br>')
    .replace(/#{3}\s?(.*?)(<br>|$)/g, '<h4>$1</h4>')
    .replace(/#{2}\s?(.*?)(<br>|$)/g, '<h3>$1</h3>')
    .replace(/#{1}\s?(.*?)(<br>|$)/g, '<h3>$1</h3>')
}

// 渲染消息中的 ECharts 图表
function renderCharts() {
  if (!messagesRef.value) return
  const blocks = messagesRef.value.querySelectorAll('.ai-chart-block')
  blocks.forEach(el => {
    if (el.dataset.failed === '1') return
    const optionStr = el.getAttribute('data-option')
    const hash = el.getAttribute('data-hash')
    if (!optionStr) return
    const cached = chartCache.get(el)
    if (cached && cached.hash === hash) return
    try {
      const option = JSON.parse(b64decode(optionStr))
      // 关闭动画，避免流式过程中元素重建导致的闪烁
      option.animation = false
      // v-html 重渲染会替换 DOM 元素，已绑定的实例随之失效
      let inst = echarts.getInstanceByDom(el)
      if (!inst) {
        inst = echarts.init(el)
      }
      inst.setOption(option, true)
      chartCache.set(el, { instance: inst, hash })
    } catch (e) {
      el.dataset.failed = '1'
      el.innerHTML = '<div class="ai-chart-error">⚠️ 图表渲染失败：JSON 解析错误</div>'
    }
  })
}

function scheduleChartRender() {
  nextTick(renderCharts)
}

// 窗口尺寸变化时自适应图表
function handleResize() {
  if (!messagesRef.value) return
  messagesRef.value.querySelectorAll('.ai-chart-block').forEach(el => {
    const cached = chartCache.get(el)
    if (cached?.instance) cached.instance.resize()
  })
}
window.addEventListener('resize', handleResize)
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})

function sendMessage() {
  const text = inputMessage.value.trim()
  if (!text || loading.value) return
  messages.value.push({ role: 'user', content: text, time: getNow() })
  inputMessage.value = ''
  loading.value = true
  scrollToBottom()

  const aiMsg = { role: 'assistant', content: '', time: getNow() }
  messages.value.push(aiMsg)
  const msgIndex = messages.value.length - 1

  currentEs = aiChatStream(text,
    (chunk) => { messages.value[msgIndex].content += chunk; scrollToBottom(); scheduleChartRender() },
    () => { messages.value[msgIndex].time = getNow(); loading.value = false; currentEs = null; scrollToBottom(); scheduleChartRender() },
    (err) => { if (!messages.value[msgIndex].content) messages.value[msgIndex].content = '抱歉，分析请求失败：' + err; loading.value = false; currentEs = null; scrollToBottom() }
  )
}

function quickAnalyze(type) {
  if (loading.value) return
  const typeName = analysisTypeNames[type] || '智能能耗分析'
  messages.value.push({ role: 'user', content: `请进行${typeName}`, time: getNow() })
  loading.value = true
  scrollToBottom()

  const aiMsg = { role: 'assistant', content: '', time: getNow() }
  messages.value.push(aiMsg)
  const msgIndex = messages.value.length - 1

  currentEs = aiAnalyzeEnergyStream(type, null,
    (chunk) => { messages.value[msgIndex].content += chunk; scrollToBottom(); scheduleChartRender() },
    () => { messages.value[msgIndex].time = getNow(); loading.value = false; currentEs = null; scrollToBottom(); scheduleChartRender() },
    (err) => { if (!messages.value[msgIndex].content) messages.value[msgIndex].content = '抱歉，能耗分析失败：' + err; loading.value = false; currentEs = null; scrollToBottom() }
  )
}

function askQuestion(q) {
  inputMessage.value = q
  sendMessage()
}
</script>

<style scoped>
.ai-page { padding: 24px; height: calc(100vh - 60px); display: flex; flex-direction: column; color: #1e293b; }
.ai-page-header { margin-bottom: 20px; }
.ai-page-title { font-size: 22px; font-weight: 700; display: flex; align-items: center; gap: 10px; }
.ai-page-title i { font-size: 26px; color: #7c3aed; }
.ai-page-desc { color: #64748b; font-size: 14px; margin-top: 4px; }
.ai-main { flex: 1; display: flex; gap: 20px; min-height: 0; }

.chat-panel { flex: 1; background: #fff; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); display: flex; flex-direction: column; min-height: 0; }
.chat-messages { flex: 1; overflow-y: auto; padding: 24px; min-height: 0; }

.welcome-area { text-align: center; padding: 60px 20px; color: #64748b; }
.welcome-icon { font-size: 64px; margin-bottom: 16px; }
.welcome-area h3 { font-size: 20px; color: #1e293b; margin-bottom: 8px; }
.welcome-area p { font-size: 14px; margin-bottom: 24px; max-width: 400px; margin-left: auto; margin-right: auto; }
.quick-actions { display: flex; flex-wrap: wrap; gap: 12px; max-width: 520px; margin: 0 auto; justify-content: center; }
.quick-btn { display: flex; align-items: center; gap: 8px; padding: 12px 16px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 10px; cursor: pointer; font-size: 14px; color: #334155; transition: all 0.2s; }
.quick-btn:hover { background: #ede9fe; border-color: #c4b5fd; color: #7c3aed; }
.quick-btn i { font-size: 18px; color: #7c3aed; }

.message { display: flex; gap: 12px; margin-bottom: 20px; }
.message.user { flex-direction: row-reverse; }
.message-avatar { width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 18px; flex-shrink: 0; }
.message.assistant .message-avatar { background: linear-gradient(135deg, #7c3aed, #a78bfa); color: #fff; }
.message.user .message-avatar { background: #3b82f6; color: #fff; }
.message-content { max-width: 70%; }
.message-text { padding: 12px 16px; border-radius: 12px; font-size: 14px; line-height: 1.7; word-break: break-word; }
.message.assistant .message-text { background: #f8fafc; border: 1px solid #e2e8f0; border-top-left-radius: 4px; }
.message.user .message-text { background: #3b82f6; color: #fff; border-top-right-radius: 4px; }
.message-time { font-size: 11px; color: #94a3b8; margin-top: 4px; }
.message.user .message-time { text-align: right; }

.typing-indicator { display: flex; gap: 4px; padding: 12px 16px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; border-top-left-radius: 4px; }
.typing-indicator span { width: 8px; height: 8px; background: #94a3b8; border-radius: 50%; animation: typing 1.4s infinite; }
.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }
@keyframes typing { 0%, 60%, 100% { transform: translateY(0); opacity: 0.4; } 30% { transform: translateY(-6px); opacity: 1; } }

.chat-input-area { padding: 16px 24px; border-top: 1px solid #e2e8f0; }
.input-wrapper { display: flex; align-items: center; gap: 12px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 8px 12px; transition: border-color 0.2s; }
.input-wrapper:focus-within { border-color: #7c3aed; box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.1); }
.input-wrapper textarea { flex: 1; border: none; background: transparent; resize: none; font-size: 14px; line-height: 1.5; outline: none; color: #1e293b; max-height: 100px; }
.input-wrapper textarea::placeholder { color: #94a3b8; }
.send-btn { width: 40px; height: 40px; border-radius: 10px; border: none; background: linear-gradient(135deg, #7c3aed, #a78bfa); color: #fff; font-size: 16px; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: opacity 0.2s; flex-shrink: 0; }
.send-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.send-btn:not(:disabled):hover { opacity: 0.9; }

.side-panel { width: 300px; display: flex; flex-direction: column; gap: 16px; flex-shrink: 0; }
.side-card { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
.side-card-title { font-size: 14px; font-weight: 600; margin-bottom: 12px; display: flex; align-items: center; gap: 6px; color: #1e293b; }
.side-card-title i { color: #7c3aed; }

.quick-command-list { display: flex; flex-direction: column; gap: 8px; }
.command-btn { width: 100%; display: flex; align-items: center; gap: 12px; padding: 10px 12px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 10px; cursor: pointer; transition: all 0.2s; text-align: left; }
.command-btn:hover:not(:disabled) { background: #ede9fe; border-color: #c4b5fd; }
.command-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.command-btn i { font-size: 20px; color: #7c3aed; flex-shrink: 0; }
.command-info { flex: 1; min-width: 0; }
.command-name { font-size: 13px; font-weight: 600; color: #1e293b; }
.command-desc { font-size: 11px; color: #64748b; margin-top: 2px; }

.faq-list { display: flex; flex-direction: column; gap: 8px; }
.faq-btn { text-align: left; padding: 10px 12px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; font-size: 13px; color: #334155; cursor: pointer; transition: all 0.2s; }
.faq-btn:hover:not(:disabled) { background: #ede9fe; border-color: #c4b5fd; color: #7c3aed; }
.faq-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.tips-list { list-style: none; padding: 0; margin: 0; }
.tips-list li { font-size: 13px; color: #64748b; padding: 6px 0; padding-left: 16px; position: relative; }
.tips-list li::before { content: '•'; position: absolute; left: 0; color: #7c3aed; }

@media (max-width: 1200px) { .side-panel { width: 260px; } }
@media (max-width: 900px) {
  .ai-main { flex-direction: column; }
  .side-panel { width: 100%; flex-direction: row; flex-wrap: wrap; }
  .side-card { flex: 1; min-width: 200px; }
}
</style>

<!-- 非 scoped：用于 v-html 动态插入的 ECharts 占位元素 -->
<style>
.ai-chart-block { width: 100%; height: 320px; margin: 12px 0; padding: 8px; background: #fff; border: 1px solid #e2e8f0; border-radius: 8px; box-sizing: border-box; }
.ai-chart-loading { padding: 16px; margin: 12px 0; text-align: center; color: #7c3aed; background: #faf5ff; border: 1px dashed #c4b5fd; border-radius: 8px; font-size: 13px; }
.ai-chart-loading i { margin-right: 6px; }
.ai-chart-error { padding: 12px; color: #dc2626; background: #fef2f2; border: 1px solid #fecaca; border-radius: 8px; font-size: 13px; }
</style>
