<template>
  <div class="login-container">
    <div class="login-card">
      <div class="header">
        <div class="logo">⚡</div>
        <h1>智能电能监控平台</h1>
        <p>Smart Energy Monitoring Platform</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" 
                    prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading" size="large" class="login-btn">
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="register-link">
        还没有账号？<a @click="showRegister = true">立即注册</a>
      </div>
    </div>

    <!-- 注册弹窗 -->
    <el-dialog v-model="showRegister" title="注册账号" width="400px">
      <el-form ref="regFormRef" :model="regForm" :rules="regRules">
        <el-form-item prop="username">
          <el-input v-model="regForm.username" placeholder="用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="name">
          <el-input v-model="regForm.name" placeholder="真实姓名" prefix-icon="UserFilled" />
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="regForm.email" placeholder="邮箱" prefix-icon="Message" />
        </el-form-item>
        <el-form-item prop="phone">
          <el-input v-model="regForm.phone" placeholder="手机号" prefix-icon="Phone" />
        </el-form-item>
        <el-form-item prop="department">
          <el-input v-model="regForm.department" placeholder="部门" prefix-icon="OfficeBuilding" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="regForm.password" type="password" placeholder="密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="regForm.confirmPassword" type="password" placeholder="确认密码" prefix-icon="Lock" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRegister = false">取消</el-button>
        <el-button type="primary" :loading="regLoading" @click="handleRegister">注册</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { register } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    const data = await userStore.doLogin(form.username, form.password)
    ElMessage.success('登录成功')
    // 获取跳转路径并跳转
    const targetPath = userStore.homePage
    console.log('登录成功，跳转到:', targetPath, '用户数据:', data)
    await router.push(targetPath)
  } catch (e) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}

// 注册
const showRegister = ref(false)
const regFormRef = ref()
const regLoading = ref(false)
const regForm = reactive({
  username: '', name: '', email: '', phone: '', department: '', password: '', confirmPassword: ''
})
const regRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请确认密码', trigger: 'blur' }]
}

async function handleRegister() {
  await regFormRef.value.validate()
  if (regForm.password !== regForm.confirmPassword) {
    ElMessage.error('两次密码不一致')
    return
  }
  regLoading.value = true
  try {
    await register(regForm)
    ElMessage.success('注册成功，请等待管理员分配角色')
    showRegister.value = false
  } catch (e) {
    // 错误已在拦截器处理
  } finally {
    regLoading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0a192f 0%, #0d1126 100%);
}

.login-card {
  width: 420px;
  padding: 48px 40px;
  background: rgba(15, 23, 42, 0.9);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 0 60px rgba(0, 212, 255, 0.15);
}

.header {
  text-align: center;
  margin-bottom: 32px;
  
  .logo {
    width: 64px;
    height: 64px;
    margin: 0 auto 16px;
    background: linear-gradient(135deg, rgba(0, 212, 255, 0.2), rgba(102, 126, 234, 0.2));
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 32px;
    border: 1px solid rgba(0, 212, 255, 0.3);
  }
  
  h1 {
    font-size: 24px;
    background: linear-gradient(135deg, #00d4ff, #667eea);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    margin-bottom: 8px;
  }
  
  p {
    color: rgba(255, 255, 255, 0.5);
    font-size: 13px;
  }
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  letter-spacing: 4px;
  background: linear-gradient(135deg, #00d4ff, #667eea);
  border: none;
}

.register-link {
  text-align: center;
  margin-top: 16px;
  color: rgba(255, 255, 255, 0.5);
  font-size: 14px;
  
  a {
    color: #00d4ff;
    cursor: pointer;
    &:hover { text-decoration: underline; }
  }
}

:deep(.el-input__wrapper) {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(0, 212, 255, 0.3);
  border-radius: 8px;
  box-shadow: none;
  
  &:hover, &.is-focus {
    border-color: rgba(0, 212, 255, 0.6);
    box-shadow: 0 0 0 2px rgba(0, 212, 255, 0.1);
  }
}

:deep(.el-input__inner) {
  color: #fff;
  &::placeholder { color: rgba(255, 255, 255, 0.4); }
}

:deep(.el-input__prefix) {
  color: rgba(0, 212, 255, 0.6);
}
</style>
