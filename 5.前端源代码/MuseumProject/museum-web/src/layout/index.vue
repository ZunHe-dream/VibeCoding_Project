<template>
  <el-container class="app-wrapper">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar-container">
      <div class="logo">
        <span v-if="!isCollapse">🏛️ Hi~{{userInfo.nickname}}</span>
        <span v-else>🏛️</span>
      </div>

      <el-menu :default-active="$route.path" :collapse="isCollapse" background-color="#594332" text-color="#ede1c9"
        active-text-color="#e4bf7a" router class="el-menu-vertical">

        <template v-if="currentRole === 'USER'">
          <el-menu-item index="/user/exhibition">📍 巡展信息浏览</el-menu-item>
          <el-menu-item index="/user/relic">🏺 文物信息查看</el-menu-item>
          <el-menu-item index="/user/appointment">📅 预约记录管理</el-menu-item>
          <el-menu-item index="/user/calendar"> 👁️ 预约日历查看</el-menu-item>
          <el-menu-item index="/user/favorite">❤️ 我的收藏关注</el-menu-item>
          <el-menu-item index="/user/notice">📢 公告通知查看</el-menu-item>
          <el-menu-item index="/user/shop">🛒 文物周边商城</el-menu-item>
          <el-menu-item index="/user/profile">👤 个人信息管理</el-menu-item>
        </template>

        <template v-if="currentRole === 'APPLICANT'">
          <el-menu-item index="/planner/apply">
            <el-icon>
              <EditPen />
            </el-icon><span> 巡展意向提报</span>
          </el-menu-item>
          <el-menu-item index="/planner/progress">
            <el-icon>
              <Loading />
            </el-icon><span> 审批进度跟踪</span>
          </el-menu-item>
          <el-menu-item index="/planner/plan">
            <el-icon>
              <Calendar />
            </el-icon><span> 巡展发布管理</span>
          </el-menu-item>
          <el-menu-item index="/planner/appointment">
            <el-icon>
              <Ticket />
            </el-icon><span> 参观预约管理</span>
          </el-menu-item>
          <el-menu-item index="/planner/artifactList">
            <el-icon>
              <Link />
            </el-icon><span> 文物借用清单</span>
          </el-menu-item>
          <el-menu-item index="/planner/notice">
            <el-icon>
              <Promotion />
            </el-icon><span> 站内公告发布</span>
          </el-menu-item>
        </template>

        <template v-if="currentRole === 'ADMIN'">
          <el-menu-item index="/admin/audit">
            <el-icon>
              <Stamp />
            </el-icon><span> 文物申请审批</span>
          </el-menu-item>
          <el-menu-item index="/admin/Artifactmanager">
            <el-icon>
              <Box />
            </el-icon><span> 馆藏文物档案</span>
          </el-menu-item>

          <el-menu-item index="/admin/artifactmonitor">
            <el-icon>
              <Van />
            </el-icon><span> 文物外借监控</span>
          </el-menu-item>

          <!-- <el-menu-item index="/admin/coordinate">
            <el-icon>
              <PieChart />
            </el-icon><span> 全局计划统筹</span>
          </el-menu-item> -->

          <el-menu-item index="/admin/notice">
            <el-icon>
              <Memo />
            </el-icon><span> 站内公告发布</span>
          </el-menu-item>

          <el-menu-item index="/admin/shop">
            <el-icon>
              <ShoppingCart />
            </el-icon><span> 周边商城管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/shop/dashboard">
            <el-icon>
              <ShoppingCart />
            </el-icon><span> 商品数据看板</span>
          </el-menu-item>

          <el-menu-item index="/admin/planners">
            <el-icon>
              <Memo />
            </el-icon><span> 本馆策展用户管理</span>
          </el-menu-item>
        </template>
        <template v-if="currentRole === 'SUPER_ADMIN'">
          <el-menu-item index="/super/exhibitions">
            <el-icon>
              <Van />
            </el-icon>
            <span> 巡展全链路监控</span>
          </el-menu-item>
          <el-menu-item index="/super/users">
            <el-icon>
              <Stamp />
            </el-icon>
            <span> 全局用户管理</span>
          </el-menu-item>
          <el-menu-item index="/super/notices">
            <el-icon>
              <Memo />
            </el-icon>
            <span> 公告全量调度</span>
          </el-menu-item>
          <el-menu-item index="/super/artifacts">
            <el-icon>
              <Box />
            </el-icon>
            <span> 馆藏资源池</span>
          </el-menu-item>
          <el-menu-item index="/super/cms">
            <el-icon>
              <PieChart />
            </el-icon>
            <span> 门户视觉中心</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container class="main-container">
      <el-header class="header">
        <div class="header-left">
          <el-icon @click="isCollapse = !isCollapse" class="fold-btn">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <div class="system-title">基于Vue3的文物巡展管理系统</div>
        </div>

        <div class="header-right">
          <div class="user-info-block">
            <<div class="user-avatar">
              <img v-if="userInfo?.avatar" :src="userInfo.avatar" class="avatar-img" />
              <span v-else>{{ (userInfo?.nickname || username).charAt(0) }}</span>
          </div>
          <div class="user-detail">
            <div class="user-welcome">欢迎，{{ userInfo?.nickname || username }}</div>
            <div class="user-belong">{{ userInfo?.belong || '—' }}</div>
          </div>
        </div>
        <div class="header-divider"></div>
        <el-button type="danger" link @click="handleLogout" class="logout-btn">退出登录</el-button>
        </div>
      </el-header>

      <el-main class="main-content">
        <div class="main-watermark" style="pointer-events: none;">
          <div v-for="n in 12" :key="n" class="wm-text">
            {{ username }} | {{ realTime }}
          </div>
        </div>

        <div class="route-view-container">
          <router-view />
        </div>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import {
  Fold, Expand, ArrowDown, EditPen, Loading, Calendar,
  Ticket, Link, Promotion, Stamp, Box, Van, PieChart, Memo, Bell, ShoppingCart
} from '@element-plus/icons-vue'

