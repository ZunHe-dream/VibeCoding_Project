<template>
    <div class="progress-container">
        <div class="status-overview">
            <el-row :gutter="20">
                <el-col :span="8" v-for="stat in summaryStats" :key="stat.label">
                    <div class="stat-card" :style="{ borderLeft: `4px solid ${stat.color}` }">
                        <div class="stat-value">{{ stat.count }}</div>
                        <div class="stat-label">{{ stat.label }}</div>
                    </div>
                </el-col>
            </el-row>
        </div>

        <el-card class="timeline-card">
            <template #header>
                <div class="card-header">
                    <span class="header-title">📜 巡展申请存照</span>
                    <el-radio-group v-model="filterStatus" size="small" @change="handleFilter">
                        <el-radio-button :label="-1">全部</el-radio-button>
                        <el-radio-button :label="0">评审中</el-radio-button>
                        <el-radio-button :label="1">已获准</el-radio-button>
                        <el-radio-button :label="2">未通过</el-radio-button>
                    </el-radio-group>
                </div>
            </template>

            <el-timeline v-if="filteredList.length > 0">
                <el-timeline-item v-for="app in filteredList" :key="app.appId" :timestamp="formatTime(app.createTime)"
                    placement="top" :type="getTimelineType(app.status)">
                    <div class="application-item">
                        <div v-if="app.status !== 0" :class="['seal', app.status === 1 ? 'seal-pass' : 'seal-reject']">
                            {{ app.status === 1 ? '准予巡展' : '不予通过' }}
                        </div>

                        <div class="item-content">
                            <h3 class="item-title">{{ app.title }}</h3>
                            <div class="item-meta">
                                <span>📍 地点：{{ app.targetLocation }}</span>
                                <span class="meta-divider">|</span>
                                <span>📅 周期：{{ app.startDate }} 至 {{ app.endDate }}</span>
                            </div>

                            <div class="item-actions">
                                <el-button type="primary" link @click="viewDetail(app.appId)">
                                    查看详情公文
                                </el-button>
                                <el-button v-if="app.status === 0" type="danger" link
                                    @click="handleCancel(app.appId)">撤回申请</el-button>
                            </div>
                        </div>
                    </div>
                </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无相关申请记录" />
        </el-card>

        <el-dialog v-model="detailVisible" title="巡展申请详细公文" width="750px" custom-class="museum-dialog">
            <div v-loading="detailLoading" class="briefcase-content">
                <div class="paper-section">
                    <h2 class="paper-title">{{ currentDetail.application?.title }}</h2>
                    <div class="paper-meta">
                        <p><strong>申请人ID：</strong> {{ currentDetail.appDetail?.applicantId }}</p>
                        <p><strong>申请单位：</strong> {{   user.belong }}</p>
                        <p><strong>巡展地点：</strong> {{ currentDetail.application?.location }}</p>
                        <p><strong>起止日期：</strong> {{ currentDetail.appDetail?.startDate }} 至 {{
                            currentDetail.appDetail?.endDate }}</p>
                    </div>

                    <el-divider content-position="left">方案描述</el-divider>
                    <div class="paper-body">
                        {{ currentDetail.appDetail?.content }}
                    </div>

                    <el-divider content-position="left">借调文物清单</el-divider>
                    <div class="artifact-mini-list">
                        <div v-for="relic in currentDetail.artifacts" :key="relic.artifactId" class="relic-item">
                            <el-image :src="relic.imageUrl" fit="cover" class="relic-img" />
                            <p class="relic-name">{{ relic.artifactName }}</p>
                        </div>
                    </div>

                    <div v-if="currentDetail.application?.status !== 0" class="audit-feedback">
                        <el-divider />
                        <p class="audit-time">审批时间：{{ formatTime(currentDetail.appDetail?.auditTime) }}</p>
                        <p class="audit-remark"><strong>馆方批复：</strong>{{ currentDetail.appDetail?.auditRemark ||
                            '等待审批中'
                            }}</p>
                    </div>
                </div>
            </div>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const rawList = ref([])
const filterStatus = ref(-1)
const detailVisible = ref(false)
const detailLoading = ref(false)
const currentDetail = ref({})
const currentUserId = ref(null)
const user = ref(null)

// 初始化
// onMounted(async () => {
//     const userJson = sessionStorage.getItem('user')
//     const user = userJson ? JSON.parse(userJson) : null
//     if (user) { currentUserId.value = user.userId }
//     else {
//         console.warn("未在 LocalStorage 中找到名为 'user' 的信息")
//     }
//     fetchList()
// })
onMounted(async () => {
    const userJson = sessionStorage.getItem('user')
    // 2. 注意这里！不要写 const user，而是直接给外面定义的 user.value 赋值
    if (userJson) {
        user.value = JSON.parse(userJson)
        console.log('user信息已成功赋值到响应式变量：', user.value)
        currentUserId.value = user.value.userId
    } else {
        console.warn("未在 SessionStorage 中找到名为 'user' 的信息")
    }
    fetchList()
})

// 获取列表并过滤（只看自己的）
const fetchList = async () => {
    try {
        const res = await axios.get('/api/exhibition/list')
        if (res.data.code === 200) {
            // 过滤出当前登录用户的申请
            rawList.value = res.data.data.filter(item => item.applicantId === Number(currentUserId.value))
        }
    } catch (e) {
        ElMessage.error("获取进度列表失败")
    }
}

