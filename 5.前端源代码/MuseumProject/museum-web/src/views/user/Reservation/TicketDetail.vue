<template>
    <div class="ticket-root">

        <!-- 动态水印 -->
        <div class="global-watermark">
            <div v-for="n in 20" :key="n" class="watermark-text">
                {{ ticketInfo.userId || '游客' }} · {{ realTimeWatermark }}
            </div>
        </div>

        <!-- 顶部导航 -->
        <nav class="top-nav">
            <el-button link @click="$router.back()" class="back-btn">
                <el-icon>
                    <ArrowLeft />
                </el-icon>返回我的
            </el-button>
            <span class="nav-title">电子凭证 · ELECTRONIC PASS</span>
            <div></div>
        </nav>

        <main class="page-body" v-loading="loading">
            <!-- ── 左侧票根 ── -->
            <section class="ticket-col">
                <div class="cultural-card">

                    <!-- 封面图 -->
                    <div class="card-cover">
                        <el-image :src="ticketInfo.coverImage" fit="cover" class="cover-img">
                            <template #error>
                                <div class="cover-fallback">MUSEUM</div>
                            </template>
                        </el-image>
                        <div class="cover-gradient"></div>

                        <!-- 倒计时覆盖在封面底部 -->
                        <div class="countdown-bar" v-if="ticketInfo.status === 0">
                            <el-icon>
                                <Timer />
                            </el-icon>
                            <span class="countdown-text">{{ countdownDetail }}</span>
                        </div>
                    </div>

                    <!-- 展览标题 -->
                    <div class="card-title-block">
                        <span class="ticket-type-badge">{{ ticketInfo.ticketType || '成人票' }}</span>
                        <h1 class="ex-name">{{ ticketInfo.exhibitionTitle || '载入中...' }}</h1>
                    </div>

                    <!-- 日期信息行 -->
                    <div class="date-strip">
                        <div class="date-item">
                            <span class="d-label">预约日期</span>
                            <span class="d-val">{{ ticketInfo.reserveDate || '---' }}</span>
                        </div>
                        <div class="date-divider"></div>
                        <div class="date-item">
                            <span class="d-label">入场时间</span>
                            <span class="d-val highlight">{{ ticketInfo.timeSlot || '全天' }}</span>
                        </div>
                        <div class="date-divider"></div>
                        <div class="date-item">
                            <span class="d-label">地点</span>
                            <span class="d-val">{{ ticketInfo.location || '---' }}</span>
                        </div>
                    </div>

                    <!-- 撕票线 -->
                    <div class="tear-line">
                        <div class="tear-circle left"></div>
                        <div class="tear-dashes"></div>
                        <div class="tear-circle right"></div>
                    </div>

                    <!-- 二维码 -->
                    <div class="qr-block">
                        <div class="qr-wrap" v-if="ticketInfo.verifyCode">
                            <img :src="`https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=${ticketInfo.verifyCode}`"
                                class="qr-img" id="ticket-print-area" />
                            <div class="scan-bar"></div>
                            <div v-if="ticketInfo.status === 1" class="stamp used">已入场</div>
                            <div v-if="ticketInfo.status === 2" class="stamp cancelled">已取消</div>
                        </div>
                        <div class="qr-empty" v-else>
                            <el-icon size="32" color="#ccc">
                                <Timer />
                            </el-icon>
                            <p>核销码待生成</p>
                        </div>
                        <p class="verify-code">{{ ticketInfo.verifyCode || '暂无核销码' }}</p>
                    </div>

                    <!-- 持票人信息 -->
                    <div class="holder-block">
                        <div class="holder-row">
                            <div class="h-item">
                                <span class="h-label">预约人</span>
                                <span class="h-val">{{ ticketInfo.userName || '---' }}</span>
                            </div>
                            <div class="h-item">
                                <span class="h-label">电话</span>
                                <span class="h-val">{{ ticketInfo.phone || '未填写' }}</span>
                            </div>
                        </div>

                        <!-- 修改预约人信息按钮 — 独立成行，更显眼 -->
                        <el-button v-if="ticketInfo.status === 0" class="edit-info-btn" @click="openEditDialog">
                            <el-icon>
                                <EditPen />
                            </el-icon>
                            修改预约人信息
                        </el-button>
                    </div>

                </div>
            </section>

            <!-- ── 右侧辅助区 ── -->
            <section class="guide-col">

                <!-- 入馆须知 -->
                <div class="guide-panel">
                    <div class="panel-header">
                        <el-icon>
                            <Bell />
                        </el-icon>
                        <h3>观展须知</h3>
                    </div>
                    <div class="guide-list">
                        <div class="guide-item" v-for="(g, i) in guides" :key="i">
                            <div class="g-num">0{{ i + 1 }}</div>
                            <div class="g-text">
                                <h4>{{ g.title }}</h4>
                                <p>{{ g.desc }}</p>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 前往场馆 -->
                <div class="nav-panel">
                    <div class="panel-header">
                        <el-icon>
                            <MapLocation />
                        </el-icon>
                        <h3>前往场馆</h3>
                    </div>
                    <div class="venue-preview">
                        <el-icon size="36" color="#c8bfb0">
                            <OfficeBuilding />
                        </el-icon>
                        <p class="venue-name">{{ ticketInfo.location || '暂未确定场馆' }}</p>
                    </div>
                    <el-button class="nav-map-btn" @click="handleBaiduNavigation">
                        使用百度地图导航
                    </el-button>


                    <!-- ✅ 三个操作按钮移至此处，椭圆磨砂风格 -->
                    <div class="action-pill-group">
                        <button class="pill-btn save" @click="handleSavePhoto">
                            <el-icon>
                                <Picture />
                            </el-icon>
                            <span>保存凭证</span>
                        </button>
                        <button class="pill-btn back" @click="$router.back()">
                            <el-icon>
                                <ArrowLeft />
                            </el-icon>
                            <span>返回上级</span>
                        </button>
                        <button v-if="ticketInfo.status === 0" class="pill-btn cancel" @click="handleCancelReservation">
                            <el-icon>
                                <CircleClose />
                            </el-icon>
                            <span>取消预约</span>
                        </button>
                    </div>
                </div>

            </section>
        </main>

        <!-- 修改信息弹窗 -->
        <el-dialog v-model="editVisible" title="修改预约人信息" width="400px" center destroy-on-close>
            <el-form :model="editForm" :rules="rules" ref="formRef" label-position="top">

                <el-form-item label="预约人" prop="realName">
                    <el-input v-model="editForm.realName" placeholder="请输入预约人姓名" />
                </el-form-item>

                <el-form-item label="手机号" prop="phone">
                    <el-input v-model="editForm.phone" placeholder="请输入11位手机号" />
                </el-form-item>
                <!-- <el-form-item label="邮箱" prop="email">
                    <el-input v-model="editForm.email" placeholder="用于接收通知邮件" />
                </el-form-item> -->
            </el-form>
            <template #footer>
                <el-button @click="editVisible = false">取消</el-button>
                <el-button type="primary" @click="submitUpdate" :loading="updateLoading">确认修改</el-button>
            </template>
        </el-dialog>

    </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
    ArrowLeft, Picture, CircleClose, Bell,
    MapLocation, OfficeBuilding, Timer, EditPen
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const resId = computed(() => route.params.id)
const loading = ref(false)
const realTimeWatermark = ref('')
let clockTimer = null

