<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useAssetStore } from '@/stores/asset';

const asset = useAssetStore();
const notice = ref<string | null>(null);
const exportFormat = ref('JSON');
const importFormat = ref('JSON');
const importSourceFileId = ref('');

const fileForm = reactive({
  purpose: 'EVIDENCE',
  originalName: '',
  sizeBytes: '0',
  mimeType: 'application/octet-stream',
  contentHash: '',
  sensitivity: 'PRIVATE'
});

const statusLabels: Record<string, string> = {
  PENDING_UPLOAD: '待上传',
  SCAN: '待扫描',
  READY: '可用',
  REJECTED: '已拒绝',
  DELETING: '清理中',
  DELETED: '已删除'
};

const jobStatusLabels: Record<string, string> = {
  CREATED: '已创建',
  PREVIEWING: '生成预览中',
  PREVIEW_READY: '预览就绪',
  APPLYING: '写入中',
  COMPLETED: '已完成',
  FAILED: '失败'
};

onMounted(async () => {
  await Promise.all([asset.loadFiles(), asset.loadTemplates()]);
});

function formatTime(value: string | null): string {
  return value ? new Date(value).toLocaleString('zh-CN') : '—';
}

/** 登记待扫描文件；未接入对象存储时服务端只建记录，不会产生可下载对象。 */
async function register(): Promise<void> {
  notice.value = null;
  if (!fileForm.originalName.trim() || !fileForm.contentHash.trim()) {
    notice.value = '请填写文件名与内容哈希（SHA-256）';
    return;
  }
  const ok = await asset.registerFile({
    purpose: fileForm.purpose,
    originalName: fileForm.originalName.trim(),
    sizeBytes: fileForm.sizeBytes,
    mimeType: fileForm.mimeType,
    contentHash: fileForm.contentHash.trim(),
    sensitivity: fileForm.sensitivity
  });
  notice.value = ok ? '文件已登记为待扫描状态' : asset.errorMessage;
  if (ok) {
    fileForm.originalName = '';
    fileForm.contentHash = '';
    fileForm.sizeBytes = '0';
  }
}

async function removeFile(fileId: string): Promise<void> {
  const file = asset.files.find((item) => item.fileId === fileId);
  if (!file) {
    return;
  }
  const ok = await asset.deleteFile(file);
  notice.value = ok ? '删除请求已受理（有引用时转清理任务）' : asset.errorMessage;
}

/** 导出本人数据；需要近期认证，且导出包有有效期。 */
async function exportData(): Promise<void> {
  notice.value = null;
  const created = await asset.createExport(JSON.stringify({ domains: ['goal', 'engagement'] }), exportFormat.value);
  notice.value = created
    ? `导出任务 ${created.jobId} 已创建，状态 ${jobStatusLabels[created.status] ?? created.status}`
    : asset.errorMessage;
}

async function importData(): Promise<void> {
  notice.value = null;
  if (!importSourceFileId.value.trim()) {
    notice.value = '请填写已就绪文件的 fileId';
    return;
  }
  const created = await asset.createImport(importSourceFileId.value.trim(), importFormat.value);
  notice.value = created
    ? `导入任务 ${created.jobId} 已创建，确认预览后才写入数据`
    : asset.errorMessage;
}

async function confirmImport(jobId: string): Promise<void> {
  const job = asset.jobs.find((item) => item.jobId === jobId);
  if (!job) {
    return;
  }
  const updated = await asset.confirmImport(job);
  notice.value = updated
    ? `导入状态 ${jobStatusLabels[updated.status] ?? updated.status}`
    : asset.errorMessage;
}

async function refresh(jobId: string): Promise<void> {
  await asset.refreshJob(jobId);
}
</script>

