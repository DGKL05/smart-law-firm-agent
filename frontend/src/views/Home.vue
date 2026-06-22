<template>
  <section class="hero">
    <div class="hero-inner">
      <div>
        <h1>智能律所平台</h1>
        <p>把律所、律师、咨询、案件、预约和合同文书整理成一套清晰的在线业务台账，适合课堂演示，也便于后续接入 Dify 与 MCP Agent。</p>
        <el-space wrap>
          <el-button type="primary" size="large" @click="$router.push('/law-firms')">
            <el-icon><OfficeBuilding /></el-icon>
            浏览律所
          </el-button>
          <el-button size="large" plain @click="$router.push('/lawyers')">
            <el-icon><Avatar /></el-icon>
            查找律师
          </el-button>
        </el-space>
      </div>
      <div class="hero-panel">
        <div class="case-tabs">
          <span>平台总览</span>
          <span>LOCALHOST</span>
        </div>
        <div class="stat-grid">
          <div class="stat-item">
            <div class="muted">入驻律所</div>
            <div class="stat-number">{{ stats.lawFirmCount || 0 }}</div>
          </div>
          <div class="stat-item">
            <div class="muted">执业律师</div>
            <div class="stat-number">{{ stats.lawyerCount || 0 }}</div>
          </div>
        </div>
        <div class="workflow-strip">
          <span>注册登录</span>
          <span>检索律师</span>
          <span>提交业务</span>
          <span>后台维护</span>
        </div>
      </div>
    </div>
  </section>

  <section class="section-band">
    <div class="feature-grid">
      <article>
        <el-icon><ScaleToOriginal /></el-icon>
        <h3>公开检索</h3>
        <p>按省份筛选律所，按分类和律所筛选律师，公开页面直接服务普通用户。</p>
      </article>
      <article>
        <el-icon><FolderChecked /></el-icon>
        <h3>我的业务</h3>
        <p>咨询、案件、预约、合同、文书、发票和通知全部按当前登录用户隔离。</p>
      </article>
      <article>
        <el-icon><DataAnalysis /></el-icon>
        <h3>后台管理</h3>
        <p>管理员统一维护用户、律所、律师和业务数据，并查看系统统计与日志。</p>
      </article>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import http from '../api/http'

const stats = ref({})
onMounted(async () => {
  stats.value = await http.get('/api/public/home/stats')
})
</script>
