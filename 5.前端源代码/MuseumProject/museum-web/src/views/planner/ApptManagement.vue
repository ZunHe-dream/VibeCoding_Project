<template>
    <div class="detail-container">
        <el-breadcrumb separator="/">
            <el-breadcrumb-item>预约管理</el-breadcrumb-item>
            <el-breadcrumb-item>参观预约名单</el-breadcrumb-item>
        </el-breadcrumb>

        <!-- 顶部统计卡片 -->
        <div class="stats-grid">
            <div class="stat-card blue">
                <span class="stat-label">总预约数</span>
                <span class="stat-value">{{ appointments.length }}</span>
            </div>
            <div class="stat-card yellow">
                <span class="stat-label">待核销</span>
                <span class="stat-value">{{ pendingCount }}</span>
            </div>
            <div class="stat-card green">
                <span class="stat-label">已核验</span>
                <span class="stat-value">{{ completedCount }}</span>
            </div>
        </div>

        <!-- 数据列表 -->
        <div style="display:flex; justify-content:flex-end; margin-bottom:12px;">
            <el-button type="success" @click="exportAppointments">导出 Excel</el-button>
        </div>
        <div class="table-wrapper">
            <table class="custom-table">
                <thead>
                    <tr>
                        <th>预约人信息</th>
                        <th>预约巡展项目</th>
                        <th>时段/票种</th>
                        <th>状态</th>
                        <th>管理操作</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="item in appointments" :key="item.resId">
                        <!-- 对应 VO 中的 realName 和 phone -->
                        <td class="user-cell" @click="openDetails(item)">
                            <div class="user-primary">{{ item.realName }}</div>
                            <div class="user-secondary">{{ item.phone }}</div>
                        </td>
                        <!-- 对应 VO 中的 exhibitionTitle -->
                        <td>
                            <div class="ex-name">{{ item.exhibitionTitle }}</div>
                            <div class="museum-name">{{ item.museumName }}</div>
                        </td>
                        <td>
                            <div class="time-slot">{{ item.timeSlot }}</div>
                            <div class="ticket-type">{{ item.ticketType }}</div>
                        </td>
                        <td>
                            <span :class="['badge', getStatusClass(item.status)]">
                                {{ getStatusText(item.status) }}
                            </span>
                        </td>
                        <td>
                            <el-button v-if="item.status === 0" type="primary" size="small"
                                @click="handleVerify(item)">核销</el-button>
                            <span v-else-if="item.status === 1" class="text-done">已完成</span>
                            <span v-else class="text-cancel">已取消</span>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <!-- 详情弹窗 -->
        <el-dialog v-model="isModalOpen" title="预约详情卡片" width="400px" center>
            <div v-if="selectedUser" class="modal-body">
                <div class="detail-row"><span>真实姓名</span><strong>{{ selectedUser.realName }}</strong></div>
                <div class="detail-row"><span>联系电话</span><strong>{{ selectedUser.phone }}</strong></div>
                <div class="detail-row"><span>身份证号</span><strong>{{ selectedUser?.idCard || '未获取到数据' }}</strong></div>
                <el-divider />
                <div class="detail-row"><span>预约巡展</span><strong>{{ selectedUser.exhibitionTitle }}</strong></div>
                <div class="detail-row"><span>参观日期</span><strong>{{ selectedUser.reserveDate }}</strong></div>
                <div class="detail-row"><span>备注信息</span>
                    <p>{{ selectedUser.remark || '无' }}</p>
                </div>
                <el-divider />
                <div class="verify-box" v-if="selectedUser.status === 0">
                    <small>核销码</small>
                    <div class="code-text">{{ selectedUser.verifyCode }}</div>
                </div>
            </div>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { ElMessageBox, ElMessage } from 'element-plus';
import request from '@/utils/request'; // 假设你封装了 axios

const appointments = ref([]);
const selectedUser = ref(null);
const isModalOpen = ref(false);

// 统计逻辑
const pendingCount = computed(() => appointments.value.filter(a => a.status === 0).length);
const completedCount = computed(() => appointments.value.filter(a => a.status === 1).length);

// 获取数据：从后端接口获取当前巡展方的预约记录
const fetchData = async () => {
    try {
        const userInfo = JSON.parse(sessionStorage.getItem('user'));
        const museumName = userInfo?.belong; // 改用 belong 字段

        if (!museumName) {
            ElMessage.warning('未获取到所属机构信息');
            return;
        }

        const res = await request.get(`/reservation/exhibitor/list`, {
            params: { museumName } // 改传 museumName
        });
        console.log('接口响应:', JSON.stringify(res));  // 👈 加这行
        if (Array.isArray(res)) {
            appointments.value = res;
        } else if (res.code === 200) {
            appointments.value = res.data;
        }
    } catch (error) {
        console.error(error);
        ElMessage.error("获取数据失败");
    }
};

