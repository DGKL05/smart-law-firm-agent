<template>
  <section class="page">
    <h1>预约管理</h1>
    <el-table :data="appointments" border>
      <el-table-column prop="appointmentTime" label="时间" />
      <el-table-column prop="status" label="状态" />
      <el-table-column prop="remark" label="备注" />
    </el-table>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getLawyerAppointments } from '../../api/lawyerCenter'

const appointments = ref([])

onMounted(async () => {
  appointments.value = await getLawyerAppointments()
})
</script>
