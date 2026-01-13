# 智能电能监控平台 - 前端

基于 Vue 3 + Element Plus + ECharts + Axios 构建的多角色电能监控平台前端。

## 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **Vue Router** - 官方路由管理
- **Pinia** - 状态管理
- **Element Plus** - UI 组件库
- **ECharts** - 图表库
- **Axios** - HTTP 请求库
- **Vite** - 构建工具
- **Sass** - CSS 预处理器

## 项目结构

```
src/
├── api/          # API 请求
├── layouts/      # 布局组件
├── router/       # 路由配置
├── stores/       # Pinia 状态管理
├── styles/       # 全局样式
├── views/        # 页面组件
│   ├── admin/      # 管理员页面
│   ├── common/     # 公共页面
│   ├── dispatcher/ # 调度员页面
│   ├── inspector/  # 巡检员页面
│   ├── manager/    # 经理页面
│   └── workshop/   # 车间用户页面
├── App.vue
└── main.js
```

## 开发

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build
```

## 角色路由

| 角色 | 路由前缀 | 功能 |
|------|----------|------|
| 车间用户 | /workshop | 用电申请、能耗查看 |
| 能源调度员 | /dispatcher | 申请审批、任务管理 |
| 设备巡检员 | /inspector | 工作台、设备管理 |
| 能源经理 | /manager | 数据分析、AI分析 |
| 系统管理员 | /admin | 用户管理、系统配置 |

## 后端接口

开发环境通过 Vite 代理转发到 `http://localhost:8080`，所有 `/api` 开头的请求会自动代理。
