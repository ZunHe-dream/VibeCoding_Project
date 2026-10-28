<template>
    <div class="notice-wrapper">
        <!-- 顶部装饰横幅 -->
        <div class="page-hero">
            <div class="hero-pattern"></div>
            <div class="hero-content">
                <div class="hero-eyebrow">· NOTICE · 公告栏 ·</div>
                <h1 class="page-title">公告通知 · 动态讯息</h1>
                <p class="page-subtitle">聚合各馆精彩展览、讲座资讯及官方重要动态</p>
            </div>
            <div class="hero-ornament left">❧</div>
            <div class="hero-ornament right">❧</div>
        </div>

        <div class="notice-container">
            <!-- 搜索栏 -->
            <div class="search-section">
                <div class="search-bar">
                    <el-input v-model="queryParams.title" placeholder="搜索公告标题关键词…" class="search-input" clearable
                        @keyup.enter="handleSearch">
                        <template #prefix>
                            <el-icon>
                                <Search />
                            </el-icon>
                        </template>
                    </el-input>
                    <el-input v-model="queryParams.authorBelong" placeholder="发布单位…" class="search-input" clearable
                        @keyup.enter="handleSearch">
                        <template #prefix>
                            <el-icon>
                                <OfficeBuilding />
                            </el-icon>
                        </template>
                    </el-input>
                    <el-button @click="handleSearch" round
                        style="background-color:#7e5c3b; border-color:#7e5c3b; color:#fff; padding: 0 24px;">
                        查询
                    </el-button>
                    <el-button @click="resetSearch" round>重置</el-button>
                </div>
            </div>

            <!-- 公告列表 -->
            <div class="notice-list">
                <el-skeleton :loading="loading" animated :count="3">
                    <template #template>
                        <div class="notice-card skeleton-card">
                            <el-skeleton-item variant="p" style="width:30%; margin-bottom:10px" />
                            <el-skeleton-item variant="h1" style="width:60%; margin-bottom:15px" />
                            <el-skeleton-item variant="text" :rows="3" />
                        </div>
                    </template>

                    <template #default>
                        <el-empty v-if="noticeList.length === 0" description="暂无最新动态" style="padding: 80px 0" />

                        <div v-for="(item, idx) in noticeList" :key="item.noticeId" class="notice-card"
                            :class="{ 'is-top': item.isTop === 1 }" :style="{ animationDelay: idx * 0.08 + 's' }">

                            <!-- 置顶标签 -->
                            <div v-if="item.isTop === 1" class="top-ribbon">
                                <el-icon>
                                    <CollectionTag />
                                </el-icon> 置顶
                            </div>

                            <!-- 左侧序号装饰 -->
                            <div class="card-index">{{ String(idx + 1).padStart(2, '0') }}</div>

                            <div class="card-body">
                                <div class="card-meta-row">
                                    <span class="notice-tag" :class="getTypeTag(item.noticeType)">
                                        {{ item.noticeType || '常规公告' }}
                                    </span>
                                    <span class="create-time">
                                        <el-icon>
                                            <Calendar />
                                        </el-icon>
                                        {{ formatTime(item.createTime) }}
                                    </span>
                                </div>

                                <h3 class="notice-title" @click="viewDetail(item)">
                                    {{ item.title }}
                                </h3>

                                <p class="notice-intro">{{ getBrief(item.content) }}</p>

                                <div class="card-footer">
                                    <div class="author-info">
                                        <el-icon>
                                            <LocationInformation />
                                        </el-icon>
                                        <span class="author-name">{{ item.authorName || '官方发布' }}</span>
                                        <span class="author-sep">·</span>
                                        <span class="author-belong">{{ item.authorBelong || '官方发布' }}</span>
                                    </div>
                                    <button class="detail-btn" @click="viewDetail(item)">
                                        查阅详情 <span class="btn-arrow">→</span>
                                    </button>
                                </div>
                            </div>
                        </div>
                    </template>
                </el-skeleton>
            </div>
        </div>

        <!-- 详情弹窗 -->
        <el-dialog v-model="detailVisible" :title="currentNotice.title" width="65%" center destroy-on-close
            class="custom-dialog">
            <div class="detail-meta">
                <span class="detail-tag" :class="getTypeTag(currentNotice.noticeType)">
                    {{ currentNotice.noticeType }}
                </span>
                <el-divider direction="vertical" />
                <span><el-icon>
                        <Calendar />
                    </el-icon> {{ formatTime(currentNotice.createTime) }}</span>
                <el-divider direction="vertical" />
                <span><el-icon>
                        <LocationInformation />
                    </el-icon> {{ currentNotice.authorName }}</span>
            </div>
            <div class="detail-divider"></div>
            <div class="detail-content html-content" v-html="currentNotice.content"></div>
            <template #footer>
                <div class="dialog-footer">
                    <el-button @click="detailVisible = false" class="read-btn">已阅</el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import {
    ArrowRight, Calendar, Notification, EditPen,
    CollectionTag, LocationInformation, Search, OfficeBuilding
} from '@element-plus/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const noticeList = ref([])
const detailVisible = ref(false)
const currentNotice = ref({})
const queryParams = reactive({ title: '', authorBelong: '' })

