<template>
    <div class="product-edit-page">
        <div class="page-header">
            <el-button link @click="$router.back()">← 返回</el-button>
            <h2 class="page-title">{{ productId ? '编辑商品' : '发布商品' }}</h2>
            <el-button type="primary" :loading="saving" @click="saveAll">保存发布</el-button>
        </div>

        <div class="edit-body">
            <!-- 左侧主内容 -->
            <div class="main-col">
                <!-- 基本信息 -->
                <div class="section-card">
                    <div class="card-title">基本信息</div>
                    <el-form :model="form" label-width="80px" size="small">
                        <el-form-item label="商品名称">
                            <el-input v-model="form.name" placeholder="请输入商品名称" />
                        </el-form-item>
                        <el-form-item label="商品分类">
                            <el-select v-model="form.catId" style="width:100%">
                                <el-option v-for="c in categories" :key="c.catId" :label="c.name" :value="c.catId" />
                            </el-select>
                        </el-form-item>
                        <el-form-item label="商品类型">
                            <el-radio-group v-model="form.type">
                                <el-radio :label="0">实体商品</el-radio>
                                <el-radio :label="1">虚拟商品</el-radio>
                            </el-radio-group>
                        </el-form-item>
                        <el-form-item label="上架状态">
                            <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="上架"
                                inactive-text="下架" />
                        </el-form-item>
                    </el-form>
                </div>

                <!-- 商品封面 -->
                <div class="section-card">
                    <div class="card-title">商品封面</div>
                    <el-upload action="/api/upload/image" name="file" :show-file-list="false" accept="image/*"
                        :on-success="(r) => form.coverUrl = r.data" :before-upload="beforeUpload">
                        <div class="cover-upload">
                            <img v-if="form.coverUrl" :src="form.coverUrl" class="cover-preview" />
                            <div v-else class="cover-placeholder">
                                <div style="font-size:32px">📷</div>
                                <div>点击上传封面图</div>
                            </div>
                        </div>
                    </el-upload>
                </div>

                <!-- 商品描述（富文本） -->
                <div class="section-card">
                    <div class="card-title">商品描述</div>
                    <div class="editor-wrap">
                        <div id="product-toolbar">
                            <span class="ql-formats">
                                <select class="ql-header">
                                    <option value="1">标题</option>
                                    <option value="">正文</option>
                                </select>
                            </span>
                            <span class="ql-formats">
                                <button class="ql-bold"></button>
                                <button class="ql-italic"></button>
                                <button class="ql-underline"></button>
                            </span>
                            <span class="ql-formats">
                                <button class="ql-list" value="ordered"></button>
                                <button class="ql-list" value="bullet"></button>
                            </span>
                            <span class="ql-formats">
                                <button class="ql-image"></button>
                                <button class="ql-link"></button>
                                <button class="ql-clean"></button>
                            </span>
                        </div>
                        <div id="product-editor" style="min-height:200px"></div>
                    </div>
                </div>

                <!-- 规格与SKU -->
                <div class="section-card">
                    <div class="card-title" style="display:flex;justify-content:space-between;align-items:center">
                        <span>规格设置</span>
                        <el-button size="small" @click="addSpec">+ 添加规格</el-button>
                    </div>

                    <div v-for="(spec, si) in specList" :key="si" class="spec-row">
                        <el-input v-model="spec.specName" placeholder="规格名，如：款式" size="small"
                            style="width:110px;flex-shrink:0" />
                        <div class="spec-tags">
                            <el-tag v-for="(val, vi) in spec.values" :key="vi" closable size="small"
                                @close="spec.values.splice(vi, 1); rebuildSkus()">{{ val }}</el-tag>
                            <el-input v-model="spec.inputVal" placeholder="输入后回车" size="small" style="width:100px"
                                @keyup.enter="addSpecValue(spec)" />
                        </div>
                        <el-button link type="danger" size="small"
                            @click="specList.splice(si, 1); rebuildSkus()">删除</el-button>
                    </div>

                    <!-- SKU表格 -->
                    <div v-if="skuList.length > 0" style="margin-top:16px">
                        <div class="card-title">SKU明细</div>
                        <div class="sku-tip">各SKU库存之和 = 商品总库存（当前：{{ skuStockSum }} / 总库存：{{ form.stock }}）
                            <span v-if="skuStockSum !== form.stock" style="color:#f56c6c">⚠️ 不一致</span>
                        </div>
                        <el-table :data="skuList" size="small" border>
                            <el-table-column prop="skuName" label="规格" min-width="120" />
                            <el-table-column label="SKU图片" width="90">
                                <template #default="{ row }">
                                    <el-upload action="/api/upload/image" name="file" :show-file-list="false"
                                        accept="image/*" :on-success="(r) => row.coverUrl = r.data">
                                        <img v-if="row.coverUrl" :src="row.coverUrl"
                                            style="width:40px;height:40px;object-fit:cover;border-radius:4px;cursor:pointer" />
                                        <el-button v-else size="small" link>上传</el-button>
                                    </el-upload>
                                </template>
                            </el-table-column>
                            <el-table-column label="价格" width="130">
                                <template #default="{ row }">
                                    <el-input-number v-model="row.price" :min="0" :precision="2" size="small"
                                        style="width:110px" />
                                </template>
                            </el-table-column>
                            <el-table-column label="库存" width="110">
                                <template #default="{ row }">
                                    <el-input-number v-model="row.stock" :min="0" size="small" style="width:90px"
                                        @change="syncTotalStock" />
                                </template>
                            </el-table-column>
                            <el-table-column label="预警" width="110">
                                <template #default="{ row }">
                                    <el-input-number v-model="row.stockWarning" :min="0" size="small"
                                        style="width:90px" />
                                </template>
                            </el-table-column>
                        </el-table>
                    </div>

                    <!-- 无规格时直接填价格库存 -->
                    <div v-else>
                        <el-form :model="form" label-width="80px" size="small" style="margin-top:12px">
                            <el-row :gutter="12">
                                <el-col :span="8">
                                    <el-form-item label="价格">
                                        <el-input-number v-model="form.price" :min="0" :precision="2"
                                            style="width:100%" />
                                    </el-form-item>
                                </el-col>
                                <el-col :span="8">
                                    <el-form-item label="库存">
                                        <el-input-number v-model="form.stock" :min="0" style="width:100%" />
                                    </el-form-item>
                                </el-col>
                                <el-col :span="8">
                                    <el-form-item label="预警库存">
                                        <el-input-number v-model="form.stockWarning" :min="0" style="width:100%" />
                                    </el-form-item>
                                </el-col>
                            </el-row>
                        </el-form>
                    </div>
                </div>
            </div>

            <!-- 右侧配置 -->
            <div class="side-col">
                <!-- 运费设置 -->
                <div class="section-card">
                    <div class="card-title">运费设置</div>
                    <el-form label-width="80px" size="small">
                        <el-form-item label="包邮">
                            <el-switch v-model="form.isFreeShipping" :active-value="1" :inactive-value="0" />
                        </el-form-item>
                        <el-form-item v-if="form.isFreeShipping === 0" label="运费">
                            <el-input-number v-model="form.shippingFee" :min="0" :precision="2" style="width:100%">
                                <template #suffix>元</template>
                            </el-input-number>
                        </el-form-item>
                        <el-form-item label="运费险">
                            <el-switch v-model="form.hasShippingInsurance" :active-value="1" :inactive-value="0" />
                        </el-form-item>
                    </el-form>
                </div>

                <!-- 价格区间预览（有SKU时显示） -->
                <div v-if="skuList.length > 0" class="section-card">
                    <div class="card-title">价格区间</div>
                    <div class="price-range">
                        ¥{{ skuMinPrice }} ~ ¥{{ skuMaxPrice }}
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const route = useRoute()
const productId = route.query.productId || null
const user = JSON.parse(sessionStorage.getItem('user') || '{}')
const belongMuseum = user.belong || ''
const adminId = user.userId

