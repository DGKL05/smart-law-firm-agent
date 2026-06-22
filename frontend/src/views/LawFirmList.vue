<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">律所检索</h1>
        <div class="muted">勾选省份并输入关键词，快速定位律所基础档案</div>
      </div>
    </div>
    <el-card class="work-card" shadow="never">
      <el-form class="search-desk">
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
          <el-input v-model="keyword" clearable placeholder="律所名称、城市或业务方向" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">筛选律所</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="records" border stripe>
        <el-table-column prop="name" label="律所名称" min-width="190" />
        <el-table-column prop="provinceName" label="省份" width="120" />
        <el-table-column prop="city" label="城市" width="120" />
        <el-table-column prop="specialties" label="业务专长" min-width="240" show-overflow-tooltip />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/law-firms/${row.id}`)">详情</el-button>
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

onMounted(load)
</script>
