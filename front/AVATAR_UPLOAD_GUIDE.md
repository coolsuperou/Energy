# 前端头像上传功能使用指南

## 📋 功能说明

在个人中心页面 (`Profile.vue`) 实现了头像上传功能:

### 功能特点
- ✅ 鼠标悬停显示上传按钮
- ✅ 支持图片预览
- ✅ 文件类型验证(只允许图片)
- ✅ 文件大小限制(最大2MB)
- ✅ 上传进度提示
- ✅ 自动刷新头像显示

---

## 🎨 使用方式

### 1. 上传头像

1. 进入个人中心页面
2. 将鼠标移到头像上,会显示相机图标
3. 点击头像区域
4. 选择图片文件(支持 jpg, png, gif 等)
5. 等待上传完成
6. 头像自动更新

### 2. 更换头像

重复上述步骤即可,旧头像会自动删除。

---

## 🔧 技术实现

### 核心文件

1. **Profile.vue** - 个人中心页面
   - 头像显示组件
   - 文件选择器
   - 上传逻辑

2. **user.js** - API 接口
   - `uploadAvatar()` - 上传头像
   - `getAvatar()` - 获取头像URL

### 关键代码

#### 模板部分
```vue
<!-- 头像容器 -->
<div class="position-relative avatar-container">
  <!-- 头像显示 -->
  <div v-if="userInfo.avatarUrl" class="rounded-circle">
    <img :src="userInfo.avatarUrl" alt="头像" />
  </div>
  
  <!-- 上传按钮悬浮层 -->
  <div class="avatar-upload-overlay" @click="triggerFileInput">
    <i class="bi bi-camera-fill"></i>
  </div>
  
  <!-- 隐藏的文件选择器 -->
  <input ref="fileInputRef" type="file" accept="image/*" @change="handleFileSelect" />
</div>
```

#### 上传逻辑
```javascript
// 触发文件选择
function triggerFileInput() {
  fileInputRef.value?.click()
}

// 处理文件选择
function handleFileSelect(event) {
  const file = event.target.files[0]
  
  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    ElMessage.error('只能上传图片文件')
    return
  }
  
  // 验证文件大小
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过2MB')
    return
  }
  
  // 上传
  uploadAvatarFile(file)
}

// 上传头像
async function uploadAvatarFile(file) {
  const res = await uploadAvatar(file)
  if (res && res.code === 200) {
    userInfo.value.avatarUrl = res.data.avatarUrl
    ElMessage.success('头像上传成功')
  }
}
```

---

## 🎯 样式说明

### 头像悬停效果

```css
.avatar-container {
  position: relative;
  cursor: pointer;
}

.avatar-upload-overlay {
  position: absolute;
  background: rgba(0, 0, 0, 0.5);
  opacity: 0;
  transition: opacity 0.3s;
}

.avatar-container:hover .avatar-upload-overlay {
  opacity: 1;
}
```

鼠标悬停时显示半透明黑色遮罩和相机图标。

---

## 📡 API 接口

### 上传头像
```javascript
POST /api/users/current/avatar
Content-Type: multipart/form-data

参数:
- file: 图片文件

响应:
{
  "code": 200,
  "message": "头像上传成功",
  "data": {
    "avatarUrl": "http://127.0.0.1:9000/avatars/123_1707456789123.jpg"
  }
}
```

### 获取头像
```javascript
GET /api/users/current/avatar

响应:
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "avatarUrl": "http://127.0.0.1:9000/avatars/123_1707456789123.jpg"
  }
}
```

---

## 🐛 常见问题

### 1. 头像不显示

**原因**: MinIO 服务未启动或 URL 不正确

**解决**: 
- 确保 MinIO 服务运行在 http://127.0.0.1:9000
- 检查浏览器控制台是否有跨域错误

### 2. 上传失败

**原因**: 
- 文件太大(>2MB)
- 文件格式不支持
- 后端服务未启动

**解决**:
- 压缩图片
- 使用 jpg/png 格式
- 检查后端服务状态

### 3. 上传后头像不更新

**原因**: 浏览器缓存

**解决**: 
- 刷新页面(F5)
- 清除浏览器缓存

---

## 🚀 扩展功能

可以考虑添加:

1. **图片裁剪**: 使用 `vue-cropper` 库
2. **拖拽上传**: 支持拖拽图片到头像区域
3. **预览功能**: 上传前预览图片
4. **进度条**: 显示上传进度百分比
5. **默认头像**: 提供多个默认头像选择

---

## 📝 测试步骤

1. 启动后端服务
2. 启动 MinIO 服务
3. 启动前端开发服务器: `npm run dev`
4. 登录系统
5. 进入个人中心
6. 测试上传头像功能

---

## 👨‍💻 作者

欧展煌

创建日期: 2026-02-09
