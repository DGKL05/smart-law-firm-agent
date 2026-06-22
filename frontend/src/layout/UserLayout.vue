<template>
  <div class="app-shell">
    <header class="topbar">
      <router-link class="brand" to="/">
        <span class="brand-mark">法</span>
        <span>用户中心</span>
      </router-link>
      <nav class="nav">
        <router-link to="/">返回首页</router-link>
      </nav>
    </header>
    <div class="layout">
      <aside class="sidebar">
        <div class="sidebar-title">
          我的业务
          <div class="sidebar-kicker">个人案卷与服务记录</div>
        </div>
        <el-menu router background-color="transparent" text-color="#e9e1d0" active-text-color="#ffe1a8">
          <el-menu-item index="/user/consultations"><el-icon><ChatLineRound /></el-icon>我的咨询</el-menu-item>
          <el-menu-item index="/user/cases"><el-icon><Document /></el-icon>我的案件</el-menu-item>
          <el-menu-item index="/user/appointments"><el-icon><Calendar /></el-icon>我的预约</el-menu-item>
          <el-menu-item index="/user/contracts"><el-icon><Tickets /></el-icon>我的合同</el-menu-item>
          <el-menu-item index="/user/documents"><el-icon><Files /></el-icon>我的文书</el-menu-item>
          <el-menu-item index="/user/invoices"><el-icon><Money /></el-icon>我的发票</el-menu-item>
          <el-menu-item index="/user/notifications"><el-icon><Bell /></el-icon>消息通知</el-menu-item>
        </el-menu>
      </aside>
      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>
