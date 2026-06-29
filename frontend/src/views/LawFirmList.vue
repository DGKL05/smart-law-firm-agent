<template>
  <section class="page archive-page">
    <div class="page-header archive-header">
      <div>
        <span class="archive-kicker">FIRM DIRECTORY</span>
        <h1 class="page-title">律所检索</h1>
        <div class="muted">按省份、城市、业务方向快速筛选律所基础档案</div>
      </div>
      <div class="archive-summary">
        <strong>{{ total }}</strong>
        <span>家可查询律所</span>
      </div>
    </div>

    <el-card class="work-card archive-console" shadow="never">
      <el-form class="search-desk archive-search">
        <el-form-item label="省份">
          <el-checkbox-group v-model="selectedProvinceCodes" class="province-grid">
            <el-checkbox-button
              v-for="item in provinces"
              :key="item.code"
              :label="item.code"
            >
              {{ item.name }}
            </el-checkbox-button>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="keyword" clearable placeholder="输入律所名称、城市或业务方向" />
        </el-form-item>
        <el-form-item class="archive-actions">
          <el-button type="primary" :icon="Search" @click="search">筛选律所</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="result-toolbar">
        <div>
          <div class="desk-title">律所档案</div>
          <div class="muted">展示执业区域、业务专长和联系方式，适合快速比较</div>
        </div>
        <span class="record-count">第 {{ pageNum }} 页</span>
      </div>

      <div v-if="records.length" class="firm-card-grid">
        <article v-for="firm in records" :key="firm.id" class="directory-card firm-directory-card">
          <div class="directory-card-mark">律</div>
          <div class="directory-card-main">
            <div class="directory-card-head">
              <div>
                <span class="card-kicker">{{ firm.provinceName }} · {{ firm.city }}</span>
                <h2>{{ firm.name }}</h2>
              </div>
              <el-tag effect="plain">{{ firm.status === 1 ? '正常执业' : '暂停服务' }}</el-tag>
            </div>

            <p class="directory-copy">{{ firm.description || '该律所暂未填写详细介绍。' }}</p>

            <div class="tag-row compact-tags">
              <el-tag v-for="tag in splitText(firm.specialties).slice(0, 4)" :key="tag" effect="plain">
                {{ tag }}
              </el-tag>
            </div>

            <div class="directory-meta">
              <span>{{ firm.address || '地址待补充' }}</span>
              <span>{{ firm.phone || '电话待补充' }}</span>
            </div>

            <div class="directory-card-foot">
              <span>执业证号：{{ firm.licenseNo || '待补充' }}</span>
              <el-button type="primary" plain :icon="ArrowRight" @click="$router.push(`/law-firms/${firm.id}`)">
                查看详情
              </el-button>
            </div>
          </div>
        </article>
      </div>
      <el-empty v-else description="暂无匹配律所" />

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

const provinces = [
  { code: '110000', name: '北京' },
  { code: '310000', name: '上海' },
  { code: '440000', name: '广东' },
  { code: '230000', name: '黑龙江' },
  { code: '330000', name: '浙江' },
  { code: '510000', name: '四川' },
  { code: '120000', name: '天津' },
  { code: '320000', name: '江苏' },
  { code: '370000', name: '山东' },
  { code: '420000', name: '湖北' }
]

const records = ref([])
const total = ref(0)
const pageNum = ref(1)
const selectedProvinceCodes = ref([])
const keyword = ref('')

async function load() {
  const data = await http.get('/api/public/law-firms', {
    params: {
      pageNum: pageNum.value,
      pageSize: 10,
      provinceCodes: selectedProvinceCodes.value.join(',') || undefined,
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
  selectedProvinceCodes.value = []
  keyword.value = ''
  search()
}

function splitText(value) {
  return (value || '').split(/[、,，\s]+/).map((item) => item.trim()).filter(Boolean)
}

onMounted(load)
</script>
