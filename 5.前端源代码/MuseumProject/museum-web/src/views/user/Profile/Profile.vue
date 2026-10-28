<template>
    <div class="profile-wrapper">
        <div class="profile-container">
            <div class="stats-row">
                <div class="stat-item">
                    <div class="stat-value">{{ artifactCount }}</div>
                    <div class="stat-label">珍藏文物</div>
                </div>
                <div class="stat-divider"></div>
                <div class="stat-item">
                    <div class="stat-value">{{ exhibitionCount }}</div>
                    <div class="stat-label">关注巡展</div>
                </div>
                <div class="stat-divider"></div>
                <div class="stat-item">
                    <div class="stat-value">馆友</div>
                    <div class="stat-label">欢迎</div>
                </div>
            </div>

            <el-row :gutter="30">
                <el-col :lg="8" :md="10" :xs="24">
                    <el-card shadow="never" class="avatar-card">
                        <div class="avatar-section">
                            <div class="avatar-upload-wrapper" @click="triggerUpload" title="点击上传头像">
                                <el-avatar :size="140" :src="userForm.avatar || defaultAvatar" />
                                <div class="avatar-mask">
                                    <el-icon>
                                        <Camera />
                                    </el-icon>
                                    <span>更换头像</span>
                                </div>
                            </div>
                            <input type="file" ref="fileInput" style="display: none" accept="image/*"
                                @change="onFileChange" />

                            <h2 class="profile-nickname">{{ userForm.nickname || '未命名馆友' }}</h2>
                            <el-tag size="small" effect="plain" type="info">账号: {{ userForm.username }}</el-tag>
                        </div>
                    </el-card>
                </el-col>

                <el-col :lg="16" :md="14" :xs="24">
                    <el-card shadow="never" class="settings-card">
                        <el-tabs v-model="activeTab" class="profile-tabs">
                            <el-tab-pane label="基本资料" name="base">
                                <el-form :model="userForm" :rules="profileRules" ref="profileFormRef"
                                    label-position="top" class="custom-form">
                                    <el-form-item label="登录账号 (不可修改)">
                                        <el-input v-model="userForm.username" disabled />
                                    </el-form-item>
                                    <el-form-item label="我的昵称">
                                        <el-input v-model="userForm.nickname" placeholder="请输入昵称" clearable />
                                    </el-form-item>
                                    <el-form-item label="电子邮箱" prop="email">
                                        <el-input v-model="userForm.email" placeholder="暂未绑定邮箱 (可选)" clearable />
                                    </el-form-item>
                                    <el-form-item label="手机号码" prop="phone">
                                        <el-input v-model="userForm.phone" placeholder="暂未绑定手机 (可选)" clearable />
                                    </el-form-item>

                                    <div class="form-actions">
                                        <el-button round
                                            style="background-color:#7e5c3b; border-color:#7e5c3b; color:#fff;"
                                            @click="handleUpdateProfile" :loading="btnLoading">保存资料修改</el-button>
                                    </div>
                                </el-form>
                            </el-tab-pane>

                            <el-tab-pane label="账号安全" name="security">
                                <el-form :model="pwdForm" label-position="top" class="custom-form">
                                    <el-form-item label="原始密码">
                                        <el-input v-model="pwdForm.oldPassword" type="password" show-password
                                            placeholder="请输入当前密码" />
                                    </el-form-item>
                                    <el-form-item label="新密码">
                                        <el-input v-model="pwdForm.newPassword" type="password" show-password
                                            placeholder="请输入新密码 (至少6位)" />
                                    </el-form-item>
                                    <el-form-item label="确认新密码">
                                        <el-input v-model="pwdForm.confirmPassword" type="password" show-password
                                            placeholder="请再次输入新密码" />
                                    </el-form-item>

                                    <div class="form-actions">
                                        <el-button type="danger" @click="handleUpdatePassword"
                                            :loading="btnLoading">确认修改密码</el-button>
                                    </div>
                                </el-form>
                            </el-tab-pane>
                            <el-tab-pane label="收货地址" name="address">
                                <div class="addr-header">
                                    <el-button type="primary" @click="openAddrDialog(null)">+ 新增地址</el-button>
                                </div>
                                <div v-if="addrList.length === 0" class="addr-empty">暂无收货地址，点击上方按钮添加</div>
                                <div v-else class="addr-list">
                                    <div v-for="addr in addrList" :key="addr.addrId" class="addr-card"
                                        :class="{ 'is-default': addr.isDefault === 1 }">
                                        <div class="addr-main">
                                            <div class="addr-top">
                                                <span class="addr-receiver">{{ addr.receiver }}</span>
                                                <span class="addr-phone">{{ addr.phone }}</span>
                                                <el-tag v-if="addr.isDefault === 1" type="danger" size="small"
                                                    effect="dark">默认</el-tag>
                                            </div>
                                            <div class="addr-detail">
                                                {{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detail }}
                                            </div>
                                        </div>
                                        <div class="addr-actions">
                                            <el-button v-if="addr.isDefault !== 1" link
                                                @click="setDefault(addr)">设为默认</el-button>
                                            <el-button link type="primary" @click="openAddrDialog(addr)">编辑</el-button>
                                            <el-button link type="danger"
                                                @click="deleteAddr(addr.addrId)">删除</el-button>
                                        </div>
                                    </div>
                                </div>
                            </el-tab-pane>
                            <el-tab-pane label="常用预约人" name="visitor">
                                <div class="addr-header">
                                    <el-button type="primary" @click="openVisitorDialog(null)">+ 新增预约人</el-button>
                                </div>
                                <div v-if="visitorList.length === 0" class="addr-empty">暂无常用预约人，点击上方按钮添加</div>
                                <div v-else class="addr-list">
                                    <div v-for="v in visitorList" :key="v.visitorId" class="addr-card"
                                        :class="{ 'is-default': v.isDefault === 1 }">
                                        <div class="addr-main">
                                            <div class="addr-top">
                                                <span class="addr-receiver">{{ v.realName }}</span>
                                                <span class="addr-phone">{{ v.phone }}</span>
                                                <el-tag v-if="v.isDefault === 1" type="danger" size="small"
                                                    effect="dark">默认</el-tag>
                                            </div>
                                            <div class="addr-detail">身份证：{{ v.idCard }}{{ v.email ? ' · ' + v.email : ''
                                                }}</div>
                                        </div>
                                        <div class="addr-actions">
                                            <el-button v-if="v.isDefault !== 1" link
                                                @click="setVisitorDefault(v)">设为默认</el-button>
                                            <el-button link type="primary" @click="openVisitorDialog(v)">编辑</el-button>
                                            <el-button link type="danger"
                                                @click="deleteVisitor(v.visitorId)">删除</el-button>
                                        </div>
                                    </div>
                                </div>
                            </el-tab-pane>
                        </el-tabs>
                    </el-card>
                </el-col>
            </el-row>
        </div>
        <!-- 地址弹窗 -->
        <el-dialog v-model="addrDialogVisible" :title="addrForm.addrId ? '编辑地址' : '新增地址'" width="480px">
            <el-form :model="addrForm" label-width="80px">
                <el-form-item label="收货人"><el-input v-model="addrForm.receiver" placeholder="请输入收货人姓名" /></el-form-item>
                <el-form-item label="手机号码"><el-input v-model="addrForm.phone" placeholder="请输入手机号" /></el-form-item>
                <el-form-item label="省份"><el-input v-model="addrForm.province" placeholder="如：广东省" /></el-form-item>
                <el-form-item label="城市"><el-input v-model="addrForm.city" placeholder="如：广州市" /></el-form-item>
                <el-form-item label="区/县"><el-input v-model="addrForm.district" placeholder="如：天河区" /></el-form-item>
                <el-form-item label="详细地址"><el-input v-model="addrForm.detail" placeholder="街道、楼栋、门牌号" /></el-form-item>
                <el-form-item label="设为默认">
                    <el-switch v-model="addrForm.isDefault" :active-value="1" :inactive-value="0" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="addrDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="saveAddr">保存</el-button>
            </template>
        </el-dialog>
        <!-- 常用预约人弹窗 -->
        <el-dialog v-model="visitorDialogVisible" :title="visitorForm.visitorId ? '编辑预约人' : '新增预约人'" width="480px">
            <el-form :model="visitorForm" label-width="80px">
                <el-form-item label="真实姓名"><el-input v-model="visitorForm.realName"
                        placeholder="请输入真实姓名" /></el-form-item>
                <el-form-item label="手机号码"><el-input v-model="visitorForm.phone" placeholder="请输入手机号" /></el-form-item>
                <el-form-item label="身份证号"><el-input v-model="visitorForm.idCard"
                        placeholder="请输入18位身份证号" /></el-form-item>
                <el-form-item label="电子邮箱"><el-input v-model="visitorForm.email" placeholder="选填" /></el-form-item>
                <el-form-item label="设为默认">
                    <el-switch v-model="visitorForm.isDefault" :active-value="1" :inactive-value="0" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="visitorDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="saveVisitor">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Camera } from '@element-plus/icons-vue'
