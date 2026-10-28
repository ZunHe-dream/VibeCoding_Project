<template>
    <div class="favorite-page-wrapper">
        <div class="page-header">
            <div class="header-content">
                <div class="title-group">
                    <span class="sub-title">MY MUSEUM HUB</span>
                    <h1 class="main-title">我的 <span class="accent">收藏与关注</span></h1>
                </div>
                <div class="seal-mark">藏</div>
            </div>
        </div>

        <div class="content-container">
            <el-tabs v-model="activeTab" class="custom-tabs" @tab-change="handleTabChange">

                <el-tab-pane label="珍藏文物" name="artifacts">
                    <div v-loading="loading" class="list-wrapper">
                        <el-row :gutter="24" v-if="artifactList.length > 0">
                            <el-col :lg="6" :md="8" :sm="12" :xs="24" v-for="item in artifactList"
                                :key="item.artifact_id">
                                <div class="relic-card" @click="goToArtifactDetail(item.artifact_id)">
                                    <div class="card-image">
                                        <img :src="item.imageUrl || item.image_url"
                                            v-if="item.imageUrl || item.image_url" />
                                        <div class="tag-dynasty">{{ item.dynasty }}</div>
                                    </div>
                                    <div class="card-info">
                                        <h3 class="name">{{ item.artifact_name }}</h3>
                                        <div class="meta-row">
                                            <el-icon>
                                                <OfficeBuilding />
                                            </el-icon>
                                            <span>{{ item.storage_location }}</span>
                                        </div>
                                        <div class="hover-line"></div>
                                    </div>
                                </div>
                            </el-col>
                        </el-row>
                        <div class="empty-state" v-else-if="!loading">
                            <p>尚未收藏任何文物</p>
                        </div>
                    </div>
                </el-tab-pane>

                <el-tab-pane label="关注巡展" name="exhibitions">
                    <div v-loading="loading" class="list-wrapper">
                        <div v-if="exhibitionList.length > 0">
                            <div class="exhibition-item" v-for="ex in exhibitionList" :key="ex.exhibitionId"
                                @click="goToExhibitionDetail(ex.exhibitionId)">
                                <div class="ex-cover"
                                    :style="{ backgroundImage: `url(${ex.coverURL || ex.imageUrl || ex.coverUrl || ex.coverImage})` }">
                                    <div class="ex-status-tag">关注中</div>
                                </div>
                                <div class="ex-details">
                                    <h3 class="ex-title">{{ ex.title }}</h3>
                                    <p class="ex-intro">{{ ex.intro || ex.description || '暂无介绍' }}</p>
                                    <div class="ex-meta">
                                        <span><el-icon>
                                                <Calendar />
                                            </el-icon> {{ ex.startTime?.split('T')[0] }}</span>
                                        <span><el-icon>
                                                <Location />
                                            </el-icon> {{ ex.location }}</span>
                                    </div>
                                </div>
                                <div class="ex-arrow">进入详情 →</div>
                            </div>
                        </div>
                        <div class="empty-state" v-else-if="!loading">
                            <p>尚未关注任何巡展</p>
                        </div>
                    </div>
                </el-tab-pane>
            </el-tabs>
        </div>
    </div>
</template>

<!-- <script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { OfficeBuilding, Calendar, Location } from '@element-plus/icons-vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const router = useRouter()
const activeTab = ref('artifacts')
const loading = ref(false)
const artifactList = ref([])
const exhibitionList = ref([])

// 获取当前登录用户 ID
const getUserId = () => {
    const userStr = sessionStorage.getItem('user')
    return userStr ? JSON.parse(userStr).userId : null
}

// 加载文物数据 (对接你的 IFavArtifactService)
const fetchArtifacts = async () => {
    const userId = getUserId()
    if (!userId) return ElMessage.warning('请先登录')
    loading.value = true
    try {
        const res = await axios.get(`http://localhost:8080/favorite/my`, { params: { userId } })
        console.log("巡展收藏返回数据:", res.data); // 调试看字段名
        if (res.data.code === 200) artifactList.value = res.data.data
    } finally {
        loading.value = false
    }
}

// 加载巡展数据 (对接你的巡展关注 Service)
const fetchExhibitions = async () => {
    const userId = getUserId()
    if (!userId) return
    loading.value = true
    try {
        // ✅ 确认路径是 /favorite/my
        const res = await axios.get(`http://localhost:8080/favorite/my`, { params: { userId } })

        // 打印出来确认一下
        console.log("巡展收藏返回数据:", res.data)

        if (res.data.code === 200) {
            // 赋值给响应式变量
            exhibitionList.value = res.data.data
        }
    } catch (error) {
        console.error("获取巡展列表失败", error)
    } finally {
        loading.value = false
    }
}

// Tab 切换逻辑
const handleTabChange = (name) => {
    if (name === 'artifacts') fetchArtifacts()
    else fetchExhibitions()
}

const goToArtifactDetail = (id) => router.push(`/user/relic-detail/${id}`)
const goToExhibitionDetail = (id) => router.push(`/user/exhibition-detail/${id}`)

onMounted(() => {
    fetchArtifacts() // 初始加载文物
})
</script> -->

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { OfficeBuilding, Calendar, Location } from '@element-plus/icons-vue'
import request from '@/utils/request' // 用于巡展（已脱壳）
import axios from 'axios' // 用于文物（未脱壳）
import { ElMessage } from 'element-plus'

const router = useRouter()
const activeTab = ref('artifacts')
const loading = ref(false)
const artifactList = ref([])
const exhibitionList = ref([])