const router = useRouter()
const isCollapse = ref(false)

// --- 💡 关键修改：从“先声明后赋值”改为“声明即赋值” ---

// 1. 先尝试获取本地存储的数据
const userJson = sessionStorage.getItem('user')
const userInfo = userJson ? JSON.parse(userJson) : null

// 2. 直接在定义时就给初始值，这样页面渲染的第一秒就有角色信息了
const currentRole = ref(userInfo ? userInfo.role : '')
const username = ref(userInfo ? (userInfo.username || '测试账号') : '')

const realTime = ref(dayjs().format('YYYY-MM-DD HH:mm:ss'))
const noticeCount = ref(3)
let timer = null

const roleNameMap = {
  'USER': '数字展厅用户',
  'APPLICANT': '巡展合作单位',
  'ADMIN': '总馆统筹专家',
  'SUPER_ADMIN': '站点管理员'
}

onMounted(() => {
  // 3. 如果发现根本没登录，直接踢回登录页
  if (!userInfo) {
    router.push('/login')
    return
  }

  timer = setInterval(() => {
    realTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss')
  }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

// const handleRoleChange = (role) => {
//   currentRole.value = role
//   if (role === 'ADMIN') router.push('/admin/audit')
//   else if (role === 'APPLICANT') router.push('/planner/apply')
//   else router.push('/user/exhibition')
// }

// const handleRoleChange = (role) => {
//   currentRole.value = role

//   // ✅ 关键一步：同步更新本地存储，否则刷新就重置
//   if (userInfo) {
//     userInfo.role = role
//     sessionStorage.setItem('user', JSON.stringify(userInfo))
//   }

//   if (role === 'ADMIN') router.push('/admin/audit')
//   else if (role === 'APPLICANT') router.push('/planner/apply')
//   else router.push('/user/exhibition')
// }

const handleLogout = () => {
  sessionStorage.removeItem('user')
  router.push('/login')
}
</script>

<style scoped>
.app-wrapper {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

.sidebar-container {
  background-color: #594332;
  transition: width 0.3s;
  overflow-x: hidden;
  box-shadow: 2px 0 8px rgba(44, 26, 14, 0.25);
  position: relative;
}
.sidebar-container::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 300px;
  height: 400px;
  /* 220 * (437/500) 保持比例 */
  background-image: url('@/assets/hw.png');
  background-size: 1536px 1024px;
  background-repeat: no-repeat;
  background-position: -30px -580px;
  opacity: 0.15;
  pointer-events: none;
}
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #4a2e1a;
  font-weight: bold;
  font-size: 13px;
  background-color: #eeddbf;
  border-bottom: 1px solid #d4c4a8;
}

.el-menu-vertical:not(.el-menu--collapse) {
  width: 220px;
}
.el-menu-vertical :deep(.el-menu-item.is-active) {
  background-color: #6b4423 !important;
}

.el-menu-vertical :deep(.el-menu-item:hover) {
  background-color: #5c3520 !important;
}
.el-menu {
  border-right: none;
}

.header {
  height: 60px;
  background: #eeddbf;
  border-bottom: 1px solid #d4c4a8;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
box-shadow: 0 4px 6px -2px rgba(44, 26, 14, 0.15);
}

.header-left {
  display: flex;
  align-items: center;
  
}

.fold-btn {
  font-size: 20px;
  cursor: pointer;
  color: #4a2e1a;
  transition: color 0.2s;
}



.notice-bell {
  cursor: pointer;
  display: flex;
  align-items: center;
  transition: transform 0.3s;
}

.notice-bell:hover {
  transform: scale(1.1);
}

.bell-badge :deep(.el-badge__content) {
  top: 5px;
  right: 5px;
}

.user-welcome {
  font-size: 14px;
  color: #666;
}

.el-dropdown-link {
  cursor: pointer;
  color: #409EFF;
  font-weight: bold;
  display: flex;
  align-items: center;
}

.main-content {
  background-color: #f0f2f5;
  position: relative;
  overflow-y: auto;
  padding: 20px;
  height: 0;
  flex:1;
}

.main-watermark {
  position: absolute;
  inset: 0;
  pointer-events: none;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  opacity: 0.05;
  z-index: 0;
  pointer-events: none;
    /* ← 关键！让鼠标点击穿透水印层 */
    z-index: 0;
    /* ← 确保不压在内容上方 */
}

.wm-text {
  transform: rotate(-25deg);
  margin: 80px 0;
  font-size: 14px;
  font-weight: bold;
  color: #000;
}

.route-view-container {
  position: relative;
  z-index: 1;
}

/* .header {
  height: 60px;
  background: linear-gradient(135deg, #7a1010 0%, #8b1a1a 50%, #a52020 100%);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  box-shadow: 0 2px 8px rgba(139, 26, 26, 0.3);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.fold-btn {
  font-size: 20px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.85);
  transition: color 0.2s;
}
.fold-btn:hover {
  color: #8b1a1a;
} */


.system-title {
  font-size: 15px;
  font-weight: 600;
  color: #5c3d1e;
  letter-spacing: 1px;
  font-family: 'SimHei', '黑体', sans-serif;
  border-left: 2px solid #c8a96e;
  padding-left: 16px;
}

.fold-btn {
  font-size: 20px;
  cursor: pointer;
  color: #5c3d1e;
  transition: color 0.2s;
}

.fold-btn:hover {
  color: #8b1a1a;
}

.user-welcome {
  font-size: 13px;
  color: #2c1a0e;
  font-weight: 500;
  line-height: 1;
}

.user-belong {
  font-size: 11px;
  color: #9a7c5a;
  line-height: 1;
}

.user-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #5c3d1e;
  border: 1px solid #c8a96e;
  color: #f5e6c8;
  font-size: 14px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-divider {
  width: 1px;
  height: 20px;
  background: #d4c4a8;
}

.logout-btn {
  font-weight: bold;
  margin-left: 10px;
  color: #5c3d1e !important;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info-block {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #4a2e1a;
  border: 1px solid #c8a96e;
  color: #f5e6c8;
  font-size: 14px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.user-detail {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.user-welcome {
  font-size: 13px;
  color: #2c1a0e;
  font-weight: 500;
  line-height: 1;
}

.user-belong {
  font-size: 11px;
  color: #9a7c5a;
  line-height: 1;
}

.header-divider {
  width: 1px;
  height: 20px;
  background: #d4c4a8;
}
</style>