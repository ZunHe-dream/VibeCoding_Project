<template>
    <div class="artifact-manager">
        <!-- 顶部标题区 -->
        <div class="page-header">
            <div class="header-left">
                <div class="header-icon">🏺</div>
                <div>
                    <h1 class="page-title">馆藏文物管理</h1>
                    <p class="page-subtitle">{{ userBelong }} · 典藏管理系统</p>
                </div>
            </div>
            <el-button class="add-btn" @click="openAddDialog">
                <span class="btn-icon">＋</span> 录入新文物
            </el-button>
        </div>

        <!-- 筛选栏 -->
        <div class="filter-bar">
            <div class="category-tabs">
                <span class="cat-tab" :class="{ active: filters.category === '' }"
                    @click="filters.category = ''; fetchList()">全部</span>
                <span v-for="cat in categories" :key="cat" class="cat-tab" :class="{ active: filters.category === cat }"
                    @click="filters.category = cat; fetchList()">{{ cat }}</span>
            </div>
            <div class="search-wrap">
                <el-input v-model="filters.name" placeholder="搜索文物名、作者、朝代、馆藏地…" clearable class="search-input"
                    @input="fetchList" @clear="fetchList">
                    <template #prefix><span style="color:#9a7c5a">🔍</span></template>
                </el-input>
            </div>
        </div>

        <!-- 文物列表 -->
        <div v-loading="loading" class="artifact-grid">
            <div v-for="item in list" :key="item.artifactId" class="artifact-card">
                <div class="card-img-wrap">
                    <img :src="item.imageUrl || 'https://via.placeholder.com/300x200?text=暂无图片'" class="card-img"
                        @error="(e) => e.target.src = 'https://via.placeholder.com/300x200?text=暂无图片'" />
                    <span :class="['status-pill', statusClass(item.status)]">{{ statusText(item.status) }}</span>
                    <span class="category-pill">{{ item.category || '未分类' }}</span>
                </div>
                <div class="card-body">
                    <div class="card-name">{{ item.artifactName }}</div>
                    <div class="card-meta">
                        <span>{{ item.dynasty || '年代不详' }}</span>
                        <span class="dot">·</span>
                        <span>{{ item.author || '佚名' }}</span>
                    </div>
                    <div class="card-location">
                        <span class="loc-icon">📍</span>目前所在:{{ item.storageLocation || '未录入存放位置' }}
                    </div>
                </div>
                <div class="card-actions">
                    <el-button link class="action-btn edit-btn" @click="openEditDialog(item)">编辑</el-button>
                    <el-divider direction="vertical" />
                    <el-button link class="action-btn view-btn" @click="openViewDialog(item)">详情</el-button>
                    <el-divider direction="vertical" />
                    <el-button link class="action-btn del-btn" @click="handleDelete(item.artifactId)">删除</el-button>
                </div>
            </div>

            <div v-if="!loading && list.length === 0" class="empty-state">
                <div class="empty-icon">🏛️</div>
                <p>暂无馆藏文物记录</p>
                <el-button class="add-btn" @click="openAddDialog">立即录入</el-button>
            </div>
        </div>

        <!-- 新增/编辑弹窗 -->
        <el-dialog v-model="formVisible" :title="isEdit ? '编辑文物信息' : '录入新文物'" width="620px" class="museum-dialog"
            destroy-on-close>
            <el-form :model="form" :rules="rules" ref="formRef" label-width="90px" class="artifact-form">
                <el-row :gutter="20">
                    <el-col :span="12">
                        <el-form-item label="文物名称" prop="artifactName">
                            <el-input v-model="form.artifactName" placeholder="请输入文物名称" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="作者/匠人" prop="author">
                            <el-input v-model="form.author" placeholder="佚名" />
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-row :gutter="20">
                    <el-col :span="12">
                        <el-form-item label="分类" prop="category">
                            <el-select v-model="form.category" placeholder="请选择分类" style="width:100%">
                                <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="朝代" prop="dynasty">
                            <el-input v-model="form.dynasty" placeholder="如：明代、清代" />
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-row :gutter="20">
                    <el-col :span="12">
                        <el-form-item label="状态" prop="status">
                            <el-select v-model="form.status" style="width:100%">
                                <el-option label="馆藏中" :value="0" />
                                <el-option label="外借巡展中" :value="1" />
                                <el-option label="维修保养中" :value="2" />
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="存放位置" prop="storageLocation">
                            <el-input v-model="form.storageLocation" placeholder="如：A馆2层3号柜" />
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-form-item label="当前位置" prop="currentLocation">
                    <el-input v-model="form.currentLocation" placeholder="为空则表示在原馆" />
                </el-form-item>
                <el-form-item label="文物图片" prop="imageUrl">
                    <div class="upload-wrap">
                        <!-- 已有图片预览 -->
                        <div v-if="form.imageUrl" class="img-preview">
                            <img :src="form.imageUrl" class="preview-img" />
                            <span class="remove-img" @click="form.imageUrl = ''">✕</span>
                        </div>
                        <!-- 上传区域 -->
                        <el-upload v-else class="img-uploader" action="/api/upload/image" :show-file-list="false"
                            :on-success="handleUploadSuccess" :on-error="handleUploadError"
                            :before-upload="beforeUpload" accept="image/*">
                            <div class="upload-trigger">
                                <div class="upload-icon">📷</div>
                                <div class="upload-text">点击上传图片</div>
                                <div class="upload-hint">支持 jpg / png / gif / webp</div>
                            </div>
                        </el-upload>
                    </div>
                </el-form-item>
                <el-form-item label="文物描述" prop="description">
                    <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入文物详细描述" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="formVisible = false">取消</el-button>
                <el-button class="confirm-btn" @click="handleSubmit">{{ isEdit ? '保存修改' : '确认录入' }}</el-button>
            </template>
        </el-dialog>

        <!-- 详情弹窗 -->
        <el-dialog v-model="viewVisible" title="文物详细信息" width="600px" class="museum-dialog">
            <div v-if="viewItem" class="detail-wrap">
                <div class="detail-img-wrap">
                    <img :src="viewItem.imageUrl || 'https://via.placeholder.com/500x300?text=暂无图片'" class="detail-img"
                        @error="(e) => e.target.src = 'https://via.placeholder.com/500x300?text=暂无图片'" />
                    <span :class="['status-pill', statusClass(viewItem.status)]">{{ statusText(viewItem.status)
                        }}</span>
                </div>
                <div class="detail-info">
                    <h2 class="detail-name">{{ viewItem.artifactName }}</h2>
                    <div class="detail-grid">
                        <div class="detail-item"><span class="detail-label">分类</span><span>{{ viewItem.category || '—'
                                }}</span>
                        </div>
                        <div class="detail-item"><span class="detail-label">朝代</span><span>{{ viewItem.dynasty || '—'
                                }}</span>
                        </div>
                        <div class="detail-item"><span class="detail-label">作者</span><span>{{ viewItem.author || '佚名'
                                }}</span>
                        </div>
                        <div class="detail-item"><span class="detail-label">存放位置</span><span>{{ viewItem.storageLocation
                                || '—'
                                }}</span></div>
                        <div class="detail-item"><span class="detail-label">当前位置</span><span>{{ viewItem.currentLocation
                                || '原馆'
                                }}</span></div>
                        <div class="detail-item"><span class="detail-label">录入时间</span><span>{{
                                viewItem.createTime?.slice(0,10)
                                || '—' }}</span></div>
                    </div>
                    <div class="detail-desc">
                        <div class="detail-label" style="margin-bottom:8px">文物描述</div>
                        <p>{{ viewItem.description || '暂无描述' }}</p>
                    </div>
                </div>
            </div>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const categories = ['绘画', '法书', '碑帖', '青铜', '瓷器', '玉石器', '织绣', '珐琅器', '漆器', '文具', '古籍文献', '珍宝', '生活用具', '钟表仪器', '雕塑']