const ticketInfo = ref({
    userId: '', userName: '', phone: '', email: '',
    coverImage: '', exhibitionTitle: '', reserveDate: '', realName: '', idCard: '',
    timeSlot: '', location: '', verifyCode: '', ticketType: '', status: 0
})

// ── 实时倒计时（每秒刷新，不需要用户手动刷新）──
const countdownDetail = ref('计算中...')

const updateCountdown = () => {
    if (!ticketInfo.value.reserveDate) {
        countdownDetail.value = '...'
        return
    }
    const slotStart = ticketInfo.value.timeSlot
        ? (ticketInfo.value.timeSlot.includes('-')
            ? ticketInfo.value.timeSlot.split('-')[0].trim()
            : ticketInfo.value.timeSlot.includes('~')
                ? ticketInfo.value.timeSlot.split('~')[0].trim()
                : '09:00')
        : '09:00'
    const target = dayjs(`${ticketInfo.value.reserveDate} ${slotStart}`)
    const now = dayjs()
    const diff = target.diff(now)

    if (diff <= 0) {
        countdownDetail.value = '已到入场时间'
        return
    }
    const days = Math.floor(diff / 86400000)
    const hours = Math.floor((diff % 86400000) / 3600000)
    const minutes = Math.floor((diff % 3600000) / 60000)
    const seconds = Math.floor((diff % 60000) / 1000)
    countdownDetail.value = `距入场：${days}天 ${hours}时 ${minutes}分 ${seconds}秒`
}

