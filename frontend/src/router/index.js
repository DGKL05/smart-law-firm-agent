import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import PublicLayout from '../layout/PublicLayout.vue'
import UserLayout from '../layout/UserLayout.vue'
import AdminLayout from '../layout/AdminLayout.vue'
import Home from '../views/Home.vue'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import LawFirmList from '../views/LawFirmList.vue'
import LawFirmDetail from '../views/LawFirmDetail.vue'
import LawyerList from '../views/LawyerList.vue'
import LawyerDetail from '../views/LawyerDetail.vue'
import GenericCrudView from '../views/GenericCrudView.vue'
import AdminStats from '../views/AdminStats.vue'
import { userConfigs, adminConfigs } from '../views/crudConfigs'

const userRoutes = Object.entries(userConfigs).map(([path, config]) => ({
  path,
  component: GenericCrudView,
  meta: { requiresAuth: true, config }
}))

const adminRoutes = Object.entries(adminConfigs).map(([path, config]) => ({
  path,
  component: GenericCrudView,
  meta: { requiresAuth: true, requiresAdmin: true, config }
}))

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: Login },
    { path: '/register', component: Register },
    {
      path: '/',
      component: PublicLayout,
      children: [
        { path: '', component: Home },
        { path: 'law-firms', component: LawFirmList },
        { path: 'law-firms/:id', component: LawFirmDetail },
        { path: 'lawyers', component: LawyerList },
        { path: 'lawyers/:id', component: LawyerDetail }
      ]
    },
    {
      path: '/user',
      component: UserLayout,
      meta: { requiresAuth: true },
      children: userRoutes
    },
    {
      path: '/admin',
      component: AdminLayout,
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        { path: 'stats', component: AdminStats, meta: { requiresAuth: true, requiresAdmin: true } },
        ...adminRoutes
      ]
    }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isLogin) {
    return '/login'
  }
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return '/'
  }
  return true
})

export default router