const saving = ref(false)
const categories = ref([])
const specList = ref([])
const skuList = ref([])
let quill = null

const form = reactive({
    productId: null, name: '', catId: null, type: 0, status: 1,
    coverUrl: '', description: '', price: 0, stock: 0, stockWarning: 10,
    isFreeShipping: 1, shippingFee: 0, hasShippingInsurance: 0
})

const skuStockSum = computed(() => skuList.value.reduce((s, i) => s + (i.stock || 0), 0))
const skuMinPrice = computed(() => skuList.value.length ? Math.min(...skuList.value.map(s => s.price || 0)).toFixed(2) : 0)
const skuMaxPrice = computed(() => skuList.value.length ? Math.max(...skuList.value.map(s => s.price || 0)).toFixed(2) : 0)

const syncTotalStock = () => { form.stock = skuStockSum.value }

onMounted(async () => {
    await fetchCategories()
    if (productId) await loadProduct()
    await nextTick()
    initQuill()
})

const fetchCategories = async () => {
    const res = await axios.get('/api/shop/category/list')
    if (res.data.code === 200) categories.value = res.data.data
}

const loadProduct = async () => {
    const res = await axios.get(`/api/shop/product/detail/${productId}`)
    if (res.data.code !== 200) return
    const d = res.data.data
    Object.assign(form, d)
    specList.value = (d.specs || []).map(s => ({
        specName: s.specName,
        values: JSON.parse(s.specValues || '[]'),
        inputVal: ''
    }))
    skuList.value = (d.skus || []).map(s => ({ ...s }))
    if (quill && d.description) quill.root.innerHTML = d.description
}

const initQuill = () => {
    const load = (src) => new Promise((res, rej) => {
        if (document.querySelector(`script[src="${src}"]`)) return res()
        const s = document.createElement('script'); s.src = src
        s.onload = res; s.onerror = rej; document.head.appendChild(s)
    })
    const loadCss = (href) => {
        if (document.querySelector(`link[href="${href}"]`)) return
        const l = document.createElement('link'); l.rel = 'stylesheet'; l.href = href
        document.head.appendChild(l)
    }
    loadCss('https://cdnjs.cloudflare.com/ajax/libs/quill/1.3.7/quill.snow.min.css')
    load('https://cdnjs.cloudflare.com/ajax/libs/quill/1.3.7/quill.min.js').then(() => {
        quill = new window.Quill('#product-editor', {
            theme: 'snow',
            modules: {
                toolbar: {
                    container: '#product-toolbar',
                    handlers: { image: imageHandler }
                }
            }
        })
        quill.on('text-change', () => { form.description = quill.root.innerHTML })
        if (form.description) quill.root.innerHTML = form.description
    })
}

