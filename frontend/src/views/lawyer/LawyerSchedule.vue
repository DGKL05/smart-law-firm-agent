<template>
  <div class="calendar-tools">
    <CrudPage :config="lawyerConfigs.schedules" />
    <CrudPage :config="lawyerConfigs.scheduleBlocks" />
  </div>
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { lawyerConfigs } from '../crudConfigs'
</script>

<style scoped>
.calendar-tools { display: grid; gap: 26px; }
</style>
