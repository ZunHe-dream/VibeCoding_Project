<template>
    <div class="artifact-root">
        <!-- 顶部 -->
        <div class="page-header">
            <div class="header-left">
                <div class="header-tag">SUPER ADMIN · 馆藏资源池</div>
                <h1 class="header-title">全站文物总览</h1>
                <div class="header-stats">
                    <div class="stat-card" v-for="s in stats" :key="s.label">
                        <div class="stat-num" :style="{ color: s.color }">{{ s.num }}</div>
                        <div class="stat-label">{{ s.label }}</div>
                    </div>
                </div>
            </div>
            <el-button class="btn-add" @click="openDialog(null)">＋ 新增文物</el-button>
        </div>

        <!-- 搜索控制栏 -->
        <div class="control-bar">
            <el-input v-model="keyword" placeholder="搜索文物名称、作者、朝代、存放位置…" clearable @change="fetchList"
                style="width:300px">
                <template #prefix><span>🔍</span></template>
            </el-input>
            <el-select v-model="filterCategory" placeholder="全部类别" clearable @change="fetchList" style="width:140px">
                <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
            </el-select>
            <el-input v-model="filterMuseum" placeholder="所属博物馆" clearable @change="fetchList" style="width:180px" />
            <span class="result-count">共 {{ list.length }} 件</span>
        </div>

        <!-- 数据表格 -->
        <div class="table-wrap" v-loading="loading">
            <el-table :data="list" border size="small" row-key="artifactId"
                :header-cell-style="{ background: '#faf8f5', color: '#5c3d1e', fontWeight: '600' }">

                <el-table-column label="文物" width="260">
                    <template #default="{ row }">
                        <div class="artifact-cell">
                            <el-image :src="row.imageUrl" fit="cover" class="artifact-thumb"
                                :preview-src-list="[row.imageUrl]" preview-teleported />
                            <div class="artifact-info">
                                <div class="artifact-name">{{ row.artifactName }}</div>
                                <div class="artifact-sub">{{ row.dynasty || '—' }} · {{ row.author || '佚名' }}</div>
                            </div>
                        </div>
                    </template>
                </el-table-column>

                <el-table-column prop="category" label="类别" width="90" align="center">
                    <template #default="{ row }">
                        <span class="cat-tag">{{ row.category || '—' }}</span>
                    </template>
                </el-table-column>

                <el-table-column prop="ownerMuseum" label="所属博物馆" width="160" />

                <el-table-column prop="storageLocation" label="存放位置" width="130" />

                <el-table-column label="状态" width="100" align="center">
                    <template #default="{ row }">
                        <span class="status-badge" :class="statusClass(row.status)">
                            {{ statusText(row.status) }}
                        </span>
                    </template>
                </el-table-column>

                <el-table-column label="操作" width="140" align="center" fixed="right">
                    <template #default="{ row }">
                        <el-button link type="primary" size="small" @click="openDialog(row)">编辑</el-button>
                        <el-divider direction="vertical" />
                        <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </div>

        <!-- 新增/编辑弹窗 -->
        <el-dialog v-model="dialogVisible" :title="form.artifactId ? '编辑文物信息' : '新增文物'" width="720px">

            <!-- 出借警告 -->
            <el-alert v-if="form.artifactId && form.status === 1" title="⚠️ 该文物当前处于「巡展中」状态，修改信息请谨慎操作，建议联系借展方确认后再修改。"
                type="warning" show-icon :closable="false" style="margin-bottom:16px" />

            <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
                <el-row :gutter="20">
                    <el-col :span="14">
                        <el-form-item label="文物名称" prop="artifactName">
                            <el-input v-model="form.artifactName" placeholder="请输入文物名称" />
                        </el-form-item>
                        <el-row :gutter="12">
                            <el-col :span="12">
                                <el-form-item label="类别" prop="category">
                                    <el-select v-model="form.category" placeholder="选择类别" style="width:100%">
                                        <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
                                    </el-select>
                                </el-form-item>
                            </el-col>
                            <el-col :span="12">
                                <el-form-item label="朝代">
                                    <el-input v-model="form.dynasty" placeholder="如：唐代" />
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-row :gutter="12">
                            <el-col :span="12">
                                <el-form-item label="作者">
                                    <el-input v-model="form.author" placeholder="佚名" />
                                </el-form-item>
                            </el-col>
                            <el-col :span="12">
                                <el-form-item label="材质">
                                    <el-input v-model="form.material" placeholder="如：青铜" />
                                </el-form-item>
                            </el-col>
                        </el-row>
                        <el-form-item label="所属博物馆" prop="ownerMuseum">
                            <el-input v-model="form.ownerMuseum" placeholder="请输入所属博物馆" />
                        </el-form-item>
                        <el-form-item label="存放位置">
                            <el-input v-model="form.storageLocation" placeholder="展厅/库房位置" />
                        </el-form-item>
                        <el-form-item label="当前状态">
                            <el-select v-model="form.status" style="width:100%" :disabled="form.status === 1">
                                <el-option :value="0" label="馆藏中" />
                                <el-option :value="1" label="外借巡展中" :disabled="true" />
                                <el-option :value="2" label="维修保养中" />
                            </el-select>
                            <div v-if="form.status === 1" style="font-size:11px;color:#e6a23c;margin-top:4px">
                                出借状态由系统自动管理，不可手动修改
                            </div>
                        </el-form-item>
                    </el-col>
                    <el-col :span="10">
                        <el-form-item label="文物图片">
                            <el-upload action="/api/upload/image" name="file" :show-file-list="false"
                                :on-success="handleUploadSuccess" :before-upload="beforeUpload" class="img-uploader">
                                <div class="img-upload-area">
                                    <img v-if="form.imageUrl" :src="form.imageUrl" class="upload-preview" />
                                    <div v-else class="upload-placeholder">
                                        <div style="font-size:28px">🖼</div>
                                        <div style="font-size:12px;color:#9a7c5a;margin-top:6px">点击上传图片</div>
                                    </div>
                                </div>
                            </el-upload>
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-form-item label="文物描述">
                    <el-input v-model="form.description" type="textarea" :rows="3" placeholder="文物的历史背景、艺术价值、出土信息等…" />
                </el-form-item>
            </el-form>

            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleSave">
                    {{ form.status === 1 ? '确认修改（出借中）' : '保存' }}
                </el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const list = ref([])