const userBelong = ref('')
onMounted(() => {
    const user = JSON.parse(sessionStorage.getItem('user') || '{}')
    userBelong.value = user.belong || '未知馆'
    fetchList()
})

// 列表
const loading = ref(false)
const list = ref([])
const filters = reactive({ category: '', name: '' })

const fetchList = async () => {
    loading.value = true
    try {
        const params = {}
        if (filters.category) params.category = filters.category
        if (filters.name) params.name = filters.name
        params.ownerMuseum = userBelong.value  // ✨ 移到 params 定义之后
        const res = await axios.get('/api/artifact/list', { params })
        list.value = Array.isArray(res.data) ? res.data : (res.data?.data || [])
    } catch (e) {
        ElMessage.error('获取文物列表失败')
    } finally {
        loading.value = false
    }
}

// 状态映射
const statusText = (s) => ({ 0: '馆藏中', 1: '外借巡展中', 2: '维修保养中' }[s] ?? '未知')
const statusClass = (s) => ({ 0: 'status-in', 1: 'status-out', 2: 'status-repair' }[s] ?? '')

// 新增/编辑
const formVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const form = reactive({
    artifactId: null,
    artifactName: '',
    author: '',
    category: '',
    dynasty: '',
    description: '',
    imageUrl: '',
    status: 0,
    storageLocation: '',
    currentLocation: ''
})
const rules = {
    artifactName: [{ required: true, message: '请输入文物名称', trigger: 'blur' }],
    category: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

const openAddDialog = () => {
    isEdit.value = false
    Object.assign(form, {
        artifactId: null, artifactName: '', author: '', category: '',
        dynasty: '', description: '', imageUrl: '', status: 0,
        storageLocation: '', currentLocation: '', ownerMuseum: userBelong.value
    })
    formVisible.value = true
}

const openEditDialog = (item) => {
    isEdit.value = true
    Object.assign(form, { ...item })
    formVisible.value = true
}

const handleSubmit = async () => {
    await formRef.value.validate(async (valid) => {
        if (!valid) return
        try {
            const res = await axios.post('/api/artifact/save', form)
            const data = res.data
            if (data.code === 200 || data === '保存成功') {
                ElMessage.success(isEdit.value ? '修改成功' : '录入成功')
                formVisible.value = false
                fetchList()
            } else {
                ElMessage.error(data.message || '操作失败')
            }
        } catch (e) {
            ElMessage.error('请求失败，请检查网络')
        }
    })
}

// 删除
const handleDelete = async (id) => {
    await ElMessageBox.confirm('确定要删除这件文物吗？此操作不可恢复。', '删除确认', {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
    })
    try {
        const res = await axios.delete(`/api/artifact/${id}`)
        const data = res.data
        if (data.code === 200 || data === '删除成功') {
            ElMessage.success('已删除')
            fetchList()
        } else {
            ElMessage.error(data.message || '删除失败')
        }
    } catch (e) {
        if (e !== 'cancel') ElMessage.error('删除失败')
    }
}

// 详情
const viewVisible = ref(false)
const viewItem = ref(null)
const openViewDialog = (item) => {
    viewItem.value = item
    viewVisible.value = true
}

const beforeUpload = (file) => {
    const isImage = file.type.startsWith('image/')
    const isLt5M = file.size / 1024 / 1024 < 5
    if (!isImage) ElMessage.error('只能上传图片文件')
    if (!isLt5M) ElMessage.error('图片大小不能超过 5MB')
    return isImage && isLt5M
}

const handleUploadSuccess = (res) => {
    // res 是后端返回的 Result 对象
    if (res.code === 200) {
        form.imageUrl = res.data
        ElMessage.success('上传成功')
    } else {
        ElMessage.error(res.message || '上传失败')
    }
}

const handleUploadError = () => {
    ElMessage.error('上传失败，请检查网络或文件格式')
}

</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+SC:wght@400;600;700&display=swap');

.artifact-manager {
    min-height: 100vh;
    background: #f7f3ee;
    padding: 32px 36px;
    font-family: 'Noto Serif SC', serif;
}

/* 顶部 */
.page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 28px;
}

