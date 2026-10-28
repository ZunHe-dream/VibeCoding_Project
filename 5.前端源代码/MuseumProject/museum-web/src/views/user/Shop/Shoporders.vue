<template>
    <div class="orders-page">
        <div class="page-header">
            <el-button link @click="$router.push('/user/shop')">← 继续购物</el-button>
            <h2 class="page-title">我的订单</h2>
        </div>

        <el-tabs v-model="activeTab" @tab-change="fetchOrders">
            <el-tab-pane label="全部" name="" />
            <el-tab-pane label="待付款" name="0" />
            <el-tab-pane label="待发货" name="1" />
            <el-tab-pane label="待收货" name="2" />
            <el-tab-pane label="已完成" name="3" />
            <el-tab-pane label="已取消" name="4" />
        </el-tabs>

        <div v-loading="loading">
            <div v-if="orders.length === 0 && !loading" class="empty">暂无订单</div>
            <div v-for="order in orders" :key="order.orderId" class="order-card">
                <!-- 订单头：显示订单号和整体状态 -->
                <div class="order-head">
                    <span class="order-id">订单号：{{ order.orderId }}</span>
                    <span :class="['order-status', statusClass(order.status)]">{{ statusText(order.status) }}</span>
                </div>

                <!-- 按博物馆分组 -->
                <div v-for="(group, museum) in groupItemsByMuseum(order.items)" :key="museum" class="museum-group">
                    <div class="museum-group-header">
                        <span class="museum-group-name">🏛️ {{ museum }}</span>
                        <span class="museum-group-subtotal">
                            小计：¥{{ calcGroupTotal(group) }}
                        </span>
                    </div>
                    <div v-for="item in group" :key="item.itemId" class="order-item">
                        <img :src="item.coverUrl || 'https://placehold.co/60x60'" class="item-img" />
                        <div class="item-info">
                            <div class="item-name">{{ item.productName }}</div>
                            <div class="item-museum">{{ item.belongMuseum }}</div>
                            <div v-if="item.skuName" style="font-size:11px;color:#c8a96e">规格：{{ item.skuName }}</div>
                        </div>
                        <div class="item-right">
                            <div class="item-price">¥{{ item.price }}</div>
                            <div class="item-qty">x{{ item.quantity }}</div>
                            <el-tag :type="['warning', 'primary', 'success', 'info'][item.itemStatus]" size="small"
                                style="margin-top:4px">
                                {{ ['待发货', '已发货', '已完成', '已取消'][item.itemStatus] ?? '—' }}
                            </el-tag>
                        </div>
                    </div>
                </div>

                <!-- 订单底部 -->
                <div class="order-foot">
                    <div class="order-total">
                        共 {{ order.items?.length }} 件，合计 <span class="total-price">¥{{ order.totalPrice }}</span>
                    </div>
                    <div class="order-actions">
                        <el-button v-if="order.status === 0" type="primary" size="small"
                            @click="goPay(order)">去付款</el-button>
                        <el-button v-if="order.status === 0" size="small"
                            @click="cancelOrder(order.orderId)">取消订单</el-button>
                        <el-button v-if="order.status === 1 && order.items?.some(i => i.itemStatus === 1)"
                            type="success" size="small" @click="finishOrder(order.orderId)">确认收货</el-button>
                        <el-button v-if="order.status === 3 && !order.reviewed" size="small"
                            @click="openReview(order)">评价</el-button>
                        <el-button v-if="order.status === 1 || order.status === 2" size="small" type="warning"
                            @click="openRefund(order)">申请退款</el-button>
                    </div>
                </div>
            </div>
        </div>

        <!-- 评价弹窗 -->
        <el-dialog v-model="reviewVisible" title="发表评价" width="480px" destroy-on-close>
            <div v-for="item in reviewOrder?.items" :key="item.itemId" class="review-item-row">
                <div class="review-item-name">{{ item.productName }}</div>
                <el-rate v-model="reviewForm[item.productId].rating" />
                <el-input v-model="reviewForm[item.productId].content" type="textarea" :rows="2" placeholder="分享你的使用感受…"
                    style="margin-top:6px" />
            </div>
            <template #footer>
                <el-button @click="reviewVisible = false">取消</el-button>
                <el-button type="primary" @click="submitReview">提交评价</el-button>
            </template>
        </el-dialog>
        <el-dialog v-model="refundVisible" title="申请退款" width="420px" destroy-on-close>
            <div style="margin-bottom:12px;font-size:13px;color:#666">
                订单号：{{ refundOrder?.orderId }}
            </div>
            <el-form label-width="80px">
                <el-form-item label="退款原因">
                    <el-input v-model="refundReason" type="textarea" :rows="3" placeholder="请填写退款原因，方便商家处理…" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="refundVisible = false">取消</el-button>
                <el-button type="warning" :loading="refundLoading" @click="submitRefund">提交申请</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const userId = JSON.parse(sessionStorage.getItem('user') || '{}').userId
const activeTab = ref('')
const loading = ref(false)
const orders = ref([])
const reviewVisible = ref(false)
const reviewOrder = ref(null)
const reviewForm = reactive({})
const refundVisible = ref(false)
const refundOrder = ref(null)
const refundReason = ref('')
const refundLoading = ref(false)

const openRefund = (order) => {
    refundOrder.value = order
    refundReason.value = ''
    refundVisible.value = true
}

