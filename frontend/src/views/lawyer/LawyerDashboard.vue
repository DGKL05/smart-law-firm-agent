<template>
  <section class="page">
    <h1>律师工作台</h1>
    <div class="cards">
      <div class="card">
        <span>今日预约</span>
        <strong>0</strong>
      </div>
      <div class="card">
        <span>待回复咨询</span>
        <strong>0</strong>
      </div>
      <div class="card">
        <span>本周可预约</span>
        <strong>周一至周五</strong>
      </div>
    </div>
  </section>
</template>

<style scoped>
.page h1 { margin: 0 0 22px; }
.cards { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
.card { background: #fff; border: 1px solid #e2d8c5; border-radius: 8px; padding: 20px; display: grid; gap: 10px; }
.card span { color: #6f746f; }
.card strong { font-size: 24px; color: #123f36; }
</style>