const fetchNotices = async () => {
    loading.value = true
    try {
        const res = await request.get('/notice/list')
        let data = Array.isArray(res) ? res : (res.data || res)
        if (queryParams.title || queryParams.authorBelong) {
            data = data.filter(item => {
                const matchTitle = item.title.toLowerCase().includes(queryParams.title.toLowerCase())
                const matchBelong = item.authorBelong?.includes(queryParams.authorBelong) || !queryParams.authorBelong
                return matchTitle && matchBelong
            })
        }
        noticeList.value = data
    } catch (e) {
        ElMessage.error('公告加载失败')
    } finally {
        setTimeout(() => loading.value = false, 400)
    }
}

const handleSearch = () => fetchNotices()
const resetSearch = () => { queryParams.title = ''; queryParams.authorBelong = ''; fetchNotices() }

const getTypeTag = (type) => {
    const map = { '系统公告': 'tag-info', '展览变动': 'tag-warning', '紧急通知': 'tag-danger', '讲座活动': 'tag-success' }
    return map[type] || 'tag-info'
}

const formatTime = (timeStr) => {
    if (!timeStr) return ''
    return timeStr.replace('T', ' ').substring(0, 16)
}

const getBrief = (content) => {
    if (!content) return ''
    const pureText = content.replace(/<[^>]+>/g, '').trim()
    return pureText.length > 130 ? pureText.substring(0, 130) + '...' : pureText
}

const viewDetail = (item) => {
    if (!item.content) return
    const txt = document.createElement('textarea')
    txt.innerHTML = item.content
    let content = txt.value
    content = content.replace(/<\s+/g, '<').replace(/\s+>/g, '>')
    currentNotice.value = { ...item, content }
    detailVisible.value = true
}

onMounted(() => fetchNotices())
</script>

<style scoped>
.notice-wrapper {
    min-height: 100vh;
    background-color: #fcfaf2;
    font-family: 'Noto Serif SC', "Hiragino Sans GB", "Microsoft YaHei", serif;
}

/* Hero 横幅 */
.page-hero {
    position: relative;
    background: linear-gradient(135deg, #6b1414 0%, #8b1a1a 60%, #7a1a0a 100%);
    padding: 52px 24px 48px;
    text-align: center;
    overflow: hidden;
}

.hero-pattern {
    position: absolute;
    inset: 0;
    background-image:
        repeating-linear-gradient(45deg, rgba(255, 255, 255, 0.02) 0px, rgba(255, 255, 255, 0.02) 1px, transparent 1px, transparent 12px),
        repeating-linear-gradient(-45deg, rgba(255, 255, 255, 0.02) 0px, rgba(255, 255, 255, 0.02) 1px, transparent 1px, transparent 12px);
    pointer-events: none;
}

.hero-content {
    position: relative;
    z-index: 1;
}

.hero-eyebrow {
    font-size: 12px;
    letter-spacing: 5px;
    color: rgba(255, 255, 255, 0.5);
    margin-bottom: 14px;
    font-family: 'Georgia', serif;
}

.page-title {
    font-size: 30px;
    font-weight: 700;
    color: #f5e6c8;
    margin: 0 0 12px;
    letter-spacing: 3px;
}

.page-subtitle {
    color: rgba(255, 255, 255, 0.6);
    font-size: 14px;
    letter-spacing: 1px;
}

.hero-ornament {
    position: absolute;
    top: 50%;
    transform: translateY(-50%);
    font-size: 48px;
    color: rgba(255, 255, 255, 0.06);
    font-family: 'Georgia', serif;
    user-select: none;
}

.hero-ornament.left {
    left: 40px;
}

.hero-ornament.right {
    right: 40px;
    transform: translateY(-50%) scaleX(-1);
}

/* 主容器 */
.notice-container {
    max-width: 900px;
    margin: 0 auto;
    padding: 40px 20px 60px;
}

/* 搜索栏 */
.search-section {
    margin-bottom: 36px;
}

.search-bar {
    display: flex;
    gap: 12px;
    align-items: center;
    background: #fff;
    border: 1px solid #e8dfd0;
    border-radius: 12px;
    padding: 16px 20px;
    box-shadow: 0 2px 12px rgba(139, 26, 26, 0.04);
}

.search-input {
    flex: 1;
}

/* 公告卡片 */
.notice-card {
    background: #fff;
    border-radius: 2px;
    margin-bottom: 20px;
    padding: 28px 28px 28px 80px;
    position: relative;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
    transition: all 0.3s ease;
    border: 1px solid #ede8df;
    border-left: 4px solid #e8dfd0;
    animation: cardIn 0.4s ease both;
    overflow: hidden;
}

@keyframes cardIn {
    from {
        opacity: 0;
        transform: translateY(10px);
    }

    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.notice-card:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 24px rgba(139, 26, 26, 0.08);
    border-left-color: #ccac7a;
}

.notice-card.is-top {
    border-left: 4px solid #8b1a1a;
    background: linear-gradient(135deg, #fffaf8, #fff);
}

/* 置顶标签 */
.top-ribbon {
    position: absolute;
    top: 0;
    right: 24px;
    background: #8b1a1a;
    color: #fff;
    font-size: 11px;
    padding: 4px 12px;
    border-radius: 0 0 6px 6px;
    font-weight: bold;
    display: flex;
    align-items: center;
    gap: 4px;
}

/* 序号装饰 */
.card-index {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28px;
    font-weight: 900;
    color: #f0e8da;
    font-family: 'Georgia', serif;
    border-right: 1px solid #f0e8da;
    background: #faf7f3;
}

.card-meta-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 14px;
}

.create-time {
    color: #bbb;
    font-size: 12px;
    display: flex;
    gap: 4px;
    align-items: center;
}

.notice-tag {
    padding: 3px 10px;
    border-radius: 2px;
    font-size: 11px;
    font-weight: bold;
    color: #fff;
    letter-spacing: 0.5px;
}

.tag-info {
    background-color: #cdcfd4;
}

.tag-warning {
    background-color: #e6a23c;
}

.tag-danger {
    background-color: #f56c6c;
}

.tag-success {
    background-color: #67c23a;
}

.notice-title {
    font-size: 19px;
    color: #2c1a0e;
    margin-bottom: 10px;
    cursor: pointer;
    font-weight: 600;
    line-height: 1.5;
    transition: color 0.2s;
}

.notice-title:hover {
    color: #8b1a1a;
}

.notice-intro {
    color: #6b6b6b;
    line-height: 1.8;
    margin-bottom: 18px;
    font-size: 14px;
    text-align: justify;
}

.card-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-top: 1px dashed #ede8df;
    padding-top: 14px;
}