// 核销逻辑
const handleVerify = (item) => {
    ElMessageBox.confirm(`确认核销 ${item.realName} 的预约吗？`, '核销提示', { type: 'success' }).then(async () => {
        const res = await request.post('/reservation/verify', { resId: item.resId });
        console.log('核销返回:', JSON.stringify(res)) // 先看看返回值结构
        // 兼容两种结构
        const success = res?.code === 200 || res === '核销成功' || typeof res === 'string'
        if (success) {
            item.status = 1
            ElMessage.success('核销成功！')
            fetchData() // 刷新列表，顺带更新统计数字
        } else {
            ElMessage.error(res?.message || '核销失败')
        }
    })
}

const exportAppointments = () => {
    if (appointments.value.length === 0) return ElMessage.warning('暂无预约数据')

    const statusMap = { 0: '待核销', 1: '已核验', 2: '已取消' }

    const header = ['预约人姓名', '手机号', '身份证', '邮箱', '巡展项目', '所属博物馆', '参观日期', '时段', '票种', '状态']

    const rows = appointments.value.map(a => [
        a.realName || '',
        a.phone || '',
        a.idCard || '',
        a.email || '',
        a.exhibitionTitle || '',
        a.museumName || '',
        a.reserveDate || '',
        a.timeSlot || '',
        a.ticketType || '',
        statusMap[a.status] ?? ''
    ])

    const BOM = '\uFEFF'
    const csv = BOM + [header, ...rows]
        .map(row => row.map(cell => `"${String(cell).replace(/"/g, '""')}"`).join(','))
        .join('\n')

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `预约名单_${new Date().toLocaleDateString()}.csv`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
}

const openDetails = (user) => {
    console.log('--- 详情点击测试 ---');
    console.log('当前点击的整行数据:', user);
    selectedUser.value = user;
    // 3. 再次确认赋值后的响应式变量
    console.log('selectedUser 赋值后的内容:', selectedUser.value);
    // 4. 特别检查身份证字段
    if (!user.idCard) {
        console.warn('⚠️ 警报：这一行数据里没有 idCard 字段！请检查后端是否真的重启了，或者 SQL 别名是否写错。');
    }
    isModalOpen.value = true;
    
};

// 状态文字转换
const getStatusText = (status) => {
    const map = { 0: '待核销', 1: '已核销', 2: '已取消' };
    return map[status];
};

const getStatusClass = (status) => {
    const map = { 0: 'pending', 1: 'completed', 2: 'cancel' };
    return map[status];
};

onMounted(fetchData);
</script>

<style scoped>
/* 容器及基础布局 */
.detail-container {
    max-width: 1100px;
    margin: 30px auto;
    padding: 0 20px;
}

.stats-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 20px;
    margin: 24px 0;
}

.stat-card {
    background: white;
    padding: 20px;
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.stat-card.blue {
    border-left: 4px solid #409eff;
}

.stat-card.yellow {
    border-left: 4px solid #e6a23c;
}

.stat-card.green {
    border-left: 4px solid #67c23a;
}

.stat-label {
    font-size: 14px;
    color: #909399;
}

.stat-value {
    font-size: 24px;
    font-weight: bold;
    display: block;
    margin-top: 5px;
}

/* 表格样式 */
.table-wrapper {
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    overflow: hidden;
}

.custom-table {
    width: 100%;
    border-collapse: collapse;
}

.custom-table th {
    background: #f5f7fa;
    padding: 15px;
    text-align: left;
    color: #606266;
}

.custom-table td {
    padding: 15px;
    border-bottom: 1px solid #ebeef5;
}

.user-primary {
    color: #409eff;
    font-weight: bold;
    cursor: pointer;
}

.user-secondary {
    font-size: 12px;
    color: #909399;
}

.ex-name {
    font-weight: 500;
}

.museum-name {
    font-size: 12px;
    color: #909399;
}

/* 状态标签 */
.badge {
    padding: 2px 10px;
    border-radius: 10px;
    font-size: 12px;
}

.badge.pending {
    background: #fdf6ec;
    color: #e6a23c;
}

.badge.completed {
    background: #f0f9eb;
    color: #67c23a;
}

.badge.cancel {
    background: #fef0f0;
    color: #f56c6c;
}

/* 详情对话框 */
.detail-row {
    display: flex;
    justify-content: space-between;
    margin-bottom: 12px;
}

.detail-row span {
    color: #909399;
}

.verify-box {
    background: #f4f4f5;
    padding: 15px;
    text-align: center;
    border-radius: 8px;
}

.code-text {
    font-size: 20px;
    font-family: monospace;
    color: #409eff;
    font-weight: bold;
}
</style>