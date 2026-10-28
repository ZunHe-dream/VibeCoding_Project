<template>
  <div class="detail-page" v-loading="loading">
    <!-- 顶部封面区 -->
    <div class="hero-section" :style="heroStyle">
      <div class="hero-overlay">
        <div class="hero-inner">
          <el-tag :type="getStatusTag(plan.currentStatus)" effect="dark" size="large" class="status-tag">
            {{ getStatusText(plan.currentStatus) }}
          </el-tag>
          <h1 class="hero-title">{{ plan.title }}</h1>
          <div class="hero-meta">
            <span>📍 {{ plan.location }}</span>
            <span>📅 {{ plan.startTime?.slice(0, 10) }} — {{ plan.endTime?.slice(0, 10) }}</span>
          </div>
          <div class="hero-actions">
            <el-button type="primary" size="large" round :disabled="plan.currentStatus !== 1" @click="handleReserve">
              {{ plan.currentStatus === 1 ? '立即预约' : '暂不开放' }}
            </el-button>
            <el-button :type="isFavorited ? 'warning' : 'default'" size="large" round :loading="favLoading"
              @click="toggleFavorite">
              {{ isFavorited ? '❤️ 已收藏' : '🤍 收藏展览' }}
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="content-wrap">
      <!-- 左侧主内容 -->
      <div class="main-col">
        <!-- 基本信息卡 -->
        <div class="info-card">
          <div class="card-title">📋 展览信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="举办地点">{{ plan.location }}</el-descriptions-item>
            <el-descriptions-item label="每日限额">{{ plan.maxCapacity }} 人</el-descriptions-item>
            <el-descriptions-item label="开展时间">{{ plan.startTime?.slice(0, 10) }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">{{ plan.endTime?.slice(0, 10) }}</el-descriptions-item>
            <el-descriptions-item label="场次安排" :span="2">
              <span v-for="s in parseJson(plan.timeSlotConfig)" :key="s.name" class="slot-tag">
                {{ s.name }}（{{ s.timeRange?.join(' - ') }}）
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="预约须知" :span="2">
              {{ plan.bookingNotice || '—' }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 票种 -->
        <div class="info-card" v-if="tickets.length > 0">
          <div class="card-title">🎫 票种价格</div>
          <div class="ticket-list">
            <div v-for="t in tickets" :key="t.name" class="ticket-item">
              <span class="ticket-name">{{ t.name }}</span>
              <span class="ticket-price">{{ t.price === 0 ? '免费' : '¥' + t.price }}</span>
            </div>
          </div>
        </div>

        <!-- 富文本展览介绍 -->
        <div class="info-card">
          <div class="card-title">📖 展览介绍</div>
          <div v-if="plan.richContent" class="rich-content ql-editor" v-html="plan.richContent" />
          <div v-else class="no-content">策展方暂未发布详细介绍</div>
        </div>

        <!-- 展出文物 -->
        <div class="info-card">
          <div class="card-title">🏺 展出文物（{{ artifacts.length }} 件）</div>
          <div v-if="artifacts.length === 0" class="no-content">暂无文物信息</div>
          <div class="artifact-grid">
            <div v-for="art in artifacts" :key="art.artifactId" class="artifact-card"
              @click="goArtifact(art.artifactId)">
              <div class="artifact-img-wrap">
                <img :src="art.imageUrl || 'https://placehold.co/200x200'" class="artifact-img"
                  @error="(e) => e.target.src = 'https://placehold.co/200x200'" />
              </div>
              <div class="artifact-info">
                <div class="artifact-name">{{ art.artifactName }}</div>
                <div class="artifact-meta">{{ art.dynasty }} · {{ art.category }}</div>
                <div class="artifact-museum">{{ art.ownerMuseum }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧边栏 -->
      <div class="side-col">
        <!-- 封面图 -->
        <div class="side-card" v-if="plan.coverURL">
          <img :src="plan.coverURL" class="side-cover" />
        </div>

        <!-- 快速预约 -->
        <div class="side-card reserve-card">
          <div class="card-title">🗓️ 快速预约</div>
          <div class="reserve-info">
            <p>📍 {{ plan.location }}</p>
            <p>⏰ {{ plan.startTime?.slice(0, 10) }} 起</p>
          </div>
          <el-button type="primary" style="width:100%" round :disabled="plan.currentStatus !== 1"
            @click="handleReserve">
            {{ plan.currentStatus === 1 ? '立即预约' : '暂不开放' }}
          </el-button>
        </div>

        <!-- 收藏 -->
        <div class="side-card fav-card">
          <el-button :type="isFavorited ? 'warning' : 'default'" style="width:100%" round :loading="favLoading"
            @click="toggleFavorite">
            {{ isFavorited ? '❤️ 已收藏此展览' : '🤍 收藏此展览' }}
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const plan = ref({})
const artifacts = ref([])
const isFavorited = ref(false)
const favLoading = ref(false)

const tickets = computed(() => {
  try { return JSON.parse(plan.value.ticketTypes || '[]') } catch { return [] }
})

const heroStyle = computed(() => {
  const url = plan.value.coverURL
  return url
    ? { backgroundImage: `url(${url})`, backgroundSize: 'cover', backgroundPosition: 'center' }
    : { background: 'linear-gradient(135deg, #2c1a0e, #7a3b1e)' }
})

const getUserId = () => {
  const u = JSON.parse(sessionStorage.getItem('user') || '{}')
  return u.userId || u.id || null
}
onMounted(async () => {
  loading.value = true
  const id = route.params.id
  try {
    const res = await request.get(`/exhibition/detail/${id}`)
    const data = res.data || res
    if (!data || !data.application) {
      ElMessage.error('展览不存在或已下架')
      router.push('/user/exhibition')
      return
    }
    plan.value = data.application || {}
    artifacts.value = data.artifacts || []
    await checkFav()
  } finally {
    loading.value = false
  }
})

const checkFav = async () => {
  const userId = getUserId()
  if (!userId) return
  const res = await request.get('/favorite/check', {
    params: { userId, exhibitionId: Number(route.params.id) }
  })
  isFavorited.value = res === true || res?.data === true
}

const toggleFavorite = async () => {
  if (!getUserId()) return ElMessage.warning('请先登录')
  favLoading.value = true
  try {
    await request.post('/favorite/toggle', {
      userId: getUserId(),
      exhibitionId: Number(route.params.id)
    })
    isFavorited.value = !isFavorited.value
    ElMessage.success(isFavorited.value ? '收藏成功 ✨' : '已取消收藏')
  } finally {
    favLoading.value = false
  }
}

const handleReserve = () => {
  if (!getUserId()) return ElMessage.warning('请先登录')
  router.push({ path: '/user/reservation/apply', query: { exId: plan.value.exhibitionId } })
}

const goArtifact = (id) => router.push(`/user/relic-detail/${id}`)

const parseJson = (str) => { try { return JSON.parse(str || '[]') } catch { return [] } }
const getStatusText = (s) => ['预热中', '进行中', '已结束'][s] ?? '未知'
const getStatusTag = (s) => ['info', 'success', ''][s] ?? 'info'
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: #f5f0e8;
  font-family: 'Noto Sans SC', sans-serif;
}

/* 顶部封面 */
.hero-section {
  height: 420px;
  position: relative;
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to bottom, rgba(0, 0, 0, 0.2), rgba(0, 0, 0, 0.7));
  display: flex;
  align-items: flex-end;
}

.hero-inner {
  padding: 40px;
  width: 100%;
  max-width: 1100px;
  margin: 0 auto;
}

.status-tag {
  margin-bottom: 12px;
}

.hero-title {
  font-size: 36px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 12px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.5);
}

