<template>
    <div class="progress-root">
        <!-- 左侧：展览列表 -->
        <div class="left-panel">
            <div class="panel-header">
                <div class="panel-title">我的巡展</div>
                <div class="panel-sub">共 {{ filteredApps.length }} 项</div>
            </div>
            <div class="search-wrap">
                <el-input v-model="searchKeyword" placeholder="搜索展览名称…" clearable prefix-icon="Search">
                    <template #prefix>
                        <span style="font-size:14px;">🔍</span>
                    </template>
                </el-input>
            </div>
            <div class="app-list" v-if="filteredApps.length > 0">
                <div v-for="app in filteredApps" :key="app.appId" class="app-item"
                    :class="{ active: selectedAppId === app.appId }" @click="selectApp(app.appId)">
                    <div class="app-item-inner">
                        <div class="app-icon">🏛️</div>
                        <div class="app-info">
                            <div class="app-name">{{ app.title }}</div>
                            <div class="app-meta">{{ app.targetLocation || '地点未定' }}</div>
                        </div>
                        <div class="app-arrow" v-if="selectedAppId === app.appId">›</div>
                    </div>
                    <!-- 进度小条 -->
                    <div class="app-progress-bar" v-if="selectedAppId === app.appId && summary.length > 0">
                        <div class="bar-seg pass" :style="{ flex: approvedCount }"></div>
                        <div class="bar-seg reject" :style="{ flex: rejectedCount }"></div>
                        <div class="bar-seg wait" :style="{ flex: pendingCount }"></div>
                    </div>
                </div>
            </div>
            <div v-else class="list-empty">
                <div>📭</div>
                <div>{{ searchKeyword ? '无匹配结果' : '暂无巡展申请' }}</div>
            </div>
        </div>

        <!-- 右侧：审批详情 -->
        <div class="right-panel">
            <template v-if="selectedAppId">
                <!-- 顶部标题 -->
                <div class="right-header">
                    <div>
                        <div class="right-title">{{ currentApp?.title }}</div>
                        <div class="right-sub">{{ currentApp?.targetLocation }} · 审批进度</div>
                    </div>
                </div>

                <!-- KPI 概览 -->
                <div class="overview-row" v-if="!summaryLoading">
                    <div class="stat-card total">
                        <div class="stat-num">{{ summary.length }}</div>
                        <div class="stat-label">申请文物总数</div>
                    </div>
                    <div class="stat-card pass">
                        <div class="stat-num">{{ approvedCount }}</div>
                        <div class="stat-label">已同意</div>
                    </div>
                    <div class="stat-card reject">
                        <div class="stat-num">{{ rejectedCount }}</div>
                        <div class="stat-label">已拒绝</div>
                    </div>
                    <div class="stat-card wait">
                        <div class="stat-num">{{ pendingCount }}</div>
                        <div class="stat-label">审核中</div>
                    </div>
                </div>

                <!-- 状态提示 -->
                <div class="status-bar" v-if="!summaryLoading && summary.length > 0">
                    <div v-if="pendingCount > 0" class="alert-box warning">
                        ⏳ 还有 <strong>{{ pendingCount }}</strong> 件文物正在等待博物馆审批，请耐心等候
                    </div>
                    <div v-else-if="approvedCount === 0" class="alert-box error">
                        ❌ 所有文物借用申请均未通过，无法办展
                    </div>
                    <div v-else class="alert-box success">
                        ✅ 审批已全部完成，共 <strong>{{ approvedCount }}</strong> 件文物获批
                        <el-button type="success" size="small" style="margin-left:12px" @click="confirmExhibition">
                            确认办展
                        </el-button>
                    </div>
                </div>

                <!-- 按博物馆分组 -->
                <div v-if="summaryLoading" class="loading-area">
                    <div class="loading-spin">⟳</div>
                    <div>加载审批数据中…</div>
                </div>
                <div v-else-if="summary.length === 0" class="right-empty">
                    <div>📄</div>
                    <div>暂无审批记录</div>
                </div>
                <div v-else class="museum-groups">
                    <div v-for="(group, museum) in groupedSummary" :key="museum" class="museum-group">
                        <div class="museum-header">
                            <span class="museum-name">🏛 {{ museum }}</span>
                            <div class="museum-stats">
                                <span class="mini-tag pass">同意 {{group.filter(r => r.status === 1 && r.isFinalized ===
                                    1).length }}</span>
                                <span class="mini-tag reject">拒绝 {{group.filter(r => r.status === 2 && r.isFinalized
                                    === 1).length }}</span>
                                <span class="mini-tag wait">待审 {{group.filter(r => r.isFinalized === 0).length
                                    }}</span>
                            </div>
                        </div>
                        <el-table :data="group" size="small" class="artifact-table">
                            <el-table-column label="文物名称">
                                <template #default="{ row }">
                                    <span class="artifact-link" @click="showArtifactDetail(row.artifactId)">
                                        {{ row.artifactName }}
                                    </span>
                                </template>
                            </el-table-column>
                            <el-table-column label="审批状态" width="100" align="center">
                                <template #default="{ row }">
                                    <span class="status-badge"
                                        :class="row.isFinalized === 0 ? 'wait' : ['wait', 'pass', 'reject'][row.status]">
                                        {{ row.isFinalized === 0 ? '审核中' : ['待审批', '已同意', '已拒绝'][row.status] }}
                                    </span>
                                </template>
                            </el-table-column>
                            <el-table-column prop="auditRemark" label="审批意见" />
                        </el-table>
                    </div>
                </div>
            </template>

            <!-- 未选中状态 -->
            <div v-else class="right-placeholder">
                <div class="placeholder-icon">📋</div>
                <div class="placeholder-text">从左侧选择一项巡展申请</div>
                <div class="placeholder-sub">查看各博物馆的文物借用审批进度</div>
            </div>
        </div>

        <!-- 文物详情弹窗 -->
        <el-dialog v-model="artifactDetailVisible" title="文物详细信息" width="480px" append-to-body>
            <div v-if="artifactDetail" style="padding:10px">
                <el-image :src="artifactDetail.imageUrl"
                    style="width:100%;max-height:220px;border-radius:8px;margin-bottom:14px" fit="contain" />
                <el-descriptions :column="2" border size="small">
                    <el-descriptions-item label="文物名称" :span="2">{{ artifactDetail.artifactName
                        }}</el-descriptions-item>
                    <el-descriptions-item label="作者">{{ artifactDetail.author || '佚名' }}</el-descriptions-item>
                    <el-descriptions-item label="朝代">{{ artifactDetail.dynasty || '—' }}</el-descriptions-item>
                    <el-descriptions-item label="分类">{{ artifactDetail.category || '—' }}</el-descriptions-item>
                    <el-descriptions-item label="状态">
                        <el-tag :type="['success', 'warning', 'info'][artifactDetail.status]" size="small">
                            {{ ['馆藏中', '外借巡展中', '维修保养中'][artifactDetail.status] }}
                        </el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="存放位置" :span="2">{{ artifactDetail.storageLocation || '—'
                        }}</el-descriptions-item>
                    <el-descriptions-item label="当前位置" :span="2">{{ artifactDetail.currentLocation || '原馆'
                        }}</el-descriptions-item>
                </el-descriptions>
                <div
                    style="margin-top:12px;background:#f9f9f9;padding:10px;border-radius:6px;font-size:13px;color:#666;line-height:1.7">
                    {{ artifactDetail.description || '暂无描述' }}
                </div>
            </div>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const myApps = ref([])
