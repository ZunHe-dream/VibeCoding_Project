<template>
    <div class="developing-container">
        <el-empty image="https://element-plus.org/images/element-plus-logo.svg" description="该模块正在策划布展中，敬请期待...">
            <el-button type="primary" @click="$router.back()">返回上一页</el-button>
        </el-empty>
    </div>
</template>

<style scoped>
.developing-container {
    height: 80vh;
    display: flex;
    justify-content: center;
    align-items: center;
    background: #fff;
    margin: 20px;
    border-radius: 8px;
}
</style>
