<template>
    <div class="command-center">
        <!-- 顶部指挥台 -->
        <div class="header-deck">
            <div class="header-left">
                <div class="title-stack">
                    <span class="title-eyebrow">SUPERADMIN · GLOBAL CONTROL</span>
                    <h1 class="title-main">公告全量调度</h1>
                </div>
                <div class="header-stats">
                    <div class="stat-pill" v-for="s in headerStats" :key="s.label">
                        <span class="stat-n">{{ s.value }}</span>
                        <span class="stat-l">{{ s.label }}</span>
                    </div>
                </div>
            </div>
            <div class="header-actions">
                <el-button class="btn-new" @click="openDialog(null)">
                    <span class="btn-plus">＋</span> 发布新公告
                </el-button>
            </div>
        </div>

        <!-- 控制栏 -->
        <div class="control-bar">
            <!-- 左侧：类型标签管理 -->
            <div class="type-tags">
                <span class="type-tag" :class="{ active: filterType === '' }"
                    @click="filterType = ''; fetchNotices()">全部</span>
                <span class="type-tag" v-for="t in noticeTypes" :key="t" :class="{ active: filterType === t }"
                    @click="filterType = t; fetchNotices()">{{ t }}</span>
                <!-- 添加类型 -->
                <el-popover placement="bottom" :width="220" trigger="click" v-model:visible="typePopoverVisible">
                    <template #reference>
                        <span class="type-tag add-tag">⚙ 管理类型</span>
                    </template>
                    <div class="type-manager">
                        <div class="type-list-manage">
                            <div v-for="(t, i) in noticeTypes" :key="t" class="type-manage-item">
                                <span>{{ t }}</span>
                                <el-button link type="danger" size="small" @click="removeType(i)">删除</el-button>
                            </div>
                            <div v-if="noticeTypes.length === 0" style="color:#aaa;font-size:12px;padding:8px">暂无自定义类型
                            </div>
                        </div>
                        <div class="type-add-row">
                            <el-input v-model="newTypeName" placeholder="新增类型名" size="small" style="flex:1" />
                            <el-button type="primary" size="small" @click="addType">添加</el-button>
                        </div>
                    </div>
                </el-popover>
            </div>

            <!-- 右侧：搜索 -->
            <div class="search-group">
                <el-input v-model="keyword" placeholder="搜索公告标题…" clearable @change="fetchNotices" style="width:200px">
                    <template #prefix><span style="font-size:13px">🔍</span></template>
                </el-input>
                <el-input v-model="authorBelong" placeholder="搜索机构…" clearable @change="fetchNotices"
                    style="width:160px" />
            </div>
        </div>

        <!-- 公告列表 -->
        <div class="notice-grid" v-loading="loading">
            <div v-if="notices.length === 0 && !loading" class="empty-state">
                <div class="empty-icon">📭</div>
                <div>暂无公告数据</div>
            </div>

            <div v-for="(n, idx) in notices" :key="n.noticeId" class="notice-row" :class="{ 'is-top': n.isTop === 1 }"
                :style="{ animationDelay: idx * 0.04 + 's' }">

                <!-- 置顶勋章 -->
                <div class="top-badge" v-if="n.isTop === 1">📌 置顶</div>

                <!-- 类型标记 -->
                <div class="row-type" :class="typeClass(n.noticeType)">{{ n.noticeType || '公告' }}</div>

                <!-- 主体 -->
                <div class="row-body" @click="openDetail(n)">
                    <div class="row-title">{{ n.title }}</div>
                    <div class="row-meta">
                        <span class="meta-author">{{ n.authorName || '—' }}</span>
                        <span class="meta-dot">·</span>
                        <span class="meta-belong">{{ n.authorBelong || '全站' }}</span>
                        <span class="meta-dot">·</span>
                        <span class="meta-time">{{ formatTime(n.createTime) }}</span>
                    </div>
                </div>

                <!-- 操作区 -->
                <div class="row-actions">
                    <el-tooltip :content="n.isTop === 1 ? '取消置顶' : '置顶'" placement="top">
                        <button class="action-btn top-btn" :class="{ topped: n.isTop === 1 }" @click="toggleTop(n)">
                            {{ n.isTop === 1 ? '📌' : '📍' }}
                        </button>
                    </el-tooltip>
                    <button class="action-btn edit-btn" @click="openDialog(n)">✏️</button>
                    <button class="action-btn del-btn" @click="deleteNotice(n.noticeId)">🗑</button>
                </div>
            </div>
        </div>

        <!-- 公告详情弹窗 -->
        <el-dialog v-model="detailVisible" title="公告详情" width="820px" class="notice-dialog">
            <div v-if="detailNotice" class="detail-body">
                <div class="detail-type-row">
                    <span class="detail-type-tag" :class="typeClass(detailNotice.noticeType)">
                        {{ detailNotice.noticeType }}
                    </span>
                    <span v-if="detailNotice.isTop === 1" class="detail-top-tag">📌 已置顶</span>
                </div>
                <h2 class="detail-title">{{ detailNotice.title }}</h2>
                <div class="detail-meta">
                    <span>发布人：{{ detailNotice.authorName }}</span>
                    <span>·</span>
                    <span>{{ detailNotice.authorBelong || '全站' }}</span>
                    <span>·</span>
                    <span>{{ formatTime(detailNotice.createTime) }}</span>
                </div>
                <div class="detail-content ql-content" v-html="detailNotice.content"></div>
            </div>
        </el-dialog>

        <!-- 发布/编辑弹窗 -->
        <el-dialog v-model="formVisible" :title="editForm.noticeId ? '编辑公告' : '发布新公告'" width="820px"
            @opened="onDialogOpened" @closed="onDialogClosed">
            <el-form :model="editForm" label-width="80px" class="edit-form">
                <el-form-item label="标题">
                    <el-input v-model="editForm.title" placeholder="公告标题" />
                </el-form-item>
                <el-row :gutter="16">
                    <el-col :span="12">
                        <el-form-item label="类型">
                            <el-select v-model="editForm.noticeType" placeholder="选择类型" style="width:100%">
                                <el-option v-for="t in noticeTypes" :key="t" :label="t" :value="t" />
                                <el-option label="系统公告" value="系统公告" />
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="置顶">
                            <el-switch v-model="editForm.isTop" :active-value="1" :inactive-value="0" />
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-form-item label="内容">
                    <div class="quill-wrap">
                        <div class="quill-toolbar" id="sn-quill-toolbar">
                            <span class="ql-formats">
                                <select class="ql-header">
                                    <option value="1">标题1</option>
                                    <option value="2">标题2</option>
                                    <option value="3">标题3</option>
                                    <option value="">正文</option>
                                </select>
                            </span>
                            <span class="ql-formats">
                                <button class="ql-bold"></button>
                                <button class="ql-italic"></button>
                                <button class="ql-underline"></button>
                                <button class="ql-strike"></button>
                            </span>
                            <span class="ql-formats">
                                <select class="ql-color"></select>
                                <select class="ql-background"></select>
                            </span>
                            <span class="ql-formats">
                                <button class="ql-list" value="ordered"></button>
                                <button class="ql-list" value="bullet"></button>
                            </span>
                            <span class="ql-formats">
                                <button class="ql-align" value=""></button>
                                <button class="ql-align" value="center"></button>
                                <button class="ql-align" value="right"></button>
                            </span>
                            <span class="ql-formats">
                                <button class="ql-link"></button>
                                <button class="ql-blockquote"></button>
                                <button class="ql-clean"></button>
                            </span>
                        </div>
                        <div id="sn-quill-editor" class="quill-editor-body"></div>
                    </div>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="formVisible = false">取消</el-button>
                <el-button type="primary" @click="saveNotice">发布</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const notices = ref([])
