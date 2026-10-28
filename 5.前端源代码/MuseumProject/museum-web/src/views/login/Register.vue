<template>
    <div class="register-page">
        <div class="bg-overlay"></div>

        <div class="register-wrapper">
            <!-- 左侧品牌区（与登录页完全一致） -->
            <div class="login-brand">
                <div class="brand-content">
                    <div class="logo-circle">🏛️</div>
                    <h1 class="brand-title">智慧博览</h1>
                    <div class="brand-divider"></div>
                    <p class="brand-slogan">识古今之变，鉴人文之光</p>
                </div>
            </div>

            <!-- 右侧注册表单 -->
            <div class="form-area">
                <div class="form-header">
                    <h2>注 册</h2>
                    <p>Register to Heritage System</p>
                </div>

                <el-form :model="form" :rules="rules" ref="formRef" class="custom-form">
                    <el-row :gutter="16">
                        <el-col :span="12">
                            <el-form-item prop="username">
                                <el-input v-model="form.username" placeholder="账号（登录用）" :prefix-icon="User"
                                    class="museum-input" />
                            </el-form-item>
                        </el-col>
                        <el-col :span="12">
                            <el-form-item prop="nickname">
                                <el-input v-model="form.nickname" placeholder="昵称 / 姓名" :prefix-icon="Avatar"
                                    class="museum-input" />
                            </el-form-item>
                        </el-col>
                    </el-row>

                    <el-row :gutter="16">
                        <el-col :span="12">
                            <el-form-item prop="password">
                                <el-input v-model="form.password" type="password" placeholder="设置密码" :prefix-icon="Lock"
                                    show-password class="museum-input" />
                            </el-form-item>
                        </el-col>
                        <el-col :span="12">
                            <el-form-item prop="confirmPassword">
                                <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码"
                                    :prefix-icon="Lock" show-password class="museum-input" />
                            </el-form-item>
                        </el-col>
                    </el-row>

                    <el-form-item prop="belong" style="display:none">
                        <el-input v-model="form.belong" placeholder="所属博物馆（如：广东省博物馆）" :prefix-icon="OfficeBuilding"
                            class="museum-input" />
                    </el-form-item>

                    <el-row :gutter="16">
                        <el-col :span="12">
                            <el-form-item prop="phone">
                                <el-input v-model="form.phone" placeholder="手机号（选填）" :prefix-icon="Phone"
                                    class="museum-input" />
                            </el-form-item>
                        </el-col>
                        <el-col :span="12">
                            <el-form-item prop="email">
                                <el-input v-model="form.email" placeholder="邮箱（选填）" :prefix-icon="Message"
                                    class="museum-input" />
                            </el-form-item>
                        </el-col>
                    </el-row>

                    <el-button type="primary" class="register-btn" @click="handleRegister" :loading="loading">
                        加入文化之旅
                    </el-button>
                </el-form>

                <div class="form-footer">
                    <span style="color:#999; font-size:14px">已有账号？</span>
                    <el-link type="primary" :underline="false" @click="router.push('/login')">立即登录</el-link>
                </div>
            </div>
        </div>

        <div class="museum-footer">
            <p>© 2026 文物巡展管理系统系统 · 典藏文明 传承未来</p>
        </div>
    </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, Avatar, Phone, Message, OfficeBuilding } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
    username: '',
    password: '',
    confirmPassword: '',
    nickname: '',
    belong: '用户',
    phone: '',
    email: '',
    role: 'USER'
})

const validateConfirmPwd = (rule, value, callback) => {
    if (value !== form.password) callback(new Error('两次密码不一致'))
    else callback()
}

const rules = {
    username: [
        { required: true, message: '请输入账号', trigger: 'blur' },
        { min: 3, max: 20, message: '账号长度3~20位', trigger: 'blur' }
    ],
    password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, message: '密码至少6位', trigger: 'blur' }
    ],
    confirmPassword: [
        { required: true, message: '请确认密码', trigger: 'blur' },
        { validator: validateConfirmPwd, trigger: 'blur' }
    ],
    nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
    belong: [{ required: true, message: '请输入所属博物馆', trigger: 'blur' }],
    phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
    email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

const handleRegister = async () => {
    await formRef.value.validate(async (valid) => {
        if (!valid) return
        loading.value = true
        try {
            const { confirmPassword, ...postData } = form
            await request.post('/user/register', postData)
            ElMessage.success('注册成功，请登录')
            router.push('/login')
        } finally {
            loading.value = false
        }
    })
}
</script>

<style scoped>
.register-page {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    background-color: #f4f1de;
    background-image: url('https://www.transparenttextures.com/patterns/paper-fibers.png');
    position: relative;
    overflow: hidden;
}

.bg-overlay {
    position: absolute;
    width: 100%;
    height: 100%;
    background: radial-gradient(circle at center, transparent, rgba(139, 26, 26, 0.05));
    pointer-events: none;
}

.register-wrapper {
    display: flex;
    width: 900px;
    background: #fff;
    border-radius: 20px;
    box-shadow: 0 20px 50px rgba(0, 0, 0, 0.15);
    overflow: hidden;
    z-index: 10;
}

/* 左侧品牌区（与登录页一致） */
.login-brand {
    flex: 0 0 300px;
    background: linear-gradient(135deg, #8b1a1a, #5c1111);
    display: flex;
    justify-content: center;
    align-items: center;
    color: #fff;
}

.brand-content {
    text-align: center;
}

.logo-circle {
    font-size: 50px;
    background: rgba(255, 255, 255, 0.1);
    width: 100px;
    height: 100px;
    line-height: 100px;
    border-radius: 50%;
    margin: 0 auto 20px;
    border: 1px solid rgba(255, 255, 255, 0.2);
}

.brand-title {
    font-size: 32px;
    font-family: "Hiragino Sans GB", "Microsoft YaHei", serif;
    letter-spacing: 4px;
    margin-bottom: 10px;
}

.brand-divider {
    width: 40px;
    height: 3px;
    background: #fdbb2d;
    margin: 15px auto;
}

.brand-slogan {
    font-size: 16px;
    opacity: 0.8;
    letter-spacing: 2px;
}

/* 右侧表单区 */
.form-area {
    flex: 1;
    padding: 44px 52px;
    display: flex;
    flex-direction: column;
    justify-content: center;
}

.form-header {
    margin-bottom: 28px;
}

.form-header h2 {
    font-size: 28px;
    color: #333;
    margin-bottom: 8px;
    font-weight: 500;
}

.form-header p {
    color: #999;
    font-size: 12px;
    letter-spacing: 1px;
}

/* 输入框（与登录页一致） */
.museum-input :deep(.el-input__wrapper) {
    background-color: #f9f9f9;
    box-shadow: none;
    border-bottom: 2px solid #eee;
    border-radius: 0;
    padding: 5px 0;
}

.museum-input :deep(.el-input__wrapper.is-focus) {
    border-bottom-color: #8b1a1a;
}

/* 注册按钮 */
.register-btn {
    width: 100%;
    height: 45px;
    background-color: #8b1a1a !important;
    border-color: #8b1a1a !important;
    font-size: 16px;
    letter-spacing: 4px;
    margin-top: 12px;
    border-radius: 4px;
}

.register-btn:hover {
    background-color: #a52a2a !important;
    box-shadow: 0 4px 12px rgba(139, 26, 26, 0.3);
}

.form-footer {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
    align-items: center;
    gap: 6px;
}

.museum-footer {
    margin-top: 30px;
    color: #8b8b8b;
    font-size: 13px;
    z-index: 10;
}
</style>