<template>
    <div class="apply-root">
        <!-- 顶部装饰Header -->
        <div class="page-hero">
            <div class="hero-bg"></div>
            <div class="hero-content">
                <div class="hero-eyebrow">EXHIBITION APPLICATION</div>
                <h1 class="hero-title">{{ isEdit ? '修改巡展申请' : '发起巡展申请' }}</h1>
                <div class="hero-meta">
                    <span class="meta-chip">申请人 · {{ applicantId || '—' }}</span>
                    <span class="meta-chip accent">{{ userInfo?.belong || '—' }}</span>
                </div>
            </div>
            <!-- 步骤指示 -->
            <div class="step-bar">
                <div v-for="(s, i) in steps" :key="i" class="step-dot"
                    :class="{ active: currentStep >= i, done: currentStep > i }" @click="scrollToSection(i)">
                    <div class="dot-circle">{{ currentStep > i ? '✓' : i + 1 }}</div>
                    <div class="dot-label">{{ s }}</div>
                </div>
                <div class="step-line"></div>
            </div>
        </div>

        <el-form :model="applyForm" :rules="rules" ref="formRef" label-position="top" class="form-body"
            v-loading="prefilling">

            <!-- Section 1: 基本信息 -->
            <div class="form-card" ref="sec0">
                <div class="card-header">
                    <div class="card-num">01</div>
                    <div class="card-title-wrap">
                        <div class="card-title">基本信息</div>
                        <div class="card-sub">填写本次巡展的核心资料</div>
                    </div>
                </div>
                <div class="card-body">
                    <el-row :gutter="32">
                        <el-col :span="14">
                            <el-form-item label="巡展主题" prop="title">
                                <el-input v-model="applyForm.title" placeholder="请输入巡展主题名称" class="styled-input"
                                    size="large" />
                            </el-form-item>
                            <el-form-item label="巡展地点" prop="targetLocation">
                                <el-input v-model="applyForm.targetLocation" placeholder="具体博物馆或地点" class="styled-input"
                                    size="large" />
                            </el-form-item>
                            <el-form-item label="巡展周期" required>
                                <div class="date-group">
                                    <el-date-picker v-model="applyForm.startDate" type="date" placeholder="开展日期"
                                        value-format="YYYY-MM-DD" :disabled-date="disablePastDates" size="large"
                                        style="flex:1" />
                                    <div class="date-arrow">→</div>
                                    <el-date-picker v-model="applyForm.endDate" type="date" placeholder="闭展日期"
                                        value-format="YYYY-MM-DD" :disabled-date="disableBeforeStartDate" size="large"
                                        style="flex:1" />
                                </div>
                            </el-form-item>
                        </el-col>
                        <el-col :span="10">
                            <el-form-item label="巡展宣传封面" prop="coverUrl">
                                <el-upload class="cover-uploader" action="/api/file/upload" :show-file-list="false"
                                    :on-success="handleCoverSuccess">
                                    <div class="cover-inner">
                                        <img v-if="applyForm.coverUrl" :src="applyForm.coverUrl" class="cover-img" />
                                        <div v-else class="cover-placeholder">
                                            <div class="cover-icon">🖼</div>
                                            <div class="cover-text">点击上传封面图</div>
                                            <div class="cover-hint">建议尺寸 400 × 267</div>
                                        </div>
                                    </div>
                                </el-upload>
                            </el-form-item>
                        </el-col>
                    </el-row>
                </div>
            </div>

            <!-- Section 2: 文物清单 -->
            <div class="form-card" ref="sec1">
                <div class="card-header">
                    <div class="card-num">02</div>
                    <div class="card-title-wrap">
                        <div class="card-title">文物清单</div>
                        <div class="card-sub">从本馆馆藏中挑选参展文物</div>
                    </div>
                    <el-button type="primary" class="pick-btn" @click="openSelector">
                        <span>＋</span> 挑选文物
                    </el-button>
                </div>
                <div class="card-body">
                    <div v-if="selectedObjects.length > 0" class="relic-wall">
                        <div v-for="item in selectedObjects" :key="item.artifactId" class="relic-card"
                            @click="showRelicDetail(item)">
                            <el-image :src="item.imageUrl" fit="cover" class="relic-img" />
                            <div class="relic-overlay">
                                <div class="relic-name">{{ item.artifactName }}</div>
                                <div class="relic-remove" @click.stop="removeArtifact(item.artifactId)">✕</div>
                            </div>
                        </div>
                        <div class="relic-count-chip">共 {{ selectedObjects.length }} 件</div>
                    </div>
                    <div v-else class="relic-empty">
                        <div class="empty-icon">🏺</div>
                        <div class="empty-text">尚未选择任何文物</div>
                        <div class="empty-hint">点击右上角"挑选文物"按钮开始选择</div>
                    </div>
                </div>
            </div>

            <!-- Section 3: 预约与票务 -->
            <div class="form-card" ref="sec2">
                <div class="card-header">
                    <div class="card-num">03</div>
                    <div class="card-title-wrap">
                        <div class="card-title">预约与票务</div>
                        <div class="card-sub">配置参观名额与票种价格</div>
                    </div>
                </div>
                <div class="card-body">
                    <div class="capacity-row">
                        <div class="capacity-label">日最大总承载量</div>
                        <el-input-number v-model="applyForm.maxCapacity" :min="1" size="large" style="width:180px" />
                        <span class="capacity-unit">人 / 天</span>
                    </div>

                    <el-row :gutter="32" style="margin-top:24px">
                        <el-col :span="12">
                            <div class="config-title">🎫 票种配置</div>
                            <div v-for="(ticket, idx) in applyForm.ticketTypes" :key="idx" class="config-row">
                                <el-input v-model="ticket.name" placeholder="票种名称" />
                                <span class="config-sep">¥</span>
                                <el-input-number v-model="ticket.price" :precision="2" :min="0" :max="999.99"
                                    style="width:250px" />
                                <el-button text type="danger" :icon="Delete"
                                    @click="applyForm.ticketTypes.splice(idx, 1)"
                                    :disabled="applyForm.ticketTypes.length <= 1" />
                            </div>
                            <div class="add-link" @click="applyForm.ticketTypes.push({ name: '', price: 0 })">
                                ＋ 添加票种
                            </div>
                        </el-col>
                        <el-col :span="12">
                            <div class="config-title">🕐 场次配置</div>
                            <div v-for="(slot, idx) in applyForm.timeSlotConfig" :key="idx" class="config-row slot-row">
                                <el-input v-model="slot.name" placeholder="场次名" style="width:80px" />
                                <el-time-picker v-model="slot.timeRange" is-range value-format="HH:mm"
                                    style="width:170px" />
                                <el-input-number v-model="slot.quota" :min="0" style="width:130px" />
                                <span class="slot-unit">人</span>
                                <el-button text type="danger" :icon="Delete"
                                    @click="applyForm.timeSlotConfig.splice(idx, 1)"
                                    :disabled="applyForm.timeSlotConfig.length <= 1" />
                            </div>
                            <div class="add-link" @click="addTimeSlot">＋ 添加场次</div>
                        </el-col>
                    </el-row>
                </div>
            </div>

            <!-- Section 4: 方案描述 -->
            <div class="form-card" ref="sec3">
                <div class="card-header">
                    <div class="card-num">04</div>
                    <div class="card-title-wrap">
                        <div class="card-title">方案描述</div>
                        <div class="card-sub">补充展览详情与观众须知</div>
                    </div>
                </div>
                <div class="card-body">
                    <el-form-item label="巡展详细说明" prop="content">
                        <el-input v-model="applyForm.content" type="textarea" :rows="4" placeholder="介绍本次巡展的背景、特色、亮点…"
                            class="styled-input" />
                    </el-form-item>
                    <el-form-item label="预约须知（观众端展示）" prop="bookingNotice">
                        <el-input v-model="applyForm.bookingNotice" type="textarea" :rows="3"
                            placeholder="参观注意事项、禁止携带物品、退票规则…" class="styled-input" />
                    </el-form-item>
                </div>
            </div>

            <!-- 提交按钮 -->
            <div class="submit-zone">
                <el-button class="submit-btn" size="large" @click="submitForm(formRef)">
                    <span class="btn-text">{{ isEdit ? '保存修改' : '提交申请' }}</span>
                    <span class="btn-arrow">→</span>
                </el-button>
            </div>
        </el-form>

        <!-- 文物选择弹窗 -->
        <el-dialog v-model="dialogVisible" title="从馆藏库中挑选文物" width="1000px" top="5vh">
            <div class="selector-wrap">
                <div class="selector-side">
                    <div v-for="cat in categories" :key="cat.name"
                        :class="['side-item', activeCategory === cat.name ? 'active' : '']"
                        @click="selectCategory(cat.name)">
                        <span>{{ cat.icon }}</span>
                        <span>{{ cat.name }}</span>
                    </div>
                </div>
                <div class="selector-main">
                    <el-input v-model="searchKey" placeholder="搜索文物名称、朝代…" @input="fetchArtifacts" size="large"
                        class="selector-search">
                        <template #prefix><span>🔍</span></template>
                    </el-input>
                    <div class="artifact-grid" v-loading="loading">
                        <el-row :gutter="12">
                            <el-col :span="8" v-for="item in artifactList" :key="item.artifactId">
                                <div :class="['art-card', isAdded(item.artifactId) ? 'selected' : '']"
                                    @click="showRelicDetail(item)">
                                    <div class="art-img-wrap">
                                        <el-image :src="item.imageUrl" fit="cover" class="art-img" />
                                        <div v-if="item.status !== 0" class="art-mask">不可借用</div>
                                        <div v-if="isAdded(item.artifactId)" class="art-check">✓</div>
                                    </div>
                                    <div class="art-info">
                                        <div class="art-name">{{ item.artifactName }}</div>
                                        <div class="art-meta">{{ item.dynasty }} · {{ item.material || '质地未知' }}</div>
                                        <el-button size="small" :type="isAdded(item.artifactId) ? 'danger' : 'primary'"
                                            :disabled="item.status !== 0" @click.stop="toggleSelection(item)"
                                            style="width:100%">
                                            {{ isAdded(item.artifactId) ? '移除' : '加入清单' }}
                                        </el-button>
                                    </div>
                                </div>
                            </el-col>
                        </el-row>
                    </div>
                </div>
            </div>
            <template #footer>
                <div class="dialog-footer">
                    <span class="selected-count">已选 <strong>{{ tempSelection.length }}</strong> 件文物</span>
                    <div>
                        <el-button @click="dialogVisible = false">取消</el-button>
                        <el-button type="primary" @click="confirmSelection">确认选择</el-button>
                    </div>
                </div>
            </template>
        </el-dialog>

        <!-- 文物详情弹窗 -->
        <el-dialog v-model="relicDetailVisible" title="文物详细档案" width="500px" append-to-body>
            <div v-if="selectedRelic" class="relic-detail">
                <el-image :src="selectedRelic.imageUrl" style="width:100%;border-radius:10px;margin-bottom:16px"
                    fit="contain" />
                <el-descriptions :column="2" border size="small">
                    <el-descriptions-item label="文物名称" :span="2">{{ selectedRelic.artifactName }}</el-descriptions-item>
                    <el-descriptions-item label="作者">{{ selectedRelic.author || '佚名' }}</el-descriptions-item>
                    <el-descriptions-item label="朝代">{{ selectedRelic.dynasty }}</el-descriptions-item>
                    <el-descriptions-item label="所属单位">{{ selectedRelic.ownerMuseum || '本馆' }}</el-descriptions-item>
                    <el-descriptions-item label="借用状态">
                        <el-tag :type="selectedRelic.status === 0 ? 'success' : 'danger'" size="small">
                            {{ selectedRelic.status === 0 ? '可借用' : '不可借用' }}
                        </el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="存放位置" :span="2">{{ selectedRelic.storageLocation || '—'
                        }}</el-descriptions-item>
                </el-descriptions>
                <div class="relic-desc">{{ selectedRelic.description || '暂无描述。' }}</div>
            </div>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, CircleCloseFilled, Delete } from '@element-plus/icons-vue'
