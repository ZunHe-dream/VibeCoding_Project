<template>
    <div class="audit-container">
        <el-row :gutter="20">
            <el-col :span="8">
                <el-card class="list-card" shadow="never">
                    <template #header>
                        <div class="list-header">
                            <span>📋 待处理申请</span>
                            <el-badge :value="pendingCount" type="danger" />
                        </div>
                    </template>

                    <el-scrollbar height="650px">
                        <div v-for="item in applicationList" :key="item.appId"
                            :class="['app-item', currentApp?.appId === item.appId ? 'is-active' : '']"
                            @click="loadDetail(item.appId)">
                            <div class="app-info">
                                <div class="app-title">{{ item.title }}</div>
                                <div class="app-time">提交于：{{ formatDate(item.createTime) }}</div>
                            </div>
                            <div style="display:flex;flex-direction:column;align-items:flex-end;gap:4px">
                                <el-tag :type="getStatusTag(item.status)" size="small">{{ getStatusText(item.status)
                                    }}</el-tag>
                                <el-tag v-if="isFinalizedApp(item.appId)" type="success" size="small" effect="plain">✓
                                    本馆已签发</el-tag>
                            </div>
                        </div>
                        <el-empty v-if="applicationList.length === 0" description="暂无申请需要处理" />
                    </el-scrollbar>
                </el-card>
            </el-col>

            <el-col :span="16">
                <el-card v-if="currentApp" class="detail-card" shadow="never">
                    <div class="doc-wrapper">
                        <!-- <div class="doc-poster" v-if="currentApp.coverUrl">
                            <el-image :src="currentApp.coverUrl" fit="cover" class="poster-img">
                                <template #error>
                                    <div class="img-slot">方案封面</div>
                                </template>
                            </el-image>
                        </div> -->

                        <div class="doc-header">
                            <h1>巡展调拨审批件</h1>
                            <div class="doc-no">编号：APPLY-{{ currentApp.appId }}</div>
                        </div>

                        <table class="doc-table">
                            <tr>
                                <td class="label">巡展主题</td>
                                <td colspan="3" class="value" style="font-weight: bold; font-size: 16px;">{{
                                    currentApp.title }}
                                </td>
                            </tr>
                            <tr>
                                <td class="label">举办地点</td>
                                <td class="value">{{ currentApp.targetLocation }}</td>
                                <td class="label">申请日期</td>
                                <td class="value">{{ formatDate(currentApp.createTime) }}</td>
                            </tr>
                            <tr>
                                <td class="label">计划周期</td>
                                <td class="value">{{ currentApp.startDate }} 至 {{ currentApp.endDate }}</td>
                                <td class="label">承载限制</td>
                                <td class="value">{{ currentApp.maxCapacity }} 人/日</td>
                            </tr>
                        </table>

                        <div class="config-row">
                            <div class="config-col">
                                <div class="section-title">🎫 票务定价方案</div>
                                <template v-if="parseJson(currentApp.ticketTypes).length > 0">
                                    <el-table :data="parseJson(currentApp.ticketTypes)" size="small" border>
                                        <el-table-column prop="name" label="票种" />
                                        <el-table-column label="价格">
                                            <template #default="{ row }">￥{{ row.price }}</template>
                                        </el-table-column>
                                    </el-table>
                                </template>
                                <template v-else>
                                    <el-table :data="[{ name: '统一票价', price: currentApp.ticketPrice }]" size="small"
                                        border>
                                        <el-table-column prop="name" label="票种" />
                                        <el-table-column label="价格"><template #default="{ row }">￥{{ row.price ?? 0
                                                }}</template></el-table-column>
                                    </el-table>
                                </template>
                            </div>
                            <div class="config-col">
                                <div class="section-title">🕒 场次名额分配</div>
                                <el-table :data="parseJson(currentApp.timeSlotConfig)" size="small" border>
                                    <el-table-column prop="name" label="场次" />
                                    <el-table-column label="时段">
                                        <template #default="{ row }">{{ row.timeRange ? row.timeRange.join('-') : '-'
                                            }}</template>
                                    </el-table-column>
                                    <el-table-column prop="quota" label="名额" width="60" />
                                </el-table>
                            </div>
                        </div>

                        <!-- <div class="section-title">🖼️ 拟借调文物清单 (点击查看档案)</div>
                        <div class="artifact-grid">
                            <div v-for="relic in currentArtifacts" :key="relic.artifactId"
                                class="artifact-box clickable" @click="showRelicDetail(relic)">
                                <el-image :src="relic.imageUrl" fit="cover" class="relic-thumb" />
                                <div class="relic-info">
                                    <p class="name">{{ relic.artifactName }}</p>
                                    <el-tag :type="relic.status === 0 ? 'success' : 'danger'" size="small">
                                        {{ relic.status === 0 ? '在库' : '外借中' }}
                                    </el-tag>
                                </div>
                            </div>
                        </div> -->
                        <div class="section-title">🖼️ 文物借用审批</div>
                        <div v-for="(group, museum) in groupedArtifacts" :key="museum" style="margin-bottom:16px">
                            <div
                                style="display:flex;align-items:center;justify-content:space-between;margin-bottom:8px">
                                <span style="font-weight:bold;color:#666">🏛️ {{ museum }}</span>
                                <div v-if="currentApp.status === 0">
                                    <el-button size="small" type="success" plain
                                        :disabled="!selectedLoans[museum]?.length" @click="batchAudit(museum, 1)">
                                        批量同意({{ selectedLoans[museum]?.length || 0 }})
                                    </el-button>
                                    <el-button size="small" type="danger" plain
                                        :disabled="!selectedLoans[museum]?.length" @click="batchAudit(museum, 2)"
                                        style="margin-left:8px">
                                        批量拒绝({{ selectedLoans[museum]?.length || 0 }})
                                    </el-button>
                                </div>
                            </div>
                            <el-table :data="group" size="small" border
                                @selection-change="(val) => selectedLoans[museum] = val">
                                <el-table-column type="selection" width="45"
                                    :selectable="(row) => row.isFinalized === 0" />
                                <el-table-column label="文物名称">
                                    <template #default="{ row }">
                                        <span class="artifact-link" @click="showArtifactDetail(row.artifactId)">
                                            {{ row.artifactName }}
                                        </span>
                                    </template>
                                </el-table-column>
                                <el-table-column label="状态" width="90">
                                    <template #default="{ row }">
                                        <el-tag :type="loanStatusTag(row.status)" size="small">{{
                                            loanStatusText(row.status) }}</el-tag>
                                    </template>
                                </el-table-column>
                                <el-table-column prop="auditRemark" label="审批意见" />
                                <el-table-column label="操作" width="160" v-if="currentApp.status === 0">
                                    <template #default="{ row }">
                                        <template v-if="row.isFinalized === 0">
                                            <template v-if="row.status === 0">
                                                <el-button size="small" type="success"
                                                    @click="auditLoan(row, 1)">同意</el-button>
                                                <el-button size="small" type="danger"
                                                    @click="auditLoan(row, 2)">拒绝</el-button>
                                            </template>
                                            <template v-else>
                                                <span style="font-size:12px;color:#999">已处理（未签发可改）</span>
                                                <el-button size="small" @click="auditLoan(row, 0)"
                                                    style="margin-left:6px">撤回</el-button>
                                            </template>
                                        </template>
                                        <span v-else style="font-size:12px;color:#999">已签发</span>
                                    </template>
                                </el-table-column>
                            </el-table>
                        </div>

                        <div class="section-title">📝 方案详细说明</div>
                        <div class="doc-content">{{ currentApp.content }}</div>

                        <div v-if="currentApp.status === 0" class="audit-action-zone">
                            <el-divider>专家评审意见</el-divider>
                            <el-input v-model="auditForm.remark" type="textarea" :rows="3" placeholder="请输入审批批复意见..." />
                            <div class="btn-group">
                                <el-button type="danger" plain @click="submitAudit(2)">驳回申请</el-button>
                                <el-button type="success" @click="submitAudit(1)"
                                    :disabled="isCurrentMuseumFinalized">签发准予</el-button>
                                <el-button @click="exportPDF" :loading="exporting" plain>📄 导出PDF</el-button>
                            </div>
                        </div>
                        <div v-else class="audit-result">
                            <el-divider>审批结果</el-divider>
                            <div :class="['result-box', currentApp.status === 1 ? 'pass' : 'reject']">
                                <p><strong>审批结论：</strong>{{ getStatusText(currentApp.status) }}</p>
                                <p><strong>批复意见：</strong>{{ currentApp.auditRemark || '无' }}</p>
                            </div>
                            <el-button @click="exportPDF" :loading="exporting" plain style="margin-top:12px">📄
                                导出PDF</el-button>
                        </div>
                    </div>
                </el-card>
            </el-col>
        </el-row>

        <el-dialog v-model="relicDialogVisible" title="文物溯源档案" width="550px" append-to-body>
            <div v-if="selectedRelic" class="relic-detail-card">
                <el-image :src="selectedRelic.imageUrl" style="width: 100%; border-radius: 8px; max-height: 300px;"
                    fit="contain" />
                <div class="relic-file-content">
                    <h3 class="detail-title">{{ selectedRelic.artifactName }}</h3>
                    <el-descriptions :column="2" border size="small">
                        <el-descriptions-item label="作者">{{ selectedRelic.author || '佚名' }}</el-descriptions-item>
                        <el-descriptions-item label="朝代">{{ selectedRelic.dynasty }}</el-descriptions-item>
                        <el-descriptions-item label="类别">{{ selectedRelic.category }}</el-descriptions-item>
                        <el-descriptions-item label="状态">
                            <el-tag :type="getStatusColor(selectedRelic.status)" size="small">
                                {{ getArtifactStatusText(selectedRelic.status) }}
                            </el-tag>
                        </el-descriptions-item>
                        <el-descriptions-item label="原属馆" :span="2">{{ selectedRelic.storageLocation
                            }}</el-descriptions-item>
                        <el-descriptions-item label="当前位置" :span="2">{{ selectedRelic.currentLocation || '正在核实'
                            }}</el-descriptions-item>
                    </el-descriptions>
                    <div class="detail-desc-box">
                        <p class="desc-label">文物详情描述：</p>
                        <div class="desc-text">{{ selectedRelic.description || '暂无详细背景描述' }}</div>
                    </div>
                </div>
            </div>
        </el-dialog>
        <el-dialog v-model="artifactDetailVisible" title="文物详细信息" width="480px" append-to-body>
            <div v-if="artifactDetail" style="padding:10px">
                <el-image :src="artifactDetail.imageUrl"
                    style="width:100%;max-height:220px;border-radius:8px;margin-bottom:14px" fit="contain" />
                <el-descriptions :column="2" border size="small">
                    <el-descriptions-item label="文物名称" :span="2">{{ artifactDetail.artifactName
                        }}</el-descriptions-item>
                    <el-descriptions-item label="作者">{{ artifactDetail.author || '佚名' }}</el-descriptions-item>
                    <el-descriptions-item label="朝代">{{ artifactDetail.dynasty || '—' }}</el-descriptions-item>
                    <el-descriptions-item label="分类">{{ artifactDetail.category || '—' }}</el-descriptions-item>
                    <el-descriptions-item label="状态">
                        <el-tag :type="['success', 'warning', 'info'][artifactDetail.status]" size="small">
                            {{ ['馆藏中', '外借巡展中', '维修保养中'][artifactDetail.status] }}
                        </el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="存放位置" :span="2">{{ artifactDetail.storageLocation || '—'
                        }}</el-descriptions-item>
                    <el-descriptions-item label="当前位置" :span="2">{{ artifactDetail.currentLocation || '原馆'
                        }}</el-descriptions-item>
                </el-descriptions>
                <div
                    style="margin-top:12px;background:#f9f9f9;padding:10px;border-radius:6px;font-size:13px;color:#666;line-height:1.7">
                    {{ artifactDetail.description || '暂无描述' }}
                </div>
            </div>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, onMounted, computed, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const applicationList = ref([])
