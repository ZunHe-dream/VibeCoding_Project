<template>
    <div class="relic-list-container">

        <!-- 顶部返回栏 -->
        <div class="page-header">
            <button class="back-btn" @click="$router.push('/user/relic')">
                <span class="back-arrow">←</span>
                <span>返回</span>
            </button>
            <div class="header-title-group">
                <span class="header-eyebrow">文物名录</span>
                <h1 class="header-title">{{ categoryName }} <span class="header-title-accent">分区</span></h1>
            </div>
            <div class="header-seal">{{ categoryName ? categoryName[0] : '文' }}</div>
        </div>

        <!-- 搜索栏 -->
        <div class="search-wrapper">
            <el-input v-model="searchKey" placeholder="搜索该分类下的文物名称/作者/朝代/馆藏地..." clearable @clear="getRelicData"
                @keyup.enter="getRelicData" class="search-input">
                <template #prefix>
                    <el-icon>
                        <Search />
                    </el-icon>
                </template>
                <template #append>
                    <el-button @click="getRelicData">搜索</el-button>
                </template>
            </el-input>
        </div>

        <div v-loading="loading">
            <el-row :gutter="28" v-if="relics.length > 0">
                <el-col :lg="6" :md="8" :sm="12" :xs="24" v-for="item in relics" :key="item.artifactId">
                    <div class="relic-card" @click="goToDetail(item.artifactId)">
                        <!-- 图片区 -->
                        <div class="relic-img-box">
                            <img :src="item.imageUrl" class="relic-img" v-if="item.imageUrl" />
                            <div v-else class="no-img">
                                <span class="no-img-char">無</span>
                                <span class="no-img-text">暂无图片</span>
                            </div>
                            <!-- 悬浮遮罩 -->
                            <div class="img-overlay">
                                <span class="overlay-text">查看详情 →</span>
                            </div>
                            <!-- 朝代角标 -->
                            <div class="dynasty-badge">{{ item.dynasty }}</div>
                        </div>

                        <!-- 内容区 -->
                        <div class="card-body">
                            <h4 class="relic-name">{{ item.artifactName }}</h4>

                            <div class="relic-meta">
                                <span class="meta-author" v-if="item.author">{{ item.author }}</span>
                                <span class="meta-author muted" v-else>佚名</span>
                            </div>

                            <div class="museum-row">
                                <el-icon>
                                    <OfficeBuilding />
                                </el-icon>
                                <span>{{ item.ownerMuseum || '馆藏地未知' }}</span>
                            </div>

                            <!-- 底部装饰线（hover 时变色） -->
                            <div class="card-bottom-line"></div>
                        </div>
                    </div>
                </el-col>
            </el-row>

            <div class="empty-wrapper" v-else>
                <div class="empty-char">空</div>
                <p class="empty-text">该分类下暂无文物数据</p>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, watch,onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, OfficeBuilding } from '@element-plus/icons-vue'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const categoryName = ref('')
const relics = ref([])
const loading = ref(false)
const searchKey = ref('')

const getRelicData = async () => {
    try {
        const res = await axios.get(`http://localhost:8080/artifact/list`, {
            params: {
                category: categoryName.value,
                name: searchKey.value
            }
        })
        if (res.data.code === 200) {
            relics.value = res.data.data
        }
    } catch (error) {
        console.error("搜索失败：", error)
    } finally {
        loading.value = false
    }
}
let timer = null
watch(searchKey, () => {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
        getRelicData()
    }, 300)
})

const goToDetail = (id) => {
    router.push(`/user/relic-detail/${id}`)
}

onMounted(() => {
    categoryName.value = route.query.category
    getRelicData()
})

onMounted(() => {
    // 页面进入时，找到那个灰色容器改掉它
    const mainDom = document.querySelector('.main-content')
    if (mainDom) {
        mainDom.style.backgroundColor = '#d0d3cb'
    }
})

onUnmounted(() => {
    // 页面离开时，还原成原来的灰色，避免影响其他页面
    const mainDom = document.querySelector('.main-content')
    if (mainDom) {
        mainDom.style.backgroundColor = '#f0f2f5'
    }
})

</script>

<style scoped>
/* ── 全局容器 ── */
.relic-list-container {
    padding: 36px 40px 60px;
    max-width: 1280px;
    margin: 0 auto;
    background: #f7f4ef;
    min-height: 100vh;
    font-family: "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
}

/* ── 顶部 Header ── */
.page-header {
    display: flex;
    align-items: center;
    gap: 20px;
    margin-bottom: 36px;
    padding-bottom: 24px;
    border-bottom: 1px solid #e2ddd6;
    position: relative;
}

.back-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    background: none;
    border: 1px solid #c9c4bb;
    border-radius: 4px;
    padding: 6px 14px;
    font-size: 13px;
    color: #6b6560;
    cursor: pointer;
    transition: all 0.2s;
    flex-shrink: 0;
}