import { useRouter, useRoute } from 'vue-router'
import axios from 'axios'

const router = useRouter(); const route = useRoute()
const formRef = ref()
const isEdit = computed(() => !!route.query.editId)
const prefilling = ref(false); const dialogVisible = ref(false)
const artifactList = ref([]); const selectedObjects = ref([]); const tempSelection = ref([])
const loading = ref(false); const searchKey = ref('')
const activeCategory = ref('全部')
const currentStep = ref(0)
const steps = ['基本信息', '文物清单', '预约票务', '方案描述']

const userJson = sessionStorage.getItem('user')
const userInfo = userJson ? JSON.parse(userJson) : null
const applicantId = ref(userInfo ? (userInfo.userId || userInfo.id) : null)

const relicDetailVisible = ref(false)
const selectedRelic = ref(null)

const categories = [
    { name: '全部', icon: '🏛️' }, { name: '绘画', icon: '🎨' }, { name: '法书', icon: '📜' },
    { name: '青铜', icon: '🏺' }, { name: '瓷器', icon: '🥣' }, { name: '玉石器', icon: '💎' }
]

const applyForm = reactive({
    title: '', content: '', targetLocation: '', startDate: '', endDate: '', coverUrl: '',
    selectedArtifacts: [], maxCapacity: 500, bookingNotice: '',
    ticketTypes: [{ name: '成人票', price: 0.00 }],
    timeSlotConfig: [{ name: '全天', timeRange: ['09:00', '18:00'], quota: 500 }]
})

