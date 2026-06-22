<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">律所检索</h1>
        <div class="muted">按省份和关键词筛选律所基础档案</div>
      </div>
    </div>
    <el-card class="work-card" shadow="never">
      <el-form inline class="search-desk">
        <el-form-item label="省份编码"><el-input v-model="provinceCode" placeholder="如 110000" clearable /></el-form-item>
        <el-form-item label="关键词"><el-input v-model="keyword" clearable /></el-form-item>
        <el-form-item><el-button type="primary" :icon="Search" @click="load">筛选律所</el-button></el-form-item>
      </el-form>
      <el-table :data="records" border stripe>
        <el-table-column prop="name" label="律所名称" min-width="180" />
        <el-table-column prop="provinceName" label="省份" width="120" />
        <el-table-column prop="city" label="城市" width="120" />
        <el-table-column prop="specialties" label="业务专长" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="120"><template #default="{ row }"><el-button link type="primary" @click="$router.push(`/law-firms/${row.id}`)">详情</el-button></template></el-table-column>
      </el-table>
      <el-pagination style="margin-top: 16px" background layout="total, prev, pager, next" :total="total" v-model:current-page="pageNum" @current-change="load" />
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import http from '../api/http'

const records = ref([])
const total = ref(0)
const pageNum = ref(1)
const provinceCode = ref('')
const keyword = ref('')

async function load() {
  const data = await http.get('/api/public/law-firms', { params: { pageNum: pageNum.value, pageSize: 10, provinceCode: provinceCode.value, keyword: keyword.value } })
  records.value = data.records || []
  total.value = data.total || 0
}

onMounted(load)
</script>