const selectedAppId = ref(null)
const summary = ref([])
const summaryLoading = ref(false)
const searchKeyword = ref('')
const userId = JSON.parse(sessionStorage.getItem('user') || '{}').userId

const artifactDetailVisible = ref(false)
const artifactDetail = ref(null)

// 搜索过滤
const filteredApps = computed(() => {
    if (!searchKeyword.value) return myApps.value
    return myApps.value.filter(a =>
        a.title?.includes(searchKeyword.value)
    )
})

const currentApp = computed(() =>
    myApps.value.find(a => a.appId === selectedAppId.value)
)

const pendingCount = computed(() => summary.value.filter(r => r.isFinalized === 0).length)
const approvedCount = computed(() => summary.value.filter(r => r.status === 1 && r.isFinalized === 1).length)
const rejectedCount = computed(() => summary.value.filter(r => r.status === 2 && r.isFinalized === 1).length)

const groupedSummary = computed(() => {
    const map = {}
    summary.value.forEach(r => {
        if (!map[r.ownerMuseum]) map[r.ownerMuseum] = []
        map[r.ownerMuseum].push(r)
    })
    return map
})

const selectApp = async (appId) => {
    selectedAppId.value = appId
    summaryLoading.value = true
    summary.value = []
    try {
        const res = await axios.get('/api/loan/summary', { params: { appId } })
        if (res.data.code === 200) summary.value = res.data.data
    } finally {
        summaryLoading.value = false
    }
}

