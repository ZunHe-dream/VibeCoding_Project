<template>
    <div class="planner-mgmt">
        <div class="page-header">
            <div class="header-left">
                <div class="header-badge">人员管理</div>
                <h1 class="page-title">本馆策展人员</h1>
                <p class="page-sub">{{ adminBelong }} · 策展团队管理中心</p>
            </div>
            <el-button class="add-btn" @click="openForm()">+ 新增策展人</el-button>
        </div>

        <!-- 搜索栏 -->
        <div class="search-bar">
            <el-input v-model="keyword" placeholder="搜索姓名或用户名…" clearable style="width:240px" @input="filterList"
                @clear="filterList" />
            <span class="total-tip">共 {{ filtered.length }} 名策展人</span>
        </div>

        <!-- 策展人卡片列表 -->
        <div v-loading="loading" class="planner-grid">
            <div v-if="filtered.length === 0 && !loading" class="empty-state">
                <div class="empty-icon">👤</div>
                <p>暂无策展人员，点击右上角新增</p>
            </div>

            <div v-for="p in filtered" :key="p.userId" class="planner-card">
                <div class="card-header">
                    <div class="avatar">{{ p.nickname?.slice(0, 1) || p.username?.slice(0, 1) }}</div>
                    <div class="card-identity">
                        <div class="card-name">{{ p.nickname || p.username }}</div>
                        <div class="card-username">@{{ p.username }}</div>
                    </div>
                    <div class="card-actions">
                        <el-button link size="small" @click="openForm(p)">编辑</el-button>
                        <el-button link size="small" type="danger" @click="deletePlanner(p)">删除</el-button>
                    </div>
                </div>

                <div class="card-meta">
                    <span v-if="p.phone">📞 {{ p.phone }}</span>
                    <span v-if="p.email">✉️ {{ p.email }}</span>
                </div>

                <!-- 展开/收起巡展记录 -->
                <div class="exhibitions-section">
                    <div class="section-toggle" @click="toggleExhibitions(p)">
                        <span>🗂 策展记录</span>
                        <span class="toggle-icon">{{ expanded[p.userId] ? '▲' : '▼' }}</span>
                    </div>

                    <div v-if="expanded[p.userId]" class="exhibition-list">
                        <div v-if="!exhibitionMap[p.userId]" class="loading-ex">加载中…</div>
                        <div v-else-if="exhibitionMap[p.userId].length === 0" class="no-ex">暂无策展记录</div>
                        <div v-for="ex in exhibitionMap[p.userId]" :key="ex.appId" class="ex-item">
                            <div class="ex-top">
                                <span class="ex-title">{{ ex.title }}</span>
                                <el-tag :type="exStatusType(ex.status)" size="small">{{ exStatusText(ex.status)
                                    }}</el-tag>
                            </div>
                            <div class="ex-meta">
                                {{ ex.targetLocation }} · {{ ex.startDate }} — {{ ex.endDate }}
                            </div>
                            <!-- 借用文物 -->
                            <div v-if="ex.artifactIds" class="artifact-ids">
                                <span class="artifact-label">借用文物ID：</span>
                                <el-tag v-for="id in ex.artifactIds.split(',')" :key="id" size="small" type="info"
                                    style="margin:2px;cursor:pointer" @click="goArtifact(id.trim())">
                                    #{{ id.trim() }}
                                </el-tag>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- 新增/编辑弹窗 -->
        <el-dialog v-model="formVisible" :title="formData.userId ? '编辑策展人' : '新增策展人'" width="480px" destroy-on-close>
            <el-form :model="formData" :rules="rules" ref="formRef" label-width="80px">
                <el-form-item label="用户名" prop="username">
                    <el-input v-model="formData.username" :disabled="!!formData.userId" placeholder="登录账号" />
                </el-form-item>
                <el-form-item label="姓名" prop="nickname">
                    <el-input v-model="formData.nickname" placeholder="真实姓名或昵称" />
                </el-form-item>
                <el-form-item v-if="!formData.userId" label="初始密码" prop="password">
                    <el-input v-model="formData.password" type="password" placeholder="设置初始登录密码" show-password />
                </el-form-item>
                <el-form-item label="手机号">
                    <el-input v-model="formData.phone" placeholder="选填" />
                </el-form-item>
                <el-form-item label="邮箱">
                    <el-input v-model="formData.email" placeholder="选填" />
                </el-form-item>
                <el-form-item label="所属馆">
                    <el-input :value="adminBelong" disabled />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="formVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="savePlanner">
                    {{ formData.userId ? '保存修改' : '确认新增' }}
                </el-button>
            </template>
        </el-dialog>
        <el-dialog v-model="artifactVisible" title="文物详情" width="480px" destroy-on-close>
            <div v-if="artifactDetail" class="artifact-detail">
                <img v-if="artifactDetail.imageUrl" :src="artifactDetail.imageUrl" class="artifact-detail-img" />
                <el-descriptions :column="2" border size="small" style="margin-top:14px">
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
                    <el-descriptions-item label="所属博物馆" :span="2">{{ artifactDetail.ownerMuseum ||
                        artifactDetail.storageLocation }}</el-descriptions-item>
                    <el-descriptions-item label="当前位置" :span="2">{{ artifactDetail.currentLocation || '原馆'
                        }}</el-descriptions-item>
                    <el-descriptions-item label="文物描述" :span="2">{{ artifactDetail.description || '暂无描述'
                        }}</el-descriptions-item>
                </el-descriptions>
            </div>
            <div v-else style="text-align:center;padding:20px;color:#999">加载中…</div>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const adminBelong = JSON.parse(sessionStorage.getItem('user') || '{}').belong || ''