// 观展须知内容
const guides = [
    { title: '准时到达', desc: '请在预约时间段内完成核销入场，超时可能需重新排队。' },
    { title: '出示凭证', desc: '保持屏幕亮度，将二维码对准闸机扫描口，或向工作人员出示。' },
    { title: '文明观展', desc: '馆内严禁开启闪光灯拍照，严禁触摸展品，保持环境安静。' },
]

// ── 修改信息 ──
const editVisible = ref(false)
const updateLoading = ref(false)
const formRef = ref(null)
const editForm = ref({ realNmae:'',phone: '', email: '' })
const rules = {
    realName: [
        { required: true, message: '请输入预约人姓名', trigger: 'blur' },
        { min: 2, max: 10, message: '长度在 2 到 10 个字符', trigger: 'blur' } // 可选：限制长度
    ],
    phone: [
        { required: true, message: '请输入手机号', trigger: 'blur' },
        { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
    ],
    email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
    ]
}

const openEditDialog = () => {
    editForm.value.realName = ticketInfo.value.userName
    editForm.value.phone = ticketInfo.value.phone
    editForm.value.email = ticketInfo.value.email
    editVisible.value = true
}

const submitUpdate = async () => {
    await formRef.value.validate(async (valid) => {
        if (!valid) return
        updateLoading.value = true
        try {
            await request.post('/reservation/updateInfo', {
                resId: resId.value,
                realName: editForm.value.realName,
                phone: editForm.value.phone,
                email: editForm.value.email
            })
            ElMessage.success('已同步修改联系信息')
            ticketInfo.value.realName = editForm.value.realName
            ticketInfo.value.phone = editForm.value.phone
            ticketInfo.value.email = editForm.value.email
            editVisible.value = false
        } catch (e) {
            ElMessage.error('修改失败')
        } finally {
            updateLoading.value = false
        }
    })
}

// ── 数据加载 ──
const getData = async () => {
    if (!resId.value) return
    loading.value = true
    try {
        const res = await request.get(`/reservation/detail/${resId.value}`)

        // 打印一下，看看后端到底传过来的是什么字段
        console.log('Ticket Data:', res)

        // 如果你的 request.js 拦截器返回的是整个 Result 对象 {code, data, msg}
        if (res && res.code === 200) {
            ticketInfo.value = res.data
        } else {
            // 如果拦截器直接返回了 data 里的内容
            ticketInfo.value = res
        }

        // 💡 关键：赋值后立即更新倒计时
        updateCountdown()
    } catch (e) {
        ElMessage.error('加载票据异常')
    } finally {
        loading.value = false
    }
}

// ── 业务操作 ──
const handleSavePhoto = () => {
    // 1. 找到要打印的那个 div
    const printContent = document.getElementById('ticket-print-area');

    if (!printContent) {
        console.error('未找到 ID 为 ticket-print-area 的元素');
        return;
    }

    // 2. 创建一个临时的窗口
    const newWindow = window.open('', '_blank');

    // 3. 获取当前页面的样式，保证打印出来不是乱码（可选）
    const styles = Array.from(document.querySelectorAll('style, link[rel="stylesheet"]'))
        .map(style => style.outerHTML)
        .join('');

    // 4. 写入内容并触发打印
    newWindow.document.write(`
    <html>
      <head>
        <title>打印凭证</title>
        ${styles}
      </head>
      <body>
        ${printContent.outerHTML}
      </body>
    </html>
  `);

    newWindow.document.close();

    // 5. 核心逻辑：加载完成后立即弹出打印面板，印完自动关闭
    newWindow.onload = () => {
        newWindow.print();
        newWindow.close();
    };
};

