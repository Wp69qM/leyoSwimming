import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { nextTick } from 'vue';
import ElementPlus from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';
import KnowledgeManagementView from '../KnowledgeManagementView.vue';
import * as knowledgeApi from '@/api/knowledgeManagement';
import { useAdminAuthStore } from '@/stores/adminAuth';
import { createPinia, setActivePinia } from 'pinia';

vi.mock('@/api/knowledgeManagement', () => ({
  getKnowledgeList: vi.fn(),
  toggleKnowledgeStatus: vi.fn(),
  deleteKnowledge: vi.fn(),
  getKnowledgeDetail: vi.fn(),
}));

const mockedGetKnowledgeList = vi.mocked(knowledgeApi.getKnowledgeList);
const mockedToggleKnowledgeStatus = vi.mocked(
  knowledgeApi.toggleKnowledgeStatus
);
const mockedDeleteKnowledge = vi.mocked(knowledgeApi.deleteKnowledge);
const mockedGetKnowledgeDetail = vi.mocked(knowledgeApi.getKnowledgeDetail);

describe('KnowledgeManagementView', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    const authStore = useAdminAuthStore();
    authStore.admin = {
      id: 1,
      username: 'admin',
      name: '管理员',
      role: 'admin',
    };

    mockedGetKnowledgeList.mockResolvedValue({
      code: 0,
      message: 'success',
      data: {
        list: [
          {
            documentId: 1,
            title: '安全须知',
            category: 'safety',
            contentType: 'text',
            sourceType: 'manual',
            status: 0,
            createdBy: '管理员',
            createdAt: '2026-09-01T10:00:00Z',
          },
          {
            documentId: 2,
            title: '蛙泳技巧',
            category: 'technique',
            contentType: 'markdown',
            sourceType: 'file',
            status: 1,
            createdBy: '管理员',
            createdAt: '2026-09-02T11:00:00Z',
          },
        ],
        total: 2,
        page: 1,
        pageSize: 20,
      },
    });
    mockedToggleKnowledgeStatus.mockResolvedValue({
      code: 0,
      message: 'success',
      data: null,
    });
    mockedDeleteKnowledge.mockResolvedValue({
      code: 0,
      message: 'success',
      data: null,
    });
    mockedGetKnowledgeDetail.mockResolvedValue({
      code: 0,
      message: 'success',
      data: {
        documentId: 1,
        title: '安全须知',
        category: 'safety',
        contentType: 'text',
        sourceType: 'manual',
        content: '安全须知内容',
        status: 0,
        createdBy: '管理员',
        createdAt: '2026-09-01T10:00:00Z',
        updatedAt: '2026-09-01T10:00:00Z',
      },
    });

    vi.spyOn(ElMessage, 'success').mockImplementation(() => {});
    vi.spyOn(ElMessage, 'error').mockImplementation(() => {});
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm');
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  function mountView() {
    return mount(KnowledgeManagementView, {
      global: {
        plugins: [ElementPlus],
      },
    });
  }

  it('页面加载时显示骨架屏并请求列表', async () => {
    mountView();
    expect(mockedGetKnowledgeList).toHaveBeenCalledWith(
      expect.objectContaining({ page: 1, pageSize: 20 })
    );
  });

  it('列表数据渲染为表格行', async () => {
    const wrapper = mountView();
    await flushPromises();
    const rows = wrapper.findAll('.el-table__row');
    expect(rows.length).toBe(2);
    expect(wrapper.text()).toContain('安全须知');
    expect(wrapper.text()).toContain('蛙泳技巧');
  });

  it('空列表时显示空状态', async () => {
    mockedGetKnowledgeList.mockResolvedValueOnce({
      code: 0,
      message: 'success',
      data: { list: [], total: 0, page: 1, pageSize: 20 },
    });
    const wrapper = mountView();
    await flushPromises();
    expect(wrapper.find('.el-empty').exists()).toBe(true);
  });

  it('加载失败时显示错误占位并可重试', async () => {
    mockedGetKnowledgeList.mockRejectedValueOnce(new Error('网络错误'));
    const wrapper = mountView();
    await flushPromises();
    expect(ElMessage.error).toHaveBeenCalledWith('网络错误');
    expect(wrapper.find('.el-empty').exists()).toBe(true);

    mockedGetKnowledgeList.mockResolvedValueOnce({
      code: 0,
      message: 'success',
      data: {
        list: [
          {
            documentId: 1,
            title: '安全须知',
            category: 'safety',
            contentType: 'text',
            sourceType: 'manual',
            status: 0,
            createdBy: '管理员',
            createdAt: '2026-09-01T10:00:00Z',
          },
        ],
        total: 1,
        page: 1,
        pageSize: 20,
      },
    });
    await wrapper.find('.el-empty .el-button').trigger('click');
    await flushPromises();
    expect(wrapper.findAll('.el-table__row').length).toBe(1);
  });

  it('筛选条件变更后点击查询会重置到第一页', async () => {
    const wrapper = mountView();
    await flushPromises();

    const selects = wrapper.findAll('.el-select');
    expect(selects.length).toBeGreaterThanOrEqual(2);

    const keywordInput = wrapper.find('.el-input__inner');
    await keywordInput.setValue('安全');
    await wrapper.find('[data-testid="search-button"]').trigger('click');
    await flushPromises();

    expect(mockedGetKnowledgeList).toHaveBeenLastCalledWith(
      expect.objectContaining({
        page: 1,
        keyword: '安全',
      })
    );
  });

  it('点击重置按钮清空筛选并刷新', async () => {
    const wrapper = mountView();
    await flushPromises();

    const keywordInput = wrapper.find('.el-input__inner');
    await keywordInput.setValue('关键字');
    await wrapper.find('[data-testid="reset-button"]').trigger('click');
    await flushPromises();

    expect(mockedGetKnowledgeList).toHaveBeenLastCalledWith(
      expect.objectContaining({
        page: 1,
        category: undefined,
        status: null,
        keyword: undefined,
      })
    );
  });

  it('点击新增文档按钮打开上传弹窗', async () => {
    const wrapper = mountView();
    await flushPromises();

    await wrapper.find('[data-testid="add-button"]').trigger('click');
    await nextTick();

    expect(wrapper.findComponent({ name: 'KnowledgeUploadModal' }).exists()).toBe(
      true
    );
  });

  it('启用/禁用按钮调用 toggle 接口并刷新列表', async () => {
    const wrapper = mountView();
    await flushPromises();

    const toggleButton = wrapper.find('[data-testid="toggle-button-1"]');
    expect(toggleButton.exists()).toBe(true);
    await toggleButton.trigger('click');
    await flushPromises();

    expect(mockedToggleKnowledgeStatus).toHaveBeenCalledWith({
      documentId: 1,
      status: 1,
    });
    expect(ElMessage.success).toHaveBeenCalled();
    expect(mockedGetKnowledgeList).toHaveBeenCalledTimes(2);
  });

  it('删除按钮弹出确认框并调用删除接口', async () => {
    const wrapper = mountView();
    await flushPromises();

    await wrapper.find('[data-testid="delete-button-1"]').trigger('click');
    await flushPromises();

    expect(ElMessageBox.confirm).toHaveBeenCalled();
    expect(mockedDeleteKnowledge).toHaveBeenCalledWith({ documentId: 1 });
    expect(ElMessage.success).toHaveBeenCalled();
  });

  it('删除接口失败时显示错误信息', async () => {
    mockedDeleteKnowledge.mockRejectedValueOnce(new Error('删除失败'));
    const wrapper = mountView();
    await flushPromises();

    await wrapper.find('[data-testid="delete-button-1"]').trigger('click');
    await flushPromises();

    expect(ElMessage.error).toHaveBeenCalledWith('删除失败');
  });

  it('切换分页时请求对应页数据', async () => {
    mockedGetKnowledgeList.mockResolvedValueOnce({
      code: 0,
      message: 'success',
      data: {
        list: Array.from({ length: 25 }, (_, i) => ({
          documentId: i + 1,
          title: `文档${i + 1}`,
          category: 'other',
          contentType: 'text',
          sourceType: 'manual',
          status: 0,
          createdBy: '管理员',
          createdAt: '2026-09-01T10:00:00Z',
        })),
        total: 25,
        page: 1,
        pageSize: 20,
      },
    });
    const wrapper = mountView();
    await flushPromises();

    const pagination = wrapper.findComponent({ name: 'ElPagination' });
    pagination.vm.$emit('current-change', 2);
    await flushPromises();

    expect(mockedGetKnowledgeList).toHaveBeenLastCalledWith(
      expect.objectContaining({ page: 2 })
    );
  });

  it('点击标题打开详情弹窗并加载详情', async () => {
    const wrapper = mountView();
    await flushPromises();

    await wrapper.find('[data-testid="title-button-1"]').trigger('click');
    await flushPromises();

    expect(mockedGetKnowledgeDetail).toHaveBeenCalledWith({ documentId: 1 });
    expect(wrapper.findComponent({ name: 'KnowledgeDetailModal' }).exists()).toBe(
      true
    );
  });
});
