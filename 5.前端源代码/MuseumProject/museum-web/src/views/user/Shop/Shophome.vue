<template>
    <div class="shop-page">
        <!-- 顶部搜索栏 -->
        <div class="shop-header">
            <div class="header-inner">
                <div class="shop-brand">
                    <span class="brand-icon">🏛️</span>
                    <span class="brand-name">博物馆周边商城</span>
                </div>
                <div class="search-bar">
                    <input v-model="keyword" placeholder="搜索文创好物…" class="search-input" @keyup.enter="fetchProducts" />
                    <button class="search-btn" @click="fetchProducts">搜索</button>
                </div>
                <div class="header-actions">
                    <div class="cart-btn" @click="$router.push('/user/shop/cart')">
                        <span>🛒</span>
                        <span v-if="cartCount > 0" class="cart-badge">{{ cartCount }}</span>
                    </div>
                    <div class="order-btn" @click="$router.push('/user/shop/orders')">我的订单</div>
                </div>
            </div>
        </div>

        <div class="shop-body">
            <!-- 左侧分类 -->
            <div class="category-panel">
                <div class="cat-title">商品分类</div>
                <div :class="['cat-item', { active: !selectedCat }]" @click="selectedCat = null; fetchProducts()">全部
                </div>
                <div v-for="cat in categories" :key="cat.catId"
                    :class="['cat-item', { active: selectedCat === cat.catId }]"
                    @click="selectedCat = cat.catId; fetchProducts()">{{ cat.name }}</div>
            </div>

            <!-- 右侧商品区 -->
            <div class="product-area">
                <!-- 筛选栏 -->
                <div class="filter-bar">
                    <span class="filter-label">排序：</span>
                    <span :class="['sort-opt', { active: sort === 'sales' }]"
                        @click="sort = 'sales'; fetchProducts()">销量优先</span>
                    <span :class="['sort-opt', { active: sort === 'price_asc' }]"
                        @click="sort = 'price_asc'; fetchProducts()">价格升序</span>
                    <span :class="['sort-opt', { active: sort === 'price_desc' }]"
                        @click="sort = 'price_desc'; fetchProducts()">价格降序</span>
                    <span class="filter-label" style="margin-left:20px">类型：</span>
                    <span :class="['sort-opt', { active: typeFilter === null }]"
                        @click="typeFilter = null; fetchProducts()">全部</span>
                    <span :class="['sort-opt', { active: typeFilter === 0 }]"
                        @click="typeFilter = 0; fetchProducts()">实体</span>
                    <span :class="['sort-opt', { active: typeFilter === 1 }]"
                        @click="typeFilter = 1; fetchProducts()">虚拟</span>
                </div>

                <!-- 商品列表 -->
                <div v-loading="loading" class="product-grid">
                    <div v-if="products.length === 0 && !loading" class="empty">暂无商品</div>
                    <div v-for="p in products" :key="p.productId" class="product-card" @click="openDetail(p)">
                        <div class="product-img-wrap">
                            <img :src="p.coverUrl || 'https://via.placeholder.com/300x200?text=暂无图片'"
                                class="product-img" />
                            <span v-if="p.type === 1" class="virtual-tag">虚拟</span>
                            <span v-if="p.lowStock" class="low-stock-tag">库存紧张</span>
                        </div>
                        <div class="product-info">
                            <div class="product-name">{{ p.name }}</div>
                            <div class="product-museum">{{ p.belongMuseum }}</div>
                            <div class="product-bottom">
                                <span class="product-price">
                                    {{ p.hasMultiSku ? '¥' + p.price + ' 起' : '¥' + p.price }}
                                </span>
                                <span class="product-sales">已售 {{ p.sales }}</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- 商品详情弹窗 -->
        <el-dialog v-model="detailVisible" width="640px" destroy-on-close :title="currentProduct?.name">
            <div v-if="currentProduct" class="detail-wrap">
                <div class="detail-top">
                    <img :src="currentProduct.coverUrl || 'https://via.placeholder.com/300x200?text=暂无图片'"
                        class="detail-img" />
                    <div class="detail-right">
                        <div class="detail-price">¥{{ currentProduct.price }}</div>
                        <!-- 规格选择 -->
                        <div v-if="currentProduct.specs?.length > 0" class="spec-selector">
                            <div v-for="spec in currentProduct.specs" :key="spec.specId" class="spec-group">
                                <div class="spec-group-name">{{ spec.specName }}</div>
                                <div class="spec-values">
                                    <span v-for="val in parseSpecValues(spec.specValues)" :key="val"
                                        :class="['spec-val', { selected: isSpecSelected(spec.specName, val) }]"
                                        @click="selectSpec(spec.specName, val)">{{ val }}</span>
                                </div>
                            </div>
                        </div>

                        <!-- 价格显示：有SKU用SKU价格，没有用商品价格 -->
                        <div class="detail-price">
                            ¥{{ selectedSku ? selectedSku.price : currentProduct.price }}
                        </div>
                        <div v-if="currentProduct.isFreeShipping === 1" class="shipping-tag">🚚 包邮</div>
                        <div v-if="currentProduct.hasShippingInsurance === 1" class="shipping-tag">🛡️ 运费险</div>
                        <div class="detail-meta">所属博物馆：{{ currentProduct.belongMuseum }}</div>
                        <div class="detail-meta">分类：{{ currentProduct.catName || '—' }}</div>
                        <div class="detail-meta">库存：
                            <span :style="{ color: currentProduct.lowStock ? '#f56c6c' : '#67c23a' }">
                                {{ currentProduct.stock }} 件{{ currentProduct.lowStock ? '（库存紧张）' : '' }}
                            </span>
                        </div>
                        <div class="detail-meta">类型：{{ currentProduct.type === 1 ? '虚拟商品' : '实体商品' }}</div>
                        <div class="qty-row">
                            <span class="qty-label">数量：</span>
                            <el-input-number v-model="buyQty" :min="1" :max="currentProduct.stock" size="small" />
                        </div>
                        <div class="detail-actions">
                            <el-button @click="addToCart(currentProduct)">加入购物车</el-button>
                            <el-button type="primary" @click="buyNow(currentProduct)">立即购买</el-button>
                        </div>
                    </div>
                </div>
                <el-divider>商品描述</el-divider>
                <div class="detail-desc">{{ currentProduct.description || '暂无描述' }}</div>

                <!-- 评价 -->
                <el-divider>用户评价</el-divider>
                <div v-if="reviews.length === 0" style="color:#999;font-size:13px">暂无评价</div>
                <div v-for="r in reviews" :key="r.reviewId" class="review-item">
                    <el-rate :model-value="r.rating" disabled size="small" />
                    <div class="review-content">{{ r.content }}</div>
                    <div class="review-time">{{ r.createTime?.slice(0, 10) }}</div>
                </div>
            </div>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const userId = JSON.parse(sessionStorage.getItem('user') || '{}').userId

