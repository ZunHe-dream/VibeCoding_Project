<template>
    <div class="monitor-page">
        <!-- 背景装饰 -->
        <div class="bg-decor">
            <div class="decor-circle c1"></div>
            <div class="decor-circle c2"></div>
            <div class="decor-line l1"></div>
            <div class="decor-line l2"></div>
        </div>

        <!-- 顶部 -->
        <div class="page-header">
            <div class="header-left">
                <div class="title-tag">COLLECTION MONITOR</div>
                <h1 class="page-title">文物外借监控</h1>
                <p class="page-sub">{{ userBelong }} · 实时掌握馆藏动态</p>
            </div>
            <div class="header-stats" v-if="!loading">
                <div class="stat-chip">
                    <span class="stat-num">{{ list.length }}</span>
                    <span class="stat-label">馆藏总数</span>
                </div>
                <div class="stat-chip">
                    <span class="stat-num">{{ loanedCount }}</span>
                    <span class="stat-label">外借中</span>
                </div>
                <div class="stat-chip">
                    <span class="stat-num">{{ totalLoan }}</span>
                    <span class="stat-label">累计借出</span>
                </div>
                <div class="stat-chip">
                    <span class="stat-num">{{ totalFav }}</span>
                    <span class="stat-label">累计收藏</span>
                </div>
            </div>
        </div>

        <div v-if="loading" class="loading-wrap">
            <el-skeleton :rows="6" animated />
        </div>

        <template v-else>
            <!-- 图表区 -->
            <div class="charts-row">
                <!-- 分类分布 -->
                <div class="chart-card">
                    <div class="chart-title">📊 馆藏分类分布</div>
                    <div class="category-bars">
                        <div v-for="(count, cat) in categoryMap" :key="cat" class="bar-row">
                            <span class="bar-label">{{ cat }}</span>
                            <div class="bar-track">
                                <div class="bar-fill" :style="{ width: (count / maxCategoryCount * 100) + '%' }"></div>
                            </div>
                            <span class="bar-count">{{ count }}</span>
                        </div>
                    </div>
                </div>

                <!-- 热门外借 Top5 -->
                <div class="chart-card">
                    <div class="chart-title">🏆 借用热度 Top 5</div>
                    <div class="top-list">
                        <div v-if="top5Loan.length === 0" class="no-data">暂无借用记录</div>
                        <div v-for="(item, index) in top5Loan" :key="item.artifactId" class="top-item">
                            <span :class="['rank-badge', `rank-${index + 1}`]">{{ index + 1 }}</span>
                            <div class="top-info">
                                <div class="top-name">{{ item.artifactName }}</div>
                                <div class="top-meta">{{ item.dynasty }} · {{ item.category }}</div>
                            </div>
                            <div class="top-score">
                                <span class="score-num">{{ item.loanCount }}</span>
                                <span class="score-unit">次</span>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 收藏热度 Top5 -->
                <div class="chart-card">
                    <div class="chart-title">❤️ 收藏热度 Top 5</div>
                    <div class="top-list">
                        <div v-if="top5Fav.length === 0" class="no-data">暂无收藏记录</div>
                        <div v-for="(item, index) in top5Fav" :key="item.artifactId" class="top-item">
                            <span :class="['rank-badge', `rank-${index + 1}`]">{{ index + 1 }}</span>
                            <div class="top-info">
                                <div class="top-name">{{ item.artifactName }}</div>
                                <div class="top-meta">{{ item.dynasty }} · {{ item.category }}</div>
                            </div>
                            <div class="top-score">
                                <span class="score-num">{{ item.favCount }}</span>
                                <span class="score-unit">次</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 筛选栏 -->
            <div class="filter-row">
                <div class="filter-tabs">
                    <span v-for="tab in tabs" :key="tab.value"
                        :class="['filter-tab', { active: activeTab === tab.value }]" @click="activeTab = tab.value">{{
                        tab.label }}</span>
                </div>
                <el-input v-model="searchKey" placeholder="搜索文物名称…" clearable class="monitor-search"
                    style="width:220px">
                    <template #prefix>🔍</template>
                </el-input>
            </div>

            <!-- 文物列表 -->
            <div class="artifact-table-wrap">
                <table class="artifact-table">
                    <thead>
                        <tr>
                            <th>文物</th>
                            <th>分类 / 朝代</th>
                            <th>存放位置</th>
                            <th>状态</th>
                            <th @click="sortBy('loanCount')" class="sortable">
                                借用次数
                                <span class="sort-icon">{{ sortKey === 'loanCount' ? (sortDesc ? '↓' : '↑') : '⇅'
                                    }}</span>
                            </th>
                            <th @click="sortBy('favCount')" class="sortable">
                                收藏次数
                                <span class="sort-icon">{{ sortKey === 'favCount' ? (sortDesc ? '↓' : '↑') : '⇅'
                                    }}</span>
                            </th>
                            <th>热度评级</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="item in filteredList" :key="item.artifactId" class="table-row">
                            <td>
                                <div class="artifact-cell">
                                    <div class="artifact-thumb">
                                        <img v-if="item.imageUrl" :src="item.imageUrl" class="thumb-img"
                                            @error="(e) => e.target.style.display = 'none'" />
                                        <span v-else class="thumb-placeholder">🏺</span>
                                    </div>
                                    <div>
                                        <div class="artifact-name">{{ item.artifactName }}</div>
                                        <div class="artifact-author">{{ item.author || '佚名' }}</div>
                                    </div>
                                </div>
                            </td>
                            <td>
                                <span class="cat-tag">{{ item.category || '—' }}</span>
                                <div class="dynasty-text">{{ item.dynasty || '年代不详' }}</div>
                            </td>
                            <td class="location-cell">{{ item.storageLocation || '未录入' }}</td>
                            <td>
                                <span :class="['status-dot', statusClass(item.status)]">
                                    {{ statusText(item.status) }}
                                </span>
                            </td>
                            <td>
                                <div class="count-cell">
                                    <div class="count-bar-wrap">
                                        <div class="count-bar loan-bar"
                                            :style="{ width: maxLoan ? (item.loanCount / maxLoan * 100) + '%' : '0%' }">
                                        </div>
                                    </div>
                                    <span class="count-num">{{ item.loanCount }}</span>
                                </div>
                            </td>
                            <td>
                                <div class="count-cell">
                                    <div class="count-bar-wrap">
                                        <div class="count-bar fav-bar"
                                            :style="{ width: maxFav ? (item.favCount / maxFav * 100) + '%' : '0%' }">
                                        </div>
                                    </div>
                                    <span class="count-num">{{ item.favCount }}</span>
                                </div>
                            </td>
                            <td>
                                <span :class="['heat-badge', heatClass(item)]">{{ heatLabel(item) }}</span>
                            </td>
                        </tr>
                        <tr v-if="filteredList.length === 0">
                            <td colspan="7" class="empty-row">暂无数据</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </template>
    </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const userBelong = ref('')
