 <template>
    <div class="museum-category-wrapper">
        <div class="category-header">
            <div class="title-decoration-left"></div>
            <div class="text-group">
                <h1 class="main-title">文物名录</h1>
                <p class="sub-title">Explore Treasures</p>
            </div>
            <div class="title-decoration-right"></div>
        </div>

        <el-row :gutter="40" class="category-grid" justify="center">
            <el-col :lg="8" :md="12" :sm="12" :xs="24" v-for="cat in categories" :key="cat.id" class="grid-col">
                <div class="category-item" @click="handleCategoryClick(cat.name)"
                    :style="{ '--theme-color': cat.color }">
                    <div class="image-section">
                        <img :src="cat.bg" :alt="cat.name" class="cat-image" />
                        <div class="mask"></div>
                    </div>

                    <div class="info-section">
                        <div class="cat-icon">{{ cat.icon }}</div>
                        <div class="text-content">
                            <h3 class="cat-name">{{ cat.name }}</h3>
                            <p class="cat-desc">{{ cat.desc }}</p>
                        </div>
                        <div class="enter-btn">
                            <el-icon>
                                <ArrowRight />
                            </el-icon>
                        </div>
                    </div>

                    <div class="bottom-bar"></div>
                </div>
            </el-col>
        </el-row>
    </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ArrowRight } from '@element-plus/icons-vue'

const router = useRouter()

// 扩充传统色池，确保 15+ 分类都有漂亮的颜色
const tradColors = {
    dai: '#2e4e7e',   // 黛蓝 (绘画)
    mo: '#2c3e50',    // 墨黑 (法书/碑帖)
    zhu: '#c0392b',   // 朱砂 (生活/漆器)
    hu: '#d35400',    // 琥珀 (金银/珍宝)
    cang: '#27ae60',  // 苍翠 (玉石)
    zi: '#8e44ad',    // 𪾑紫 (织绣)
    xuan: '#34495e',  // 玄色 (雕塑)
    jin: '#b8860b',   // 赭金 (钟表/珐琅)
    bi: '#16a085'     // 碧色 (古籍/文具)
}

const categories = [
    { id: 1, name: '绘画', icon: '🎨', desc: '丹青绘影，气韵生动', bg: '/public/pics/huihua.jpg', color: tradColors.dai },
    { id: 2, name: '法书', icon: '📜', desc: '墨妙笔精，古韵悠长', bg: '/public/pics/fashu.jpg', color: tradColors.mo },
    { id: 3, name: '碑帖', icon: '✍️', desc: '金石刻辞，传拓神采', bg: '/public/pics/beitie.gif', color: tradColors.mo },
    { id: 4, name: '青铜', icon: '🏺', desc: '吉金铸史，重器无声', bg: '/public/pics/qingtongqi.png', color: tradColors.zhu },
    { id: 5, name: '瓷器', icon: '🥣', desc: '华瓷美器，匠心营造', bg: '/public/pics//ciqi.gif', color: tradColors.hu },
    { id: 6, name: '玉石器', icon: '💎', desc: '温润如玉，德音莫违', bg: '/public/pics//yushiqi.gif', color: tradColors.cang },
    { id: 7, name: '织绣', icon: '🧵', desc: '锦绣华章，霓裳羽衣', bg: '/public/pics//zhixiu.gif', color: tradColors.zi },
    { id: 8, name: '珐琅器', icon: '💠', desc: '景泰流光，万紫千红', bg: '/public/pics/falangqi.gif', color: tradColors.jin },
    { id: 9, name: '漆器', icon: '🍷', desc: '髹饰光芒，剔红堆彩', bg: '/public/pics/qiqi.jpg', color: tradColors.zhu },
    { id: 10, name: '文具', icon: '🖋️', desc: '笔墨纸砚，文人风骨', bg: '/public/pics/wenju.gif', color: tradColors.bi },
    { id: 11, name: '古籍文献', icon: '📚', desc: '经史子集，文明薪火', bg: '/public/pics/guji.gif', color: tradColors.bi },
    { id: 12, name: '珍宝', icon: '👑', desc: '珠光宝气，皇室威严', bg: '/public/pics/zhenbao.gif', color: tradColors.hu },
    { id: 13, name: '生活用具', icon: '🍽️', desc: '日常起居，宫廷雅致', bg: '/public/pics/shenghuo.jpg', color: tradColors.zhu },
    { id: 14, name: '钟表仪器', icon: '🕰️', desc: '西洋奇巧，岁月留痕', bg: '/public/pics/zhongbiao.jpg', color: tradColors.jin },
    { id: 15, name: '雕塑', icon: '🗿', desc: '鬼斧神工，立象尽意', bg: 'https://images.unsplash.com/photo-1578301978693-85fa9c0320b9?w=600', color: tradColors.xuan }
]

