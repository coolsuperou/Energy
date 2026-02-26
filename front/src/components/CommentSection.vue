<template>
  <div class="comment-section">
    <div class="comment-header">
      <h6><i class="bi bi-chat-dots"></i> 评论交流</h6>
      <span class="comment-count">{{ comments.length }} 条评论</span>
    </div>

    <!-- 评论列表 -->
    <div class="comment-list" v-loading="loading">
      <div v-if="comments.length === 0" class="comment-empty">
        <i class="bi bi-chat-square-text"></i>
        <span>暂无评论，快来发表第一条评论吧</span>
      </div>
      <div v-else class="comment-items">
        <div v-for="comment in comments" :key="comment.id" class="comment-item">
          <div class="comment-avatar">
            <el-avatar :size="36" :src="comment.avatarUrl">
              {{ getAvatarText(comment.userName) }}
            </el-avatar>
          </div>
          <div class="comment-body">
            <div class="comment-meta">
              <span class="comment-author">{{ comment.userName || '用户' }}</span>
              <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
            </div>
            <div class="comment-content">{{ comment.content }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 发表评论 -->
    <div class="comment-input">
      <el-input
        v-model="newComment"
        type="textarea"
        :rows="2"
        placeholder="请输入评论内容..."
        maxlength="500"
        show-word-limit
        resize="none"
      />
      <div class="comment-actions">
        <el-button type="primary" :loading="submitting" :disabled="!newComment.trim()" @click="submitComment">
          <i class="bi bi-send"></i> 发表评论
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getComments, addComment } from '@/api/comment'

const props = defineProps({
  relatedType: {
    type: String,
    required: true,
    validator: (value) => ['application', 'task'].includes(value)
  },
  relatedId: {
    type: Number,
    required: true
  }
})

const loading = ref(false)
const submitting = ref(false)
const comments = ref([])
const newComment = ref('')

// 加载评论列表
async function loadComments() {
  if (!props.relatedId) return
  
  loading.value = true
  try {
    const res = await getComments(props.relatedType, props.relatedId)
    if (res && res.code === 200) {
      comments.value = res.data || []
    }
  } catch (e) {
    console.error('加载评论失败', e)
  }
  loading.value = false
}

// 发表评论
async function submitComment() {
  if (!newComment.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  submitting.value = true
  try {
    const res = await addComment({
      relatedType: props.relatedType,
      relatedId: props.relatedId,
      content: newComment.value.trim()
    })
    if (res && res.code === 200) {
      ElMessage.success('评论发表成功')
      newComment.value = ''
      loadComments()
    }
  } catch (e) {
    console.error('发表评论失败', e)
  }
  submitting.value = false
}

// 格式化时间
function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  
  return time.replace('T', ' ').substring(0, 16)
}

// 获取头像文字
function getAvatarText(name) {
  if (!name) return '?'
  return name.substring(0, 1)
}

// 监听 relatedId 变化，重新加载评论
watch(() => props.relatedId, (newVal) => {
  if (newVal) {
    loadComments()
  }
}, { immediate: true })

onMounted(() => {
  loadComments()
})
</script>

<style lang="scss" scoped>
.comment-section {
  border-top: 1px solid #ebeef5;
  padding-top: 16px;
  margin-top: 16px;
}

.comment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;

  h6 {
    margin: 0;
    font-size: 14px;
    font-weight: 600;
    color: #303133;

    i {
      margin-right: 6px;
      color: #409eff;
    }
  }

  .comment-count {
    font-size: 12px;
    color: #909399;
  }
}

.comment-list {
  min-height: 60px;
  max-height: 300px;
  overflow-y: auto;
  margin-bottom: 16px;
}

.comment-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  color: #909399;

  i {
    font-size: 32px;
    margin-bottom: 8px;
  }

  span {
    font-size: 13px;
  }
}

.comment-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;

  .comment-avatar {
    flex-shrink: 0;
  }

  .comment-body {
    flex: 1;
    min-width: 0;
  }

  .comment-meta {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 6px;

    .comment-author {
      font-size: 13px;
      font-weight: 500;
      color: #303133;
    }

    .comment-time {
      font-size: 12px;
      color: #909399;
    }
  }

  .comment-content {
    font-size: 13px;
    color: #606266;
    line-height: 1.6;
    word-break: break-word;
  }
}

.comment-input {
  .comment-actions {
    display: flex;
    justify-content: flex-end;
    margin-top: 10px;

    .el-button {
      i {
        margin-right: 4px;
      }
    }
  }
}
</style>
