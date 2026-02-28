<template>
  <div class="login-page">
    <!-- 背景 -->
    <div class="background-container">
      <div class="background-overlay"></div>
    </div>
    <div class="orb orb-1"></div>
    <div class="orb orb-2"></div>

    <div class="main-container">
      <div class="login-card">
        <div class="header-section">
          <div class="logo-icon">⚡</div>
          <h1 class="main-title">智能电能监控平台</h1>
          <p class="subtitle">Smart Energy Monitoring Platform</p>
        </div>

        <!-- 错误提示 -->
        <div class="error-message" v-if="errorMsg">
          <i class="bi bi-exclamation-circle"></i>
          <span>{{ errorMsg }}</span>
        </div>

        <form @submit.prevent="handleLogin">
          <div class="form-group">
            <div class="input-with-icon">
              <i class="bi bi-person input-icon"></i>
              <input type="text" v-model="form.username" class="form-input" placeholder="请输入用户名" required>
            </div>
          </div>
          <div class="form-group">
            <div class="input-with-icon">
              <i class="bi bi-lock input-icon"></i>
              <div class="password-wrapper">
                <input :type="showPwd ? 'text' : 'password'" v-model="form.password" class="form-input" placeholder="请输入密码" required>
                <button type="button" class="show-password-btn" @click="showPwd = !showPwd">
                  <i :class="showPwd ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
                </button>
              </div>
            </div>
          </div>
          <div class="form-options">
            <label class="remember-me">
              <input type="checkbox" v-model="remember">
              <span>记住我</span>
            </label>
            <a href="#" class="forgot-link">忘记密码？</a>
          </div>
          <button type="submit" class="login-btn" :disabled="loading">
            {{ loading ? '登录中...' : '登 录' }}
          </button>
          <div class="register-link">
            还没有账号？<a href="#" @click.prevent="showRegister = true">立即注册</a>
          </div>
        </form>
      </div>
    </div>

    <!-- 注册弹窗 -->
    <div class="modal-overlay" v-if="showRegister" @click.self="showRegister = false">
      <div class="register-card">
        <div class="modal-header">
          <h3>注册账号</h3>
          <button class="close-btn" @click="showRegister = false">&times;</button>
        </div>
        <div class="success-message" v-if="regSuccess">
          <i class="bi bi-check-circle"></i>
          <span>注册成功！请登录</span>
        </div>
        <div class="error-message" v-if="regError">
          <i class="bi bi-exclamation-circle"></i>
          <span>{{ regError }}</span>
        </div>
        <form @submit.prevent="handleRegister">
          <div class="form-group">
            <div class="input-with-icon">
              <i class="bi bi-person input-icon"></i>
              <input type="text" v-model="regForm.username" class="form-input" placeholder="请输入用户名" required>
            </div>
          </div>
          <div class="form-group">
            <div class="input-with-icon">
              <i class="bi bi-person-badge input-icon"></i>
              <input type="text" v-model="regForm.name" class="form-input" placeholder="请输入真实姓名" required>
            </div>
          </div>
          <div class="form-group">
            <div class="input-with-icon">
              <i class="bi bi-envelope input-icon"></i>
              <input type="email" v-model="regForm.email" class="form-input" placeholder="请输入邮箱">
            </div>
          </div>
          <div class="form-group">
            <div class="input-with-icon">
              <i class="bi bi-phone input-icon"></i>
              <input type="tel" v-model="regForm.phone" class="form-input" placeholder="请输入手机号">
            </div>
          </div>
          <div class="form-group">
            <div class="input-with-icon">
              <i class="bi bi-people input-icon"></i>
              <select v-model="regForm.role" class="form-input form-select" required>
                <option value="" disabled>请选择角色</option>
                <option value="manager">经理</option>
                <option value="dispatcher">调度员</option>
                <option value="inspector">巡检员</option>
                <option value="workshop">车间</option>
              </select>
            </div>
          </div>
          <div class="form-group" v-if="regForm.role === 'workshop'">
            <div class="input-with-icon">
              <i class="bi bi-building input-icon"></i>
              <select v-model="regForm.department" class="form-input form-select" required>
                <option value="" disabled>请选择所属车间</option>
                <option value="第一车间">第一车间</option>
                <option value="第二车间">第二车间</option>
                <option value="第三车间">第三车间</option>
                <option value="第四车间">第四车间</option>
              </select>
            </div>
          </div>
          <div class="form-group" v-if="regForm.role && regForm.role !== 'workshop'">
            <div class="input-with-icon">
              <i class="bi bi-building input-icon"></i>
              <input type="text" v-model="regForm.department" class="form-input" placeholder="所属部门" readonly>
            </div>
          </div>
          <div class="form-group">
            <div class="input-with-icon">
              <i class="bi bi-lock input-icon"></i>
              <input type="password" v-model="regForm.password" class="form-input" placeholder="请输入密码（6-20位）" required>
            </div>
          </div>
          <div class="form-group">
            <div class="input-with-icon">
              <i class="bi bi-lock-fill input-icon"></i>
              <input type="password" v-model="regForm.confirmPassword" class="form-input" placeholder="请确认密码" required>
            </div>
          </div>
          <button type="submit" class="login-btn" :disabled="regLoading">
            {{ regLoading ? '注册中...' : '注 册' }}
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { register } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