const handleBaiduNavigation = () => {
    if (!ticketInfo.value || !ticketInfo.value.location) {
        // 建议加个提示，增强用户体验
        // ElMessage.warning('暂无地点信息') 
        return
    }

    const dest = encodeURIComponent(ticketInfo.value.location)

    // 使用 direction 接口：自动规划路线
    // mode=driving 表示驾车模式，也可以换成 walking (步行) 或 transit (公交)
    const url = `https://api.map.baidu.com/direction?origin=name:我的位置&destination=${dest}&mode=driving&region=全国&output=html&src=webapp.yourname.yourproject`

    window.open(url, '_blank')
}

const handleCancelReservation = () => {
    ElMessageBox.confirm('确定要取消这次行程吗？取消后预约名额将重新释放。', '温馨提示', {
        confirmButtonText: '确定取消',
        cancelButtonText: '暂不取消',
        type: 'warning'
    }).then(async () => {
        try {
            const res = await request.post(`/reservation/cancel/${resId.value}`)
            if (res.code === 200) {
                ElMessage.success('已取消行程')
                router.back()
            }
        } catch {
            ElMessage.error('取消操作失败')
        }
    })
}

onMounted(() => {
    getData()
    // 每秒更新倒计时和水印
    clockTimer = setInterval(() => {
        realTimeWatermark.value = dayjs().format('YYYY/M/D HH:mm:ss')
        updateCountdown()
    }, 1000)
})

onUnmounted(() => {
    if (clockTimer) clearInterval(clockTimer)
})
</script>

<style scoped>
/* ── 变量 ── */
:root {
    --gold: #b89a6a;
    --ink: #2c2825;
    --muted: #9a9087;
    --bg: #f7f4ef;
    --card-bg: #ffffff;
    --border: #ede9e2;
}

/* ── 根容器 ── */
.ticket-root {
    min-height: 100vh;
    background: #f7f4ef;
    background-image: url('https://www.transparenttextures.com/patterns/natural-paper.png');
    font-family: "Optima", "Source Han Serif CN", "Noto Serif SC", serif;
    color: #2c2825;
    position: relative;
    overflow-x: hidden;
    padding-bottom: 60px;
}

/* ── 水印 ── */
.global-watermark {
    position: fixed;
    inset: 0;
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    pointer-events: none;
    z-index: 0;
    opacity: 0.04;
}

.watermark-text {
    transform: rotate(-20deg);
    font-size: 12px;
    color: #6b6560;
    padding: 55px 0;
    white-space: nowrap;
}

/* ── 导航栏 ── */
.top-nav {
    height: 58px;
    background: rgba(255, 255, 255, 0.85);
    backdrop-filter: blur(10px);
    border-bottom: 1px solid #ede9e2;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 40px;
    position: sticky;
    top: 0;
    z-index: 50;
}

.back-btn {
    color: #6b6560 !important;
    font-size: 13px;
}

.back-btn:hover {
    color: #b89a6a !important;
}

.nav-title {
    font-size: 13px;
    letter-spacing: 3px;
    color: #9a9087;
    font-weight: 400;
}

/* ── 主体布局 ── */
.page-body {
    display: flex;
    justify-content: center;
    gap: 48px;
    padding: 48px 24px;
    max-width: 1140px;
    margin: 0 auto;
    position: relative;
    z-index: 1;
}

/* ── 左侧票根 ── */
.ticket-col {
    flex: 0 0 400px;
}

.cultural-card {
    background: #fff;
    border-radius: 16px;
    overflow: hidden;
    box-shadow: 0 20px 60px rgba(62, 58, 54, 0.10);
    border: 1px solid #ede9e2;
}

/* 封面 */
.card-cover {
    height: 240px;
    position: relative;
}

.cover-img {
    width: 100%;
    height: 100%;
}

.cover-fallback {
    width: 100%;
    height: 100%;
    background: #e8e4da;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    letter-spacing: 4px;
    color: #b0a89e;
}

.cover-gradient {
    position: absolute;
    inset: 0;
    background: linear-gradient(to bottom, transparent 40%, rgba(0, 0, 0, 0.55));
}

.countdown-bar {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 10px 20px;
    display: flex;
    align-items: center;
    gap: 8px;
    color: #fff;
    font-size: 13px;
    letter-spacing: 0.5px;
    z-index: 2;
}

