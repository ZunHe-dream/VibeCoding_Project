<template>
  <div class="main-content">
    <div class="header-section">
      <el-carousel trigger="click" height="400px" :interval="5000" arrow="always">
        <el-carousel-item v-for="(img, index) in bannerImages" :key="index">
          <div class="banner-wrapper" :style="img.linkUrl ? 'cursor:pointer' : ''"
            @click="img.linkUrl && router.push(img.linkUrl)">
            <img :src="img.url" class="banner-img" />
            <!-- 文字浮层：保持原有的标题和副标题 -->
            <div class="banner-text">
              <h1 class="logo-text">{{ img.mainTitle || '🏛️ 基于Vue3的文物巡展管理系统' }}</h1>
              <p class="sub-text">{{ img.title || '跨越时空的文明对话' }}</p>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </div>

    <div class="container">
      <div class="action-bar">
        <el-input v-model="queryParams.title" placeholder="搜索展览名称..." @input="handleSearch" clearable
          style="width: 180px; margin-right: 10px" />

        <el-input v-model="queryParams.location" placeholder="搜索举办地点..." @input="handleSearch" clearable
          style="width: 180px; margin-right: 10px" />

        <el-date-picker v-model="timeRange" type="daterange" range-separator="至" start-placeholder="开始日期"
          end-placeholder="结束日期" value-format="YYYY-MM-DD" @change="handleDateChange"
          style="width: 280px; margin-right: 15px" />

        <!-- <el-button type="primary" :icon="Refresh" @click="resetQuery" round>重置</el-button> -->
        <el-button :icon="Refresh" @click="resetQuery" round
          style="background-color:#7e5c3b; border-color:#7e5c3b; color:#fff;">重置</el-button>
      </div>

      <el-skeleton :loading="firstLoading" animated :count="4">
        <template #template>
          <div style="padding: 14px; width: 25%">
            <el-skeleton-item variant="image" style="width: 100%; height: 200px" />
            <el-skeleton-item variant="p" style="margin-top: 10px" />
          </div>
        </template>

        <template #default>
          <el-row :gutter="25">
            <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="item in exhibitions" :key="item.exhibitionId">
              <el-card :body-style="{ padding: '0px' }" class="exhibit-card" shadow="hover">
                <div class="img-wrapper">
                  <img :src="item.coverURL || 'https://images.unsplash.com/photo-1566127444979-b3d2b654e3d7?w=500'"
                    class="card-img" />
                </div>

                <div style="padding: 16px">
                  <h3 class="card-title">{{ item.title }}</h3>
                  <div class="card-info">
                    <p>📍 {{ item.location }}</p>
                    <p>📅 {{ formatDate(item.startTime) }} 至 {{ formatDate(item.endTime) }}</p>
                  </div>
                  <div class="card-footer">
                    <el-tag size="small" :type="getStatusType(item.currentStatus)" effect="dark">
                      {{ getStatusText(item.currentStatus) }}
                    </el-tag>
                    <el-button type="primary" link @click="toDetail(item.exhibitionId)">了解详情 →</el-button>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </template>
      </el-skeleton>

      <!-- ✅ 新增：底部哨兵元素，用于 IntersectionObserver 检测 -->
      <div ref="sentinel" class="sentinel"></div>

      <div class="loading-status">
        <p v-if="loading && !firstLoading">⏳ 正在加载更多历史文明...</p>
        <p v-if="noMore" class="no-more-text"> 已加载全部巡展，但文明永不止步 ...</p>
      </div>
    </div>
  </div>
</template>

<script setup>
// ✅ 新增 ref 用于引用哨兵元素
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import request from '@/utils/request'
import { Refresh } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const exhibitions = ref([]) 
const firstLoading = ref(true) 
const loading = ref(false)     
const noMore = ref(false)

// ✅ 新增：哨兵元素的 ref
const sentinel = ref(null)
// ✅ 新增：observer 实例，卸载时需要清理
let observer = null

const timeRange = ref([])
const queryParams = reactive({
  pageNum: 1,
  pageSize: 8,
  title: '',     // 对应原来的名称
  location: '',  // ✨ 新增：地点字段
  beginTime: '', // ✨ 新增：起始时间
  endTime: ''    // ✨ 新增：结束时间
})

const handleRoleChange = () => {
  console.log('角色切换');
};

const loadData = async () => {
  console.log('loadData触发，loading:', loading.value, 'noMore:', noMore.value) // 👈 加这行
  if (loading.value || noMore.value) return;
  
  loading.value = true;
  console.log(`📡 发起请求：页码=${queryParams.pageNum}`);
  
  try {
    const res = await request.get('/exhibition/plan/search', { params: queryParams });
    console.log('接口返回:', JSON.stringify(res))
    const newItems = (res.records || []).filter(i => i.exhibitionId && i.title)
    
    if (newItems.length > 0) {
      const existingIds = exhibitions.value.map(i => i.exhibitionId);
      const uniqueItems = newItems.filter(i => !existingIds.includes(i.exhibitionId));
      exhibitions.value.push(...uniqueItems);
      queryParams.pageNum++;
      console.log("✅ 加载成功，下一页码:", queryParams.pageNum);
      console.log('第一条展览数据:', JSON.stringify(exhibitions.value[0]))
    }
    
    if (newItems.length < queryParams.pageSize || newItems.length === 0) {
      noMore.value = true;
      // ✅ 数据全部加载完毕后，停止观察，避免无效触发
      observer?.disconnect()
      console.log("🏁 已经没有更多数据了");
    }
    if (exhibitions.value.length === 0) {
      // 没有任何数据时确保loading状态关闭
      firstLoading.value = false
    }
  } catch (error) {
    console.error("❌ 数据加载失败:", error);
  } finally {
    loading.value = false;
    firstLoading.value = false;
  }
};

