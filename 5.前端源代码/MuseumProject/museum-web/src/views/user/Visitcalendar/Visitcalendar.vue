<template>
    <div class="calendar-page">
        <!-- 头部 -->
        <div class="page-header">
            <div class="header-left">
                <h2 class="page-title">参观日历</h2>
                <p class="page-sub">您的展览预约一览</p>
            </div>
            <div class="header-stats">
                <div class="stat-item">
                    <span class="stat-num">{{ stats.total }}</span>
                    <span class="stat-label">全部预约</span>
                </div>
                <div class="stat-item">
                    <span class="stat-num blue">{{ stats.pending }}</span>
                    <span class="stat-label">待参观</span>
                </div>
                <div class="stat-item">
                    <span class="stat-num green">{{ stats.done }}</span>
                    <span class="stat-label">已参观</span>
                </div>
            </div>
        </div>

        <!-- 月份导航 -->
        <div class="month-nav">
            <el-button circle @click="changeMonth(-1)">‹</el-button>
            <span class="month-label">{{ currentYear }} 年 {{ currentMonth + 1 }} 月</span>
            <el-button circle @click="changeMonth(1)">›</el-button>
            <el-button size="small" style="margin-left:12px" @click="goToday">今天</el-button>
        </div>

        <!-- 日历 -->
        <div class="calendar-wrap" v-loading="loading">
            <!-- 星期头 -->
            <div class="week-header">
                <div v-for="d in ['日', '一', '二', '三', '四', '五', '六']" :key="d" class="week-cell">{{ d }}</div>
            </div>
            <!-- 日期格子 -->
            <div class="calendar-grid">
                <div v-for="cell in calendarCells" :key="cell.key" :class="['day-cell',
                    { 'other-month': !cell.currentMonth },
                    { 'today': cell.isToday },
                    { 'has-event': cell.reservations.length > 0 }
                ]">
                    <div class="day-num">{{ cell.day }}</div>
                    <div class="event-list">
                        <div v-for="res in cell.reservations.slice(0, 3)" :key="res.resId"
                            :class="['event-tag', statusClass(res.status)]" @click="openDetail(res)">
                            <span class="event-dot"></span>
                            <span class="event-name">{{ res.exhibitionTitle || '展览' }}</span>
                        </div>
                        <div v-if="cell.reservations.length > 3" class="event-more"
                            @click="openDayDetail(cell.reservations)">
                            +{{ cell.reservations.length - 3 }} 项
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- 预约详情弹窗 -->
        <el-dialog v-model="detailVisible" title="预约详情" width="420px" destroy-on-close>
            <div v-if="detailRes" class="detail-wrap">
                <div :class="['detail-status-bar', statusClass(detailRes.status)]">
                    {{ statusText(detailRes.status) }}
                </div>
                <div class="detail-rows">
                    <div class="detail-row">
                        <span class="detail-label">展览名称</span>
                        <span class="detail-val">{{ detailRes.exhibitionTitle || '—' }}</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">参观日期</span>
                        <span class="detail-val">{{ detailRes.reserveDate }}</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">场次</span>
                        <span class="detail-val">{{ detailRes.timeSlot }}</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">票种</span>
                        <span class="detail-val">{{ detailRes.ticketType }}</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">参观人</span>
                        <span class="detail-val">{{ detailRes.userName }}</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">联系电话</span>
                        <span class="detail-val">{{ detailRes.phone || '—' }}</span>
                    </div>
                    <div class="detail-row" v-if="detailRes.remark">
                        <span class="detail-label">备注</span>
                        <span class="detail-val">{{ detailRes.remark }}</span>
                    </div>
                </div>

                <!-- 核销码 -->
                <div v-if="detailRes.status === 0" class="qr-section">
                    <div class="qr-label">入场核销码</div>
                    <div class="verify-code">{{ detailRes.verifyCode }}</div>
                    <div id="qrcode-container" class="qrcode-wrap"></div>
                    <p class="qr-tip">入场时出示此码供工作人员扫描核销</p>
                </div>
            </div>
        </el-dialog>

        <!-- 当天多条记录弹窗 -->
        <el-dialog v-model="dayDetailVisible" title="当日预约" width="400px">
            <div class="day-list">
                <div v-for="res in dayReservations" :key="res.resId" :class="['day-item', statusClass(res.status)]"
                    @click="openDetail(res); dayDetailVisible = false">
                    <div class="day-item-title">{{ res.exhibitionTitle }}</div>
                    <div class="day-item-meta">{{ res.timeSlot }} · {{ res.realName }} · {{ statusText(res.status) }}
                    </div>
                </div>
            </div>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const loading = ref(false)