import request from '@/utils/request'

const activeTab = ref('base')
const fileInput = ref(null)
const profileFormRef = ref(null)
const btnLoading = ref(false)
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const artifactCount = ref(0)
const exhibitionCount = ref(0)

const userForm = ref({
    userId: '',
    username: '',
    nickname: '',
    avatar: '',
    email: '',
    phone: ''
})

const pwdForm = ref({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
})

// ✨ 正则规则：不填时不校验，填了则必须符合格式
const profileRules = {
    email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
    phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }]
}

const getUserId = () => {
    const userStr = sessionStorage.getItem('user')
    return userStr ? JSON.parse(userStr).userId : null
}

const initProfile = () => {
    const userStr = sessionStorage.getItem('user')
    if (userStr) {
        const data = JSON.parse(userStr)
        userForm.value = {
            userId: data.userId || data.id,
            username: data.username,
            nickname: data.nickname || '',
            avatar: data.avatar || '',
            email: data.email || '',
            phone: data.phone || ''
        }
    }
}

const fetchStats = async () => {
    const userId = getUserId()
    if (!userId) return
    try {
        const resArt = await request.get(`/favorite/artifacts`, { params: { userId } })
        artifactCount.value = (resArt.data || resArt).length
        const resEx = await request.get(`/favorite/my`, { params: { userId } })
        exhibitionCount.value = (resEx.data || resEx).length
    } catch (e) { console.error("统计获取失败") }
}