const loading = ref(false)
const keyword = ref('')
const filterType = ref('')
const authorBelong = ref('')

// 公告类型管理（持久化到 localStorage）
const noticeTypes = ref([])
const typePopoverVisible = ref(false)
const newTypeName = ref('')

// 初始化时从后端加载
const fetchTypes = async () => {
    const res = await axios.get('/api/notice/types')
    if (res.data.code === 200) noticeTypes.value = res.data.data
}


const saveTypes = () => localStorage.setItem(TYPES_KEY, JSON.stringify(noticeTypes.value))

const addType = async () => {
    const name = newTypeName.value.trim()
    if (!name) return ElMessage.warning('请输入类型名称')
    if (noticeTypes.value.includes(name)) return ElMessage.warning('类型已存在')
    const res = await axios.post('/api/notice/types/add', null, { params: { typeName: name } })
    if (res.data.code === 200) {
        ElMessage.success('类型已添加')
        newTypeName.value = ''
        fetchTypes()
    }
}

const removeType = async (i) => {
    const name = noticeTypes.value[i]
    const res = await axios.delete(`/api/notice/types/${name}`)
    if (res.data.code === 200) {
        ElMessage.success('已删除')
        fetchTypes()
    }
}

// 统计
const headerStats = computed(() => [
    { value: notices.value.length, label: '全站公告' },
    { value: notices.value.filter(n => n.isTop === 1).length, label: '已置顶' },
    { value: [...new Set(notices.value.map(n => n.authorBelong).filter(Boolean))].length, label: '发布机构' },
])

