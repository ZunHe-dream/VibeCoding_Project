<template>
    <div class="checkout-page">
        <div class="page-header">
            <el-button link @click="$router.back()">← 返回</el-button>
            <h2 class="page-title">确认订单</h2>
        </div>

        <!-- 收货地址 -->
        <div class="section-card">
            <div class="section-title">收货地址</div>
            <div v-if="addresses.length === 0" class="no-addr">
                暂无地址，<el-button link type="primary" @click="addrDialogVisible = true">添加地址</el-button>
            </div>
            <div v-else>
                <div v-for="addr in addresses" :key="addr.addrId"
                    :class="['addr-item', { selected: selectedAddr?.addrId === addr.addrId }]"
                    @click="selectedAddr = addr">
                    <div class="addr-info">
                        <span class="addr-receiver">{{ addr.receiver }}</span>
                        <span class="addr-phone">{{ addr.phone }}</span>
                        <span v-if="addr.isDefault === 1" class="default-tag">默认</span>
                    </div>
                    <div class="addr-detail">{{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detail }}
                    </div>
                </div>
                <el-button link type="primary" @click="addrDialogVisible = true" style="margin-top:8px">+
                    添加新地址</el-button>
            </div>
        </div>

        <!-- 商品清单 -->
        <div class="section-card">
            <div class="section-title">商品清单</div>
            <div v-for="item in orderItems" :key="item.productId" class="order-item">
                <img :src="item.product?.coverUrl || 'https://via.placeholder.com/60x60'" class="order-img" />
                <div class="order-item-info">
                    <div class="order-item-name">{{ item.product?.name }}</div>
                    <div class="order-item-museum">{{ item.product?.belongMuseum }}</div>
                </div>
                <div class="order-item-qty">x{{ item.quantity }}</div>
                <div class="order-item-price">¥{{ (item.product?.price * item.quantity).toFixed(2) }}</div>
                
            </div>
        </div>
        <div class="section-card" v-if="parseFloat(shippingFee) > 0">
            <div class="detail-row" style="justify-content:space-between;font-size:13px">
                <span style="color:#666">运费</span>
                <span>¥{{ shippingFee }}</span>
            </div>
        </div>

        <!-- 备注 -->
        <div class="section-card">
            <div class="section-title">订单备注</div>
            <el-input v-model="remark" type="textarea" :rows="2" placeholder="选填，给卖家留言…" />
        </div>

        <!-- 底部结算 -->
        <div class="checkout-footer">
            <div class="total-info">
                共 {{ orderItems.length }} 件，含运费合计：<span class="total-price">¥{{ totalPrice }}</span>
            </div>
            <el-button type="primary" size="large" :loading="submitting" @click="submitOrder">提交订单</el-button>
        </div>

        <!-- 地址弹窗 -->
        <el-dialog v-model="addrDialogVisible" title="添加收货地址" width="480px" destroy-on-close>
            <el-form :model="addrForm" label-width="80px">
                <el-form-item label="收货人"><el-input v-model="addrForm.receiver" /></el-form-item>
                <el-form-item label="手机号"><el-input v-model="addrForm.phone" /></el-form-item>
                <el-form-item label="省份"><el-input v-model="addrForm.province" /></el-form-item>
                <el-form-item label="城市"><el-input v-model="addrForm.city" /></el-form-item>
                <el-form-item label="区/县"><el-input v-model="addrForm.district" /></el-form-item>
                <el-form-item label="详细地址"><el-input v-model="addrForm.detail" /></el-form-item>
                <el-form-item label="设为默认">
                    <el-switch v-model="addrForm.isDefault" :active-value="1" :inactive-value="0" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="addrDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="saveAddr">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const route = useRoute()
const userId = JSON.parse(sessionStorage.getItem('user') || '{}').userId
const addresses = ref([])
const selectedAddr = ref(null)
const remark = ref('')
const submitting = ref(false)
const addrDialogVisible = ref(false)
const addrForm = reactive({ receiver: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: 0 })
const orderItems = ref(JSON.parse(route.query.items || '[]'))
const cartIds = ref(JSON.parse(route.query.cartIds || '[]'))

onMounted(async () => {
    await fetchAddresses()
})

