<template>
  <div class="reserve-root" v-loading="loading">
    <el-card class="reserve-card" v-if="exhibition">
      <div class="exh-header">
        <el-image :src="exhibition.coverUrl" fit="cover" class="exh-cover" />
        <div class="exh-info">
          <h2 class="exh-title">{{ exhibition.title }}</h2>
          <p><el-icon>
              <Location />
            </el-icon> {{ exhibition.targetLocation }}</p>
          <p><el-icon>
              <Calendar />
            </el-icon> 展期：{{ exhibition.startDate }} 至 {{ exhibition.endDate }}</p>
        </div>
      </div>

      <el-divider content-position="left">预约登记</el-divider>
      <!-- 常用预约人快速填充 -->
      <div v-if="visitorList.length > 0" class="visitor-bar">
        <span class="visitor-label">常用预约人：</span>
        <el-radio-group v-model="selectedVisitorId" @change="fillVisitor">
          <el-radio-button v-for="v in visitorList" :key="v.visitorId" :label="v.visitorId">
            {{ v.realName }}
            <el-tag v-if="v.isDefault === 1" size="small" type="danger" style="margin-left:4px">默认</el-tag>
          </el-radio-button>
        </el-radio-group>
        <el-button link style="margin-left:8px" @click="selectedVisitorId = null; clearVisitor()">清空</el-button>
      </div>
      <el-form :model="resForm" :rules="rules" ref="formRef" label-position="top">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="resForm.realName" placeholder="请输入参观人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="resForm.phone" placeholder="接收核销短信" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="身份证号" prop="idCard">
              <el-input v-model="resForm.idCard" placeholder="请输入18位身份证号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="电子邮箱" prop="email">
              <el-input v-model="resForm.email" placeholder="用于接收预约回执" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="参观日期" prop="reserveDate">
          <el-date-picker v-model="resForm.reserveDate" type="date" placeholder="请选择日期" style="width: 100%"
            value-format="YYYY-MM-DD" :disabled-date="getDisabledDate" />
        </el-form-item>

        <el-form-item label="参观时段" prop="timeSlot">
          <el-radio-group v-model="resForm.timeSlot">
            <el-radio-button v-for="slot in config.slots" :key="slot.name" :label="slot.name">
              {{ slot.name }} ({{ slot.timeRange[0] }}-{{ slot.timeRange[1] }})
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="票种选择" prop="ticketType">
          <el-radio-group v-model="resForm.ticketType">
            <el-radio v-for="ticket in config.tickets" :key="ticket.name" :label="ticket.name" border>
              {{ ticket.name }} <span class="price-tag">￥{{ ticket.price }}</span>
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <div class="payment-bar" v-if="currentPrice > 0">
          <div class="total-price">
            需支付：<span>￥{{ currentPrice }}</span>
          </div>
          <p class="pay-tip">支付完成后将生成预约核销码</p>
        </div>

        <el-form-item label="备注说明">
          <el-input v-model="resForm.remark" type="textarea" placeholder="特殊需求备注" />
        </el-form-item>

        <div class="action-zone">
          <el-button type="primary" size="large" @click="handleReserve(formRef)" class="submit-btn">
            {{ currentPrice > 0 ? '立即支付并预约' : '免费预约' }}
          </el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, Location } from '@element-plus/icons-vue'
import axios from 'axios'


const route = useRoute()
const router = useRouter()
const formRef = ref()
const loading = ref(true)
const exhibition = ref(null) // 展览详情
const config = reactive({ slots: [], tickets: [] }) // 解析后的配置

const resForm = reactive({
  realName: '',
  phone: '',
  idCard: '', //新增
  email:'',//新增
  reserveDate: '',
  timeSlot: '',
  ticketType: '',
  remark: '',
  exhibitionId: route.query.exId // 从地址栏获取展览ID
})

