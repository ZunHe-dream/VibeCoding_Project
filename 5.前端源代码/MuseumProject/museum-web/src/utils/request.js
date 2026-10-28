import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
    baseURL: '/api', // 这里对应 vite.config.js 里的代理路径
    timeout: 5000
})

// 响应拦截器：帮你把后端包装的 Result 拆开
request.interceptors.response.use(
    response => {
        const res = response.data
        // 如果后端返回的状态码不是 200，就弹窗报错
        if (res.code !== 200) {
            ElMessage.error(res.msg || '服务器错误')
            return Promise.reject(new Error(res.msg || 'Error'))
        } else {
            return res.data // 直接返回真正的用户数据对象
        }
    },
    error => {
        ElMessage.error('网络连接失败，请检查后端服务')
        return Promise.reject(error)
    }
)

export default request