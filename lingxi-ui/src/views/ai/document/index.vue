<template>
  <div class="app-container ai-tool-page">
    <el-row :gutter="20">
      <el-col :xl="10" :lg="11" :md="24" :sm="24" :xs="24">
        <el-card class="tool-card input-card">
          <div slot="header" class="card-header">
            <span>AI 公文助手</span>
            <el-tag size="mini" type="warning">生成 / 润色 / 压缩 / 扩写</el-tag>
          </div>

          <div class="input-card-body">
            <el-form ref="form" :model="form" :rules="rules" label-width="96px" class="input-form">
              <el-form-item label="处理模式" prop="mode">
                <el-radio-group v-model="form.mode">
                  <el-radio-button label="generate">生成成稿</el-radio-button>
                  <el-radio-button label="polish">润色提升</el-radio-button>
                  <el-radio-button label="compress">压缩精简</el-radio-button>
                  <el-radio-button label="expand">扩写丰富</el-radio-button>
                </el-radio-group>
              </el-form-item>

              <el-form-item label="文稿类型" prop="documentType">
                <el-select v-model="form.documentType" style="width: 100%">
                  <el-option label="通知" value="通知" />
                  <el-option label="请示" value="请示" />
                  <el-option label="汇报" value="汇报" />
                  <el-option label="会议纪要" value="会议纪要" />
                  <el-option label="工作方案" value="工作方案" />
                  <el-option label="简报" value="简报" />
                  <el-option label="发言稿" value="发言稿" />
                </el-select>
              </el-form-item>

              <el-form-item label="语气风格" prop="tone">
                <el-select v-model="form.tone" style="width: 100%">
                  <el-option label="正式规范" value="正式规范" />
                  <el-option label="领导汇报" value="领导汇报" />
                  <el-option label="简洁明快" value="简洁明快" />
                  <el-option label="稳健审慎" value="稳健审慎" />
                </el-select>
              </el-form-item>

              <el-form-item label="主题" prop="topic">
                <el-input
                  v-model="form.topic"
                  placeholder="例如：关于开展二季度安全生产专项检查的通知"
                />
              </el-form-item>

              <el-form-item label="背景信息">
                <el-input
                  v-model="form.background"
                  type="textarea"
                  :rows="4"
                  placeholder="补充文稿背景、对象、目的、组织安排等信息"
                />
              </el-form-item>

              <el-form-item label="原始内容">
                <el-input
                  v-model="form.sourceContent"
                  type="textarea"
                  :rows="8"
                  placeholder="如果是润色、压缩或扩写，请粘贴原文；如果是生成，可留空"
                />
              </el-form-item>

              <el-form-item label="附加要求">
                <el-input
                  v-model="form.requirements"
                  type="textarea"
                  :rows="4"
                  placeholder="例如：控制在800字以内，分三部分，结尾增加落实要求"
                />
              </el-form-item>
            </el-form>

            <div class="action-bar">
              <el-button type="primary" size="medium" :loading="loading" @click="handleSubmit">
                {{ loading ? '生成中...' : '立即生成' }}
              </el-button>
              <el-button size="medium" @click="handleReset">重置</el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xl="14" :lg="13" :md="24" :sm="24" :xs="24">
        <el-card class="tool-card result-card">
          <div slot="header" class="card-header">
            <span>生成结果</span>
            <el-tag v-if="result.documentType" size="mini">{{ result.documentType }}</el-tag>
          </div>

          <div v-if="!hasResult" class="empty-state">
            <i class="el-icon-document-copy" />
            <p>填写左侧信息后生成公文内容</p>
          </div>

          <div v-else class="result-wrapper">
            <div class="result-meta">
              <h2>{{ result.title }}</h2>
              <div class="meta-badges">
                <el-tag size="mini" type="info">{{ result.tone }}</el-tag>
                <el-tag size="mini" type="success">{{ result.documentType }}</el-tag>
              </div>
              <p class="summary">{{ result.summary }}</p>
            </div>

            <el-row :gutter="16" class="result-panels">
              <el-col :span="10">
                <div class="mini-panel">
                  <div class="panel-title">结构提纲</div>
                  <ul class="plain-list">
                    <li v-for="(item, index) in result.outline" :key="`outline-${index}`">{{ item }}</li>
                  </ul>
                </div>
                <div class="mini-panel">
                  <div class="panel-title">润色重点</div>
                  <ul class="plain-list">
                    <li v-for="(item, index) in result.polishPoints" :key="`point-${index}`">{{ item }}</li>
                  </ul>
                </div>
              </el-col>
              <el-col :span="14">
                <div class="mini-panel content-panel">
                  <div class="panel-title">正文内容</div>
                  <el-button
                    class="copy-content-btn"
                    type="text"
                    size="mini"
                    icon="el-icon-document-copy"
                    @click="handleCopyContent"
                  >复制</el-button>
                  <div class="document-content">{{ result.content }}</div>
                </div>
              </el-col>
            </el-row>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { writeDocument } from '@/api/ai/tool'