const loading = ref(false)
const keyword = ref('')
const filterCategory = ref('')
const filterMuseum = ref('')

const categories = ['绘画', '法书', '青铜器', '瓷器', '玉石器', '金银器', '织绣', '雕塑', '文献', '其他']

const stats = computed(() => [
    { label: '文物总数', num: list.value.length, color: '#2c1a0e' },
    { label: '馆藏中', num: list.value.filter(a => a.status === 0).length, color: '#2d7a3a' },
    { label: '巡展中', num: list.value.filter(a => a.status === 1).length, color: '#8b1a1a' },
    { label: '维修中', num: list.value.filter(a => a.status === 2).length, color: '#9a6a00' },
    { label: '馆藏机构', num: [...new Set(list.value.map(a => a.ownerMuseum).filter(Boolean))].length, color: '#4a6fa5' },
])

const statusText = (s) => ['馆藏中', '巡展中', '维修中'][s] ?? '—'
const statusClass = (s) => ['status-in', 'status-out', 'status-repair'][s] ?? ''

const fetchList = async () => {
    loading.value = true
    try {
        const res = await axios.get('/api/artifact/super/list', {
            params: { keyword: keyword.value, category: filterCategory.value }
        })
        let data = res.data?.data || res.data || []
        if (filterMuseum.value) {
            data = data.filter(a => a.ownerMuseum?.includes(filterMuseum.value))
        }
        list.value = data
    } finally {
        loading.value = false
    }
}

// 弹窗
const dialogVisible = ref(false)
const formRef = ref()
const form = ref({})

const rules = {
    artifactName: [{ required: true, message: '请输入文物名称', trigger: 'blur' }],
    ownerMuseum: [{ required: true, message: '请输入所属博物馆', trigger: 'blur' }],
    category: [{ required: true, message: '请选择类别', trigger: 'change' }],
}

const openDialog = (row) => {
    form.value = row ? { ...row } : { status: 0 }
    dialogVisible.value = true
    formRef.value?.clearValidate()
}

const handleSave = async () => {
    await formRef.value.validate(async (valid) => {
        if (!valid) return

        // 出借中文物需二次确认
        if (form.value.status === 1) {
            await ElMessageBox.confirm(
                '该文物当前处于「巡展中」状态，确定要修改其信息吗？',
                '⚠️ 出借中文物修改确认',
                { confirmButtonText: '确认修改', cancelButtonText: '取消', type: 'warning' }
            )
        }

        const res = await axios.post('/api/artifact/save', form.value)
        if (res.data.code === 200) {
            ElMessage.success('保存成功')
            dialogVisible.value = false
            fetchList()
        } else {
            ElMessage.error(res.data.message || '保存失败')
        }
    })
}