const loading = ref(true)
const list = ref([])
const searchKey = ref('')
const activeTab = ref('all')
const sortKey = ref('loanCount')
const sortDesc = ref(true)

const tabs = [
    { label: '全部', value: 'all' },
    { label: '外借中', value: 'loaned' },
    { label: '馆藏中', value: 'in' },
    { label: '热门展品', value: 'hot' },
    { label: '收藏佳品', value: 'fav' },
]

onMounted(async () => {
    const user = JSON.parse(sessionStorage.getItem('user') || '{}')
    userBelong.value = user.belong || ''
    await fetchData()
})

const fetchData = async () => {
    loading.value = true
    try {
        const res = await axios.get('/api/artifact/monitor', {
            params: { ownerMuseum: userBelong.value }
        })
        const data = res.data
        list.value = Array.isArray(data) ? data : (data?.data || [])
    } catch (e) {
        ElMessage.error('获取监控数据失败')
    } finally {
        loading.value = false
    }
}

// 统计数据
const loanedCount = computed(() => list.value.filter(i => i.status === 1).length)
const totalLoan = computed(() => list.value.reduce((s, i) => s + i.loanCount, 0))
const totalFav = computed(() => list.value.reduce((s, i) => s + i.favCount, 0))
const maxLoan = computed(() => Math.max(...list.value.map(i => i.loanCount), 1))
const maxFav = computed(() => Math.max(...list.value.map(i => i.favCount), 1))

// 分类统计
const categoryMap = computed(() => {
    const map = {}
    list.value.forEach(i => {
        const cat = i.category || '未分类'
        map[cat] = (map[cat] || 0) + 1
    })
    // 按数量降序
    return Object.fromEntries(
        Object.entries(map).sort((a, b) => b[1] - a[1]).slice(0, 8)
    )
})
const maxCategoryCount = computed(() => Math.max(...Object.values(categoryMap.value), 1))

