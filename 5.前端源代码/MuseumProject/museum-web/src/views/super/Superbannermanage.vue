<template>
    <div class="banner-root">
        <!-- 顶部 -->
        <div class="page-header">
            <div class="header-left">
                <div class="header-tag">SUPER ADMIN · 门户配置</div>
                <h1 class="header-title">轮播图管理</h1>
            </div>
            <el-button class="btn-add" @click="openDialog(null)">＋ 新增轮播图</el-button>
        </div>

        <!-- 尺寸提示 -->
        <el-alert title="📐 图片规格建议：推荐上传尺寸 1200 × 400 px，比例 3:1，格式 JPG/PNG，大小不超过 2MB。上传其他比例的图片将被自动裁剪铺满，可能导致显示变形。"
            type="info" show-icon :closable="false" class="size-tip" />

        <!-- 预览区 -->
        <div class="preview-section">
            <div class="preview-label">📺 当前轮播预览（仅显示启用中）</div>
            <el-carousel v-if="activeBanners.length > 0" height="220px" :interval="3000" arrow="always">
                <el-carousel-item v-for="b in activeBanners" :key="b.id">
                    <div class="preview-item">
                        <img :src="b.imageUrl" class="preview-img" />
                        <div class="preview-overlay">
                            <div class="preview-title">{{ b.title || '（无标题）' }}</div>
                        </div>
                    </div>
                </el-carousel-item>
            </el-carousel>
            <div v-else class="preview-empty">暂无启用中的轮播图</div>
        </div>

        <!-- 列表 -->
        <div class="table-wrap" v-loading="loading">
            <el-table :data="list" border size="small"
                :header-cell-style="{ background: '#faf8f5', color: '#5c3d1e', fontWeight: '600' }">

                <el-table-column label="排序" width="70" align="center">
                    <template #default="{ row }">
                        <div class="sort-btns">
                            <el-button link size="small" @click="moveUp(row)"
                                :disabled="list.indexOf(row) === 0">↑</el-button>
                            <el-button link size="small" @click="moveDown(row)"
                                :disabled="list.indexOf(row) === list.length - 1">↓</el-button>
                        </div>
                    </template>
                </el-table-column>

                <el-table-column label="预览" width="160">
                    <template #default="{ row }">
                        <img :src="row.imageUrl" class="thumb" />
                    </template>
                </el-table-column>

                <el-table-column prop="title" label="标题文字" />

                <el-table-column label="状态" width="90" align="center">
                    <template #default="{ row }">
                        <el-switch v-model="row.status" :active-value="1" :inactive-value="0"
                            @change="toggleStatus(row)" />
                    </template>
                </el-table-column>

                <el-table-column prop="createTime" label="创建时间" width="160" align="center">
                    <template #default="{ row }">
                        {{ row.createTime?.slice(0, 16) }}
                    </template>
                </el-table-column>

                <el-table-column label="操作" width="120" align="center">
                    <template #default="{ row }">
                        <el-button link type="primary" size="small" @click="openDialog(row)">编辑</el-button>
                        <el-divider direction="vertical" />
                        <el-button link type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </div>

        <!-- 新增/编辑弹窗 -->
        <el-dialog v-model="dialogVisible" :title="form.id ? '编辑轮播图' : '新增轮播图'" width="560px">
            <el-form :model="form" label-width="90px">

                <el-form-item label="轮播图片" required>
                    <el-upload action="/api/upload/image" name="file" :show-file-list="false"
                        :on-success="handleUploadSuccess" :before-upload="beforeUpload" class="img-uploader">
                        <div class="upload-area">
                            <img v-if="form.imageUrl" :src="form.imageUrl" class="upload-preview" />
                            <div v-else class="upload-placeholder">
                                <div class="upload-icon">🖼</div>
                                <div class="upload-text">点击上传</div>
                                <div class="upload-hint">推荐 1200×400px · 比例3:1 · ≤2MB</div>
                            </div>
                        </div>
                    </el-upload>

                    <!-- 尺寸警告 -->
                    <div v-if="imgWarning" class="img-warning">
                        ⚠️ {{ imgWarning }}
                    </div>
                </el-form-item>
                <el-form-item label="大标题">
                    <el-input v-model="form.mainTitle" placeholder="显示在图片上的大标题，默认：基于Vue3的文物巡展管理系统" />
                </el-form-item>

                <el-form-item label="小标题">
                    <el-input v-model="form.title" placeholder="显示在大标题下方的副标题" />
                </el-form-item>

                <el-form-item label="点击跳转">
                    <el-input v-model="form.linkUrl" placeholder="点击轮播图跳转的路径，如 /user/exhibition，不填则不跳转" />
                    <div style="font-size:12px; color:#9a7c5a; margin-top:4px">
                        常用路径：/user/exhibition（巡展列表）· /user/relic（文物分类）· /user/shop（周边商城）
                    </div>
                </el-form-item>
                <el-form-item label="排序">
                    <el-input-number v-model="form.sort" :min="0" style="width:120px" />
                    <span style="font-size:12px; color:#9a7c5a; margin-left:10px">数字越小越靠前</span>
                </el-form-item>

                <el-form-item label="状态">
                    <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="显示"
                        inactive-text="隐藏" />
                </el-form-item>
            </el-form>

            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleSave"
                    style="background:#8b1a1a; border-color:#8b1a1a">保存</el-button>
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
const dialogVisible = ref(false)
const form = ref({})
const imgWarning = ref('')

const activeBanners = computed(() => list.value.filter(b => b.status === 1))