const currentApp = ref(null)
const currentArtifacts = ref([])
const auditForm = ref({ remark: '' })
const relicDialogVisible = ref(false)
const selectedRelic = ref(null)
const loanRequests = ref([])
const userBelong = JSON.parse(sessionStorage.getItem('user') || '{}').belong || ''
const selectedLoans = reactive({}) // 存各博物馆的勾选项
const finalizedAppIds = ref([])
const artifactDetailVisible = ref(false)
const artifactDetail = ref(null)

const exporting = ref(false)

const exportPDF = async () => {
    exporting.value = true
    try {
        await loadScript('https://cdnjs.cloudflare.com/ajax/libs/html2canvas/1.4.1/html2canvas.min.js')
        await loadScript('https://cdnjs.cloudflare.com/ajax/libs/jspdf/2.5.1/jspdf.umd.min.js')

        const element = document.querySelector('.doc-wrapper')
        const canvas = await window.html2canvas(element, { scale: 2, useCORS: true })
        const imgData = canvas.toDataURL('image/png')

        const { jsPDF } = window.jspdf
        const pdf = new jsPDF('p', 'mm', 'a4')
        const pageWidth = pdf.internal.pageSize.getWidth()
        const pageHeight = pdf.internal.pageSize.getHeight()
        const imgWidth = pageWidth
        const imgHeight = (canvas.height * imgWidth) / canvas.width

        let heightLeft = imgHeight
        let position = 0
        pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight)
        heightLeft -= pageHeight

        while (heightLeft > 0) {
            position -= pageHeight
            pdf.addPage()
            pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight)
            heightLeft -= pageHeight
        }

        pdf.save(`巡展审批件-${currentApp.value?.title || currentApp.value?.appId}.pdf`)
    } catch (e) {
        ElMessage.error('导出失败')
        console.error(e)
    } finally {
        exporting.value = false
    }
}

