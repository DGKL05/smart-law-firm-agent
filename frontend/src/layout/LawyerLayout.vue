<template>
  <div class="lawyer-shell">
    <aside class="lawyer-sidebar">
      <router-link class="lawyer-brand" to="/">
        <span class="brand-mark">法</span>
        <span>律师工作台</span>
      </router-link>
      <nav class="lawyer-nav">
        <router-link to="/lawyer-center/dashboard">工作台</router-link>
        <router-link to="/lawyer-center/profile">我的资料</router-link>
        <router-link to="/lawyer-center/schedule">可预约时间</router-link>
        <router-link to="/lawyer-center/appointments">预约管理</router-link>
        <router-link to="/lawyer-center/consultations">咨询回复</router-link>
      </nav>
    </aside>
    <main class="lawyer-main">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.lawyer-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 260px 1fr;
  background: #f6f3ea;
  color: #193b34;
}

.lawyer-sidebar {
  background: #0f3b33;
  color: #f8f0df;
  padding: 28px 22px;
}

.lawyer-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  color: inherit;
  text-decoration: none;
  font-size: 22px;
  font-weight: 700;
}

.brand-mark {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #b64032;
  color: #fff;
}

.lawyer-nav {
  display: grid;
  gap: 8px;
  margin-top: 32px;
}

.lawyer-nav a {
  color: #eadfc6;
  text-decoration: none;
  padding: 12px 14px;
  border-radius: 8px;
}

.lawyer-nav a.router-link-active {
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
}

.lawyer-main {
  padding: 32px;
}

@media (max-width: 800px) {
  .lawyer-shell {
    grid-template-columns: 1fr;
  }

  .lawyer-sidebar {
    position: static;
  }
}
</style>
