<template>
    <div class="admin-shop">
        <el-tabs v-model="activeTab">
            <el-tab-pane label="商品管理" name="products" />
            <el-tab-pane label="订单管理" name="orders" />
            <el-tab-pane label="分类管理" name="categories" />
        </el-tabs>

        <!-- 商品管理 -->
        <div v-if="activeTab === 'products'">
            <div class="toolbar">
                <el-button style="background-color:#7e5c3b; border-color:#7e5c3b; color:#fff;"
                    @click="openProductDialog()">+ 发布商品</el-button>
                <el-input v-model="productKeyword" placeholder="搜索商品…" style="width:200px;margin-left:12px" clearable
                    @input="fetchProducts" @clear="fetchProducts" />
            </div>

            <el-table :data="products" v-loading="productLoading" border size="small" style="margin-top:12px">
                <el-table-column label="图片" width="70">
                    <template #default="{ row }">
                        <img :src="row.coverUrl || 'https://via.placeholder.com/50'"
                            style="width:44px;height:44px;object-fit:cover;border-radius:4px" />
                    </template>
                </el-table-column>
                <el-table-column prop="name" label="商品名称" />
                <el-table-column prop="catName" label="分类" width="90" />
                <el-table-column prop="price" label="价格" width="80">
                    <template #default="{ row }">¥{{ row.price }}</template>
                </el-table-column>
                <el-table-column label="库存" width="80">
                    <template #default="{ row }">
                        <span :style="{ color: row.lowStock ? '#f56c6c' : '#333' }">
                            {{ row.stock }}{{ row.lowStock ? ' ⚠️' : '' }}
                        </span>
                    </template>
                </el-table-column>
                <el-table-column prop="sales" label="销量" width="70" />
                <el-table-column label="状态" width="80">
                    <template #default="{ row }">
                        <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                            {{ row.status === 1 ? '上架' : '下架' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="160">
                    <template #default="{ row }">
                        <el-button link @click="openProductDialog(row)">编辑</el-button>
                        <el-button link @click="toggleStatus(row)">{{ row.status === 1 ? '下架' : '上架' }}</el-button>
                        <el-button link type="danger" @click="deleteProduct(row.productId)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </div>

        <!-- 订单管理 -->
        <div v-if="activeTab === 'orders'">
            <div class="toolbar">
                <el-select v-model="orderStatusFilter" placeholder="全部状态" clearable style="width:140px"
                    @change="fetchOrders">
                    <el-option label="待付款" :value="0" />
                    <el-option label="待发货" :value="1" />
                    <el-option label="待收货" :value="2" />
                    <el-option label="已完成" :value="3" />
                    <el-option label="已取消" :value="4" />
                    <el-option label="退款中" :value="5" />
                    <el-option label="已退款" :value="6" />
                </el-select>
                <el-button style="background-color:#7e5c3b; border-color:#7e5c3b; color:#fff;margin-left:12px"
                    @click="exportOrders">
                    导出 Excel
                </el-button>
            </div>

            <el-table :data="adminOrders" v-loading="orderLoading" border size="small" style="margin-top:12px">
                <el-table-column prop="orderId" label="订单号" width="180" />
                <el-table-column label="商品" min-width="160">
                    <template #default="{ row }">
                        <div v-for="item in row.items?.filter(i => i.belongMuseum === belongMuseum)" :key="item.itemId"
                            style="font-size:12px;padding:2px 0">
                            {{ item.productName }} x{{ item.quantity }}
                            <el-tag :type="['warning', 'primary', 'success', 'info'][item.itemStatus]" size="small"
                                style="margin-left:4px">
                                {{ ['待发货', '已发货', '已完成', '已取消'][item.itemStatus] ?? '—' }}
                            </el-tag>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column prop="receiver" label="收货人" width="90" />
                <el-table-column prop="phone" label="电话" width="120" />
                <el-table-column prop="totalPrice" label="金额" width="80">
                    <template #default="{ row }">¥{{ row.totalPrice }}</template>
                </el-table-column>
                <el-table-column label="状态" width="90">
                    <template #default="{ row }">
                        <el-tag
                            :type="['warning', 'primary', '', 'success', 'info', 'warning', 'info'][row.status] || 'info'"
                            size="small">
                            {{ ['待付款', '待发货', '待收货', '已完成', '已取消', '退款中', '已退款'][row.status] || '未知' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="createTime" label="下单时间" width="160">
                    <template #default="{ row }">{{ row.createTime?.slice(0, 16) }}</template>
                </el-table-column>
                <el-table-column label="退款原因" width="120">
                    <template #default="{ row }">
                        <span v-if="row.refundReason" style="color:#e6a23c;font-size:12px">{{ row.refundReason }}</span>
                        <span v-else style="color:#ccc">—</span>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="160">
                    <template #default="{ row }">
                        <el-button
                            v-if="row.items?.some(i => i.belongMuseum === belongMuseum && i.itemStatus === 0) && row.status === 1"
                            type="primary" size="small" @click="shipOrder(row.orderId)">
                            发货
                        </el-button>
                        <template v-if="row.status === 5">
                            <el-button type="success" size="small"
                                @click="handleRefund(row, 'approve')">同意退款</el-button>
                            <el-button type="danger" size="small" @click="handleRefund(row, 'reject')">拒绝退款</el-button>
                        </template>
                    </template>
                </el-table-column>
            </el-table>
        </div>

        <!-- 分类管理 -->
        <div v-if="activeTab === 'categories'">
            <div class="toolbar">
                <el-button style="background-color:#7e5c3b; border-color:#7e5c3b; color:#fff;"
                    @click="openCatDialog()">+ 新增分类</el-button>
            </div>
            <el-table :data="categories" border size="small" style="margin-top:12px">
                <el-table-column prop="name" label="分类名称" />
                <el-table-column prop="sort" label="排序" width="80" />
                <el-table-column label="操作" width="120">
                    <template #default="{ row }">
                        <el-button link @click="openCatDialog(row)">编辑</el-button>
                        <el-button link type="danger" @click="deleteCat(row.catId)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </div>

        <!-- 商品编辑弹窗 -->
        <el-dialog v-model="productDialogVisible" :title="productForm.productId ? '编辑商品' : '发布商品'" width="560px"
            destroy-on-close>
            <el-form :model="productForm" label-width="80px" size="small">
                <el-form-item label="商品名称"><el-input v-model="productForm.name" /></el-form-item>
                <el-form-item label="分类">
                    <el-select v-model="productForm.catId" style="width:100%">
                        <el-option v-for="c in categories" :key="c.catId" :label="c.name" :value="c.catId" />
                    </el-select>
                </el-form-item>
                <el-row :gutter="12">
                    <el-col :span="12">
                        <el-form-item label="价格"><el-input-number v-model="productForm.price" :min="0" :precision="2"
                                style="width:100%" /></el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="库存"><el-input-number v-model="productForm.stock" :min="0"
                                style="width:100%" /></el-form-item>
                    </el-col>
                </el-row>
                <el-row :gutter="12">
                    <el-col :span="12">
                        <el-form-item label="预警库存"><el-input-number v-model="productForm.stockWarning" :min="0"
                                style="width:100%" /></el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="商品类型">
                            <el-select v-model="productForm.type" style="width:100%">
                                <el-option label="实体商品" :value="0" />
                                <el-option label="虚拟商品" :value="1" />
                            </el-select>
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-form-item label="商品封面">
                    <div style="display:flex; align-items:flex-start; gap:12px;">
                        <el-upload action="/api/upload/image" name="file" :show-file-list="false" accept="image/*"
                            :on-success="handleUploadSuccess" :on-error="handleUploadError"
                            :before-upload="beforeUpload">
                            <img v-if="productForm.coverUrl" :src="productForm.coverUrl"
                                style="width:100px; height:100px; object-fit:cover; border-radius:6px; border:1px solid #ddd;" />
                            <div v-else
                                style="width:100px; height:100px; border:1px dashed #ccc; border-radius:6px; display:flex; flex-direction:column; align-items:center; justify-content:center; color:#999; font-size:12px; cursor:pointer;">
                                <div style="font-size:24px;">+</div>
                                <div>点击上传</div>
                            </div>
                        </el-upload>
                        <div style="font-size:12px; color:#999; padding-top:8px;">
                            支持 JPG/PNG，建议尺寸 300×300
                        </div>
                    </div>
                </el-form-item>
                <el-form-item label="商品描述">
                    <el-input v-model="productForm.description" type="textarea" :rows="3" />
                </el-form-item>
                <el-form-item label="上架状态">
                    <el-switch v-model="productForm.status" :active-value="1" :inactive-value="0" active-text="上架"
                        inactive-text="下架" />
                </el-form-item>
                <!-- 运费配置 -->
                <el-row :gutter="12">
                    <el-col :span="12">
                        <el-form-item label="包邮">
                            <el-switch v-model="productForm.isFreeShipping" :active-value="1" :inactive-value="0" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="运费险">
                            <el-switch v-model="productForm.hasShippingInsurance" :active-value="1"
                                :inactive-value="0" />
                        </el-form-item>
                    </el-col>
                </el-row>

                <!-- 规格编辑 -->
                <el-divider>商品规格（选填）</el-divider>
                <div v-for="(spec, si) in specList" :key="si" class="spec-row">
                    <el-input v-model="spec.specName" placeholder="规格名，如：款式" style="width:120px;flex-shrink:0"
                        size="small" />
                    <div class="spec-values">
                        <el-tag v-for="(val, vi) in spec.values" :key="vi" closable size="small"
                            @close="spec.values.splice(vi, 1); rebuildSkus()">{{ val }}</el-tag>
                        <el-input v-model="spec.inputVal" placeholder="输入后回车添加" size="small" style="width:120px"
                            @keyup.enter="addSpecValue(spec)" />
                    </div>
                    <el-button link type="danger" size="small"
                        @click="specList.splice(si, 1); rebuildSkus()">删除</el-button>
                </div>
                <el-button size="small" @click="addSpec" style="margin-bottom:12px">+ 添加规格</el-button>

                <!-- SKU表格 -->
                <div v-if="skuList.length > 0">
                    <el-divider>SKU列表</el-divider>
                    <el-table :data="skuList" size="small" border>
                        <el-table-column prop="skuName" label="规格组合" />
                        <el-table-column label="价格" width="130">
                            <template #default="{ row }">
                                <el-input-number v-model="row.price" :min="0" :precision="2" size="small"
                                    style="width:110px" />
                            </template>
                        </el-table-column>
                        <el-table-column label="库存" width="110">
                            <template #default="{ row }">
                                <el-input-number v-model="row.stock" :min="0" size="small" style="width:90px" />
                            </template>
                        </el-table-column>
                        <el-table-column label="预警" width="110">
                            <template #default="{ row }">
                                <el-input-number v-model="row.stockWarning" :min="0" size="small" style="width:90px" />
                            </template>
                        </el-table-column>
                    </el-table>
                </div>
            </el-form>
            <template #footer>
                <el-button @click="productDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="saveProduct">保存</el-button>
            </template>
        </el-dialog>

        <!-- 分类弹窗 -->
        <el-dialog v-model="catDialogVisible" :title="catForm.catId ? '编辑分类' : '新增分类'" width="360px" destroy-on-close>
            <el-form :model="catForm" label-width="70px">
                <el-form-item label="分类名称"><el-input v-model="catForm.name" /></el-form-item>
                <el-form-item label="排序"><el-input-number v-model="catForm.sort" :min="0" /></el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="catDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="saveCat">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import { useRouter } from 'vue-router'
const user = JSON.parse(sessionStorage.getItem('user') || '{}')
const belongMuseum = user.belong || ''
const adminId = user.userId
const activeTab = ref('products')
const specList = ref([]) // 规格列表，每项：{ specName, values[], inputVal }
const skuList = ref([])  // SKU列表，每项：{ skuName, price, stock, stockWarning }
const router = useRouter()
const addSpec = () => {
    specList.value.push({ specName: '', values: [], inputVal: '' })
}
const handleRefund = async (order, action) => {
    const isApprove = action === 'approve'
    const { value: remark } = await ElMessageBox.prompt(
        isApprove ? '同意退款后库存将自动恢复，可填写备注' : '请填写拒绝原因',
        isApprove ? '同意退款' : '拒绝退款',
        {
            confirmButtonText: '确认',
            cancelButtonText: '取消',
            inputPlaceholder: isApprove ? '备注（选填）' : '拒绝原因（必填）',
            confirmButtonClass: isApprove ? '' : 'el-button--danger'
        }
    ).catch(() => null)

    if (remark === null) return // 点了取消

    if (!isApprove && !remark?.trim()) {
        return ElMessage.warning('请填写拒绝原因')
    }

    const url = isApprove ? '/api/shop/order/refund/approve' : '/api/shop/order/refund/reject'
    const res = await axios.post(url, {
        orderId: order.orderId,
        remark: remark || '',
        prevStatus: order.prevStatus
    })

    if (res.data.code === 200) {
        ElMessage.success(isApprove ? '已同意退款' : '已拒绝退款')
        fetchOrders()
    } else {
        ElMessage.error(res.data.message || '操作失败')
    }
}


const exportOrders = () => {
    if (adminOrders.value.length === 0) return ElMessage.warning('暂无订单数据')

    const statusMap = ['待付款', '待发货', '待收货', '已完成', '已取消']

    // 构建表头
    const header = ['订单号', '商品明细', '收货人', '电话', '收货地址', '金额(¥)', '状态', '下单时间', '付款时间']

    // 构建数据行
    const rows = adminOrders.value.map(o => [
        o.orderId,
        (o.items || []).map(i => `${i.productName}x${i.quantity}`).join('、'),
        o.receiver,
        o.phone,
        o.address || '',
        o.totalPrice,
        statusMap[o.status] || '',
        o.createTime?.slice(0, 16) || '',
        o.payTime?.slice(0, 16) || ''
    ])

    // 拼 CSV 内容（加 BOM 解决 Excel 中文乱码）
    const BOM = '\uFEFF'
    const csv = BOM + [header, ...rows]
        .map(row => row.map(cell => `"${String(cell).replace(/"/g, '""')}"`).join(','))
        .join('\n')

    // 触发下载
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `订单导出_${new Date().toLocaleDateString()}.csv`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
}

const addSpecValue = (spec) => {
    if (!spec.inputVal.trim()) return
    if (!spec.values.includes(spec.inputVal.trim())) {
        spec.values.push(spec.inputVal.trim())
        rebuildSkus()
    }
    spec.inputVal = ''
}

// 笛卡尔积自动生成SKU
const rebuildSkus = () => {
    const validSpecs = specList.value.filter(s => s.values.length > 0)
    if (validSpecs.length === 0) { skuList.value = []; return }

    let result = ['']
    validSpecs.forEach(spec => {
        const newResult = []
        result.forEach(prefix => {
            spec.values.forEach(val => {
                newResult.push(prefix ? `${prefix}-${val}` : val)
            })
        })
        result = newResult
    })

    // 保留已有SKU的价格库存
    const existingMap = {}
    skuList.value.forEach(s => existingMap[s.skuName] = s)

    skuList.value = result.map(name => existingMap[name] || {
        skuName: name, price: productForm.price || 0, stock: 0, stockWarning: 10
    })
}

// 商品
const products = ref([])
const productLoading = ref(false)
const productKeyword = ref('')
const productDialogVisible = ref(false)
const productForm = reactive({
    productId: null, name: '', catId: null, price: 0, stock: 0,
    stockWarning: 10, type: 0, coverUrl: '', description: '', status: 1
})

const beforeUpload = (file) => {
    const isImage = file.type.startsWith('image/')
    const isLt5M = file.size / 1024 / 1024 < 5
    if (!isImage) { ElMessage.error('只能上传图片文件'); return false }
    if (!isLt5M) { ElMessage.error('图片不能超过 5MB'); return false }
    return true
}

const handleUploadSuccess = (res) => {
    if (res.code === 200) {
        productForm.coverUrl = res.data
        ElMessage.success('图片上传成功')
    } else {
        ElMessage.error('上传失败：' + res.message)
    }
}

const handleUploadError = () => {
    ElMessage.error('图片上传失败，请检查网络')
}

const fetchProducts = async () => {
    productLoading.value = true
    try {
        const res = await axios.get('/api/shop/product/list', {
            params: { museum: belongMuseum, keyword: productKeyword.value || undefined }
        })
        if (res.data.code === 200) products.value = res.data.data
    } finally {
        productLoading.value = false
    }
}

const openProductDialog = (p = null) => {
    router.push({ path: '/shop/product/edit', query: p ? { productId: p.productId } : {} })
}

// const openProductDialog = async (p = null) => {
//     specList.value = []
//     skuList.value = []
//     if (p) {
//         Object.assign(productForm, p)
//         // 加载已有规格和SKU
//         const res = await axios.get(`/api/shop/product/detail/${p.productId}`)
//         if (res.data.code === 200) {
//             const detail = res.data.data
//             specList.value = (detail.specs || []).map(s => ({
//                 specName: s.specName,
//                 values: JSON.parse(s.specValues || '[]'),
//                 inputVal: ''
//             }))
//             skuList.value = (detail.skus || []).map(s => ({
//                 skuName: s.skuName,
//                 price: s.price,
//                 stock: s.stock,
//                 stockWarning: s.stockWarning
//             }))
//         }
//     } else {
//         Object.assign(productForm, {
//             productId: null, name: '', catId: null, price: 0, stock: 0,
//             stockWarning: 10, type: 0, coverUrl: '', description: '', status: 1,
//             isFreeShipping: 0, hasShippingInsurance: 0
//         })
//     }
//     productDialogVisible.value = true
// }

const saveProduct = async () => {
    const res = await axios.post('/api/shop/product/save', {
        ...productForm, belongMuseum, adminId
    })
    if (res.data.code !== 200) return ElMessage.error('保存失败')

    const savedProductId = productForm.productId || res.data.data?.productId
    // 如果有规格，保存SKU
    if (specList.value.length > 0 && savedProductId) {
        await axios.post('/api/shop/product/sku/save', {
            productId: savedProductId,
            specs: specList.value.map(s => ({
                specName: s.specName,
                specValues: JSON.stringify(s.values)
            })),
            skus: skuList.value
        })
    }
    ElMessage.success('保存成功')
    productDialogVisible.value = false
    fetchProducts()
}

const toggleStatus = async (row) => {
    row.status = row.status === 1 ? 0 : 1
    await axios.post('/api/shop/product/save', row)
    ElMessage.success(row.status === 1 ? '已上架' : '已下架')
}

const deleteProduct = async (id) => {
    await ElMessageBox.confirm('确认删除该商品？')
    const res = await axios.delete(`/api/shop/product/${id}`)
    if (res.data.code === 200) { ElMessage.success('已删除'); fetchProducts() }
}

// 订单
const adminOrders = ref([])
const orderLoading = ref(false)
const orderStatusFilter = ref(null)

const fetchOrders = async () => {
    orderLoading.value = true
    try {
        const res = await axios.get('/api/shop/order/admin/list', {
            params: { belongMuseum, status: orderStatusFilter.value !== null ? orderStatusFilter.value : undefined }
        })
        if (res.data.code === 200) adminOrders.value = res.data.data
    } finally {
        orderLoading.value = false
    }
}

const shipOrder = async (orderId) => {
    const res = await axios.post('/api/shop/order/ship', null, {
        params: { orderId, belongMuseum }
    })
    if (res.data.code === 200) { ElMessage.success('发货成功'); fetchOrders() }
}

// 分类
const categories = ref([])
const catDialogVisible = ref(false)
const catForm = reactive({ catId: null, name: '', sort: 0 })

const fetchCategories = async () => {
    const res = await axios.get('/api/shop/category/list')
    if (res.data.code === 200) categories.value = res.data.data
}

const openCatDialog = (c = null) => {
    if (c) Object.assign(catForm, c)
    else Object.assign(catForm, { catId: null, name: '', sort: 0 })
    catDialogVisible.value = true
}

const saveCat = async () => {
    const res = await axios.post('/api/shop/category/save', catForm)
    if (res.data.code === 200) { ElMessage.success('保存成功'); catDialogVisible.value = false; fetchCategories() }
}

const deleteCat = async (id) => {
    await ElMessageBox.confirm('确认删除该分类？')
    const res = await axios.delete(`/api/shop/category/${id}`)
    if (res.data.code === 200) { ElMessage.success('已删除'); fetchCategories() }
}

onMounted(() => {
    fetchProducts()
    fetchCategories()
    fetchOrders()
})
</script>

<style scoped>
.admin-shop {
    padding: 20px;
    font-family: 'Noto Sans SC', sans-serif;
}

.toolbar {
    display: flex;
    align-items: center;
    margin-bottom: 4px;
}

.spec-row {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    margin-bottom: 10px;
    flex-wrap: wrap;
}

.spec-values {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    align-items: center;
    flex: 1;
}
</style>