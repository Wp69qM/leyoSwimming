import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { nextTick } from 'vue';
import ElementPlus from 'element-plus';
import { ElMessage } from 'element-plus';
import KnowledgeDetailModal from '../KnowledgeDetailModal.vue';
import * as knowledgeApi from '@/api/knowledgeManagement';
import { useAdminAuthStore } from '@/stores/adminAuth';
import { createPinia, setActivePinia } from 'pinia';

vi.mock('@/api/knowledgeManagement', () => ({
  getKnowledgeDetail: vi.fn(),
  downloadKnowledgeFile: vi.fn(),
}));

const mockedGetKnowledgeDetail = vi.mocked(knowledgeApi.getKnowledgeDetail);
const mockedDownloadKnowledgeFile = vi.mocked(
  knowledgeApi.downloadKnowledgeFile
);

describe('KnowledgeDetailModal', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    useAdminAuthStore();

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
    mockedDownloadKnowledgeFile.mockResolvedValue();

    vi.spyOn(ElMessage, 'success').mockImplementation(() => {});
    vi.spyOn(ElMessage, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  function mountModal(props = {}) {
    return mount(KnowledgeDetailModal, {
      props: {
        visible: true,
        documentId: 1,
        ...props,
      },
      global: {
        plugins: [ElementPlus],
      },
    });
  }

  it('打开时加载并显示文档详情', async () => {
    const wrapper = mountModal();
    await flushPromises();

    expect(mockedGetKnowledgeDetail).toHaveBeenCalledWith({ documentId: 1 });
    expect(wrapper.text()).toContain('安全须知');
    const textarea = wrapper.find('textarea');
    expect(textarea.element.value).toBe('安全须知内容');
  });

  it('手动输入来源不显示下载按钮', async () => {
    const wrapper = mountModal();
    await flushPromises();

    expect(wrapper.find('button').text()).not.toContain('下载');
  });

  it('文件来源显示下载按钮并触发下载', async () => {
    mockedGetKnowledgeDetail.mockResolvedValueOnce({
      code: 0,
      message: 'success',
      data: {
        documentId: 2,
        title: '蛙泳技巧',
        category: 'technique',
        contentType: 'markdown',
        sourceType: 'file',
        content: '# 蛙泳技巧',
        status: 0,
        createdBy: '管理员',
        createdAt: '2026-09-01T10:00:00Z',
        updatedAt: '2026-09-01T10:00:00Z',
      },
    });

    const wrapper = mountModal({ documentId: 2 });
    await flushPromises();

    const downloadButton = wrapper.findAll('button').find((btn) =>
      btn.text().includes('下载')
    );
    expect(downloadButton).toBeDefined();

    await downloadButton!.trigger('click');
    await flushPromises();

    expect(mockedDownloadKnowledgeFile).toHaveBeenCalledWith({ documentId: 2 });
  });

  it('详情加载失败时提示错误', async () => {
    mockedGetKnowledgeDetail.mockRejectedValueOnce(new Error('加载失败'));
    mountModal();
    await flushPromises();

    expect(ElMessage.error).toHaveBeenCalledWith('加载失败');
  });

  it('关闭弹窗后清空详情数据', async () => {
    const wrapper = mountModal();
    await flushPromises();

    expect(wrapper.text()).toContain('安全须知');

    await wrapper.setProps({ visible: false });
    await nextTick();

    await wrapper.setProps({ visible: true, documentId: 1 });
    await flushPromises();

    expect(mockedGetKnowledgeDetail).toHaveBeenCalledTimes(2);
  });
});
