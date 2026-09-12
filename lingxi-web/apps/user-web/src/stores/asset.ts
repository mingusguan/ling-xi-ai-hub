import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { FileResult, TemplateVersionResult, TransferJobResult } from '@lingxi/api-client';
import { api } from '@/api/client';
import { useSessionStore } from '@/stores/session';

/** 私有文件、内容模板与导入导出任务状态。 */
export const useAssetStore = defineStore('asset', () => {
  const files = ref<FileResult[]>([]);
  /** 文件总数为服务端 long，按字符串展示，禁止 Number 转换。 */
  const fileTotal = ref<string>('0');
  const templates = ref<TemplateVersionResult[]>([]);
  const jobs = ref<TransferJobResult[]>([]);
  /** 最近一次导出/导入任务，用于持续查询状态。 */
  const currentJob = ref<TransferJobResult | null>(null);
  const loading = ref(false);
  const errorMessage = ref<string | null>(null);

  async function guard<T>(action: () => Promise<T>): Promise<T | null> {
    const session = useSessionStore();
    loading.value = true;
    errorMessage.value = null;
    try {
      await session.ensureFreshToken();
      return await action();
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '请求失败';
      return null;
    } finally {
      loading.value = false;
    }
  }

  /** 加载本人文件列表。 */
  async function loadFiles(page = 1, pageSize = 50): Promise<void> {
    const result = await guard(() => api.content.listMyFiles(page, pageSize));
    if (result) {
      files.value = result.items;
      fileTotal.value = result.total;
    }
  }

  /** 加载已发布内容模板；服务端按账号年龄范围过滤。 */
  async function loadTemplates(): Promise<void> {
    const result = await guard(() => api.content.listTemplates());
    if (result) {
      templates.value = result;
    }
  }

  /**
   * 登记待扫描文件。
   *
   * <p>服务端在未接入私有对象存储适配器时直接失败关闭（`CONTENT_STORAGE_UNAVAILABLE`），
   * 不会产生假的成功记录；这里把该业务错误透出给页面，让用户明确知道原因。
   */
  async function registerFile(input: {
    purpose: string;
    originalName: string;
    sizeBytes: string;
    mimeType: string;
    contentHash: string;
    sensitivity: string;
  }): Promise<boolean> {
    const result = await guard(() =>
      api.content.createUploadTicket(input, api.http.newIdempotencyKey())
    );
    if (result !== null) {
      await loadFiles();
      return true;
    }
    return false;
  }

  /** 删除文件；被引用时服务端转清理任务。 */
  async function deleteFile(file: FileResult): Promise<boolean> {
    const result = await guard(() => api.content.deleteFile(file.fileId, file.version));
    if (result !== null) {
      await loadFiles();
      return true;
    }
    return false;
  }

  /** 创建导出任务（需要近期认证）。 */
  async function createExport(scopeJson: string, format: string): Promise<TransferJobResult | null> {
    const created = await guard(() =>
      api.content.createExportJob({ scopeJson, format }, api.http.newIdempotencyKey())
    );
    if (created) {
      jobs.value = [created, ...jobs.value];
      currentJob.value = created;
    }
    return created;
  }

  /** 创建导入任务（服务端异步生成预览）。 */
  async function createImport(
    sourceFileId: string,
    format: string
  ): Promise<TransferJobResult | null> {
    const created = await guard(() =>
      api.content.createImportJob({ sourceFileId, format }, api.http.newIdempotencyKey())
    );
    if (created) {
      jobs.value = [created, ...jobs.value];
      currentJob.value = created;
    }
    return created;
  }

  /** 确认导入预览结果并落库。 */
  async function confirmImport(job: TransferJobResult): Promise<TransferJobResult | null> {
    const updated = await guard(() =>
      api.content.confirmImportJob(job.jobId, job.version)
    );
    if (updated) {
      jobs.value = jobs.value.map((item) => (item.jobId === updated.jobId ? updated : item));
      currentJob.value = updated;
    }
    return updated;
  }

  /** 刷新单个任务状态。 */
  async function refreshJob(jobId: string): Promise<void> {
    const result = await guard(() => api.content.getTransferJob(jobId));
    if (result) {
      jobs.value = jobs.value.map((item) => (item.jobId === result.jobId ? result : item));
      currentJob.value = result;
    }
  }

  return {
    files,
    fileTotal,
    templates,
    jobs,
    currentJob,
    loading,
    errorMessage,
    loadFiles,
    loadTemplates,
    registerFile,
    deleteFile,
    createExport,
    createImport,
    confirmImport,
    refreshJob
  };
});