const submitRefund = async () => {
    if (!refundReason.value.trim()) return ElMessage.warning('请填写退款原因')
    refundLoading.value = true
    try {
        const res = await axios.post('/api/shop/order/refund/apply', {
            orderId: refundOrder.value.orderId,
            reason: refundReason.value
        })
        if (res.data.code === 200) {
            ElMessage.success('退款申请已提交，等待商家处理')
            refundVisible.value = false
            fetchOrders()
        } else {
            ElMessage.error(res.data.message || '提交失败')
        }
    } finally {
        refundLoading.value = false
    }
}


const fetchOrders = async () => {
    loading.value = true
    try {
        const res = await axios.get('/api/shop/order/list', {
            params: { userId, status: activeTab.value !== '' ? activeTab.value : undefined }
        })
        if (res.data.code === 200) orders.value = res.data.data
    } finally {
        loading.value = false
    }
}
onMounted(fetchOrders)

const statusText = (s) => ['待付款', '待发货', '待收货', '已完成', '已取消', '退款中', '已退款'][s] ?? '未知'
const statusClass = (s) => ['status-pending', 'status-ship', 'status-receive', 'status-done', 'status-cancel', 'status-refunding', 'status-refunded'][s] ?? ''

const goPay = (order) => {
    router.push({ path: '/user/shop/pay', query: { orderId: order.orderId, amount: order.totalPrice } })
}

const cancelOrder = async (orderId) => {
    await ElMessageBox.confirm('确认取消订单？取消后库存将自动恢复')
    const res = await axios.post('/api/shop/order/cancel', null, { params: { orderId } })
    if (res.data.code === 200) { ElMessage.success('已取消'); fetchOrders() }
}

const finishOrder = async (orderId) => {
    await ElMessageBox.confirm('确认已收到商品？')
    const res = await axios.post('/api/shop/order/finish', null, { params: { orderId } })
    if (res.data.code === 200) { ElMessage.success('确认收货成功'); fetchOrders() }
}

const openReview = (order) => {
    reviewOrder.value = order
    order.items.forEach(item => {
        reviewForm[item.productId] = { rating: 5, content: '' }
    })
    reviewVisible.value = true
}

const submitReview = async () => {
    const order = reviewOrder.value
    for (const item of order.items) {
        await axios.post('/api/shop/review/save', {
            orderId: order.orderId,
            productId: item.productId,
            userId,
            rating: reviewForm[item.productId].rating,
            content: reviewForm[item.productId].content
        })
    }
    ElMessage.success('评价成功')
    reviewVisible.value = false
    fetchOrders()
}

const groupItemsByMuseum = (items) => {
    if (!items) return {}
    const map = {}
    items.forEach(item => {
        const museum = item.belongMuseum || '未知博物馆'
        if (!map[museum]) map[museum] = []
        map[museum].push(item)
    })
    return map
}

const calcGroupTotal = (items) => {
    return items.reduce((s, i) => s + i.price * i.quantity, 0).toFixed(2)
}
</script>

<style scoped>
.orders-page {
    max-width: 860px;
    margin: 0 auto;
    padding: 24px 20px;
    font-family: 'Noto Sans SC', sans-serif;
}

.page-header {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 16px;
}

.page-title {
    font-size: 20px;
    font-weight: 700;
    color: #2c1a0e;
    margin: 0;
}

.empty {
    text-align: center;
    padding: 60px;
    color: #999;
}

.order-card {
    background: #fff;
    border-radius: 10px;
    overflow: hidden;
    margin-bottom: 16px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.order-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    background: #faf7f3;
    border-bottom: 1px solid #f0e8dc;
}

.order-id {
    font-size: 12px;
    color: #999;
}

.order-status {
    font-size: 13px;
    font-weight: 600;
}

.status-pending {
    color: #e6a23c;
}

.status-ship {
    color: #409eff;
}

.status-receive {
    color: #7a3b1e;
}

.status-done {
    color: #67c23a;
}

.status-cancel {
    color: #999;
}

.order-item {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 14px 16px;
    border-bottom: 1px solid #f8f8f8;
}

.item-img {
    width: 60px;
    height: 60px;
    object-fit: cover;
    border-radius: 6px;
    flex-shrink: 0;
}

.item-info {
    flex: 1;
    min-width: 0;
}

.item-name {
    font-size: 14px;
    color: #2c1a0e;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.item-museum {
    font-size: 12px;
    color: #999;
    margin-top: 2px;
}

.item-right {
    text-align: right;
    flex-shrink: 0;
}

.item-price {
    font-size: 14px;
    color: #e74c3c;
    font-weight: 600;
}

.item-qty {
    font-size: 12px;
    color: #999;
    margin-top: 2px;
}

.order-foot {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
}

.order-total {
    font-size: 13px;
    color: #666;
}

.total-price {
    font-size: 18px;
    font-weight: 700;
    color: #e74c3c;
}

.order-actions {
    display: flex;
    gap: 8px;
}

.review-item-row {
    margin-bottom: 16px;
    padding-bottom: 16px;
    border-bottom: 1px solid #f0f0f0;
}

.review-item-name {
    font-size: 14px;
    font-weight: 500;
    color: #2c1a0e;
    margin-bottom: 8px;
}
.status-refunding {
    color: #e6a23c;
}

.status-refunded {
    color: #909399;
}
.museum-group {
    border: 1px solid #f0e8dc;
    border-radius: 8px;
    margin: 8px 0;
    overflow: hidden;
}

.museum-group-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 16px;
    background: #faf7f3;
    border-bottom: 1px solid #f0e8dc;
}

.museum-group-name {
    font-size: 13px;
    font-weight: 600;
    color: #7a3b1e;
}

.museum-group-subtotal {
    font-size: 13px;
    color: #e74c3c;
    font-weight: 600;
}
</style>