const rules = {
    title: [{ required: true, message: '必填', trigger: 'blur' }],
    targetLocation: [{ required: true, message: '必填', trigger: 'blur' }],
}

const scrollToSection = (i) => {
    currentStep.value = i
}

const showRelicDetail = (item) => {
    selectedRelic.value = item
    relicDetailVisible.value = true
}

const disablePastDates = (time) => time.getTime() < Date.now() - 8.64e7
const disableBeforeStartDate = (time) => {
    if (!applyForm.startDate) return disablePastDates(time)
    return time.getTime() < new Date(applyForm.startDate).getTime()
}

const handleCoverSuccess = (res) => {
    applyForm.coverUrl = 'http://localhost:8080' + res.data
}

const fetchArtifacts = async () => {
    loading.value = true
    const params = { name: searchKey.value, category: activeCategory.value === '全部' ? null : activeCategory.value }
    const res = await axios.get('/api/artifact/list', { params })
    artifactList.value = res.data.data
    loading.value = false
}

const selectCategory = (name) => { activeCategory.value = name; fetchArtifacts() }
const openSelector = () => { dialogVisible.value = true; tempSelection.value = [...selectedObjects.value]; fetchArtifacts() }
const isAdded = (id) => tempSelection.value.some(o => o.artifactId === id)
const toggleSelection = (item) => {
    const idx = tempSelection.value.findIndex(o => o.artifactId === item.artifactId)
    idx > -1 ? tempSelection.value.splice(idx, 1) : tempSelection.value.push(item)
}
const confirmSelection = () => {
    selectedObjects.value = [...tempSelection.value]
    applyForm.selectedArtifacts = selectedObjects.value.map(o => o.artifactId)
    dialogVisible.value = false
}
const removeArtifact = (id) => {
    selectedObjects.value = selectedObjects.value.filter(o => o.artifactId !== id)
    applyForm.selectedArtifacts = applyForm.selectedArtifacts.filter(i => i !== id)
}
const addTimeSlot = () => applyForm.timeSlotConfig.push({ name: '', timeRange: ['09:00', '18:00'], quota: 0 })

