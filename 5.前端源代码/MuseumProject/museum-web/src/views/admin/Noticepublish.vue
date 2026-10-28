<template>
    <div class="notice-publish">

        <!-- 左侧：公告列表 -->
        <div class="left-panel">
            <div class="panel-header">
                <h2 class="panel-title">📋 公告管理</h2>
                <el-button class="new-btn" @click="handleNew">＋ 新建公告</el-button>
            </div>

            <!-- 搜索筛选 -->
            <div class="search-bar">
                <el-input v-model="searchTitle" placeholder="搜索标题…" clearable class="search-input" @input="fetchList"
                    @clear="fetchList" />
                <el-select v-model="searchType" placeholder="全部类型" clearable class="type-select" @change="fetchList">
                    <el-option label="全部" value="" />
                    <el-option v-for="t in noticeTypes" :key="t" :label="t" :value="t" />
                </el-select>
            </div>

            <!-- 列表 -->
            <div class="notice-list" v-loading="listLoading">
                <div v-if="list.length === 0 && !listLoading" class="list-empty">暂无公告</div>
                <div v-for="item in list" :key="item.noticeId"
                    :class="['notice-item', { active: currentId === item.noticeId, topped: item.isTop === 1 }]"
                    @click="handleSelect(item)">
                    <div class="item-top-row">
                        <span class="item-title">
                            <span v-if="item.isTop === 1" class="top-badge">置顶</span>
                            {{ item.title }}
                        </span>
                    </div>
                    <div class="item-meta-row">
                        <span class="item-type">{{ item.noticeType }}</span>
                        <span class="item-author">{{ item.authorName }}</span>
                        <span class="item-time">{{ formatTime(item.createTime) }}</span>
                    </div>
                    <!-- 仅显示自己发的才有操作按钮 -->
                    <div class="item-actions" v-if="canEdit(item)" @click.stop>
                        <span class="action-link edit-link" @click="handleSelect(item)">编辑</span>
                        <span v-if="canDelete(item)" class="action-link del-link"
                            @click="handleDelete(item.noticeId)">删除</span>
                    </div>
                </div>
            </div>
        </div>

        <!-- 右侧：编辑区 -->
        <div class="right-panel">
            <div class="editor-header">
                <h2 class="editor-title">{{ isEdit ? '编辑公告' : '新建公告' }}</h2>
                <div class="editor-actions">
                    <el-switch v-if="userInfo.role === 'ADMIN'" v-model="form.isTop" :active-value="1"
                        :inactive-value="0" active-text="置顶" inactive-text="" class="top-switch" />
                    <el-button class="save-btn" :loading="saving" @click="handleSave">
                        {{ isEdit ? '保存修改' : '发布公告' }}
                    </el-button>
                </div>
            </div>

            <!-- 标题 -->
            <el-input v-model="form.title" placeholder="请输入公告标题…" class="title-input" maxlength="150" show-word-limit />

            <!-- 类型选择 -->
            <div class="type-row">
                <span class="type-label">公告类型</span>
                <div class="type-options">
                    <span v-for="t in noticeTypes" :key="t"
                        :class="['type-option', { selected: form.noticeType === t }]" @click="form.noticeType = t">{{ t
                        }}</span>
                </div>
            </div>

            <!-- 富文本编辑器 -->
            <div class="editor-wrap">
                <div class="quill-toolbar" id="quill-toolbar">
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
                        <button class="ql-indent" value="-1"></button>
                        <button class="ql-indent" value="+1"></button>
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
                    <button class="ql-image"></button>
                </div>
                <div id="quill-editor" class="quill-editor-body"></div>
            </div>

            <!-- 预览区 -->
            <div class="preview-toggle" @click="previewOpen = !previewOpen">
                {{ previewOpen ? '▲ 收起预览' : '▼ 展开预览' }}
            </div>
            <div v-show="previewOpen" class="preview-area">
                <h3 class="preview-title">{{ form.title || '（无标题）' }}</h3>
                <div class="preview-meta">{{ form.noticeType }} · {{ userInfo.nickname }}</div>
                <div class="preview-content ql-editor" v-html="form.content"></div>
            </div>
        </div>

    </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

// ── 用户信息 ──────────────────────────────────────
const userInfo = ref({})
onMounted(async () => {
    userInfo.value = JSON.parse(sessionStorage.getItem('user') || '{}')
    if (userInfo.value.role === 'ADMIN') {
        form.noticeType = '博物馆公告'
    } else {
        form.noticeType = '巡展资讯'
    }
    form.authorId = userInfo.value.userId
    form.authorName = userInfo.value.nickname || userInfo.value.username

    await fetchList()
    await nextTick()
    try {
        await fetchTypes()
    } catch (e) {
        noticeTypes.value = ['系统公告', '博物馆公告', '巡展资讯', '活动通知', '温馨提示']
    }
    initQuill()
})

