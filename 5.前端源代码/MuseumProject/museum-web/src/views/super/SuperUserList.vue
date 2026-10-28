<template>
    <div class="super-users">
        <div class="page-header">
            <div class="header-left">
                <div class="header-tag">SUPER ADMIN · 全局用户管理</div>
                <h1 class="page-title">用户全量档案</h1>
            </div>
            <el-button class="add-btn" @click="openForm()">+ 新增用户</el-button>
        </div>

        <!-- 统计 -->
        <div class="stat-row">
            <div class="stat-card" v-for="s in stats" :key="s.label">
                <div class="stat-num" :style="{ color: s.color }">{{ s.num }}</div>
                <div class="stat-label">{{ s.label }}</div>
            </div>
        </div>

        <!-- 筛选 -->
        <div class="filter-bar">
            <el-input v-model="filters.keyword" placeholder="搜索用户名/姓名…" clearable style="width:200px" @input="fetchList"
                @clear="fetchList" />
            <el-select v-model="filters.role" placeholder="角色筛选" clearable style="width:130px" @change="fetchList">
                <el-option label="普通用户" value="USER" />
                <el-option label="策展方" value="APPLICANT" />
                <el-option label="管理员" value="ADMIN" />
                <el-option label="商品管理员" value="SHOP_ADMIN" />
            </el-select>
            <el-input v-model="filters.belong" placeholder="所属单位…" clearable style="width:160px" @input="fetchList"
                @clear="fetchList" />
            <el-button @click="resetFilters">重置</el-button>
        </div>

        <!-- 表格 -->
        <div class="table-wrap" v-loading="loading">
            <el-table :data="list" border size="small">
                <el-table-column prop="userId" label="ID" width="60" align="center" />
                <el-table-column label="用户信息" min-width="160">
                    <template #default="{ row }">
                        <div class="user-cell">
                            <div class="user-avatar">{{ row.nickname?.slice(0, 1) || row.username?.slice(0, 1) }}</div>
                            <div>
                                <div class="user-name">{{ row.nickname || row.username }}</div>
                                <div class="user-uname">@{{ row.username }}</div>
                            </div>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column label="角色" width="110" align="center">
                    <template #default="{ row }">
                        <el-tag :type="roleTagType(row.role)" size="small">{{ roleText(row.role) }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="belong" label="所属单位" width="140" show-overflow-tooltip />
                <el-table-column prop="phone" label="手机号" width="120" />
                <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
                <el-table-column label="注册时间" width="150">
                    <template #default="{ row }">{{ row.createTime?.slice(0, 10) }}</template>
                </el-table-column>
                <el-table-column label="操作" width="120" fixed="right" align="center">
                    <template #default="{ row }">
                        <el-button link size="small" @click="openForm(row)">编辑</el-button>
                        <el-button link size="small" type="danger" @click="deleteUser(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
            <div class="table-footer">共 {{ list.length }} 名用户</div>
        </div>

        <!-- 新增/编辑弹窗 -->
        <el-dialog v-model="formVisible" :title="formData.userId ? '编辑用户' : '新增用户'" width="500px" destroy-on-close>
            <el-form :model="formData" :rules="rules" ref="formRef" label-width="80px">
                <el-form-item label="用户名" prop="username">
                    <el-input v-model="formData.username" :disabled="!!formData.userId" placeholder="登录账号" />
                </el-form-item>
                <el-form-item label="姓名/昵称" prop="nickname">
                    <el-input v-model="formData.nickname" placeholder="显示名称" />
                </el-form-item>
                <el-form-item v-if="!formData.userId" label="初始密码" prop="password">
                    <el-input v-model="formData.password" type="password" show-password placeholder="设置初始密码" />
                </el-form-item>
                <el-form-item label="角色" prop="role">
                    <el-select v-model="formData.role" style="width:100%" @change="onRoleChange">
                        <el-option label="普通用户" value="USER" />
                        <el-option label="策展方" value="APPLICANT" />
                        <el-option label="管理员" value="ADMIN" />
                        <el-option label="商品管理员" value="SHOP_ADMIN" />
                    </el-select>
                </el-form-item>
                <el-form-item label="所属单位" prop="belong">
                    <el-input v-model="formData.belong" :disabled="formData.role === 'USER'"
                        :placeholder="formData.role === 'USER' ? '普通用户固定为`用户`</el-input>' : '请输入所属博物馆或机构'"/>
        </el-form-item>
        <el-form-item label=" 手机号">
                        <el-input v-model="formData.phone" placeholder="选填" />
                </el-form-item>
                <el-form-item label="邮箱">
                    <el-input v-model="formData.email" placeholder="选填" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="formVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="saveUser">
                    {{ formData.userId ? '保存修改' : '确认新增' }}
                </el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const formVisible = ref(false)
const formRef = ref()

const filters = reactive({ keyword: '', role: '', belong: '' })
const formData = reactive({
    userId: null, username: '', nickname: '', password: '',
    role: 'USER', belong: '用户', phone: '', email: ''
})

const rules = computed(() => ({
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    nickname: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
    password: formData.userId ? [] : [{ required: true, message: '请设置初始密码', trigger: 'blur' }],
    role: [{ required: true, message: '请选择角色', trigger: 'change' }],
    belong: formData.role !== 'USER' ? [{ required: true, message: '请输入所属单位', trigger: 'blur' }] : []
}))

const stats = computed(() => [
    { label: '全部用户', num: list.value.length, color: '#2c1a0e' },
    { label: '普通用户', num: list.value.filter(u => u.role === 'USER').length, color: '#409eff' },
    { label: '策展方', num: list.value.filter(u => u.role === 'APPLICANT').length, color: '#67c23a' },
    { label: '管理员', num: list.value.filter(u => u.role === 'ADMIN').length, color: '#8b1a1a' },
    { label: '商品管理员', num: list.value.filter(u => u.role === 'SHOP_ADMIN').length, color: '#e6a23c' },
])



const fetchList = async () => {
    loading.value = true
    try {
        const res = await axios.get('/api/user/super/list', {
            params: {
                keyword: filters.keyword || undefined,
                role: filters.role || undefined,
                belong: filters.belong || undefined
            }
        })
        if (res.data.code === 200) list.value = res.data.data
    } finally {
        loading.value = false
    }
}
onMounted(fetchList)

const resetFilters = () => {
    filters.keyword = ''; filters.role = ''; filters.belong = ''
    fetchList()
}

const onRoleChange = (role) => {
    if (role === 'USER') formData.belong = '用户'
    else formData.belong = ''
}

const openForm = (u = null) => {
    Object.assign(formData, {
        userId: null, username: '', nickname: '', password: '',
        role: 'USER', belong: '用户', phone: '', email: ''
    })
    if (u) Object.assign(formData, { ...u, password: '' })
    formVisible.value = true
}

const saveUser = async () => {
    try { await formRef.value.validate() } catch { return }
    saving.value = true
    try {
        const res = await axios.post('/api/user/super/save', { ...formData })
        if (res.data.code === 200) {
            ElMessage.success(formData.userId ? '修改成功' : '新增成功')
            formVisible.value = false
            fetchList()
        } else {
            ElMessage.error(res.data.message || '操作失败')
        }
    } finally {
        saving.value = false
    }
}

const deleteUser = async (u) => {
    await ElMessageBox.confirm(
        `确认删除用户【${u.nickname || u.username}】？此操作不可撤销。`,
        '删除确认', { type: 'warning', confirmButtonText: '确认删除', confirmButtonClass: 'el-button--danger' }
    )
    const res = await axios.delete(`/api/user/super/delete/${u.userId}`)
    if (res.data.code === 200) { ElMessage.success('已删除'); fetchList() }
    else ElMessage.error(res.data.message || '删除失败')
}

const roleText = (r) => ({ USER: '普通用户', APPLICANT: '策展方', ADMIN: '管理员', SHOP_ADMIN: '商品管理员', SUPER_ADMIN: '站点管理员' })[r] ?? r
const roleTagType = (r) => ({ USER: '', APPLICANT: 'success', ADMIN: 'danger', SHOP_ADMIN: 'warning', SUPER_ADMIN: 'info' })[r] ?? 'info'
</script>

<style scoped>
.super-users {
    padding: 28px 32px;
    min-height: 100vh;
    background: #faf8f5;
    font-family: 'Noto Serif SC', 'Noto Sans SC', serif;
}

.page-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    margin-bottom: 24px;
}

