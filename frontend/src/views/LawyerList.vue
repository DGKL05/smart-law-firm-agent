<template>
  <section class="page archive-page">
    <div class="page-header archive-header">
      <div>
        <span class="archive-kicker">LAWYER ROSTER</span>
        <h1 class="page-title">律师检索</h1>
        <div class="muted">按专业分类、所属律所和关键词定位合适律师</div>
      </div>
      <div class="archive-summary">
        <strong>{{ total }}</strong>
        <span>位执业律师</span>
      </div>
    </div>

    <el-card class="work-card archive-console" shadow="never">
      <el-form class="search-desk archive-search lawyer-search">
        <el-form-item label="分类">
          <el-select v-model="category" clearable placeholder="全部" class="filter-control">
            <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="律所">
          <el-select v-model="lawFirmId" clearable filterable placeholder="请选择律所" class="filter-control wide">
            <el-option v-for="item in lawFirms" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="keyword" clearable placeholder="输入姓名、擅长方向" />
        </el-form-item>
        <el-form-item class="archive-actions">
          <el-button type="primary" :icon="Search" @click="search">筛选律师</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="result-toolbar">
        <div>
          <div class="desk-title">律师名录</div>
          <div class="muted">每位律师均关联所属律所，可进入详情查看预约时间</div>
        </div>
        <span class="record-count">第 {{ pageNum }} 页</span>
      </div>

      <div v-if="records.length" class="lawyer-directory-grid">
        <article v-for="lawyer in records" :key="lawyer.id" class="directory-card lawyer-directory-card">
          <div class="lawyer-avatar large-card-avatar">{{ lawyer.name?.slice(0, 1) }}</div>
          <div class="directory-card-main">
            <div class="directory-card-head">
              <div>
                <span class="card-kicker">{{ lawyer.lawFirmName }}</span>
                <h2>{{ lawyer.name }}</h2>
              </div>
              <el-tag effect="plain">{{ lawyer.category || '综合法律' }}</el-tag>
            </div>

            <p class="muted lawyer-title-line">{{ lawyer.title }} · {{ lawyer.experienceYears || 0 }} 年经验</p>
            <p class="directory-copy">{{ lawyer.description || lawyer.goodAt || '该律师暂未填写详细介绍。' }}</p>

            <div class="tag-row compact-tags">
              <el-tag v-for="tag in splitText(lawyer.goodAt).slice(0, 4)" :key="tag" effect="plain">
                {{ tag }}
              </el-tag>
            </div>

            <div class="slot-list compact lawyer-slot-strip">
              <el-tag v-for="slot in splitSlots(lawyer.availableTimeSlots).slice(0, 3)" :key="slot" effect="plain">
                {{ slot }}
              </el-tag>
              <el-tag v-if="!splitSlots(lawyer.availableTimeSlots).length" effect="plain">工作日可预约</el-tag>
            </div>

            <div class="directory-card-foot">
              <span>{{ lawyer.phone || '联系方式待补充' }}</span>
              <el-button type="primary" plain :icon="ArrowRight" @click="$router.push(`/lawyers/${lawyer.id}`)">
                查看详情
              </el-button>
            </div>
          </div>
        </article>
      </div>
      <el-empty v-else description="暂无匹配律师" />

      <el-pagination
        class="archive-pagination"
        background
        layout="total, prev, pager, next"
        :total="total"
        v-model:current-page="pageNum"
        @current-change="load"
      />
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ArrowRight, Search } from '@element-plus/icons-vue'
import http from '../api/http'

const categories = ['民事民法', '经济金融', '涉外纠纷', '公司企业', '知识产权', '劳动争议', '婚姻家事', '房产纠纷', '建设工程', '互联网合规']
const records = ref([])
const lawFirms = ref([])
const total = ref(0)
const pageNum = ref(1)
const category = ref('')
const lawFirmId = ref(null)
const keyword = ref('')

async function loadLawFirms() {
  const data = await http.get('/api/public/law-firms', { params: { pageNum: 1, pageSize: 100 } })
  lawFirms.value = data.records || []
}

async function load() {
  const data = await http.get('/api/public/lawyers', {
    params: {
      pageNum: pageNum.value,
      pageSize: 10,
      category: category.value || undefined,
      lawFirmId: lawFirmId.value || undefined,
      keyword: keyword.value || undefined
    }
  })
  records.value = data.records || []
  total.value = data.total || 0
}

function search() {
  pageNum.value = 1
  load()
}

function resetFilters() {
  category.value = ''
  lawFirmId.value = null
  keyword.value = ''
  search()
}

function splitText(value) {
  return (value || '').split(/[、,，\s]+/).map((item) => item.trim()).filter(Boolean)
}

function splitSlots(value) {
  return (value || '').split(',').map((item) => item.trim()).filter(Boolean)
}

onMounted(async () => {
  await loadLawFirms()
  await load()
})
</script>