// 类型色系
const typeColorMap = { '紧急通知': 'danger', '展览公告': 'primary', '讲座活动': 'success', '特展信息': 'warning' }
const typeClass = (type) => typeColorMap[type] || 'default'

const formatTime = (t) => {
    if (!t) return '—'
    return new Date(t).toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

const fetchNotices = async () => {
    loading.value = true
    try {
        const res = await axios.get('/api/notice/super/list', {
            params: { keyword: keyword.value, type: filterType.value }
        })
        let list = res.data?.data || res.data || []
        // 前端补充机构过滤（super/list 无 authorBelong 参数时）
        if (authorBelong.value) {
            list = list.filter(n => n.authorBelong?.includes(authorBelong.value))
        }
        notices.value = list
    } finally {
        loading.value = false
    }
}

// 置顶
const toggleTop = async (n) => {
    const res = await axios.post(`/api/notice/toggleTop/${n.noticeId}`)
    if (res.data.code === 200) {
        ElMessage.success(n.isTop === 1 ? '已取消置顶' : '已置顶')
        fetchNotices()
    }
}

// 删除
const deleteNotice = async (id) => {
    await ElMessageBox.confirm('确定删除该公告？', '提示', { type: 'warning' })
    const res = await axios.delete(`/api/notice/${id}`)
    if (res.data.code === 200) { ElMessage.success('已删除'); fetchNotices() }
}

// 详情
const detailVisible = ref(false)
const detailNotice = ref(null)
const openDetail = (n) => { detailNotice.value = n; detailVisible.value = true }

// 编辑/发布
const formVisible = ref(false)
const editForm = ref({})
const userInfo = JSON.parse(sessionStorage.getItem('user') || '{}')

// ── Quill 富文本 ──────────────────────────────────
let quill = null

const initQuill = () => {
    if (typeof window.Quill === 'undefined') {
        const link = document.createElement('link')
        link.rel = 'stylesheet'
        link.href = 'https://cdnjs.cloudflare.com/ajax/libs/quill/1.3.7/quill.snow.min.css'
        document.head.appendChild(link)
        const script = document.createElement('script')
        script.src = 'https://cdnjs.cloudflare.com/ajax/libs/quill/1.3.7/quill.min.js'
        script.onload = () => createQuill()
        document.head.appendChild(script)
    } else {
        createQuill()
    }
}

const createQuill = () => {
    quill = new window.Quill('#sn-quill-editor', {
        theme: 'snow',
        modules: { toolbar: { container: '#sn-quill-toolbar' } },
        placeholder: '请输入公告内容，支持富文本格式…'
    })
    quill.on('text-change', () => {
        editForm.value.content = quill.root.innerHTML
    })
    // 填充已有内容
    if (editForm.value.content) {
        quill.root.innerHTML = editForm.value.content
    }
}

const onDialogOpened = () => {
    if (quill) {
        quill.root.innerHTML = editForm.value.content || ''
    } else {
        initQuill()
    }
}

const onDialogClosed = () => {
    if (quill) {
        quill = null
        // 下次打开重新初始化，避免 DOM 残留
        const el = document.getElementById('sn-quill-editor')
        if (el) el.innerHTML = ''
    }
}

const openDialog = (n) => {
    editForm.value = n
        ? { ...n }
        : { title: '', content: '', noticeType: '系统公告', isTop: 0, authorName: userInfo.nickname || '超级管理员', authorId: userInfo.userId }
    formVisible.value = true
}

const saveNotice = async () => {
    if (!editForm.value.title) return ElMessage.warning('请填写标题')
    if (!editForm.value.content) return ElMessage.warning('请填写内容')
    const res = await axios.post('/api/notice/save', editForm.value)
    if (res.data.code === 200) {
        ElMessage.success('发布成功')
        formVisible.value = false
        fetchNotices()
    }
}

onMounted(() => {
    fetchTypes()   // ← 从后端加载类型
    fetchNotices()
})
</script>

<style scoped>
.command-center {
    min-height: 100vh;
    background: #faf8f5;
    color: #2c1a0e;
    font-family: 'Noto Serif SC', 'Noto Sans SC', serif;
    padding: 32px 40px;
}

/* 顶部 */
.header-deck {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 28px;
    padding-bottom: 24px;
    border-bottom: 1px solid #e8dfd0;
}

.title-eyebrow {
    font-size: 11px;
    letter-spacing: 3px;
    color: #8b1a1a;
    display: block;
    margin-bottom: 8px;
    font-weight: 600;
}

.title-main {
    font-size: 26px;
    font-weight: 700;
    color: #1a0a0a;
    margin: 0 0 20px;
    letter-spacing: 1px;
}

.header-stats {
    display: flex;
    gap: 16px;
}

.stat-pill {
    display: flex;
    flex-direction: column;
    align-items: center;
    background: #fff;
    border: 1px solid #e8dfd0;
    border-radius: 10px;
    padding: 10px 20px;
    min-width: 80px;
}

.stat-n {
    font-size: 24px;
    font-weight: 700;
    color: #8b1a1a;
}

.stat-l {
    font-size: 11px;
    color: #9a7c5a;
    margin-top: 2px;
}

.btn-new {
    background: #8b1a1a !important;
    border: none !important;
    border-radius: 8px !important;
    color: white !important;
    font-weight: 600 !important;
    font-size: 14px !important;
    padding: 12px 24px !important;
    height: auto !important;
    display: flex;
    align-items: center;
    gap: 8px;
}

.btn-new:hover {
    background: #a52020 !important;
}

.btn-plus {
    font-size: 18px;
    font-weight: 300;
}

/* 控制栏 */
.control-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20px;
    flex-wrap: wrap;
    gap: 12px;
}