const handleSearch = () => {
  exhibitions.value = []; 
  queryParams.pageNum = 1;
  noMore.value = false;
  // ✅ 搜索重置后，重新挂载 observer
  setupObserver()
  loadData();
};

// 当日期选择器发生变化时，自动更新参数并触发搜索
const handleDateChange = (val) => {
  if (val && val.length === 2) {
    queryParams.beginTime = val[0] + 'T00:00:00'  // 开始日期从当天零点开始
    queryParams.endTime = val[1] + 'T23:59:59'    // 结束日期到当天最后一秒
  } else {
    queryParams.beginTime = ''
    queryParams.endTime = ''
  }
  handleSearch() // 触发重新加载数据
}

// 点击“重置”按钮时执行
const resetQuery = () => {
  queryParams.title = ''
  queryParams.location = ''
  queryParams.beginTime = ''
  queryParams.endTime = ''
  timeRange.value = [] // 清空日期选择器显示
  handleSearch() // 重新加载第一页
}

// ✅ 新增：封装 IntersectionObserver 初始化逻辑，搜索重置时可复用
const setupObserver = () => {
  // 先清理旧的，避免重复监听
  observer?.disconnect()

  observer = new IntersectionObserver((entries) => {
    // 当哨兵元素进入视口，且当前不在加载中、也没到底，就触发加载
    if (entries[0].isIntersecting && !loading.value && !noMore.value) {
      loadData()
    }
  }, {
    // rootMargin 提前 100px 触发，体验更顺滑
    rootMargin: '0px 0px 100px 0px',
    threshold: 0
  })

  if (sentinel.value) {
    observer.observe(sentinel.value)
  }
}

const toDetail = (id) => {
  router.push(`/user/exhibition-detail/${id}`)
}
const bannerImages = ref([])

const fetchBanners = async () => {
  try {
    const res = await request.get('/banner/list')  // ← axios 改 request，路径去掉 /api
    const data = res?.data || res || []
    bannerImages.value = data.map(b => ({
      url: b.imageUrl, title: b.title, mainTitle: b.mainTitle,
      linkUrl: b.linkUrl }))
  } catch (e) {
    bannerImages.value = [
      { url: 'https://images.unsplash.com/photo-1566127444979-b3d2b654e3d7?w=1200', title: '探寻三星堆的神秘面纱' }
    ]
  }
}
const formatDate = (dateStr) => dateStr ? dateStr.split('T')[0] : '待定'
const getStatusText = (s) => ['计划中', '进行中', '已结束'][s] || '未知'
const getStatusType = (s) => ['warning', 'success', 'info'][s] || ''

onMounted(async () => {
  await fetchBanners()
  setupObserver()
  loadData()
})

onUnmounted(() => {
  // ✅ 组件销毁时清理 observer，防止内存泄漏
  observer?.disconnect()
});


</script>

<style scoped>
.main-content { background-color: #fcfaf8; min-height: 100vh; }
.header-section { background: linear-gradient(135deg, #1a2a6c, #b21f1f); color: white; padding: 60px 20px; text-align: center; margin-bottom: 30px; }
.logo-text { font-size: 2.5rem; margin: 0; letter-spacing: 2px; text-shadow: 2px 2px 4px rgba(0,0,0,0.3); }
.sub-text { opacity: 0.8; font-size: 1.1rem; }
.container { max-width: 1300px; margin: 0 auto; padding: 0 20px; padding-bottom: 50px; }
.action-bar { 
  margin-bottom: 30px; 
  display: flex; 
  justify-content: center; 
  align-items: center;
  flex-wrap: wrap; /* ✨ 增加这一行：如果屏幕窄，它会自动换行，不会撑破页面 */
  gap: 10px;      /* 元素之间的间距 */
}

.exhibit-card { border: none; border-radius: 15px; transition: all 0.3s ease; margin-bottom: 25px; overflow: hidden; background: #fff; }
.exhibit-card:hover { transform: translateY(-10px); box-shadow: 0 15px 30px rgba(0,0,0,0.15); }
.img-wrapper { height: 220px; overflow: hidden; }
.card-img { width: 100%; height: 100%; object-fit: cover; }

.card-title { font-size: 1.15rem; color: #2c3e50; margin: 0 0 10px 0; font-weight: 600; }
.card-info p { font-size: 13px; color: #666; margin: 4px 0; }
.card-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 15px; border-top: 1px solid #eee; padding-top: 12px; }

.loading-status { text-align: center; padding: 30px; }
.no-more-text { color: #999; }

/* ✅ 新增：哨兵元素本身不可见，但需要有高度才能被 Observer 检测到 */
.sentinel { height: 1px; }

/* 轮播图外层容器 */
.header-section {
  margin-bottom: 40px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0,0,0,0.1);
}

.banner-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
}

.banner-img {
  width: 100%;
  height: 100%;
  object-fit: cover; /* 保证图片不拉伸，裁剪铺满 */
  filter: brightness(0.7); /* 把图片调暗一点，衬托文字 */
}

.banner-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%); /* 居中定位 */
  text-align: center;
  color: white;
  text-shadow: 0 2px 10px rgba(0,0,0,0.5); /* 文字阴影防止看不清 */
  z-index: 10;
}

.logo-text {
  font-size: 3rem;
  margin-bottom: 10px;
  letter-spacing: 2px;
}

.sub-text {
  font-size: 1.5rem;
  opacity: 0.9;
}

</style>