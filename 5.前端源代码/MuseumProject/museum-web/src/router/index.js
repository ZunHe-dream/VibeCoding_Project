import { createRouter, createWebHistory, useRouter } from 'vue-router'
import { h, nextTick } from 'vue' // 引入渲染函数 h
// 1. 引入布局骨架
import Layout from '../layout/index.vue'

const routes = [
  // =================== 独立页面（不带侧边栏） ===================
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/Login.vue'),
    meta: { title: '登录' }
  },
  {
  path: '/register',
  name: 'Register',
  component: () => import('../views/login/Register.vue'),
  meta: { title: '注册' }
},

  // =================== 系统主架构（带侧边栏的页面） ===================
  {
    path: '/',
    component: Layout, 
    redirect: '/login', 
    children: [
      // --- 【普通用户模块】 ---
      {
        path: '/user/exhibition',
        name: 'ExhibitionList',
        component: () => import('../views/ExhibitionList.vue'),
        meta: { title: '巡展信息浏览' }
      },
      {
        path: '/user/relic',
        name: 'Detail',
        component: () => import('../views/user/Relic/RelicCategory.vue'),
        meta: { title: '文物分类区' }
      },
      {
        path: '/user/relic-list',
        name: 'RelicList',
        component: () => import('../views/user/Relic/RelicList.vue'),
        meta: { title: '文物列表' }
      },
      {
        path: '/user/relic-detail/:id',
        name: 'RelicDetail',
        component: () => import('@/views/user/Relic/RelicDetail.vue'),
        meta: { title: '文物详情' }
      },
      {
        path: '/user/appointment',
        component: () => import('../views/user/Reservation/ReservationManager.vue'),
        meta: { title: '我的预约管理' }
      },
      {
        path: '/user/reservation/apply',
        component: () => import('../views/user/Reservation/ReservationApply.vue'),
        meta: { title: '参观预约' }
      },
      {
        path: '/user/ticket/:id',
        name: 'TicketDetail',
        component: () => import('../views/user/Reservation/TicketDetail.vue'),
        meta: { title: '入场凭证' }
      },
      {
        path: '/user/favorite',
        component: () => import('../views/user/Favorite/MyFavorites.vue'),
        meta: { title: '我的收藏' }
      },
      {
        path: '/user/profile',
        component: () => import('../views/user/Profile/Profile.vue'),
        meta: { title: '个人信息管理' }
      },
      {
        path: '/user/calendar',
        component: () => import('../views/user/Visitcalendar/Visitcalendar.vue'),
        meta: { title: '预约日历' }
      },
      {
        path: '/user/shop',
        name: 'ShopHome',
        component: () => import('@/views/user/Shop/Shophome.vue'),
        meta: { title: '周边商城' }
      },
      { path: '/user/shophome', redirect: '/user/shop' },
      {
        path: '/user/shop/cart',
        name: 'ShopCart',
        component: () => import('@/views/user/Shop/Shopcart.vue'),
        meta: { title: '购物车' }
      },
      {
        path: '/user/shop/checkout',
        name: 'ShopCheckout',
        component: () => import('@/views/user/Shop/Shopcheckout.vue'),
        meta: { title: '订单结算' }
      },
      {
        path: '/user/shop/pay',
        name: 'ShopPay',
        component: () => import('@/views/user/Shop/ShopPay.vue'),
        meta: { title: '支付' }
      },
      {
        path: '/user/shop/pay-result',
        name: 'ShopPayResult',
        component: () => import('@/views/user/Shop/Shoppayresult.vue'),
        meta: { title: '支付结果' }
      },
      {
        path: '/user/shop/orders',
        name: 'ShopOrders',
        component: () => import('@/views/user/Shop/Shoporders.vue'),
        meta: { title: '我的订单' }
      },
      {
        path: '/user/exhibition-detail/:id',
        name: 'ExhibitionDetail',
        component: () => import('../views/user/ExhibitionDetail.vue'),
        meta: { title: '巡展详情' }
      },
      {
        path: '/user/notice',
        name: 'Notice',
        component: () => import('../views/user/Notice/Notice.vue'),
        meta: { title: '用户公告' }
      },

      // --- 【巡展申请方模块】 ---
      // ✅ 修复：将 template 改为 render 函数，防止生产环境/运行时崩溃
      {
        path: '/planner/apply',
        name: 'Apply',
        component: () => import('../views/planner/apply.vue'),
        meta: { title: '巡展意向提报' }
      },
      {
        path: '/planner/progress',
        name: 'Progress',
        component: () => import('../views/planner/LoanProgress.vue'),
        meta: { title: '审批进度跟踪' }
      },
      {
        path: '/planner/plan',
        name: 'publish',
        component: () => import('../views/planner/PlaneConfirm.vue'),
        meta: { title: '巡展发布管理' }
      },
      {
        path: '/planner/appointment',
        component: () => import('../views/planner/ApptManagement.vue'),
        meta: { title: '预约管理' }
      },
      {
        path: '/planner/artifactList',
        component: () => import('../views/planner/ArtifactLoanList.vue'),
        meta: { title: '文物借用清单' }
      },
       { 
        path: '/planner/notice', 
        component: () => import('@/views/admin/NoticePublish.vue'),
        meta: { title: '发布公告', role: 'planner' } // 标记这是巡展方视角
      },
      {
        path: '/planner/exhibition/editor', component: () => import('@/views/planner/ExhibitionEditor.vue'),
        meta: { title: '巡展内容编辑', role: 'planner' }
      },
      // --- 【商品详情页面】 ---
      { path: '/shop/product/edit', component: () => import('@/views/Shop/Shopproductedit.vue'),meta: { title: '编辑商品信息' } },
      { path: '/user/shop/detail', component: () => import('@/views/Shop/Shopproductdetail.vue'),meta: { title: '商品详情' } },
      {
        path: '/admin/shop/dashboard',
        name: 'ShopDashboard',
        component: () => import('@/views/Shop/Shopdashboard.vue'),
        meta: { title: '数据看板' }
      },
     
      // --- 【管理员模块】 ---
      {
        path: '/admin/audit',
        name: 'Audit',
        component: () => import('../views/admin/audit.vue'),
        meta: { title: '文物申请审批' }
      },
      {
        path: '/admin/artifactmonitor',
        name: 'artifactmonitor',
        component: () => import('../views/admin/Artifactmonitor.vue'),
        meta: { title: '文物监控' }
      },
      { 
  path: '/admin/notice', 
  component: () => import('@/views/admin/NoticePublish.vue'),
  meta: { title: '公告管理', role: 'admin' } // 标记这是管理员视角
},
      {
        path: '/admin/Artifactmanager',
        name: 'Artifactmanager',
        component: () => import('../views/admin/Artifactmanager.vue'),
        meta: { title: '文物管理' }
      },
      {
        path: '/admin/shop',
        name: 'ShopAdmin',
        component: () => import('@/views/admin/Shop/Shopadmin.vue'),
        meta: { title: '周边商城管理' }
      },
      { path: '/admin/planners', component: () => import('@/views/admin/PlannerManagement.vue'),
        meta: { title: '本馆策展用户管理' } },
      // {
      //   path: '/admin/relic',
      //   component: { render: () => h('div', { style: 'padding:40px; text-align:center' }, [h('h3', '💎 本馆文物管理模块正在开发中...')]) }
      // },

      { path: '/super/exhibitions', component: () => import('@/views/super/SuperExhibitionList.vue'),
        meta: { title: '巡展全链路监控' } },
      { path: '/super/users', component: () => import('@/views/super/SuperUserList.vue'),
        meta: { title: '全局用户管理' }  },
      { path: '/super/notices', component: () => import('@/views/super/SuperNoticeList.vue'),
        meta: { title: '公告全量调度' }  },
      { path: '/super/artifacts', component: () => import('@/views/super/SuperArtifactList.vue'),
        meta: { title: '馆藏资源池' }  },
      { path: '/super/cms', component: () => import('@/views/super/Superbannermanage.vue'),
        meta: { title: '门户视觉中心' }  },
      
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  // 1. 基础复位：针对 window 滚动的复位
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0, behavior: 'smooth' }
    }
  }
})

router.beforeEach((to, from, next) => {
  // 如果路由配置了 title，就用配置的；没有就用默认的 'museum_web'
  document.title = to.meta.title || 'museum_web'
  next()
})


// 2. 暴力复位：针对 Element Plus 布局容器内部滚动的复位
router.afterEach(() => {
  // 使用 nextTick 确保组件已挂载，DOM 已更新
  nextTick(() => {
    // 尝试寻找 Element Plus 的 el-main 容器
    const mainContainer = document.querySelector('.el-main')
    if (mainContainer) {
      mainContainer.scrollTo({
        top: 0,
        behavior: 'smooth'
      })
    }
    
    // 如果存在 window 级别的滚动，也一并复位
    window.scrollTo({
      top: 0,
      behavior: 'smooth'
    })
  })
})

export default router