// Top5
const top5Loan = computed(() =>
    [...list.value].sort((a, b) => b.loanCount - a.loanCount).slice(0, 5)
)
const top5Fav = computed(() =>
    [...list.value].sort((a, b) => b.favCount - a.favCount).slice(0, 5)
)

// 筛选 + 排序
const filteredList = computed(() => {
    let result = [...list.value]

    // 搜索
    if (searchKey.value) {
        result = result.filter(i =>
            i.artifactName?.includes(searchKey.value) ||
            i.author?.includes(searchKey.value)
        )
    }

    // Tab 筛选
    if (activeTab.value === 'loaned') result = result.filter(i => i.status === 1)
    else if (activeTab.value === 'in') result = result.filter(i => i.status === 0)
    else if (activeTab.value === 'hot') result = result.filter(i => i.loanCount > 0).sort((a, b) => b.loanCount - a.loanCount)
    else if (activeTab.value === 'fav') result = result.filter(i => i.favCount > 0).sort((a, b) => b.favCount - a.favCount)

    // 排序
    if (activeTab.value === 'all' || activeTab.value === 'loaned' || activeTab.value === 'in') {
        result.sort((a, b) => sortDesc.value
            ? b[sortKey.value] - a[sortKey.value]
            : a[sortKey.value] - b[sortKey.value]
        )
    }

    return result
})

const sortBy = (key) => {
    if (sortKey.value === key) sortDesc.value = !sortDesc.value
    else { sortKey.value = key; sortDesc.value = true }
}

// 状态
const statusText = (s) => ({ 0: '馆藏中', 1: '外借中', 2: '维修中' }[s] ?? '未知')
const statusClass = (s) => ({ 0: 'dot-in', 1: 'dot-out', 2: 'dot-repair' }[s] ?? '')