const submitForm = async (formEl) => {
    if (!formEl) return
    if (!applicantId.value && userInfo) applicantId.value = userInfo.id
    const totalQuota = applyForm.timeSlotConfig.reduce((sum, item) => sum + (item.quota || 0), 0)
    if (totalQuota > applyForm.maxCapacity) return ElMessage.error('各场次名额总和超过日最大承载量！')
    if (applyForm.selectedArtifacts.length === 0) return ElMessage.warning('请选择巡展文物')
    await formEl.validate(async (valid) => {
        if (!valid) return
        const postData = {
            ...applyForm,
            appId: isEdit.value ? Number(route.query.editId) : null,
            applicantId: applicantId.value,
            artifactIds: applyForm.selectedArtifacts.join(','),
            timeSlotConfig: JSON.stringify(applyForm.timeSlotConfig),
            ticketTypes: JSON.stringify(applyForm.ticketTypes)
        }
        const res = await axios.post('/api/exhibition/apply', postData)
        if (res.data.code === 200) { ElMessage.success('提交成功'); router.push('/planner/progress') }
    })
}

onMounted(() => {
    const user = JSON.parse(sessionStorage.getItem('user') || '{}')
    if (user) applicantId.value = user.userId
    if (!route.query.editId) {
        selectedObjects.value = []
        applyForm.selectedArtifacts = []
    }
})
</script>

