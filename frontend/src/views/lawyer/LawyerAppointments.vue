<template>
  <CrudPage :config="lawyerConfigs.appointments" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { lawyerConfigs } from '../crudConfigs'
</script>