const defaultForm = () => ({
  mode: 'generate',
  documentType: '通知',
  tone: '正式规范',
  topic: '',
  background: '',
  sourceContent: '',
  requirements: ''
})

export default {
  name: 'AiDocumentTool',
  data() {
    return {
      loading: false,
      form: defaultForm(),
      result: {},
      rules: {
        mode: [{ required: true, message: '请选择处理模式', trigger: 'change' }],
        documentType: [{ required: true, message: '请选择文稿类型', trigger: 'change' }],
        tone: [{ required: true, message: '请选择语气风格', trigger: 'change' }],
        topic: [{ required: true, message: '请输入主题', trigger: 'blur' }]
      }
    }
  },
  computed: {
    hasResult() {
      return !!(this.result && this.result.content)
    }
  },
  methods: {
    handleSubmit() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        this.loading = true
        writeDocument(this.form).then(res => {
          this.result = res.data || {}
          this.$message.success('公文内容已生成')
        }).finally(() => {
          this.loading = false
        })
      })
    },
    handleReset() {
      this.form = defaultForm()
      this.result = {}
      this.$nextTick(() => {
        this.$refs.form && this.$refs.form.clearValidate()
      })
    },
    handleCopyContent() {
      if (!this.result || !this.result.content) {
        this.$message.warning('暂无可复制内容')
        return
      }
      const text = this.result.content
      if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(text).then(() => {
          this.$message.success('正文内容已复制')
        }).catch(() => {
          this.copyContentByTextarea(text)
        })
        return
      }
      this.copyContentByTextarea(text)
    },
    copyContentByTextarea(text) {
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.setAttribute('readonly', 'readonly')
      textarea.style.position = 'fixed'
      textarea.style.left = '-9999px'
      document.body.appendChild(textarea)
      textarea.select()
      try {
        document.execCommand('copy')
        this.$message.success('正文内容已复制')
      } catch (e) {
        this.$message.error('复制失败，请手动复制')
      } finally {
        document.body.removeChild(textarea)
      }
    }
  }
}
</script>

<style scoped>
.ai-tool-page {
  min-height: calc(100vh - 120px);
  background: transparent;
}

.tool-card {
  border-color: rgba(255, 255, 255, 0.08);
  background: rgba(22, 30, 46, 0.7);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.input-card ::v-deep .el-card__body {
  padding: 0;
}

.result-card ::v-deep .el-card__body {
  max-height: calc(100vh - 190px);
  overflow-y: auto;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  color: #e2e8f0;
}

.input-card-body {
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 150px);
}

.input-form {
  flex: 1;
  overflow-y: auto;
  padding: 20px 20px 8px;
}

.action-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  position: sticky;
  bottom: 0;
  z-index: 5;
  padding: 12px 20px 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(22, 30, 46, 0.88);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  box-shadow: 0 -8px 24px rgba(0, 0, 0, 0.18);
}

.empty-state {
  min-height: calc(100vh - 230px);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  color: #94a3b8;
  gap: 16px;
}

.empty-state i {
  font-size: 52px;
}

.result-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-meta h2 {
  margin: 0 0 8px;
  color: #e2e8f0;
  font-size: 24px;
}

.meta-badges {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.summary {
  margin: 0;
  color: #cbd5e1;
  line-height: 1.7;
}

.result-panels {
  margin-top: 8px;
}

.mini-panel {
  background: rgba(30, 41, 59, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  padding: 16px;
  margin-bottom: 16px;
}

.panel-title {
  font-size: 14px;
  font-weight: 700;
  color: #60a5fa;
  margin-bottom: 12px;
}

.plain-list {
  margin: 0;
  padding-left: 18px;
  color: #cbd5e1;
  line-height: 1.8;
}

.content-panel {
  position: relative;
  margin-bottom: 0;
}

.copy-content-btn {
  position: absolute;
  top: 10px;
  right: 16px;
  color: #93c5fd;
  padding: 0;
}

.copy-content-btn:hover {
  color: #bfdbfe;
}

.document-content {
  white-space: pre-wrap;
  line-height: 1.9;
  color: #e2e8f0;
  max-height: 420px;
  overflow-y: auto;
}

</style>
