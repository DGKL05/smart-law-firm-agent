<template>
  <section class="page detail-page">
    <el-card class="detail-sheet profile-sheet editorial-sheet" shadow="never">
      <div class="profile-hero firm-profile-hero">
        <div>
          <div class="profile-kicker">{{ item.provinceName }} · {{ item.city }}</div>
          <h1 class="page-title">{{ item.name }}</h1>
          <p class="profile-summary">{{ item.description || '该律所暂未填写详细介绍。' }}</p>
          <div class="tag-row">
            <el-tag v-for="tag in specialties" :key="tag" effect="plain" size="large">
              {{ tag }}
            </el-tag>
          </div>
        </div>
        <div class="profile-stats elevated-stats">
          <div class="profile-stat">
            <strong>{{ lawyers.length }}</strong>
            <span>本所律师</span>
          </div>
          <div class="profile-stat">
            <strong>{{ specialties.length }}</strong>
            <span>服务方向</span>
          </div>
          <div class="profile-stat">
            <strong>{{ item.status === 1 ? '正常' : '停用' }}</strong>
            <span>执业状态</span>
          </div>
        </div>
      </div>

      <div class="detail-grid editorial-detail-grid">
        <section class="info-panel wide">
          <div class="panel-title">律所档案</div>
          <dl class="info-list">
            <div>
              <dt>执业许可证号</dt>
              <dd>{{ item.licenseNo || '待补充' }}</dd>
            </div>
            <div>
              <dt>所在区域</dt>
              <dd>{{ item.provinceName }} {{ item.city }}</dd>
            </div>
            <div>
              <dt>办公地址</dt>
              <dd>{{ item.address || '待补充' }}</dd>
            </div>
            <div>
              <dt>服务定位</dt>
              <dd>{{ item.specialties || '待补充' }}</dd>
            </div>
          </dl>
        </section>

        <section class="info-panel contact-panel">
          <div class="panel-title">联系方式</div>
          <dl class="info-list compact-info">
            <div>
              <dt>电话</dt>
              <dd>{{ item.phone || '待补充' }}</dd>
            </div>
            <div>
              <dt>邮箱</dt>
              <dd>{{ item.email || '待补充' }}</dd>
            </div>
          </dl>
        </section>

        <section class="info-panel">
          <div class="panel-title">服务流程</div>
          <div class="process-list refined-process">
            <span>需求梳理</span>
            <span>律师匹配</span>
            <span>预约咨询</span>
            <span>方案跟进</span>
          </div>
        </section>
      </div>

      <div class="detail-section">
        <div class="desk-toolbar">
          <div>
            <div class="desk-title">本所律师</div>
            <div class="muted">每位律师都绑定当前律所，可进入详情查看介绍和可预约咨询时间</div>
          </div>
          <span class="record-count">{{ lawyers.length }} 位律师</span>
        </div>
        <div v-if="lawyers.length" class="lawyer-card-grid polished-lawyer-grid">
          <article v-for="lawyer in lawyers" :key="lawyer.id" class="lawyer-card polished-lawyer-card">
            <div class="lawyer-avatar">{{ lawyer.name?.slice(0, 1) }}</div>
            <div class="lawyer-card-body">
              <div class="card-title-row">
                <h3>{{ lawyer.name }}</h3>
                <el-tag effect="plain">{{ lawyer.category || '综合法律' }}</el-tag>
              </div>
              <p class="muted">{{ lawyer.title }} · {{ lawyer.experienceYears || 0 }} 年经验</p>
              <p class="card-copy">{{ lawyer.goodAt || lawyer.description || '暂未填写擅长方向。' }}</p>
              <div class="slot-list compact">
                <el-tag effect="plain">
                  {{ availableSlots(lawyer).length }} / {{ lawyer.scheduleSlots?.length || 0 }} 可约
                </el-tag>
                <el-tag v-for="slot in availableSlots(lawyer).slice(0, 2)" :key="slot.startTime" effect="plain">
                  {{ formatSlot(slot) }}
                </el-tag>
              </div>
              <div class="card-actions">
                <el-button type="primary" plain @click="$router.push(`/lawyers/${lawyer.id}`)">查看律师详情</el-button>
              </div>
            </div>
          </article>
        </div>
        <el-empty v-else description="暂无本所律师" />
      </div>
    </el-card>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import http from '../api/http'

const route = useRoute()
const item = ref({})
const lawyers = computed(() => item.value.lawyers || [])
const specialties = computed(() => splitText(item.value.specialties))

function splitText(value) {
  return (value || '').split(/[、,，\s]+/).map((item) => item.trim()).filter(Boolean)
}

function availableSlots(lawyer) {
  return (lawyer.scheduleSlots || []).filter((slot) => slot.available)
}

function formatSlot(slot) {
  return `${slot.date?.slice(5).replace('-', '/')} ${slot.startTime?.slice(11, 16)}`
}

onMounted(async () => {
  item.value = await http.get(`/api/public/law-firms/${route.params.id}`)
})
</script>
