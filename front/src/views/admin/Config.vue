<template>
  <div class="admin-config">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2><i class="bi bi-gear me-2"></i>系统配置</h2>
      <p class="text-muted">管理电价参数、预警阈值和时段配置</p>
    </div>

    <el-form
      ref="configFormRef"
      :model="configForm"
      :rules="configRules"
      label-width="140px"
      v-loading="loading"
    >
      <!-- 电价参数配置 -->
      <div class="config-section">
        <div class="section-header">
          <i class="bi bi-currency-yen"></i>
          <span>电价参数配置</span>
        </div>
        <div class="section-content">
          <el-row :gutter="24">
            <el-col :span="8">
              <el-form-item label="峰时电价" prop="peakPrice">
                <el-input-number
                  v-model="configForm.peakPrice"
                  :precision="2"
                  :step="0.1"
                  :min="0"
                  :max="10"
                  controls-position="right"
                  style="width: 100%"
                />
                <span class="unit">元/kWh</span>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="平时电价" prop="normalPrice">
                <el-input-number
                  v-model="configForm.normalPrice"
                  :precision="2"
                  :step="0.1"
                  :min="0"
                  :max="10"
                  controls-position="right"
                  style="width: 100%"
                />
                <span class="unit">元/kWh</span>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="谷时电价" prop="valleyPrice">
                <el-input-number
                  v-model="configForm.valleyPrice"
                  :precision="2"
                  :step="0.1"
                  :min="0"
                  :max="10"
                  controls-position="right"
                  style="width: 100%"
                />
                <span class="unit">元/kWh</span>
              </el-form-item>
            </el-col>
          </el-row>
          <div class="price-preview">
            <div class="preview-item peak">
              <span class="label">峰时</span>
              <span class="value">¥{{ configForm.peakPrice?.toFixed(2) || '0.00' }}</span>
            </div>
            <div class="preview-item normal">
              <span class="label">平时</span>
              <span class="value">¥{{ configForm.normalPrice?.toFixed(2) || '0.00' }}</span>
            </div>
            <div class="preview-item valley">
              <span class="label">谷时</span>
              <span class="value">¥{{ configForm.valleyPrice?.toFixed(2) || '0.00' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 预警阈值配置 -->
      <div class="config-section">
        <div class="section-header">
          <i class="bi bi-exclamation-triangle"></i>
          <span>预警阈值配置</span>
        </div>
        <div class="section-content">
          <el-row :gutter="24">
            <el-col :span="8">
              <el-form-item label="功率超限阈值" prop="powerOverloadThreshold">
                <el-input-number
                  v-model="configForm.powerOverloadThreshold"
                  :min="50"
                  :max="150"
                  :step="5"
                  controls-position="right"
                  style="width: 100%"
                />
                <span class="unit">%</span>
              </el-form-item>
              <p class="field-hint">超过此比例触发超限预警</p>
            </el-col>
            <el-col :span="8">
              <el-form-item label="功率预警阈值" prop="powerWarningThreshold">
                <el-input-number
                  v-model="configForm.powerWarningThreshold"
                  :min="50"
                  :max="100"
                  :step="5"
                  controls-position="right"
                  style="width: 100%"
                />
                <span class="unit">%</span>
              </el-form-item>
              <p class="field-hint">超过此比例触发接近限额预警</p>
            </el-col>
            <el-col :span="8">
              <el-form-item label="配额预警阈值" prop="quotaWarningThreshold">
                <el-input-number
                  v-model="configForm.quotaWarningThreshold"
                  :min="50"
                  :max="100"
                  :step="5"
                  controls-position="right"
                  style="width: 100%"
                />
                <span class="unit">%</span>
              </el-form-item>
              <p class="field-hint">已用配额超过此比例触发预警</p>
            </el-col>
          </el-row>
        </div>
      </div>

      <!-- 时段配置 -->
      <div class="config-section">
        <div class="section-header">
          <i class="bi bi-clock"></i>
          <span>时段配置</span>
        </div>
        <div class="section-content">
          <el-row :gutter="24">
            <el-col :span="12">
              <div class="time-period-card peak">
                <div class="period-title">
                  <i class="bi bi-sun"></i> 峰时时段 1
                </div>
                <div class="period-inputs">
                  <el-form-item label="开始时间" prop="peakStartHour1">
                    <el-select v-model="configForm.peakStartHour1" style="width: 100%">
                      <el-option
                        v-for="h in 24"
                        :key="h - 1"
                        :label="`${(h - 1).toString().padStart(2, '0')}:00`"
                        :value="h - 1"
                      />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="结束时间" prop="peakEndHour1">
                    <el-select v-model="configForm.peakEndHour1" style="width: 100%">
                      <el-option
                        v-for="h in 24"
                        :key="h"
                        :label="`${h.toString().padStart(2, '0')}:00`"
                        :value="h"
                      />
                    </el-select>
                  </el-form-item>
                </div>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="time-period-card peak">
                <div class="period-title">
                  <i class="bi bi-sunset"></i> 峰时时段 2
                </div>
                <div class="period-inputs">
                  <el-form-item label="开始时间" prop="peakStartHour2">
                    <el-select v-model="configForm.peakStartHour2" style="width: 100%">
                      <el-option
                        v-for="h in 24"
                        :key="h - 1"
                        :label="`${(h - 1).toString().padStart(2, '0')}:00`"
                        :value="h - 1"
                      />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="结束时间" prop="peakEndHour2">
                    <el-select v-model="configForm.peakEndHour2" style="width: 100%">
                      <el-option
                        v-for="h in 24"
                        :key="h"
                        :label="`${h.toString().padStart(2, '0')}:00`"
                        :value="h"
                      />
                    </el-select>
                  </el-form-item>
                </div>
              </div>
            </el-col>
          </el-row>
          <el-row :gutter="24" style="margin-top: 16px">
            <el-col :span="12">
              <div class="time-period-card normal">
                <div class="period-title">
                  <i class="bi bi-brightness-high"></i> 平时时段
                </div>
                <div class="period-inputs">
                  <el-form-item label="开始时间" prop="normalStartHour">
                    <el-select v-model="configForm.normalStartHour" style="width: 100%">
                      <el-option
                        v-for="h in 24"
                        :key="h - 1"
                        :label="`${(h - 1).toString().padStart(2, '0')}:00`"
                        :value="h - 1"
                      />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="结束时间" prop="normalEndHour">
                    <el-select v-model="configForm.normalEndHour" style="width: 100%">
                      <el-option
                        v-for="h in 24"
                        :key="h"
                        :label="`${h.toString().padStart(2, '0')}:00`"
                        :value="h"
                      />
                    </el-select>
                  </el-form-item>
                </div>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="time-period-card valley">
                <div class="period-title">
                  <i class="bi bi-moon-stars"></i> 谷时时段
                </div>
                <div class="period-info">
                  <p>谷时为峰时和平时以外的时段</p>
                  <p class="valley-hours">{{ valleyHoursText }}</p>
                </div>
              </div>
            </el-col>
          </el-row>

          <!-- 时段可视化 -->
          <div class="time-visualization">
            <div class="viz-title">24小时时段分布</div>
            <div class="viz-bar">
              <div
                v-for="h in 24"
                :key="h - 1"
                class="hour-block"
                :class="getHourType(h - 1)"
                :title="`${(h - 1).toString().padStart(2, '0')}:00 - ${getPeriodName(h - 1)}`"
              >
                {{ h - 1 }}
              </div>
            </div>
            <div class="viz-legend">
              <span class="legend-item peak"><i></i>峰时</span>
              <span class="legend-item normal"><i></i>平时</span>
              <span class="legend-item valley"><i></i>谷时</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="form-actions">
        <el-button @click="resetForm">
          <i class="bi bi-arrow-counterclockwise me-1"></i>重置
        </el-button>
        <el-button type="primary" @click="saveConfig" :loading="saving">
          <i class="bi bi-check-lg me-1"></i>保存配置
        </el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSystemConfig, updateSystemConfig } from '@/api/admin'

// 加载状态
const loading = ref(false)
const saving = ref(false)
const configFormRef = ref(null)

// 配置表单
const configForm = reactive({
  // 电价参数
  peakPrice: 1.2,
  normalPrice: 0.8,
  valleyPrice: 0.4,
  // 预警阈值
  powerOverloadThreshold: 100,
  powerWarningThreshold: 85,
  quotaWarningThreshold: 80,
  // 时段配置
  peakStartHour1: 8,
  peakEndHour1: 12,
  peakStartHour2: 18,
  peakEndHour2: 22,
  normalStartHour: 12,
  normalEndHour: 18
})

// 表单验证规则
const configRules = {
  peakPrice: [{ required: true, message: '请输入峰时电价', trigger: 'blur' }],
  normalPrice: [{ required: true, message: '请输入平时电价', trigger: 'blur' }],
  valleyPrice: [{ required: true, message: '请输入谷时电价', trigger: 'blur' }],
  powerOverloadThreshold: [{ required: true, message: '请输入功率超限阈值', trigger: 'blur' }],
  powerWarningThreshold: [{ required: true, message: '请输入功率预警阈值', trigger: 'blur' }],
  quotaWarningThreshold: [{ required: true, message: '请输入配额预警阈值', trigger: 'blur' }]
}

// 计算谷时时段文字
const valleyHoursText = computed(() => {
  const hours = []
  for (let h = 0; h < 24; h++) {
    if (getHourType(h) === 'valley') {
      hours.push(h)
    }
  }
  if (hours.length === 0) return '无'
  
  // 合并连续时段
  const ranges = []
  let start = hours[0]
  let end = hours[0]
  
  for (let i = 1; i < hours.length; i++) {
    if (hours[i] === end + 1) {
      end = hours[i]
    } else {
      ranges.push(`${start.toString().padStart(2, '0')}:00-${(end + 1).toString().padStart(2, '0')}:00`)
      start = hours[i]
      end = hours[i]
    }
  }
  ranges.push(`${start.toString().padStart(2, '0')}:00-${((end + 1) % 24).toString().padStart(2, '0')}:00`)
  
  return ranges.join('、')
})

// 判断小时属于哪个时段
function getHourType(hour) {
  // 峰时1
  if (hour >= configForm.peakStartHour1 && hour < configForm.peakEndHour1) {
    return 'peak'
  }
  // 峰时2
  if (hour >= configForm.peakStartHour2 && hour < configForm.peakEndHour2) {
    return 'peak'
  }
  // 平时
  if (hour >= configForm.normalStartHour && hour < configForm.normalEndHour) {
    return 'normal'
  }
  // 谷时
  return 'valley'
}

// 获取时段名称
function getPeriodName(hour) {
  const type = getHourType(hour)
  const names = { peak: '峰时', normal: '平时', valley: '谷时' }
  return names[type]
}

// 加载配置
async function loadConfig() {
  loading.value = true
  try {
    const res = await getSystemConfig()
    if (res.code === 200 && res.data) {
      Object.assign(configForm, res.data)
    }
  } catch (e) {
    console.error('加载配置失败', e)
    ElMessage.error('加载配置失败')
  } finally {
    loading.value = false
  }
}

// 保存配置
async function saveConfig() {
  if (!configFormRef.value) return
  
  await configFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    saving.value = true
    try {
      const res = await updateSystemConfig(configForm)
      if (res.code === 200) {
        ElMessage.success('配置保存成功')
      }
    } catch (e) {
      console.error('保存配置失败', e)
      ElMessage.error('保存配置失败')
    } finally {
      saving.value = false
    }
  })
}

