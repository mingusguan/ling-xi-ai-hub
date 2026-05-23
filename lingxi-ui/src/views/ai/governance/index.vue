<template>
  <div class="app-container governance-page">
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
        <el-form-item label="场景">
          <el-select v-model="queryParams.sceneType" clearable placeholder="全部" style="width: 150px">
            <el-option label="聊天" value="CHAT" />
            <el-option label="AI工具" value="AI_TOOL" />
            <el-option label="知识问答" value="KNOWLEDGE_QA" />
            <el-option label="Agent" value="AGENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="queryParams.sceneName" clearable placeholder="document_write" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" clearable placeholder="全部" style="width: 130px">
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAILURE" />
          </el-select>
        </el-form-item>
        <el-form-item label="风险">
          <el-select v-model="queryParams.riskLevel" clearable placeholder="全部" style="width: 120px">
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="低" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="敏感">
          <el-select v-model="queryParams.sensitiveHit" clearable placeholder="全部" style="width: 110px">
            <el-option label="是" value="1" />
            <el-option label="否" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="sceneType" label="场景" width="120" />
        <el-table-column prop="sceneName" label="名称" width="150" show-overflow-tooltip />
        <el-table-column prop="requestSummary" label="请求摘要" min-width="220" show-overflow-tooltip />
        <el-table-column prop="responseSummary" label="响应摘要" min-width="220" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === 'SUCCESS' ? 'success' : 'danger'" size="mini">
              {{ scope.row.status === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="riskLevel" label="风险" width="90">
          <template slot-scope="scope">
            <el-tag :type="riskType(scope.row.riskLevel)" size="mini">{{ formatRisk(scope.row.riskLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sensitiveHit" label="敏感" width="80">
          <template slot-scope="scope">{{ scope.row.sensitiveHit === '1' ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column prop="costMillis" label="耗时(ms)" width="100" />
        <el-table-column prop="tokenCount" label="Token" width="90" />
        <el-table-column prop="username" label="用户" width="110" />
        <el-table-column prop="createTime" label="时间" width="170" />
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="queryParams.pageNum"
        :limit.sync="queryParams.pageSize"
        @pagination="getList"
      />
    </el-card>
  </div>
</template>

<script>
import { getGovernanceStats, listGovernanceLogs } from '@/api/ai/governance'

export default {
  name: 'AiGovernance',
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      stats: {},
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        sceneType: '',
        sceneName: '',
        status: '',
        riskLevel: '',
        sensitiveHit: ''
      }
    }
  },
  computed: {
    statCards() {
      return [
        { key: 'todayCalls', label: '今日调用', value: this.stats.todayCalls || 0 },
        { key: 'todayFailures', label: '今日失败', value: this.stats.todayFailures || 0 },
        { key: 'todaySensitiveHits', label: '敏感命中', value: this.stats.todaySensitiveHits || 0 },
        { key: 'todayHighRisks', label: '高风险', value: this.stats.todayHighRisks || 0 },
        { key: 'todayToolCalls', label: '工具调用', value: this.stats.todayToolCalls || 0 },
        { key: 'averageCostMillis', label: '平均耗时', value: `${this.stats.averageCostMillis || 0}ms` }
      ]
    }
  },
  created() {
    this.getStats()
    this.getList()
  },
  methods: {
    getStats() {
      getGovernanceStats().then(res => {
        this.stats = res.data || {}
      })
    },
    getList() {
      this.loading = true
      listGovernanceLogs(this.queryParams).then(res => {
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
      this.queryParams = {
        pageNum: 1,
        pageSize: 10,
        sceneType: '',
        sceneName: '',
        status: '',
        riskLevel: '',
        sensitiveHit: ''
      }
      this.getList()
    },
    formatRisk(value) {
      return { HIGH: '高', MEDIUM: '中', LOW: '低' }[value] || value || '-'
    },
    riskType(value) {
      return { HIGH: 'danger', MEDIUM: 'warning', LOW: 'success' }[value] || 'info'
    }
  }
}
</script>

<style scoped>
.governance-page {
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
</style>