const reservations = ref([])
const userId = JSON.parse(sessionStorage.getItem('user') || '{}').userId

const now = new Date()
const currentYear = ref(now.getFullYear())
const currentMonth = ref(now.getMonth())

const detailVisible = ref(false)
const detailRes = ref(null)
const dayDetailVisible = ref(false)
const dayReservations = ref([])

// 统计
const stats = computed(() => ({
    total: reservations.value.length,
    pending: reservations.value.filter(r => r.status === 0).length,
    done: reservations.value.filter(r => r.status === 1).length
}))

// 生成日历格子
const calendarCells = computed(() => {
    console.log('打印:',JSON.stringify(reservations.value[0]))
    const year = currentYear.value
    const month = currentMonth.value
    const firstDay = new Date(year, month, 1).getDay()
    const daysInMonth = new Date(year, month + 1, 0).getDate()
    const daysInPrev = new Date(year, month, 0).getDate()
    const todayStr = formatDate(new Date())
    const cells = []

    // 上月补位
    for (let i = firstDay - 1; i >= 0; i--) {
        const d = daysInPrev - i
        const dateStr = formatDate(new Date(year, month - 1, d))
        cells.push({ key: `prev-${d}`, day: d, currentMonth: false, isToday: false, dateStr, reservations: getResByDate(dateStr) })
    }
    // 本月
    for (let d = 1; d <= daysInMonth; d++) {
        const dateStr = formatDate(new Date(year, month, d))
        cells.push({ key: `cur-${d}`, day: d, currentMonth: true, isToday: dateStr === todayStr, dateStr, reservations: getResByDate(dateStr) })
    }
    // 下月补位
    const remaining = 42 - cells.length
    for (let d = 1; d <= remaining; d++) {
        const dateStr = formatDate(new Date(year, month + 1, d))
        cells.push({ key: `next-${d}`, day: d, currentMonth: false, isToday: false, dateStr, reservations: getResByDate(dateStr) })
    }
    return cells
})

const getResByDate = (dateStr) =>
    reservations.value.filter(r => r.reserveDate === dateStr)

