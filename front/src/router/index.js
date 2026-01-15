import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  // 车间用户
  {
    path: '/workshop',
    component: () => import('@/layouts/WorkshopLayout.vue'),
    meta: { requiresAuth: true, role: 'workshop' },
    children: [
      { path: '', name: 'WorkshopHome', component: () => import('@/views/workshop/Index.vue') },
      { path: 'apply', name: 'Apply', component: () => import('@/views/workshop/Apply.vue') },
      { path: 'energy', name: 'Energy', component: () => import('@/views/workshop/Energy.vue') },
      { path: 'feedback', name: 'Feedback', component: () => import('@/views/workshop/Feedback.vue') },
      { path: 'message', name: 'WorkshopMessage', component: () => import('@/views/common/Message.vue') }
    ]
  },
  // 能源调度员
  {
    path: '/dispatcher',
    component: () => import('@/layouts/DispatcherLayout.vue'),
    meta: { requiresAuth: true, role: 'dispatcher' },
    children: [
      { path: '', name: 'DispatcherHome', component: () => import('@/views/dispatcher/Index.vue') },
      { path: 'approval', name: 'Approval', component: () => import('@/views/dispatcher/Approval.vue') },
      { path: 'dispatch', name: 'Dispatch', component: () => import('@/views/dispatcher/Dispatch.vue') },
      { path: 'alert', name: 'Alert', component: () => import('@/views/dispatcher/Alert.vue') },
      { path: 'order', name: 'Order', component: () => import('@/views/dispatcher/Order.vue') },
      { path: 'tasks', name: 'TaskManage', component: () => import('@/views/dispatcher/Tasks.vue') },
      { path: 'message', name: 'DispatcherMessage', component: () => import('@/views/common/Message.vue') }
    ]
  },
  // 设备巡检员
  {
    path: '/inspector',
    component: () => import('@/layouts/InspectorLayout.vue'),
    meta: { requiresAuth: true, role: 'inspector' },
    children: [
      { path: '', name: 'InspectorHome', component: () => import('@/views/inspector/Index.vue') },
      { path: 'equipment', name: 'Equipment', component: () => import('@/views/inspector/Equipment.vue') },
      { path: 'message', name: 'InspectorMessage', component: () => import('@/views/common/Message.vue') },
      { path: 'profile', name: 'Profile', component: () => import('@/views/inspector/Profile.vue') }
    ]
  },
  // 能源经理
  {
    path: '/manager',
    component: () => import('@/layouts/ManagerLayout.vue'),
    meta: { requiresAuth: true, role: 'manager' },
    children: [
      { path: '', name: 'ManagerHome', component: () => import('@/views/manager/Index.vue') },
      { path: 'analysis', name: 'Analysis', component: () => import('@/views/manager/Analysis.vue') },
      { path: 'ai', name: 'AIAnalysis', component: () => import('@/views/manager/AI.vue') },
      { path: 'message', name: 'ManagerMessage', component: () => import('@/views/common/Message.vue') }
    ]
  },
  // 系统管理员
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, role: 'admin' },
    children: [
      { path: '', name: 'AdminHome', component: () => import('@/views/admin/Index.vue') },
      { path: 'users', name: 'UserManage', component: () => import('@/views/admin/Users.vue') },
      { path: 'config', name: 'SystemConfig', component: () => import('@/views/admin/Config.vue') },
      { path: 'logs', name: 'OperationLogs', component: () => import('@/views/admin/Logs.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  // 不需要认证的页面直接放行
  if (to.meta.requiresAuth === false) {
    next()
    return
  }
  
  // 需要认证但未登录，跳转登录页
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next('/login')
    return
  }
  
  // 已登录访问登录页，跳转首页
  if (to.path === '/login' && userStore.isLoggedIn) {
    next(userStore.homePage)
    return
  }
  
  next()
})

export default router
