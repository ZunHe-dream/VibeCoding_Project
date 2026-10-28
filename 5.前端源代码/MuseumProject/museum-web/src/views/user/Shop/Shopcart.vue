<template>
    <div class="cart-page">
        <div class="page-header">
            <el-button link @click="$router.back()">← 继续购物</el-button>
            <h2 class="page-title">购物车</h2>
        </div>

        <div v-loading="loading" class="cart-body">
            <div v-if="cartList.length === 0 && !loading" class="empty-cart">
                <div style="font-size:48px">🛒</div>
                <p>购物车空空如也</p>
                <el-button type="primary" @click="$router.push('/user/shop')">去逛逛</el-button>
            </div>

            <template v-else>
                <div class="cart-list">
                    <div v-for="item in cartList" :key="item.cartId" class="cart-item">
                        <el-checkbox v-model="item.checked" />
                        <img :src="item.product?.coverUrl || 'https://via.placeholder.com/80x80'" class="cart-img" />
                        <div class="cart-info">
                            <div class="cart-name">{{ item.product?.name }}</div>
                            <div class="cart-museum">{{ item.product?.belongMuseum }}</div>
                            <div v-if="item.skuName" style="font-size:11px;color:#c8a96e;margin-top:2px">规格：{{
                                item.skuName }}</div>
                            <div class="cart-price">¥{{ item.product?.price }}</div>
                            <div v-if="item.product?.isFreeShipping !== 1 && item.product?.shippingFee > 0"
                                style="font-size:12px; color:#999; margin-top:2px;">
                                运费：¥{{ item.product?.shippingFee }}
                            </div>
                        </div>
                        <div class="cart-qty">
                            <el-input-number v-model="item.quantity" :min="1" :max="item.product?.stock" size="small"
                                @change="updateCart(item)" />
                        </div>
                        <div class="cart-subtotal">¥{{ (item.product?.price * item.quantity).toFixed(2) }}</div>
                        <el-button link type="danger" @click="removeCart(item.cartId)">删除</el-button>
                    </div>
                </div>

                <div class="cart-footer">
                    <div class="select-all">
                        <el-checkbox v-model="allChecked">全选</el-checkbox>
                        <el-button link type="danger" @click="removeBatch" style="margin-left:16px">删除选中</el-button>
                    </div>
                    <div class="footer-right">
                        <div style="text-align:right; margin-right:12px;">
                            <div v-if="parseFloat(shippingFee) > 0"
                                style="font-size:13px; color:#999; margin-bottom:4px;">
                                运费：¥{{ shippingFee }}
                            </div>
                            <div>
                                <span class="total-label">合计：</span>
                                <span class="total-price">¥{{ totalPrice }}</span>
                            </div>
                        </div>
                        <el-button type="primary" :disabled="checkedItems.length === 0" @click="goCheckout">
                            结算 ({{ checkedItems.length }})
                        </el-button>
                    </div>
                </div>
            </template>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const userId = JSON.parse(sessionStorage.getItem('user') || '{}').userId
const loading = ref(false)
const cartList = ref([])
const checkedItems = computed(() => cartList.value.filter(i => i.checked))
const allChecked = computed({
    get: () => cartList.value.length > 0 && cartList.value.every(i => i.checked),
    set: (val) => {
        cartList.value.forEach((i) => {
            i.checked = val
        })
    }
})

const shippingFee = computed(() => {
    return checkedItems.value.reduce((s, i) => {
        if (i.product?.isFreeShipping === 1) return s
        return s + parseFloat(i.product?.shippingFee || 0)
    }, 0).toFixed(2)
})

// 修改原来的 totalPrice，加上运费
const totalPrice = computed(() =>
    (checkedItems.value.reduce((s, i) => s + i.product?.price * i.quantity, 0)
        + parseFloat(shippingFee.value)).toFixed(2)
)

const fetchCart = async () => {
    loading.value = true
    try {
        const res = await axios.get('/api/shop/cart/list', { params: { userId } })
        if (res.data?.code === 200) {
            const raw = res.data.data
            const list = Array.isArray(raw) ? raw : []
            cartList.value = list.map((i) => ({ ...i, checked: true }))
        }
    } catch (e) {
        console.error(e)
        ElMessage.error('加载购物车失败')
        cartList.value = []
    } finally {
        loading.value = false
    }
}
onMounted(fetchCart)

const updateCart = async (item) => {
    await axios.post('/api/shop/cart/update', { cartId: item.cartId, quantity: item.quantity })
}

const removeCart = async (cartId) => {
    await axios.delete(`/api/shop/cart/${cartId}`)
    cartList.value = cartList.value.filter(i => i.cartId !== cartId)
    ElMessage.success('已移除')
}

const removeBatch = async () => {
    if (checkedItems.value.length === 0) return ElMessage.warning('请先选择商品')
    await ElMessageBox.confirm('确认删除选中商品？')
    for (const item of checkedItems.value) {
        await axios.delete(`/api/shop/cart/${item.cartId}`)
    }
    cartList.value = cartList.value.filter(i => !i.checked)
    ElMessage.success('已删除')
}

const goCheckout = () => {
    const items = checkedItems.value.map(i => ({
        productId: i.productId,
        quantity: i.quantity,
        product: i.product,
        cartId: i.cartId,
        skuId: i.skuId 
    }))
    router.push({ path: '/user/shop/checkout', query: { items: JSON.stringify(items) } })
}
</script>

<style scoped>
.cart-page {
    max-width: 900px;
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
    font-size: 20px;
    font-weight: 700;
    color: #2c1a0e;
    margin: 0;
}

.empty-cart {
    text-align: center;
    padding: 80px 0;
    color: #999;
}

.cart-list {
    background: #fff;
    border-radius: 10px;
    overflow: hidden;
    margin-bottom: 16px;
}

.cart-item {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 16px 20px;
    border-bottom: 1px solid #f5f5f5;
}

.cart-item:last-child {
    border-bottom: none;
}

.cart-img {
    width: 72px;
    height: 72px;
    object-fit: cover;
    border-radius: 6px;
    flex-shrink: 0;
}

.cart-info {
    flex: 1;
    min-width: 0;
}

.cart-name {
    font-size: 14px;
    font-weight: 500;
    color: #2c1a0e;
    margin-bottom: 4px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.cart-museum {
    font-size: 12px;
    color: #999;
}

.cart-price {
    font-size: 14px;
    color: #e74c3c;
    margin-top: 4px;
}

.cart-qty {
    flex-shrink: 0;
}

.cart-subtotal {
    width: 80px;
    text-align: right;
    font-weight: 700;
    color: #e74c3c;
    flex-shrink: 0;
}

.cart-footer {
    background: #fff;
    border-radius: 10px;
    padding: 16px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.footer-right {
    display: flex;
    align-items: center;
    gap: 16px;
}

.total-label {
    font-size: 14px;
    color: #666;
}

.total-price {
    font-size: 24px;
    font-weight: 700;
    color: #e74c3c;
}
</style>