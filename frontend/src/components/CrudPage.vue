<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ config.title }}</h1>
        <div class="muted">案卷式数据台账，支持搜索、分页、新增、编辑和删除</div>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增记录</el-button>
    </div>

    <el-card class="work-card" shadow="never">
      <div class="desk-toolbar">
        <div>
          <div class="desk-title">检索条件</div>
          <div class="muted">关键词会匹配标题、名称或业务字段</div>
        </div>
        <div class="record-count">{{ total }} 条记录</div>
      </div>
      <el-form inline class="search-desk">
        <el-form-item label="关键词">
          <el-input v-model="keyword" placeholder="输入关键词" clearable @keyup.enter="load" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="load">搜索</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="records" border stripe style="width: 100%" class="ledger-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column v-for="column in config.columns" :key="column.prop" :prop="column.prop" :label="column.label" min-width="130" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button v-for="item in config.extraActions || []" :key="item.action" size="small" @click="runExtra(row, item)">{{ item.label }}</el-button>
            <el-button size="small" type="danger" :icon="Delete" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        style="margin-top: 16px"
        background
        layout="total, prev, pager, next, sizes"
        :total="total"
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        @current-change="load"
        @size-change="load"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑记录' : '新增记录'" width="620px" class="case-dialog">
      <el-form :model="form" label-width="100px">
        <el-form-item v-for="field in config.fields" :key="field.prop" :label="field.label" :required="field.prop === 'title' || field.prop === 'name' || field.prop === 'username'">
          <el-date-picker v-if="field.type === 'datetime'" v-model="form[field.prop]" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
          <el-input-number v-else-if="field.type === 'number'" v-model="form[field.prop]" style="width: 100%" />
          <el-input v-else v-model="form[field.prop]" :type="['content', 'description', 'question', 'reply'].includes(field.prop) ? 'textarea' : 'text'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { Delete, Edit, Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'

const props = defineProps({
  config: { type: Object, required: true }
})

const records = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const dialogVisible = ref(false)
const form = reactive({})

watch(() => props.config.endpoint, () => load(), { flush: 'post' })

async function load() {
  const data = await http.get(props.config.endpoint, { params: { pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value } })
  records.value = data.records || []
  total.value = data.total || 0
}

function resetForm(row = {}) {
  Object.keys(form).forEach((key) => delete form[key])
  props.config.fields.forEach((field) => {
    form[field.prop] = row[field.prop] ?? null
  })
  if (row.id) form.id = row.id
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  resetForm(row)
  dialogVisible.value = true
}

async function submit() {
  if (form.id) {
    await http.put(`${props.config.endpoint}/${form.id}`, form)
  } else {
    await http.post(props.config.endpoint, form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function remove(row) {
  await ElMessageBox.confirm('确认删除该记录？', '提示', { type: 'warning' })
  await http.delete(`${props.config.endpoint}/${row.id}`)
  ElMessage.success('删除成功')
  load()
}

async function runExtra(row, item) {
  await http[item.method](`${props.config.endpoint}/${row.id}${item.suffix}`)
  ElMessage.success('操作成功')
  load()
}

onMounted(load)
</script>
