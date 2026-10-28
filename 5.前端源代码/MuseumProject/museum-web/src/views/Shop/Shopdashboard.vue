<template>
    <div class="dashboard">
        <!-- 顶部标题 -->
        <div class="dash-header">
            <div class="dash-title">
                <span class="title-icon">📊</span>
                <span>数据看板</span>
                <span class="museum-tag">{{ belongMuseum }}</span>
            </div>
            <el-button :icon="Refresh" circle @click="loadAll" :loading="loading" />
        </div>

        <!-- KPI Cards -->
        <div class="kpi-row">
            <div class="kpi-card" v-for="k in kpiCards" :key="k.label">
                <div class="kpi-icon">{{ k.icon }}</div>
                <div class="kpi-body">
                    <div class="kpi-value">{{ k.value }}</div>
                    <div class="kpi-label">{{ k.label }}</div>
                </div>
                <div class="kpi-badge" :style="{ background: k.color }"></div>
            </div>
        </div>

        <!-- 第二行：趋势图 + 分类占比 -->
        <div class="row-2">
            <!-- 近7天营收趋势 -->
            <div class="card trend-card">
                <div class="card-title">📈 近7天营收趋势</div>
                <div class="chart-area">
                    <div v-if="trendData.length === 0" class="no-data">暂无数据</div>
                    <svg v-else class="trend-svg" viewBox="0 0 520 160" preserveAspectRatio="none">
                        <!-- 网格线 -->
                        <line v-for="i in 4" :key="i" x1="40" :y1="i * 32" x2="520" :y2="i * 32" stroke="#e5e7eb"
                            stroke-width="1" />
                        <!-- Y轴标签 -->
                        <text v-for="i in 4" :key="'y' + i" x="35" :y="i * 32 + 4" fill="#9ca3af" font-size="9"
                            text-anchor="end">
                            {{ formatYLabel(trendMax * (4 - i) / 4) }}
                        </text>
                        <!-- 渐变填充 -->
                        <defs>
                            <linearGradient id="trendGrad" x1="0" y1="0" x2="0" y2="1">
                                <stop offset="0%" stop-color="#6366f1" stop-opacity="0.3" />
                                <stop offset="100%" stop-color="#6366f1" stop-opacity="0" />
                            </linearGradient>
                        </defs>
                        <polygon v-if="trendPoints.length" :points="trendFillPoints" fill="url(#trendGrad)" />
                        <polyline v-if="trendPoints.length" :points="trendPoints" fill="none" stroke="#6366f1"
                            stroke-width="2.5" stroke-linejoin="round" />
                        <!-- 数据点 -->
                        <circle v-for="(pt, i) in trendDots" :key="i" :cx="pt.x" :cy="pt.y" r="4" fill="#6366f1"
                            stroke="white" stroke-width="2" />
                        <!-- X轴日期 -->
                        <text v-for="(d, i) in trendData" :key="'x' + i" :x="40 + i * (480 / (trendData.length - 1 || 1))"
                            y="155" fill="#9ca3af" font-size="9" text-anchor="middle">
                            {{ d.date.slice(5) }}
                        </text>
                    </svg>
                </div>
            </div>

            <!-- 分类占比 -->
            <div class="card cat-card">
                <div class="card-title">🗂 商品类别占比</div>
                <div v-if="catData.length === 0" class="no-data">暂无数据</div>
                <div v-else class="cat-body">
                    <svg class="donut-svg" viewBox="0 0 120 120">
                        <circle cx="60" cy="60" r="45" fill="none" stroke="#f3f4f6" stroke-width="18" />
                        <circle v-for="(seg, i) in donutSegs" :key="i" cx="60" cy="60" r="45" fill="none"
                            :stroke="seg.color" stroke-width="18" :stroke-dasharray="seg.dash"
                            :stroke-dashoffset="seg.offset" stroke-linecap="butt"
                            style="transition: stroke-dashoffset 0.6s ease" />
                        <text x="60" y="64" text-anchor="middle" font-size="11" font-weight="bold" fill="#374151">
                            {{ catData.length }}类
                        </text>
                    </svg>
                    <div class="cat-legend">
                        <div v-for="(c, i) in catData" :key="i" class="legend-item">
                            <span class="legend-dot" :style="{ background: catColors[i % catColors.length] }"></span>
                            <span class="legend-name">{{ c.name }}</span>
                            <span class="legend-pct">{{ c.percent }}%</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- 第三行：销量排行 + 销售额排行 -->
        <div class="row-3">
            <div class="card rank-card">
                <div class="card-title">🏆 销量 Top 10</div>
                <div v-if="topSales.length === 0" class="no-data">暂无数据</div>
                <div v-else class="rank-list">
                    <div v-for="(item, i) in topSales" :key="item.productId" class="rank-item">
                        <span class="rank-no" :class="['rank-' + (i + 1)]">{{ i + 1 }}</span>
                        <img :src="item.coverUrl || 'https://via.placeholder.com/36'" class="rank-img" />
                        <span class="rank-name">{{ item.name }}</span>
                        <span class="rank-val">{{ item.quantity }} 件</span>
                        <div class="rank-bar-wrap">
                            <div class="rank-bar"
                                :style="{ width: (item.quantity / topSales[0].quantity * 100) + '%', background: i < 3 ? '#6366f1' : '#a5b4fc' }">
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="card rank-card">
                <div class="card-title">💰 销售额 Top 10</div>
                <div v-if="topRevenue.length === 0" class="no-data">暂无数据</div>
                <div v-else class="rank-list">
                    <div v-for="(item, i) in topRevenue" :key="item.productId" class="rank-item">
                        <span class="rank-no" :class="['rank-' + (i + 1)]">{{ i + 1 }}</span>
                        <img :src="item.coverUrl || 'https://via.placeholder.com/36'" class="rank-img" />
                        <span class="rank-name">{{ item.name }}</span>
                        <span class="rank-val">¥{{ Number(item.revenue).toFixed(0) }}</span>
                        <div class="rank-bar-wrap">
                            <div class="rank-bar"
                                :style="{ width: (item.revenue / topRevenue[0].revenue * 100) + '%', background: i < 3 ? '#f59e0b' : '#fcd34d' }">
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- 第四行：库存预警 + 最近订单流 -->
        <div class="row-4">
            <!-- 库存预警 -->
            <div class="card stock-card">
                <div class="card-title">
                    ⚠️ 库存预警
                    <el-badge v-if="lowStockList.length > 0" :value="lowStockList.length" type="danger"
                        style="margin-left:8px" />
                </div>
                <div v-if="lowStockList.length === 0" class="no-data no-warn">✅ 所有商品库存充足</div>
                <div v-else class="stock-list">
                    <div v-for="p in lowStockList" :key="p.productId" class="stock-item">
                        <img :src="p.coverUrl || 'https://via.placeholder.com/40'" class="stock-img" />
                        <div class="stock-info">
                            <div class="stock-name">{{ p.name }}</div>
                            <div class="stock-bar-wrap">
                                <div class="stock-bar-bg">
                                    <div class="stock-bar-fill"
                                        :style="{ width: Math.min(p.stock / (p.stockWarning * 2) * 100, 100) + '%', background: p.stock === 0 ? '#ef4444' : '#f97316' }">
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="stock-count" :class="{ 'out': p.stock === 0 }">
                            {{ p.stock === 0 ? '已售罄' : '剩 ' + p.stock + ' 件' }}
                        </div>
                    </div>
                </div>
            </div>

            <!-- 最近订单流 -->
            <div class="card orders-card">
                <div class="card-title">🔔 最近成交</div>
                <div v-if="recentOrders.length === 0" class="no-data">暂无数据</div>
                <div v-else class="orders-feed">
                    <div v-for="(o, i) in recentOrders" :key="o.orderId" class="feed-item"
                        :style="{ animationDelay: i * 0.08 + 's' }">
                        <img :src="o.coverUrl || 'https://via.placeholder.com/40'" class="feed-img" />
                        <div class="feed-info">
                            <div class="feed-product">{{ o.productName }}</div>
                            <div class="feed-meta">{{ o.address }} · {{ timeAgo(o.payTime) }}</div>
                        </div>
                        <div class="feed-price">¥{{ Number(o.totalPrice).toFixed(2) }}</div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import axios from 'axios'