.header-left {
    display: flex;
    align-items: center;
    gap: 16px;
}

.header-icon {
    font-size: 40px;
    line-height: 1;
}

.page-title {
    font-size: 26px;
    font-weight: 700;
    color: #2c1a0e;
    margin: 0;
    letter-spacing: 2px;
}

.page-subtitle {
    font-size: 13px;
    color: #9a7c5a;
    margin: 4px 0 0;
}

.add-btn {
    background: #7a3b1e !important;
    color: #fff !important;
    border: none !important;
    border-radius: 8px !important;
    padding: 10px 22px !important;
    font-size: 14px !important;
    font-family: 'Noto Serif SC', serif !important;
    letter-spacing: 1px;
    box-shadow: 0 4px 12px rgba(122, 59, 30, 0.25);
    transition: all 0.2s;
}

.add-btn:hover {
    background: #9a4d27 !important;
    transform: translateY(-1px);
}

.btn-icon {
    margin-right: 4px;
    font-size: 16px;
}

/* 筛选栏 */
.filter-bar {
    background: #fff;
    border-radius: 12px;
    padding: 16px 20px;
    margin-bottom: 24px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.category-tabs {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.cat-tab {
    padding: 5px 14px;
    border-radius: 20px;
    font-size: 13px;
    cursor: pointer;
    background: #f4ede4;
    color: #7a5c3a;
    border: 1.5px solid transparent;
    transition: all 0.18s;
    user-select: none;
}

.cat-tab:hover {
    background: #e8d9c8;
}

.cat-tab.active {
    background: #7a3b1e;
    color: #fff;
    border-color: #7a3b1e;
}

.search-input :deep(.el-input__wrapper) {
    border-radius: 8px;
    background: #faf7f3;
    box-shadow: 0 0 0 1px #e0d0be;
}

.search-input :deep(.el-input__wrapper.is-focus) {
    box-shadow: 0 0 0 1.5px #7a3b1e !important;
}

/* 文物卡片网格 */
.artifact-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
    gap: 22px;
}

.artifact-card {
    background: #fff;
    border-radius: 14px;
    overflow: hidden;
    box-shadow: 0 3px 10px rgba(0, 0, 0, 0.07);
    transition: transform 0.2s, box-shadow 0.2s;
    display: flex;
    flex-direction: column;
}

.artifact-card:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.card-img-wrap {
    position: relative;
    height: 180px;
    background: #f0e8dc;
    overflow: hidden;
}

.card-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.3s;
}

