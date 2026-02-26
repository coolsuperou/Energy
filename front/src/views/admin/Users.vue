<template>
  <div class="admin-users">
    <!-- 搜索和操作栏 -->
    <div class="toolbar">
      <div class="search-area">
        <el-input
          v-model="searchParams.keyword"
          placeholder="搜索用户名或姓名"
          clearable
          style="width: 200px"
          @keyup.enter="loadUsers"
        >
          <template #prefix>
            <i class="bi bi-search"></i>
          </template>
        </el-input>
        <el-select
          v-model="searchParams.role"
          placeholder="选择角色"
          clearable
          style="width: 150px"
          @change="loadUsers"
        >
          <el-option label="全部角色" value="" />
          <el-option label="车间用户" value="workshop" />
          <el-option label="调度员" value="dispatcher" />
          <el-option label="巡检员" value="inspector" />
          <el-option label="经理" value="manager" />
          <el-option label="管理员" value="admin" />
        </el-select>
        <el-button type="primary" @click="loadUsers">
          <i class="bi bi-search me-1"></i>查询
        </el-button>
      </div>
      <el-button type="success" @click="openUserDialog()">
        <i class="bi bi-plus-lg me-1"></i>添加用户
      </el-button>
    </div>

    <!-- 用户列表表格 -->
    <el-table :data="userList" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="getRoleTagType(row.role)">{{ getRoleName(row.role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="department" label="部门" width="120" />
      <el-table-column prop="phone" label="电话" width="130" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 'active' ? 'success' : 'danger'" size="small">
            {{ row.status === 'active' ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170">
        <template #default="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="openUserDialog(row)">
            <i class="bi bi-pencil"></i> 编辑
          </el-button>
          <el-button type="warning" link size="small" @click="openResetPasswordDialog(row)">
            <i class="bi bi-key"></i> 重置密码
          </el-button>
          <el-button
            :type="row.status === 'active' ? 'warning' : 'success'"
            link
            size="small"
            @click="toggleUserStatus(row)"
            :disabled="row.role === 'admin'"
          >
            <i :class="row.status === 'active' ? 'bi bi-pause-circle' : 'bi bi-play-circle'"></i>
            {{ row.status === 'active' ? '禁用' : '启用' }}
          </el-button>
          <el-popconfirm
            title="确定要删除该用户吗？"
            @confirm="handleDeleteUser(row.id)"
          >
            <template #reference>
              <el-button type="danger" link size="small" :disabled="row.role === 'admin'">
                <i class="bi bi-trash"></i> 删除
              </el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadUsers"
        @current-change="loadUsers"
      />
    </div>

    <!-- 添加/编辑用户对话框 -->
    <el-dialog
      v-model="userDialogVisible"
      :title="isEditUser ? '编辑用户' : '添加用户'"
      width="500px"
      destroy-on-close
    >
      <el-form ref="userFormRef" :model="userForm" :rules="userRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入用户名" :disabled="isEditUser" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEditUser">
          <el-input
            v-model="userForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="userForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" placeholder="请选择角色" style="width: 100%">
            <el-option label="车间用户" value="WORKSHOP" />
            <el-option label="调度员" value="DISPATCHER" />
            <el-option label="巡检员" value="INSPECTOR" />
            <el-option label="经理" value="MANAGER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门" prop="department">
          <el-input v-model="userForm.department" placeholder="请输入部门" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitUserForm" :loading="submitLoading">
          {{ isEditUser ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog
      v-model="resetPasswordDialogVisible"
      title="重置密码"
      width="400px"
      destroy-on-close
    >
      <el-form ref="resetPasswordFormRef" :model="resetPasswordForm" :rules="resetPasswordRules" label-width="80px">
        <el-form-item label="用户">
          <el-input :value="resetPasswordForm.username" disabled />
        </el-form-item>
        <el-form-item label="新密码" prop="password">
          <el-input
            v-model="resetPasswordForm.password"
            type="password"
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="resetPasswordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPasswordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitResetPassword" :loading="resetPasswordLoading">
          确认重置
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getUserList,
  createUser,
  updateUser,
  deleteUser,
  updateUserStatus,
  resetPassword
} from '@/api/admin'

// 用户列表数据
const userList = ref([])
const loading = ref(false)
const searchParams = reactive({
  keyword: '',
  role: ''
})
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 用户对话框
const userDialogVisible = ref(false)
const isEditUser = ref(false)
const editUserId = ref(null)
const submitLoading = ref(false)
const userFormRef = ref(null)
const userForm = reactive({
  username: '',
  password: '',
  name: '',
  role: '',
  department: '',
  phone: '',
  email: ''
})

const userRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

// 重置密码对话框
const resetPasswordDialogVisible = ref(false)
const resetPasswordLoading = ref(false)
const resetPasswordFormRef = ref(null)
const resetPasswordForm = reactive({
  userId: null,
  username: '',
  password: '',
  confirmPassword: ''
})

const resetPasswordRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== resetPasswordForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 加载用户列表
async function loadUsers() {
  loading.value = true
  try {
    const res = await getUserList({
      page: pagination.page,
      size: pagination.size,
      role: searchParams.role,
      keyword: searchParams.keyword
    })
    if (res.code === 200 && res.data) {
      userList.value = res.data.records || []
      pagination.total = res.data.total || 0
    }
  } catch (e) {
    console.error('加载用户列表失败', e)
  } finally {
    loading.value = false
  }
}

// 打开用户对话框
function openUserDialog(user = null) {
  isEditUser.value = !!user
  editUserId.value = user?.id || null
  
  if (user) {
    userForm.username = user.username
    userForm.password = ''
    userForm.name = user.name
    userForm.role = user.role?.toUpperCase() || ''
    userForm.department = user.department || ''
    userForm.phone = user.phone || ''
    userForm.email = user.email || ''
  } else {
    userForm.username = ''
    userForm.password = ''
    userForm.name = ''
    userForm.role = ''
    userForm.department = ''
    userForm.phone = ''
    userForm.email = ''
  }
  
  userDialogVisible.value = true
}

// 提交用户表单
async function submitUserForm() {
  if (!userFormRef.value) return
  
  await userFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitLoading.value = true
    try {
      const data = { ...userForm }
      
      if (isEditUser.value) {
        await updateUser(editUserId.value, data)
        ElMessage.success('用户更新成功')
      } else {
        await createUser(data)
        ElMessage.success('用户创建成功')
      }
      
      userDialogVisible.value = false
      loadUsers()
    } catch (e) {
      console.error('保存用户失败', e)
    } finally {
      submitLoading.value = false
    }
  })
}

// 切换用户状态
async function toggleUserStatus(user) {
  const newStatus = user.status === 'active' ? 'disabled' : 'active'
  const action = newStatus === 'active' ? '启用' : '禁用'
  
  try {
    await updateUserStatus(user.id, newStatus)
    ElMessage.success(`用户已${action}`)
    loadUsers()
  } catch (e) {
    console.error('更新用户状态失败', e)
  }
}

// 删除用户
async function handleDeleteUser(id) {
  try {
    await deleteUser(id)
    ElMessage.success('用户已删除')
    loadUsers()
  } catch (e) {
    console.error('删除用户失败', e)
  }
}

// 打开重置密码对话框
function openResetPasswordDialog(user) {
  resetPasswordForm.userId = user.id
  resetPasswordForm.username = user.username
  resetPasswordForm.password = ''
  resetPasswordForm.confirmPassword = ''
  resetPasswordDialogVisible.value = true
}

// 提交重置密码
async function submitResetPassword() {
  if (!resetPasswordFormRef.value) return
  
  await resetPasswordFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    resetPasswordLoading.value = true
    try {
      await resetPassword(resetPasswordForm.userId, resetPasswordForm.password)
      ElMessage.success('密码重置成功')
      resetPasswordDialogVisible.value = false
    } catch (e) {
      console.error('重置密码失败', e)
    } finally {
      resetPasswordLoading.value = false
    }
  })
}

// 工具函数
function getRoleName(role) {
  const roleMap = {
    workshop: '车间用户',
    dispatcher: '调度员',
    inspector: '巡检员',
    manager: '经理',
    admin: '管理员'
  }
  return roleMap[role?.toLowerCase()] || role
}

function getRoleTagType(role) {
  const typeMap = {
    workshop: 'info',
    dispatcher: 'warning',
    inspector: 'success',
    manager: 'primary',
    admin: 'danger'
  }
  return typeMap[role?.toLowerCase()] || 'info'
}

function formatDateTime(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 初始化
onMounted(() => {
  loadUsers()
})
</script>

<style lang="scss" scoped>
.admin-users {
  .toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    padding: 16px;
    background: #1a1a2e;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);

    .search-area {
      display: flex;
      gap: 12px;
      align-items: center;
    }
  }

  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
    padding: 16px;
    background: #1a1a2e;
    border-radius: 0 0 8px 8px;
  }

  :deep(.el-table) {
    border-radius: 8px;
    overflow: hidden;
    
    --el-table-bg-color: #1a1a2e;
    --el-table-tr-bg-color: #1a1a2e;
    --el-table-header-bg-color: #16213e;
    --el-table-row-hover-bg-color: #16213e;
    --el-table-border-color: #2a2a4a;
    --el-table-text-color: rgba(255, 255, 255, 0.85);
    --el-table-header-text-color: rgba(255, 255, 255, 0.9);
  }

  :deep(.el-input__wrapper) {
    background-color: #16213e;
    box-shadow: 0 0 0 1px #2a2a4a inset;
  }

  :deep(.el-select .el-input__wrapper) {
    background-color: #16213e;
  }

  :deep(.el-dialog) {
    background-color: #1a1a2e;
    
    .el-dialog__header {
      color: rgba(255, 255, 255, 0.9);
    }
    
    .el-dialog__body {
      color: rgba(255, 255, 255, 0.85);
    }
  }

  :deep(.el-form-item__label) {
    color: rgba(255, 255, 255, 0.85);
  }

  :deep(.el-pagination) {
    --el-pagination-bg-color: transparent;
    --el-pagination-text-color: rgba(255, 255, 255, 0.85);
    --el-pagination-button-bg-color: #16213e;
    --el-pagination-hover-color: #409eff;
  }
}
</style>