// 重置表单
function resetForm() {
  loadConfig()
  ElMessage.info('已重置为服务器配置')
}

// 初始化
onMounted(() => {
  loadConfig()
})
</script>

<style lang="scss" scoped>
.admin-config {
  color: rgba(255, 255, 255, 0.9);

  .page-header {
    margin-bottom: 24px;
    
    h2 {
      margin: 0 0 8px 0;
      font-size: 24px;
      font-weight: 600;
      color: rgba(255, 255, 255, 0.95);
    }
    
    .text-muted {
      color: rgba(255, 255, 255, 0.6);
      margin: 0;
    }
  }

  .config-section {
    background: #1a1a2e;
    border-radius: 12px;
    margin-bottom: 24px;
    overflow: hidden;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);

    .section-header {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 16px 20px;
      background: linear-gradient(135deg, #16213e 0%, #1a1a2e 100%);
      border-bottom: 1px solid #2a2a4a;
      font-size: 16px;
      font-weight: 600;
      color: rgba(255, 255, 255, 0.95);

      i {
        font-size: 18px;
        color: #409eff;
      }
    }

    .section-content {
      padding: 24px;
    }
  }

  .unit {
    margin-left: 8px;
    color: rgba(255, 255, 255, 0.6);
    font-size: 14px;
  }

  .field-hint {
    margin: 4px 0 0 140px;
    font-size: 12px;
    color: rgba(255, 255, 255, 0.5);
  }

  .price-preview {
    display: flex;
    gap: 24px;
    margin-top: 20px;
    padding: 16px;
    background: #16213e;
    border-radius: 8px;

    .preview-item {
      flex: 1;
      text-align: center;
      padding: 16px;
      border-radius: 8px;
      
      .label {
        display: block;
        font-size: 14px;
        color: rgba(255, 255, 255, 0.7);
        margin-bottom: 8px;
      }
      
      .value {
        display: block;
        font-size: 28px;
        font-weight: 700;
      }

      &.peak {
        background: rgba(245, 108, 108, 0.15);
        .value { color: #f56c6c; }
      }
      
      &.normal {
        background: rgba(230, 162, 60, 0.15);
        .value { color: #e6a23c; }
      }
      
      &.valley {
        background: rgba(103, 194, 58, 0.15);
        .value { color: #67c23a; }
      }
    }
  }

  .time-period-card {
    padding: 20px;
    border-radius: 8px;
    border: 1px solid #2a2a4a;

    .period-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 15px;
      font-weight: 600;
      margin-bottom: 16px;
      
      i {
        font-size: 18px;
      }
    }

    .period-inputs {
      display: flex;
      gap: 16px;
      
      .el-form-item {
        flex: 1;
        margin-bottom: 0;
      }
    }

    .period-info {
      p {
        margin: 0;
        color: rgba(255, 255, 255, 0.7);
        font-size: 14px;
        
        &.valley-hours {
          margin-top: 8px;
          font-size: 16px;
          font-weight: 600;
          color: #67c23a;
        }
      }
    }

    &.peak {
      background: rgba(245, 108, 108, 0.08);
      border-color: rgba(245, 108, 108, 0.3);
      
      .period-title {
        color: #f56c6c;
      }
    }

    &.normal {
      background: rgba(230, 162, 60, 0.08);
      border-color: rgba(230, 162, 60, 0.3);
      
      .period-title {
        color: #e6a23c;
      }
    }

    &.valley {
      background: rgba(103, 194, 58, 0.08);
      border-color: rgba(103, 194, 58, 0.3);
      
      .period-title {
        color: #67c23a;
      }
    }
  }

  .time-visualization {
    margin-top: 24px;
    padding: 20px;
    background: #16213e;
    border-radius: 8px;

    .viz-title {
      font-size: 14px;
      font-weight: 600;
      margin-bottom: 12px;
      color: rgba(255, 255, 255, 0.85);
    }

    .viz-bar {
      display: flex;
      border-radius: 6px;
      overflow: hidden;

      .hour-block {
        flex: 1;
        height: 40px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 11px;
        font-weight: 500;
        color: rgba(255, 255, 255, 0.9);
        cursor: pointer;
        transition: transform 0.2s;

        &:hover {
          transform: scaleY(1.1);
        }

        &.peak {
          background: linear-gradient(180deg, #f56c6c 0%, #c45656 100%);
        }

        &.normal {
          background: linear-gradient(180deg, #e6a23c 0%, #b88230 100%);
        }

        &.valley {
          background: linear-gradient(180deg, #67c23a 0%, #529b2e 100%);
        }
      }
    }

    .viz-legend {
      display: flex;
      justify-content: center;
      gap: 24px;
      margin-top: 12px;

      .legend-item {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 13px;
        color: rgba(255, 255, 255, 0.8);

        i {
          display: inline-block;
          width: 14px;
          height: 14px;
          border-radius: 3px;
        }

        &.peak i { background: #f56c6c; }
        &.normal i { background: #e6a23c; }
        &.valley i { background: #67c23a; }
      }
    }
  }

  .form-actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    padding: 20px;
    background: #1a1a2e;
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  }

  // Element Plus 样式覆盖
  :deep(.el-form-item__label) {
    color: rgba(255, 255, 255, 0.85);
  }

  :deep(.el-input__wrapper),
  :deep(.el-input-number) {
    background-color: #16213e;
    box-shadow: 0 0 0 1px #2a2a4a inset;
  }

  :deep(.el-input-number .el-input__wrapper) {
    background-color: transparent;
    box-shadow: none;
  }

  :deep(.el-select .el-input__wrapper) {
    background-color: #16213e;
  }

  :deep(.el-input-number__decrease),
  :deep(.el-input-number__increase) {
    background-color: #16213e;
    border-color: #2a2a4a;
    color: rgba(255, 255, 255, 0.7);

    &:hover {
      color: #409eff;
    }
  }
}
</style>