const form = reactive({ username: '', password: '' })
const loading = ref(false)
const errorMsg = ref('')
const showPwd = ref(false)
const remember = ref(false)

async function handleLogin() {
  errorMsg.value = ''
  if (!form.username || !form.password) {
    errorMsg.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  try {
    await userStore.doLogin(form.username, form.password)
    ElMessage.success('登录成功')
    await router.push(userStore.homePage)
  } catch (e) {
    errorMsg.value = e.message || '登录失败'
  } finally {
    loading.value = false
  }
}

// 注册
const showRegister = ref(false)
const regLoading = ref(false)
const regSuccess = ref(false)
const regError = ref('')
const regForm = reactive({
  username: '', name: '', email: '', phone: '', department: '', role: '', password: '', confirmPassword: ''
})

// 角色与部门映射
const roleDepartmentMap = {
  manager: '能源管理部',
  dispatcher: '能源管理部',
  inspector: '设备维护部',
  workshop: ''
}

// 监听角色变化，自动填写部门
watch(() => regForm.role, (newRole) => {
  if (newRole && roleDepartmentMap[newRole]) {
    regForm.department = roleDepartmentMap[newRole]
  } else if (newRole === 'workshop') {
    regForm.department = ''
  }
})

async function handleRegister() {
  regError.value = ''
  regSuccess.value = false
  if (regForm.password !== regForm.confirmPassword) {
    regError.value = '两次输入的密码不一致'
    return
  }
  regLoading.value = true
  try {
    await register(regForm)
    regSuccess.value = true
    setTimeout(() => { showRegister.value = false }, 3000)
  } catch (e) {
    regError.value = e.message || '注册失败'
  } finally {
    regLoading.value = false
  }
}

// 鼠标视差效果
function handleMouseMove(e) {
  const orbs = document.querySelectorAll('.orb')
  const mouseX = e.clientX / window.innerWidth
  const mouseY = e.clientY / window.innerHeight
  orbs.forEach((orb, index) => {
    const speed = (index + 1) * 20
    const x = (mouseX - 0.5) * speed
    const y = (mouseY - 0.5) * speed
    orb.style.transform = `translate(${x}px, ${y}px)`
  })
}

onMounted(() => {
  document.addEventListener('mousemove', handleMouseMove)
})

onUnmounted(() => {
  document.removeEventListener('mousemove', handleMouseMove)
})
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  overflow: hidden;
  position: relative;
}

.background-container {
  position: fixed;
  top: 0; left: 0;
  width: 100%; height: 100%;
  z-index: 0;
}

.background-overlay {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  background: 
    radial-gradient(circle at 30% 30%, rgba(102, 126, 234, 0.15) 0%, transparent 50%),
    radial-gradient(circle at 70% 70%, rgba(0, 212, 255, 0.1) 0%, transparent 50%),
    linear-gradient(135deg, rgba(10, 25, 47, 0.95) 0%, rgba(13, 17, 38, 0.98) 100%);
}

.orb {
  position: fixed;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
  pointer-events: none;
  animation: orbFloat 8s ease-in-out infinite;
  z-index: 0;
}

.orb-1 {
  width: 400px; height: 400px;
  background: radial-gradient(circle, rgba(102, 126, 234, 0.4) 0%, transparent 70%);
  top: -200px; right: -100px;
}

.orb-2 {
  width: 300px; height: 300px;
  background: radial-gradient(circle, rgba(0, 212, 255, 0.3) 0%, transparent 70%);
  bottom: -150px; left: -50px;
  animation-delay: 2s;
}

@keyframes orbFloat {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -30px) scale(1.1); }
  66% { transform: translate(-20px, 20px) scale(0.9); }
}

.main-container {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.login-card {
  width: 100%;
  max-width: 480px;
  background: rgba(15, 23, 42, 0.85);
  backdrop-filter: blur(20px);
  border-radius: 32px;
  padding: 48px 40px;
  box-shadow: 
    0 0 60px rgba(0, 212, 255, 0.15),
    0 20px 60px rgba(0, 0, 0, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.1);
  animation: fadeInUp 0.8s ease-out;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

.header-section {
  text-align: center;
  margin-bottom: 36px;
}

.logo-icon {
  width: 72px; height: 72px;
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.2) 0%, rgba(102, 126, 234, 0.2) 100%);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  margin: 0 auto 20px;
  border: 1px solid rgba(0, 212, 255, 0.3);
}