const user = JSON.parse(sessionStorage.getItem('user') || '{}')
const belongMuseum = user.belong || ''

const loading = ref(false)
const kpi = ref({})
const trendData = ref([])
const catData = ref([])
const topSales = ref([])
const topRevenue = ref([])
const lowStockList = ref([])
const recentOrders = ref([])

const catColors = ['#6366f1', '#f59e0b', '#10b981', '#ef4444', '#3b82f6', '#8b5cf6', '#ec4899', '#14b8a6']

// KPI 卡片
const kpiCards = computed(() => [
    { label: '今日营收', value: '¥' + (Number(kpi.value.todayRevenue) || 0).toFixed(2), icon: '💴', color: '#6366f1' },
    { label: '本周营收', value: '¥' + (Number(kpi.value.weekRevenue) || 0).toFixed(2), icon: '📅', color: '#f59e0b' },
    { label: '成交订单', value: (kpi.value.orderCount || 0) + ' 单', icon: '🧾', color: '#10b981' },
    { label: '客单价', value: '¥' + (Number(kpi.value.aov) || 0).toFixed(2), icon: '👤', color: '#ef4444' },
])

// 趋势图计算
const trendMax = computed(() => {
    const max = Math.max(...trendData.value.map(d => Number(d.revenue)), 1)
    return max
})