.author-info {
    color: #999;
    font-size: 12px;
    display: flex;
    gap: 6px;
    align-items: center;
}

.author-name {
    color: #7a6040;
    font-weight: 500;
}

.author-sep {
    color: #d4c4a8;
}

.author-belong {
    color: #9a7c5a;
}

.detail-btn {
    background: none;
    border: 1px solid #ccac7a;
    color: #8b1a1a;
    font-size: 13px;
    font-weight: 600;
    padding: 6px 16px;
    border-radius: 2px;
    cursor: pointer;
    transition: all 0.2s;
    font-family: inherit;
    letter-spacing: 0.5px;
}

.detail-btn:hover {
    background: #8b1a1a;
    color: #f5e6c8;
    border-color: #8b1a1a;
}

.btn-arrow {
    transition: transform 0.2s;
    display: inline-block;
}

.detail-btn:hover .btn-arrow {
    transform: translateX(4px);
}

/* 详情弹窗 */
:deep(.custom-dialog .el-dialog__header) {
    padding-top: 30px;
}

:deep(.custom-dialog .el-dialog__title) {
    color: #8b1a1a;
    font-weight: bold;
    font-size: 20px;
    font-family: 'Noto Serif SC', serif;
}

.detail-meta {
    text-align: center;
    color: #999;
    margin: 10px 0 16px;
    font-size: 13px;
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 8px;
}

.detail-tag {
    padding: 2px 10px;
    border-radius: 2px;
    font-size: 11px;
    font-weight: bold;
    color: #fff;
}

.detail-divider {
    height: 1px;
    background: linear-gradient(to right, transparent, #ccac7a, transparent);
    margin: 0 20px 24px;
}

.html-content {
    line-height: 1.8;
    font-size: 15px;
    color: #333;
    padding: 0 15px;
}

:deep(.html-content p) {
    margin-bottom: 15px;
    text-indent: 2em;
}

:deep(.html-content img) {
    max-width: 100%;
    height: auto;
    border-radius: 4px;
    margin: 10px auto;
    display: block;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.detail-content :deep(img) {
    display: block !important;
    max-width: 100% !important;
    height: auto !important;
    margin: 15px auto !important;
    border-radius: 8px;
}

.detail-content :deep(p) {
    margin-bottom: 12px;
    line-height: 1.8;
}

.read-btn {
    background-color: #8b1a1a;
    color: white;
    border: none;
    padding: 10px 32px;
    border-radius: 2px;
    letter-spacing: 2px;
}

.read-btn:hover {
    background-color: #a02020;
    color: white;
}

.skeleton-card {
    min-height: 150px;
}
</style>