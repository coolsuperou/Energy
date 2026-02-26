<template>
  <div class="layout-wrapper">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: isCollapse }">
      <div class="sidebar-logo">
        <div class="logo-icon">⚡</div>
        <span v-show="!isCollapse" class="logo-text">电能监控平台</span>
      </div>
      <nav class="sidebar-menu">
        <slot name="menu"></slot>
        <div class="menu-divider"></div>
        <router-link to="/login" class="nav-link" @click="handleLogout">
          <i class="bi bi-box-arrow-left"></i>
          <span v-show="!isCollapse">退出登录</span>
        </router-link>
      </nav>
    </aside>

    <!-- 主内容区 -->
    <main class="main-wrapper" :class="{ expanded: isCollapse }">
      <!-- 顶部导航 -->
      <header class="top-navbar">
        <div class="navbar-left">
          <button class="collapse-btn" @click="isCollapse = !isCollapse">
            <i class="bi bi-list"></i>
          </button>
          <h5 class="page-title">{{ roleName }}</h5>
        </div>
        <div class="navbar-right">
          <button class="notify-btn" @click="$router.push(messageRoute)">
            <i class="bi bi-bell"></i>
            <span v-if="unreadCount > 0" class="notify-badge">{{ unreadCount }}</span>
          </button>
          <div class="user-dropdown" @click="goToProfile">
            <div class="user-avatar">
              <img v-if="userStore.user?.avatarUrl" :src="userStore.user.avatarUrl" alt="头像" />
              <span v-else>{{ userStore.user?.name?.charAt(0) || 'U' }}</span>
            </div>
            <span class="user-name">{{ userStore.user?.name }} · {{ userStore.user?.department }}</span>
          </div>
        </div>
      </header>

      <!-- 内容区 -->
      <div class="content-wrapper">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getUnreadCount } from '@/api/notification'

const props = defineProps({
  roleName: String,
  messageRoute: { type: String, default: '/message' }
})

const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)
const unreadCount = ref(0)

function handleLogout() {
  userStore.doLogout()
  router.push('/login')
}

// 跳转到个人中心
function goToProfile() {
  const role = userStore.user?.role
  let roleValue = typeof role === 'string' ? role : role?.value
  if (roleValue) roleValue = roleValue.toLowerCase()
  
  const profileRoutes = {
    'admin': '/admin/profile',
    'dispatcher': '/dispatcher/profile',
    'inspector': '/inspector/profile',
    'manager': '/manager/profile',
    'workshop': '/workshop/profile'
  }
  
  const route = profileRoutes[roleValue] || '/inspector/profile'
  router.push(route)
}

// 获取未读消息数量
async function fetchUnreadCount() {
  try {
    const res = await getUnreadCount()
    if (res.code === 200 && res.data) {
      unreadCount.value = res.data.count || 0
    }
  } catch (e) {
    console.error('获取未读消息数量失败', e)
  }
}

// 点击外部关闭下拉菜单已删除

let pollTimer = null

onMounted(() => {
  // 初始获取未读数量
  fetchUnreadCount()
  // 每30秒轮询一次
  pollTimer = setInterval(fetchUnreadCount, 30000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style lang="scss" scoped>
.layout-wrapper {
  min-height: 100vh;
  background: #f1f5f9;
}

// 侧边栏
.sidebar {
  width: 260px;
  min-height: 100vh;
  background: linear-gradient(180deg, #1e293b 0%, #0f172a 100%);
  position: fixed;
  left: 0;
  top: 0;
  z-index: 1000;
  transition: width 0.3s;
  
  &.collapsed {
    width: 64px;
    
    .logo-text, .nav-link span, .menu-divider { display: none; }
    .nav-link { justify-content: center; padding: 12px; }
  }
}

.sidebar-logo {
  padding: 20px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
  display: flex;
  align-items: center;
  gap: 12px;
  
  .logo-icon {
    width: 40px;
    height: 40px;
    background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    flex-shrink: 0;
  }
  
  .logo-text {
    color: #fff;
    font-size: 16px;
    font-weight: 600;
    white-space: nowrap;
  }
}

.sidebar-menu {
  padding: 16px 12px;
  
  :deep(.nav-link) {
    color: rgba(255,255,255,0.7);
    padding: 12px 16px;
    border-radius: 8px;
    margin-bottom: 4px;
    display: flex;
    align-items: center;
    gap: 12px;
    text-decoration: none;
    transition: all 0.3s;
    
    i { font-size: 18px; }
    
    &:hover {
      color: #fff;
      background: rgba(255,255,255,0.1);
    }
    
    &.router-link-exact-active {
      color: #fff;
      background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
    }
    
    .badge {
      margin-left: auto;
      background: #ef4444;
      color: #fff;
      padding: 2px 8px;
      border-radius: 10px;
      font-size: 12px;
    }
  }
  
  .menu-divider {
    height: 1px;
    background: rgba(255,255,255,0.1);
    margin: 16px 0;
  }
}

// 主内容区
.main-wrapper {
  margin-left: 260px;
  min-height: 100vh;
  transition: margin-left 0.3s;
  
  &.expanded { margin-left: 64px; }
}

// 顶部导航
.top-navbar {
  background: #fff;
  padding: 16px 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
  
  .collapse-btn {
    width: 36px;
    height: 36px;
    border: none;
    background: #f1f5f9;
    border-radius: 8px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    
    &:hover { background: #e2e8f0; }
    i { font-size: 18px; color: #475569; }
  }
  
  .page-title {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: #1e293b;
  }
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.notify-btn {
  width: 40px;
  height: 40px;
  border: none;
  background: #f1f5f9;
  border-radius: 50%;
  cursor: pointer;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  
  &:hover { background: #e2e8f0; }
  i { font-size: 18px; color: #475569; }
  
  .notify-badge {
    position: absolute;
    top: 0;
    right: 0;
    background: #ef4444;
    color: #fff;
    font-size: 10px;
    padding: 2px 6px;
    border-radius: 10px;
    min-width: 18px;
    text-align: center;
  }
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px;
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  
  &:hover { background: #f1f5f9; }
  
  .user-avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 600;
    overflow: hidden;
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    
    span {
      font-size: 14px;
    }
  }
  
  .user-name {
    color: #1e293b;
    font-size: 14px;
  }
}

// 内容区
.content-wrapper {
  padding: 24px;
}

// 响应式
@media (max-width: 992px) {
  .sidebar {
    transform: translateX(-100%);
    &.show { transform: translateX(0); }
  }
  .main-wrapper { margin-left: 0 !important; }
}
</style>