const fetchAddresses = async () => {
    const res = await axios.get('/api/shop/address/list', { params: { userId } })
    if (res.data.code === 200) {
        addresses.value = res.data.data
        selectedAddr.value = addresses.value.find(a => a.isDefault === 1) || addresses.value[0] || null
    }
}

const saveAddr = async () => {
    const res = await axios.post('/api/shop/address/save', { ...addrForm, userId })
    if (res.data.code === 200) {
        ElMessage.success('地址已保存')
        addrDialogVisible.value = false
        await fetchAddresses()
    }
}

const submitOrder = async () => {
    if (!selectedAddr.value) return ElMessage.warning('请选择收货地址')
    if (orderItems.value.length === 0) return ElMessage.warning('订单为空')
    submitting.value = true
    console.log('提交items:', JSON.stringify(orderItems.value)) // 👈 加这行
    try {
        const res = await axios.post('/api/shop/order/create', {
            userId,
            addrId: selectedAddr.value.addrId,
            remark: remark.value,
            cartIds: cartIds.value,
            shippingFee: parseFloat(shippingFee.value), // ← 加这行
            items: orderItems.value.map(i => ({ productId: i.productId, quantity: i.quantity, skuId: i.skuId }))
        })
        if (res.data.code === 200) {
            const orderId = res.data.data
            router.replace({ path: '/user/shop/pay', query: { orderId, amount: totalPrice.value } })
        } else {
            ElMessage.error(res.data.message || '下单失败')
        }
    } finally {
        submitting.value = false
    }
}

const shippingFee = computed(() => {
    // 取所有商品中最大的运费（或你的业务逻辑）
    const fees = orderItems.value.map(i => {
        if (i.product?.isFreeShipping === 1) return 0
        return parseFloat(i.product?.shippingFee || 0)
    })
    return fees.reduce((s, f) => s + f, 0).toFixed(2)
})

const totalPrice = computed(() => {
    const goods = orderItems.value.reduce((s, i) => s + i.product?.price * i.quantity, 0)
    return (goods + parseFloat(shippingFee.value)).toFixed(2)
})

</script>

<style scoped>
.checkout-page {
    max-width: 800px;
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

.section-card {
    background: #fff;
    border-radius: 10px;
    padding: 18px 20px;
    margin-bottom: 16px;
}

.section-title {
    font-size: 15px;
    font-weight: 600;
    color: #2c1a0e;
    margin-bottom: 14px;
}

.no-addr {
    font-size: 13px;
    color: #999;
}

.addr-item {
    padding: 12px;
    border: 1.5px solid #e8e8e8;
    border-radius: 8px;
    margin-bottom: 8px;
    cursor: pointer;
    transition: all 0.15s;
}

.addr-item.selected {
    border-color: #7a3b1e;
    background: #fdf5ec;
}

.addr-info {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 4px;
}

.addr-receiver {
    font-weight: 600;
    font-size: 14px;
}

.addr-phone {
    font-size: 13px;
    color: #666;
}

.default-tag {
    background: #7a3b1e;
    color: #fff;
    font-size: 10px;
    padding: 1px 6px;
    border-radius: 3px;
}

.addr-detail {
    font-size: 13px;
    color: #666;
}

.order-item {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 10px 0;
    border-bottom: 1px solid #f5f5f5;
}

.order-item:last-child {
    border-bottom: none;
}

.order-img {
    width: 60px;
    height: 60px;
    object-fit: cover;
    border-radius: 6px;
    flex-shrink: 0;
}

.order-item-info {
    flex: 1;
    min-width: 0;
}

.order-item-name {
    font-size: 14px;
    color: #2c1a0e;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.order-item-museum {
    font-size: 12px;
    color: #999;
    margin-top: 2px;
}

.order-item-qty {
    color: #999;
    font-size: 13px;
    flex-shrink: 0;
}

.order-item-price {
    font-weight: 700;
    color: #e74c3c;
    flex-shrink: 0;
    width: 80px;
    text-align: right;
}

.checkout-footer {
    background: #fff;
    border-radius: 10px;
    padding: 16px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    position: sticky;
    bottom: 20px;
    box-shadow: 0 -2px 20px rgba(0, 0, 0, 0.08);
}

.total-info {
    font-size: 14px;
    color: #666;
}

.total-price {
    font-size: 22px;
    font-weight: 700;
    color: #e74c3c;
}
</style>