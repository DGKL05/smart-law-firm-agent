<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">律师检索</h1>
        <div class="muted">按专业分类、所属律所和关键词定位合适律师</div>
      </div>
    </div>
    <el-card class="work-card" shadow="never">
      <el-form class="search-desk">
        <el-form-item label="分类">
          <el-select v-model="category" clearable placeholder="全部" style="width: 200px">
            <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="律所">
          <el-select v-model="lawFirmId" clearable filterable placeholder="请选择律所" style="width: 280px">
            <el-option v-for="item in lawFirms" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="keyword" clearable placeholder="姓名、擅长方向" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">筛选律师</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="records" border stripe>
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="lawFirmName" label="所属律所" min-width="190" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column prop="title" label="职称" width="140" />
        <el-table-column prop="goodAt" label="擅长" min-width="220" show-overflow-tooltip />
        <el-table-column label="可预约时间" min-width="220">
          <template #default="{ row }">
            <div class="slot-list compact">
              <el-tag v-for="slot in splitSlots(row.availableTimeSlots)" :key="slot" effect="plain">
                {{ slot }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/lawyers/${row.id}`)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="margin-top: 16px"
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
import { Search } from '@element-plus/icons-vue'
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

function splitSlots(value) {
  return (value || '').split(',').map((item) => item.trim()).filter(Boolean)
}

onMounted(async () => {
  await loadLawFirms()
  await load()
})
</script>
