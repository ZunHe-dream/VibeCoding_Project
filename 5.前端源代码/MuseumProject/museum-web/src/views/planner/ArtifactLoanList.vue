<template>
    <div class="container">
        <el-breadcrumb separator="/">
            <el-breadcrumb-item>巡展管理</el-breadcrumb-item>
            <el-breadcrumb-item>文物借用清单</el-breadcrumb-item>
        </el-breadcrumb>

        <div v-if="loading" class="loading-wrap">
            <el-skeleton :rows="5" animated />
        </div>

        <div v-else-if="groupedList.length === 0" class="empty">
            <el-empty description="暂无文物借用记录" />
        </div>

        <div v-else>
            <!-- 按巡展分组展示 -->
            <div v-for="group in groupedList" :key="group.appId" class="group-card">
                <!-- 巡展标题栏 -->
                <div class="group-header">
                    <div class="group-title">展览名称:  {{ group.exhibitionTitle }}</div>
                    <div class="group-meta">
                        <span>{{ group.startDate }} ~ {{ group.endDate }}</span>
                        <el-tag type="success" size="small" style="margin-left:12px">已通过</el-tag>
                        <span class="artifact-count">共借用 {{ group.artifacts.length }} 件文物</span>
                    </div>
                </div>

                <!-- 文物列表 -->
                <div class="artifact-grid">
                    <div v-for="art in group.artifacts" :key="art.artifactId" class="artifact-card">
                        <div class="artifact-img-wrap">
                            <img :src="art.imageUrl || '/placeholder.png'" class="artifact-img" />
                            <span :class="['status-badge', getArtifactStatusClass(art.artifactStatus)]">
                                {{ getArtifactStatusText(art.artifactStatus) }}
                            </span>
                        </div>
                        <div class="artifact-info">
                            <div class="artifact-name">{{ art.artifactName }}</div>
                            <div class="artifact-meta">{{ art.dynasty }} · {{ art.category }}</div>
                            <div class="artifact-meta">作者：{{ art.author || '佚名' }}</div>
                            <el-divider style="margin: 8px 0" />
                            <div class="artifact-location">
                                <span class="label">原属馆</span>
                                <span>{{ art.storageLocation || '未知' }}</span>
                            </div>
                            <div class="artifact-location">
                                <span class="label">当前位置</span>
                                <span>{{ art.currentLocation || art.storageLocation }}</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import request from '@/utils/request';

const loading = ref(true);
const rawList = ref([]);

const fetchData = async () => {
    try {
        const userInfo = JSON.parse(sessionStorage.getItem('user'));
        const belong = userInfo?.belong;
        if (!belong) {
            ElMessage.warning('未获取到所属机构信息');
            return;
        }
        const res = await request.get('/exhibition/artifact-list', { params: { belong } });
        // 兼容是否有 code 包装
        rawList.value = Array.isArray(res) ? res : (res.data || []);
    } catch (e) {
        console.error(e);
        ElMessage.error('获取数据失败');
    } finally {
        loading.value = false;
    }
};

// 按巡展分组
const groupedList = computed(() => {
    const map = new Map();
    for (const item of rawList.value) {
        if (!map.has(item.appId)) {
            map.set(item.appId, {
                appId: item.appId,
                exhibitionTitle: item.exhibitionTitle,
                startDate: item.startDate,
                endDate: item.endDate,
                artifacts: []
            });
        }
        map.get(item.appId).artifacts.push({
            artifactId: item.artifactId,
            artifactName: item.artifactName,
            author: item.author,
            category: item.category,
            dynasty: item.dynasty,
            imageUrl: item.imageUrl,
            artifactStatus: item.artifactStatus,
            storageLocation: item.storageLocation,
            currentLocation: item.currentLocation
        });
    }
    return Array.from(map.values());
});

const getArtifactStatusText = (status) => {
    const map = { 0: '馆藏中', 1: '巡展中', 2: '维修中' };
    return map[status] ?? '未知';
};

const getArtifactStatusClass = (status) => {
    const map = { 0: 'status-in', 1: 'status-tour', 2: 'status-repair' };
    return map[status] ?? '';
};

onMounted(fetchData);
</script>

<style scoped>
.container {
    max-width: 1100px;
    margin: 30px auto;
    padding: 0 20px;
}

.loading-wrap {
    margin-top: 40px;
}

.empty {
    margin-top: 60px;
}

.group-card {
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
    margin-top: 24px;
    overflow: hidden;
}

.group-header {
    padding: 16px 20px;
    border-bottom: 1px solid #ebeef5;
    background: #f9fafc;
}

.group-title {
    font-size: 16px;
    font-weight: bold;
    color: #303133;
}

.group-meta {
    font-size: 13px;
    color: #909399;
    margin-top: 6px;
    display: flex;
    align-items: center;
}

.artifact-count {
    margin-left: 16px;
    color: #409eff;
}

.artifact-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: 20px;
    padding: 20px;
}

.artifact-card {
    border: 1px solid #ebeef5;
    border-radius: 10px;
    overflow: hidden;
    transition: box-shadow 0.2s;
}

.artifact-card:hover {
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}

.artifact-img-wrap {
    position: relative;
}

.artifact-img {
    width: 100%;
    height: 160px;
    object-fit: cover;
    background: #f5f7fa;
}

.status-badge {
    position: absolute;
    top: 8px;
    right: 8px;
    padding: 2px 8px;
    border-radius: 8px;
    font-size: 11px;
}

.status-in {
    background: #f0f9eb;
    color: #67c23a;
}

.status-tour {
    background: #ecf5ff;
    color: #409eff;
}

.status-repair {
    background: #fdf6ec;
    color: #e6a23c;
}

.artifact-info {
    padding: 12px;
}

.artifact-name {
    font-weight: bold;
    font-size: 15px;
    color: #303133;
}

.artifact-meta {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
}

.artifact-location {
    font-size: 12px;
    color: #606266;
    margin-top: 4px;
    display: flex;
    justify-content: space-between;
}

.label {
    color: #909399;
}
</style>