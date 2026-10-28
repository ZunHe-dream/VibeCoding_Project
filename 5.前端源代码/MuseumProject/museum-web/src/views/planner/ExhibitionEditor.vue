<template>
    <div class="editor-page">
        <div class="page-header">
            <el-button link @click="$router.back()">← 返回</el-button>
            <h2 class="page-title">编辑巡展介绍</h2>
            <el-button type="primary" :loading="saving" @click="handleSave">保存并发布</el-button>
        </div>

        <!-- 只读硬属性 -->
        <div class="info-card">
            <div class="info-title">巡展基本信息（不可编辑）</div>
            <el-descriptions :column="3" border size="small">
                <el-descriptions-item label="巡展名称">{{ plan.title }}</el-descriptions-item>
                <el-descriptions-item label="举办地点">{{ plan.location }}</el-descriptions-item>
                <el-descriptions-item label="开展时间">{{ plan.startTime?.slice(0, 10) }}</el-descriptions-item>
                <el-descriptions-item label="结束时间">{{ plan.endTime?.slice(0, 10) }}</el-descriptions-item>
                <el-descriptions-item label="每日限额">{{ plan.maxCapacity }} 人</el-descriptions-item>
                <el-descriptions-item label="票价">
                    {{ plan.ticketPrice > 0 ? '¥' + plan.ticketPrice : '见详情' }}
                </el-descriptions-item>
            </el-descriptions>
        </div>

        <!-- 可编辑内容 -->
        <!-- 封面图编辑 -->
        <div class="info-card" style="margin-bottom:16px">
            <div class="editor-label" style="margin-bottom:12px">展览封面图</div>
            <el-upload action="/api/file/upload" name="file" :show-file-list="false" accept="image/*"
                :on-success="handleCoverSuccess" :before-upload="beforeUpload">
                <div class="cover-upload-area">
                    <img v-if="coverUrl" :src="coverUrl" class="cover-preview" />
                    <div v-else class="cover-placeholder">
                        <div style="font-size:32px">📷</div>
                        <div>点击上传封面图</div>
                    </div>
                </div>
            </el-upload>
        </div>
        <div class="editor-card">
            <div class="editor-label">展览宣传介绍（富文本）</div>
            <div class="quill-wrap">
                <div id="ex-toolbar">
                    <span class="ql-formats">
                        <select class="ql-header">
                            <option value="1">标题1</option>
                            <option value="2">标题2</option>
                            <option value="">正文</option>
                        </select>
                    </span>
                    <span class="ql-formats">
                        <button class="ql-bold"></button>
                        <button class="ql-italic"></button>
                        <button class="ql-underline"></button>
                    </span>
                    <span class="ql-formats">
                        <select class="ql-color"></select>
                        <select class="ql-background"></select>
                    </span>
                    <span class="ql-formats">
                        <button class="ql-list" value="ordered"></button>
                        <button class="ql-list" value="bullet"></button>
                        <button class="ql-align" value="center"></button>
                        <button class="ql-align" value="right"></button>
                    </span>
                    <span class="ql-formats">
                        <button class="ql-image"></button>
                        <button class="ql-link"></button>
                        <button class="ql-clean"></button>
                    </span>
                </div>
                <div id="ex-editor"></div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const exhibitionId = route.query.exhibitionId
const saving = ref(false)
const plan = ref({})
let quill = null

const coverUrl = ref('')

// 在 loadDetail 里回填封面
// 找到 plan.value = res.data.data.application || {} 这行下面加：
// coverUrl.value = plan.value.coverURL || plan.value.coverUrl || ''


const handleCoverSuccess = (res) => {
    console.log('上传返回：', JSON.stringify(res))  // ← 加这行
    coverUrl.value = 'http://localhost:8080' + res.data  // 已经是完整URL
    ElMessage.success('封面上传成功')
}

const beforeUpload = (file) => {
    if (!file.type.startsWith('image/')) { ElMessage.error('只能上传图片'); return false }
    if (file.size / 1024 / 1024 > 5) { ElMessage.error('图片不能超过5MB'); return false }
    return true
}

onMounted(async () => {
    // 加载巡展数据
    const res = await axios.get(`/api/exhibition/detail/${exhibitionId}`)
    if (res.data.code === 200) {
        plan.value = res.data.data.application || {}
        coverUrl.value = plan.value.coverURL || plan.value.coverUrl || ''
        console.log('加载到的plan数据:', JSON.stringify(plan.value)) //
    }
    await nextTick()
    initQuill()
})

