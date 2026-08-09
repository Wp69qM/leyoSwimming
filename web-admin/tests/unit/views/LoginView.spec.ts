import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { ElMessage } from 'element-plus';
import LoginView from '@/views/LoginView.vue';
import { useAdminAuthStore } from '@/stores/adminAuth';

const pushMock = vi.fn();
const queryMock = { redirect: '/' };

vi.mock('vue-router', async () => {
  const actual = await vi.importActual<typeof import('vue-router')>('vue-router');
  return {
    ...actual,
    useRouter: () => ({ push: pushMock }),
    useRoute: () => ({ query: queryMock }),
  };
});

function flushPromises() {
  return new Promise((resolve) => setTimeout(resolve, 0));
}

beforeEach(() => {
  vi.spyOn(ElMessage, 'success').mockImplementation(
    () => undefined as unknown as ReturnType<typeof ElMessage.success>,
  );
  vi.spyOn(ElMessage, 'error').mockImplementation(
    () => undefined as unknown as ReturnType<typeof ElMessage.error>,
  );
});

describe('LoginView', () => {
  beforeEach(() => {
    pushMock.mockReset();
    queryMock.redirect = '/';
  });

  function mountLoginView() {
    const pinia = createPinia();
    setActivePinia(pinia);
    return mount(LoginView, {
      global: { plugins: [pinia] },
    });
  }

  it('提交表单时调用 store.login 并在成功后跳转首页', async () => {
    const wrapper = mountLoginView();
    const store = useAdminAuthStore();
    store.login = vi.fn().mockResolvedValue({ success: true });

    await wrapper.find('input[placeholder="请输入管理员账号"]').setValue('admin');
    await wrapper.find('input[placeholder="请输入密码"]').setValue('admin123');
    await wrapper.find('button[type="submit"]').trigger('submit');
    await flushPromises();

    expect(store.login).toHaveBeenCalledWith({ username: 'admin', password: 'admin123' });
    expect(pushMock).toHaveBeenCalledWith('/');
    expect(ElMessage.success).toHaveBeenCalledWith('登录成功');
  });

  it('登录失败时显示错误提示', async () => {
    const wrapper = mountLoginView();
    const store = useAdminAuthStore();
    store.login = vi.fn().mockResolvedValue({ success: false, message: '用户名或密码错误' });

    await wrapper.find('input[placeholder="请输入管理员账号"]').setValue('admin');
    await wrapper.find('input[placeholder="请输入密码"]').setValue('wrong');
    await wrapper.find('button[type="submit"]').trigger('submit');
    await flushPromises();

    expect(wrapper.text()).toContain('用户名或密码错误');
    expect(pushMock).not.toHaveBeenCalled();
  });

  it('空表单提交时显示字段校验错误', async () => {
    const wrapper = mountLoginView();
    const store = useAdminAuthStore();
    store.login = vi.fn();

    await wrapper.find('button[type="submit"]').trigger('submit');
    await flushPromises();

    expect(wrapper.text()).toContain('请输入用户名');
    expect(wrapper.text()).toContain('请输入密码');
    expect(store.login).not.toHaveBeenCalled();
  });
});