const triggerUpload = () => fileInput.value.click()
const onFileChange = async (e) => {
    const file = e.target.files[0]
    if (!file) return
    const formData = new FormData()
    formData.append('file', file)
    try {
        ElMessage.info('上传中...')
        const res = await request.post('/upload/image', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
        userForm.value.avatar = res
        ElMessage.success('头像上传成功，保存后生效')
    } catch (error) { ElMessage.error('上传失败') }
}

const handleUpdateProfile = async () => {
    if (!profileFormRef.value) return
    await profileFormRef.value.validate(async (valid) => {
        if (!valid) return ElMessage.error('请检查输入格式')
        btnLoading.value = true
        try {
            await request.put('/user/update', userForm.value)
            const user = JSON.parse(sessionStorage.getItem('user'))
            sessionStorage.setItem('user', JSON.stringify({ ...user, ...userForm.value }))
            ElMessage.success('个人资料已更新')
        } catch (error) { console.error(error) }
        finally { btnLoading.value = false }
    })
}

const handleUpdatePassword = async () => {
    if (!pwdForm.value.oldPassword) return ElMessage.warning('请输入原始密码')
    if (pwdForm.value.newPassword.length < 6) return ElMessage.warning('新密码至少6位')
    if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) {
        return ElMessage.error('两次输入的新密码不一致')
    }

    btnLoading.value = true
    try {
        await request.post('/user/password', {
            userId: userForm.value.userId,
            oldPassword: pwdForm.value.oldPassword,
            newPassword: pwdForm.value.newPassword
        })
        ElMessage.success('密码修改成功')
        pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    } catch (e) { console.error(e) }
    finally { btnLoading.value = false }
}

// ── 收货地址 ──────────────────────────
const addrList = ref([])
const addrDialogVisible = ref(false)
const addrForm = ref({})

const fetchAddr = async () => {
    const userId = getUserId()
    if (!userId) return
    const res = await request.get('/shop/address/list', { params: { userId } })
    console.log('地址接口返回：', res)
    addrList.value = res?.data || res || []
}

const openAddrDialog = (addr) => {
    addrForm.value = addr
        ? { ...addr }
        : { receiver: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: 0 }
    addrDialogVisible.value = true
}

const saveAddr = async () => {
    if (!addrForm.value.receiver || !addrForm.value.phone || !addrForm.value.detail) {
        return ElMessage.warning('请填写收货人、手机号和详细地址')
    }
    addrForm.value.userId = getUserId()
    await request.post('/shop/address/save', addrForm.value)
    ElMessage.success('保存成功')
    addrDialogVisible.value = false
    fetchAddr()
}

const setDefault = async (addr) => {
    await request.post('/shop/address/save', { ...addr, isDefault: 1 })
    ElMessage.success('已设为默认地址')
    fetchAddr()
}

const deleteAddr = async (addrId) => {
    await ElMessageBox.confirm('确定删除该地址？', '提示', { type: 'warning' })
    await request.delete(`/shop/address/${addrId}`)
    ElMessage.success('已删除')
    fetchAddr()
}