const keyword = ref('')
const selectedCat = ref(null)
const sort = ref('sales')
const typeFilter = ref(null)
const loading = ref(false)
const products = ref([])
const categories = ref([])
const cartCount = ref(0)
const selectedSku = ref(null)
const detailVisible = ref(false)
const currentProduct = ref(null)
const buyQty = ref(1)
const reviews = ref([])


const selectedSpecs = ref({}) // { 款式: '甲款', 颜色: '红色' }

const parseSpecValues = (str) => {
    try { return JSON.parse(str || '[]') } catch { return [] }
}

const isSpecSelected = (specName, val) => selectedSpecs.value[specName] === val

const selectSpec = (specName, val) => {
    selectedSpecs.value[specName] = val
    // 匹配SKU
    if (currentProduct.value.skus?.length > 0) {
        const skuName = Object.values(selectedSpecs.value).join('-')
        selectedSku.value = currentProduct.value.skus.find(s => s.skuName === skuName) || null
    }
}

onMounted(async () => {
    await fetchCategories()
    await fetchProducts()
    await fetchCartCount()
})

// 避免详情弹窗未关就跳转时，Teleport 遮罩残留在 body 上挡住主内容区
onBeforeRouteLeave(() => {
    detailVisible.value = false
    // 新增：强制清理残留遮罩
    setTimeout(() => {
        document.querySelectorAll('.el-overlay').forEach(el => el.remove())
        document.body.style.overflow = ''
    }, 50)
})

const fetchCategories = async () => {
    const res = await axios.get('/api/shop/category/list')
    if (res.data.code === 200) categories.value = res.data.data
}

