import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { nextTick } from 'vue';
import ElementPlus from 'element-plus';
import { ElMessage } from 'element-plus';
import KnowledgeUploadModal from '../KnowledgeUploadModal.vue';
import * as knowledgeApi from '@/api/knowledgeManagement';

vi.mock('@/api/knowledgeManagement', () => ({
  addKnowledge: vi.fn(),
  uploadKnowledgeFile: vi.fn(),
}));

const mockedAddKnowledge = vi.mocked(knowledgeApi.addKnowledge);
const mockedUploadKnowledgeFile = vi.mocked(
  knowledgeApi.uploadKnowledgeFile
);

describe('KnowledgeUploadModal', () => {
  beforeEach(() => {
    mockedAddKnowledge.mockResolvedValue({
      code: 0,
      message: 'success',
      data: null,
    });
    mockedUploadKnowledgeFile.mockResolvedValue({
      code: 0,
      message: 'success',
      data: null,
    });

    vi.spyOn(ElMessage, 'success').mockImplementation(() => {});
    vi.spyOn(ElMessage, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  function mountModal(props = { visible: true }) {
    return mount(KnowledgeUploadModal, {
      props,
      global: {
        plugins: [ElementPlus],
      },
      attachTo: document.body,
    });
  }

  async function switchToFileTab(wrapper: ReturnType<typeof mountModal>) {
    const fileRadio = wrapper
      .findAll('.el-radio-button')
      .find((radio) => radio.text().includes('文件上传'));
    await fileRadio?.trigger('click');
    await nextTick();
    await flushPromises();
  }

  async function selectCategory(
    wrapper: ReturnType<typeof mountModal>,
    value = 'safety'
  ) {
    const categorySelect = wrapper.findComponent({ name: 'ElSelect' });
    categorySelect.vm.$emit('update:modelValue', value);
    await flushPromises();
  }

  it('visible 为 true 时渲染弹窗', async () => {
    const wrapper = mountModal();
    await nextTick();
    expect(wrapper.find('.el-dialog').exists()).toBe(true);
  });

  it('默认选中手动输入 Tab', async () => {
    const wrapper = mountModal();
    await nextTick();

    const radios = wrapper.findAll('.el-radio-button');
    const manualRadio = radios.find((radio) =>
      radio.text().includes('手动输入')
    );
    expect(manualRadio).toBeDefined();
    expect(manualRadio?.classes()).toContain('is-active');

    expect(wrapper.text()).toContain('内容类型');
    expect(wrapper.find('[data-testid="content-type-radio"]').exists()).toBe(
      true
    );
  });

  it('切换到文件上传 Tab 显示上传组件', async () => {
    const wrapper = mountModal();
    await nextTick();

    await switchToFileTab(wrapper);

    expect(wrapper.find('[data-testid="file-upload"]').isVisible()).toBe(true);
    expect(wrapper.find('[data-testid="content-type-radio"]').exists()).toBe(
      false
    );
  });

  it('手动输入表单校验必填项', { retry: 5 }, async () => {
    const wrapper = mountModal();
    await nextTick();
    await flushPromises();
    await new Promise((resolve) => setTimeout(resolve, 0));
    await nextTick();

    await wrapper.find('[data-testid="submit-button"]').trigger('click');
    await flushPromises();
    await nextTick();

    expect(mockedAddKnowledge).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('请输入标题');
    expect(wrapper.text()).toContain('请选择分类');
    expect(wrapper.text()).toContain('请输入内容');
  });

  it('手动输入填写完整后提交 addKnowledge', async () => {
    const wrapper = mountModal();
    await nextTick();

    const titleInput = wrapper.find('[data-testid="title-input"]');
    const contentInput = wrapper.find('[data-testid="content-textarea"]');

    await titleInput.setValue('安全须知');
    await selectCategory(wrapper, 'safety');

    await contentInput.setValue('泳池安全注意事项');
    await wrapper.find('[data-testid="submit-button"]').trigger('click');
    await flushPromises();

    expect(mockedAddKnowledge).toHaveBeenCalledWith(
      expect.objectContaining({
        title: '安全须知',
        category: 'safety',
        contentType: 'text',
        content: '泳池安全注意事项',
      })
    );
    expect(ElMessage.success).toHaveBeenCalledWith('保存成功');
    expect(wrapper.emitted('success')).toBeTruthy();
  });

  it('文件上传提交 uploadKnowledgeFile', { retry: 5 }, async () => {
    const wrapper = mountModal();
    await nextTick();

    await switchToFileTab(wrapper);

    const titleInput = wrapper.find('[data-testid="title-input"]');
    await titleInput.setValue('文件文档');

    const file = new File(['content'], 'doc.md', { type: 'text/markdown' });
    const upload = wrapper.findComponent({ name: 'ElUpload' });
    upload.vm.$emit('change', { raw: file });
    await nextTick();

    await selectCategory(wrapper, 'safety');

    await wrapper.find('[data-testid="submit-button"]').trigger('click');
    await flushPromises();

    expect(mockedUploadKnowledgeFile).toHaveBeenCalled();
    expect(ElMessage.success).toHaveBeenCalledWith('上传成功');
  });

  it('API 失败时显示错误信息且不关闭弹窗', async () => {
    mockedAddKnowledge.mockRejectedValueOnce(new Error('标题已存在'));
    const wrapper = mountModal();
    await nextTick();

    const titleInput = wrapper.find('[data-testid="title-input"]');
    const contentInput = wrapper.find('[data-testid="content-textarea"]');

    await titleInput.setValue('安全须知');
    await selectCategory(wrapper, 'safety');
    await contentInput.setValue('内容');

    await wrapper.find('[data-testid="submit-button"]').trigger('click');
    await flushPromises();

    expect(ElMessage.error).toHaveBeenCalledWith('标题已存在');
    expect(wrapper.emitted('update:visible')).toBeFalsy();
  });

  it('点击取消按钮关闭弹窗', async () => {
    const wrapper = mountModal();
    await nextTick();

    await wrapper.find('[data-testid="cancel-button"]').trigger('click');
    await flushPromises();

    expect(wrapper.emitted('update:visible')?.[0]).toEqual([false]);
  });
});
