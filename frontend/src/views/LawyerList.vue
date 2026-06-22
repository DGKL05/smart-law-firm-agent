<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">律师检索</h1>
        <div class="muted">按专业分类、律所和关键词定位合适律师</div>
      </div>
    </div>
    <el-card class="work-card" shadow="never">
      <el-form inline class="search-desk">
        <el-form-item label="分类">
          <el-select v-model="category" clearable placeholder="全部" style="width: 160px">
            <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="律所ID"><el-input-number v-model="lawFirmId" :min="0" /></el-form-item>
        <el-form-item label="关键词"><el-input v-model="keyword" clearable /></el-form-item>
        <el-form-item><el-button type="primary" :icon="Search" @click="load">筛选律师</el-button></el-form-item>
      </el-form>
      <el-table :data="records" border stripe>
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column prop="title" label="职称" width="140" />
        <el-table-column prop="goodAt" label="擅长" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="120"><template #default="{ row }"><el-button link type="primary" @click="$router.push(`/lawyers/${row.id}`)">详情</el-button></template></el-table-column>
      </el-table>
      <el-pagination style="margin-top: 16px" background layout="total, prev, pager, next" :total="total" v-model:current-page="pageNum" @current-change="load" />
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import http from '../api/http'

const categories = ['民事民法', '经济金融', '刑事行政', '涉外纠纷', '公司企业', '其他类别']
const records = ref([])
const total = ref(0)
const pageNum = ref(1)
const category = ref('')
const lawFirmId = ref(null)
const keyword = ref('')

async function load() {
  const data = await http.get('/api/public/lawyers', { params: { pageNum: pageNum.value, pageSize: 10, category: category.value, lawFirmId: lawFirmId.value || undefined, keyword: keyword.value } })
  records.value = data.records || []
  total.value = data.total || 0
}

onMounted(load)
</script>
