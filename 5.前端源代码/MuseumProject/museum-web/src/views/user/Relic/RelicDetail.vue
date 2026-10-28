<template>
    <div class="detail-root" v-loading="loading">
        <!-- 返回栏 -->
        <div class="back-bar">
            <button class="back-btn" @click="$router.back()">
                <span class="back-arrow">←</span> 返回
            </button>
        </div>

        <div class="detail-layout" v-if="artifact">
            <!-- 左侧：图片区 -->
            <div class="image-col">
                <div class="image-frame">
                    <el-image :src="artifact.imageUrl" :preview-src-list="[artifact.imageUrl]" fit="contain"
                        class="main-img" preview-teleported />
                    <div class="img-hint">点击图片可放大查看</div>
                </div>

                <!-- 状态标记 -->
                <div class="status-block">
                    <div class="status-dot" :class="statusClass"></div>
                    <span class="status-text">{{ statusText }}</span>
                </div>
            </div>

            <!-- 右侧：信息区 -->
            <div class="info-col">
                <!-- 分类标签 -->
                <div class="tag-row">
                    <span class="dynasty-tag">{{ artifact.dynasty || '年代不详' }}</span>
                    <span class="cat-tag">{{ artifact.category || '未分类' }}</span>
                </div>

                <!-- 标题 -->
                <h1 class="artifact-name">{{ artifact.artifactName }}</h1>

                <!-- 分割线 -->
                <div class="title-rule">
                    <div class="rule-line"></div>
                    <div class="rule-diamond">◆</div>
                    <div class="rule-line"></div>
                </div>

                <!-- 核心信息 -->
                <div class="specs-grid">
                    <div class="spec-cell">
                        <div class="spec-label">作　者</div>
                        <div class="spec-value">{{ artifact.author || '佚名' }}</div>
                    </div>
                    <div class="spec-cell">
                        <div class="spec-label">尺　寸</div>
                        <div class="spec-value">{{ artifact.dimension || '详见实物' }}</div>
                    </div>
                    <div class="spec-cell">
                        <div class="spec-label">材　质</div>
                        <div class="spec-value">{{ artifact.material || '—' }}</div>
                    </div>
                    <div class="spec-cell">
                        <div class="spec-label">馆藏位置</div>
                        <div class="spec-value">{{ artifact.storageLocation || '—' }}</div>
                    </div>
                    <div class="spec-cell" v-if="artifact.ownerMuseum">
                        <div class="spec-label">所属博物馆</div>
                        <div class="spec-value">{{ artifact.ownerMuseum }}</div>
                    </div>
                </div>

                <!-- 历史简介 -->
                <div class="desc-section">
                    <div class="desc-header">
                        <span class="desc-icon">📜</span>
                        <span class="desc-title">历史详情</span>
                    </div>
                    <div class="desc-body">
                        {{ artifact.description || '暂无详细历史记载，此文物静默千年，等待后人解读。' }}
                    </div>
                </div>

                <!-- 收藏按钮 -->
                <div class="action-row">
                    <button class="fav-btn" :class="{ 'is-fav': isFavorited }" :disabled="favLoading"
                        @click="toggleFavorite">
                        <span class="fav-icon">{{ isFavorited ? '★' : '☆' }}</span>
                        <span>{{ isFavorited ? '已收藏' : '收藏此文物' }}</span>
                    </button>
                </div>
            </div>
        </div>

        <!-- 空状态 -->
        <div v-if="!artifact && !loading" class="empty-state">
            <div style="font-size:48px">🏺</div>
            <div>文物信息未找到</div>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { Star, StarFilled } from '@element-plus/icons-vue'

const route = useRoute()
const relicDetail = ref({})
const artifact = ref(null)
const loading = ref(false)
const isFavorited = ref(false)
const favLoading = ref(false)

const statusClass = computed(() => {
    const s = artifact.value?.status
    return s === 0 ? 'dot-in' : s === 1 ? 'dot-out' : 'dot-repair'
})