const formatDate = (d) => {
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${y}-${m}-${day}`
}

const changeMonth = (delta) => {
    const d = new Date(currentYear.value, currentMonth.value + delta, 1)
    currentYear.value = d.getFullYear()
    currentMonth.value = d.getMonth()
}

const goToday = () => {
    currentYear.value = now.getFullYear()
    currentMonth.value = now.getMonth()
}

const statusText = (s) => ['待核销', '已参观', '已取消'][s] ?? '未知'
const statusClass = (s) => ['status-pending', 'status-done', 'status-cancel'][s] ?? ''

const openDetail = async (res) => {
    detailRes.value = res
    detailVisible.value = true
    if (res.status === 0 && res.verifyCode) {
        await nextTick()
        renderQR(res.verifyCode)
    }
}

const openDayDetail = (list) => {
    dayReservations.value = list
    dayDetailVisible.value = true
}

// 简单二维码渲染（用 QRServer API）
const renderQR = (code) => {
    const container = document.getElementById('qrcode-container')
    if (!container) return
    container.innerHTML = `<img src="https://api.qrserver.com/v1/create-qr-code/?size=160x160&data=${code}" width="160" height="160" />`
}

const fetchData = async () => {
    loading.value = true
    try {
        const res = await axios.get('/api/reservation/my', { params: { userId } })
        const data = res.data
        reservations.value = Array.isArray(data) ? data : (data?.data || [])
    } catch (e) {
        ElMessage.error('获取预约数据失败')
    } finally {
        loading.value = false
    }
}

onMounted(fetchData)
</script>

<style scoped>
.calendar-page {
    background-color: #fcfaf8;
    max-width: 960px;
    margin: 0 auto;
    padding: 28px 24px;
    font-family: 'Noto Sans SC', 'PingFang SC', sans-serif;
    box-shadow: 0 2px 20px rgba(0, 0, 0, 0.1);
    border-radius: 8px;
}

/* 头部 */
.page-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    margin-bottom: 20px;
}

.page-title {
    font-size: 22px;
    font-weight: 700;
    color: #733d1e;
    margin: 0 0 4px;
}

.page-sub {
    font-size: 13px;
    color: #999;
    margin: 0;
}

.header-stats {
    display: flex;
    gap: 20px;
}

.stat-item {
    text-align: center;
}

.stat-num {
    display: block;
    font-size: 22px;
    font-weight: 700;
    color: #1a1a2e;
}

.stat-num.blue {
    color: #3b82f6;
}

.stat-num.green {
    color: #22c55e;
}

.stat-label {
    font-size: 11px;
    color: #999;
}

/* 月份导航 */
.month-nav {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 16px;
}

.month-label {
    font-size: 16px;
    font-weight: 600;
    color: #1a1a2e;
    min-width: 120px;
    text-align: center;
}

/* 日历 */
.calendar-wrap {
    background: #fff;
    border-radius: 12px;
    border: 1px solid #e8e8e8;
    overflow: hidden;
}

.week-header {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    background: #f8f8f8;
    border-bottom: 1px solid #e8e8e8;
}

.week-cell {
    text-align: center;
    padding: 10px 0;
    font-size: 12px;
    color: #666;
    font-weight: 500;
}

.calendar-grid {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
}

.day-cell {
    min-height: 100px;
    border-right: 1px solid #f0f0f0;
    border-bottom: 1px solid #f0f0f0;
    padding: 6px;
    background: #fff;
    transition: background 0.15s;
}

.day-cell:nth-child(7n) {
    border-right: none;
}

.day-cell.other-month {
    background: #fafafa;
}

.day-cell.other-month .day-num {
    color: #ccc;
}

.day-cell.today .day-num {
    background: #3b82f6;
    color: #fff;
    border-radius: 50%;
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.day-cell.has-event {
    background: #fdfcff;
}

.day-num {
    font-size: 13px;
    color: #333;
    font-weight: 500;
    margin-bottom: 4px;
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
}

/* 事件标签 */
.event-list {
    display: flex;
    flex-direction: column;
    gap: 3px;
}

.event-tag {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 11px;
    cursor: pointer;
    overflow: hidden;
    transition: opacity 0.15s;
}

.event-tag:hover {
    opacity: 0.8;
}

.event-dot {
    width: 5px;
    height: 5px;
    border-radius: 50%;
    flex-shrink: 0;
}

.event-name {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    flex: 1;
}

.status-pending {
    background: #eff6ff;
    color: #3b82f6;
}

.status-pending .event-dot {
    background: #3b82f6;
}

.status-done {
    background: #f0fdf4;
    color: #22c55e;
}

.status-done .event-dot {
    background: #22c55e;
}

.status-cancel {
    background: #f9f9f9;
    color: #aaa;
}

.status-cancel .event-dot {
    background: #aaa;
}

.event-more {
    font-size: 11px;
    color: #999;
    cursor: pointer;
    padding: 1px 4px;
}

.event-more:hover {
    color: #3b82f6;
}

/* 详情弹窗 */
.detail-wrap {
    padding: 4px 0;
}

.detail-status-bar {
    text-align: center;
    padding: 8px;
    border-radius: 6px;
    font-size: 13px;
    font-weight: 600;
    margin-bottom: 16px;
}

.detail-status-bar.status-pending {
    background: #eff6ff;
    color: #3b82f6;
}

.detail-status-bar.status-done {
    background: #f0fdf4;
    color: #22c55e;
}

.detail-status-bar.status-cancel {
    background: #f9f9f9;
    color: #aaa;
}

.detail-rows {
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin-bottom: 16px;
}

.detail-row {
    display: flex;
    justify-content: space-between;
    font-size: 14px;
}

.detail-label {
    color: #999;
    flex-shrink: 0;
}

.detail-val {
    color: #1a1a2e;
    font-weight: 500;
    text-align: right;
}

.qr-section {
    text-align: center;
    border-top: 1px dashed #e8e8e8;
    padding-top: 16px;
}

.qr-label {
    font-size: 12px;
    color: #999;
    margin-bottom: 8px;
}

.verify-code {
    font-size: 20px;
    font-weight: 700;
    letter-spacing: 4px;
    color: #1a1a2e;
    margin-bottom: 12px;
    font-family: 'Courier New', monospace;
}

.qrcode-wrap {
    display: flex;
    justify-content: center;
    margin-bottom: 8px;
}

.qr-tip {
    font-size: 11px;
    color: #bbb;
}

/* 当天列表 */
.day-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.day-item {
    padding: 12px;
    border-radius: 8px;
    cursor: pointer;
    transition: opacity 0.15s;
}

.day-item:hover {
    opacity: 0.8;
}

.day-item-title {
    font-size: 14px;
    font-weight: 600;
    margin-bottom: 4px;
}

.day-item-meta {
    font-size: 12px;
    opacity: 0.7;
}
</style>