const trendDots = computed(() => {
    if (!trendData.value.length) return []
    const n = trendData.value.length
    return trendData.value.map((d, i) => ({
        x: 40 + i * (480 / (n - 1 || 1)),
        y: 128 - (Number(d.revenue) / trendMax.value) * 110
    }))
})

const trendPoints = computed(() =>
    trendDots.value.map(p => `${p.x},${p.y}`).join(' ')
)

const trendFillPoints = computed(() => {
    if (!trendDots.value.length) return ''
    const pts = trendDots.value.map(p => `${p.x},${p.y}`).join(' ')
    const last = trendDots.value[trendDots.value.length - 1]
    const first = trendDots.value[0]
    return `${pts} ${last.x},140 ${first.x},140`
})

const formatYLabel = (v) => v >= 1000 ? (v / 1000).toFixed(1) + 'k' : Math.round(v)

// 甜甜圈图
const DONUT_CIRC = 2 * Math.PI * 45
const donutSegs = computed(() => {
    const total = catData.value.reduce((s, c) => s + Number(c.quantity), 0)
    if (!total) return []
    let offset = 0
    return catData.value.map((c, i) => {
        const pct = Number(c.quantity) / total
        const dash = pct * DONUT_CIRC
        const seg = {
            color: catColors[i % catColors.length],
            dash: `${dash} ${DONUT_CIRC - dash}`,
            offset: DONUT_CIRC * 0.25 - offset  // 从顶部开始
        }
        offset += dash
        return seg
    })
})

// 时间格式化
const timeAgo = (t) => {
    if (!t) return ''
    const diff = (Date.now() - new Date(t).getTime()) / 1000
    if (diff < 60) return Math.floor(diff) + '秒前'
    if (diff < 3600) return Math.floor(diff / 60) + '分钟前'
    if (diff < 86400) return Math.floor(diff / 3600) + '小时前'
    return Math.floor(diff / 86400) + '天前'
}

const loadAll = async () => {
    loading.value = true
    try {
        const p = { params: { belongMuseum } }
        const [r1, r2, r3, r4, r5, r6] = await Promise.all([
            axios.get('/api/shop/dashboard/kpi', p),
            axios.get('/api/shop/dashboard/revenue-trend', p),
            axios.get('/api/shop/dashboard/category-analysis', p),
            axios.get('/api/shop/dashboard/top-products', p),
            axios.get('/api/shop/dashboard/low-stock', p),
            axios.get('/api/shop/dashboard/recent-orders', p),
        ])
        kpi.value = r1.data.data || {}
        trendData.value = r2.data.data || []
        catData.value = r3.data.data || []
        topSales.value = r4.data.data?.topSales || []
        topRevenue.value = r4.data.data?.topRevenue || []
        lowStockList.value = r5.data.data || []
        recentOrders.value = r6.data.data || []
    } finally {
        loading.value = false
    }
}

onMounted(loadAll)
</script>

<style scoped>
.dashboard {
    padding: 20px;
    background: #f8fafc;
    min-height: 100%;
    font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.dash-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20px;
}

.dash-title {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 20px;
    font-weight: 700;
    color: #111827;
}

.title-icon {
    font-size: 24px;
}

.museum-tag {
    font-size: 13px;
    font-weight: 500;
    background: #ede9fe;
    color: #6366f1;
    padding: 3px 10px;
    border-radius: 20px;
}

/* KPI */
.kpi-row {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
    margin-bottom: 20px;
}

.kpi-card {
    background: white;
    border-radius: 14px;
    padding: 20px;
    display: flex;
    align-items: center;
    gap: 16px;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
    position: relative;
    overflow: hidden;
}

.kpi-badge {
    position: absolute;
    right: -12px;
    top: -12px;
    width: 60px;
    height: 60px;
    border-radius: 50%;
    opacity: 0.1;
}