const rules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式错误', trigger: 'blur' }
  ],
  // ✨ 新增身份证校验
  idCard: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/, message: '身份证格式错误', trigger: 'blur' }
  ],
  // ✨ 新增邮箱校验
  email: [
    { required: true, message: '请输入电子邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
  ],
  reserveDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  timeSlot: [{ required: true, message: '请选择场次', trigger: 'change' }],
  ticketType: [{ required: true, message: '请选择票种', trigger: 'change' }]
}

// 核心：计算当前票价
const currentPrice = computed(() => {
  const selected = config.tickets.find(t => t.name === resForm.ticketType)
  return selected ? selected.price : 0
})

// 加载展览数据
const initPage = async () => {
    // 修正：从 route.query.exId 获取
    const currentExId = route.query.exId

    if (!currentExId) {
        ElMessage.error('未找到巡展编号，请重新选择')
        return
    }

    try {
        loading.value = true
        // 拼接正确的 ID
      const res = await axios.get(`/api/exhibition/detail/${currentExId}`)
      console.log('接口返回数据:', JSON.stringify(res.data.data))
        if (res.data.code === 200) {
            exhibition.value = res.data.data.appDetail
            config.slots = JSON.parse(exhibition.value.timeSlotConfig || '[]')
          config.tickets = JSON.parse(exhibition.value.ticketTypes || '[]')
          console.log('票种数据:', config.tickets) // 👈 加这行
        }
    } catch (e) {
        console.error("详情请求失败:", e)
        ElMessage.error('获取巡展详情失败')
    } finally {
        loading.value = false
    }
}

// 1. 日期限制逻辑
const getDisabledDate = (time) => {
  const today = new Date().setHours(0, 0, 0, 0)
  const start = new Date(exhibition.value.startDate).getTime()
  const end = new Date(exhibition.value.endDate).getTime()
  
  // 不能早于今天，不能早于开展，不能晚于闭展
  return time.getTime() < today || time.getTime() < start || time.getTime() > end
}

// 4. 预约与支付流程
// const handleReserve = async (formEl) => {

//   if (!formEl) return
//   await formEl.validate(async (valid) => {
//     if (valid) {
//       // 支付确认提示
//       const confirmMsg = currentPrice.value > 0
//         ? `您预约的是 [${resForm.ticketType}]，需支付 ￥${currentPrice.value} 元，是否确认支付？`
//         : '确认提交免费预约申请？'

//       ElMessageBox.confirm(confirmMsg, '预约确认', {
//         confirmButtonText: currentPrice.value > 0 ? '前往支付' : '确认',
//         type: 'info'
//       }).then(async () => {
//         // 这里模拟支付成功逻辑
//         const postData = {
//           ...resForm,
//           userId: JSON.parse(sessionStorage.getItem('userInfo'))?.userId,
//           status: currentPrice.value > 0 ? 0 : 1 // 假设0为待支付/待核销，根据业务定
//         }
//         const res = await axios.post('/api/reservation/submit', postData)
//         if (res.data.code === 200) {
//           ElMessage.success('预约成功！')
//           router.push('/user/my-reservations')
//         }
//       })
//     }
//   })
// }
// 4. 预约与支付流程
const handleReserve = async (formEl) => {
  if (!formEl) return

  // 获取当前登录用户 ID
  const userInfo = JSON.parse(sessionStorage.getItem('user'))
  const userId = userInfo?.userId || userInfo?.id // 兼容不同的字段名

  if (!userId) {
    ElMessage.warning('登录信息已过期，请重新登录')
    // router.push('/login')
    return
  }

  await formEl.validate(async (valid) => {
    if (!valid) {
      ElMessage.error('请填写完整的预约信息')
      return
    }

    // 支付确认提示
    const isPaid = currentPrice.value > 0
    const confirmMsg = isPaid
      ? `您预约的是 [${resForm.ticketType}]，需支付 ￥${currentPrice.value} 元，是否确认支付？`
      : '确认提交免费预约申请？'

    try {
      await ElMessageBox.confirm(confirmMsg, '预约确认', {
        confirmButtonText: isPaid ? '支付并预约' : '确认预约',
        cancelButtonText: '取消',
        type: 'success'
      })

      // 准备提交的数据
      const postData = {
        ...resForm,
        userId: userId,
        price: currentPrice.value, // 建议显式传价格
        status: 0, // 👈 统一改为 0：代表“已预约/待核销”
        createTime: new Date() // 可选，通常后端会自动生成
      }

      // 发起请求
      const res = await axios.post('/api/reservation/apply', postData)
      
      
      if (res.data.code === 200) {
        ElMessage({
          type: 'success',
          message: isPaid ? '支付并预约成功！' : '预约成功！',
          duration: 2000
        })

        // 跳转到你之前定义的“我的预约管理”页面
        // 注意确认你的路由路径是 /user/appointment
        router.push('/user/appointment')
      } else {
        ElMessage.error(res.data.msg || '预约失败，请稍后再试')
      }
    } catch (error) {
      // 捕获取消按键或网络错误
      if (error !== 'cancel') {
        console.error("提交预约出错:", error)
        ElMessage.error('网络连接失败，请检查后端服务')
      }
    }
  })
}