const fetchList = async () => {
    loading.value = true
    try {
        const res = await axios.get('/api/banner/admin/list')
        list.value = res.data?.data || res.data || []
    } finally {
        loading.value = false
    }
}

const openDialog = (row) => {
    form.value = row
        ? { ...row }
        : { title: '', mainTitle: '', imageUrl: '', sort: 0, status: 1, linkUrl: '' }
    imgWarning.value = ''
    dialogVisible.value = true
}

const beforeUpload = (file) => {
    const isImg = file.type.startsWith('image/')
    const lt2M = file.size / 1024 / 1024 < 2
    if (!isImg) { ElMessage.error('只能上传图片文件'); return false }
    if (!lt2M) { ElMessage.error('图片大小不能超过 2MB'); return false }
    // 检查尺寸比例
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => {
        const ratio = img.width / img.height
        if (Math.abs(ratio - 3) > 0.5) {
            imgWarning.value = `当前图片比例约 ${img.width}×${img.height}（${ratio.toFixed(1)}:1），建议使用 3:1 比例（如 1200×400），否则显示时可能裁剪变形`
        } else {
            imgWarning.value = ''
        }
        URL.revokeObjectURL(url)
    }
    img.src = url
    return true
}

const handleUploadSuccess = (res) => {
    if (res.code === 200) {
        form.value.imageUrl = res.data
        ElMessage.success('图片上传成功')
    } else {
        ElMessage.error('上传失败')
    }
}

const handleSave = async () => {
    if (!form.value.imageUrl) return ElMessage.warning('请先上传图片')
    const res = await axios.post('/api/banner/save', form.value)
    if (res.data.code === 200) {
        ElMessage.success('保存成功')
        dialogVisible.value = false
        fetchList()
    }
}

const handleDelete = async (id) => {
    await ElMessageBox.confirm('确定删除该轮播图？', '提示', { type: 'warning' })
    const res = await axios.delete(`/api/banner/${id}`)
    if (res.data.code === 200) { ElMessage.success('已删除'); fetchList() }
}

const toggleStatus = async (row) => {
    await axios.post('/api/banner/save', row)
}

// 排序上移/下移
const moveUp = async (row) => {
    const idx = list.value.indexOf(row)
    if (idx === 0) return
    const prev = list.value[idx - 1]
        ;[row.sort, prev.sort] = [prev.sort, row.sort]
    await axios.post('/api/banner/sort', [row, prev])
    fetchList()
}

const moveDown = async (row) => {
    const idx = list.value.indexOf(row)
    if (idx === list.value.length - 1) return
    const next = list.value[idx + 1]
        ;[row.sort, next.sort] = [next.sort, row.sort]
    await axios.post('/api/banner/sort', [row, next])
    fetchList()
}

onMounted(fetchList)
</script>

<style scoped>
.banner-root {
    min-height: 100vh;
    background: #faf8f5;
    font-family: 'Noto Serif SC', 'Noto Sans SC', serif;
    padding: 32px 40px;
}

.page-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 20px;
    padding-bottom: 20px;
    border-bottom: 1px solid #e8dfd0;
}

.header-tag {
    font-size: 11px;
    letter-spacing: 3px;
    color: #8b1a1a;
    font-weight: 600;
    margin-bottom: 6px;
}

.header-title {
    font-size: 24px;
    font-weight: 700;
    color: #1a0a0a;
    margin: 0;
}

.btn-add {
    background: #8b1a1a !important;
    border: none !important;
    color: white !important;
    border-radius: 8px !important;
    font-weight: 600 !important;
}

.size-tip {
    margin-bottom: 20px;
    border-radius: 8px;
}

/* 预览区 */
.preview-section {
    background: #fff;
    border-radius: 12px;
    border: 1px solid #e8dfd0;
    padding: 16px;
    margin-bottom: 20px;
}

.preview-label {
    font-size: 13px;
    font-weight: 600;
    color: #5c3d1e;
    margin-bottom: 12px;
}

.preview-item {
    position: relative;
    width: 100%;
    height: 100%;
}

.preview-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    border-radius: 8px;
    filter: brightness(0.75);
}

.preview-overlay {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
}

.preview-title {
    color: white;
    font-size: 18px;
    font-weight: 700;
    text-shadow: 0 2px 8px rgba(0, 0, 0, 0.5);
    letter-spacing: 2px;
}

.preview-empty {
    text-align: center;
    color: #9a7c5a;
    padding: 40px;
    font-size: 14px;
}

/* 表格 */
.table-wrap {
    background: #fff;
    border-radius: 10px;
    overflow: hidden;
    border: 1px solid #e8dfd0;
}

.thumb {
    width: 140px;
    height: 46px;
    object-fit: cover;
    border-radius: 4px;
    border: 1px solid #e8dfd0;
}

.sort-btns {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0;
}

/* 上传区 */
.img-uploader {
    width: 100%;
}

.upload-area {
    width: 100%;
    height: 150px;
    border: 2px dashed #d4c4a8;
    border-radius: 8px;
    overflow: hidden;
    cursor: pointer;
    background: #faf7f2;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: border-color 0.2s;
}

.upload-area:hover {
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

.upload-icon {
    font-size: 28px;
    margin-bottom: 6px;
}

.upload-text {
    font-size: 14px;
    color: #8b6940;
    font-weight: 600;
}

.upload-hint {
    font-size: 12px;
    color: #b8a080;
    margin-top: 4px;
}

.img-warning {
    margin-top: 8px;
    font-size: 12px;
    color: #e6a23c;
    background: #fdf6ec;
    border: 1px solid #faecd8;
    border-radius: 6px;
    padding: 6px 10px;
    line-height: 1.6;
}
</style>