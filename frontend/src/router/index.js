import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import PublicLayout from '../layout/PublicLayout.vue'
import UserLayout from '../layout/UserLayout.vue'
import AdminLayout from '../layout/AdminLayout.vue'
import LawyerLayout from '../layout/LawyerLayout.vue'
import Home from '../views/Home.vue'
import Login from '../views/Login.vue'
import OAuthCallback from '../views/OAuthCallback.vue'
import Register from '../views/Register.vue'
import LawFirmList from '../views/LawFirmList.vue'
import LawFirmDetail from '../views/LawFirmDetail.vue'
import LawyerList from '../views/LawyerList.vue'
import LawyerDetail from '../views/LawyerDetail.vue'
import LawyerDashboard from '../views/lawyer/LawyerDashboard.vue'
import LawyerProfile from '../views/lawyer/LawyerProfile.vue'
import LawyerSchedule from '../views/lawyer/LawyerSchedule.vue'
import LawyerAppointments from '../views/lawyer/LawyerAppointments.vue'
import LawyerConsultations from '../views/lawyer/LawyerConsultations.vue'
import GenericCrudView from '../views/GenericCrudView.vue'
import AdminStats from '../views/AdminStats.vue'
import AdminWeeklySchedule from '../views/AdminWeeklySchedule.vue'
import AccountSettings from '../views/AccountSettings.vue'
import { userConfigs, adminConfigs } from '../views/crudConfigs'
import { getAccessToken } from '../api/oauth'

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
    { path: '/oauth/callback', component: OAuthCallback },
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
      children: [
        { path: '', redirect: '/user/consultations' },
        { path: 'account', component: AccountSettings },
        ...userRoutes
      ]
    },
    {
      path: '/lawyer-center',
      component: LawyerLayout,
      meta: { requiresAuth: true, requiresLawyer: true },
      children: [
        { path: '', redirect: '/lawyer-center/dashboard' },
        { path: 'dashboard', component: LawyerDashboard },
        { path: 'profile', component: LawyerProfile },
        { path: 'schedule', component: LawyerSchedule },
        { path: 'appointments', component: LawyerAppointments },
        { path: 'consultations', component: LawyerConsultations }
      ]
    },
    {
      path: '/admin',
      component: AdminLayout,
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        { path: 'stats', component: AdminStats, meta: { requiresAuth: true, requiresAdmin: true } },
        { path: 'schedules', component: AdminWeeklySchedule },
        ...adminRoutes
      ]
    }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (auth.isLogin && !getAccessToken()) auth.clear()
  if (to.meta.requiresAuth && !auth.isLogin) {
    return '/login'
  }
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return '/'
  }
  if (to.meta.requiresLawyer && !auth.isLawyer) {
    return '/'
  }
  return true
})

export default router
