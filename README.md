# 博物馆文物管理系统 · Cultural Relic Management System

一个前后端分离的博物馆综合业务平台，覆盖**文物管理、展览策划与审核、参观预约、公告发布、文创商城**等场景，面向普通观众、策展方、博物馆管理员和站点管理员四类角色。

后端 Spring Boot + MyBatis-Plus + MySQL，前端 Vue 3 + Vite + Element Plus，共 119 个 REST 接口、23 张业务表。

---

## 目录

- [功能概览](#功能概览)
- [技术栈](#技术栈)
- [目录结构](#目录结构)
- [快速开始](#快速开始)
- [测试账号](#测试账号)
- [数据库表](#数据库表)
- [接口概览](#接口概览)
- [说明文档](#说明文档)
- [已知限制](#已知限制)

---

## 功能概览

系统按角色划分功能入口，登录后由 `sys_user.role` 字段决定侧边栏菜单。

| 角色 | `role` 值 | 主要功能 |
| --- | --- | --- |
| 普通用户 | `USER` | 展览浏览、文物分类/列表/详情、参观预约与入场凭证、我的收藏、通知公告、个人资料、文创商城（商品浏览、购物车、下单、支付、订单） |
| 策展方 | `APPLICANT` | 提交展览申请、查看审核进度、编辑展览方案、管理本馆预约、发起文物借展申请并跟踪进度 |
| 博物馆管理员 | `ADMIN` | 审核展览申请、文物与图片管理、文物状态监控、发布公告、商城商品/订单看板、策展人账号管理 |
| 站点管理员 | `SUPER_ADMIN` | 全站展览、全站用户、全站公告、全站文物、门户轮播（CMS）管理 |

> 数据库 `role` 枚举中还预留了 `SHOP_ADMIN`（商城管理员），种子数据未提供该账号，目前商城管理入口挂在博物馆管理员菜单下。

---

## 技术栈

**后端**

| 组件 | 版本 / 说明 |
| --- | --- |
| JDK | 17（`pom.xml` 中 `java.version`） |
| Spring Boot | 3.1.5（内嵌 Tomcat） |
| MyBatis-Plus | 3.5.3.1 |
| Druid | 1.2.16 连接池 |
| MySQL | 8.0（`mysql-connector-j`） |
| Lombok | 简化实体类 |
| Maven | 3.8+ |

**前端**

| 组件 | 版本 / 说明 |
| --- | --- |
| Vue | 3.5 |
| Vite | 7.x |
| Element Plus | 2.13 |
| Vue Router | 4.6 |
| Axios | 1.13（统一封装于 `src/utils/request.js`） |
| Node.js | 20.19+（Vite 7 的最低要求） |

---

## 目录结构

```
VibeCoding_Project/
├── 1.系统源代码/
│   ├── 后端/
│   │   └── cultural-relic-management/     # Spring Boot 工程（Maven）
│   │       ├── pom.xml
│   │       └── src/main/
│   │           ├── java/org/example/
│   │           │   ├── CulturalRelicApplication.java   # 启动类
│   │           │   ├── controller/   # 16 个控制器，119 个接口
│   │           │   ├── service/      # 业务层（含 impl）
│   │           │   ├── mapper/       # MyBatis-Plus Mapper
│   │           │   ├── entity/       # 25 个实体
│   │           │   ├── config/       # CORS、静态资源、分页插件
│   │           │   └── common/       # 统一返回体 Result
│   │           └── resources/application.yml
│   └── 图片资源/museum_uploads/            # 系统使用的文物与展览图片
├── 2.数据库文件/
│   └── cultural_relic_system.sql           # MySQL 结构 + 初始数据
├── 3.系统配置说明/系统配置说明.docx
├── 4.系统使用说明/系统使用说明.docx
└── 5.前端源代码/
    └── MuseumProject/
        ├── museum-web/                     # Vue 3 + Vite 前端工程
        │   ├── src/{views,components,layout,router,utils}
        │   ├── public/                     # 文物分类配图
        │   ├── vite.config.js              # 端口 5173，/api 反向代理
        │   └── package.json
        └── requirements.txt
```

---

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0
- Node.js 20.19+ 与 npm

### 1. 初始化数据库

```bash
# 建库（字符集务必 utf8mb4，SQL 中含中文与 emoji）
mysql -u root -p -e "CREATE DATABASE cultural_relic_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"

# 导入结构与初始数据
mysql -u root -p cultural_relic_system < "2.数据库文件/cultural_relic_system.sql"
```

### 2. 启动后端

修改 `1.系统源代码/后端/cultural-relic-management/src/main/resources/application.yml` 中的数据库账号密码：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/cultural_relic_system?useUnicode=true&characterEncoding=utf-8&serverTimezone=GMT%2B8
    username: root
    password: 你的密码
```

然后运行启动类 `org.example.CulturalRelicApplication`，或：

```bash
cd "1.系统源代码/后端/cultural-relic-management"
mvn spring-boot:run
```

服务默认监听 `http://localhost:8080`。

> **图片上传目录**：`FileController` 与 `WebMvcConfig` 中把上传目录硬编码为 `D:/museum_uploads/`，并通过 `/files/**` 对外暴露。首次运行前请手动创建该目录，否则上传接口会报错；如需换路径，两个类要同步修改。

### 3. 启动前端

```bash
cd "5.前端源代码/MuseumProject/museum-web"
npm install
npm run dev
```

打开终端输出的地址（默认 `http://localhost:5173`）。开发环境下 `/api` 前缀的请求会经 Vite 代理转发到 `http://localhost:8080` 并去掉前缀，因此本地联调无需额外配置跨域。

### 4. 打包部署（可选）

```bash
# 后端
cd "1.系统源代码/后端/cultural-relic-management"
mvn clean package
java -jar target/cultural-relic-management-1.0-SNAPSHOT.jar

# 前端
cd "5.前端源代码/MuseumProject/museum-web"
npm run build          # 产物在 dist/
```

生产部署时需自行把 `dist/` 交由 Nginx 等托管，并把 `/api` 反向代理到后端；后端的 CORS 白名单目前只放行 `http://localhost:5173`，跨域部署需要相应调整。

---

## 测试账号

初始数据中的账号（密码为明文，见 SQL 文件）：

| 角色 | 账号 | 密码 | 所属 |
| --- | --- | --- | --- |
| 普通用户 | `user1` | `123456` | 用户 |
| 策展方 | `app1` | `123456` | 河北博物馆 |
| 策展方 | `xiaowang` | `147258` | 广东博物馆 |
| 策展方 | `app_shanghai` | `123456` | 上海博物馆 |
| 博物馆管理员 | `admin1` | `123456` | 河北博物馆 |
| 博物馆管理员 | `admin2` | `123456` | 广东省博物馆 |
| 博物馆管理员 | `admin3` | `123456` | 上海博物馆 |
| 站点管理员 | `superadmin` | `123456` | 系统 |

> `4.系统使用说明` 中记录的站点管理员账号写作 `super_admin`，与数据库中实际的 `superadmin` 不一致，请以数据库为准。

---

## 数据库表

`cultural_relic_system` 库共 23 张表：

| 模块 | 表 |
| --- | --- |
| 用户与账号 | `sys_user`、`user_visitor`、`user_address` |
| 文物与收藏 | `artifact`、`favorite`、`user_fav_artifact` |
| 展览与策划 | `exhibition_application`、`exhibition_plan`、`exhibition_artifact_rel` |
| 预约与借展 | `reservation`、`artifact_loan_request` |
| 公告与门户 | `notice`、`notice_type`、`banner`、`portal_banner` |
| 文创商城 | `shop_category`、`shop_product`、`shop_product_spec`、`shop_product_sku`、`shop_cart`、`shop_order`、`shop_order_item`、`shop_review` |

主键统一为自增 `bigint`，`sys_user.role` 使用枚举区分角色，`belong` 字段用于按博物馆做数据隔离。

---

## 接口概览

后端共 119 个接口，统一返回 `Result<T>{ code, msg, data }`，前端在 `request.js` 的响应拦截器中自动拆包并按 `code` 统一提示错误。

| 模块 | 基础路径 | 接口数 | 说明 |
| --- | --- | --- | --- |
| 用户 | `/user` | 12 | 登录/登出、改资料、改密码、策展人与管理员账号管理 |
| 文物 | `/artifact` | 7 | 文物增删改查、分类与状态 |
| 展览 | `/exhibition` | 16 | 展览列表/详情、审核流程 |
| 展览申请 | `/exhibition/application` | 3 | 策展方提交与查询申请 |
| 展览方案 | `/exhibition/plan` | 4 | 方案编辑与确认 |
| 预约 | `/reservation` | 8 | 参观预约、核销、凭证 |
| 借展 | `/loan` | 6 | 文物借展申请与审批进度 |
| 收藏 | `/favorite` | 5 | 文物收藏 / 展览收藏 |
| 公告 | `/notice` | 10 | 公告类型与内容管理 |
| 轮播 | `/banner` | 5 | 门户轮播（CMS） |
| 商城 | `/shop` | 32 | 商品、规格、SKU、购物车、订单、支付、评价 |
| 商城看板 | `/shop/dashboard` | 6 | 销售与订单统计 |
| 访客 | `/visitor` | 3 | 访客登记与统计 |
| 文件 | `/upload`、`/file` | 2 | 图片上传与静态访问 |

关键约定：

- 登录成功后用户对象写入 `HttpSession`（键名 `user`），后续部分接口依赖会话状态。
- 上传接口返回可访问 URL，静态资源由 `/files/**` 映射到本地磁盘目录。

---

## 说明文档

- `3.系统配置说明/系统配置说明.docx`：开发环境、工具版本
- `4.系统使用说明/系统使用说明.docx`：环境准备、启动步骤与测试账号

---

## 已知限制

本项目为毕业设计作品，侧重功能完整与业务流程演示，以下问题在生产环境需要改造：

1. **密码明文存储与传输**，未做加盐哈希；建议引入 BCrypt。
2. **会话基于 `HttpSession`**，多实例部署需改为集中式会话或 Token 鉴权。
3. **上传目录硬编码** `D:/museum_uploads/`，需改为可配置项。
4. **CORS 白名单仅 `http://localhost:5173`**，换端口或部署域名需同步修改 `CorsConfig`。
5. 前端请求超时固定 5 秒（`request.js`），大文件或慢接口需调整。
6. 仓库中 `museum-web/node_modules`、`dist`、Python `venv` 均未纳入版本控制，克隆后需自行安装依赖。
7. 图片资源约 190 MB，仓库体积较大，克隆时注意网络。

---

仅供学习与交流使用。