.countdown-text {
    font-variant-numeric: tabular-nums;
}

/* 展览标题块 */
.card-title-block {
    padding: 20px 24px 12px;
    border-bottom: 1px solid #f0ece4;
}

.ticket-type-badge {
    font-size: 11px;
    color: #b89a6a;
    border: 1px solid #e8d9c0;
    background: #fdf8ef;
    padding: 2px 10px;
    border-radius: 20px;
    display: inline-block;
    margin-bottom: 8px;
}

.ex-name {
    font-size: 22px;
    font-weight: 600;
    color: #2c2825;
    margin: 0;
    line-height: 1.3;
}

/* 日期信息条 */
.date-strip {
    display: flex;
    align-items: center;
    padding: 16px 24px;
    background: #fdf8ef;
    border-bottom: 1px solid #f0ece4;
    gap: 12px;
}

.date-item {
    flex: 1;
    text-align: center;
}

.d-label {
    display: block;
    font-size: 10px;
    color: #b0a89e;
    letter-spacing: 1px;
    margin-bottom: 4px;
}

.d-val {
    font-size: 13px;
    color: #3e3a36;
    font-weight: 500;
}

.d-val.highlight {
    color: #b89a6a;
}

.date-divider {
    width: 1px;
    height: 30px;
    background: #ede9e2;
    flex-shrink: 0;
}

/* 撕票线 */
.tear-line {
    display: flex;
    align-items: center;
    position: relative;
    padding: 0;
    background: #fff;
}

.tear-circle {
    width: 20px;
    height: 20px;
    border-radius: 50%;
    background: #f7f4ef;
    flex-shrink: 0;
    border: 1px solid #ede9e2;
}

.tear-circle.left {
    margin-left: -10px;
    border-left: none;
}

.tear-circle.right {
    margin-right: -10px;
    border-right: none;
}

.tear-dashes {
    flex: 1;
    height: 1px;
    background: repeating-linear-gradient(to right, #ddd9d0 0px, #ddd9d0 6px, transparent 6px, transparent 12px);
    margin: 0 4px;
}

/* 二维码块 */
.qr-block {
    padding: 24px;
    display: flex;
    flex-direction: column;
    align-items: center;
    border-bottom: 1px solid #f0ece4;
}

.qr-wrap {
    position: relative;
    padding: 12px;
    border: 1px solid #ede9e2;
    background: #fff;
    display: inline-block;
    border-radius: 4px;
}

.qr-img {
    width: 180px;
    height: 180px;
    display: block;
    filter: grayscale(0.1);
}

.scan-bar {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 2px;
    background: #b89a6a;
    animation: scanAnim 3s infinite linear;
}

@keyframes scanAnim {
    0% {
        top: 0;
    }

    100% {
        top: 100%;
    }
}

.stamp {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%) rotate(-15deg);
    border: 3px solid #c0392b;
    color: #c0392b;
    padding: 6px 18px;
    font-size: 26px;
    font-weight: 900;
    background: rgba(255, 255, 255, 0.92);
    border-radius: 2px;
    letter-spacing: 2px;
}

.stamp.cancelled {
    border-color: #909399;
    color: #909399;
}

.qr-empty {
    width: 180px;
    height: 180px;
    border: 1px dashed #ddd;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #bbb;
    font-size: 12px;
    gap: 8px;
    border-radius: 4px;
}

.verify-code {
    margin-top: 12px;
    font-family: monospace;
    font-size: 12px;
    color: #b0a89e;
    letter-spacing: 2px;
}

/* 持票人信息 */
.holder-block {
    padding: 20px 24px 24px;
}

.holder-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
    margin-bottom: 16px;
}

.h-item {}

.h-label {
    display: block;
    font-size: 10px;
    color: #b0a89e;
    letter-spacing: 1px;
    margin-bottom: 4px;
}

.h-val {
    font-size: 14px;
    color: #2c2825;
    font-weight: 500;
}

/* 修改信息按钮 — 独立一行，更显眼 */
.edit-info-btn {
    width: 100%;
    height: 40px;
    border: 1px dashed #c8bfb0;
    border-radius: 8px;
    background: #fdf8ef;
    color: #8b6f47;
    font-size: 14px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    transition: all 0.2s;
    letter-spacing: 1px;
}