.artifact-card:hover .card-img {
    transform: scale(1.04);
}

.status-pill {
    position: absolute;
    top: 10px;
    right: 10px;
    padding: 3px 10px;
    border-radius: 12px;
    font-size: 11px;
    font-weight: 600;
}

.status-in {
    background: #e8f5e9;
    color: #388e3c;
}

.status-out {
    background: #e3f2fd;
    color: #1565c0;
}

.status-repair {
    background: #fff8e1;
    color: #f57f17;
}

.category-pill {
    position: absolute;
    bottom: 10px;
    left: 10px;
    background: rgba(44, 26, 14, 0.7);
    color: #f5e6d0;
    padding: 2px 10px;
    border-radius: 10px;
    font-size: 11px;
    backdrop-filter: blur(4px);
}

.card-body {
    padding: 14px 16px 10px;
    flex: 1;
}

.card-name {
    font-size: 16px;
    font-weight: 700;
    color: #2c1a0e;
    margin-bottom: 6px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.card-meta {
    font-size: 12px;
    color: #9a7c5a;
    margin-bottom: 6px;
}

.dot {
    margin: 0 6px;
}

.card-location {
    font-size: 12px;
    color: #b08060;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.loc-icon {
    margin-right: 4px;
}

.card-actions {
    padding: 10px 16px;
    border-top: 1px solid #f0e8dc;
    display: flex;
    align-items: center;
    gap: 4px;
}

.action-btn {
    font-size: 13px !important;
}

.edit-btn {
    color: #7a3b1e !important;
}

.view-btn {
    color: #5c7a3e !important;
}

.del-btn {
    color: #c0392b !important;
}

/* 空状态 */
.empty-state {
    grid-column: 1 / -1;
    text-align: center;
    padding: 80px 0;
    color: #9a7c5a;
}

.empty-icon {
    font-size: 56px;
    margin-bottom: 16px;
}

/* 弹窗 */
:deep(.museum-dialog .el-dialog__header) {
    background: #2c1a0e;
    padding: 18px 24px;
    border-radius: 12px 12px 0 0;
}

:deep(.museum-dialog .el-dialog__title) {
    color: #f5e6d0;
    font-family: 'Noto Serif SC', serif;
    font-size: 17px;
    letter-spacing: 2px;
}

:deep(.museum-dialog .el-dialog__headerbtn .el-dialog__close) {
    color: #f5e6d0;
}

:deep(.museum-dialog .el-dialog) {
    border-radius: 12px;
    overflow: hidden;
}

.artifact-form :deep(.el-input__wrapper),
.artifact-form :deep(.el-textarea__inner),
.artifact-form :deep(.el-select .el-input__wrapper) {
    background: #faf7f3;
    box-shadow: 0 0 0 1px #e0d0be;
    border-radius: 6px;
}

.confirm-btn {
    background: #7a3b1e !important;
    color: #fff !important;
    border: none !important;
    border-radius: 6px !important;
}

/* 详情弹窗 */
.detail-wrap {
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.detail-img-wrap {
    position: relative;
    border-radius: 10px;
    overflow: hidden;
}

.detail-img {
    width: 100%;
    height: 240px;
    object-fit: cover;
    background: #f0e8dc;
}

.detail-name {
    font-size: 22px;
    font-weight: 700;
    color: #2c1a0e;
    margin: 0 0 14px;
    letter-spacing: 1px;
}

.detail-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 10px;
    margin-bottom: 16px;
}

.detail-item {
    display: flex;
    flex-direction: column;
    gap: 3px;
    font-size: 13px;
    color: #4a3828;
}

.detail-label {
    font-size: 11px;
    color: #9a7c5a;
    text-transform: uppercase;
    letter-spacing: 1px;
}

.detail-desc {
    background: #faf7f3;
    border-radius: 8px;
    padding: 14px;
    font-size: 13px;
    color: #4a3828;
    line-height: 1.8;
    border-left: 3px solid #7a3b1e;
}

.upload-wrap {
    width: 100%;
}

.img-uploader :deep(.el-upload) {
    width: 100%;
}

.upload-trigger {
    border: 2px dashed #e0d0be;
    border-radius: 8px;
    padding: 24px;
    text-align: center;
    cursor: pointer;
    transition: border-color 0.2s;
    background: #faf7f3;
}

.upload-trigger:hover {
    border-color: #7a3b1e;
}

.upload-icon {
    font-size: 32px;
    margin-bottom: 8px;
}

.upload-text {
    font-size: 14px;
    color: #4a3828;
}

.upload-hint {
    font-size: 12px;
    color: #9a7c5a;
    margin-top: 4px;
}

.img-preview {
    position: relative;
    display: inline-block;
    border-radius: 8px;
    overflow: hidden;
}

.preview-img {
    width: 100%;
    max-height: 180px;
    object-fit: cover;
    border-radius: 8px;
    display: block;
}

.remove-img {
    position: absolute;
    top: 6px;
    right: 6px;
    background: rgba(0, 0, 0, 0.5);
    color: #fff;
    border-radius: 50%;
    width: 22px;
    height: 22px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    font-size: 12px;
}

.remove-img:hover {
    background: rgba(192, 57, 43, 0.8);
}

</style>