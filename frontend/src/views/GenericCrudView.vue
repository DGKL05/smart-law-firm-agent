<template>
  <CrudPage v-if="config" :config="config" :initial-lawyer-id="initialLawyerId" />
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import CrudPage from '../components/CrudPage.vue'

const route = useRoute()
const config = computed(() => route.meta.config)
const initialLawyerId = computed(() => {
  const id = Number(route.query.lawyerId)
  return Number.isSafeInteger(id) && id > 0 ? id : undefined
})
</script>
