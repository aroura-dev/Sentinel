<template>
  <div class="ph-wrap">
    <el-card shadow="never" class="ph-card">
      <div class="ph-inner">
        <div class="ph-icon">
          <el-icon :size="52"><Tools /></el-icon>
        </div>
        <div class="ph-title">{{ route.meta.title }} · 建设中</div>
        <div class="ph-sub">
          这个功能正在规划开发中，上线后就能在这里使用。敬请期待。
        </div>
        <div class="ph-actions">
          <el-button type="primary" @click="$router.push('/workbench')">返回工作台</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<style scoped>
.ph-wrap {
  min-height: 55vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
.ph-card {
  width: 560px;
  max-width: 100%;
  border-radius: 12px;
}
.ph-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 40px 0 32px;
  text-align: center;
}
.ph-icon {
  color: #0891b2;
}
.ph-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}
.ph-sub {
  font-size: 13px;
  color: #909399;
  line-height: 1.7;
  padding: 0 32px;
}
.ph-actions {
  margin-top: 8px;
}
</style>
