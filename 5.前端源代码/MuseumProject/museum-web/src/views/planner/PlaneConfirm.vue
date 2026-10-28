<template>
    <div class="publish-root">

        <!-- 页面顶部标题区 -->
        <div class="page-header">
            <div class="header-left">
                <button class="back-btn" @click="$router.back()">
                    <span>←</span> 巡展单位中心
                </button>
                <div class="title-block">
                    <span class="eyebrow">EXHIBITION RELEASE MANAGEMENT</span>
                    <h1 class="main-title">展讯发布管理</h1>
                </div>
            </div>
            <div class="header-seal">发</div>
        </div>

        <p class="subtitle-notice">
            <span class="notice-dot"></span>
            审批通过后，可在此配置并正式发布展讯。若需修改内容，请点击「撤回并修改」，原展讯将失效并重新进入审批流。
        </p>

        <el-tabs v-model="activeTab" class="plan-tabs">
            <el-tab-pane label="进行中的巡展" name="active">
                <div class="empty-state" v-if="plans.length === 0">
                    <div class="empty-char">无</div>
                    <p class="empty-text">暂无待发布的巡展计划</p>
                </div>
                <div class="plan-list" v-else>
                    <div class="plan-card" v-for="plan in plans" :key="plan.exhibitionId" @click="openDetail(plan)">
                        <!-- 左侧封面 -->
                        <div class="cover-col">
                            <el-image
                                :src="plan.coverURL || 'https://images.unsplash.com/photo-1566127444979-b3d2b654e3d7?w=400'"
                                fit="cover" class="cover-img" />
                            <div :class="['status-badge', plan.currentStatus === 1 ? 'badge-live' : 'badge-pending']">
                                {{ plan.currentStatus === 1 ? '已发布' : '待发布' }}
                            </div>
                        </div>
                        <!-- 中间信息 -->
                        <div class="info-col">
                            <div class="info-top">
                                <h2 class="plan-title">{{ plan.title }}</h2>
                                <button class="re-edit-btn" @click.stop="handleReEdit(plan)">↺ 撤回并修改</button>
                            </div>
                            <div class="meta-grid">
                                <div class="meta-item">
                                    <span class="meta-label">展出地点</span>
                                    <span class="meta-val">{{ plan.location }}</span>
                                </div>
                                <div class="meta-item">
                                    <span class="meta-label">展出周期</span>
                                    <span class="meta-val">{{ formatDate(plan.startTime) }} — {{
                                        formatDate(plan.endTime) }}</span>
                                </div>
                                <div class="meta-item full-width">
                                    <span class="meta-label">巡展描述</span>
                                    <span class="meta-val desc">{{ plan.description || '暂无详细描述' }}</span>
                                </div>
                            </div>
                        </div>
                        <!-- 右侧操作 -->
                        <div class="action-col">
                            <div class="action-pending" v-if="plan.currentStatus === 0">
                                <div class="capacity-block">
                                    <span class="capacity-label">每日预约上限</span>
                                    <el-input-number v-model="plan.maxCapacity" :min="1" :max="5000" size="small"
                                        class="capacity-input" @click.stop />
                                    <span class="capacity-unit">人次</span>
                                </div>
                                <button class="publish-btn" @click.stop="handleConfirm(plan)">确认展期并正式发布</button>
                                <p class="publish-tip">发布后观众即可在线预约入场</p>
                            </div>
                            <div class="action-live" v-else>
                                <div class="stat-block">
                                    <span class="stat-num">{{ plan.maxCapacity }}</span>
                                    <span class="stat-label">每日预约名额</span>
                                </div>
                                <button class="view-list-btn" @click.stop="openReservations(plan)">查看预约名单</button>
                                <button class="edit-content-btn" @click.stop="goEdit(plan)">✏️ 编辑介绍内容</button>
                            </div>
                        </div>
                    </div>
                </div>
            </el-tab-pane>

            <el-tab-pane :label="`策展历史记录（${historyPlans.length}）`" name="history">
                <div class="empty-state" v-if="historyPlans.length === 0">
                    <div class="empty-char">无</div>
                    <p class="empty-text">暂无历史策展记录</p>
                </div>
                <div class="history-list" v-else>
                    <div class="history-item" v-for="plan in historyPlans" :key="plan.exhibitionId"
                        @click="openDetail(plan, true)" style="cursor:pointer">
                        <div class="history-cover">
                            <el-image
                                :src="plan.coverURL || 'https://images.unsplash.com/photo-1566127444979-b3d2b654e3d7?w=400'"
                                fit="cover" class="history-img" />
                        </div>
                        <div class="history-info">
                            <div class="history-name">{{ plan.title }}</div>
                            <div class="history-meta">{{ plan.location }} · {{ formatDate(plan.startTime) }} — {{
                                formatDate(plan.endTime) }}</div>
                        </div>
                        <div style="display:flex;align-items:center;gap:10px">
                            <button class="view-list-btn" style="width:auto;padding:0 14px"
                                @click.stop="openReservations(plan)">预约名单</button>
                            <div :class="['history-tag', plan.currentStatus === 2 ? 'tag-revoked' : 'tag-ended']">
                                {{ historyReason(plan) }}
                            </div>
                        </div>
                    </div>
                </div>
            </el-tab-pane>
        </el-tabs>
    </div>
    <el-dialog v-model="detailVisible" title="展览详情" width="620px" destroy-on-close>
        <div v-if="detailData" class="detail-dialog">
            <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="展览名称" :span="2">{{ detailData.application?.title }}</el-descriptions-item>
                <el-descriptions-item label="举办地点" :span="2">{{ detailData.application?.location
                    }}</el-descriptions-item>
                <el-descriptions-item label="开展时间">{{ formatDate(detailData.application?.startTime)
                    }}</el-descriptions-item>
                <el-descriptions-item label="结束时间">{{ formatDate(detailData.application?.endTime)
                    }}</el-descriptions-item>
                <el-descriptions-item label="每日限额">{{ detailData.application?.maxCapacity }} 人</el-descriptions-item>
                <el-descriptions-item label="状态">
                    <el-tag :type="['info', 'success', 'danger'][detailData.application?.currentStatus]" size="small">
                        {{ ['待发布', '进行中', '已结束'][detailData.application?.currentStatus] }}
                    </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="场次配置" :span="2">
                    <span v-for="s in parseJson(detailData.application?.timeSlotConfig)" :key="s.name"
                        style="margin-right:12px">
                        {{ s.name }}（{{ s.timeRange?.join('-') }}，{{ s.quota }}人）
                    </span>
                </el-descriptions-item>
                <el-descriptions-item label="预约须知" :span="2">{{ detailData.application?.bookingNotice || '—'
                    }}</el-descriptions-item>
            </el-descriptions>

            <div style="margin-top:16px">
                <div class="section-sub-title">借用文物清单</div>
                <div v-if="!detailData.artifacts?.length" style="color:#999;font-size:13px;padding:8px 0">暂无借用文物</div>
                <div class="artifact-mini-list">
                    <div v-for="art in detailData.artifacts" :key="art.artifactId" class="artifact-mini-item">
                        <el-image :src="art.imageUrl" fit="cover" class="artifact-mini-img" />
                        <div class="artifact-mini-info">
                            <div class="artifact-mini-name">{{ art.artifactName }}</div>
                            <div class="artifact-mini-meta">{{ art.dynasty }} · {{ art.category }}</div>
                            <div class="artifact-mini-meta">{{ art.ownerMuseum || art.storageLocation }}</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <div v-else style="text-align:center;padding:20px;color:#999">加载中…</div>

        <template #footer>
            <el-button v-if="!isHistoryDetail" type="primary" @click="goEdit(currentPlan)">✏️ 编辑介绍内容</el-button>
            <el-button @click="detailVisible = false">关闭</el-button>
        </template>
    </el-dialog>
    <!-- 预约名单弹窗 -->
    <el-dialog v-model="reservationVisible" title="预约名单" width="700px" destroy-on-close>
        <div style="display:flex;gap:10px;margin-bottom:12px;flex-wrap:wrap">
            <el-input v-model="resSearch.name" placeholder="参观人姓名" clearable style="width:140px" />
            <el-input v-model="resSearch.phone" placeholder="联系电话" clearable style="width:140px" />
            <el-input v-model="resSearch.code" placeholder="核销码" clearable style="width:150px" />
            <el-date-picker v-model="resSearch.date" type="date" placeholder="参观日期" value-format="YYYY-MM-DD" clearable
                style="width:150px" />
            <el-button @click="resetResSearch">重置</el-button>
        </div>
        <el-table :data="filteredResList" size="small" border v-loading="reservationLoading">
            <el-table-column prop="userName" label="参观人" width="90" />
            <el-table-column prop="phone" label="联系电话" width="120" />
            <el-table-column prop="reserveDate" label="参观日期" width="110" />
            <el-table-column prop="timeSlot" label="场次" width="100" />
            <el-table-column prop="ticketType" label="票种" width="90" />
            <el-table-column label="状态" width="90">
                <template #default="{ row }">
                    <el-tag :type="['warning', 'success', 'info'][row.status]" size="small">
                        {{ ['待核销', '已参观', '已取消'][row.status] }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="verifyCode" label="核销码" />
        </el-table>
        <div style="margin-top:10px;font-size:13px;color:#999">
            共 {{ filteredResList.length }} 条（全部 {{ reservationList.length }} 条）
        </div>
    </el-dialog>
</template>

<script setup>
import { ref, computed,onMounted,reactive} from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import axios from 'axios'
const activeTab = ref('active')
const isHistoryDetail = ref(false)
const currentPlan = ref(null)
const router = useRouter()
axios.defaults.withCredentials = true
const allPlans = ref([]) // 存全部数据
const detailVisible = ref(false)
const detailData = ref(null)
const reservationVisible = ref(false)
const reservationList = ref([])
const reservationLoading = ref(false)
const resSearch = reactive({ name: '', phone: '', code: '', date: '' })

const filteredResList = computed(() => {
    return reservationList.value.filter(r => {
        if (resSearch.name && !r.userName?.includes(resSearch.name)) return false
        if (resSearch.phone && !r.phone?.includes(resSearch.phone)) return false
        if (resSearch.code && !r.verifyCode?.includes(resSearch.code)) return false
        if (resSearch.date && r.reserveDate !== resSearch.date) return false
        return true
    })
})

const resetResSearch = () => {
    resSearch.name = ''
    resSearch.phone = ''
    resSearch.code = ''
    resSearch.date = ''
}
// 跳转编辑页
const goEdit = (plan) => {
    detailVisible.value = false
    router.push({ path: '/planner/exhibition/editor', query: { exhibitionId: plan.exhibitionId } })
}

const parseJson = (val) => {
    if (!val) return []
    try { return JSON.parse(val) } catch { return [] }
}

// 点击展览卡片查看详情
// const openDetail = async (plan) => {
//     detailData.value = null
//     detailVisible.value = true
//     const res = await axios.get(`/api/exhibition/detail/${plan.exhibitionId}`)
//     if (res.data.code === 200) detailData.value = res.data.data
// }

// 改回弹窗逻辑，加 isHistory 参数区分
const openDetail = async (plan, isHistory = false) => {
    isHistoryDetail.value = isHistory
    currentPlan.value = plan
    detailData.value = null
    detailVisible.value = true
    const res = await axios.get(`/api/exhibition/detail/${plan.exhibitionId}`)
    if (res.data.code === 200) detailData.value = res.data.data
}

// 查看预约名单
const openReservations = async (plan) => {
    reservationList.value = []
    reservationVisible.value = true
    reservationLoading.value = true
    try {
        const res = await axios.get('/api/reservation/byExhibition', {
            params: { exhibitionId: plan.exhibitionId }
        })
        const data = res.data
        reservationList.value = Array.isArray(data) ? data : (data?.data || [])
    } finally {
        reservationLoading.value = false
    }
}

const loadPlans = async () => {
    const user = JSON.parse(sessionStorage.getItem('user') || '{}')
    console.log('当前用户:', user)  // 加这行
    if (!user?.userId) return ElMessage.warning('请先登录')
    try {
        const res = await axios.get('/api/exhibition/plan/my-plans', {
            params: { userId: user.userId }
        })
        console.log('接口返回:', JSON.stringify(res.data))  // 加这行
        if (res.data.code === 200) allPlans.value = res.data.data
    } catch (e) {
        ElMessage.error('获取数据失败')
        console.error(e)  // 加这行
    }
}

const today = new Date().toISOString().slice(0, 10)

// 进行中/待发布：没失效且没过期
const plans = computed(() =>
    allPlans.value.filter(p =>
        p.currentStatus !== 2 && (!p.endTime || p.endTime.slice(0, 10) >= today)
    )
)

// 历史记录：已撤回 或 已过期
const historyPlans = computed(() =>
    allPlans.value.filter(p =>
        p.currentStatus === 2 || (p.endTime && p.endTime.slice(0, 10) < today)
    )
)

const historyReason = (plan) => {
    if (plan.currentStatus === 2) return '已撤回'
    if (plan.endTime && plan.endTime.slice(0, 10) < today) return '已结束'
    return '已归档'
}

const handleReEdit = (plan) => {
    ElMessageBox.confirm(
        '确认撤回吗？原发布的展讯将失效，您需要重新修改并提交审批。',
        '操作确认',
        { confirmButtonText: '确定撤回', cancelButtonText: '点错了', type: 'warning' }
    ).then(async () => {
        try {
            const res = await axios.post('/api/exhibition/re-edit', {
                exhibitionId: plan.exhibitionId,
                appId: plan.appId
            })
            if (res.data.code === 200) {
                ElMessage.success('撤回成功，正在前往编辑页面...')
                setTimeout(() => {
                    router.push({ path: '/planner/apply', query: { editId: plan.appId } })
                }, 1000)
            } else {
                ElMessage.error(res.data.message)
            }
        } catch (e) {
            ElMessage.error('网络连接失败')
        }
    }).catch(() => { })
}

const handleConfirm = (plan) => {
    ElMessageBox.confirm(`确定要在 ${plan.location} 正式发布展讯吗？`, '发布确认')
        .then(async () => {
            try {
                const res = await axios.post('/api/exhibition/plan/confirm', {
                    exhibitionId: plan.exhibitionId,
                    maxCapacity: plan.maxCapacity
                })
                if (res.data.code === 200) {
                    ElMessage.success('展讯已发布！')
                    loadPlans()
                }
            } catch (e) {
                ElMessage.error('发布操作失败')
            }
        })
}

const formatDate = (d) => d ? d.substring(0, 10) : '—'

onMounted(loadPlans)
</script>

<style scoped>
/* ── 全局变量 ── */
.publish-root {
    --ink: #1a1714;
    --ink-soft: #3e3a36;
    --gold: #9a7d4a;
    --gold-light: #f5edd8;
    --gold-mid: #e0cfa8;
    --muted: #8a8078;
    --border: #e5e0d8;
    --bg: #f7f4ef;
    --card: #ffffff;
    --red: #8b2020;
    --green: #2a6b46;

    font-family: "PingFang SC", "Hiragino Sans GB", "Noto Serif SC", serif;
    padding: 40px 48px 60px;
    max-width: 1200px;
    margin: 0 auto;
    min-height: 100vh;
    background: var(--bg);
    color: var(--ink);
}

/* ── 顶部 Header ── */
.page-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    margin-bottom: 16px;
    padding-bottom: 24px;
    border-bottom: 1px solid var(--border);
}

