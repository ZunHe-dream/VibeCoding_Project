<template>
    <div class="booking-wrapper">
        <div class="booking-container">
            <div class="page-header">
                <h2 class="title">
                    <el-icon class="header-icon">
                        <Ticket />
                    </el-icon>
                    <span>我的预约行程</span>
                </h2>
                <p class="subtitle">管理您的参观计划，传承文化薪火</p>
            </div>

            <el-tabs v-model="activeTab" class="custom-tabs">
                <el-tab-pane label="待参观" name="upcoming"></el-tab-pane>
                <el-tab-pane label="历史记录" name="history"></el-tab-pane>
                <el-tab-pane label="已取消" name="cancelled"></el-tab-pane>
            </el-tabs>

            <div v-loading="loading" class="list-section">
                <el-empty v-if="filteredList.length === 0" :image-size="200" description="这里空空如也，快去预约展览吧" />

                <div v-for="item in filteredList" :key="item.resId" class="booking-card">
                    <div class="card-cover">
                        <el-image :src="item.coverImage" fit="cover" class="cover-img">
                            <template #error>
                                <div class="img-placeholder">
                                    <span>文化博览</span>
                                </div>
                            </template>
                        </el-image>
                        <div class="status-badge" :class="statusClass(item.status)">
                            {{ statusText(item.status) }}
                        </div>
                    </div>

                    <div class="card-info">
                        <div class="info-main">
                            <h3 class="exhibition-title">{{ item.exhibitionTitle || '精品巡回展' }}</h3>
                            <div class="info-grid">
                                <div class="info-item">
                                    <el-icon>
                                        <Location />
                                    </el-icon>
                                    <span class="label">地点：</span>
                                    <span class="value">{{ item.location }}</span>
                                </div>
                                <div class="info-item date-highlight">
                                    <el-icon>
                                        <Calendar />
                                    </el-icon>
                                    <span class="label">日期：</span>
                                    <span class="value">{{ item.reserveDate }}</span>
                                </div>
                                <div class="info-item">
                                    <el-icon>
                                        <Timer />
                                    </el-icon>
                                    <span class="label">下单：</span>
                                    <span class="value">{{ formatTime(item.createTime) }}</span>
                                </div>
                            </div>
                        </div>
                        <div class="ticket-line"></div>
                    </div>

                    <div class="card-action">
                        <div class="res-id">NO.{{ item.resId }}</div>
                        <el-button v-if="item.status === 0" type="danger" round
                            @click="handleCancel(item.resId)">取消预约</el-button>
                        <el-button v-if="item.status === 1" type="success" round disabled>已核销</el-button>
                        <el-button v-if="item.status === 2" type="info" round disabled>已作废</el-button>

                        <el-button type="primary" link @click="goDetail(item.resId)" class="detail-link">
                            入场凭证 <el-icon>
                                <ArrowRight />
                            </el-icon>
                        </el-button>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Ticket, Location, Calendar, Timer, ArrowRight } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const loading = ref(false)
const activeTab = ref('upcoming')
const reservationList = ref([])

// 获取用户ID
const userStr = sessionStorage.getItem('user')
const userId = userStr ? JSON.parse(userStr).userId : 1

const fetchMyReservations = async () => {
    loading.value = true
    try {
        const res = await request.get(`/reservation/my?userId=${userId}`)
        reservationList.value = res.data || res
    } catch (e) {
        ElMessage.error("记录加载失败")
    } finally {
        loading.value = false
    }
}

const filteredList = computed(() => {
    return reservationList.value.filter(item => {
        if (activeTab.value === 'upcoming') return item.status === 0
        if (activeTab.value === 'history') return item.status === 1
        if (activeTab.value === 'cancelled') return item.status === 2
        return true
    })
})

const statusText = (status) => {
    return { 0: '待参观', 1: '已入场', 2: '已取消' }[status] || '未知'
}

const statusClass = (status) => {
    return { 0: 'bg-blue', 1: 'bg-green', 2: 'bg-gray' }[status]
}

const formatTime = (time) => time ? time.replace('T', ' ').substring(0, 16) : '--'