.header-tag {
    font-size: 11px;
    letter-spacing: 3px;
    color: #8b1a1a;
    margin-bottom: 6px;
}

.page-title {
    font-size: 26px;
    font-weight: 700;
    color: #1a0a0a;
    margin: 0;
}

.add-btn {
    background: #8b1a1a !important;
    color: #fff !important;
    border: none !important;
    border-radius: 8px !important;
    padding: 10px 22px !important;
}

.add-btn:hover {
    background: #a52020 !important;
}

.stat-row {
    display: flex;
    gap: 14px;
    margin-bottom: 24px;
}

.stat-card {
    flex: 1;
    background: #fff;
    border-radius: 10px;
    padding: 16px 20px;
    border: 1px solid #e8dfd0;
    text-align: center;
}

.stat-num {
    font-size: 30px;
    font-weight: 700;
    line-height: 1;
    margin-bottom: 6px;
}

.stat-label {
    font-size: 12px;
    color: #9a7c5a;
}

.filter-bar {
    display: flex;
    gap: 10px;
    align-items: center;
    background: #fff;
    border-radius: 10px;
    padding: 14px 16px;
    margin-bottom: 16px;
    border: 1px solid #e8dfd0;
}

.table-wrap {
    background: #fff;
    border-radius: 10px;
    overflow: hidden;
    border: 1px solid #e8dfd0;
}

.table-footer {
    padding: 12px 16px;
    font-size: 12px;
    color: #9a7c5a;
    text-align: right;
}

.user-cell {
    display: flex;
    align-items: center;
    gap: 10px;
}

.user-avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    flex-shrink: 0;
    background: linear-gradient(135deg, #8b1a1a, #c8a96e);
    color: #fff;
    font-size: 15px;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
}

.user-name {
    font-size: 13px;
    font-weight: 600;
    color: #2c1a0e;
}

.user-uname {
    font-size: 11px;
    color: #9a7c5a;
}
</style>