<style scoped>
.apply-root {
    min-height: 100vh;
    background: #f0ede8;
    font-family: 'Noto Serif SC', 'Noto Sans SC', serif;
}

/* Hero Header */
.page-hero {
    background: linear-gradient(135deg, #1a0f05 0%, #3d2010 50%, #5c3320 100%);
    padding: 40px 48px 0;
    position: relative;
    overflow: hidden;
}

.hero-bg {
    position: absolute;
    inset: 0;
    background-image:
        radial-gradient(ellipse at 80% 50%, rgba(200, 169, 110, 0.15) 0%, transparent 60%),
        radial-gradient(ellipse at 20% 80%, rgba(200, 169, 110, 0.08) 0%, transparent 50%);
    pointer-events: none;
}

.hero-content {
    position: relative;
    z-index: 1;
    margin-bottom: 32px;
}

.hero-eyebrow {
    font-size: 11px;
    letter-spacing: 4px;
    color: #c8a96e;
    font-family: 'Georgia', serif;
    margin-bottom: 10px;
}

.hero-title {
    font-size: 28px;
    font-weight: 800;
    color: #f5e6c8;
    margin: 0 0 16px;
    letter-spacing: 1px;
}

.hero-meta {
    display: flex;
    gap: 10px;
}

.meta-chip {
    font-size: 12px;
    padding: 4px 12px;
    border-radius: 20px;
    background: rgba(255, 255, 255, 0.1);
    color: #d4b896;
    border: 1px solid rgba(255, 255, 255, 0.1);
}

.meta-chip.accent {
    background: rgba(200, 169, 110, 0.2);
    color: #c8a96e;
    border-color: rgba(200, 169, 110, 0.3);
}

/* 步骤条 */
.step-bar {
    display: flex;
    align-items: flex-start;
    gap: 0;
    position: relative;
    padding-bottom: 0;
}

.step-line {
    position: absolute;
    top: 16px;
    left: 16px;
    right: 16px;
    height: 1px;
    background: rgba(255, 255, 255, 0.1);
    z-index: 0;
}

.step-dot {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    position: relative;
    z-index: 1;
    padding-bottom: 20px;
}

.dot-circle {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.08);
    border: 1px solid rgba(255, 255, 255, 0.15);
    color: #7a6040;
    font-size: 13px;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.3s;
}

.step-dot.active .dot-circle {
    background: #c8a96e;
    border-color: #c8a96e;
    color: #1a0f05;
}

.step-dot.done .dot-circle {
    background: rgba(200, 169, 110, 0.3);
    border-color: #c8a96e;
    color: #c8a96e;
}

.dot-label {
    font-size: 11px;
    color: #7a6040;
    white-space: nowrap;
}

.step-dot.active .dot-label {
    color: #c8a96e;
}

/* 表单主体 */
.form-body {
    max-width: 1000px;
    margin: 0 auto;
    padding: 32px 24px 60px;
    display: flex;
    flex-direction: column;
    gap: 24px;
}