const handleCancel = (resId) => {
    ElMessageBox.confirm('确定要取消预约吗？名额有限，取消后可能无法再次预约。', '风险提示', {
        confirmButtonText: '确定取消',
        cancelButtonText: '保留预约',
        type: 'warning'
    }).then(async () => {
        try {
            await request.post(`/reservation/cancel/${resId}`)
            ElMessage.success('预约已取消')
            fetchMyReservations()
        } catch (e) {
            ElMessage.error('操作失败')
        }
    })
}

/**
 * ✨ 跳转逻辑
 * @param resId 预约记录ID
 */
const goDetail = (resId) => {
    if (!resId) {
        ElMessage.warning("未找到预约编号")
        return
    }
    // 跳转到凭证页面
    router.push(`/user/ticket/${resId}`)
}

onMounted(() => fetchMyReservations())

</script>

<style scoped>
.booking-wrapper {
    min-height: 100vh;
    background-color: #fcfaf8;
    padding: 40px 20px;
}

.booking-container {
    max-width: 950px;
    margin: 0 auto;
}

.page-header {
    margin-bottom: 40px;
    border-left: 4px solid #8b1a1a;
    padding-left: 20px;
}

.page-header .title {
    font-size: 26px;
    color: #333;
    margin: 0;
    display: flex;
    align-items: center;
    font-weight: 600;
}

.header-icon {
    margin-right: 12px;
    color: #8b1a1a;
}

.page-header .subtitle {
    color: #999;
    margin-top: 8px;
    font-size: 14px;
}

.custom-tabs :deep(.el-tabs__nav-wrap::after) {
    height: 1px;
    background-color: #e4e7ed;
}

.custom-tabs :deep(.el-tabs__item) {
    font-size: 15px;
    font-weight: 500;
}

.booking-card {
    background: #fff;
    border-radius: 12px;
    display: flex;
    margin-bottom: 24px;
    height: 180px;
    box-shadow: 0 6px 18px rgba(0, 0, 0, 0.04);
    border: 1px solid #eee;
    transition: all 0.3s ease;
    position: relative;
}

.booking-card:hover {
    box-shadow: 0 10px 25px rgba(0, 0, 0, 0.08);
    transform: translateY(-2px);
}

.card-cover {
    width: 240px;
    height: 100%;
    position: relative;
    flex-shrink: 0;
}

.cover-img {
    width: 100%;
    height: 100%;
}

.img-placeholder {
    width: 100%;
    height: 100%;
    background: #e0e0e0;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #999;
    font-weight: bold;
}

.status-badge {
    position: absolute;
    top: 0;
    right: 0;
    padding: 6px 15px;
    color: #fff;
    font-size: 12px;
    font-weight: bold;
    border-bottom-left-radius: 12px;
    z-index: 5;
    box-shadow: -2px 2px 8px rgba(0, 0, 0, 0.1);
}

.bg-blue {
    background: linear-gradient(135deg, #409eff, #2e7ed1);
}

.bg-green {
    background: linear-gradient(135deg, #67c23a, #529b2e);
}

.bg-gray {
    background: #909399;
}

.card-info {
    flex: 1;
    padding: 25px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    position: relative;
}

.exhibition-title {
    margin: 0 0 15px 0;
    font-size: 19px;
    color: #2c3e50;
    font-weight: bold;
}

.info-grid {
    display: grid;
    gap: 10px;
}

.info-item {
    display: flex;
    align-items: center;
    font-size: 13px;
    color: #666;
    gap: 6px;
}

.info-item .label {
    color: #999;
}

.date-highlight {
    color: #8b1a1a;
    font-weight: 500;
}

.ticket-line {
    position: absolute;
    right: 0;
    top: 10%;
    height: 80%;
    border-right: 2px dashed #f0f0f0;
}

.card-action {
    width: 160px;
    background-color: #fafafa;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    gap: 15px;
    padding: 0 20px;
}

.res-id {
    font-family: 'Courier New', Courier, monospace;
    font-size: 12px;
    color: #ccc;
    font-weight: bold;
}

.detail-link {
    font-size: 13px;
    color: #666;
}
</style>