const statusText = computed(() => {
    const s = artifact.value?.status
    return s === 0 ? '馆藏中' : s === 1 ? '外借巡展中' : '维修保养中'
})

const getUserId = () => {
    const userStr = sessionStorage.getItem('user')
    return userStr ? JSON.parse(userStr).userId : null
}

const loadDetail = async () => {
    loading.value = true
    try {
        const id = route.params.id
        const res = await axios.get(`http://localhost:8080/artifact/${id}`)
        relicDetail.value = id.data
        if (res.data.code === 200) artifact.value = res.data.data
    } catch (error) {
        console.error('加载失败', error)
    } finally {
        loading.value = false
    }
}

const checkFavoriteStatus = async () => {
    const userId = getUserId()
    if (!userId) return
    try {
        const res = await axios.get(`http://localhost:8080/favorite/artifacts`, { params: { userId } })
        if (res.data.code === 200) {
            const list = res.data.data
            const currentId = Number(route.params.id)
            isFavorited.value = list.some(item => item.artifact_id === currentId)
        }
    } catch (error) {
        console.error('查询收藏状态失败', error)
    }
}

const toggleFavorite = async () => {
    const userId = getUserId()
    if (!userId) { ElMessage.warning('请先登录后再收藏'); return }
    favLoading.value = true
    try {
        const res = await axios.post(`http://localhost:8080/favorite/artifact/toggle`, {
            userId, artifactId: Number(route.params.id)
        })
        if (res.data.code === 200) {
            isFavorited.value = !isFavorited.value
            ElMessage.success(isFavorited.value ? '收藏成功 ✨' : '已取消收藏')
        }
    } catch (error) {
        ElMessage.error('操作失败，请稍后重试')
    } finally {
        favLoading.value = false
    }
}

onMounted(async () => {
    await loadDetail()
    if (relicDetail.value && relicDetail.value.name) {
        document.title = `文物详情 - ${relicDetail.value.name}`
    } else {
        document.title = '文物详情'
    }
    await checkFavoriteStatus()
})

</script>

<style scoped>
.detail-root {
    min-height: 100vh;
    background: #faf8f4;
    font-family: 'Noto Serif SC', 'Noto Sans SC', serif;
    padding: 0 0 60px;
}

/* 返回栏 */
.back-bar {
    padding: 20px 48px;
    border-bottom: 1px solid #e8dfd0;
    background: #fff;
}

.back-btn {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    background: none;
    border: none;
    font-size: 14px;
    color: #8b5e3c;
    cursor: pointer;
    font-family: inherit;
    padding: 6px 0;
    transition: color 0.2s;
}

.back-btn:hover {
    color: #8b1a1a;
}

.back-arrow {
    font-size: 18px;
}

/* 主布局 */
.detail-layout {
    max-width: 1100px;
    margin: 0 auto;
    padding: 48px 48px 0;
    display: grid;
    grid-template-columns: 420px 1fr;
    gap: 64px;
    align-items: start;
}

/* 图片区 */
.image-col {
    position: sticky;
    top: 24px;
}

.image-frame {
    background: #fff;
    border: 1px solid #e8dfd0;
    border-radius: 4px;
    overflow: hidden;
    box-shadow: 0 4px 20px rgba(44, 26, 14, 0.08);
    position: relative;
}

.main-img {
    width: 100%;
    height: 420px;
    display: block;
    transition: transform 0.4s ease;
    cursor: zoom-in;
}

.main-img:hover {
    transform: scale(1.03);
}

.img-hint {
    text-align: center;
    font-size: 11px;
    color: #c0a888;
    padding: 8px 0;
    background: #faf8f4;
    border-top: 1px solid #f0e8da;
    letter-spacing: 0.5px;
}

.status-block {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 16px;
    padding: 10px 16px;
    background: #fff;
    border-radius: 4px;
    border: 1px solid #e8dfd0;
}

