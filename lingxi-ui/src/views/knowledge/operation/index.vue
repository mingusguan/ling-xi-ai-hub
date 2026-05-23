<template>
  <div class="app-container knowledge-operation-page">
    <el-row :gutter="12" class="stat-row">
      <el-col v-for="item in statCards" :key="item.key" :span="4">
        <div class="stat-box">
          <div class="stat-label">{{ item.label }}</div>
          <div class="stat-value">{{ item.value }}</div>
        </div>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <el-form :model="queryParams" inline size="small">
        <el-form-item label="关键词">
          <el-input v-model="queryParams.keyword" clearable placeholder="问题或答案" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="无答案">
          <el-select v-model="queryParams.noAnswer" clearable placeholder="全部" style="width: 120px">
            <el-option label="是" value="1" />
            <el-option label="否" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="可信度">
          <el-select v-model="queryParams.confidenceLevel" clearable placeholder="全部" style="width: 130px">
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="低" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="反馈">
          <el-select v-model="queryParams.feedback" clearable placeholder="全部" style="width: 140px">
            <el-option label="有帮助" value="HELPFUL" />
            <el-option label="需改进" value="UNHELPFUL" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="question" label="问题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="answer" label="答案摘要" min-width="260" show-overflow-tooltip />
        <el-table-column prop="confidenceLevel" label="可信度" width="90">
          <template slot-scope="scope">
            <el-tag :type="confidenceType(scope.row.confidenceLevel)" size="mini">{{ formatConfidence(scope.row.confidenceLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="topScore" label="最高分" width="90">
          <template slot-scope="scope">{{ formatScore(scope.row.topScore) }}</template>
        </el-table-column>
        <el-table-column prop="noAnswer" label="无答案" width="80">
          <template slot-scope="scope">
            <el-tag :type="scope.row.noAnswer === '1' ? 'danger' : 'success'" size="mini">{{ scope.row.noAnswer === '1' ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="feedback" label="反馈" width="100">
          <template slot-scope="scope">{{ formatFeedback(scope.row.feedback) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click="showDetail(scope.row)">详情</el-button>
            <el-button type="text" size="mini" @click="openFeedback(scope.row)">反馈</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="queryParams.pageNum"
        :limit.sync="queryParams.pageSize"
        @pagination="getList"
      />
    </el-card>

    <el-dialog title="问答详情" :visible.sync="detailVisible" width="720px" append-to-body>
      <div v-if="current">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="问题">{{ current.question }}</el-descriptions-item>
          <el-descriptions-item label="答案">{{ current.answer }}</el-descriptions-item>
          <el-descriptions-item label="引用">
            <div v-for="(source, index) in parseSources(current.sourceChunks)" :key="index" class="source-item">
              <div class="source-title">[{{ index + 1 }}] 文档 {{ source.documentId }} / 片段 {{ source.chunkId }} / {{ formatScore(source.score) }}</div>
              <div class="source-text">{{ source.content }}</div>
            </div>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <el-dialog title="问答反馈" :visible.sync="feedbackVisible" width="480px" append-to-body>
      <el-form :model="feedbackForm" label-width="80px">
        <el-form-item label="反馈">
          <el-radio-group v-model="feedbackForm.feedback">
            <el-radio label="HELPFUL">有帮助</el-radio>
            <el-radio label="UNHELPFUL">需改进</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="feedbackForm.feedbackRemark" type="textarea" :rows="4" placeholder="记录缺少的知识点或改进建议" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="feedbackVisible = false">取消</el-button>
        <el-button type="primary" @click="submitFeedback">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getKnowledgeOperationStats, listKnowledgeOperations, feedbackKnowledgeQa } from '@/api/knowledge/operation'

export default {
  name: 'KnowledgeOperation',
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      stats: {},
      queryParams: { pageNum: 1, pageSize: 10, keyword: '', noAnswer: '', feedback: '', confidenceLevel: '' },
      detailVisible: false,
      feedbackVisible: false,
      current: null,
      feedbackForm: { conversationId: null, feedback: 'HELPFUL', feedbackRemark: '' }
    }
  },
  computed: {
    statCards() {
      return [
        { key: 'totalDocuments', label: '文档总数', value: this.stats.totalDocuments || 0 },
        { key: 'embeddedDocuments', label: '已入库', value: this.stats.embeddedDocuments || 0 },
        { key: 'todayQuestions', label: '今日问答', value: this.stats.todayQuestions || 0 },
        { key: 'todayNoAnswer', label: '无答案', value: this.stats.todayNoAnswer || 0 },
        { key: 'todayNegativeFeedback', label: '负反馈', value: this.stats.todayNegativeFeedback || 0 },
        { key: 'averageScore', label: '平均命中', value: this.formatScore(this.stats.averageScore) }
      ]
    }
  },
  created() {
    this.getStats()
    this.getList()
  },
  methods: {
    getStats() {
      getKnowledgeOperationStats().then(res => {
        this.stats = res.data || {}
      })
    },
    getList() {
      this.loading = true
      listKnowledgeOperations(this.queryParams).then(res => {
        this.list = res.rows || []
        this.total = res.total || 0
      }).finally(() => {
        this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.queryParams = { pageNum: 1, pageSize: 10, keyword: '', noAnswer: '', feedback: '', confidenceLevel: '' }
      this.getList()
    },
    showDetail(row) {
      this.current = row
      this.detailVisible = true
    },
    openFeedback(row) {
      this.feedbackForm = {
        conversationId: row.conversationId,
        feedback: row.feedback || 'HELPFUL',
        feedbackRemark: row.feedbackRemark || ''
      }
      this.feedbackVisible = true
    },
    submitFeedback() {
      feedbackKnowledgeQa(this.feedbackForm).then(() => {
        this.$message.success('反馈已保存')
        this.feedbackVisible = false
        this.getStats()
        this.getList()
      })
    },
    parseSources(value) {
      if (!value) return []
      try {
        return JSON.parse(value)
      } catch (e) {
        return []
      }
    },
    formatScore(score) {
      const number = Number(score || 0)
      return `${Math.round(number * 100)}%`
    },
    formatConfidence(value) {
      return { HIGH: '高', MEDIUM: '中', LOW: '低' }[value] || value || '-'
    },
    confidenceType(value) {
      return { HIGH: 'success', MEDIUM: 'warning', LOW: 'danger' }[value] || 'info'
    },
    formatFeedback(value) {
      return { HELPFUL: '有帮助', UNHELPFUL: '需改进' }[value] || '-'
    }
  }
}
</script>

<style scoped>
.knowledge-operation-page {
  background: #f5f7fa;
}

.stat-row {
  margin-bottom: 12px;
}

.stat-box {
  height: 74px;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

.stat-value {
  margin-top: 8px;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.source-item {
  padding: 8px 0;
  border-bottom: 1px solid #ebeef5;
}

.source-item:last-child {
  border-bottom: 0;
}

.source-title {
  margin-bottom: 4px;
  color: #409eff;
  font-size: 13px;
}

.source-text {
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
}
</style>