// 统计数据
const summaryStats = computed(() => [
    { label: '评审中', count: rawList.value.filter(i => i.status === 0).length, color: '#409EFF' },
    { label: '已获准', count: rawList.value.filter(i => i.status === 1).length, color: '#67C23A' },
    { label: '未通过', count: rawList.value.filter(i => i.status === 2).length, color: '#F56C6C' }
])

// 过滤列表
const filteredList = computed(() => {
    if (filterStatus.value === -1) return rawList.value
    return rawList.value.filter(item => item.status === filterStatus.value)
})

// 查看详情（对接 detail/{id} 接口）
const viewDetail = async (id) => {
    detailVisible.value = true
    detailLoading.value = true
    try {
        const res = await axios.get(`/api/exhibition/detail/${id}`)
        if (res.data.code === 200) {
            currentDetail.value = res.data.data
        }
    } catch (e) {
        ElMessage.error("获取详情失败")
    } finally {
        detailLoading.value = false
    }
}

// 撤回逻辑
// 真实的撤回逻辑
const handleCancel = (id) => {
    ElMessageBox.confirm(
        '确定要撤回这份巡展申请吗？撤回后数据将无法恢复。',
        '警告',
        {
            confirmButtonText: '确定撤回',
            cancelButtonText: '再想想',
            type: 'warning',
            buttonSize: 'default'
        }
    ).then(async () => {
        try {
            // 调用后端新增的 delete 接口
            const res = await axios.delete(`/api/exhibition/delete/${id}`)
            if (res.data.code === 200) {
                ElMessage.success("申请已成功撤回")
                // 重新获取列表，刷新页面显示
                fetchList()
            } else {
                ElMessage.error(res.data.msg || "撤回失败")
            }
        } catch (error) {
            console.error("撤回请求报错：", error)
            ElMessage.error("网络异常，无法连接服务器")
        }
    }).catch(() => {
        // 用户点击取消，不做操作
    })
}

// 工具函数
const formatTime = (timeStr) => {
    if (!timeStr) return ''
    return timeStr.replace('T', ' ').substring(0, 16)
}

const getTimelineType = (status) => {
    const map = { 0: 'primary', 1: 'success', 2: 'danger' }
    return map[status]
}

const handleFilter = () => { /* 计算属性自动处理 */ }
</script>

<style scoped>
.progress-container {
    max-width: 1000px;
    margin: 20px auto;
    padding: 0 20px;
}

/* 统计卡片 */
.status-overview {
    margin-bottom: 25px;
}

.stat-card {
    background: white;
    padding: 20px;
    border-radius: 8px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
    text-align: center;
}

.stat-value {
    font-size: 24px;
    font-weight: bold;
    color: #303133;
}

.stat-label {
    color: #909399;
    font-size: 14px;
    margin-top: 5px;
}

/* 时间轴 */
.timeline-card {
    border-radius: 12px;
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.header-title {
    font-weight: bold;
    font-size: 18px;
}

.application-item {
    background: #fdfdfd;
    border: 1px solid #f0f0f0;
    padding: 20px;
    border-radius: 8px;
    position: relative;
    overflow: hidden;
}

/* 朱红印章样式 */
.seal {
    position: absolute;
    right: 20px;
    top: 10px;
    width: 90px;
    height: 90px;
    border: 3px solid;
    border-radius: 50%;
    display: flex;
    justify-content: center;
    align-items: center;
    font-weight: bold;
    transform: rotate(-15deg);
    opacity: 0.8;
    pointer-events: none;
    font-family: "STKaiti", "楷体";
}

.seal-pass {
    color: #67c23a;
    border-color: #67c23a;
}

.seal-reject {
    color: #f56c6c;
    border-color: #f56c6c;
}

.item-title {
    margin: 0 0 10px 0;
    color: #303133;
}

.item-meta {
    font-size: 14px;
    color: #606266;
    margin-bottom: 15px;
}

.meta-divider {
    margin: 0 15px;
    color: #dcdfe6;
}

.item-actions {
    border-top: 1px dashed #eee;
    padding-top: 10px;
}

/* 公文包详情 */
.briefcase-content {
    padding: 10px;
    background-color: #fcfaf5;
    /* 宣纸色 */
    border: 1px solid #e8e0d0;
}

.paper-section {
    padding: 20px;
}

.paper-title {
    text-align: center;
    color: #2c3e50;
    margin-bottom: 30px;
    font-family: "STSong", "华文宋体";
}

.paper-body {
    line-height: 1.8;
    color: #4a4a4a;
    min-height: 100px;
    white-space: pre-wrap;
}

.artifact-mini-list {
    display: flex;
    gap: 15px;
    overflow-x: auto;
    padding: 10px 0;
}

.relic-item {
    flex-shrink: 0;
    width: 100px;
    text-align: center;
}

.relic-img {
    width: 100px;
    height: 100px;
    border-radius: 4px;
}

.relic-name {
    font-size: 12px;
    margin-top: 5px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.audit-feedback {
    margin-top: 20px;
    font-style: italic;
    color: #909399;
}
</style>