onBeforeUnmount(() => {
    if (quill) quill = null
})

// ── 公告类型 ──────────────────────────────────────
const noticeTypes = ref([])

const fetchTypes = async () => {
    const res = await axios.get('/api/notice/types')
    if (res.data.code === 200) noticeTypes.value = res.data.data
}
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

// ── 列表 ──────────────────────────────────────────
const list = ref([])
const listLoading = ref(false)
const searchTitle = ref('')
const searchType = ref('')

const fetchList = async () => {
    listLoading.value = true
    try {
        const res = await axios.get('/api/notice/list', {
            params: {
                title: searchTitle.value || undefined,
                type: searchType.value || undefined,
                authorBelong: userInfo.value.belong || undefined  // ← 加这行
            }
        })
        const data = res.data
        list.value = Array.isArray(data) ? data : (data?.data || [])
    } catch (e) {
        ElMessage.error('获取公告列表失败')
    } finally {
        listLoading.value = false
    }
}

// ── 权限判断 ──────────────────────────────────────
// 管理员可编辑所有，普通发布者只能编辑自己的
const canEdit = (item) => {
    const u = userInfo.value
    if (u.role === 'ADMIN') return true
    return item.authorId === u.userId
}
const canDelete = (item) => {
    const u = userInfo.value
    // 策展方不能删除任何公告
    if (u.role === 'APPLICANT') return false
    // 管理员只能删除本单位的公告
    if (u.role === 'ADMIN') return item.authorBelong === u.belong
    return false
}

// ── 表单 ──────────────────────────────────────────
const isEdit = ref(false)
const currentId = ref(null)
const saving = ref(false)
const previewOpen = ref(false)

const form = reactive({
    noticeId: null,
    title: '',
    content: '',
    authorId: null,
    authorName: '',
    noticeType: '系统公告',
    isTop: 0
})

// 选中某条公告进入编辑
const handleSelect = (item) => {
    if (!canEdit(item)) return
    isEdit.value = true
    currentId.value = item.noticeId
    Object.assign(form, {
        noticeId: item.noticeId,
        title: item.title,
        content: item.content,
        authorId: item.authorId,
        authorName: item.authorName,
        noticeType: item.noticeType,
        isTop: item.isTop
    })
    // 同步到 Quill
    if (quill) quill.root.innerHTML = item.content || ''
}

// 新建
const handleNew = () => {
    isEdit.value = false
    currentId.value = null
    const u = userInfo.value
    Object.assign(form, {
        noticeId: null,
        title: '',
        content: '',
        authorId: u.userId,
        authorName: u.nickname || u.username,
        noticeType: u.role === 'ADMIN' ? '博物馆公告' : '巡展资讯',
        isTop: 0
    })
    if (quill) quill.setContents([])
}

// 保存
const handleSave = async () => {
    if (!form.title.trim()) {
        ElMessage.warning('请输入公告标题')
        return
    }
    if (!form.content || form.content === '<p><br></p>') {
        ElMessage.warning('请输入公告内容')
        return
    }
    saving.value = true
    try {
        const res = await axios.post('/api/notice/save', form)
        const data = res.data
        if (data.code === 200) {
            ElMessage.success(isEdit.value ? '修改成功' : '发布成功')
            await fetchList()
            if (!isEdit.value) handleNew()
        } else {
            ElMessage.error(data.message || '操作失败')
        }
    } catch (e) {
        ElMessage.error('请求失败')
    } finally {
        saving.value = false
    }
}

// 删除
const handleDelete = async (id) => {
    await ElMessageBox.confirm('确定删除这条公告吗？', '确认删除', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
    })
    try {
        const res = await axios.delete(`/api/notice/${id}`)
        const data = res.data
        if (data.code === 200) {
            ElMessage.success('已删除')
            if (currentId.value === id) handleNew()
            await fetchList()
        } else {
            ElMessage.error(data.message || '删除失败')
        }
    } catch (e) {
        if (e !== 'cancel') ElMessage.error('删除失败')
    }
}

// ── Quill 富文本 ───────────────────────────────────
let quill = null