// ── 常用预约人 ──────────────────────────
const visitorList = ref([])
const selectedVisitorId = ref(null)

const fetchVisitors = async () => {
  const userStr = sessionStorage.getItem('user')
  if (!userStr) return
  const userId = JSON.parse(userStr).userId
  try {
    const res = await axios.get('/api/visitor/list', { params: { userId } })
    visitorList.value = res.data?.data || []
    // 自动选中默认预约人
    const def = visitorList.value.find(v => v.isDefault === 1)
    if (def) {
      selectedVisitorId.value = def.visitorId
      fillVisitor(def.visitorId)
    }
  } catch (e) { }
}

const fillVisitor = (visitorId) => {
  const v = visitorList.value.find(v => v.visitorId === visitorId)
  if (!v) return
  resForm.realName = v.realName
  resForm.phone = v.phone
  resForm.idCard = v.idCard
  resForm.email = v.email || ''
}

const clearVisitor = () => {
  resForm.realName = ''
  resForm.phone = ''
  resForm.idCard = ''
  resForm.email = ''
}

onMounted(async () => {
  initPage()
  fetchVisitors()   // ← 加这行
})

</script>

<style scoped>
.reserve-root { padding: 20px; background: #f5f7fa; min-height: 100vh; display: flex; justify-content: center; }
.reserve-card { width: 100%; max-width: 700px; border-radius: 12px; }

/* 展览信息页眉样式 */
.exh-header { display: flex; gap: 20px; margin-bottom: 20px; background: #fffaf5; padding: 15px; border-radius: 8px; border: 1px solid #ffeddf; }
.exh-cover { width: 180px; height: 110px; border-radius: 6px; }
.exh-title { margin: 0 0 10px 0; color: #333; }
.exh-info p { margin: 5px 0; font-size: 14px; color: #666; display: flex; align-items: center; gap: 5px; }

/* 票价标签 */
.price-tag { color: #f56c6c; font-weight: bold; margin-left: 5px; }

/* 支付条 */
.payment-bar { background: #fef0f0; border: 1px solid #fde2e2; padding: 15px; border-radius: 8px; margin: 20px 0; text-align: right; }
.total-price { font-size: 16px; color: #606266; }
.total-price span { font-size: 24px; color: #f56c6c; font-weight: bold; }
.pay-tip { font-size: 12px; color: #999; margin: 5px 0 0 0; }

.action-zone { text-align: center; margin-top: 30px; }
.submit-btn { width: 250px; height: 50px; border-radius: 25px; font-size: 18px; }
.visitor-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  background: #f0f7ff;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  padding: 10px 14px;
  margin-bottom: 20px;
}

.visitor-label {
  font-size: 13px;
  color: #374151;
  white-space: nowrap;
  font-weight: 600;
}
</style>