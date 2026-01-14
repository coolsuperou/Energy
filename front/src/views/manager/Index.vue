<template>
  <div class="manager-home">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon blue"><i class="bi bi-lightning-charge"></i></div>
        <div class="stat-info">
          <div class="stat-value">45,680</div>
          <div class="stat-label">本月总能耗 (kWh)</div>
          <div class="stat-trend up"><i class="bi bi-arrow-down"></i> 较上月 -3.2%</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green"><i class="bi bi-currency-yen"></i></div>
        <div class="stat-info">
          <div class="stat-value">¥28,560</div>
          <div class="stat-label">本月电费</div>
          <div class="stat-trend up"><i class="bi bi-arrow-down"></i> 较上月 -5.1%</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange"><i class="bi bi-graph-up"></i></div>
        <div class="stat-info">
          <div class="stat-value">92.5%</div>
          <div class="stat-label">配额使用率</div>
          <div class="stat-trend muted">剩余 7.5%</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple"><i class="bi bi-building"></i></div>
        <div class="stat-info">
          <div class="stat-value">4</div>
          <div class="stat-label">管辖车间</div>
          <div class="stat-trend muted">18台设备</div>
        </div>
      </div>
    </div>

    <div class="content-grid">
      <!-- 能耗趋势 -->
      <div class="card chart-card">
        <div class="card-header">
          <span>能耗趋势分析</span>
          <div class="chart-tabs">
            <button class="tab active">日</button>
            <button class="tab">周</button>
            <button class="tab">月</button>
          </div>
        </div>
        <div class="card-body">
          <div class="chart-placeholder">
            <i class="bi bi-bar-chart-line"></i>
            <span>能耗趋势图表</span>
          </div>
        </div>
      </div>

      <!-- 车间排名 -->
      <div class="card">
        <div class="card-header">车间能耗排名</div>
        <div class="card-body">
          <div class="ranking-list">
            <div class="ranking-item">
              <span class="ranking-badge gold">1</span>
              <span class="ranking-name">第三车间</span>
              <span class="ranking-value">12,500 kWh</span>
            </div>
            <div class="ranking-item">
              <span class="ranking-badge silver">2</span>
              <span class="ranking-name">第四车间</span>
              <span class="ranking-value">11,200 kWh</span>
            </div>
            <div class="ranking-item">
              <span class="ranking-badge bronze">3</span>
              <span class="ranking-name">第一车间</span>
              <span class="ranking-value">10,800 kWh</span>
            </div>
            <div class="ranking-item">
              <span class="ranking-badge normal">4</span>
              <span class="ranking-name">第二车间</span>
              <span class="ranking-value">9,180 kWh</span>
            </div>
          </div>
        </div>
      </div>

      <!-- AI 建议 -->
      <div class="card ai-card">
        <div class="card-header">
          <span><i class="bi bi-robot me-2"></i>AI 节能建议</span>
          <router-link to="/manager/ai" class="card-link">查看详情 →</router-link>
        </div>
        <div class="card-body">
          <div class="ai-suggestion">
            <i class="bi bi-lightbulb"></i>
            <div class="suggestion-content">
              <h6>优化第三车间用电时段</h6>
              <p>建议将部分生产任务调整至谷时段，预计可节省电费 ¥2,500/月</p>
            </div>
          </div>
          <div class="ai-suggestion">
            <i class="bi bi-gear"></i>
            <div class="suggestion-content">
              <h6>设备能效优化</h6>
              <p>数控机床-04 能效偏低，建议进行维护保养</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 配额使用 -->
      <div class="card">
        <div class="card-header">配额使用情况</div>
        <div class="card-body">
          <div class="quota-list">
            <div class="quota-item">
              <div class="quota-header">
                <span>第一车间</span>
                <span>12,500 / 50,000 kWh</span>
              </div>
              <div class="progress-wrapper">
                <div class="progress-bar" style="width: 25%"></div>
              </div>
            </div>
            <div class="quota-item">
              <div class="quota-header">
                <span>第二车间</span>
                <span>10,800 / 45,000 kWh</span>
              </div>
              <div class="progress-wrapper">
                <div class="progress-bar" style="width: 24%"></div>
              </div>
            </div>
            <div class="quota-item">
              <div class="quota-header">
                <span>第三车间</span>
                <span>18,000 / 60,000 kWh</span>
              </div>
              <div class="progress-wrapper">
                <div class="progress-bar warning" style="width: 30%"></div>
              </div>
            </div>
            <div class="quota-item">
              <div class="quota-header">
                <span>第四车间</span>
                <span>16,500 / 55,000 kWh</span>
              </div>
              <div class="progress-wrapper">
                <div class="progress-bar" style="width: 30%"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
</script>

<style lang="scss">
@import '@/styles/manager.scss';
</style>
