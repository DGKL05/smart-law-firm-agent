<template>
  <CrudPage :config="lawyerConfigs.consultations" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { lawyerConfigs } from '../crudConfigs'
</script>
