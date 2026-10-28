<template>
    <div class="product-detail-page" v-loading="loading">
        <div class="detail-nav">
            <el-button link @click="$router.back()">← 返回商城</el-button>
        </div>

        <div v-if="product" class="detail-body">
            <!-- 左侧图片区 -->
            <div class="gallery-col">
                <div class="main-img-wrap">
                    <img :src="currentImg || product.coverUrl || 'https://via.placeholder.com/400'" class="main-img" />
                </div>
                <!-- SKU图片切换 -->
                <div v-if="skuImgs.length > 0" class="thumb-list">
                    <img v-for="(img, i) in skuImgs" :key="i" :src="img"
                        :class="['thumb', { active: currentImg === img }]" @click="currentImg = img" />
                </div>
            </div>

            <!-- 右侧信息区 -->
            <div class="info-col">
                <div class="product-name">{{ product.name }}</div>
                <div class="product-museum">{{ product.belongMuseum }}</div>

                <!-- 价格 -->
                <div class="price-block">
                    <span class="price">¥{{ selectedSku ? selectedSku.price : priceRange }}</span>
                    <span v-if="product.isFreeShipping === 1" class="shipping-tag">🚚 包邮</span>
                    <span v-else-if="product.shippingFee > 0" class="shipping-tag">运费 ¥{{ product.shippingFee }}</span>
                    <span v-if="product.hasShippingInsurance === 1" class="shipping-tag">🛡️ 运费险</span>
                </div>

                <!-- 规格选择 -->
                <div v-if="product.specs?.length > 0" class="spec-section">
                    <div v-for="spec in product.specs" :key="spec.specId" class="spec-group">
                        <div class="spec-name">{{ spec.specName }}</div>
                        <div class="spec-values">
                            <span v-for="val in parseJson(spec.specValues)" :key="val"
                                :class="['spec-val', { selected: selectedSpecs[spec.specName] === val, disabled: !isSkuAvailable(spec.specName, val) }]"
                                @click="selectSpec(spec.specName, val)">{{ val }}</span>
                        </div>
                    </div>
                </div>

                <!-- 库存 -->
                <div class="stock-row">
                    <span class="stock-label">库存</span>
                    <span
                        :style="{ color: currentStock <= (selectedSku?.stockWarning || product.stockWarning) ? '#f56c6c' : '#67c23a' }">
                        {{ currentStock }} 件{{ currentStock <= (selectedSku?.stockWarning || product.stockWarning)
                            ? ' (库存紧张)' : '' }} </span>
                </div>

                <!-- 数量 -->
                <div class="qty-row">
                    <span class="stock-label">数量</span>
                    <el-input-number v-model="buyQty" :min="1" :max="currentStock" size="default" />
                </div>

                <!-- 操作按钮 -->
                <div class="action-row">
                    <el-button size="large" style="flex:1" @click="addToCart">🛒 加入购物车</el-button>
                    <el-button type="primary" size="large" style="flex:1;background:#c0392b;border-color:#c0392b"
                        @click="buyNow">立即购买</el-button>
                </div>

                <!-- 商品基础信息 -->
                <div class="meta-table">
                    <div class="meta-row"><span class="meta-key">分类</span><span>{{ product.catName || '—' }}</span>
                    </div>
                    <div class="meta-row"><span class="meta-key">类型</span><span>{{ product.type === 1 ? '虚拟商品' : '实体商品'
                            }}</span></div>
                    <div class="meta-row"><span class="meta-key">销量</span><span>{{ product.sales }} 件</span></div>
                </div>
            </div>
        </div>

        <!-- 商品详情富文本 -->
        <div v-if="product" class="desc-section">
            <div class="section-tabs">
                <span :class="['stab', { active: descTab === 'detail' }]" @click="descTab = 'detail'">商品详情</span>
                <span :class="['stab', { active: descTab === 'review' }]" @click="descTab = 'review'">
                    用户评价 ({{ reviews.length }})
                </span>
            </div>

            <!-- 详情 -->
            <div v-show="descTab === 'detail'" class="desc-content ql-editor" v-html="product.description || '暂无详情'" />

            <!-- 评价 -->
            <div v-show="descTab === 'review'" class="review-section">
                <div v-if="reviews.length === 0" class="empty-review">暂无评价</div>
                <div v-for="r in reviews" :key="r.reviewId" class="review-card">
                    <div class="review-top">
                        <el-rate :model-value="r.rating" disabled size="small" />
                        <span class="review-time">{{ r.createTime?.slice(0, 10) }}</span>
                    </div>
                    <div class="review-content">{{ r.content }}</div>
                </div>

                <!-- 评分统计 -->
                <div v-if="reviews.length > 0" class="rating-stat">
                    <div class="avg-rating">{{ avgRating }}</div>
                    <div class="avg-label">综合评分</div>
                    <el-rate :model-value="parseFloat(avgRating)" disabled allow-half />
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const productId = route.query.productId
const userId = JSON.parse(sessionStorage.getItem('user') || '{}').userId