.main-title {
  font-size: 26px;
  font-weight: 600;
  background: linear-gradient(135deg, #00d4ff 0%, #667eea 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 8px;
  letter-spacing: 2px;
}

.subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
}

.form-group { margin-bottom: 20px; }

.input-with-icon {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 18px;
  color: rgba(0, 212, 255, 0.6);
  z-index: 2;
}

.form-input {
  width: 100%;
  height: 52px;
  background: rgba(15, 23, 42, 0.6);
  border: 1.5px solid rgba(0, 212, 255, 0.3);
  border-radius: 12px;
  padding: 0 18px 0 50px;
  font-size: 15px;
  color: #ffffff;
  transition: all 0.3s ease;
  outline: none;

  &::placeholder { color: rgba(255, 255, 255, 0.4); }

  &:focus {
    background: rgba(15, 23, 42, 0.8);
    border-color: rgba(0, 212, 255, 0.7);
    box-shadow: 0 0 0 4px rgba(0, 212, 255, 0.15), 0 0 20px rgba(0, 212, 255, 0.3);
  }
}

.form-select {
  appearance: none;
  cursor: pointer;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' fill='rgba(0,212,255,0.6)' viewBox='0 0 16 16'%3E%3Cpath d='M7.247 11.14 2.451 5.658C1.885 5.013 2.345 4 3.204 4h9.592a1 1 0 0 1 .753 1.659l-4.796 5.48a1 1 0 0 1-1.506 0z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 16px center;
  padding-right: 40px;

  option {
    background: #0f172a;
    color: #ffffff;
  }
}

.password-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  flex: 1;
  width: 100%;

  .form-input { padding-right: 50px; }
}

.show-password-btn {
  position: absolute;
  right: 16px;
  background: none;
  border: none;
  color: rgba(0, 212, 255, 0.6);
  cursor: pointer;
  padding: 8px;
  border-radius: 6px;
  transition: all 0.3s ease;

  &:hover {
    background: rgba(0, 212, 255, 0.1);
    color: rgba(0, 212, 255, 0.9);
  }
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.remember-me {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;

  input { accent-color: #00d4ff; }
}

.forgot-link {
  color: rgba(0, 212, 255, 0.8);
  font-size: 14px;
  text-decoration: none;

  &:hover { color: #00d4ff; }
}

.login-btn {
  width: 100%;
  height: 54px;
  background: linear-gradient(135deg, #00d4ff 0%, #667eea 100%);
  border: none;
  border-radius: 14px;
  color: #fff;
  font-size: 17px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  letter-spacing: 6px;
  box-shadow: 0 4px 20px rgba(0, 212, 255, 0.4);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 30px rgba(0, 212, 255, 0.6);
  }

  &:disabled {
    opacity: 0.7;
    cursor: not-allowed;
    transform: none;
  }
}

.error-message {
  background: rgba(239, 68, 68, 0.2);
  border: 1px solid rgba(239, 68, 68, 0.5);
  border-radius: 10px;
  padding: 12px 16px;
  margin-bottom: 20px;
  color: #fca5a5;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.register-link {
  text-align: center;
  margin-top: 20px;
  color: rgba(255, 255, 255, 0.5);
  font-size: 14px;

  a {
    color: #00d4ff;
    text-decoration: none;
    font-weight: 500;

    &:hover { text-decoration: underline; }
  }
}

.modal-overlay {
  position: fixed;
  top: 0; left: 0;
  width: 100%; height: 100%;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(5px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.register-card {
  width: 100%;
  max-width: 420px;
  background: rgba(15, 23, 42, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 36px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 0 60px rgba(0, 212, 255, 0.2);
  animation: fadeInUp 0.4s ease-out;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;

  h3 {
    color: #00d4ff;
    font-size: 22px;
    font-weight: 600;
    margin: 0;
  }
}

.close-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.5);
  font-size: 28px;
  cursor: pointer;
  line-height: 1;
  transition: all 0.3s;

  &:hover { color: #00d4ff; transform: rotate(90deg); }
}

.success-message {
  background: rgba(34, 197, 94, 0.2);
  border: 1px solid rgba(34, 197, 94, 0.5);
  border-radius: 10px;
  padding: 12px 16px;
  margin-bottom: 20px;
  color: #86efac;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 10px;
}

@media (max-width: 640px) {
  .login-card { padding: 36px 24px; border-radius: 24px; }
  .main-title { font-size: 22px; }
}
</style>