const showArtifactDetail = async (artifactId) => {
    const res = await axios.get(`/api/artifact/${artifactId}`)
    if (res.data.code === 200) {
        artifactDetail.value = res.data.data
        artifactDetailVisible.value = true
    }
}

const confirmExhibition = async () => {
    await ElMessageBox.confirm(
        `确认以 ${approvedCount.value} 件通过的文物正式办展？拒绝的文物将从本次申请中移除。`,
        '确认办展',
        { confirmButtonText: '确认办展', cancelButtonText: '再想想', type: 'info' }
    )
    const res = await axios.post('/api/loan/confirm', null, { params: { appId: selectedAppId.value } })
    if (res.data.code === 200) {
        ElMessage.success(res.data.data)
        selectApp(selectedAppId.value)
    }
}

onMounted(async () => {
    const res = await axios.get('/api/exhibition/list')
    if (res.data.code === 200) {
        myApps.value = res.data.data.filter(a => a.applicantId === userId)
        // 默认选中第一项
        if (myApps.value.length > 0) {
            selectApp(myApps.value[0].appId)
        }
    }
})
</script>

<style scoped>
.progress-root {
    display: flex;
    height: calc(100vh - 80px);
    background: #f5f4f0;
    font-family: 'Noto Serif SC', 'Noto Sans SC', serif;
    overflow: hidden;
}