const initQuill = () => {
    const loadCss = (href) => {
        if (document.querySelector(`link[href="${href}"]`)) return
        const l = document.createElement('link'); l.rel = 'stylesheet'; l.href = href
        document.head.appendChild(l)
    }
    const loadScript = (src) => new Promise((res, rej) => {
        if (document.querySelector(`script[src="${src}"]`)) return res()
        const s = document.createElement('script'); s.src = src
        s.onload = res; s.onerror = rej; document.head.appendChild(s)
    })

    loadCss('https://cdnjs.cloudflare.com/ajax/libs/quill/1.3.7/quill.snow.min.css')
    loadScript('https://cdnjs.cloudflare.com/ajax/libs/quill/1.3.7/quill.min.js').then(() => {
        quill = new window.Quill('#ex-editor', {
            theme: 'snow',
            modules: {
                toolbar: {
                    container: '#ex-toolbar',
                    handlers: { image: imageHandler }
                }
            },
            placeholder: '在这里编写巡展的详细介绍，支持图文混排…'
        })
        // 回填已有内容
        if (plan.value.richContent) {
            quill.root.innerHTML = plan.value.richContent
        }
    })
}

const imageHandler = () => {
    const input = document.createElement('input')
    input.type = 'file'; input.accept = 'image/*'; input.click()
    input.onchange = async () => {
        const file = input.files[0]; if (!file) return
        const fd = new FormData(); fd.append('file', file)
        const res = await axios.post('/api/upload/image', fd, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
        if (res.data.code === 200) {
            console.log('图片上传返回:', JSON.stringify(res.data))
            const url = res.data.data 
            const range = quill.getSelection(true)
            quill.insertEmbed(range.index, 'image', url)
        }
    }
}

const handleSave = async () => {
    if (!quill) return
    const content = quill.root.innerHTML
    console.log('准备保存的内容:', content) // 👈 加这行
    console.log('exhibitionId:', exhibitionId) // 👈 加这行
    if (content === '<p><br></p>') return ElMessage.warning('请输入展览介绍内容')

    saving.value = true
    try {
        // 更新 richContent 字段
        const res = await axios.post('/api/exhibition/plan/updateContent', {
            exhibitionId: Number(exhibitionId),
            richContent: content,
            coverURL: coverUrl.value,
            coverUrl: coverUrl.value // ✨ 加这行
        })
        if (res.data.code === 200) {
            ElMessage.success('发布成功')
            router.back()
        } else {
            ElMessage.error(res.data.message || '保存失败')
        }
    } finally {
        saving.value = false
    }
}
</script>

<style scoped>
.editor-page {
    max-width: 960px;
    margin: 0 auto;
    padding: 24px 20px;
    font-family: 'Noto Sans SC', sans-serif;
}

.page-header {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 20px;
}

.page-title {
    font-size: 18px;
    font-weight: 700;
    color: #2c1a0e;
    margin: 0;
    flex: 1;
}

.info-card {
    background: #fff;
    border-radius: 10px;
    padding: 18px;
    margin-bottom: 16px;
    border-left: 4px solid #c8a96e;
}

.info-title {
    font-size: 13px;
    color: #9a7c5a;
    margin-bottom: 12px;
    letter-spacing: 1px;
}

.editor-card {
    background: #fff;
    border-radius: 10px;
    padding: 18px;
}

.editor-label {
    font-size: 14px;
    font-weight: 600;
    color: #2c1a0e;
    margin-bottom: 12px;
}

.quill-wrap {
    border: 1px solid #e0d0be;
    border-radius: 8px;
    overflow: hidden;
}

:deep(#ex-toolbar) {
    border: none !important;
    border-bottom: 1px solid #e0d0be !important;
    background: #faf7f3;
}

:deep(.ql-container) {
    border: none !important;
}

:deep(.ql-editor) {
    min-height: 400px;
    font-size: 15px;
    line-height: 1.9;
}

:deep(.ql-editor img) {
    max-width: 100%;
    border-radius: 6px;
}

.cover-upload-area {
    cursor: pointer;
}

.cover-preview {
    width: 100%;
    max-height: 260px;
    object-fit: cover;
    border-radius: 8px;
    display: block;
}

.cover-placeholder {
    width: 100%;
    height: 180px;
    border: 2px dashed #e0d0be;
    border-radius: 8px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #9a7c5a;
    gap: 8px;
    font-size: 13px;
}

.cover-placeholder:hover {
    border-color: #7a3b1e;
    color: #7a3b1e;
}
</style>