.type-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
}

.type-tag {
    padding: 5px 14px;
    border-radius: 20px;
    font-size: 13px;
    cursor: pointer;
    border: 1px solid #e8dfd0;
    color: #9a7c5a;
    background: #fff;
    transition: all 0.2s;
    user-select: none;
}

.type-tag:hover {
    border-color: #8b1a1a;
    color: #8b1a1a;
}

.type-tag.active {
    background: #8b1a1a;
    border-color: #8b1a1a;
    color: white;
}

.type-tag.add-tag {
    border-style: dashed;
}

.search-group {
    display: flex;
    gap: 10px;
}

/* 公告列表 */
.notice-grid {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.empty-state {
    text-align: center;
    padding: 80px;
    color: #9a7c5a;
    font-size: 15px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
}

.empty-icon {
    font-size: 48px;
}

.notice-row {
    display: flex;
    align-items: center;
    gap: 16px;
    background: #fff;
    border: 1px solid #e8dfd0;
    border-radius: 12px;
    padding: 16px 20px;
    position: relative;
    transition: all 0.2s;
    animation: rowIn 0.35s ease both;
    overflow: hidden;
}

@keyframes rowIn {
    from {
        opacity: 0;
        transform: translateY(8px);
    }

    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.notice-row:hover {
    border-color: #c8a96e;
    box-shadow: 0 2px 10px rgba(139, 26, 26, 0.06);
}

.notice-row.is-top {
    border-color: #c8a96e;
    background: #fffbf5;
}

.notice-row.is-top::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 3px;
    background: #8b1a1a;
    border-radius: 3px 0 0 3px;
}

.top-badge {
    position: absolute;
    top: 8px;
    right: 108px;
    font-size: 11px;
    color: #8b1a1a;
    background: rgba(139, 26, 26, 0.07);
    padding: 2px 8px;
    border-radius: 4px;
    border: 1px solid rgba(139, 26, 26, 0.15);
}

/* 类型标记 */
.row-type {
    flex-shrink: 0;
    font-size: 12px;
    font-weight: 600;
    padding: 4px 10px;
    border-radius: 6px;
    white-space: nowrap;
}

.row-type.danger {
    background: #fff0f0;
    color: #8b1a1a;
}

.row-type.primary {
    background: #f0f4ff;
    color: #4a6fa5;
}

.row-type.success {
    background: #f0faf2;
    color: #2d7a3a;
}

.row-type.warning {
    background: #fff8e6;
    color: #9a6a00;
}

.row-type.default {
    background: #f5f0eb;
    color: #9a7c5a;
}

/* 主体 */
.row-body {
    flex: 1;
    cursor: pointer;
    overflow: hidden;
}

.row-title {
    font-size: 15px;
    font-weight: 600;
    color: #2c1a0e;
    margin-bottom: 5px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.row-meta {
    display: flex;
    gap: 6px;
    font-size: 12px;
    color: #9a7c5a;
    align-items: center;
    flex-wrap: wrap;
}

.meta-dot {
    color: #d4c4a8;
}

.meta-belong {
    color: #8b1a1a;
}

/* 操作按钮 */
.row-actions {
    display: flex;
    gap: 6px;
    flex-shrink: 0;
}

.action-btn {
    width: 34px;
    height: 34px;
    border-radius: 8px;
    border: 1px solid #e8dfd0;
    background: #faf8f5;
    cursor: pointer;
    font-size: 15px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s;
}

.action-btn:hover {
    border-color: #c8a96e;
    background: #fff;
}

.top-btn.topped {
    background: #fff8e6;
    border-color: #c8a96e;
}

.del-btn:hover {
    background: #fff0f0;
    border-color: #e8a0a0;
}

/* 详情弹窗 */
.detail-type-row {
    display: flex;
    gap: 10px;
    align-items: center;
    margin-bottom: 14px;
}

.detail-type-tag {
    font-size: 12px;
    font-weight: 600;
    padding: 4px 12px;
    border-radius: 6px;
}

.detail-type-tag.danger {
    background: #fff0f0;
    color: #8b1a1a;
}

.detail-type-tag.primary {
    background: #f0f4ff;
    color: #4a6fa5;
}

.detail-type-tag.success {
    background: #f0faf2;
    color: #2d7a3a;
}

.detail-type-tag.warning {
    background: #fff8e6;
    color: #9a6a00;
}

.detail-type-tag.default {
    background: #f5f0eb;
    color: #9a7c5a;
}

.detail-top-tag {
    font-size: 12px;
    color: #8b1a1a;
}

.detail-title {
    font-size: 20px;
    font-weight: 700;
    color: #1a0a0a;
    margin: 0 0 10px;
}

.detail-meta {
    font-size: 13px;
    color: #9a7c5a;
    display: flex;
    gap: 8px;
    margin-bottom: 20px;
    flex-wrap: wrap;
}

/* 富文本内容 */
.detail-content {
    font-size: 14px;
    color: #2c1a0e;
    line-height: 1.9;
    background: #faf8f5;
    border-radius: 10px;
    padding: 20px;
    border: 1px solid #e8dfd0;
}

.detail-content :deep(h1),
.detail-content :deep(h2),
.detail-content :deep(h3) {
    color: #1a0a0a;
    margin: 12px 0 8px;
}

.detail-content :deep(p) {
    margin: 6px 0;
}

.detail-content :deep(strong) {
    color: #8b1a1a;
}

.detail-content :deep(img) {
    max-width: 100%;
    border-radius: 8px;
    margin: 8px 0;
}

.detail-content :deep(ul),
.detail-content :deep(ol) {
    padding-left: 20px;
}

/* 编辑表单 */
.edit-form :deep(.el-form-item__label) {
    color: #5c3d1e;
    font-weight: 500;
}

/* 类型管理弹出 */
.type-manager {
    padding: 4px 0;
}

.type-list-manage {
    max-height: 180px;
    overflow-y: auto;
    margin-bottom: 10px;
}

.type-manage-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 6px 0;
    border-bottom: 1px solid #f0e8da;
    font-size: 13px;
    color: #2c1a0e;
}

.type-add-row {
    display: flex;
    gap: 8px;
}

/* Quill 编辑器 */
.quill-wrap {
    width: 100%;
    border: 1px solid #e8dfd0;
    border-radius: 8px;
    overflow: hidden;
}

.quill-toolbar {
    border-bottom: 1px solid #e8dfd0 !important;
    background: #faf8f5 !important;
    padding: 6px 8px !important;
}

.quill-editor-body {
    min-height: 260px;
    font-size: 14px;
    font-family: 'Noto Serif SC', serif;
}
</style>