// ── 常用预约人 ──────────────────────────
const visitorList = ref([])
const visitorDialogVisible = ref(false)
const visitorForm = ref({})

const fetchVisitors = async () => {
    const userId = getUserId()
    if (!userId) return
    const res = await request.get('/visitor/list', { params: { userId } })
    visitorList.value = res?.data || res || []
}

const openVisitorDialog = (v) => {
    visitorForm.value = v
        ? { ...v }
        : { realName: '', phone: '', idCard: '', email: '', isDefault: 0 }
    visitorDialogVisible.value = true
}

const saveVisitor = async () => {
    if (!visitorForm.value.realName || !visitorForm.value.phone || !visitorForm.value.idCard) {
        return ElMessage.warning('请填写姓名、手机号和身份证号')
    }
    visitorForm.value.userId = getUserId()
    await request.post('/visitor/save', visitorForm.value)
    ElMessage.success('保存成功')
    visitorDialogVisible.value = false
    fetchVisitors()
}

const setVisitorDefault = async (v) => {
    await request.post('/visitor/save', { ...v, isDefault: 1 })
    ElMessage.success('已设为默认预约人')
    fetchVisitors()
}

const deleteVisitor = async (visitorId) => {
    await ElMessageBox.confirm('确定删除该预约人？', '提示', { type: 'warning' })
    await request.delete(`/visitor/${visitorId}`)
    ElMessage.success('已删除')
    fetchVisitors()
}

onMounted(() => {
    initProfile()
    fetchStats()
    fetchAddr()
    fetchVisitors()
})
</script>

<style scoped>
.profile-wrapper {
    min-height: 100vh;
    padding: 30px 20px;
    background-color: #fcfaf8;
}

.profile-container {
    max-width: 1000px;
    margin: 0 auto;
}

.stats-row {
    display: flex;
    align-items: center;
    background: #fff;
    padding: 25px;
    border-radius: 12px;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.03);
    margin-bottom: 30px;
    border: 1px solid #eee;
}

.stat-item {
    flex: 1;
    text-align: center;
}

.stat-divider {
    width: 1px;
    height: 40px;
    background: #eee;
}

.stat-value {
    font-size: 26px;
    font-weight: bold;
    color: #8b4a2a;
    margin-bottom: 4px;
}

.stat-label {
    font-size: 13px;
    color: #999;
}

.avatar-card {
    border-radius: 12px;
    border: none;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.03);
    text-align: center;
    padding: 20px 0;
}

.avatar-upload-wrapper {
    position: relative;
    display: inline-block;
    cursor: pointer;
    border-radius: 50%;
    border: 4px solid #fff;
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
    margin-bottom: 20px;
}

.avatar-mask {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.4);
    border-radius: 50%;
    color: #fff;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    opacity: 0;
    transition: 0.3s;
}

.avatar-upload-wrapper:hover .avatar-mask {
    opacity: 1;
}

.profile-nickname {
    font-size: 20px;
    color: #333;
    margin: 10px 0;
}

.settings-card {
    border-radius: 12px;
    border: none;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.03);
    min-height: 550px;
}

.profile-tabs {
    padding: 10px;
}

.custom-form {
    margin-top: 20px;
}

.form-actions {
    margin-top: 30px;
    border-top: 1px solid #f5f5f5;
    padding-top: 20px;
}

:deep(.el-tabs__item.is-active) {
    color: #8b4a2a;
}

:deep(.el-tabs__active-bar) {
    background-color: #8b4a2a;
}

:deep(.el-form-item__label) {
    font-weight: bold;
    color: #666;
}
.addr-header {
    margin-bottom: 16px;
}

.addr-empty {
    color: #9ca3af;
    text-align: center;
    padding: 40px 0;
    font-size: 14px;
}

.addr-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.addr-card {
    border: 1px solid #e5e7eb;
    border-radius: 10px;
    padding: 14px 16px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    transition: border-color 0.2s;
}

.addr-card.is-default {
    border-color: #ef4444;
    background: #fff5f5;
}

.addr-top {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 6px;
}

.addr-receiver {
    font-weight: 700;
    font-size: 15px;
    color: #111827;
}

.addr-phone {
    font-size: 14px;
    color: #6b7280;
}

.addr-detail {
    font-size: 13px;
    color: #4b5563;
}

.addr-actions {
    display: flex;
    gap: 4px;
    flex-shrink: 0;
}
</style>