const imageHandler = () => {
    const input = document.createElement('input')
    input.type = 'file'; input.accept = 'image/*'; input.click()
    input.onchange = async () => {
        const file = input.files[0]; if (!file) return
        const fd = new FormData(); fd.append('file', file)
        const res = await axios.post('/api/upload/image', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
        if (res.data.code === 200) {
            const range = quill.getSelection(true)
            quill.insertEmbed(range.index, 'image', res.data.data)
        }
    }
}

const beforeUpload = (file) => {
    if (!file.type.startsWith('image/')) { ElMessage.error('只能上传图片'); return false }
    if (file.size / 1024 / 1024 > 5) { ElMessage.error('图片不能超过5MB'); return false }
    return true
}

const addSpec = () => specList.value.push({ specName: '', values: [], inputVal: '' })

const addSpecValue = (spec) => {
    const v = spec.inputVal.trim()
    if (!v || spec.values.includes(v)) return
    spec.values.push(v); spec.inputVal = ''; rebuildSkus()
}

const rebuildSkus = () => {
    const valid = specList.value.filter(s => s.values.length > 0)
    if (!valid.length) { skuList.value = []; return }
    let result = ['']
    valid.forEach(spec => {
        const next = []
        result.forEach(p => spec.values.forEach(v => next.push(p ? `${p}-${v}` : v)))
        result = next
    })
    const existing = {}
    skuList.value.forEach(s => existing[s.skuName] = s)
    skuList.value = result.map(name => existing[name] || {
        skuName: name, price: form.price || 0, stock: 0, stockWarning: 10, coverUrl: ''
    })
}

const saveAll = async () => {
    if (!form.name.trim()) return ElMessage.warning('请输入商品名称')
    if (skuList.value.length > 0 && skuStockSum.value !== form.stock) {
        ElMessage.warning(`SKU库存之和(${skuStockSum.value})与总库存(${form.stock})不一致，已自动同步`)
        form.stock = skuStockSum.value
    }
    saving.value = true
    console.log('保存数据:', JSON.stringify({ ...form, belongMuseum, adminId })) // 👈 加这里
    try {
        const res = await axios.post('/api/shop/product/save', { ...form, belongMuseum, adminId })
        if (res.data.code !== 200) return ElMessage.error('保存失败')
        const pid = form.productId || res.data.data
        if (specList.value.length > 0 && pid) {
            await axios.post('/api/shop/product/sku/save', {
                productId: pid,
                specs: specList.value.map(s => ({ specName: s.specName, specValues: JSON.stringify(s.values) })),
                skus: skuList.value
            })
        }
        ElMessage.success('发布成功')
        router.back()
    } finally {
        saving.value = false
    }
    console.log('保存的SKU:', JSON.stringify(skuList.value))
}
</script>

<style scoped>
.product-edit-page {
    min-height: 100vh;
    background: #f5f5f5;
    padding: 20px;
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

.edit-body {
    display: flex;
    gap: 16px;
    align-items: flex-start;
}

.main-col {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.side-col {
    width: 260px;
    flex-shrink: 0;
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.section-card {
    background: #fff;
    border-radius: 10px;
    padding: 18px;
}

.card-title {
    font-size: 14px;
    font-weight: 600;
    color: #2c1a0e;
    margin-bottom: 14px;
}

.cover-upload {
    cursor: pointer;
}

.cover-preview {
    width: 160px;
    height: 160px;
    object-fit: cover;
    border-radius: 8px;
    border: 1px solid #e8e8e8;
}

.cover-placeholder {
    width: 160px;
    height: 160px;
    border: 2px dashed #ddd;
    border-radius: 8px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #999;
    font-size: 13px;
    gap: 8px;
}

.cover-placeholder:hover {
    border-color: #7a3b1e;
    color: #7a3b1e;
}

.editor-wrap {
    border: 1px solid #e8e8e8;
    border-radius: 8px;
    overflow: hidden;
}

:deep(#product-toolbar) {
    border-bottom: 1px solid #e8e8e8 !important;
    border-top: none !important;
    border-left: none !important;
    border-right: none !important;
}

:deep(.ql-container) {
    border: none !important;
}

:deep(.ql-editor) {
    min-height: 200px;
    font-size: 14px;
}

.spec-row {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    margin-bottom: 10px;
    flex-wrap: wrap;
}

.spec-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    align-items: center;
    flex: 1;
}

.sku-tip {
    font-size: 12px;
    color: #999;
    margin-bottom: 10px;
}

.price-range {
    font-size: 22px;
    font-weight: 700;
    color: #e74c3c;
}
</style>