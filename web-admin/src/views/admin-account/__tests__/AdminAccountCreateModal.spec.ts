import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { nextTick } from 'vue';
import ElementPlus from 'element-plus';
import AdminAccountCreateModal from '../AdminAccountCreateModal.vue';
import * as api from '@/api/adminAccountManagement';

vi.mock('@/api/adminAccountManagement', () => ({
  addAdminAccount: vi.fn(),
}));

const mockedAdd = vi.mocked(api.addAdminAccount);

describe('AdminAccountCreateModal validation', () => {
  beforeEach(() => {
    mockedAdd.mockResolvedValue({ code: 0, message: 'success', data: null });
  });
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('blocks submit when empty', async () => {
    const wrapper = mount(AdminAccountCreateModal, {
      props: { visible: true },
      global: { plugins: [ElementPlus] },
    });
    await nextTick();
    await flushPromises();

    await wrapper.find('button[type="button"]').trigger('click');
    await flushPromises();

    expect(mockedAdd).not.toHaveBeenCalled();
  });
});