<template>
  <section class="lx-card">
    <header class="lx-row assets__head">
      <div>
        <h2>内容资产</h2>
        <p class="lx-muted">
          文件为私有资产，需先登记并扫描就绪后才可被引用；模板由后台审核发布，客户端只读。
          服务端未接入私有对象存储适配器时，登记文件与导出会返回
          <code>CONTENT_STORAGE_UNAVAILABLE</code> 并失败关闭——这是设计上的安全行为，不会产生假成功。
        </p>
      </div>
      <span class="lx-tag">共 {{ asset.fileTotal }} 个文件</span>
    </header>

    <p v-if="asset.errorMessage" class="lx-error">{{ asset.errorMessage }}</p>
    <p v-if="notice" class="lx-muted">{{ notice }}</p>

    <h3 class="assets__section">登记文件</h3>
    <div class="assets__form">
      <label class="lx-muted">
        用途
        <select v-model="fileForm.purpose" class="lx-input">
          <option value="EVIDENCE">打卡证据</option>
          <option value="AVATAR">头像</option>
          <option value="IMPORT_SOURCE">导入源文件</option>
          <option value="NOTE_ATTACHMENT">笔记附件</option>
        </select>
      </label>
      <label class="lx-muted">
        敏感级别
        <select v-model="fileForm.sensitivity" class="lx-input">
          <option value="PUBLIC">公开</option>
          <option value="PRIVATE">私有</option>
          <option value="SENSITIVE">敏感</option>
        </select>
      </label>
      <label class="lx-muted">
        文件名
        <input v-model="fileForm.originalName" class="lx-input" placeholder="reading-plan.csv" />
      </label>
      <label class="lx-muted">
        大小（字节）
        <input v-model="fileForm.sizeBytes" class="lx-input" inputmode="numeric" />
      </label>
      <label class="lx-muted">
        MIME
        <input v-model="fileForm.mimeType" class="lx-input" />
      </label>
      <label class="lx-muted">
        内容哈希（SHA-256）
        <input v-model="fileForm.contentHash" class="lx-input" placeholder="64 位十六进制" />
      </label>
      <button class="lx-button" @click="register">登记为待扫描</button>
    </div>

    <h3 class="assets__section">我的文件</h3>
    <div v-if="asset.files.length === 0" class="lx-empty">还没有文件记录。</div>
    <table v-else class="lx-table">
      <thead>
        <tr>
          <th>文件名</th>
          <th>用途</th>
          <th>状态</th>
          <th>大小</th>
          <th>敏感级别</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="file in asset.files" :key="file.fileId">
          <td>{{ file.originalName }}</td>
          <td>{{ file.purpose }}</td>
          <td>
            <span class="lx-tag" :class="{ 'lx-tag--warn': file.status !== 'READY' }">
              {{ statusLabels[file.status] ?? file.status }}
            </span>
          </td>
          <td>{{ file.sizeBytes }}</td>
          <td>{{ file.sensitivity }}</td>
          <td>
            <button class="lx-button lx-button--ghost" @click="removeFile(file.fileId)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>

    <h3 class="assets__section">内容模板（只读）</h3>
    <div v-if="asset.templates.length === 0" class="lx-empty">当前没有适用于你的已发布模板。</div>
    <table v-else class="lx-table">
      <thead>
        <tr>
          <th>模板版本</th>
          <th>年龄范围</th>
          <th>状态</th>
          <th>发布时间</th>
          <th>审核备注</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="template in asset.templates" :key="template.versionId">
          <td>{{ template.templateId }} · v{{ template.versionNo }}</td>
          <td>{{ template.ageScope }}</td>
          <td>{{ template.status }}</td>
          <td>{{ formatTime(template.publishedAt) }}</td>
          <td>{{ template.reviewReason ?? '—' }}</td>
        </tr>
      </tbody>
    </table>

    <h3 class="assets__section">导入导出</h3>
    <div class="assets__transfers">
      <div class="lx-card assets__transfer">
        <strong>导出我的数据</strong>
        <p class="lx-muted">需要近期认证；导出包由服务端生成，并有有效期。</p>
        <div class="lx-row">
          <select v-model="exportFormat" class="lx-input">
            <option value="JSON">JSON</option>
            <option value="CSV">CSV</option>
          </select>
          <button class="lx-button" @click="exportData">创建导出任务</button>
        </div>
      </div>
      <div class="lx-card assets__transfer">
        <strong>导入数据</strong>
        <p class="lx-muted">先创建任务生成预览，确认后才写入；源文件必须已扫描就绪。</p>
        <label class="lx-muted">
          源文件 fileId
          <input v-model="importSourceFileId" class="lx-input" inputmode="numeric" />
        </label>
        <div class="lx-row">
          <select v-model="importFormat" class="lx-input">
            <option value="JSON">JSON</option>
            <option value="CSV">CSV</option>
          </select>
          <button class="lx-button" @click="importData">创建导入任务</button>
        </div>
      </div>
    </div>

    <div v-if="asset.jobs.length > 0">
      <h3 class="assets__section">传输任务</h3>
      <table class="lx-table">
        <thead>
          <tr>
            <th>任务</th>
            <th>类型</th>
            <th>状态</th>
            <th>结果文件</th>
            <th>过期时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="job in asset.jobs" :key="job.jobId">
            <td>{{ job.jobId }}</td>
            <td>{{ job.type }}</td>
            <td>
              <span class="lx-tag" :class="{ 'lx-tag--warn': job.status !== 'COMPLETED' }">
                {{ jobStatusLabels[job.status] ?? job.status }}
              </span>
            </td>
            <td>{{ job.resultFileId ?? '—' }}</td>
            <td>{{ formatTime(job.expiresAt) }}</td>
            <td class="lx-row">
              <button class="lx-button lx-button--ghost" @click="refresh(job.jobId)">刷新</button>
              <button
                v-if="job.type === 'IMPORT' && job.status === 'PREVIEW_READY'"
                class="lx-button lx-button--ghost"
                @click="confirmImport(job.jobId)"
              >
                确认导入
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<style scoped>
.assets__head {
  justify-content: space-between;
}

.assets__section {
  margin: var(--lx-space-5) 0 var(--lx-space-3);
}

.assets__form {
  display: grid;
  gap: var(--lx-space-3);
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
}

.assets__transfers {
  display: grid;
  gap: var(--lx-space-4);
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.assets__transfer {
  display: flex;
  flex-direction: column;
  gap: var(--lx-space-2);
}
</style>