/* 卡片 */
.form-card {
    background: white;
    border-radius: 16px;
    overflow: hidden;
    box-shadow: 0 2px 12px rgba(44, 26, 14, 0.08);
}

.card-header {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 20px 28px;
    background: linear-gradient(135deg, #faf7f3, #f5efe6);
    border-bottom: 1px solid #ede4d8;
}

.card-num {
    font-size: 36px;
    font-weight: 900;
    color: #e8d5b5;
    font-family: 'Georgia', serif;
    line-height: 1;
    min-width: 48px;
}

.card-title {
    font-size: 17px;
    font-weight: 700;
    color: #2c1a0e;
}

.card-sub {
    font-size: 12px;
    color: #9a7c5a;
    margin-top: 2px;
}

.card-title-wrap {
    flex: 1;
}

.card-body {
    padding: 28px;
}

/* 输入框 */
.styled-input :deep(.el-input__wrapper),
.styled-input :deep(.el-textarea__inner) {
    border-radius: 8px;
    border-color: #e8dfd0;
}

/* 日期 */
.date-group {
    display: flex;
    align-items: center;
    gap: 12px;
    width: 100%;
}

.date-arrow {
    color: #c8a96e;
    font-size: 18px;
}

/* 封面上传 */
.cover-uploader {
    width: 100%;
}

.cover-inner {
    width: 100%;
    height: 200px;
    border: 2px dashed #d4c4a8;
    border-radius: 12px;
    overflow: hidden;
    cursor: pointer;
    transition: border-color 0.2s;
    background: #faf7f2;
}

.cover-inner:hover {
    border-color: #c8a96e;
}

.cover-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
}

.cover-placeholder {
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
}

.cover-icon {
    font-size: 32px;
}

.cover-text {
    font-size: 14px;
    color: #8b6940;
    font-weight: 600;
}

.cover-hint {
    font-size: 12px;
    color: #b8a080;
}

/* 挑选按钮 */
.pick-btn {
    background: #2c1a0e;
    border-color: #2c1a0e;
    border-radius: 8px;
    margin-left: auto;
}

.pick-btn:hover {
    background: #4a2e1a;
    border-color: #4a2e1a;
}

/* 文物展示墙 */
.relic-wall {
    display: flex;
    flex-wrap: wrap;
    gap: 14px;
    position: relative;
}

.relic-card {
    width: 110px;
    height: 110px;
    border-radius: 10px;
    overflow: hidden;
    position: relative;
    cursor: pointer;
    box-shadow: 0 2px 8px rgba(44, 26, 14, 0.15);
    transition: transform 0.2s;
}

.relic-card:hover {
    transform: translateY(-3px);
}

.relic-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.relic-overlay {
    position: absolute;
    inset: 0;
    background: linear-gradient(to top, rgba(0, 0, 0, 0.7) 0%, transparent 60%);
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    padding: 6px 8px;
    opacity: 0;
    transition: opacity 0.2s;
}

.relic-card:hover .relic-overlay {
    opacity: 1;
}

.relic-name {
    font-size: 11px;
    color: white;
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.relic-remove {
    width: 20px;
    height: 20px;
    border-radius: 50%;
    background: #ef4444;
    color: white;
    font-size: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    cursor: pointer;
}

.relic-count-chip {
    position: absolute;
    top: -10px;
    right: 0;
    background: #2c1a0e;
    color: #c8a96e;
    font-size: 11px;
    padding: 2px 10px;
    border-radius: 10px;
}

.relic-empty {
    text-align: center;
    padding: 48px;
    color: #b8a080;
}

.empty-icon {
    font-size: 40px;
    margin-bottom: 10px;
}

.empty-text {
    font-size: 15px;
    color: #7a6040;
    font-weight: 600;
}

.empty-hint {
    font-size: 12px;
    margin-top: 6px;
}

/* 票务配置 */
.capacity-row {
    display: flex;
    align-items: center;
    gap: 14px;
    background: #faf7f3;
    padding: 16px 20px;
    border-radius: 10px;
    border: 1px solid #ede4d8;
}

.capacity-label {
    font-size: 14px;
    color: #4a2e1a;
    font-weight: 600;
}

.capacity-unit {
    font-size: 13px;
    color: #9a7c5a;
}

.config-title {
    font-size: 13px;
    font-weight: 700;
    color: #4a2e1a;
    margin-bottom: 12px;
    padding-bottom: 8px;
    border-bottom: 1px solid #ede4d8;
}

.config-row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 10px;
    background: #faf7f3;
    padding: 10px 12px;
    border-radius: 8px;
}