const loading = ref(false)
const product = ref(null)
const reviews = ref([])
const selectedSpecs = reactive({})
const selectedSku = ref(null)
const buyQty = ref(1)
const currentImg = ref(null)
const descTab = ref('detail')

// 有规格时显示价格区间，无规格显示单价
const priceRange = computed(() => {
    if (!product.value?.skus?.length) return product.value?.price
    const prices = product.value.skus.map(s => s.price)
    const min = Math.min(...prices).toFixed(2)
    const max = Math.max(...prices).toFixed(2)
    return min === max ? `${min}` : `${min} ~ ${max}`
})

const currentStock = computed(() =>
    selectedSku.value ? selectedSku.value.stock : product.value?.stock || 0
)

// 所有有图片的SKU图片
const skuImgs = computed(() =>
    [...new Set((product.value?.skus || []).filter(s => s.coverUrl).map(s => s.coverUrl))]
)

const avgRating = computed(() => {
    if (!reviews.value.length) return '0.0'
    return (reviews.value.reduce((s, r) => s + r.rating, 0) / reviews.value.length).toFixed(1)
})

onMounted(async () => {
    loading.value = true
    try {
        const [p, r] = await Promise.all([
            axios.get(`/api/shop/product/detail/${productId}`),
            axios.get('/api/shop/review/list', { params: { productId } })
        ])
        if (p.data.code === 200) {
            product.value = p.data.data
            currentImg.value = product.value.coverUrl
            console.log('SKU列表:', JSON.stringify(product.value.skus)) // 👈 加这行
        }
        if (r.data.code === 200) reviews.value = r.data.data
    } finally {
        loading.value = false
    }
})

const parseJson = (str) => { try { return JSON.parse(str || '[]') } catch { return [] } }

const isSkuAvailable = (specName, val) => {
    if (!product.value?.skus?.length) return true
    const test = { ...selectedSpecs, [specName]: val }
    return product.value.skus.some(sku => {
        const parts = sku.skuName.split('-')
        return Object.values(test).every(v => parts.includes(v)) && sku.stock > 0
    })
}

const selectSpec = (specName, val) => {
    if (!isSkuAvailable(specName, val)) return
    selectedSpecs[specName] = val
    const allSpecNames = product.value.specs.map(s => s.specName)
    const allSelected = allSpecNames.every(n => selectedSpecs[n])
    if (allSelected) {
        const skuName = allSpecNames.map(n => selectedSpecs[n]).join('-')
        selectedSku.value = product.value.skus.find(s => s.skuName === skuName) || null
        if (selectedSku.value?.coverUrl) currentImg.value = selectedSku.value.coverUrl // 👈 这行
    }
}

const addToCart = async () => {
    if (!userId) return ElMessage.warning('请先登录')
    if (product.value.specs?.length > 0 && !selectedSku.value) return ElMessage.warning('请选择规格')
    const res = await axios.post('/api/shop/cart/add', {
        userId,
        productId,
        quantity: buyQty.value,
        skuId: selectedSku.value?.skuId || null  // ✨ 传skuId
    })
    if (res.data.code === 200) { ElMessage.success('已加入购物车'); cartCount.value++ }
}

const buyNow = () => {
    if (!userId) return ElMessage.warning('请先登录')
    if (product.value.specs?.length > 0 && !selectedSku.value) return ElMessage.warning('请选择规格')
    const p = { ...product.value, price: selectedSku.value?.price || product.value.price }
    console.log('selectedSku:', JSON.stringify(selectedSku.value)) // 👈 加这行
    router.push({
        path: '/user/shop/checkout',
        query: { items: JSON.stringify([{ productId, quantity: buyQty.value, product: p, skuId: selectedSku.value?.skuId }]) }
    })
}
</script>

<style scoped>
@import url('https://cdnjs.cloudflare.com/ajax/libs/quill/1.3.7/quill.snow.min.css');

.product-detail-page {
    max-width: 1100px;
    margin: 0 auto;
    padding: 20px;
    font-family: 'Noto Sans SC', sans-serif;
}