const fetchProducts = async () => {
    loading.value = true
    try {
        const res = await axios.get('/api/shop/product/list', {
            params: {
                keyword: keyword.value || undefined,
                catId: selectedCat.value || undefined,
                type: typeFilter.value !== null ? typeFilter.value : undefined,
                sort: sort.value
            }
        })
        if (res.data.code === 200) products.value = res.data.data
    } finally {
        loading.value = false
    }
}

const fetchCartCount = async () => {
    if (!userId) return
    const res = await axios.get('/api/shop/cart/list', { params: { userId } })
    if (res.data.code === 200) cartCount.value = res.data.data.length
}

const openDetail = (p) => {
    router.push({ path: '/user/shop/detail', query: { productId: p.productId } })
}

// const openDetail = async (p) => {
//     currentProduct.value = p
//     buyQty.value = 1
//     selectedSku.value = null
//     detailVisible.value = true
//     // 加载规格和SKU
//     const res = await axios.get(`/api/shop/product/detail/${p.productId}`)
//     if (res.data.code === 200) {
//         currentProduct.value = res.data.data
//     }
//     const r2 = await axios.get('/api/shop/review/list', { params: { productId: p.productId } })
//     if (r2.data.code === 200) reviews.value = r2.data.data
// }

const addToCart = async (p) => {
    if (!userId) return ElMessage.warning('请先登录')
    if (p.skus?.length > 0 && !selectedSku.value) return ElMessage.warning('请先选择规格')
    const res = await axios.post('/api/shop/cart/add', {
        userId,
        productId: p.productId,
        quantity: buyQty.value,
        skuId: selectedSku.value?.skuId || null
    })
    if (res.data.code === 200) { ElMessage.success('已加入购物车'); cartCount.value++ }
}

const buyNow = (p) => {
    if (!userId) return ElMessage.warning('请先登录')
    router.push({
        path: '/user/shop/checkout',
        query: {
            items: JSON.stringify([{ productId: p.productId, quantity: buyQty.value, product: p }])
        }
    })
}
</script>

<style scoped>
.shop-page {
    min-height: 100vh;
    background: #f5f5f5;
    font-family: 'Noto Sans SC', sans-serif;
}

.shop-header {
    background: #2c1a0e;
    padding: 12px 0;
    position: sticky;
    top: 0;
    z-index: 100;
}

.header-inner {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 20px;
    display: flex;
    align-items: center;
    gap: 20px;
}

.shop-brand {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
}

.brand-icon {
    font-size: 22px;
}

.brand-name {
    color: #f0e6d0;
    font-size: 16px;
    font-weight: 700;
    white-space: nowrap;
}

.search-bar {
    flex: 1;
    display: flex;
}

.search-input {
    flex: 1;
    padding: 8px 14px;
    border: none;
    border-radius: 6px 0 0 6px;
    font-size: 14px;
    outline: none;
}

.search-btn {
    background: #c8a96e;
    color: #2c1a0e;
    border: none;
    padding: 8px 18px;
    border-radius: 0 6px 6px 0;
    cursor: pointer;
    font-weight: 600;
}

.search-btn:hover {
    background: #d4b97e;
}

.header-actions {
    display: flex;
    align-items: center;
    gap: 14px;
    flex-shrink: 0;
}

.cart-btn {
    position: relative;
    cursor: pointer;
    font-size: 22px;
}

.cart-badge {
    position: absolute;
    top: -6px;
    right: -8px;
    background: #e74c3c;
    color: #fff;
    border-radius: 10px;
    font-size: 10px;
    padding: 1px 5px;
    min-width: 16px;
    text-align: center;
}

.order-btn {
    color: #f0e6d0;
    font-size: 13px;
    cursor: pointer;
    white-space: nowrap;
}

.order-btn:hover {
    color: #c8a96e;
}

.shop-body {
    max-width: 1200px;
    margin: 20px auto;
    padding: 0 20px;
    display: flex;
    gap: 20px;
}

/* 分类 */
.category-panel {
    width: 140px;
    flex-shrink: 0;
    background: #fff;
    border-radius: 10px;
    padding: 12px 0;
    height: fit-content;
    position: sticky;
    top: 70px;
}

.cat-title {
    font-size: 12px;
    color: #999;
    padding: 4px 16px 10px;
    letter-spacing: 1px;
}