.hero-meta {
  display: flex;
  gap: 24px;
  color: rgba(255, 255, 255, 0.85);
  font-size: 14px;
  margin-bottom: 20px;
}

.hero-actions {
  display: flex;
  gap: 12px;
}

/* 内容区 */
.content-wrap {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px 20px;
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.main-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.side-col {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: sticky;
  top: 20px;
}

.info-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
}

.side-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
}

.card-title {
  font-size: 15px;
  font-weight: 700;
  color: #2c1a0e;
  margin-bottom: 14px;
}

/* 场次标签 */
.slot-tag {
  display: inline-block;
  background: #faf0e6;
  color: #7a3b1e;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
  margin-right: 8px;
}

/* 票种 */
.ticket-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ticket-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: #faf7f3;
  border-radius: 8px;
  border-left: 3px solid #c8a96e;
}

.ticket-name {
  font-size: 14px;
  color: #2c1a0e;
}

.ticket-price {
  font-size: 16px;
  font-weight: 700;
  color: #c0392b;
}

/* 富文本 */
.rich-content {
  font-size: 15px;
  line-height: 1.9;
  color: #333;
}

:deep(.rich-content img) {
  max-width: 100%;
  border-radius: 8px;
  margin: 8px 0;
}

:deep(.ql-editor) {
  padding: 0;
  border: none;
}

.no-content {
  color: #999;
  font-size: 14px;
  padding: 20px 0;
  text-align: center;
}

/* 文物网格 */
.artifact-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 14px;
}

.artifact-card {
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #e8e0d0;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.artifact-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}

.artifact-img-wrap {
  height: 130px;
  background: #f8f4ee;
  overflow: hidden;
}

.artifact-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.artifact-card:hover .artifact-img {
  transform: scale(1.05);
}

.artifact-info {
  padding: 10px;
}

.artifact-name {
  font-size: 13px;
  font-weight: 600;
  color: #2c1a0e;
  margin-bottom: 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.artifact-meta {
  font-size: 11px;
  color: #9a7c5a;
}

.artifact-museum {
  font-size: 11px;
  color: #b0a090;
  margin-top: 2px;
}

/* 侧边栏 */
.side-cover {
  width: 100%;
  border-radius: 8px;
  object-fit: cover;
}

.reserve-info {
  font-size: 13px;
  color: #666;
  line-height: 2;
  margin-bottom: 12px;
}

.fav-card {
  padding: 12px;
}
</style>