const handleCategoryClick = (name) => {
    router.push({ path: '/user/relic-list', query: { category: name } })
}
</script>

<style scoped>
.museum-category-wrapper {
    padding: 50px 20px;
    background-color: #f6f3ed;
    background-image: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAQAAAAECAYAAACp8Z5+AAAAIklEQVQIW2NkQAKrVq36z8gAFWNhYfEfi4+P/88IJZgYAAAA//8MAL7cD6EaC3EAAAAASUVORK5CYII=');
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    align-items: center;
    /* 容器内内容整体水平居中 */
}

.category-header {
    display: flex;
    justify-content: center;
    align-items: center;
    margin-bottom: 60px;
    gap: 20px;
    width: 100%;
}

.title-decoration-left,
.title-decoration-right {
    width: 15%;
    max-width: 100px;
    height: 1px;
    background: linear-gradient(to left, #2c3e50, transparent);
}

.title-decoration-right {
    background: linear-gradient(to right, #2c3e50, transparent);
}

.text-group {
    text-align: center;
}

.main-title {
    font-size: 32px;
    color: #2c3e50;
    margin: 0;
    font-weight: 500;
    letter-spacing: 2px;
}

.sub-title {
    color: #95a5a6;
    font-size: 13px;
    letter-spacing: 4px;
    margin-top: 8px;
}

/* 网格宽度限制 */
.category-grid {
    width: 100%;
    max-width: 1200px;
    /* 限制总宽度，防止大屏分得太开 */
}

/* 关键：确保每一列内部的卡片居中 */
.grid-col {
    display: flex;
    justify-content: center;
}

.category-item {
    width: 100%;
    max-width: 380px;
    /* 限制单个卡片最大宽度，求稳 */
    background: white;
    border-radius: 4px;
    overflow: hidden;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    cursor: pointer;
    transition: all 0.3s ease;
    margin-bottom: 40px;
    display: flex;
    flex-direction: column;
}

.image-section {
    position: relative;
    height: 180px;
    overflow: hidden;
}

.cat-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.5s;
}

.mask {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: linear-gradient(to bottom, rgba(255, 255, 255, 0), rgba(255, 255, 255, 1));
}

.info-section {
    padding: 20px;
    display: flex;
    align-items: center;
    gap: 15px;
    z-index: 2;
}

.cat-icon {
    font-size: 24px;
    width: 44px;
    height: 44px;
    background: #fdfdfd;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    border: 1px solid #f0f0f0;
    flex-shrink: 0;
}

.text-content {
    flex-grow: 1;
    text-align: left;
}

.cat-name {
    margin: 0;
    color: var(--theme-color);
    font-size: 19px;
}

.cat-desc {
    color: #7f8c8d;
    font-size: 13px;
    margin-top: 4px;
}

.enter-btn {
    color: #bdc3c7;
}

.bottom-bar {
    height: 2px;
    width: 100%;
    background: #f0f0f0;
}

/* 稳重的悬停效果 */
.category-item:hover {
    transform: translateY(-5px);
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}

.category-item:hover .bottom-bar {
    background: var(--theme-color);
}

.category-item:hover .enter-btn {
    color: var(--theme-color);
    transform: translateX(3px);
}
</style>

