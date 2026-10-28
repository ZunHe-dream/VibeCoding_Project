<template>
    <div class="pay-page">
        <div class="pay-card">
            <div class="pay-logo">🏛️</div>
            <h2 class="pay-title">博物馆安全支付</h2>
            <div class="pay-amount">¥{{ amount }}</div>
            <div class="pay-order">订单号：{{ orderId }}</div>

            <div class="pay-input-wrap">
                <div class="pay-label">请输入6位支付密码</div>
                <div class="pin-boxes">
                    <div v-for="i in 6" :key="i"
                        :class="['pin-box', { filled: pin.length >= i, active: pin.length === i - 1 }]">
                        {{ pin.length >= i ? '●' : '' }}
                    </div>
                </div>
                <!-- 隐藏的真实输入 -->
                <input ref="pinInput" v-model="pin" maxlength="6" class="hidden-input" @input="onPinInput"
                    inputmode="numeric" />
            </div>

            <div class="numpad">
                <div v-for="n in [1, 2, 3, 4, 5, 6, 7, 8, 9, '', 0, '⌫']" :key="n" :class="['num-key', { empty: n === '' }]"
                    @click="pressKey(n)">{{ n }}</div>
            </div>

            <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>

            <el-button link @click="$router.push('/user/shop/orders')" style="margin-top:16px">
                取消支付
            </el-button>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const orderId = route.query.orderId
const amount = route.query.amount
const pin = ref('')
const errorMsg = ref('')
const paying = ref(false)
const pinInput = ref(null)

onMounted(() => pinInput.value?.focus())

const pressKey = (key) => {
    if (key === '⌫') {
        pin.value = pin.value.slice(0, -1)
    } else if (key !== '' && pin.value.length < 6) {
        pin.value += String(key)
        if (pin.value.length === 6) triggerPay()
    }
}

const onPinInput = () => {
    pin.value = pin.value.replace(/\D/g, '').slice(0, 6)
    if (pin.value.length === 6) triggerPay()
}

const triggerPay = async () => {
    if (paying.value) return
    paying.value = true
    errorMsg.value = ''

    // 模拟支付验证（任意6位密码即可）
    await new Promise(r => setTimeout(r, 800))

    try {
        const res = await axios.post('/api/shop/order/pay', null, { params: { orderId } })
        if (res.data.code === 200) {
            router.replace({ path: '/user/shop/pay-result', query: { orderId, success: '1' } })
        } else {
            errorMsg.value = res.data.message || '支付失败'
            pin.value = ''
            paying.value = false
        }
    } catch {
        errorMsg.value = '网络异常，请重试'
        pin.value = ''
        paying.value = false
    }
}
</script>

<style scoped>
.pay-page {
    min-height: 100vh;
    background: linear-gradient(135deg, #2c1a0e, #5a3520);
    display: flex;
    align-items: center;
    justify-content: center;
    font-family: 'Noto Sans SC', sans-serif;
}

.pay-card {
    background: #fff;
    border-radius: 20px;
    padding: 40px 36px;
    width: 360px;
    text-align: center;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.pay-logo {
    font-size: 40px;
    margin-bottom: 8px;
}

.pay-title {
    font-size: 18px;
    font-weight: 700;
    color: #2c1a0e;
    margin: 0 0 20px;
}

.pay-amount {
    font-size: 40px;
    font-weight: 700;
    color: #e74c3c;
    margin-bottom: 6px;
}

.pay-order {
    font-size: 12px;
    color: #bbb;
    margin-bottom: 28px;
}

.pay-label {
    font-size: 13px;
    color: #666;
    margin-bottom: 14px;
}

.pin-boxes {
    display: flex;
    justify-content: center;
    gap: 10px;
    margin-bottom: 20px;
    cursor: text;
}

.pin-box {
    width: 42px;
    height: 52px;
    border: 2px solid #e8e8e8;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 22px;
    color: #2c1a0e;
    transition: border-color 0.15s;
}

.pin-box.filled {
    border-color: #7a3b1e;
}

.pin-box.active {
    border-color: #c8a96e;
    box-shadow: 0 0 0 3px rgba(200, 169, 110, 0.2);
}

.hidden-input {
    position: absolute;
    opacity: 0;
    width: 0;
    height: 0;
}

.numpad {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 10px;
    margin-bottom: 16px;
}

.num-key {
    height: 52px;
    border-radius: 10px;
    background: #f8f4ee;
    font-size: 20px;
    font-weight: 600;
    color: #2c1a0e;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    user-select: none;
    transition: background 0.15s;
}

.num-key:hover {
    background: #f0e8dc;
}

.num-key:active {
    background: #e8d8c0;
    transform: scale(0.96);
}

.num-key.empty {
    background: transparent;
    cursor: default;
}

.error-msg {
    color: #f56c6c;
    font-size: 13px;
    margin-top: 8px;
}
</style>