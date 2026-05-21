<template>
  <div class="app-container ai-tool-page report-page">
    <el-row :gutter="20">
      <el-col :xl="9" :lg="10" :md="24" :sm="24" :xs="24">
        <el-card class="tool-card input-card">
          <div slot="header" class="card-header">
            <span>AI 报表解读</span>
            <el-tag size="mini" type="success">Excel / CSV / 粘贴表格</el-tag>
          </div>

          <el-form ref="form" :model="form" :rules="rules" label-width="96px">
            <el-form-item label="报表名称" prop="reportTitle">
              <el-input v-model="form.reportTitle" placeholder="例如：2026年4月销售日报" />
            </el-form-item>

            <el-form-item label="业务背景">
              <el-input
                v-model="form.businessContext"
                type="textarea"
                :rows="4"
                placeholder="补充业务口径、区域范围、组织结构或周期说明"
              />
            </el-form-item>

            <el-form-item label="分析重点">
              <el-input
                v-model="form.analysisFocus"
                type="textarea"
                :rows="4"
                placeholder="例如：关注同比下滑、区域排名变化、异常高值和库存风险"
              />
            </el-form-item>

            <el-form-item label="上传报表">
              <el-upload
                class="report-upload"
                drag
                action="#"
                :auto-upload="false"
                :show-file-list="true"
                :limit="1"
                :on-change="handleFileChange"
                :on-remove="handleFileRemove"
              >
                <i class="el-icon-upload"></i>
                <div class="el-upload__text">将文件拖到此处，或<em>点击选择</em></div>
                <div slot="tip" class="el-upload__tip">支持 `.xlsx`、`.xls`、`.csv`，也可以不上传，直接在下方粘贴表格内容</div>
              </el-upload>
            </el-form-item>

            <el-form-item label="表格内容">
              <el-input
                v-model="form.tableContent"
                type="textarea"
                :rows="10"
                placeholder="可直接粘贴 Excel 表格文本，例如：日期 | 区域 | 销售额 | 同比"
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="loading" @click="handleSubmit">
                {{ loading ? '解读中...' : '开始解读' }}
              </el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xl="15" :lg="14" :md="24" :sm="24" :xs="24">
        <el-card class="tool-card result-card">
          <div slot="header" class="card-header">
            <span>分析结果</span>
            <el-tag v-if="result.reportTitle" size="mini" type="info">{{ result.reportTitle }}</el-tag>
          </div>

          <div v-if="!hasResult" class="empty-state">
            <i class="el-icon-data-analysis" />
            <p>上传报表或粘贴表格内容后开始解读</p>
          </div>

          <div v-else class="result-wrapper">
            <div class="summary-card">
              <h2>{{ result.reportTitle }}</h2>
              <p>{{ result.summary }}</p>
            </div>

            <el-row :gutter="16">
              <el-col :span="8">
                <div class="mini-panel">
                  <div class="panel-title">关键发现</div>
                  <ul class="plain-list">
                    <li v-for="(item, index) in result.keyFindings" :key="`finding-${index}`">{{ item }}</li>
                  </ul>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="mini-panel">
                  <div class="panel-title">风险提示</div>
                  <ul class="plain-list">
                    <li v-for="(item, index) in result.risks" :key="`risk-${index}`">{{ item }}</li>
                  </ul>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="mini-panel">
                  <div class="panel-title">行动建议</div>
                  <ul class="plain-list">
                    <li v-for="(item, index) in result.suggestions" :key="`suggestion-${index}`">{{ item }}</li>
                  </ul>
                </div>
              </el-col>
            </el-row>

            <el-row :gutter="16" class="detail-panels">
              <el-col :span="12">
                <div class="mini-panel text-panel">
                  <div class="panel-title">趋势分析</div>
                  <div class="rich-text">{{ result.trendAnalysis }}</div>
                </div>
              </el-col>
              <el-col :span="12">
                <div class="mini-panel text-panel">
                  <div class="panel-title">管理层摘要</div>
                  <div class="rich-text">{{ result.managementBrief }}</div>
                </div>
              </el-col>
            </el-row>

            <div class="mini-panel preview-panel">
              <div class="panel-title">解析后的表格预览</div>
              <pre class="table-preview">{{ result.parsedTablePreview }}</pre>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { interpretReport } from '@/api/ai/tool'

const defaultForm = () => ({
  reportTitle: '',
  analysisFocus: '',
  businessContext: '',
  tableContent: ''
})

export default {
  name: 'AiReportTool',
  data() {
    return {
      loading: false,
      form: defaultForm(),
      result: {},
      uploadFile: null,
      rules: {
        reportTitle: [{ required: true, message: '请输入报表名称', trigger: 'blur' }]
      }
    }
  },
  computed: {
    hasResult() {
      return !!(this.result && this.result.summary)
    }
  },
  methods: {
    handleFileChange(file) {
      this.uploadFile = file.raw
    },
    handleFileRemove() {
      this.uploadFile = null
    },
    handleSubmit() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        if (!this.uploadFile && !this.form.tableContent) {
          this.$message.warning('请上传报表文件或粘贴表格内容')
          return
        }
        const data = new FormData()
        Object.keys(this.form).forEach(key => {
          const value = this.form[key]
          if (value !== undefined && value !== null) {
            data.append(key, value)
          }
        })
        if (this.uploadFile) {
          data.append('file', this.uploadFile)
        }
        this.loading = true
        interpretReport(data).then(res => {
          this.result = res.data || {}
          this.$message.success('报表解读完成')
        }).finally(() => {
          this.loading = false
        })
      })
    },
    handleReset() {
      this.form = defaultForm()
      this.result = {}
      this.uploadFile = null
      this.$nextTick(() => {
        this.$refs.form && this.$refs.form.clearValidate()
      })
    }
  }
}
</script>

<style scoped>
.report-page {
  background: transparent;
}

.ai-tool-page {
  min-height: calc(100vh - 120px);
}

.tool-card {
  border: none;
  border-radius: 18px;
  box-shadow: 0 14px 40px rgba(15, 118, 110, 0.08);
}

.result-card {
  border-color: rgba(255, 255, 255, 0.08);
  background: rgba(22, 30, 46, 0.7);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  color: #0f766e;
}

.result-card .card-header {
  color: #e2e8f0;
}

.input-card ::v-deep .el-card__body {
  max-height: calc(100vh - 150px);
  overflow-y: auto;
}

.result-card ::v-deep .el-card__body {
  max-height: calc(100vh - 190px);
  overflow-y: auto;
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

.summary-card {
  background: rgba(30, 41, 59, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 18px 20px;
  margin-bottom: 16px;
}

.summary-card h2 {
  margin: 0 0 8px;
  color: #e2e8f0;
}

.summary-card p {
  margin: 0;
  color: #cbd5e1;
  line-height: 1.8;
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

.text-panel,
.preview-panel {
  margin-bottom: 16px;
}

.rich-text {
  white-space: pre-wrap;
  color: #e2e8f0;
  line-height: 1.9;
}

.table-preview {
  margin: 0;
  max-height: 240px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-word;
  color: #e2e8f0;
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  line-height: 1.7;
}

</style>
