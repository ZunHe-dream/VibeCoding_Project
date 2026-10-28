<template>
    <div class="result-page">
        <div class="result-card">
            <div class="result-icon">✅</div>
            <h2 class="result-title">支付成功</h2>
            <div class="result-order">订单号：{{ orderId }}</div>
            <div class="result-actions">
                <el-button @click="$router.push('/user/shop/orders')">查看订单</el-button>
                <el-button type="primary" @click="$router.push('/user/shop')">继续购物</el-button>
            </div>
        </div>
    </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
const orderId = route.query.orderId
</script>

<style scoped>
.result-page {
    min-height: 100vh;
    background: #f5f5f5;
    display: flex;
    align-items: center;
    justify-content: center;
}

.result-card {
    background: #fff;
    border-radius: 16px;
    padding: 48px 40px;
    text-align: center;
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08);
}

.result-icon {
    font-size: 64px;
    margin-bottom: 16px;
}

.result-title {
    font-size: 24px;
    font-weight: 700;
    color: #2c1a0e;
    margin: 0 0 8px;
}

.result-order {
    font-size: 13px;
    color: #999;
    margin-bottom: 28px;
}

.result-actions {
    display: flex;
    gap: 12px;
    justify-content: center;
}
</style>