const loading = ref(false)
const keyword = ref('')
const planners = ref([])
const expanded = reactive({})
const exhibitionMap = reactive({})
const formVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const formData = reactive({
    userId: null, username: '', nickname: '', password: '', phone: '', email: ''
})

const artifactVisible = ref(false)
const artifactDetail = ref(null)

const goArtifact = async (id) => {
    artifactDetail.value = null
    artifactVisible.value = true
    const res = await axios.get(`/api/artifact/${id}`)
    if (res.data.code === 200) artifactDetail.value = res.data.data
}

const rules = {
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    nickname: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
    password: [{ required: true, message: '请设置初始密码', trigger: 'blur' }]
}

const filtered = computed(() => {
    if (!keyword.value) return planners.value
    const k = keyword.value.toLowerCase()
    return planners.value.filter(p =>
        p.nickname?.toLowerCase().includes(k) || p.username?.toLowerCase().includes(k)
    )
})



const fetchPlanners = async () => {
    loading.value = true
    try {
        const res = await axios.get('/api/user/planners', { params: { belong: adminBelong } })
        if (res.data.code === 200) planners.value = res.data.data
    } finally {
        loading.value = false
    }
}
onMounted(fetchPlanners)

const filterList = () => { } // computed 自动响应

const toggleExhibitions = async (p) => {
    if (expanded[p.userId]) {
        expanded[p.userId] = false
        return
    }
    expanded[p.userId] = true
    if (exhibitionMap[p.userId]) return // 已加载过
    try {
        const res = await axios.get('/api/exhibition/list', { params: { applicantId: p.userId } })
        if (res.data.code === 200) exhibitionMap[p.userId] = res.data.data
    } catch {
        exhibitionMap[p.userId] = []
    }
}

const openForm = (p = null) => {
    Object.assign(formData, {
        userId: null, username: '', nickname: '', password: '', phone: '', email: ''
    })
    if (p) Object.assign(formData, { ...p, password: '' })
    formVisible.value = true
}

const savePlanner = async () => {
    console.log('开始保存，formData:', JSON.stringify(formData))
    await formRef.value.validate(async (valid) => {
        console.log('验证结果:', valid)
        if (!valid) return
        saving.value = true
        try {
            const res = await axios.post('/api/user/planner/save', {
                ...formData,
                belong: adminBelong,
                role: 'APPLICANT'
            })
            console.log('接口返回:', JSON.stringify(res.data))
            if (res.data.code === 200) {
                ElMessage.success(formData.userId ? '修改成功' : '新增成功')
                formVisible.value = false
                fetchPlanners()
            } else {
                ElMessage.error(res.data.message || '操作失败')
            }
        } catch (e) {
            console.error('请求出错:', e)
        } finally {
            saving.value = false
        }
    })
}