// 热度评级
const heatLabel = (item) => {
    const score = item.loanCount * 2 + item.favCount
    if (score >= 10) return '🔥 顶级藏品'
    if (score >= 5) return '⭐ 热门展品'
    if (score >= 2) return '📈 潜力展品'
    return '💤 待发掘'
}
const heatClass = (item) => {
    const score = item.loanCount * 2 + item.favCount
    if (score >= 10) return 'heat-top'
    if (score >= 5) return 'heat-hot'
    if (score >= 2) return 'heat-rising'
    return 'heat-low'
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@700&family=Noto+Sans+SC:wght@300;400;500&display=swap');

* {
    box-sizing: border-box;
}

.monitor-page {
    min-height: 100vh;
    background-color: #1f1408;
    padding: 36px 40px;
    font-family: 'Noto Sans SC', sans-serif;
    color: #e8e0d0;
    position: relative;
    overflow-x: hidden;
}

/* 背景装饰 */
.bg-decor {
    position: fixed;
    inset: 0;
    pointer-events: none;
    z-index: 0;
}

.decor-circle {
    position: absolute;
    border-radius: 50%;
    opacity: 0.06;
}

.c1 {
    width: 600px;
    height: 600px;
    background: #c8a96e;
    top: -200px;
    right: -100px;
}

.c2 {
    width: 400px;
    height: 400px;
    background: #7a3b1e;
    bottom: -100px;
    left: -100px;
}

.decor-line {
    position: absolute;
    background: linear-gradient(90deg, transparent, rgba(200, 169, 110, 0.15), transparent);
    height: 1px;
    width: 100%;
}

.l1 {
    top: 30%;
}

.l2 {
    top: 65%;
}

/* 顶部 */
.page-header {
    position: relative;
    z-index: 1;
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    margin-bottom: 36px;
}

.title-tag {
    font-size: 11px;
    letter-spacing: 4px;
    color: #c8a96e;
    margin-bottom: 8px;
    font-weight: 500;
}

.page-title {
    font-family: 'Playfair Display', 'Noto Sans SC', serif;
    font-size: 36px;
    color: #f0e6d0;
    margin: 0 0 6px;
    letter-spacing: 2px;
}

.page-sub {
    font-size: 13px;
    color: #7a6a50;
    margin: 0;
}

.header-stats {
    display: flex;
    gap: 16px;
}

.stat-chip {
    background: rgba(200, 169, 110, 0.08);
    border: 1px solid rgba(200, 169, 110, 0.2);
    border-radius: 12px;
    padding: 12px 20px;
    text-align: center;
    min-width: 80px;
}

.stat-num {
    display: block;
    font-size: 26px;
    font-weight: 700;
    color: #c8a96e;
    font-family: 'Playfair Display', serif;
    line-height: 1;
}

.stat-label {
    font-size: 11px;
    color: #7a6a50;
    margin-top: 4px;
    display: block;
}

/* loading */
.loading-wrap {
    position: relative;
    z-index: 1;
    padding: 40px 0;
}

/* 图表区 */
.charts-row {
    position: relative;
    z-index: 1;
    display: grid;
    grid-template-columns: 1.4fr 1fr 1fr;
    gap: 20px;
    margin-bottom: 24px;
}

.chart-card {
    background: rgba(255, 255, 255, 0.03);
    border: 1px solid rgba(200, 169, 110, 0.15);
    border-radius: 16px;
    padding: 22px;
    backdrop-filter: blur(10px);
}

.chart-title {
    font-size: 14px;
    font-weight: 500;
    color: #c8a96e;
    margin-bottom: 18px;
    letter-spacing: 1px;
}

/* 分类柱状图 */
.category-bars {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.bar-row {
    display: flex;
    align-items: center;
    gap: 10px;
}

.bar-label {
    font-size: 12px;
    color: #9a8a6a;
    width: 60px;
    flex-shrink: 0;
    text-align: right;
}

.bar-track {
    flex: 1;
    height: 8px;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 4px;
    overflow: hidden;
}

.bar-fill {
    height: 100%;
    background: linear-gradient(90deg, #7a3b1e, #c8a96e);
    border-radius: 4px;
    transition: width 0.8s ease;
}

.bar-count {
    font-size: 12px;
    color: #c8a96e;
    width: 20px;
    text-align: right;
    flex-shrink: 0;
}

/* Top 列表 */
.top-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.no-data {
    text-align: center;
    color: #5a4a30;
    font-size: 13px;
    padding: 20px 0;
}

.top-item {
    display: flex;
    align-items: center;
    gap: 12px;
}

.rank-badge {
    width: 26px;
    height: 26px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 700;
    flex-shrink: 0;
}

.rank-1 {
    background: #c8a96e;
    color: #0f1117;
}

.rank-2 {
    background: #9a9a9a;
    color: #0f1117;
}

.rank-3 {
    background: #8b5e3c;
    color: #fff;
}

.rank-4,
.rank-5 {
    background: rgba(255, 255, 255, 0.08);
    color: #9a8a6a;
}

.top-info {
    flex: 1;
    min-width: 0;
}

.top-name {
    font-size: 13px;
    color: #e8e0d0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.top-meta {
    font-size: 11px;
    color: #5a4a30;
    margin-top: 2px;
}

.top-score {
    text-align: right;
    flex-shrink: 0;
}

.score-num {
    font-size: 20px;
    font-weight: 700;
    color: #c8a96e;
    font-family: 'Playfair Display', serif;
}

.score-unit {
    font-size: 11px;
    color: #5a4a30;
    margin-left: 2px;
}

/* 筛选栏 */
.filter-row {
    position: relative;
    z-index: 1;
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
}

.filter-tabs {
    display: flex;
    gap: 8px;
}

.filter-tab {
    padding: 6px 16px;
    border-radius: 20px;
    font-size: 13px;
    cursor: pointer;
    color: #7a6a50;
    border: 1px solid rgba(200, 169, 110, 0.1);
    transition: all 0.2s;
    user-select: none;
}

.filter-tab:hover {
    color: #c8a96e;
    border-color: rgba(200, 169, 110, 0.3);
}

.filter-tab.active {
    background: rgba(200, 169, 110, 0.15);
    color: #c8a96e;
    border-color: rgba(200, 169, 110, 0.4);
}

.monitor-search :deep(.el-input__wrapper) {
    background: rgba(255, 255, 255, 0.04);
    box-shadow: 0 0 0 1px rgba(200, 169, 110, 0.2);
    border-radius: 8px;
    color: #e8e0d0;
}

.monitor-search :deep(.el-input__inner) {
    color: #e8e0d0;
}

.monitor-search :deep(.el-input__wrapper.is-focus) {
    box-shadow: 0 0 0 1px rgba(200, 169, 110, 0.5) !important;
}

/* 表格 */
.artifact-table-wrap {
    position: relative;
    z-index: 1;
    background: rgba(255, 255, 255, 0.02);
    border: 1px solid rgba(200, 169, 110, 0.12);
    border-radius: 16px;
    overflow: hidden;
}

.artifact-table {
    width: 100%;
    border-collapse: collapse;
}

.artifact-table thead tr {
    background: rgba(200, 169, 110, 0.08);
    border-bottom: 1px solid rgba(200, 169, 110, 0.15);
}

.artifact-table th {
    padding: 14px 16px;
    font-size: 12px;
    color: #7a6a50;
    font-weight: 500;
    text-align: left;
    letter-spacing: 1px;
}

.artifact-table th.sortable {
    cursor: pointer;
    user-select: none;
    transition: color 0.2s;
}

.artifact-table th.sortable:hover {
    color: #c8a96e;
}

.sort-icon {
    margin-left: 4px;
    font-size: 11px;
}

.table-row {
    border-bottom: 1px solid rgba(255, 255, 255, 0.04);
    transition: background 0.15s;
}

.table-row:hover {
    background: rgba(200, 169, 110, 0.05);
}

.table-row:last-child {
    border-bottom: none;
}

.artifact-table td {
    padding: 14px 16px;
    vertical-align: middle;
}

.artifact-cell {
    display: flex;
    align-items: center;
    gap: 12px;
}

.artifact-thumb {
    width: 44px;
    height: 44px;
    border-radius: 8px;
    overflow: hidden;
    background: rgba(200, 169, 110, 0.1);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
}

.thumb-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.thumb-placeholder {
    font-size: 20px;
}

.artifact-name {
    font-size: 14px;
    color: #e8e0d0;
    font-weight: 500;
}

.artifact-author {
    font-size: 11px;
    color: #5a4a30;
    margin-top: 2px;
}

.cat-tag {
    display: inline-block;
    background: rgba(200, 169, 110, 0.12);
    color: #c8a96e;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 11px;
}

.dynasty-text {
    font-size: 11px;
    color: #5a4a30;
    margin-top: 4px;
}

.location-cell {
    font-size: 12px;
    color: #7a6a50;
}

.status-dot {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    font-size: 12px;
    padding: 3px 10px;
    border-radius: 10px;
}

.status-dot::before {
    content: '';
    width: 6px;
    height: 6px;
    border-radius: 50%;
    display: inline-block;
}

.dot-in {
    background: rgba(100, 180, 100, 0.1);
    color: #6db86d;
}

.dot-in::before {
    background: #6db86d;
}

.dot-out {
    background: rgba(200, 169, 110, 0.1);
    color: #c8a96e;
}

.dot-out::before {
    background: #c8a96e;
    animation: pulse 1.5s infinite;
}

.dot-repair {
    background: rgba(200, 100, 50, 0.1);
    color: #d4824a;
}

.dot-repair::before {
    background: #d4824a;
}

@keyframes pulse {

    0%,
    100% {
        opacity: 1;
    }

    50% {
        opacity: 0.3;
    }
}

.count-cell {
    display: flex;
    align-items: center;
    gap: 8px;
}

.count-bar-wrap {
    flex: 1;
    height: 6px;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 3px;
    overflow: hidden;
}

.count-bar {
    height: 100%;
    border-radius: 3px;
    transition: width 0.6s ease;
}

.loan-bar {
    background: linear-gradient(90deg, #7a3b1e, #c8694a);
}

.fav-bar {
    background: linear-gradient(90deg, #5a3a7a, #9a6ec8);
}

.count-num {
    font-size: 14px;
    font-weight: 600;
    color: #c8a96e;
    min-width: 24px;
    text-align: right;
}

.heat-badge {
    display: inline-block;
    padding: 3px 10px;
    border-radius: 8px;
    font-size: 12px;
    white-space: nowrap;
}

.heat-top {
    background: rgba(200, 169, 110, 0.2);
    color: #c8a96e;
    border: 1px solid rgba(200, 169, 110, 0.3);
}

.heat-hot {
    background: rgba(200, 100, 50, 0.15);
    color: #d4824a;
    border: 1px solid rgba(200, 100, 50, 0.2);
}

.heat-rising {
    background: rgba(100, 150, 200, 0.1);
    color: #7aabcc;
    border: 1px solid rgba(100, 150, 200, 0.2);
}

.heat-low {
    background: rgba(255, 255, 255, 0.04);
    color: #5a4a30;
    border: 1px solid rgba(255, 255, 255, 0.06);
}

.empty-row {
    text-align: center;
    color: #5a4a30;
    padding: 40px;
    font-size: 14px;
}
</style>