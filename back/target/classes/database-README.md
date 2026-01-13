# 数据库初始化说明

## 文件说明

- **schema.sql**: 数据库表结构定义文件
- **data.sql**: 测试数据初始化文件

## 使用方法

### 方法一：使用 MySQL 命令行

```bash
# 1. 登录 MySQL
mysql -u root -p

# 2. 执行表结构创建
source /path/to/schema.sql

# 3. 执行测试数据初始化
source /path/to/data.sql
```

### 方法二：使用 MySQL Workbench

1. 打开 MySQL Workbench
2. 连接到数据库服务器
3. 打开 `schema.sql` 文件并执行
4. 打开 `data.sql` 文件并执行

### 方法三：Spring Boot 自动初始化

在 `application.properties` 或 `application.yml` 中配置：

```properties
# application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/electric_energy_platform?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# 自动执行 SQL 脚本（仅开发环境使用）
spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:schema.sql
spring.sql.init.data-locations=classpath:data.sql
```

或使用 YAML 格式：

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/electric_energy_platform?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
      data-locations: classpath:data.sql
```

## 数据库结构

### 核心表

1. **users** - 用户表（5个角色）
2. **equipments** - 设备表
3. **applications** - 用电申请表
4. **energy_data** - 能耗数据表
5. **tasks** - 巡检任务表
6. **notifications** - 消息通知表
7. **quotas** - 配额表
8. **comments** - 评论表
9. **operation_logs** - 操作日志表

## 测试账号

所有测试账号的密码均为：**123456**

| 用户名 | 角色 | 姓名 | 部门 |
|--------|------|------|------|
| admin | 系统管理员 | 张管理 | 信息技术部 |
| dispatcher01 | 能源调度员 | 李调度 | 能源管理部 |
| dispatcher02 | 能源调度员 | 王调度 | 能源管理部 |
| inspector01 | 设备巡检员 | 赵巡检 | 设备维护部 |
| inspector02 | 设备巡检员 | 钱巡检 | 设备维护部 |
| inspector03 | 设备巡检员 | 孙巡检 | 设备维护部 |
| manager01 | 能源经理 | 周经理 | 能源管理部 |
| workshop01 | 车间用户 | 吴车间 | 第一车间 |
| workshop02 | 车间用户 | 郑车间 | 第二车间 |
| workshop03 | 车间用户 | 冯车间 | 第三车间 |
| workshop04 | 车间用户 | 陈车间 | 第四车间 |

## 测试数据说明

### 设备数据
- 第一车间：5台设备（数控机床、激光切割机、焊接机器人、冲压机）
- 第二车间：5台设备（数控机床、激光切割机、磨床、钻床）
- 第三车间：4台设备（装配生产线、喷涂设备、烘干设备、检测设备）
- 第四车间：4台设备（锻压机、热处理炉、抛光机、清洗设备）

### 车间配额
- 第一车间：50,000 kWh/月（已用 12,500 kWh）
- 第二车间：45,000 kWh/月（已用 10,800 kWh）
- 第三车间：60,000 kWh/月（已用 18,000 kWh）
- 第四车间：55,000 kWh/月（已用 16,500 kWh）

### 用电申请
- 3个已批准的申请（今日生效）
- 2个待审批的申请（明日）

### 巡检任务
- 4个任务（2个待处理、1个进行中、0个已完成）

## 注意事项

1. **生产环境警告**：`data.sql` 包含 `TRUNCATE` 语句，会清空所有数据，仅用于开发测试环境
2. **密码安全**：测试账号使用统一密码，生产环境必须修改
3. **外键约束**：表之间有外键关联，删除数据时注意顺序
4. **字符集**：使用 utf8mb4 字符集，支持中文和 emoji
5. **时区设置**：建议在连接字符串中指定 `serverTimezone=Asia/Shanghai`

## 数据库设计原则

1. **用户申请驱动**：所有能耗数据由用户的用电申请产生
2. **多角色协作**：支持管理员、调度员、巡检员、经理、车间用户5种角色
3. **完整业务闭环**：申请 → 审批 → 能耗生成 → 巡检 → 分析
4. **消息通知机制**：关键业务节点自动触发通知
5. **审计追踪**：操作日志记录所有用户操作
