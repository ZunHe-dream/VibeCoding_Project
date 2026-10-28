<template>
  <div class="super-exhibition">
    <!-- 页头 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-tag">SUPER ADMIN · 巡展全链路监控</div>
        <h1 class="page-title">全量策展记录</h1>
      </div>
      <el-button class="export-btn" @click="exportExcel" :loading="exporting">
        ↓ 导出 Excel
      </el-button>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-row">
      <div class="stat-card" v-for="s in stats" :key="s.label">
        <div class="stat-num" :style="{ color: s.color }">{{ s.num }}</div>
        <div class="stat-label">{{ s.label }}</div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-input v-model="filters.keyword" placeholder="搜索展览名/策展人/单位…" clearable style="width:240px" @input="fetchList"
        @clear="fetchList" />
      <el-select v-model="filters.status" placeholder="审批状态" clearable style="width:120px" @change="fetchList">
        <el-option label="待审批" :value="0" />
        <el-option label="已通过" :value="1" />
        <el-option label="已驳回" :value="2" />
      </el-select>
      <el-select v-model="filters.planStatus" placeholder="展览进度" clearable style="width:120px" @change="fetchList">
        <el-option label="筹备中" :value="0" />
        <el-option label="进行中" :value="1" />
        <el-option label="已结束" :value="2" />
      </el-select>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <!-- 表格 -->
    <div class="table-wrap" v-loading="loading">
      <el-table :data="list" border size="small" row-key="appId" :row-class-name="rowClass">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-area">
              <div class="expand-section">
                <span class="expand-label">审批意见：</span>
                <span class="expand-val">{{ row.auditRemark || '—' }}</span>
              </div>
              <div class="expand-section">
                <span class="expand-label">巡展详细信息：</span>
                <div class="expand-area">
                  <div class="expand-section">
                    <span class="expand-label">策展单位：</span>
                    <span class="expand-val">{{ row.applicantBelong }} · {{ row.applicantNickname }}</span>
                  </div>
                  <div class="expand-section" v-if="row.ticketTypes">
                    <span class="expand-label">票种价格：</span>
                    <span class="expand-val">
                      <el-tag v-for="t in parseTickets(row.ticketTypes)" :key="t.name" size="small"
                        style="margin-right:6px">
                        {{ t.name }}：{{ t.price === 0 ? '免费' : '¥' + t.price }}
                      </el-tag>
                    </span>
                  </div>
                  <div class="expand-section" v-if="row.timeSlotConfig">
                    <span class="expand-label">场次配置：</span>
                    <span class="expand-val">
                      <span v-for="s in parseSlots(row.timeSlotConfig)" :key="s.name" style="margin-right:12px">
                        {{ s.name }}（{{ s.timeRange?.join('-') }}，{{ s.quota }}人）
                      </span>
                    </span>
                  </div>
                  <div class="expand-section">
                    <span class="expand-label">借用文物：</span>
                    <span v-if="!row.artifactIds" class="expand-val">无</span>
                    <span v-else class="artifact-tags">
                      <el-tag v-for="id in row.artifactIds.split(',')" :key="id" size="small" type="info"
                        style="margin:2px;cursor:pointer" @click="showArtifact(id.trim())">#{{ id.trim() }}</el-tag>
                    </span>
                  </div>
                  <div class="expand-section" v-if="row.exhibitionId">
                    <el-button size="small" type="primary" plain @click="showReservations(row)">查看预约名单</el-button>
                    <el-button size="small" plain @click="exportReservations(row)">导出预约名单</el-button>
                  </div>
                </div>
                <span v-if="!row.artifactIds" class="expand-val">无</span>
                <!-- <span v-else class="artifact-tags">
                  <el-tag v-for="id in row.artifactIds.split(',')" :key="id" size="small" type="info"
                    style="margin:2px;cursor:pointer" @click="showArtifact(id.trim())">#{{ id.trim() }}</el-tag>
                </span> -->
              </div>
              <!-- <div class="expand-section" v-if="row.exhibitionId">
                <el-button size="small" type="primary" plain @click="showReservations(row)">
                  查看预约名单
                </el-button>
                <el-button size="small" plain @click="exportReservations(row)">
                  导出预约名单
                </el-button>
              </div> -->
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="title" label="展览名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="策展单位" width="140">
          <template #default="{ row }">
            <div>{{ row.applicantBelong }}</div>
            <div style="font-size:11px;color:#999">{{ row.applicantNickname }}</div>
          </template>
        </el-table-column>
        <el-table-column label="展览时间" width="200">
          <template #default="{ row }">
            {{ row.startDate }} ~ {{ row.endDate }}
          </template>
        </el-table-column>
        <el-table-column prop="targetLocation" label="举办地点" width="140" show-overflow-tooltip />
        <el-table-column label="审批状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="appStatusType(row.status)" size="small">
              {{ appStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="展览进度" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.exhibitionId !== null" :type="planStatusType(row.currentStatus)" size="small">
              {{ planStatusText(row.currentStatus) }}
            </el-tag>
            <span v-else style="color:#ccc;font-size:12px">未发布</span>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="150">
          <template #default="{ row }">{{ row.createTime?.slice(0,16) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center" fixed="right">
          <template #default="{ row }">
            <el-table-column label="操作" width="140" align="center" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.exhibitionId && row.currentStatus === 1" link type="warning" size="small"
                  @click="endExhibition(row)">结束巡展</el-button>
                <el-button link type="danger" size="small" :disabled="row.exhibitionId && row.currentStatus === 1"
                  :title="row.currentStatus === 1 ? '请先结束巡展' : ''"
                  @click="row.currentStatus !== 1 && deleteExhibition(row)">删除</el-button>
              </template>
            </el-table-column>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">共 {{ list.length }} 条记录</div>
    </div>

    <!-- 文物详情弹窗 -->
    <el-dialog v-model="artifactVisible" title="文物详情" width="460px" destroy-on-close>
      <div v-if="artifactDetail" class="artifact-detail">
        <img v-if="artifactDetail.imageUrl" :src="artifactDetail.imageUrl" class="artifact-img"
          @error="(e) => e.target.style.display='none'" />
        <el-descriptions :column="2" border size="small" style="margin-top:12px">
          <el-descriptions-item label="文物名称" :span="2">{{ artifactDetail.artifactName }}</el-descriptions-item>
          <el-descriptions-item label="作者">{{ artifactDetail.author || '佚名' }}</el-descriptions-item>
          <el-descriptions-item label="朝代">{{ artifactDetail.dynasty || '—' }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ artifactDetail.category || '—' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="['success','warning','info'][artifactDetail.status]" size="small">
              {{ ['馆藏中','外借中','维修中'][artifactDetail.status] }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="所属博物馆" :span="2">{{ artifactDetail.ownerMuseum }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ artifactDetail.description || '暂无' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <div v-else style="text-align:center;padding:20px;color:#999">加载中…</div>
    </el-dialog>

    <!-- 预约名单弹窗 -->
    <el-dialog v-model="resVisible" :title="`预约名单 · ${resTitle}`" width="760px" destroy-on-close>
      <el-table :data="resList" size="small" border v-loading="resLoading">
        <el-table-column prop="realName" label="参观人" width="90" />
        <el-table-column prop="phone" label="电话" width="120" />
        <el-table-column prop="idCard" label="证件号" width="160" />
        <el-table-column prop="reserveDate" label="参观日期" width="110" />
        <el-table-column prop="timeSlot" label="场次" width="90" />
        <el-table-column prop="ticketType" label="票种" width="90" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="['warning','success','info'][row.status]" size="small">
              {{ ['待核销','已参观','已取消'][row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="verifyCode" label="核销码" />
      </el-table>
      <div style="margin-top:10px;font-size:13px;color:#999">
        共 {{ resList.length }} 条预约记录
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
const parseTickets = (str) => { try { return JSON.parse(str || '[]') } catch { return [] } }
const parseSlots = (str) => { try { return JSON.parse(str || '[]') } catch { return [] } }
const loading = ref(false)
const exporting = ref(false)
const list = ref([])
const filters = reactive({ keyword: '', status: null, planStatus: null })

const endExhibition = async (row) => {
  await ElMessageBox.confirm(
    `确认结束【${row.title}】？结束后用户将无法继续预约。`,
    '结束巡展确认', { type: 'warning', confirmButtonText: '确认结束' }
  )
  const res = await axios.post('/api/exhibition/plan/end', null, {
    params: { exhibitionId: row.exhibitionId }
  })
  if (res.data.code === 200) {
    ElMessage.success('已结束巡展')
    fetchList()
  } else {
    ElMessage.error(res.data.message || '操作失败')
  }
}

// 统计
const stats = computed(() => [
  { label: '全部申请', num: list.value.length, color: '#2c1a0e' },
  { label: '待审批', num: list.value.filter(i => i.status === 0).length, color: '#e6a23c' },
  { label: '已通过', num: list.value.filter(i => i.status === 1).length, color: '#67c23a' },
  { label: '已驳回', num: list.value.filter(i => i.status === 2).length, color: '#f56c6c' },
  { label: '进行中', num: list.value.filter(i => i.currentStatus === 1).length, color: '#8b1a1a' },
])



const fetchList = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/exhibition/super/list', {
      params: {
        keyword: filters.keyword || undefined,
        status: filters.status !== null ? filters.status : undefined,
        planStatus: filters.planStatus !== null ? filters.planStatus : undefined
      }
    })
    if (res.data.code === 200) list.value = res.data.data
  } finally {
    loading.value = false
  }
}
onMounted(fetchList)

const resetFilters = () => {
  filters.keyword = ''; filters.status = null; filters.planStatus = null
  fetchList()
}

const rowClass = ({ row }) => row.status === 2 ? 'row-rejected' : ''

// 状态映射
const appStatusText = (s) => ['待审批','已通过','已驳回'][s] ?? '未知'
const appStatusType = (s) => ['warning','success','danger'][s] ?? 'info'
const planStatusText = (s) => ['筹备中','进行中','已结束'][s] ?? '—'
const planStatusType = (s) => ['info','success',''][s] ?? 'info'

// 文物详情
const artifactVisible = ref(false)
const artifactDetail = ref(null)
const showArtifact = async (id) => {
  artifactDetail.value = null
  artifactVisible.value = true
  const res = await axios.get(`/api/artifact/${id}`)
  if (res.data.code === 200) artifactDetail.value = res.data.data
}

// 预约名单
const resVisible = ref(false)
const resList = ref([])
const resLoading = ref(false)
const resTitle = ref('')
const showReservations = async (row) => {
  resTitle.value = row.title
  resList.value = []
  resVisible.value = true
  resLoading.value = true
  try {
    const res = await axios.get('/api/reservation/byExhibition', {
      params: { exhibitionId: row.exhibitionId }
    })
    const data = res.data
    resList.value = Array.isArray(data) ? data : (data?.data || [])
  } finally {
    resLoading.value = false
  }
}

// 删除展览
const deleteExhibition = async (row) => {
  await ElMessageBox.confirm(
    `确认删除【${row.title}】？\n将同步恢复借用文物状态并取消所有预约。`,
    '危险操作确认',
    { type: 'warning', confirmButtonText: '确认删除', confirmButtonClass: 'el-button--danger' }
  )
  const res = await axios.delete(`/api/exhibition/super/delete/${row.appId}`)
  if (res.data.code === 200) {
    ElMessage.success('已删除并同步处理关联数据')
    fetchList()
  } else {
    ElMessage.error(res.data.message || '删除失败')
  }
}

// 导出全量Excel
const exportExcel = async () => {
  exporting.value = true
  try {
    await loadScript('https://cdnjs.cloudflare.com/ajax/libs/xlsx/0.18.5/xlsx.full.min.js')
    const wb = window.XLSX.utils.book_new()
    const data = list.value.map(r => ({
      '展览名称': r.title,
      '策展单位': r.applicantBelong,
      '策展人': r.applicantNickname,
      '举办地点': r.targetLocation,
      '开始日期': r.startDate,
      '结束日期': r.endDate,
      '审批状态': appStatusText(r.status),
      '展览进度': r.exhibitionId ? planStatusText(r.currentStatus) : '未发布',
      '提交时间': r.createTime?.slice(0,16)
    }))
    const ws = window.XLSX.utils.json_to_sheet(data)
    window.XLSX.utils.book_append_sheet(wb, ws, '全量策展记录')
    window.XLSX.writeFile(wb, `全量策展记录_${new Date().toLocaleDateString()}.xlsx`)
    ElMessage.success('导出成功')
  } finally {
    exporting.value = false
  }
}

// 导出特定展览预约名单
const exportReservations = async (row) => {
  await loadScript('https://cdnjs.cloudflare.com/ajax/libs/xlsx/0.18.5/xlsx.full.min.js')
  const res = await axios.get('/api/reservation/byExhibition', {
    params: { exhibitionId: row.exhibitionId }
  })
  const data = res.data?.data || res.data || []
  const wb = window.XLSX.utils.book_new()
  const sheetData = data.map(r => ({
    '参观人': r.realName,
    '联系电话': r.phone,
    '证件号': r.idCard,
    '参观日期': r.reserveDate,
    '场次': r.timeSlot,
    '票种': r.ticketType,
    '状态': ['待核销','已参观','已取消'][r.status] ?? '未知',
    '核销码': r.verifyCode
  }))
  const ws = window.XLSX.utils.json_to_sheet(sheetData)
  window.XLSX.utils.book_append_sheet(wb, ws, '预约名单')
  window.XLSX.writeFile(wb, `${row.title}_预约名单.xlsx`)
  ElMessage.success('导出成功')
}

const loadScript = (src) => new Promise((res, rej) => {
  if (document.querySelector(`script[src="${src}"]`)) return res()
  const s = document.createElement('script'); s.src = src
  s.onload = res; s.onerror = rej; document.head.appendChild(s)
})
</script>

<style scoped>
.super-exhibition {
  padding: 28px 32px; min-height: 100vh;
  background: #faf8f5; font-family: 'Noto Serif SC', 'Noto Sans SC', serif;
}

.page-header {
  display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 24px;
}
.header-tag {
  font-size: 11px; letter-spacing: 3px; color: #8b1a1a;
  text-transform: uppercase; margin-bottom: 6px;
}
.page-title { font-size: 26px; font-weight: 700; color: #1a0a0a; margin: 0; }

.export-btn {
  background: #8b1a1a !important; color: #fff !important;
  border: none !important; border-radius: 8px !important;
  padding: 10px 22px !important; font-size: 13px !important;
}
.export-btn:hover { background: #a52020 !important; }

/* 统计卡片 */
.stat-row { display: flex; gap: 14px; margin-bottom: 24px; }
.stat-card {
  flex: 1; background: #fff; border-radius: 10px; padding: 16px 20px;
  border: 1px solid #e8dfd0; text-align: center;
}
.stat-num { font-size: 30px; font-weight: 700; line-height: 1; margin-bottom: 6px; }
.stat-label { font-size: 12px; color: #9a7c5a; }

/* 筛选栏 */
.filter-bar {
  display: flex; gap: 10px; align-items: center;
  background: #fff; border-radius: 10px; padding: 14px 16px;
  margin-bottom: 16px; border: 1px solid #e8dfd0;
}

/* 表格 */
.table-wrap { background: #fff; border-radius: 10px; overflow: hidden; border: 1px solid #e8dfd0; }
.table-footer { padding: 12px 16px; font-size: 12px; color: #9a7c5a; text-align: right; }

/* 展开区 */
.expand-area { padding: 12px 20px; background: #faf7f3; }
.expand-section { display: flex; align-items: flex-start; gap: 10px; margin-bottom: 10px; flex-wrap: wrap; }
.expand-label { font-size: 12px; color: #9a7c5a; flex-shrink: 0; margin-top: 2px; }
.expand-val { font-size: 13px; color: #2c1a0e; }
.artifact-tags { display: flex; flex-wrap: wrap; }

/* 驳回行高亮 */
:deep(.row-rejected td) { background: #fff8f8 !important; }

/* 文物详情 */
.artifact-img { width: 100%; max-height: 220px; object-fit: cover; border-radius: 8px; }

/* 主色调覆盖 */
:deep(.el-tag--danger) { --el-tag-bg-color: #fff2f2; --el-tag-border-color: #fcd3d3; --el-tag-text-color: #8b1a1a; }
:deep(.el-button--primary) { --el-button-bg-color: #8b1a1a; --el-button-border-color: #8b1a1a; }
:deep(.el-button--primary:hover) { --el-button-hover-bg-color: #a52020; }
</style>