.edit-info-btn:hover {
    background: #f5ece0;
    border-color: #b89a6a;
    color: #b89a6a;
}

/* ── 右侧辅助 ── */
.guide-col {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 24px;
    max-width: 420px;
}

.guide-panel,
.nav-panel {
    background: #fff;
    border-radius: 16px;
    padding: 28px;
    border: 1px solid #ede9e2;
    box-shadow: 0 4px 20px rgba(62, 58, 54, 0.05);
}

.panel-header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 22px;
    padding-bottom: 14px;
    border-bottom: 1px solid #f0ece4;
}

.panel-header h3 {
    font-size: 16px;
    margin: 0;
    color: #2c2825;
    font-weight: 500;
}

.panel-header .el-icon {
    color: #b89a6a;
}

.guide-list {
    display: flex;
    flex-direction: column;
    gap: 18px;
}

.guide-item {
    display: flex;
    gap: 14px;
    align-items: flex-start;
}

.g-num {
    font-size: 11px;
    color: #b89a6a;
    border-bottom: 1px solid #b89a6a;
    line-height: 1.4;
    flex-shrink: 0;
    margin-top: 2px;
}

.g-text h4 {
    font-size: 14px;
    color: #2c2825;
    margin: 0 0 4px;
    font-weight: 500;
}

.g-text p {
    font-size: 12px;
    color: #7a6e65;
    line-height: 1.65;
    margin: 0;
}

/* 场馆预览 */
.venue-preview {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    height: 90px;
    background: #fdf8ef;
    border-radius: 10px;
    border: 1px solid #f0ece4;
    margin-bottom: 14px;
}

.venue-name {
    font-size: 13px;
    color: #9a9087;
    margin: 0;
}

.nav-map-btn {
    width: 100%;
    height: 40px;
    border-radius: 20px !important;
    background: #2a5caa !important;
    color: #fff !important;
    border: none !important;
    font-size: 13px;
    letter-spacing: 1px;
    margin-bottom: 20px;
    transition: box-shadow 0.25s !important;
}

.nav-map-btn:hover {
    box-shadow: 0 6px 20px rgba(42, 92, 170, 0.35) !important;
}

/* ── 椭圆磨砂按钮组 ── */
.action-pill-group {
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding-top: 4px;
}

.pill-btn {
    width: 100%;
    height: 46px;
    border-radius: 23px;
    border: none;
    cursor: pointer;
    font-size: 14px;
    letter-spacing: 1.5px;
    font-family: inherit;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    transition: box-shadow 0.28s ease, transform 0.18s ease;
    backdrop-filter: blur(8px);
    -webkit-backdrop-filter: blur(8px);
}

/* 保存凭证 — 磨砂金 */
.pill-btn.save {
    background: rgba(184, 154, 106, 0.18);
    color: #8b6f47;
    border: 1px solid rgba(184, 154, 106, 0.4);
}

.pill-btn.save:hover {
    box-shadow: 0 8px 24px rgba(184, 154, 106, 0.35);
    transform: translateY(-2px);
}

/* 返回上级 — 磨砂灰 */
.pill-btn.back {
    background: rgba(154, 144, 135, 0.12);
    color: #6b6560;
    border: 1px solid rgba(154, 144, 135, 0.3);
}

.pill-btn.back:hover {
    box-shadow: 0 8px 24px rgba(100, 90, 80, 0.2);
    transform: translateY(-2px);
}

/* 取消预约 — 磨砂红 */
.pill-btn.cancel {
    background: rgba(192, 57, 43, 0.08);
    color: #c0392b;
    border: 1px solid rgba(192, 57, 43, 0.25);
}

.pill-btn.cancel:hover {
    box-shadow: 0 8px 24px rgba(192, 57, 43, 0.25);
    transform: translateY(-2px);
}

/* ── 响应式 ── */
@media (max-width: 900px) {
    .page-body {
        flex-direction: column;
        align-items: center;
        padding: 24px 16px;
        gap: 24px;
    }

    .ticket-col {
        flex: auto;
        width: 100%;
        max-width: 420px;
    }

    .guide-col {
        width: 100%;
        max-width: 420px;
    }

    .top-nav {
        padding: 0 20px;
    }
}
</style>