.config-sep {
    color: #c8a96e;
    font-weight: 700;
}

.slot-unit {
    font-size: 12px;
    color: #9a7c5a;
}

.add-link {
    font-size: 13px;
    color: #8b5e3c;
    cursor: pointer;
    padding: 6px 0;
    display: inline-block;
}

.add-link:hover {
    color: #c8a96e;
}

/* 提交 */
.submit-zone {
    text-align: center;
    padding: 16px 0 8px;
}

.submit-btn {
    width: 260px;
    height: 52px;
    background: linear-gradient(135deg, #2c1a0e, #5c3320);
    border: none;
    border-radius: 26px;
    font-size: 16px;
    color: #f5e6c8;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    cursor: pointer;
    transition: all 0.3s;
    box-shadow: 0 4px 20px rgba(44, 26, 14, 0.3);
}

.submit-btn:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 28px rgba(44, 26, 14, 0.4);
    background: linear-gradient(135deg, #3d2010, #7a4428) !important;
    color: #f5e6c8 !important;
    border: none !important;
}

.btn-arrow {
    font-size: 18px;
    transition: transform 0.2s;
}

.submit-btn:hover .btn-arrow {
    transform: translateX(4px);
}

/* 文物选择弹窗 */
.selector-wrap {
    display: flex;
    height: 520px;
    gap: 0;
}

.selector-side {
    width: 150px;
    flex-shrink: 0;
    border-right: 1px solid #f0e8da;
    padding: 8px;
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.side-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 12px;
    border-radius: 8px;
    cursor: pointer;
    font-size: 13px;
    color: #5c3320;
    transition: all 0.2s;
}

.side-item:hover {
    background: #faf7f3;
}

.side-item.active {
    background: #f5efe6;
    color: #8b5e3c;
    font-weight: 700;
}

.selector-main {
    flex: 1;
    padding: 0 16px;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.selector-search {
    margin: 4px 0;
}

.artifact-grid {
    flex: 1;
}

.art-card {
    border: 2px solid #f0e8da;
    border-radius: 10px;
    overflow: hidden;
    margin-bottom: 12px;
    transition: all 0.2s;
}

.art-card.selected {
    border-color: #8b5e3c;
    box-shadow: 0 0 0 3px rgba(139, 94, 60, 0.15);
}

.art-img-wrap {
    position: relative;
    height: 130px;
}

.art-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.art-mask {
    position: absolute;
    inset: 0;
    background: rgba(255, 255, 255, 0.75);
    display: flex;
    align-items: center;
    justify-content: center;
    color: #999;
    font-weight: bold;
    font-size: 13px;
}

.art-check {
    position: absolute;
    top: 6px;
    right: 6px;
    width: 22px;
    height: 22px;
    background: #8b5e3c;
    color: white;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
}

.art-info {
    padding: 10px;
}

.art-name {
    font-weight: 700;
    font-size: 13px;
    color: #2c1a0e;
    margin-bottom: 4px;
}

.art-meta {
    font-size: 11px;
    color: #9a7c5a;
    margin-bottom: 8px;
}

.dialog-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
}

.selected-count {
    font-size: 14px;
    color: #5c3320;
}

.selected-count strong {
    color: #8b5e3c;
    font-size: 18px;
}

/* 文物详情 */
.relic-detail {
    padding: 8px;
}

.relic-desc {
    margin-top: 14px;
    background: #faf7f3;
    padding: 12px;
    border-radius: 8px;
    font-size: 13px;
    color: #666;
    line-height: 1.8;
    white-space: pre-wrap;
}
</style>