const getUserId = () => {
    const userStr = sessionStorage.getItem('user')
    return userStr ? JSON.parse(userStr).userId : null
}

// 加载文物：使用 axios + 下划线字段
const fetchArtifacts = async () => {
    const userId = getUserId()
    if (!userId) return
    loading.value = true
    try {
        const res = await axios.get(`http://localhost:8080/favorite/artifacts`, { params: { userId } })
        if (res.data.code === 200) {
            artifactList.value = res.data.data
        }
    } catch (e) {
        console.error("文物加载失败", e)
    } finally {
        loading.value = false
    }
}

// 加载巡展：使用 request + 驼峰字段
const fetchExhibitions = async () => {
    const userId = getUserId()
    if (!userId) return
    loading.value = true
    try {
        // 因为 request 拦截器已经 return res.data，所以这里直接拿到的就是数组
        const res = await request.get('/favorite/my', { params: { userId } })
        exhibitionList.value = res || []
        console.log('巡展数据字段：', JSON.stringify(exhibitionList.value[0]))
    } catch (e) {
        console.error("巡展加载失败", e)
    } finally {
        loading.value = false
    }
}

const handleTabChange = (name) => {
    if (name === 'artifacts') fetchArtifacts()
    else fetchExhibitions()
}

const goToArtifactDetail = (id) => router.push(`/user/relic-detail/${id}`)
const goToExhibitionDetail = (id) => router.push(`/user/exhibition-detail/${id}`)

onMounted(() => {
    fetchArtifacts()
})
</script>

<style scoped>
.favorite-page-wrapper {
    padding: 40px 20px;
    background-color: #fcfaf8;;
    /* 透出父容器的 #d0d3cb */
    min-height: 100vh;
}

.page-header {
    max-width: 1200px;
    margin: 0 auto 30px;
    border-bottom: 1px solid rgba(0, 0, 0, 0.1);
    padding-bottom: 20px;
}

.header-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.main-title {
    font-size: 28px;
    color: #2c2825;
}

.accent {
    color: #8b4a2a;
}

.seal-mark {
    width: 45px;
    height: 45px;
    border: 2px solid #8b4a2a;
    color: #8b4a2a;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: bold;
    font-size: 22px;
}

/* Tabs 自定义样式 */
.content-container {
    max-width: 1200px;
    margin: 0 auto;
}

:deep(.el-tabs__item.is-active) {
    color: #8b4a2a !important;
    font-weight: bold;
}

:deep(.el-tabs__active-bar) {
    background-color: #8b4a2a;
}

/* 文物卡片 (Grid) */
.relic-card {
    background: #fff;
    border-radius: 4px;
    overflow: hidden;
    margin-bottom: 24px;
    cursor: pointer;
    border: 1px solid #e2ddd6;
    transition: 0.3s;
}

.relic-card:hover {
    transform: translateY(-5px);
    box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1);
}

.card-image {
    height: 180px;
    position: relative;
    background: #f5f5f5;
}

.card-image img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.tag-dynasty {
    position: absolute;
    top: 10px;
    left: 10px;
    background: #8b4a2a;
    color: #fff;
    padding: 2px 8px;
    font-size: 11px;
}

.card-info {
    padding: 15px;
    position: relative;
}

.name {
    margin: 0 0 10px;
    font-size: 16px;
    color: #2c2825;
}

.meta-row {
    display: flex;
    align-items: center;
    gap: 5px;
    font-size: 12px;
    color: #9b9089;
}

.hover-line {
    height: 2px;
    background: #8b4a2a;
    width: 0;
    position: absolute;
    bottom: 0;
    left: 0;
    transition: 0.3s;
}

.relic-card:hover .hover-line {
    width: 100%;
}

/* 巡展列表项 (Flex) */
.exhibition-item {
    display: flex;
    background: #fff;
    border-radius: 8px;
    margin-bottom: 20px;
    border: 1px solid #e2ddd6;
    cursor: pointer;
    transition: 0.3s;
    overflow: hidden;
}

.exhibition-item:hover {
    transform: translateX(10px);
    border-color: #8b4a2a;
}

.ex-cover {
    width: 280px;
    height: 160px;
    background-size: cover;
    background-position: center;
    position: relative;
}

.ex-status-tag {
    position: absolute;
    top: 10px;
    left: 10px;
    background: rgba(0, 0, 0, 0.6);
    color: #fff;
    padding: 3px 10px;
    font-size: 11px;
}

.ex-details {
    flex: 1;
    padding: 20px;
    display: flex;
    flex-direction: column;
}

.ex-title {
    margin: 0 0 8px;
    font-size: 20px;
    color: #2c2825;
}

.ex-intro {
    font-size: 14px;
    color: #7a6e65;
    line-height: 1.5;
    margin-bottom: 12px;

    /* 解决警告：加上标准属性 */
    display: -webkit-box;
    -webkit-line-clamp: 2;
    line-clamp: 2;
    /* 加上这一行 */
    -webkit-box-orient: vertical;

    overflow: hidden;
}

.ex-meta {
    margin-top: auto;
    display: flex;
    gap: 20px;
    font-size: 12px;
    color: #9b9089;
}

.ex-arrow {
    width: 100px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    color: #8b4a2a;
    border-left: 1px dashed #e2ddd6;
    font-weight: bold;
}

.empty-state {
    text-align: center;
    padding: 100px 0;
    color: #9b9089;
    letter-spacing: 2px;
}
</style>