.status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex-shrink: 0;
}

.dot-in {
    background: #52c41a;
    box-shadow: 0 0 0 3px rgba(82, 196, 26, 0.15);
}

.dot-out {
    background: #8b1a1a;
    box-shadow: 0 0 0 3px rgba(139, 26, 26, 0.15);
}

.dot-repair {
    background: #faad14;
    box-shadow: 0 0 0 3px rgba(250, 173, 20, 0.15);
}

.status-text {
    font-size: 13px;
    color: #5c3d1e;
}

/* 信息区 */
.info-col {
    padding-top: 4px;
}

.tag-row {
    display: flex;
    gap: 10px;
    margin-bottom: 20px;
}

.dynasty-tag {
    font-size: 12px;
    padding: 4px 14px;
    border-radius: 2px;
    background: #2c1a0e;
    color: #c8a96e;
    letter-spacing: 1px;
    font-weight: 600;
}

.cat-tag {
    font-size: 12px;
    padding: 4px 14px;
    border-radius: 2px;
    background: #f5efe6;
    color: #8b5e3c;
    border: 1px solid #e8dfd0;
}

.artifact-name {
    font-size: 34px;
    font-weight: 700;
    color: #1a0a0a;
    margin: 0 0 24px;
    line-height: 1.3;
    letter-spacing: 2px;
}

/* 装饰分割线 */
.title-rule {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 32px;
}

.rule-line {
    flex: 1;
    height: 1px;
    background: linear-gradient(to right, #e8dfd0, transparent);
}

.rule-line:last-child {
    background: linear-gradient(to left, #e8dfd0, transparent);
}

.rule-diamond {
    font-size: 10px;
    color: #c8a96e;
}

/* 规格表格 */
.specs-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1px;
    background: #e8dfd0;
    border: 1px solid #e8dfd0;
    border-radius: 4px;
    overflow: hidden;
    margin-bottom: 36px;
}

.spec-cell {
    background: #fff;
    padding: 14px 18px;
}

.spec-cell:nth-child(odd):last-child {
    grid-column: span 2;
}

.spec-label {
    font-size: 11px;
    color: #c0a888;
    letter-spacing: 2px;
    margin-bottom: 5px;
    text-transform: uppercase;
}

.spec-value {
    font-size: 15px;
    color: #2c1a0e;
    font-weight: 500;
}

/* 历史描述 */
.desc-section {
    margin-bottom: 40px;
}

.desc-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 14px;
}

.desc-icon {
    font-size: 16px;
}

.desc-title {
    font-size: 14px;
    font-weight: 700;
    color: #5c3d1e;
    letter-spacing: 1px;
    padding-bottom: 6px;
    border-bottom: 2px solid #c8a96e;
}

.desc-body {
    font-size: 15px;
    color: #4a3728;
    line-height: 2;
    text-align: justify;
    white-space: pre-wrap;
    padding: 20px;
    background: #fff;
    border-radius: 4px;
    border: 1px solid #e8dfd0;
    border-left: 3px solid #c8a96e;
}

/* 收藏按钮 */
.action-row {
    display: flex;
    gap: 14px;
}

.fav-btn {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 12px 32px;
    border-radius: 2px;
    border: 1px solid #c8a96e;
    background: #fff;
    color: #8b5e3c;
    font-size: 15px;
    font-weight: 600;
    font-family: inherit;
    cursor: pointer;
    letter-spacing: 1px;
    transition: all 0.25s;
}

.fav-btn:hover {
    background: #8b1a1a;
    border-color: #8b1a1a;
    color: #f5e6c8;
}

.fav-btn.is-fav {
    background: #8b1a1a;
    border-color: #8b1a1a;
    color: #f5e6c8;
}

.fav-icon {
    font-size: 18px;
}

/* 空状态 */
.empty-state {
    text-align: center;
    padding: 120px 40px;
    color: #9a7c5a;
    font-size: 16px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;
}
</style>