const deletePlanner = async (p) => {
    await ElMessageBox.confirm(
        `确认删除策展人【${p.nickname || p.username}】？删除后该账号将无法登录。`,
        '确认删除', { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
    const res = await axios.delete(`/api/user/planner/${p.userId}`)
    if (res.data.code === 200) {
        ElMessage.success('已删除')
        fetchPlanners()
    }
}

const exStatusText = (s) => ['待审批', '已通过', '已驳回'][s] ?? '未知'
const exStatusType = (s) => ['warning', 'success', 'danger'][s] ?? 'info'
</script>

<style scoped>
.planner-mgmt {
    max-width: 1100px;
    margin: 0 auto;
    padding: 28px 24px;
    font-family: 'Noto Serif SC', 'Noto Sans SC', serif;
}

.page-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    margin-bottom: 24px;
}

.header-badge {
    font-size: 11px;
    letter-spacing: 3px;
    color: #9a7c5a;
    text-transform: uppercase;
    margin-bottom: 6px;
}

.page-title {
    font-size: 24px;
    font-weight: 700;
    color: #2c1a0e;
    margin: 0 0 4px;
}

.page-sub {
    font-size: 13px;
    color: #9a7c5a;
    margin: 0;
}

.add-btn {
    background: #2c1a0e !important;
    color: #f0e6d0 !important;
    border: none !important;
    border-radius: 8px !important;
    padding: 10px 22px !important;
}

.add-btn:hover {
    background: #4a2e1a !important;
}

.search-bar {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 20px;
}

.total-tip {
    font-size: 13px;
    color: #9a7c5a;
}

.planner-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
    gap: 16px;
}

.empty-state {
    grid-column: 1/-1;
    text-align: center;
    padding: 60px;
    color: #9a7c5a;
}

.empty-icon {
    font-size: 48px;
    margin-bottom: 12px;
}

.planner-card {
    background: #fff;
    border-radius: 12px;
    overflow: hidden;
    border: 1px solid #e8dfd0;
    transition: box-shadow 0.2s;
}

.planner-card:hover {
    box-shadow: 0 6px 20px rgba(44, 26, 14, 0.1);
}

.card-header {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px 16px 10px;
}

.avatar {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    background: linear-gradient(135deg, #8b5e3c, #c8a96e);
    color: #fff;
    font-size: 18px;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
}

.card-identity {
    flex: 1;
    min-width: 0;
}

.card-name {
    font-size: 15px;
    font-weight: 700;
    color: #2c1a0e;
}

.card-username {
    font-size: 12px;
    color: #9a7c5a;
    margin-top: 2px;
}

.card-actions {
    display: flex;
    gap: 4px;
    flex-shrink: 0;
}

.card-meta {
    padding: 0 16px 12px;
    display: flex;
    gap: 16px;
    flex-wrap: wrap;
    font-size: 12px;
    color: #9a7c5a;
    border-bottom: 1px solid #f0e8dc;
}

.exhibitions-section {
    padding: 12px 16px;
}

.section-toggle {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 13px;
    color: #7a5c3a;
    cursor: pointer;
    user-select: none;
    padding: 4px 0;
}

.section-toggle:hover {
    color: #2c1a0e;
}

.toggle-icon {
    font-size: 10px;
}

.exhibition-list {
    margin-top: 10px;
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.loading-ex,
.no-ex {
    font-size: 12px;
    color: #bbb;
    text-align: center;
    padding: 8px 0;
}

.ex-item {
    background: #faf7f3;
    border-radius: 8px;
    padding: 10px 12px;
    border-left: 3px solid #c8a96e;
}

.ex-top {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 4px;
}

.ex-title {
    font-size: 13px;
    font-weight: 600;
    color: #2c1a0e;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 200px;
}

.ex-meta {
    font-size: 11px;
    color: #9a7c5a;
    margin-bottom: 4px;
}

.artifact-label {
    font-size: 11px;
    color: #b0a090;
}

.artifact-ids {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 2px;
    margin-top: 4px;
}
.artifact-detail-img {
    width: 100%;
    max-height: 240px;
    object-fit: cover;
    border-radius: 8px;
}
</style>