.kpi-icon {
    font-size: 32px;
}

.kpi-value {
    font-size: 22px;
    font-weight: 800;
    color: #111827;
    line-height: 1;
}

.kpi-label {
    font-size: 13px;
    color: #6b7280;
    margin-top: 4px;
}

/* 通用卡片 */
.card {
    background: white;
    border-radius: 14px;
    padding: 20px;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.card-title {
    font-size: 15px;
    font-weight: 700;
    color: #111827;
    margin-bottom: 16px;
    display: flex;
    align-items: center;
    gap: 6px;
}

.no-data {
    color: #9ca3af;
    font-size: 13px;
    text-align: center;
    padding: 30px 0;
}

.no-warn {
    color: #10b981;
    font-size: 14px;
}

/* 趋势图 */
.row-2 {
    display: grid;
    grid-template-columns: 2fr 1fr;
    gap: 16px;
    margin-bottom: 20px;
}

.chart-area {
    width: 100%;
}

.trend-svg {
    width: 100%;
    height: 160px;
}

/* 分类 */
.cat-body {
    display: flex;
    align-items: center;
    gap: 16px;
}

.donut-svg {
    width: 120px;
    height: 120px;
    flex-shrink: 0;
    transform: rotate(-90deg);
}

.cat-legend {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.legend-item {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
}

.legend-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    flex-shrink: 0;
}

.legend-name {
    flex: 1;
    color: #374151;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.legend-pct {
    color: #6b7280;
    font-weight: 600;
}

/* 排行榜 */
.row-3 {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
    margin-bottom: 20px;
}

.rank-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.rank-item {
    display: flex;
    align-items: center;
    gap: 10px;
}

.rank-no {
    width: 22px;
    height: 22px;
    border-radius: 50%;
    background: #e5e7eb;
    color: #6b7280;
    font-size: 12px;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
}

.rank-1 {
    background: #fbbf24;
    color: white;
}

.rank-2 {
    background: #9ca3af;
    color: white;
}

.rank-3 {
    background: #cd7c3a;
    color: white;
}

.rank-img {
    width: 36px;
    height: 36px;
    border-radius: 8px;
    object-fit: cover;
    flex-shrink: 0;
}

.rank-name {
    flex: 1;
    font-size: 13px;
    color: #374151;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.rank-val {
    font-size: 13px;
    font-weight: 700;
    color: #111827;
    white-space: nowrap;
    min-width: 60px;
    text-align: right;
}

.rank-bar-wrap {
    width: 70px;
}

.rank-bar {
    height: 6px;
    border-radius: 3px;
    transition: width 0.6s ease;
}

/* 库存预警 + 订单流 */
.row-4 {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
}

.stock-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
    max-height: 280px;
    overflow-y: auto;
}

.stock-item {
    display: flex;
    align-items: center;
    gap: 12px;
}

.stock-img {
    width: 40px;
    height: 40px;
    border-radius: 8px;
    object-fit: cover;
    flex-shrink: 0;
}

.stock-info {
    flex: 1;
}

.stock-name {
    font-size: 13px;
    color: #374151;
    margin-bottom: 4px;
}

.stock-bar-bg {
    height: 6px;
    background: #f3f4f6;
    border-radius: 3px;
}

.stock-bar-fill {
    height: 6px;
    border-radius: 3px;
    transition: width 0.6s ease;
}

.stock-count {
    font-size: 12px;
    font-weight: 700;
    color: #f97316;
    white-space: nowrap;
}

.stock-count.out {
    color: #ef4444;
}

/* 订单流 */
.orders-feed {
    display: flex;
    flex-direction: column;
    gap: 10px;
    max-height: 280px;
    overflow-y: auto;
}

.feed-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 8px;
    border-radius: 10px;
    background: #f9fafb;
    animation: fadeSlide 0.4s ease both;
}

@keyframes fadeSlide {
    from {
        opacity: 0;
        transform: translateY(6px);
    }

    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.feed-img {
    width: 40px;
    height: 40px;
    border-radius: 8px;
    object-fit: cover;
    flex-shrink: 0;
}

.feed-info {
    flex: 1;
}

.feed-product {
    font-size: 13px;
    color: #374151;
    font-weight: 500;
}

.feed-meta {
    font-size: 12px;
    color: #9ca3af;
    margin-top: 2px;
}

.feed-price {
    font-size: 14px;
    font-weight: 800;
    color: #6366f1;
    white-space: nowrap;
}
</style>