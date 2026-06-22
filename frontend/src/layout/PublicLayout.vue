<template>
  <div class="app-shell">
    <header class="topbar">
      <router-link class="brand" to="/">
        <span class="brand-mark">法</span>
        <span>智能律所平台</span>
      </router-link>
      <nav class="nav">
        <router-link to="/">首页</router-link>
        <router-link to="/law-firms">律所</router-link>
        <router-link to="/lawyers">律师</router-link>
        <router-link v-if="auth.isLogin" to="/user/consultations">用户中心</router-link>
        <router-link v-if="auth.isAdmin" to="/admin/stats">管理后台</router-link>
        <router-link v-if="!auth.isLogin" to="/login">登录</router-link>
        <span v-else class="nav-user">{{ auth.user?.nickname || auth.user?.username }}</span>
        <el-button v-if="auth.isLogin" size="small" plain @click="logout">退出</el-button>
      </nav>
    </header>
    <router-view />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

function logout() {
  auth.logout()
  router.push('/login')
}
</script>