.back-btn:hover {
    background: #ede9e2;
    color: #2c2825;
    border-color: #a09890;
}

.back-arrow {
    font-size: 16px;
    line-height: 1;
}

.header-title-group {
    flex: 1;
}

.header-eyebrow {
    font-size: 11px;
    letter-spacing: 4px;
    color: #9b9089;
    text-transform: uppercase;
    display: block;
    margin-bottom: 4px;
}

.header-title {
    font-size: 26px;
    font-weight: 500;
    color: #1e1a17;
    margin: 0;
    letter-spacing: 2px;
}

.header-title-accent {
    color: #8b4a2a;
}

/* 印章装饰 */
.header-seal {
    width: 48px;
    height: 48px;
    border: 2px solid #8b4a2a;
    border-radius: 4px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    color: #8b4a2a;
    font-weight: 500;
    letter-spacing: 0;
    flex-shrink: 0;
    opacity: 0.85;
}

/* ── 搜索栏 ── */
.search-wrapper {
    margin-bottom: 32px;
    max-width: 500px;
}

.search-input :deep(.el-input__wrapper) {
    background: #fff;
    border: 1px solid #ddd8d0;
    border-radius: 4px;
    box-shadow: none !important;
}

.search-input :deep(.el-input__wrapper:hover),
.search-input :deep(.el-input__wrapper.is-focus) {
    border-color: #8b4a2a;
}

.search-input :deep(.el-input-group__append) {
    background: #8b4a2a;
    border-color: #8b4a2a;
    color: #fff;
    border-radius: 0 4px 4px 0;
}

.search-input :deep(.el-input-group__append .el-button) {
    color: #fff;
}

/* ── 卡片 ── */
.relic-card {
    background: #fff;
    border-radius: 4px;
    overflow: hidden;
    margin-bottom: 28px;
    cursor: pointer;
    border: 1px solid #e8e4dc;
    transition: transform 0.3s ease, box-shadow 0.3s ease;
    display: flex;
    flex-direction: column;
}

.relic-card:hover {
    transform: translateY(-6px);
    box-shadow: 0 16px 40px rgba(60, 40, 20, 0.10);
}

/* ── 图片区 ── */
.relic-img-box {
    position: relative;
    width: 100%;
    height: 210px;
    overflow: hidden;
    background: #f0ece4;
}

.relic-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.5s ease;
    filter: saturate(0.9);
}

.relic-card:hover .relic-img {
    transform: scale(1.06);
}

/* 遮罩层 */
.img-overlay {
    position: absolute;
    inset: 0;
    background: rgba(30, 20, 10, 0.45);
    display: flex;
    align-items: center;
    justify-content: center;
    opacity: 0;
    transition: opacity 0.3s ease;
}

.relic-card:hover .img-overlay {
    opacity: 1;
}

.overlay-text {
    color: #fff;
    font-size: 14px;
    letter-spacing: 3px;
    border: 1px solid rgba(255, 255, 255, 0.6);
    padding: 8px 18px;
    border-radius: 2px;
}

/* 无图片占位 */
.no-img {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    background: #ede9e2;
}

.no-img-char {
    font-size: 32px;
    color: #c4bdb4;
    font-weight: 300;
}

.no-img-text {
    font-size: 12px;
    color: #b0a89e;
    letter-spacing: 2px;
}

/* 朝代角标 */
.dynasty-badge {
    position: absolute;
    top: 12px;
    left: 12px;
    background: rgba(20, 12, 6, 0.65);
    color: #f0e8d8;
    font-size: 11px;
    letter-spacing: 2px;
    padding: 3px 10px;
    border-radius: 2px;
    backdrop-filter: blur(4px);
}

/* ── 卡片内容 ── */
.card-body {
    padding: 16px 18px 0;
    display: flex;
    flex-direction: column;
    gap: 6px;
    flex: 1;
}

.relic-name {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: #1e1a17;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    letter-spacing: 1px;
}

.relic-meta {
    display: flex;
    align-items: center;
    gap: 8px;
}

.meta-author {
    font-size: 13px;
    color: #7a6e65;
}

.meta-author.muted {
    color: #b0a89e;
    font-style: italic;
}

.museum-row {
    display: flex;
    align-items: center;
    gap: 5px;
    font-size: 12px;
    color: #a09890;
    margin-bottom: 0;
}

/* 底部装饰线 */
.card-bottom-line {
    height: 3px;
    margin-top: 14px;
    background: #e8e4dc;
    transition: background 0.3s ease;
}

.relic-card:hover .card-bottom-line {
    background: #8b4a2a;
}

/* ── 空状态 ── */
.empty-wrapper {
    text-align: center;
    padding: 80px 0;
}

.empty-char {
    font-size: 64px;
    color: #d4cdc4;
    margin-bottom: 16px;
    font-weight: 300;
}

.empty-text {
    font-size: 14px;
    color: #a09890;
    letter-spacing: 3px;
}


</style>