const loadScript = (src) => new Promise((resolve, reject) => {
    if (document.querySelector(`script[src="${src}"]`)) return resolve()
    const s = document.createElement('script')
    s.src = src
    s.onload = resolve
    s.onerror = reject
    document.head.appendChild(s)
})

const showArtifactDetail = async (artifactId) => {
    const res = await axios.get(`/api/artifact/${artifactId}`)
    if (res.data.code === 200) {
        artifactDetail.value = res.data.data
        artifactDetailVisible.value = true
    }
}

const isCurrentMuseumFinalized = computed(() =>
    loanRequests.value
        .filter(r => r.ownerMuseum === userBelong)
        .every(r => r.isFinalized === 1)
)

// 批量审批
const batchAudit = async (museum, status) => {
    const selected = selectedLoans[museum] || []
    if (selected.length === 0) return ElMessage.warning('请先勾选文物')

    const label = status === 1 ? '同意' : '拒绝'
    await ElMessageBox.confirm(`确认批量${label}已选 ${selected.length} 件文物？`, '批量审批')

    await axios.post('/api/loan/batchAudit', {
        appId: currentApp.value.appId,
        ownerMuseum: museum,
        status,
        reqIds: selected.map(r => r.reqId), // 传选中的ID
        remark: status === 2 ? '批量拒绝' : '批量同意'
    })
    ElMessage.success('操作成功')
    loadLoanRequests(currentApp.value.appId)
}