.cat-item {
    padding: 10px 16px;
    font-size: 13px;
    color: #333;
    cursor: pointer;
    transition: all 0.15s;
    border-left: 3px solid transparent;
}

.cat-item:hover {
    background: #faf7f3;
    color: #7a3b1e;
}

.cat-item.active {
    background: #fdf5ec;
    color: #7a3b1e;
    border-left-color: #7a3b1e;
    font-weight: 600;
}

/* 商品区 */
.product-area {
    flex: 1;
    min-width: 0;
}

.filter-bar {
    background: #fff;
    border-radius: 10px;
    padding: 12px 16px;
    margin-bottom: 16px;
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 4px;
}

.filter-label {
    font-size: 12px;
    color: #999;
}

.sort-opt {
    font-size: 13px;
    color: #666;
    padding: 4px 10px;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.15s;
}

.sort-opt:hover {
    color: #7a3b1e;
}

.sort-opt.active {
    background: #7a3b1e;
    color: #fff;
}

.product-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 16px;
}

.empty {
    grid-column: 1/-1;
    text-align: center;
    padding: 60px;
    color: #999;
}

.product-card {
    background: #fff;
    border-radius: 10px;
    overflow: hidden;
    cursor: pointer;
    transition: transform 0.2s, box-shadow 0.2s;
}

.product-card:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}

.product-img-wrap {
    position: relative;
    height: 160px;
    background: #f8f4ee;
}

.product-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.virtual-tag {
    position: absolute;
    top: 8px;
    left: 8px;
    background: #3b82f6;
    color: #fff;
    font-size: 10px;
    padding: 2px 6px;
    border-radius: 4px;
}

.low-stock-tag {
    position: absolute;
    top: 8px;
    right: 8px;
    background: #f56c6c;
    color: #fff;
    font-size: 10px;
    padding: 2px 6px;
    border-radius: 4px;
}

.product-info {
    padding: 12px;
}

.product-name {
    font-size: 14px;
    font-weight: 500;
    color: #2c1a0e;
    margin-bottom: 4px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.product-museum {
    font-size: 11px;
    color: #999;
    margin-bottom: 8px;
}

.product-bottom {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.product-price {
    font-size: 18px;
    font-weight: 700;
    color: #e74c3c;
}

.product-sales {
    font-size: 11px;
    color: #bbb;
}

/* 详情弹窗 */
.detail-top {
    display: flex;
    gap: 20px;
}

.detail-img {
    width: 220px;
    height: 180px;
    object-fit: cover;
    border-radius: 8px;
    flex-shrink: 0;
}

.detail-right {
    flex: 1;
}

.detail-price {
    font-size: 28px;
    font-weight: 700;
    color: #e74c3c;
    margin-bottom: 12px;
}

.detail-meta {
    font-size: 13px;
    color: #666;
    margin-bottom: 6px;
}

.qty-row {
    display: flex;
    align-items: center;
    gap: 10px;
    margin: 14px 0;
}

.qty-label {
    font-size: 13px;
    color: #666;
}

.detail-actions {
    display: flex;
    gap: 10px;
    margin-top: 16px;
}

.detail-desc {
    font-size: 14px;
    color: #444;
    line-height: 1.8;
}

.review-item {
    padding: 10px 0;
    border-bottom: 1px solid #f0f0f0;
}

.review-content {
    font-size: 13px;
    color: #333;
    margin: 6px 0;
}

.review-time {
    font-size: 11px;
    color: #bbb;
}
.spec-selector {
    margin: 10px 0;
}

.spec-group {
    margin-bottom: 10px;
}

.spec-group-name {
    font-size: 12px;
    color: #999;
    margin-bottom: 6px;
}

.spec-values {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
}

.spec-val {
    padding: 4px 12px;
    border: 1.5px solid #e8e8e8;
    border-radius: 4px;
    font-size: 13px;
    cursor: pointer;
    transition: all 0.15s;
}

.spec-val:hover {
    border-color: #7a3b1e;
    color: #7a3b1e;
}

.spec-val.selected {
    border-color: #7a3b1e;
    background: #7a3b1e;
    color: #fff;
}

.shipping-tag {
    display: inline-block;
    font-size: 12px;
    color: #67c23a;
    margin-right: 8px;
    margin-top: 4px;
}
</style>