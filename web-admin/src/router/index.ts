import { createRouter, createWebHistory } from 'vue-router';
import { useAdminAuthStore } from '@/stores/adminAuth';
import AdminLayout from '@/components/layout/AdminLayout.vue';
import HomeView from '@/views/HomeView.vue';
import LoginView from '@/views/LoginView.vue';
import ResignationApprovalQueueView from '@/views/resignation/ResignationApprovalQueueView.vue';
import ResignationTicketDetailView from '@/views/resignation/ResignationTicketDetailView.vue';
import CoachAuditQueueView from '@/views/coach-audit/CoachAuditQueueView.vue';
import CoachAuditDetailView from '@/views/coach-audit/CoachAuditDetailView.vue';
import UserManagementView from '@/views/user-management/UserManagementView.vue';
import CoachManagementView from '@/views/coach-management/CoachManagementView.vue';
import CoachDetailView from '@/views/coach-management/CoachDetailView.vue';
import AdminAccountManagementView from '@/views/admin-account/AdminAccountManagementView.vue';

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: AdminLayout,
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'home',
          component: HomeView,
        },
        {
          path: 'resignation/approval-queue',
          name: 'resignation-approval-queue',
          component: ResignationApprovalQueueView,
        },
        {
          path: 'resignation/ticket-detail/:ticketId',
          name: 'resignation-ticket-detail',
          component: ResignationTicketDetailView,
        },
        {
          path: 'coach-audit/queue',
          name: 'coach-audit-queue',
          component: CoachAuditQueueView,
        },
        {
          path: 'coach-audit/detail/:applicationId',
          name: 'coach-audit-detail',
          component: CoachAuditDetailView,
        },
        {
          path: 'user-management',
          name: 'user-management',
          component: UserManagementView,
        },
        {
          path: 'coach-management',
          name: 'coach-management',
          component: CoachManagementView,
        },
        {
          path: 'coach-management/detail/:coachId',
          name: 'coach-management-detail',
          component: CoachDetailView,
        },
        {
          path: 'admin-account-management',
          name: 'admin-account-management',
          component: AdminAccountManagementView,
        },
      ],
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { guest: true },
    },
  ],
});

router.beforeEach((to) => {
  const authStore = useAdminAuthStore();
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } };
  }
  if (to.meta.guest && authStore.isAuthenticated) {
    return { path: '/' };
  }
  return true;
});

export default router;