/* 左侧面板 */
.left-panel {
    width: 280px;
    flex-shrink: 0;
    background: #2c1a0e;
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

.panel-header {
    padding: 24px 20px 12px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.panel-title {
    font-size: 16px;
    font-weight: 700;
    color: #f5e6c8;
    letter-spacing: 1px;
}

.panel-sub {
    font-size: 12px;
    color: #9a7c5a;
    margin-top: 4px;
}

.search-wrap {
    padding: 12px 14px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.search-wrap :deep(.el-input__wrapper) {
    background: rgba(255, 255, 255, 0.07);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 8px;
    box-shadow: none !important;
}

.search-wrap :deep(.el-input__inner) {
    color: #f5e6c8;
    font-size: 13px;
}

.search-wrap :deep(.el-input__inner::placeholder) {
    color: #6b5540;
}

.app-list {
    flex: 1;
    overflow-y: auto;
    padding: 8px 0;
}

.app-list::-webkit-scrollbar {
    width: 4px;
}

.app-list::-webkit-scrollbar-track {
    background: transparent;
}

.app-list::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.1);
    border-radius: 2px;
}

.app-item {
    cursor: pointer;
    transition: background 0.2s;
    border-left: 3px solid transparent;
}

.app-item:hover {
    background: rgba(255, 255, 255, 0.05);
}

.app-item.active {
    background: rgba(245, 230, 200, 0.1);
    border-left-color: #c8a96e;
}

.app-item-inner {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px 16px;
}

.app-icon {
    font-size: 18px;
    flex-shrink: 0;
}

.app-info {
    flex: 1;
    overflow: hidden;
}

.app-name {
    font-size: 13px;
    color: #f5e6c8;
    font-weight: 500;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.app-meta {
    font-size: 11px;
    color: #7a6040;
    margin-top: 2px;
}

.app-arrow {
    color: #c8a96e;
    font-size: 18px;
    font-weight: 300;
}

.app-progress-bar {
    display: flex;
    height: 3px;
    margin: 0 16px 8px;
    border-radius: 2px;
    overflow: hidden;
    gap: 1px;
}

.bar-seg {
    min-width: 4px;
    border-radius: 2px;
    transition: flex 0.4s ease;
}

.bar-seg.pass {
    background: #52c41a;
}

.bar-seg.reject {
    background: #f5222d;
}

.bar-seg.wait {
    background: #faad14;
}

.list-empty {
    text-align: center;
    color: #6b5540;
    padding: 40px 20px;
    font-size: 13px;
    display: flex;
    flex-direction: column;
    gap: 8px;
}

/* 右侧面板 */
.right-panel {
    flex: 1;
    overflow-y: auto;
    padding: 28px 32px;
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.right-panel::-webkit-scrollbar {
    width: 6px;
}

.right-panel::-webkit-scrollbar-track {
    background: transparent;
}

.right-panel::-webkit-scrollbar-thumb {
    background: #d4c4a8;
    border-radius: 3px;
}

.right-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
}

.right-title {
    font-size: 20px;
    font-weight: 700;
    color: #2c1a0e;
}

.right-sub {
    font-size: 13px;
    color: #9a7c5a;
    margin-top: 4px;
}

/* KPI 卡片 */
.overview-row {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 14px;
}

.stat-card {
    background: white;
    border-radius: 12px;
    padding: 18px;
    text-align: center;
    border-top: 3px solid #e8dfd0;
    box-shadow: 0 1px 4px rgba(44, 26, 14, 0.06);
}

.stat-card.pass {
    border-top-color: #52c41a;
}

.stat-card.reject {
    border-top-color: #f5222d;
}

.stat-card.wait {
    border-top-color: #faad14;
}

.stat-card.total {
    border-top-color: #8b5e3c;
}

.stat-num {
    font-size: 30px;
    font-weight: 800;
    color: #2c1a0e;
}

.stat-card.pass .stat-num {
    color: #52c41a;
}

.stat-card.reject .stat-num {
    color: #f5222d;
}

.stat-card.wait .stat-num {
    color: #faad14;
}

.stat-label {
    font-size: 12px;
    color: #9a7c5a;
    margin-top: 4px;
}

/* 状态提示 */
.alert-box {
    padding: 12px 16px;
    border-radius: 10px;
    font-size: 14px;
    display: flex;
    align-items: center;
}

.alert-box.warning {
    background: #fffbe6;
    color: #7c5800;
    border: 1px solid #ffe58f;
}

.alert-box.error {
    background: #fff2f0;
    color: #a8071a;
    border: 1px solid #ffccc7;
}

.alert-box.success {
    background: #f6ffed;
    color: #135200;
    border: 1px solid #b7eb8f;
}

/* 博物馆分组 */
.museum-group {
    background: white;
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 1px 4px rgba(44, 26, 14, 0.06);
}

.museum-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    background: #faf7f3;
    border-bottom: 1px solid #f0e8da;
}

.museum-name {
    font-weight: 600;
    color: #2c1a0e;
    font-size: 14px;
}

.museum-stats {
    display: flex;
    gap: 8px;
}

.mini-tag {
    font-size: 12px;
    padding: 2px 8px;
    border-radius: 4px;
    font-weight: 500;
}

.mini-tag.pass {
    background: #f6ffed;
    color: #52c41a;
}

.mini-tag.reject {
    background: #fff2f0;
    color: #f5222d;
}

.mini-tag.wait {
    background: #fffbe6;
    color: #d48806;
}

.artifact-table {
    border: none !important;
}

.artifact-link {
    color: #8b5e3c;
    cursor: pointer;
    text-decoration: underline;
    text-underline-offset: 2px;
}

.artifact-link:hover {
    color: #c8a96e;
}

.status-badge {
    font-size: 12px;
    padding: 2px 8px;
    border-radius: 4px;
    font-weight: 500;
}

.status-badge.pass {
    background: #f6ffed;
    color: #52c41a;
}

.status-badge.reject {
    background: #fff2f0;
    color: #f5222d;
}

.status-badge.wait {
    background: #f5f5f5;
    color: #8c8c8c;
}

/* 空状态 */
.right-placeholder {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 12px;
    color: #9a7c5a;
}

.placeholder-icon {
    font-size: 56px;
}

.placeholder-text {
    font-size: 16px;
    font-weight: 600;
    color: #5c3d1e;
}

.placeholder-sub {
    font-size: 13px;
}

.right-empty {
    text-align: center;
    color: #9a7c5a;
    padding: 60px;
    font-size: 14px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
}

.right-empty div:first-child {
    font-size: 40px;
}

.loading-area {
    text-align: center;
    padding: 60px;
    color: #9a7c5a;
    font-size: 14px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
}

.loading-spin {
    font-size: 32px;
    animation: spin 1s linear infinite;
}

@keyframes spin {
    from {
        transform: rotate(0deg);
    }

    to {
        transform: rotate(360deg);
    }
}
</style>