const groupedArtifacts = computed(() => {
    const map = {}
    loanRequests.value
        .filter(r => r.ownerMuseum === userBelong) // 只显示本馆的
        .forEach(r => {
            if (!map[r.ownerMuseum]) map[r.ownerMuseum] = []
            map[r.ownerMuseum].push(r)
        })
    return map
})

const loadLoanRequests = async (appId) => {
    const res = await axios.get('/api/loan/summary', { params: { appId } })
    if (res.data.code === 200) loanRequests.value = res.data.data
}

const auditLoan = async (row, status) => {
    // ✨ 撤回直接提交，不弹输入框
    if (status === 0) {
        const res = await axios.post('/api/loan/audit', { reqId: row.reqId, status, remark: '' })
        if (res.data.code === 200) {
            ElMessage.success('已撤回')
            loadLoanRequests(currentApp.value.appId)
        }
        return
    }
    const { value: remark } = await ElMessageBox.prompt(
        status === 1 ? '同意借出，可填写备注' : '请填写拒绝原因',
        status === 1 ? '同意借出' : '拒绝借出',
        { confirmButtonText: '确认', cancelButtonText: '取消', inputPlaceholder: '备注（选填）' }
    )
    const res = await axios.post('/api/loan/audit', { reqId: row.reqId, status, remark })
    if (res.data.code === 200) {
        ElMessage.success(res.data.data)
        loadLoanRequests(currentApp.value.appId)
    }
}

