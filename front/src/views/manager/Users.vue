<template>
  <div class="manager-users">
    <!-- 标签页切换 -->
    <el-tabs v-model="activeTab" class="user-tabs">
      <!-- 用户管理标签页 -->
      <el-tab-pane label="用户管理" name="users">
        <!-- 搜索和操作栏 -->
        <div class="toolbar">
          <div class="search-area">
            <el-input
              v-model="userSearch.keyword"
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
              v-model="userSearch.role"
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
        <el-table :data="userList" v-loading="userLoading" stripe>
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
              <el-tag :type="isActive(row.status) ? 'success' : 'danger'" size="small">
                {{ isActive(row.status) ? '正常' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="openUserDialog(row)">
                <i class="bi bi-pencil"></i> 编辑
              </el-button>
              <el-button
                :type="isActive(row.status) ? 'warning' : 'success'"
                link
                size="small"
                @click="toggleUserStatus(row)"
                :disabled="row.role === 'manager'"
              >
                <i :class="isActive(row.status) ? 'bi bi-pause-circle' : 'bi bi-play-circle'"></i>
                {{ isActive(row.status) ? '禁用' : '启用' }}
              </el-button>
              <el-popconfirm
                title="确定要删除该用户吗？"
                @confirm="handleDeleteUser(row.id)"
                :disabled="row.role === 'manager'"
              >
                <template #reference>
                  <el-button type="danger" link size="small" :disabled="row.role === 'manager'">
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
            v-model:current-page="userPagination.page"
            v-model:page-size="userPagination.size"
            :total="userPagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadUsers"
            @current-change="loadUsers"
          />
        </div>
      </el-tab-pane>

      <!-- 技能认证审核标签页 -->
      <el-tab-pane name="certifications">
        <template #label>
          <span>
            技能认证审核
            <el-badge v-if="pendingCount > 0" :value="pendingCount" class="cert-badge" />
          </span>
        </template>

        <!-- 筛选栏 -->
        <div class="toolbar">
          <div class="search-area">
            <el-select
              v-model="certSearch.status"
              placeholder="选择状态"
              clearable
              style="width: 150px"
              @change="loadCertifications"
            >
              <el-option label="全部状态" value="" />
              <el-option label="待审核" value="pending" />
              <el-option label="已认证" value="certified" />
              <el-option label="已拒绝" value="rejected" />
            </el-select>
            <el-button type="primary" @click="loadCertifications">
              <i class="bi bi-search me-1"></i>查询
            </el-button>
          </div>
        </div>

        <!-- 认证列表表格 -->
        <el-table :data="certList" v-loading="certLoading" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="userName" label="申请人" width="100" />
          <el-table-column prop="userDepartment" label="部门" width="120" />
          <el-table-column prop="skillName" label="技能名称" min-width="150" />
          <el-table-column label="证书" width="100">
            <template #default="{ row }">
              <el-button
                v-if="row.certificateUrl"
                type="primary"
                link
                size="small"
                @click="previewCertificate(row.certificateUrl)"
              >
                <i class="bi bi-image"></i> 查看
              </el-button>
              <span v-else class="text-muted">无</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getCertStatusType(row.status)">
                {{ getCertStatusName(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="申请时间" width="170">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column prop="reviewerName" label="审核人" width="100" />
          <el-table-column prop="rejectReason" label="拒绝原因" min-width="150" show-overflow-tooltip />
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status === 'pending'">
                <el-button type="success" link size="small" @click="handleApprove(row)">
                  <i class="bi bi-check-lg"></i> 通过
                </el-button>
                <el-button type="danger" link size="small" @click="openRejectDialog(row)">
                  <i class="bi bi-x-lg"></i> 拒绝
                </el-button>
              </template>
              <span v-else class="text-muted">已处理</span>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="certPagination.page"
            v-model:page-size="certPagination.size"
            :total="certPagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadCertifications"
            @current-change="loadCertifications"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

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
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="userForm.password"
            type="password"
            :placeholder="isEditUser ? '留空则不修改密码' : '请输入密码'"
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

    <!-- 拒绝认证对话框 -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝认证" width="400px" destroy-on-close>
      <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-width="80px">
        <el-form-item label="拒绝原因" prop="reason">
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入拒绝原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="submitReject" :loading="rejectLoading">确认拒绝</el-button>
      </template>
    </el-dialog>

    <!-- 证书预览对话框 -->
    <el-dialog v-model="previewVisible" title="证书预览" width="600px">
      <div class="certificate-preview">
        <el-image :src="previewUrl" fit="contain" style="max-width: 100%; max-height: 500px" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getUserList,
  createUser,
  updateUser,
  deleteUser,
  updateUserStatus,
  getCertificationList,
  approveCertification,
  rejectCertification
} from '@/api/manager'

// 当前标签页
const activeTab = ref('users')

// ==================== 用户管理 ====================
const userList = ref([])
const userLoading = ref(false)
const userSearch = reactive({
  keyword: '',
  role: ''
})
const userPagination = reactive({
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
  password: [
    {
      required: true,
      validator: (rule, value, callback) => {
        if (!isEditUser.value && !value) {
          callback(new Error('请输入密码'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

// 加载用户列表
async function loadUsers() {
  userLoading.value = true
  try {
    const res = await getUserList({
      page: userPagination.page,
      size: userPagination.size,
      role: userSearch.role,
      keyword: userSearch.keyword
    })
    if (res.code === 200 && res.data) {
      userList.value = res.data.records || []
      userPagination.total = res.data.total || 0
    }
  } catch (e) {
    console.error('加载用户列表失败', e)
  } finally {
    userLoading.value = false
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
      if (isEditUser.value && !data.password) {
        delete data.password
      }
      
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
  const newStatus = isActive(user.status) ? 'disabled' : 'active'
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

// ==================== 技能认证审核 ====================
const certList = ref([])
const certLoading = ref(false)
const certSearch = reactive({
  status: ''
})
const certPagination = reactive({
  page: 1,
  size: 10,
  total: 0
})
const pendingCount = ref(0)

// 拒绝对话框
const rejectDialogVisible = ref(false)
const rejectLoading = ref(false)
const rejectFormRef = ref(null)
const rejectForm = reactive({
  id: null,
  reason: ''
})
const rejectRules = {
  reason: [{ required: true, message: '请输入拒绝原因', trigger: 'blur' }]
}

// 证书预览
const previewVisible = ref(false)
const previewUrl = ref('')

// 加载认证列表
async function loadCertifications() {
  certLoading.value = true
  try {
    const res = await getCertificationList({
      page: certPagination.page,
      size: certPagination.size,
      status: certSearch.status
    })
    if (res.code === 200 && res.data) {
      certList.value = res.data.records || []
      certPagination.total = res.data.total || 0
    }
  } catch (e) {
    console.error('加载认证列表失败', e)
  } finally {
    certLoading.value = false
  }
}

// 加载待审核数量
async function loadPendingCount() {
  try {
    const res = await getCertificationList({ page: 1, size: 1, status: 'pending' })
    if (res.code === 200 && res.data) {
      pendingCount.value = res.data.total || 0
    }
  } catch (e) {
    console.error('加载待审核数量失败', e)
  }
}

// 审核通过
async function handleApprove(cert) {
  try {
    await approveCertification(cert.id)
    ElMessage.success('认证已通过')
    loadCertifications()
    loadPendingCount()
  } catch (e) {
    console.error('审核通过失败', e)
  }
}

// 打开拒绝对话框
function openRejectDialog(cert) {
  rejectForm.id = cert.id
  rejectForm.reason = ''
  rejectDialogVisible.value = true
}

// 提交拒绝
async function submitReject() {
  if (!rejectFormRef.value) return
  
  await rejectFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    rejectLoading.value = true
    try {
      await rejectCertification(rejectForm.id, rejectForm.reason)
      ElMessage.success('认证已拒绝')
      rejectDialogVisible.value = false
      loadCertifications()
      loadPendingCount()
    } catch (e) {
      console.error('拒绝认证失败', e)
    } finally {
      rejectLoading.value = false
    }
  })
}

// 预览证书
function previewCertificate(url) {
  previewUrl.value = url
  previewVisible.value = true
}

// ==================== 工具函数 ====================
function isActive(status) {
  return status?.toLowerCase() === 'active'
}

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

function getCertStatusName(status) {
  const statusMap = {
    pending: '待审核',
    certified: '已认证',
    rejected: '已拒绝'
  }
  return statusMap[status] || status
}

function getCertStatusType(status) {
  const typeMap = {
    pending: 'warning',
    certified: 'success',
    rejected: 'danger'
  }
  return typeMap[status] || 'info'
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
  loadCertifications()
  loadPendingCount()
})
</script>

<style lang="scss" scoped>
.manager-users {
  .user-tabs {
    :deep(.el-tabs__header) {
      margin-bottom: 20px;
    }
  }

  .toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    padding: 16px;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);

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
    background: #fff;
    border-radius: 0 0 8px 8px;
  }

  .cert-badge {
    margin-left: 6px;
    
    :deep(.el-badge__content) {
      top: 2px;
    }
  }

  .certificate-preview {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 200px;
  }

  .text-muted {
    color: #999;
    font-size: 13px;
  }

  :deep(.el-table) {
    border-radius: 8px;
    overflow: hidden;
  }
}
</style>
