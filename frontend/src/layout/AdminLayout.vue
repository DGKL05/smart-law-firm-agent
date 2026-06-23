<template>
  <div class="app-shell">
    <header class="topbar">
      <router-link class="brand" to="/">
        <span class="brand-mark">管</span>
        <span>管理后台</span>
      </router-link>
      <nav class="nav">
        <router-link to="/">返回首页</router-link>
      </nav>
    </header>
    <div class="layout">
      <aside class="sidebar">
        <div class="sidebar-title">
          后台菜单
          <div class="sidebar-kicker">权限、业务与运营数据</div>
        </div>
        <el-menu router background-color="transparent" text-color="#e9e1d0" active-text-color="#ffe1a8">
          <el-menu-item index="/admin/stats"><el-icon><DataAnalysis /></el-icon>系统统计</el-menu-item>
          <el-menu-item index="/admin/users"><el-icon><User /></el-icon>用户管理</el-menu-item>
          <el-menu-item index="/admin/law-firms"><el-icon><OfficeBuilding /></el-icon>律所管理</el-menu-item>
          <el-menu-item index="/admin/lawyers"><el-icon><Avatar /></el-icon>律师管理</el-menu-item>
          <el-menu-item index="/admin/consultations"><el-icon><ChatLineRound /></el-icon>咨询管理</el-menu-item>
          <el-menu-item index="/admin/cases"><el-icon><Document /></el-icon>案件管理</el-menu-item>
          <el-menu-item index="/admin/appointments"><el-icon><Calendar /></el-icon>预约管理</el-menu-item>
          <el-menu-item index="/admin/notifications"><el-icon><Bell /></el-icon>通知管理</el-menu-item>
        </el-menu>
      </aside>
      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>