const handleDelete = async (row) => {
    if (row.status === 1) {
        return ElMessage.error('该文物正处于巡展中，无法删除！请等待归还后操作。')
    }
    await ElMessageBox.confirm(
        `确定删除文物「${row.artifactName}」？此操作不可恢复。`,
        '删除确认',
        { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await axios.delete(`/api/artifact/${row.artifactId}`)
    if (res.data.code === 200) {
        ElMessage.success('已删除')
        fetchList()
    }
}

const handleUploadSuccess = (res) => {
    if (res.code === 200) {
        form.value.imageUrl = res.data
        ElMessage.success('图片上传成功')
    } else {
        ElMessage.error('图片上传失败')
    }
}

const beforeUpload = (file) => {
    const isImg = file.type.startsWith('image/')
    const lt5M = file.size / 1024 / 1024 < 5
    if (!isImg) { ElMessage.error('只能上传图片'); return false }
    if (!lt5M) { ElMessage.error('图片不超过5MB'); return false }
    return true
}

onMounted(fetchList)
</script>

<style scoped>
.artifact-root {
    min-height: 100vh;
    background: #faf8f5;
    font-family: 'Noto Serif SC', 'Noto Sans SC', serif;
    padding: 32px 40px;
}

/* 顶部 */
.page-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 24px;
    padding-bottom: 24px;
    border-bottom: 1px solid #e8dfd0;
}

.header-tag {
    font-size: 11px;
    letter-spacing: 3px;
    color: #8b1a1a;
    font-weight: 600;
    margin-bottom: 8px;
}

.header-title {
    font-size: 26px;
    font-weight: 700;
    color: #1a0a0a;
    margin: 0 0 20px;
}

.header-stats {
    display: flex;
    gap: 14px;
}

.stat-card {
    background: #fff;
    border-radius: 10px;
    padding: 10px 18px;
    border: 1px solid #e8dfd0;
    text-align: center;
    min-width: 72px;
}

.stat-num {
    font-size: 22px;
    font-weight: 700;
}

.stat-label {
    font-size: 11px;
    color: #9a7c5a;
    margin-top: 2px;
}

.btn-add {
    background: #8b1a1a !important;
    border: none !important;
    color: white !important;
    border-radius: 8px !important;
    font-weight: 600 !important;
    padding: 12px 24px !important;
    height: auto !important;
}

.btn-add:hover {
    background: #a52020 !important;
}

/* 控制栏 */
.control-bar {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
    flex-wrap: wrap;
}

.result-count {
    font-size: 13px;
    color: #9a7c5a;
    margin-left: auto;
}

/* 表格 */
.table-wrap {
    background: #fff;
    border-radius: 10px;
    overflow: hidden;
    border: 1px solid #e8dfd0;
}

.artifact-cell {
    display: flex;
    align-items: center;
    gap: 12px;
}

.artifact-thumb {
    width: 52px;
    height: 52px;
    border-radius: 8px;
    object-fit: cover;
    flex-shrink: 0;
    border: 1px solid #e8dfd0;
    cursor: zoom-in;
}

.artifact-name {
    font-size: 14px;
    font-weight: 600;
    color: #2c1a0e;
}

.artifact-sub {
    font-size: 12px;
    color: #9a7c5a;
    margin-top: 3px;
}

.cat-tag {
    font-size: 12px;
    background: #f5efe6;
    color: #8b5e3c;
    padding: 2px 8px;
    border-radius: 4px;
}

.status-badge {
    font-size: 12px;
    font-weight: 600;
    padding: 3px 10px;
    border-radius: 6px;
}

.status-in {
    background: #f0faf2;
    color: #2d7a3a;
}

.status-out {
    background: #fff0f0;
    color: #8b1a1a;
}

.status-repair {
    background: #fff8e6;
    color: #9a6a00;
}

/* 上传 */
.img-uploader {
    width: 100%;
}

.img-upload-area {
    width: 100%;
    height: 200px;
    border: 2px dashed #d4c4a8;
    border-radius: 10px;
    overflow: hidden;
    cursor: pointer;
    background: #faf7f2;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: border-color 0.2s;
}

.img-upload-area:hover {
    border-color: #8b1a1a;
}

.upload-preview {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.upload-placeholder {
    text-align: center;
}
</style>