const loanStatusText = (s) => ['待审批', '已同意', '已拒绝'][s] ?? '未知'
const loanStatusTag = (s) => ['warning', 'success', 'danger'][s] ?? ''

const parseJson = (val) => {
    if (!val) return []
    if (typeof val !== 'string') return Array.isArray(val) ? val : []
    try {
        const parsed = JSON.parse(val)
        return Array.isArray(parsed) ? parsed : []
    } catch (e) { return [] }
}

const fetchList = async () => {
    try {
        const res = await axios.get('/api/exhibition/list/byMuseum', {
            params: { ownerMuseum: userBelong }
        })
        if (res.data.code === 200) {
            applicationList.value = res.data.data
            checkFinalizedApps(res.data.data) // ✨ 加这行
        }
    } catch (e) {
        ElMessage.error('获取列表失败')
    }
}
// 在 fetchList 后调用
const checkFinalizedApps = async (list) => {
    const ids = []
    for (const app of list) {
        const res = await axios.get('/api/loan/summary', { params: { appId: app.appId } })
        if (res.data.code === 200) {
            const myLoans = res.data.data.filter(r => r.ownerMuseum === userBelong)
            if (myLoans.length > 0 && myLoans.every(r => r.isFinalized === 1)) {
                ids.push(app.appId)
            }
        }
    }
    finalizedAppIds.value = ids
}
const isFinalizedApp = (appId) => finalizedAppIds.value.includes(appId)
onMounted(fetchList)

const loadDetail = async (id) => {
    try {
        const res = await axios.get(`/api/exhibition/application/detail/${id}`)
        if (res.data.code === 200) {
            currentApp.value = res.data.data.application
            currentArtifacts.value = res.data.data.artifacts
            auditForm.value.remark = ''
            loadLoanRequests(id) // ✨ 加这行
        }
    } catch (e) { ElMessage.error("加载详情失败") }
}

const showRelicDetail = (relic) => {
    selectedRelic.value = relic
    relicDialogVisible.value = true
}

const submitAudit = async (status) => {
    try {
        await ElMessageBox.confirm('确定提交该审批结论吗？', '提示')

        const pendingLoans = loanRequests.value.filter(
            r => r.ownerMuseum === userBelong && r.status === 0
        )
        if (pendingLoans.length > 0) {
            await ElMessageBox.confirm(
                `还有 ${pendingLoans.length } 件文物尚未审批，确认签发后将自动拒绝这些文物，是否继续？`,
                '提示',
                { confirmButtonText: '继续签发', cancelButtonText: '返回审批', type: 'warning' }
            )
            await axios.post('/api/loan/batchAudit', {
                appId: currentApp.value.appId,
                ownerMuseum: userBelong,
                status: 2,
                remark: '签发时自动拒绝'
            })
        }

        // ✨ 只做签发，不直接改申请单状态
        const finalizeRes = await axios.post('/api/loan/finalize', null, {
            params: { appId: currentApp.value.appId, ownerMuseum: userBelong }
        })

        if (finalizeRes.data.code === 200) {
            ElMessage.success(finalizeRes.data.data)
            fetchList()
            loadDetail(currentApp.value.appId)
        }
    } catch (e) {
        if (e !== 'cancel') ElMessage.error('操作异常')
    }
}

