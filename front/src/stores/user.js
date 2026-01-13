import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, logout, getCurrentUser } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const user = ref(null)
  const token = ref(localStorage.getItem('token') || '')

  const isLoggedIn = computed(() => !!user.value)
  
  const homePage = computed(() => {
    if (!user.value) return '/login'
    const rolePages = {
      'admin': '/admin',
      'dispatcher': '/dispatcher',
      'inspector': '/inspector',
      'manager': '/manager',
      'workshop': '/workshop'
    }
    // 兼容 role 是字符串或对象的情况，统一转小写
    let roleValue = typeof user.value.role === 'string' 
      ? user.value.role 
      : user.value.role?.value
    if (roleValue) {
      roleValue = roleValue.toLowerCase()
    }
    return rolePages[roleValue] || '/login'
  })

  async function doLogin(username, password) {
    const res = await login({ username, password })
    if (res.code === 200) {
      user.value = res.data
      localStorage.setItem('user', JSON.stringify(res.data))
      return res.data
    }
    throw new Error(res.message)
  }

  async function doLogout() {
    await logout()
    user.value = null
    localStorage.removeItem('user')
  }

  function initUser() {
    const savedUser = localStorage.getItem('user')
    if (savedUser) {
      try {
        user.value = JSON.parse(savedUser)
      } catch (e) {
        localStorage.removeItem('user')
      }
    }
  }

  // 初始化
  initUser()

  return {
    user,
    token,
    isLoggedIn,
    homePage,
    doLogin,
    doLogout,
    initUser
  }
})