const initQuill = () => {
    if (typeof window.Quill === 'undefined') {
        // 动态加载 Quill
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
    quill = new window.Quill('#quill-editor', {
        theme: 'snow',
        modules: {
            toolbar: {
                container: '#quill-toolbar',
                handlers: {
                    // ✨ 自定义图片处理器
                    image:imageHandler
                }
            }
        },
        placeholder: '请在此输入公告内容，支持富文本格式…'
    })
    quill.on('text-change', () => {
        form.content = quill.root.innerHTML
    })
}

// ✨ 图片上传处理器
const imageHandler = () => {
    const input = document.createElement('input')
    input.setAttribute('type', 'file')
    input.setAttribute('accept', 'image/*')
    input.click()

    input.onchange = async () => {
        const file = input.files[0]
        if (!file) return

        // 文件大小限制 5MB
        if (file.size / 1024 / 1024 > 5) {
            ElMessage.error('图片大小不能超过 5MB')
            return
        }

        const formData = new FormData()
        formData.append('file', file)

        try {
            ElMessage.info('图片上传中…')
            const res = await axios.post('/api/upload/image', formData, {
                headers: { 'Content-Type': 'multipart/form-data' }
            })
            const data = res.data
            if (data.code === 200) {
                // 在光标位置插入图片
                const range = quill.getSelection(true)
                quill.insertEmbed(range.index, 'image', data.data)
                quill.setSelection(range.index + 1)
                ElMessage.success('图片插入成功')
            } else {
                ElMessage.error(data.message || '上传失败')
            }
        } catch (e) {
            ElMessage.error('图片上传失败，请检查网络')
        }
    }
}

// ── 工具函数 ──────────────────────────────────────
const formatTime = (t) => {
    if (!t) return ''
    return String(t).slice(0, 10)
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+SC:wght@400;600;700&family=Noto+Sans+SC:wght@300;400;500&display=swap');

.notice-publish {
    display: flex;
    height: 100vh;
    background: #f5f0e8;
    font-family: 'Noto Sans SC', sans-serif;
    overflow: hidden;
}

/* ── 左侧面板 ── */
.left-panel {
    width: 320px;
    flex-shrink: 0;
    background: #fff;
    border-right: 1px solid #e8dfd0;
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

.panel-header {
    padding: 20px 18px 14px;
    border-bottom: 1px solid #f0e8dc;
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.panel-title {
    font-size: 16px;
    font-weight: 700;
    color: #2c1a0e;
    margin: 0;
    font-family: 'Noto Serif SC', serif;
}

.new-btn {
    background: #7a3b1e !important;
    color: #fff !important;
    border: none !important;
    border-radius: 6px !important;
    font-size: 12px !important;
    padding: 6px 12px !important;
}

.search-bar {
    padding: 12px 14px;
    display: flex;
    gap: 8px;
    border-bottom: 1px solid #f0e8dc;
}

.search-input :deep(.el-input__wrapper) {
    background: #faf7f3;
    box-shadow: 0 0 0 1px #e0d0be;
    border-radius: 6px;
    font-size: 12px;
}

.type-select {
    width: 110px;
    flex-shrink: 0;
}

.type-select :deep(.el-input__wrapper) {
    background: #faf7f3;
    box-shadow: 0 0 0 1px #e0d0be;
    border-radius: 6px;
    font-size: 12px;
}

.notice-list {
    flex: 1;
    overflow-y: auto;
    padding: 8px 0;
}

.list-empty {
    text-align: center;
    color: #b0a090;
    padding: 40px 0;
    font-size: 13px;
}

.notice-item {
    padding: 12px 18px;
    cursor: pointer;
    border-left: 3px solid transparent;
    transition: all 0.15s;
    border-bottom: 1px solid #f8f4ee;
}

.notice-item:hover {
    background: #faf7f3;
}

.notice-item.active {
    background: #fdf5ec;
    border-left-color: #7a3b1e;
}

.notice-item.topped {
    background: #fffbf4;
}

.item-top-row {
    margin-bottom: 5px;
}

.item-title {
    font-size: 13px;
    font-weight: 500;
    color: #2c1a0e;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    line-height: 1.5;
}

.top-badge {
    display: inline-block;
    background: #e8320a;
    color: #fff;
    font-size: 10px;
    padding: 1px 5px;
    border-radius: 3px;
    margin-right: 5px;
    vertical-align: middle;
    flex-shrink: 0;
}

.item-meta-row {
    display: flex;
    gap: 8px;
    align-items: center;
    flex-wrap: wrap;
}

.item-type {
    font-size: 10px;
    background: #f0e8dc;
    color: #7a5c3a;
    padding: 1px 6px;
    border-radius: 3px;
}

.item-author,
.item-time {
    font-size: 11px;
    color: #b0a090;
}

.item-actions {
    margin-top: 6px;
    display: flex;
    gap: 12px;
}

.action-link {
    font-size: 11px;
    cursor: pointer;
}

.edit-link {
    color: #7a3b1e;
}

.del-link {
    color: #c0392b;
}

.edit-link:hover {
    text-decoration: underline;
}

.del-link:hover {
    text-decoration: underline;
}

/* ── 右侧编辑区 ── */
.right-panel {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    padding: 24px 32px;
    gap: 14px;
}

.editor-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-shrink: 0;
}

.editor-title {
    font-size: 20px;
    font-weight: 700;
    color: #2c1a0e;
    margin: 0;
    font-family: 'Noto Serif SC', serif;
}

.editor-actions {
    display: flex;
    align-items: center;
    gap: 14px;
}

.top-switch :deep(.el-switch__label) {
    font-size: 12px;
    color: #7a5c3a;
}

.save-btn {
    background: #7a3b1e !important;
    color: #fff !important;
    border: none !important;
    border-radius: 8px !important;
    padding: 10px 24px !important;
    font-size: 14px !important;
    font-family: 'Noto Sans SC', sans-serif !important;
}

.title-input :deep(.el-input__wrapper) {
    background: #fff;
    box-shadow: 0 0 0 1.5px #e0d0be;
    border-radius: 8px;
    font-size: 18px;
    padding: 10px 14px;
}

.title-input :deep(.el-input__inner) {
    font-size: 18px;
    font-weight: 600;
    color: #2c1a0e;
    font-family: 'Noto Serif SC', serif;
}

.type-row {
    display: flex;
    align-items: center;
    gap: 14px;
    flex-shrink: 0;
}

.type-label {
    font-size: 13px;
    color: #7a5c3a;
    flex-shrink: 0;
}

.type-options {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
}

.type-option {
    padding: 4px 14px;
    border-radius: 16px;
    font-size: 12px;
    cursor: pointer;
    background: #f0e8dc;
    color: #7a5c3a;
    border: 1.5px solid transparent;
    transition: all 0.15s;
    user-select: none;
}

.type-option:hover {
    background: #e8ddd0;
}

.type-option.selected {
    background: #7a3b1e;
    color: #fff;
    border-color: #7a3b1e;
}

/* 富文本编辑器 */
.editor-wrap {
    flex: 1;
    display: flex;
    flex-direction: column;
    background: #fff;
    border-radius: 10px;
    border: 1.5px solid #e0d0be;
    overflow: hidden;
    min-height: 0;
}

/* 覆盖 Quill 默认样式 */
:deep(#quill-toolbar) {
    border: none !important;
    border-bottom: 1px solid #f0e8dc !important;
    background: #faf7f3;
    padding: 8px 12px;
    flex-shrink: 0;
}

:deep(.ql-toolbar .ql-stroke) {
    stroke: #7a5c3a !important;
}

:deep(.ql-toolbar .ql-fill) {
    fill: #7a5c3a !important;
}

:deep(.ql-toolbar button:hover .ql-stroke) {
    stroke: #7a3b1e !important;
}

:deep(.ql-toolbar button.ql-active .ql-stroke) {
    stroke: #7a3b1e !important;
}

:deep(.ql-toolbar .ql-picker-label) {
    color: #7a5c3a !important;
}

.quill-editor-body {
    flex: 1;
    overflow-y: auto;
}

:deep(.ql-editor) {
    min-height: 100%;
    font-size: 15px;
    line-height: 1.9;
    color: #2c1a0e;
    font-family: 'Noto Sans SC', sans-serif;
    padding: 18px 20px;
}

:deep(.ql-editor.ql-blank::before) {
    color: #c0b090;
    font-style: normal;
}

:deep(.ql-container) {
    border: none !important;
    font-size: 15px;
    flex: 1;
    display: flex;
    flex-direction: column;
}

/* 预览 */
.preview-toggle {
    flex-shrink: 0;
    font-size: 12px;
    color: #9a7c5a;
    cursor: pointer;
    text-align: center;
    padding: 4px 0;
    user-select: none;
}

.preview-toggle:hover {
    color: #7a3b1e;
}

.preview-area {
    flex-shrink: 0;
    max-height: 260px;
    overflow-y: auto;
    background: #fff;
    border-radius: 10px;
    border: 1.5px solid #e0d0be;
    padding: 18px 22px;
}

.preview-title {
    font-size: 18px;
    font-weight: 700;
    color: #2c1a0e;
    font-family: 'Noto Serif SC', serif;
    margin: 0 0 6px;
}

.preview-meta {
    font-size: 12px;
    color: #9a7c5a;
    margin-bottom: 14px;
    padding-bottom: 10px;
    border-bottom: 1px solid #f0e8dc;
}

.preview-content {
    font-size: 14px;
    color: #3c2a1a;
    line-height: 1.9;
}
</style>