.detail-nav {
    margin-bottom: 16px;
}

.detail-body {
    display: flex;
    gap: 40px;
    background: #fff;
    border-radius: 12px;
    padding: 28px;
    margin-bottom: 20px;
}

/* 图片区 */
.gallery-col {
    width: 380px;
    flex-shrink: 0;
}

.main-img-wrap {
    width: 380px;
    height: 380px;
    border-radius: 10px;
    overflow: hidden;
    background: #f8f4ee;
}

.main-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.thumb-list {
    display: flex;
    gap: 8px;
    margin-top: 12px;
    flex-wrap: wrap;
}

.thumb {
    width: 56px;
    height: 56px;
    object-fit: cover;
    border-radius: 6px;
    border: 2px solid #e8e8e8;
    cursor: pointer;
}

.thumb.active {
    border-color: #c0392b;
}

/* 信息区 */
.info-col {
    flex: 1;
    min-width: 0;
}

.product-name {
    font-size: 22px;
    font-weight: 700;
    color: #1a1a1a;
    margin-bottom: 6px;
    line-height: 1.4;
}

.product-museum {
    font-size: 13px;
    color: #999;
    margin-bottom: 16px;
}

.price-block {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 20px;
    flex-wrap: wrap;
}

.price {
    font-size: 32px;
    font-weight: 700;
    color: #c0392b;
}

.shipping-tag {
    font-size: 12px;
    color: #67c23a;
    background: #f0fdf4;
    padding: 2px 8px;
    border-radius: 4px;
}

.spec-section {
    margin-bottom: 20px;
}

.spec-group {
    margin-bottom: 14px;
}

.spec-name {
    font-size: 13px;
    color: #666;
    margin-bottom: 8px;
}

.spec-values {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.spec-val {
    padding: 6px 16px;
    border: 1.5px solid #e8e8e8;
    border-radius: 6px;
    font-size: 13px;
    cursor: pointer;
    transition: all 0.15s;
    user-select: none;
}

.spec-val:hover {
    border-color: #c0392b;
    color: #c0392b;
}

.spec-val.selected {
    border-color: #c0392b;
    background: #c0392b;
    color: #fff;
}

.spec-val.disabled {
    opacity: 0.35;
    cursor: not-allowed;
}

.spec-val.disabled:hover {
    border-color: #e8e8e8;
    color: inherit;
}

.stock-row,
.qty-row {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 14px;
}

.stock-label {
    font-size: 13px;
    color: #666;
    width: 36px;
    flex-shrink: 0;
}

.action-row {
    display: flex;
    gap: 12px;
    margin-bottom: 20px;
}

.meta-table {
    border-top: 1px solid #f0f0f0;
    padding-top: 16px;
}

.meta-row {
    display: flex;
    padding: 6px 0;
    font-size: 13px;
    border-bottom: 1px solid #f8f8f8;
}

.meta-key {
    color: #999;
    width: 60px;
    flex-shrink: 0;
}

/* 详情/评价 */
.desc-section {
    background: #fff;
    border-radius: 12px;
    padding: 24px;
}

.section-tabs {
    display: flex;
    gap: 0;
    border-bottom: 2px solid #f0f0f0;
    margin-bottom: 20px;
}

.stab {
    padding: 10px 24px;
    font-size: 15px;
    cursor: pointer;
    color: #666;
    border-bottom: 2px solid transparent;
    margin-bottom: -2px;
    transition: all 0.15s;
}

.stab.active {
    color: #c0392b;
    border-bottom-color: #c0392b;
    font-weight: 600;
}

.desc-content {
    font-size: 14px;
    line-height: 1.9;
    color: #333;
}

:deep(.desc-content img) {
    max-width: 100%;
    border-radius: 6px;
}

:deep(.ql-editor) {
    padding: 0;
    border: none;
}

.empty-review {
    text-align: center;
    padding: 40px;
    color: #999;
}

.review-card {
    padding: 16px 0;
    border-bottom: 1px solid #f5f5f5;
}

.review-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
}

.review-time {
    font-size: 12px;
    color: #bbb;
}

.review-content {
    font-size: 14px;
    color: #333;
    line-height: 1.7;
}

.rating-stat {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 24px;
    background: #faf7f3;
    border-radius: 10px;
    margin-top: 16px;
}

.avg-rating {
    font-size: 48px;
    font-weight: 700;
    color: #c0392b;
    line-height: 1;
}

.avg-label {
    font-size: 12px;
    color: #999;
    margin: 4px 0 8px;
}
</style>