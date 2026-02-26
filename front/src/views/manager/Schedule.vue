<template>
  <div class="schedule-page">
    <div class="page-header">
      <h2>排班考勤管理</h2>
    </div>

    <!-- 标签页切换 -->
    <el-tabs v-model="activeTab" class="main-tabs">
      <!-- 排班管理 -->
      <el-tab-pane label="排班管理" name="schedule">
        <div class="tab-content">
          <!-- 操作栏 -->
          <div class="toolbar">
            <div class="left">
              <el-date-picker
                v-model="scheduleMonth"
                type="month"
                placeholder="选择月份"
                format="YYYY年MM月"
                value-format="YYYY-MM"
                @change="loadScheduleList"
              />
              <el-select v-model="scheduleFilter.role" placeholder="角色筛选" clearable @change="loadScheduleList">
                <el-option label="调度员" value="dispatcher" />
                <el-option label="巡检员" value="inspector" />
                <el-option label="车间" value="workshop" />
              </el-select>
              <el-input
                v-model="scheduleFilter.department"
                placeholder="部门筛选"
                clearable
                style="width: 150px"
                @change="loadScheduleList"
              />
            </div>
            <div class="right">
              <el-button type="primary" @click="handleGenerateSchedule">
                <i class="bi bi-calendar-plus"></i> 生成排班
              </el-button>
            </div>
          </div>

          <!-- 排班表 -->
          <el-table :data="scheduleList" v-loading="scheduleLoading" stripe>
            <el-table-column prop="userName" label="姓名" width="100" fixed />
            <el-table-column prop="role" label="角色" width="80">
              <template #default="{ row }">
                <el-tag size="small">{{ getRoleName(row.role) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="department" label="部门" width="100" />
            <el-table-column label="排班详情" min-width="600">
              <template #default="{ row }">
                <div class="schedule-cells">
                  <div
                    v-for="schedule in row.schedules"
                    :key="schedule.id"
                    class="schedule-cell"
                    :class="getShiftClass(schedule.shiftType)"
                    @click="handleEditSchedule(schedule, row)"
                  >
                    <div class="date">{{ formatDay(schedule.attendanceDate) }}</div>
                    <div class="shift">{{ getShiftName(schedule.shiftType) }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <el-pagination
            v-model:current-page="schedulePage"
            v-model:page-size="scheduleSize"
            :total="scheduleTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @size-change="loadScheduleList"
            @current-change="loadScheduleList"
          />
        </div>
      </el-tab-pane>

      <!-- 考勤记录 -->
      <el-tab-pane label="考勤记录" name="attendance">
        <div class="tab-content">
          <!-- 筛选栏 -->
          <div class="toolbar">
            <div class="left">
              <el-date-picker
                v-model="attendanceDateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                @change="loadAttendanceList"
              />
              <el-select v-model="attendanceFilter.status" placeholder="状态筛选" clearable @change="loadAttendanceList">
                <el-option label="正常" value="normal" />
                <el-option label="迟到" value="late" />
                <el-option label="早退" value="early_leave" />
                <el-option label="缺勤" value="absent" />
              </el-select>
            </div>
            <div class="right">
              <el-button @click="loadDailySummary">
                <i class="bi bi-bar-chart"></i> 今日汇总
              </el-button>
            </div>
          </div>

          <!-- 考勤列表 -->
          <el-table :data="attendanceList" v-loading="attendanceLoading" stripe>
            <el-table-column prop="userName" label="姓名" width="100" />
            <el-table-column prop="department" label="部门" width="100" />
            <el-table-column prop="attendanceDate" label="日期" width="120" />
            <el-table-column prop="shiftType" label="班次" width="80">
              <template #default="{ row }">
                {{ getShiftName(row.shiftType) }}
              </template>
            </el-table-column>
            <el-table-column label="计划时间" width="140">
              <template #default="{ row }">
                {{ row.scheduledStartTime || '-' }} ~ {{ row.scheduledEndTime || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="打卡时间" width="140">
              <template #default="{ row }">
                {{ row.clockInTime || '-' }} ~ {{ row.clockOutTime || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small">
                  {{ getStatusName(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="workHours" label="工时" width="80">
              <template #default="{ row }">
                {{ row.workHours ? row.workHours + 'h' : '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="handleSupplement(row)">
                  补卡
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <el-pagination
            v-model:current-page="attendancePage"
            v-model:page-size="attendanceSize"
            :total="attendanceTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @size-change="loadAttendanceList"
            @current-change="loadAttendanceList"
          />
        </div>
      </el-tab-pane>

      <!-- 月度报表 -->
      <el-tab-pane label="月度报表" name="report">
        <div class="tab-content">
          <div class="toolbar">
            <div class="left">
              <el-date-picker
                v-model="reportMonth"
                type="month"
                placeholder="选择月份"
                format="YYYY年MM月"
                value-format="YYYY-MM"
                @change="loadMonthlyReport"
              />
            </div>
          </div>

          <!-- 报表表格 -->
          <el-table :data="reportList" v-loading="reportLoading" stripe show-summary>
            <el-table-column prop="userName" label="姓名" width="100" />
            <el-table-column prop="role" label="角色" width="80">
              <template #default="{ row }">
                <el-tag size="small">{{ getRoleName(row.role) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="department" label="部门" width="100" />
            <el-table-column prop="workDays" label="出勤天数" width="100" />
            <el-table-column prop="normalDays" label="正常" width="80" />
            <el-table-column prop="lateDays" label="迟到" width="80" />
            <el-table-column prop="earlyLeaveDays" label="早退" width="80" />
            <el-table-column prop="absentDays" label="缺勤" width="80" />
            <el-table-column prop="restDays" label="休息" width="80" />
            <el-table-column prop="attendanceRate" label="出勤率" width="100">
              <template #default="{ row }">
                <span :class="{ 'text-danger': row.attendanceRate < 90 }">
                  {{ row.attendanceRate }}%
                </span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 排班调整对话框 -->
    <el-dialog v-model="scheduleDialogVisible" title="调整排班" width="400px">
      <el-form :model="scheduleForm" label-width="100px">
        <el-form-item label="员工">
          <span>{{ scheduleForm.userName }}</span>
        </el-form-item>
        <el-form-item label="日期">
          <span>{{ scheduleForm.attendanceDate }}</span>
        </el-form-item>
        <el-form-item label="班次类型">
          <el-select v-model="scheduleForm.shiftType" @change="handleShiftTypeChange">
            <el-option label="白班" value="DAY" />
            <el-option label="夜班" value="NIGHT" />
            <el-option label="休息" value="REST" />
          </el-select>
        </el-form-item>
        <el-form-item label="上班时间" v-if="scheduleForm.shiftType !== 'REST'">
          <el-time-picker v-model="scheduleForm.scheduledStartTime" format="HH:mm" value-format="HH:mm" />
        </el-form-item>
        <el-form-item label="下班时间" v-if="scheduleForm.shiftType !== 'REST'">
          <el-time-picker v-model="scheduleForm.scheduledEndTime" format="HH:mm" value-format="HH:mm" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scheduleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitScheduleAdjust" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 补卡对话框 -->
    <el-dialog v-model="supplementDialogVisible" title="补卡" width="400px">
      <el-form :model="supplementForm" label-width="100px">
        <el-form-item label="员工">
          <span>{{ supplementForm.userName }}</span>
        </el-form-item>
        <el-form-item label="日期">
          <span>{{ supplementForm.attendanceDate }}</span>
        </el-form-item>
        <el-form-item label="上班打卡">
          <el-time-picker v-model="supplementForm.clockInTime" format="HH:mm" value-format="HH:mm" placeholder="补录上班时间" />
        </el-form-item>
        <el-form-item label="下班打卡">
          <el-time-picker v-model="supplementForm.clockOutTime" format="HH:mm" value-format="HH:mm" placeholder="补录下班时间" />
        </el-form-item>
        <el-form-item label="补卡原因">
          <el-input v-model="supplementForm.reason" type="textarea" rows="2" placeholder="请输入补卡原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="supplementDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitSupplement" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 今日汇总对话框 -->
    <el-dialog v-model="summaryDialogVisible" title="今日考勤汇总" width="400px">
      <div class="summary-content" v-if="dailySummary">
        <div class="summary-item">
          <span class="label">日期：</span>
          <span class="value">{{ dailySummary.date }}</span>
        </div>
        <div class="summary-item">
          <span class="label">应出勤：</span>
          <span class="value">{{ dailySummary.totalCount }} 人</span>
        </div>
        <div class="summary-item">
          <span class="label">正常：</span>
          <span class="value text-success">{{ dailySummary.normalCount }} 人</span>
        </div>
        <div class="summary-item">
          <span class="label">迟到：</span>
          <span class="value text-warning">{{ dailySummary.lateCount }} 人</span>
        </div>
        <div class="summary-item">
          <span class="label">早退：</span>
          <span class="value text-warning">{{ dailySummary.earlyLeaveCount }} 人</span>
        </div>
        <div class="summary-item">
          <span class="label">缺勤：</span>
          <span class="value text-danger">{{ dailySummary.absentCount }} 人</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>


<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  generateSchedule,
  getScheduleList,
  adjustSchedule,
  getAttendanceList,
  getDailySummary,
  supplementAttendance,
  getMonthlyReport
} from '@/api/attendance'

// 当前标签页
const activeTab = ref('schedule')

// ==================== 排班管理 ====================
const scheduleMonth = ref(getCurrentMonth())
const scheduleFilter = ref({ role: '', department: '' })
const scheduleList = ref([])
const scheduleLoading = ref(false)
const schedulePage = ref(1)
const scheduleSize = ref(10)
const scheduleTotal = ref(0)

// 排班调整对话框
const scheduleDialogVisible = ref(false)
const scheduleForm = ref({})
const submitLoading = ref(false)

// ==================== 考勤记录 ====================
const attendanceDateRange = ref([])
const attendanceFilter = ref({ status: '' })
const attendanceList = ref([])
const attendanceLoading = ref(false)
const attendancePage = ref(1)
const attendanceSize = ref(10)
const attendanceTotal = ref(0)

// 补卡对话框
const supplementDialogVisible = ref(false)
const supplementForm = ref({})

// 今日汇总
const summaryDialogVisible = ref(false)
const dailySummary = ref(null)

// ==================== 月度报表 ====================
const reportMonth = ref(getCurrentMonth())
const reportList = ref([])
const reportLoading = ref(false)

// 获取当前月份
function getCurrentMonth() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

// 加载排班列表
async function loadScheduleList() {
  scheduleLoading.value = true
  try {
    const [year, month] = scheduleMonth.value.split('-')
    const res = await getScheduleList({
      page: schedulePage.value,
      size: scheduleSize.value,
      year: parseInt(year),
      month: parseInt(month),
      role: scheduleFilter.value.role,
      department: scheduleFilter.value.department
    })
    if (res.code === 200) {
      scheduleList.value = res.data.records || []
      scheduleTotal.value = res.data.total || 0
    }
  } catch (error) {
    console.error('加载排班列表失败', error)
  } finally {
    scheduleLoading.value = false
  }
}

// 生成排班
async function handleGenerateSchedule() {
  try {
    await ElMessageBox.confirm(
      `确定要生成 ${scheduleMonth.value} 的排班计划吗？已存在的排班不会被覆盖。`,
      '生成排班',
      { type: 'info' }
    )
    const [year, month] = scheduleMonth.value.split('-')
    const res = await generateSchedule(parseInt(year), parseInt(month))
    if (res.code === 200) {
      ElMessage.success(`排班生成成功，共生成 ${res.data.generatedCount} 条记录`)
      loadScheduleList()
    } else {
      ElMessage.error(res.message || '生成失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('生成排班失败', error)
    }
  }
}

// 编辑排班
function handleEditSchedule(schedule, row) {
  scheduleForm.value = {
    id: schedule.id,
    userName: row.userName,
    attendanceDate: schedule.attendanceDate,
    shiftType: schedule.shiftType?.toUpperCase() || 'DAY',
    scheduledStartTime: schedule.scheduledStartTime,
    scheduledEndTime: schedule.scheduledEndTime
  }
  scheduleDialogVisible.value = true
}

// 班次类型变更
function handleShiftTypeChange(val) {
  if (val === 'DAY') {
    scheduleForm.value.scheduledStartTime = '08:30'
    scheduleForm.value.scheduledEndTime = '17:30'
  } else if (val === 'NIGHT') {
    scheduleForm.value.scheduledStartTime = '20:00'
    scheduleForm.value.scheduledEndTime = '08:00'
  } else {
    scheduleForm.value.scheduledStartTime = null
    scheduleForm.value.scheduledEndTime = null
  }
}

// 提交排班调整
async function submitScheduleAdjust() {
  submitLoading.value = true
  try {
    const res = await adjustSchedule(scheduleForm.value.id, {
      shiftType: scheduleForm.value.shiftType,
      scheduledStartTime: scheduleForm.value.scheduledStartTime,
      scheduledEndTime: scheduleForm.value.scheduledEndTime
    })
    if (res.code === 200) {
      ElMessage.success('排班调整成功')
      scheduleDialogVisible.value = false
      loadScheduleList()
    } else {
      ElMessage.error(res.message || '调整失败')
    }
  } catch (error) {
    console.error('调整排班失败', error)
  } finally {
    submitLoading.value = false
  }
}

// 加载考勤列表
async function loadAttendanceList() {
  attendanceLoading.value = true
  try {
    const params = {
      page: attendancePage.value,
      size: attendanceSize.value,
      status: attendanceFilter.value.status
    }
    if (attendanceDateRange.value && attendanceDateRange.value.length === 2) {
      params.startDate = attendanceDateRange.value[0]
      params.endDate = attendanceDateRange.value[1]
    }
    const res = await getAttendanceList(params)
    if (res.code === 200) {
      attendanceList.value = res.data.records || []
      attendanceTotal.value = res.data.total || 0
    }
  } catch (error) {
    console.error('加载考勤列表失败', error)
  } finally {
    attendanceLoading.value = false
  }
}

// 加载今日汇总
async function loadDailySummary() {
  try {
    const today = new Date().toISOString().split('T')[0]
    const res = await getDailySummary(today)
    if (res.code === 200) {
      dailySummary.value = res.data
      summaryDialogVisible.value = true
    }
  } catch (error) {
    console.error('加载今日汇总失败', error)
  }
}

// 补卡
function handleSupplement(row) {
  supplementForm.value = {
    id: row.id,
    userName: row.userName,
    attendanceDate: row.attendanceDate,
    clockInTime: row.clockInTime,
    clockOutTime: row.clockOutTime,
    reason: ''
  }
  supplementDialogVisible.value = true
}

// 提交补卡
async function submitSupplement() {
  if (!supplementForm.value.clockInTime && !supplementForm.value.clockOutTime) {
    ElMessage.warning('请至少填写一个打卡时间')
    return
  }
  submitLoading.value = true
  try {
    const res = await supplementAttendance(supplementForm.value.id, {
      clockInTime: supplementForm.value.clockInTime,
      clockOutTime: supplementForm.value.clockOutTime,
      reason: supplementForm.value.reason
    })
    if (res.code === 200) {
      ElMessage.success('补卡成功')
      supplementDialogVisible.value = false
      loadAttendanceList()
    } else {
      ElMessage.error(res.message || '补卡失败')
    }
  } catch (error) {
    console.error('补卡失败', error)
  } finally {
    submitLoading.value = false
  }
}

// 加载月度报表
async function loadMonthlyReport() {
  reportLoading.value = true
  try {
    const [year, month] = reportMonth.value.split('-')
    const res = await getMonthlyReport(parseInt(year), parseInt(month))
    if (res.code === 200) {
      reportList.value = res.data || []
    }
  } catch (error) {
    console.error('加载月度报表失败', error)
  } finally {
    reportLoading.value = false
  }
}

// 工具函数
function getRoleName(role) {
  const roleMap = {
    'dispatcher': '调度员',
    'inspector': '巡检员',
    'workshop': '车间',
    'admin': '管理员',
    '调度员': '调度员',
    '巡检员': '巡检员',
    '车间': '车间'
  }
  return roleMap[role] || role
}

function getShiftName(shiftType) {
  if (!shiftType) return '-'
  const shiftMap = {
    'day': '白班', 'DAY': '白班', '白班': '白班',
    'night': '夜班', 'NIGHT': '夜班', '夜班': '夜班',
    'rest': '休息', 'REST': '休息', '休息': '休息'
  }
  return shiftMap[shiftType] || shiftType
}

function getShiftClass(shiftType) {
  if (!shiftType) return ''
  const type = shiftType.toLowerCase()
  if (type === 'day' || type === '白班') return 'shift-day'
  if (type === 'night' || type === '夜班') return 'shift-night'
  if (type === 'rest' || type === '休息') return 'shift-rest'
  return ''
}

function getStatusName(status) {
  if (!status) return '-'
  const statusMap = {
    'normal': '正常', '正常': '正常', 'NORMAL': '正常',
    'late': '迟到', '迟到': '迟到', 'LATE': '迟到',
    'early_leave': '早退', '早退': '早退', 'EARLY_LEAVE': '早退',
    'absent': '缺勤', '缺勤': '缺勤', 'ABSENT': '缺勤',
    'rest': '休息', '休息': '休息', 'REST': '休息',
    'scheduled': '待考勤', 'SCHEDULED': '待考勤',
    'pending': '未打卡', 'PENDING': '未打卡'
  }
  return statusMap[status] || status
}

function getStatusType(status) {
  if (!status) return 'info'
  const type = status.toLowerCase()
  if (type === 'normal' || type === '正常') return 'success'
  if (type === 'late' || type === '迟到') return 'warning'
  if (type === 'early_leave' || type === '早退') return 'warning'
  if (type === 'absent' || type === '缺勤') return 'danger'
  if (type === 'scheduled' || type === 'pending') return 'info'
  return 'info'
}

function formatDay(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.getDate()
}

onMounted(() => {
  loadScheduleList()
  loadAttendanceList()
  loadMonthlyReport()
})
</script>


<style lang="scss" scoped>
.schedule-page {
  padding: 20px;

  .page-header {
    margin-bottom: 20px;
    h2 {
      margin: 0;
      font-size: 20px;
      font-weight: 600;
    }
  }

  .main-tabs {
    background: #fff;
    border-radius: 8px;
    padding: 20px;
  }

  .tab-content {
    .toolbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
      flex-wrap: wrap;
      gap: 10px;

      .left {
        display: flex;
        gap: 10px;
        flex-wrap: wrap;
      }
    }

    .el-pagination {
      margin-top: 16px;
      justify-content: flex-end;
    }
  }

  // 排班单元格样式
  .schedule-cells {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
  }

  .schedule-cell {
    width: 50px;
    padding: 4px;
    border-radius: 4px;
    text-align: center;
    cursor: pointer;
    transition: all 0.2s;
    font-size: 12px;

    &:hover {
      transform: scale(1.05);
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
    }

    .date {
      font-weight: 600;
    }

    .shift {
      font-size: 10px;
      margin-top: 2px;
    }

    &.shift-day {
      background: #e6f7ff;
      border: 1px solid #91d5ff;
      color: #1890ff;
    }

    &.shift-night {
      background: #f9f0ff;
      border: 1px solid #d3adf7;
      color: #722ed1;
    }

    &.shift-rest {
      background: #f5f5f5;
      border: 1px solid #d9d9d9;
      color: #999;
    }
  }

  // 汇总内容样式
  .summary-content {
    .summary-item {
      display: flex;
      justify-content: space-between;
      padding: 12px 0;
      border-bottom: 1px solid #f0f0f0;

      &:last-child {
        border-bottom: none;
      }

      .label {
        color: #666;
      }

      .value {
        font-weight: 600;

        &.text-success {
          color: #52c41a;
        }

        &.text-warning {
          color: #faad14;
        }

        &.text-danger {
          color: #ff4d4f;
        }
      }
    }
  }

  .text-danger {
    color: #ff4d4f;
  }
}
</style>
