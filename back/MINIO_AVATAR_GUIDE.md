# MinIO 头像上传功能使用指南

## 📋 功能概述

实现了基于 MinIO 对象存储的用户头像上传功能,支持:
- 头像上传(最大2MB)
- 自动删除旧头像
- 图片格式验证
- 完整的访问URL返回

---

## 🚀 部署步骤

### 1. 启动 MinIO 服务

确保 MinIO 已经在运行:
```
MinIO API: http://127.0.0.1:9090
MinIO WebUI: http://127.0.0.1:9000
用户名: minioadmin
密码: minioadmin
```

### 2. 更新数据库表结构

在 MySQL 中执行以下 SQL:
```bash
mysql -u root -p < src/main/resources/add_avatar_column.sql
```

或者手动执行:
```sql
USE electric_energy_platform;
ALTER TABLE users ADD COLUMN avatar_url VARCHAR(255) COMMENT '头像URL' AFTER email;
```

### 3. 编译项目

```bash
cd ElectricEnergyManagement/back
mvn clean compile
```

### 4. 启动 Spring Boot 应用

```bash
mvn spring-boot:run
```

---

## 📡 API 接口说明

### 1. 上传头像

**接口**: `POST /api/users/current/avatar`

**请求方式**: `multipart/form-data`

**请求参数**:
- `file`: 图片文件(必填)

**请求示例** (使用 curl):
```bash
curl -X POST http://localhost:8080/api/users/current/avatar \
  -H "Cookie: JSESSIONID=你的SessionID" \
  -F "file=@/path/to/avatar.jpg"
```

**响应示例**:
```json
{
  "code": 200,
  "message": "头像上传成功",
  "data": {
    "avatarUrl": "http://127.0.0.1:9000/avatars/123_1707456789123.jpg"
  }
}
```

**错误响应**:
```json
{
  "code": 400,
  "message": "只能上传图片文件",
  "data": null
}
```

### 2. 获取头像URL

**接口**: `GET /api/users/current/avatar`

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "avatarUrl": "http://127.0.0.1:9000/avatars/123_1707456789123.jpg"
  }
}
```

---

## 🎨 前端集成示例

### Vue 3 示例

```vue
<template>
  <div class="avatar-upload">
    <img v-if="avatarUrl" :src="avatarUrl" alt="头像" class="avatar-preview" />
    <input type="file" @change="handleFileChange" accept="image/*" />
    <button @click="uploadAvatar">上传头像</button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'

const avatarUrl = ref('')
const selectedFile = ref(null)

// 选择文件
const handleFileChange = (event) => {
  selectedFile.value = event.target.files[0]
}

// 上传头像
const uploadAvatar = async () => {
  if (!selectedFile.value) {
    alert('请选择文件')
    return
  }

  const formData = new FormData()
  formData.append('file', selectedFile.value)

  try {
    const response = await axios.post('/api/users/current/avatar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    
    avatarUrl.value = response.data.data.avatarUrl
    alert('上传成功!')
  } catch (error) {
    alert('上传失败: ' + error.response.data.message)
  }
}

// 获取当前头像
const loadAvatar = async () => {
  try {
    const response = await axios.get('/api/users/current/avatar')
    avatarUrl.value = response.data.data.avatarUrl
  } catch (error) {
    console.error('获取头像失败', error)
  }
}

// 页面加载时获取头像
loadAvatar()
</script>

<style scoped>
.avatar-preview {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
}
</style>
```

---

## 🔧 配置说明

### application.yml 配置

```yaml
# MinIO 配置
minio:
  endpoint: http://127.0.0.1:9090  # API端口
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: avatars

# 文件上传限制
spring:
  servlet:
    multipart:
      max-file-size: 2MB
      max-request-size: 2MB
```

---

## 📝 核心代码说明

### 1. MinioService.java

**核心方法**:
- `uploadFile()`: 上传文件到 MinIO
- `deleteFile()`: 删除文件
- `getFileUrl()`: 获取文件访问URL
- `ensureBucketExists()`: 确保存储桶存在

**文件命名规则**: `用户ID_时间戳.扩展名`
例如: `123_1707456789123.jpg`

### 2. UserController.java

**新增接口**:
- `POST /users/current/avatar`: 上传头像
- `GET /users/current/avatar`: 获取头像URL

**验证规则**:
- 文件不能为空
- 必须是图片类型 (`image/*`)
- 文件大小 ≤ 2MB

---

## 🐛 常见问题

### 1. 上传失败: "Connection refused"

**原因**: MinIO 服务未启动

**解决**: 启动 MinIO 服务

### 2. 图片无法访问

**原因**: Bucket 权限未设置为公开

**解决**: MinioService 会自动设置 Bucket 为公开访问,如果还是不行,手动在 MinIO WebUI 中设置

### 3. 文件大小超限

**原因**: 文件超过 2MB

**解决**: 
- 前端压缩图片
- 或修改 `application.yml` 中的 `max-file-size`

### 4. 端口冲突

**MinIO 使用两个端口**:
- 9090: API 端口(程序连接用)
- 9000: WebUI 端口(浏览器访问用)

确保这两个端口都没有被占用。

---

## 🎯 测试步骤

### 1. 使用 Postman 测试

1. 先登录获取 Session
2. 选择 POST 请求: `http://localhost:8080/api/users/current/avatar`
3. Body 选择 `form-data`
4. 添加 key: `file`, type: `File`, 选择图片
5. 发送请求

### 2. 验证结果

1. 检查响应中的 `avatarUrl`
2. 在浏览器中打开该 URL,应该能看到图片
3. 在 MinIO WebUI (http://127.0.0.1:9000) 中查看 `avatars` bucket,应该能看到上传的文件

---

## 📚 扩展功能建议

1. **图片裁剪**: 前端集成图片裁剪库(如 Cropper.js)
2. **缩略图**: 上传时生成多个尺寸的缩略图
3. **CDN 加速**: 配置 CDN 加速图片访问
4. **水印**: 自动添加水印
5. **格式转换**: 统一转换为 WebP 格式

---

## 👨‍💻 作者

欧展煌

创建日期: 2026-02-09
