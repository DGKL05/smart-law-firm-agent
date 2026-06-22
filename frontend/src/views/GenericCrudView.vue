<template>
  <CrudPage :config="config" />
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import CrudPage from '../components/CrudPage.vue'

const route = useRoute()
const config = computed(() => route.meta.config)
</script>