.header-left {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.back-btn {
    background: none;
    border: none;
    font-size: 13px;
    color: var(--muted);
    cursor: pointer;
    padding: 0;
    font-family: inherit;
    letter-spacing: 1px;
    transition: color 0.2s;
    text-align: left;
}

.back-btn:hover {
    color: var(--gold);
}

.eyebrow {
    display: block;
    font-size: 10px;
    letter-spacing: 4px;
    color: var(--muted);
    margin-bottom: 6px;
}

.main-title {
    font-size: 26px;
    font-weight: 500;
    color: var(--ink);
    margin: 0;
    letter-spacing: 3px;
}

/* 印章 */
.header-seal {
    width: 52px;
    height: 52px;
    border: 2px solid var(--red);
    border-radius: 4px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 22px;
    color: var(--red);
    font-weight: 500;
    opacity: 0.8;
    letter-spacing: 0;
    flex-shrink: 0;
}

/* 副标题提示 */
.subtitle-notice {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    font-size: 13px;
    color: var(--muted);
    margin-bottom: 36px;
    padding: 12px 16px;
    background: var(--gold-light);
    border-left: 3px solid var(--gold);
    border-radius: 0 4px 4px 0;
    line-height: 1.7;
}

.notice-dot {
    width: 6px;
    height: 6px;
    background: var(--gold);
    border-radius: 50%;
    flex-shrink: 0;
    margin-top: 7px;
}

/* ── 空状态 ── */
.empty-state {
    text-align: center;
    padding: 80px 0;
}

.empty-char {
    font-size: 72px;
    color: #d4cdc4;
    font-weight: 300;
    margin-bottom: 16px;
    line-height: 1;
}

.empty-text {
    font-size: 16px;
    color: var(--muted);
    letter-spacing: 2px;
    margin-bottom: 8px;
}

.empty-sub {
    font-size: 12px;
    color: #b0a89e;
    letter-spacing: 1px;
}

/* ── 计划卡片 ── */
.plan-list {
    display: flex;
    flex-direction: column;
    gap: 24px;
}

.plan-card {
    display: flex;
    gap: 0;
    background: var(--card);
    border-radius: 8px;
    border: 1px solid var(--border);
    overflow: hidden;
    box-shadow: 0 4px 20px rgba(60, 50, 40, 0.06);
    transition: box-shadow 0.3s ease, transform 0.3s ease;
}

.plan-card:hover {
    box-shadow: 0 12px 36px rgba(60, 50, 40, 0.10);
    transform: translateY(-2px);
}

/* 封面列 */
.cover-col {
    width: 240px;
    flex-shrink: 0;
    position: relative;
}

.cover-img {
    width: 100%;
    height: 100%;
    min-height: 200px;
    display: block;
}

.status-badge {
    position: absolute;
    top: 14px;
    left: 14px;
    font-size: 11px;
    letter-spacing: 2px;
    padding: 4px 12px;
    border-radius: 2px;
    font-weight: 500;
}

.badge-live {
    background: rgba(42, 107, 70, 0.88);
    color: #d4f0e2;
}

.badge-pending {
    background: rgba(154, 125, 74, 0.85);
    color: #fdf5e0;
}

/* 信息列 */
.info-col {
    flex: 1;
    padding: 28px 32px;
    border-right: 1px solid var(--border);
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.info-top {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
}

.plan-title {
    font-size: 20px;
    font-weight: 500;
    color: var(--ink);
    margin: 0;
    letter-spacing: 2px;
    line-height: 1.4;
    flex: 1;
}

.re-edit-btn {
    flex-shrink: 0;
    background: none;
    border: 1px solid #d8c8c8;
    border-radius: 20px;
    color: var(--red);
    font-size: 12px;
    padding: 5px 14px;
    cursor: pointer;
    font-family: inherit;
    letter-spacing: 1px;
    transition: all 0.2s;
    opacity: 0.8;
}

.re-edit-btn:hover {
    background: #fdf0f0;
    border-color: var(--red);
    opacity: 1;
}

.meta-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 14px 24px;
}

.meta-item {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.meta-item.full-width {
    grid-column: 1 / -1;
}

.meta-label {
    font-size: 10px;
    letter-spacing: 2px;
    color: var(--muted);
    text-transform: uppercase;
}

.meta-val {
    font-size: 14px;
    color: var(--ink-soft);
    line-height: 1.5;
}

.meta-val.desc {
    font-size: 13px;
    color: var(--muted);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

/* 操作列 */
.action-col {
    width: 230px;
    flex-shrink: 0;
    padding: 28px 24px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    background: #fdfcfa;
}

/* 待发布 */
.action-pending {
    display: flex;
    flex-direction: column;
    gap: 16px;
    align-items: center;
    text-align: center;
}

.capacity-block {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    width: 100%;
    padding: 14px;
    background: var(--gold-light);
    border-radius: 6px;
    border: 1px solid var(--gold-mid);
}

.capacity-label {
    font-size: 11px;
    letter-spacing: 2px;
    color: var(--muted);
}

.capacity-unit {
    font-size: 11px;
    color: var(--muted);
}

.publish-btn {
    width: 100%;
    height: 44px;
    border-radius: 4px;
    border: none;
    background: var(--ink);
    color: var(--gold-light);
    font-size: 13px;
    letter-spacing: 2px;
    cursor: pointer;
    font-family: inherit;
    transition: all 0.25s;
}

.publish-btn:hover {
    background: #3e3530;
    box-shadow: 0 6px 20px rgba(26, 23, 20, 0.2);
    transform: translateY(-1px);
}

.publish-tip {
    font-size: 11px;
    color: #b8b0a6;
    letter-spacing: 1px;
    margin: 0;
    line-height: 1.6;
}

/* 已发布 */
.action-live {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 20px;
    text-align: center;
}

.stat-block {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    padding: 16px;
    width: 100%;
    background: linear-gradient(135deg, #f0f9f4, #e8f5ee);
    border-radius: 6px;
    border: 1px solid #c8e4d4;
}

.stat-num {
    font-size: 36px;
    font-weight: 300;
    color: var(--green);
    line-height: 1;
    font-variant-numeric: tabular-nums;
}

.stat-label {
    font-size: 11px;
    letter-spacing: 2px;
    color: #5a8a70;
}

.view-list-btn {
    width: 100%;
    height: 38px;
    border-radius: 4px;
    border: 1px solid #c8e4d4;
    background: #fff;
    color: var(--green);
    font-size: 13px;
    letter-spacing: 1px;
    cursor: pointer;
    font-family: inherit;
    transition: all 0.2s;
}

.view-list-btn:hover {
    background: #f0f9f4;
    border-color: var(--green);
}

/* 响应式 */
@media (max-width: 900px) {
    .publish-root {
        padding: 24px 20px 40px;
    }

    .plan-card {
        flex-direction: column;
    }

    .cover-col {
        width: 100%;
        height: 180px;
    }

    .cover-img {
        min-height: 180px;
    }

    .info-col {
        border-right: none;
        border-bottom: 1px solid var(--border);
        padding: 20px;
    }

    .action-col {
        width: 100%;
        padding: 20px;
    }
}
.history-section {
    margin-top: 40px;
    border-top: 1px dashed var(--border);
    padding-top: 28px;
}

.history-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
}

.history-title {
    font-size: 14px;
    letter-spacing: 2px;
    color: var(--muted);
}

.history-count {
    font-size: 12px;
    color: #b0a89e;
}

.history-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.history-item {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 14px 18px;
    background: #faf9f7;
    border: 1px solid var(--border);
    border-radius: 6px;
    opacity: 0.75;
}

.history-cover {
    width: 60px;
    height: 44px;
    flex-shrink: 0;
    border-radius: 4px;
    overflow: hidden;
}

.history-img {
    width: 100%;
    height: 100%;
}

.history-info {
    flex: 1;
    min-width: 0;
}

.history-name {
    font-size: 14px;
    color: var(--ink-soft);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    margin-bottom: 4px;
}

.history-meta {
    font-size: 12px;
    color: var(--muted);
}

.history-tag {
    flex-shrink: 0;
    font-size: 11px;
    padding: 3px 10px;
    border-radius: 2px;
    letter-spacing: 1px;
}

.tag-revoked {
    background: #fdf0f0;
    color: var(--red);
    border: 1px solid #f0d0d0;
}

.tag-ended {
    background: #f5f4f2;
    color: var(--muted);
    border: 1px solid var(--border);
}

.section-sub-title {
    font-size: 13px;
    font-weight: 600;
    color: var(--ink-soft);
    margin-bottom: 10px;
    letter-spacing: 1px;
}

.artifact-mini-list {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
}

.artifact-mini-item {
    display: flex;
    gap: 10px;
    align-items: center;
    width: calc(50% - 5px);
    padding: 8px;
    background: #faf9f7;
    border: 1px solid var(--border);
    border-radius: 6px;
}

.artifact-mini-img {
    width: 52px;
    height: 52px;
    border-radius: 4px;
    flex-shrink: 0;
}

.artifact-mini-name {
    font-size: 13px;
    font-weight: 500;
    color: var(--ink);
}

.artifact-mini-meta {
    font-size: 11px;
    color: var(--muted);
    margin-top: 2px;
}

.edit-content-btn {
    width: 100%;
    height: 38px;
    border-radius: 4px;
    border: 1px solid var(--gold);
    background: #fff;
    color: var(--gold);
    font-size: 13px;
    letter-spacing: 1px;
    cursor: pointer;
    font-family: inherit;
    transition: all 0.2s;
    margin-top: 8px;
}

.edit-content-btn:hover {
    background: #fdf8ee;
}
</style>