const pendingCount = computed(() => applicationList.value.filter(i => i.status === 0).length)
const getStatusText = (s) => ['展览待发布', '展览已发布', '不予通过'][s] || '未知'
const getStatusTag = (s) => ['info', 'success', 'danger'][s] || ''
const getArtifactStatusText = (s) => ['馆藏中', '外借巡展中', '维修保养中', '禁止外借'][s] || '未知'
const getStatusColor = (s) => ['success', 'danger', 'warning', 'info'][s] || ''
const formatDate = (d) => d ? d.replace('T', ' ').substring(0, 16) : '-'
</script>

<style scoped>
.audit-container {
    padding: 20px;
    background: #f4f7f9;
    min-height: 100vh;
}

.list-card {
    height: 750px;
}

.app-item {
    padding: 15px;
    border-bottom: 1px solid #f0f0f0;
    cursor: pointer;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.app-item.is-active {
    background: #ecf5ff;
    border-right: 4px solid #409eff;
}

.app-title {
    font-weight: bold;
    margin-bottom: 5px;
}

.app-time {
    font-size: 12px;
    color: #999;
}

.doc-wrapper {
    background: #fff;
    padding: 30px;
    border: 1px solid #dcdfe6;
    position: relative;
}

.doc-poster {
    position: absolute;
    right: 30px;
    top: 80px;
    z-index: 5;
}

.poster-img {
    width: 120px;
    height: 160px;
    border: 1px solid #333;
    box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.1);
}

.doc-header {
    text-align: center;
    border-bottom: 2px solid #ff4d4f;
    padding-bottom: 15px;
    margin-bottom: 20px;
}

.doc-header h1 {
    color: #ff4d4f;
    letter-spacing: 4px;
    margin: 0;
}

.doc-table {
    width: 100%;
    border-collapse: collapse;
    margin-bottom: 20px;
}

.doc-table td {
    border: 1px solid #333;
    padding: 10px;
    font-size: 14px;
}

.doc-table td.label {
    background: #fdf2f2;
    width: 100px;
    font-weight: bold;
    text-align: center;
}

.section-title {
    font-weight: bold;
    margin: 20px 0 10px;
    border-left: 4px solid #ff4d4f;
    padding-left: 10px;
}

.artifact-grid {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
}

/* 缩小缩略图卡片 */
.artifact-box {
    width: 90px;
    border: 1px solid #eee;
    padding: 5px;
    text-align: center;
    background: #fff;
    transition: 0.2s;
}

.artifact-box.clickable {
    cursor: pointer;
}

.artifact-box.clickable:hover {
    border-color: #409eff;
    transform: scale(1.02);
}

.relic-thumb {
    width: 100%;
    height: 80px;
    border-radius: 4px;
}

.relic-info .name {
    margin: 5px 0;
    font-size: 11px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.doc-content {
    line-height: 1.6;
    background: #fcfcfc;
    padding: 15px;
    border-radius: 4px;
    min-height: 60px;
    border: 1px solid #eee;
    white-space: pre-wrap;
}

/* 详情弹窗增强样式 */
.relic-file-content {
    margin-top: 15px;
}

.detail-title {
    text-align: center;
    margin-bottom: 15px;
    color: #2c3e50;
}

.detail-desc-box {
    margin-top: 15px;
    padding: 10px;
    background: #f8f9fb;
    border-radius: 4px;
}

.desc-label {
    font-weight: bold;
    font-size: 14px;
    margin-bottom: 5px;
    color: #606266;
}

.desc-text {
    font-size: 13px;
    color: #666;
    line-height: 1.6;
}

.audit-action-zone {
    margin-top: 30px;
}

.btn-group {
    display: flex;
    justify-content: flex-end;
    gap: 15px;
    margin-top: 15px;
}

.result-box {
    padding: 15px;
    border-radius: 8px;
}

.result-box.pass {
    background: #f0f9eb;
    border: 1px solid #c2e7b0;
    color: #67c23a;
}

.result-box.reject {
    background: #fef0f0;
    border: 1px solid #fde2e2;
    color: #f56c6c;
}
.artifact-link {
    color: #409eff;
    cursor: pointer;
    text-decoration: underline;
}
</style>