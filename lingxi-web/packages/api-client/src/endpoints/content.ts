import type { ApiClient, LongId, PageResult } from '../http';
import type { FileResult, TemplateVersionResult, TransferJobResult } from '../types';

/** 私有文件、内容模板与导入导出接口客户端。 */
export class ContentApi {
  constructor(private readonly client: ApiClient) {}

  /** 分页查询本人拥有的文件（含未完成上传与待清理记录）。 */
  listMyFiles(page = 1, pageSize = 20): Promise<PageResult<FileResult>> {
    return this.client.send<PageResult<FileResult>>('/api/v1/files', {
      query: { page, pageSize }
    });
  }

  /** 查询单个已就绪文件。 */
  getFile(fileId: LongId): Promise<FileResult> {
    return this.client.send<FileResult>(`/api/v1/files/${fileId}`);
  }

  /**
   * 申请上传凭证。
   *
   * <p>服务端只创建 SCAN 状态的文件记录并返回对象存储凭证；本机无对象存储适配器时，
   * 后续 `markUploaded` 会失败关闭，因此页面按“仅登记待扫描文件”展示。
   */
  createUploadTicket(
    body: {
      purpose: string;
      originalName: string;
      sizeBytes: LongId;
      mimeType: string;
      contentHash: string;
      sensitivity: string;
    },
    idempotencyKey: string
  ): Promise<unknown> {
    return this.client.send<unknown>('/api/v1/files/upload-tickets', {
      method: 'POST',
      body,
      idempotencyKey
    });
  }

  /** 标记文件已上传，进入扫描流程。 */
  markUploaded(fileId: LongId, expectedVersion: LongId): Promise<FileResult> {
    return this.client.send<FileResult>(`/api/v1/files/${fileId}/uploaded`, {
      method: 'POST',
      body: { expectedVersion }
    });
  }

  /** 删除文件；文件被引用时服务端会转清理任务而不是直接删除。 */
  deleteFile(fileId: LongId, expectedVersion: LongId): Promise<FileResult> {
    return this.client.send<FileResult>(`/api/v1/files/${fileId}`, {
      method: 'DELETE',
      query: { expectedVersion }
    });
  }

  /** 查询已发布内容模板；服务端按账号年龄范围过滤。 */
  listTemplates(): Promise<TemplateVersionResult[]> {
    return this.client.send<TemplateVersionResult[]>('/api/v1/templates');
  }

  /** 创建导入任务，服务端异步生成预览。 */
  createImportJob(
    body: { sourceFileId: LongId; format: string },
    idempotencyKey: string
  ): Promise<TransferJobResult> {
    return this.client.send<TransferJobResult>('/api/v1/import-jobs', {
      method: 'POST',
      body,
      idempotencyKey
    });
  }

  /** 确认导入预览结果并落库。 */
  confirmImportJob(jobId: LongId, expectedVersion: LongId): Promise<TransferJobResult> {
    return this.client.send<TransferJobResult>(`/api/v1/import-jobs/${jobId}/confirm`, {
      method: 'POST',
      body: { expectedVersion }
    });
  }

  /** 创建导出任务；需要近期认证，异步生成导出文件。 */
  createExportJob(
    body: { scopeJson: string; format: string },
    idempotencyKey: string
  ): Promise<TransferJobResult> {
    return this.client.send<TransferJobResult>('/api/v1/export-jobs', {
      method: 'POST',
      body,
      idempotencyKey
    });
  }

  /** 查询导入导出任务状态与结果文件。 */
  getTransferJob(jobId: LongId): Promise<TransferJobResult> {
    return this.client.send<TransferJobResult>(`/api/v1/transfer-jobs/${jobId}`);
  }
}
