<template>
  <div class="app-container agent-page">
    <el-card shadow="never">
      <el-form :model="queryParams" inline size="small">
        <el-form-item label="编码">
          <el-input v-model="queryParams.agentCode" clearable placeholder="hr_policy_agent" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="queryParams.agentName" clearable placeholder="HR制度助手" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" clearable placeholder="全部" style="width: 140px">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
          <el-button type="primary" plain icon="el-icon-plus" @click="openAgent()">新建智能体</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="agentList" border>
        <el-table-column prop="agentCode" label="编码" width="150" />
        <el-table-column prop="agentName" label="名称" width="180" />
        <el-table-column prop="businessScene" label="业务场景" width="130" />
        <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
        <el-table-column prop="ownerTeam" label="团队" width="130" />
        <el-table-column prop="auditEnabled" label="审计" width="80">
          <template slot-scope="scope">
            <el-tag :type="scope.row.auditEnabled === '1' ? 'success' : 'info'" size="mini">
              {{ scope.row.auditEnabled === '1' ? '开启' : '关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="statusType(scope.row.status)" size="mini">{{ formatStatus(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click="openAgent(scope.row)">编辑</el-button>
            <el-button type="text" size="mini" @click="openDetail(scope.row)">编排</el-button>
            <el-button v-if="scope.row.status !== 'PUBLISHED'" type="text" size="mini" @click="publish(scope.row)">发布</el-button>
            <el-button v-if="scope.row.status !== 'DISABLED'" type="text" size="mini" @click="disable(scope.row)">停用</el-button>
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

    <el-dialog :title="agentForm.agentId ? '编辑智能体' : '新建智能体'" :visible.sync="agentDialogVisible" width="760px" append-to-body>
      <el-form ref="agentForm" :model="agentForm" :rules="agentRules" label-width="100px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="编码" prop="agentCode">
              <el-input v-model="agentForm.agentCode" placeholder="hr_policy_agent" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="名称" prop="agentName">
              <el-input v-model="agentForm.agentName" placeholder="HR制度助手" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="业务场景">
              <el-input v-model="agentForm.businessScene" placeholder="HR / FINANCE / IT" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属团队">
              <el-input v-model="agentForm.ownerTeam" placeholder="人力行政部" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述">
          <el-input v-model="agentForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="系统提示词">
          <el-input v-model="agentForm.systemPrompt" type="textarea" :rows="5" />
        </el-form-item>
        <el-form-item label="安全边界">
          <el-input v-model="agentForm.guardrails" type="textarea" :rows="3" placeholder="不得越权查询，不确定时提示人工确认" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="agentForm.status" style="width: 100%">
                <el-option label="草稿" value="DRAFT" />
                <el-option label="已发布" value="PUBLISHED" />
                <el-option label="已停用" value="DISABLED" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="治理审计">
              <el-switch v-model="agentForm.auditEnabled" active-value="1" inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer">
        <el-button @click="agentDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAgent">保存</el-button>
      </div>
    </el-dialog>

    <el-drawer title="智能体编排" :visible.sync="detailVisible" size="58%">
      <div v-if="currentAgent" class="drawer-body">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="编码">{{ currentAgent.agentCode }}</el-descriptions-item>
          <el-descriptions-item label="名称">{{ currentAgent.agentName }}</el-descriptions-item>
          <el-descriptions-item label="场景">{{ currentAgent.businessScene }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ formatStatus(currentAgent.status) }}</el-descriptions-item>
        </el-descriptions>

        <div class="toolbar">
          <el-button type="primary" plain size="mini" icon="el-icon-plus" @click="openStep()">新增步骤</el-button>
        </div>
        <el-table :data="steps" border>
          <el-table-column prop="stepOrder" label="序号" width="70" />
          <el-table-column prop="stepName" label="步骤" width="150" />
          <el-table-column prop="stepType" label="类型" width="110" />
          <el-table-column prop="toolName" label="工具" width="160" />
          <el-table-column prop="instruction" label="说明" min-width="220" show-overflow-tooltip />
          <el-table-column prop="failurePolicy" label="失败策略" width="120" />
          <el-table-column label="操作" width="120">
            <template slot-scope="scope">
              <el-button type="text" size="mini" @click="openStep(scope.row)">编辑</el-button>
              <el-button type="text" size="mini" @click="removeStep(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-divider>MCP工具绑定</el-divider>
        <el-table :data="bindings" border>
          <el-table-column prop="toolName" label="工具名称" width="180" />
          <el-table-column prop="enabled" label="启用" width="80">
            <template slot-scope="scope">{{ scope.row.enabled === '1' ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column prop="configJson" label="配置" min-width="240" show-overflow-tooltip />
        </el-table>
      </div>
    </el-drawer>

    <el-dialog :title="stepForm.stepId ? '编辑步骤' : '新增步骤'" :visible.sync="stepDialogVisible" width="620px" append-to-body>
      <el-form ref="stepForm" :model="stepForm" :rules="stepRules" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="序号" prop="stepOrder">
              <el-input-number v-model="stepForm.stepOrder" :min="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="步骤名" prop="stepName">
              <el-input v-model="stepForm.stepName" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="类型" prop="stepType">
          <el-select v-model="stepForm.stepType" style="width: 100%">
            <el-option label="提示词" value="PROMPT" />
            <el-option label="工具调用" value="TOOL" />
            <el-option label="人工审批" value="APPROVAL" />
            <el-option label="输出整理" value="OUTPUT" />
          </el-select>
        </el-form-item>
        <el-form-item label="工具">
          <el-input v-model="stepForm.toolName" placeholder="search_knowledge" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="stepForm.instruction" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="配置JSON">
          <el-input v-model="stepForm.configJson" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="失败策略">
          <el-select v-model="stepForm.failurePolicy" style="width: 100%">
            <el-option label="停止" value="STOP" />
            <el-option label="继续" value="CONTINUE" />
            <el-option label="人工复核" value="MANUAL_REVIEW" />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="stepDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitStep">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listAgents, getAgent, addAgent, updateAgent, publishAgent, disableAgent, saveAgentStep, deleteAgentStep } from '@/api/ai/agent'

const emptyAgent = {
  agentId: null,
  agentCode: '',
  agentName: '',
  businessScene: '',
  description: '',
  systemPrompt: '',
  guardrails: '',
  status: 'DRAFT',
  ownerTeam: '',
  auditEnabled: '1'
}

export default {
  name: 'AiAgent',
  data() {
    return {
      loading: false,
      agentList: [],
      total: 0,
      queryParams: { pageNum: 1, pageSize: 10, agentCode: '', agentName: '', status: '' },
      agentDialogVisible: false,
      detailVisible: false,
      stepDialogVisible: false,
      agentForm: { ...emptyAgent },
      currentAgent: null,
      steps: [],
      bindings: [],
      stepForm: {},
      agentRules: {
        agentCode: [{ required: true, message: '请输入智能体编码', trigger: 'blur' }],
        agentName: [{ required: true, message: '请输入智能体名称', trigger: 'blur' }]
      },
      stepRules: {
        stepOrder: [{ required: true, message: '请输入序号', trigger: 'blur' }],
        stepName: [{ required: true, message: '请输入步骤名', trigger: 'blur' }],
        stepType: [{ required: true, message: '请选择类型', trigger: 'change' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listAgents(this.queryParams).then(res => {
        this.agentList = res.rows || []
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
      this.queryParams = { pageNum: 1, pageSize: 10, agentCode: '', agentName: '', status: '' }
      this.getList()
    },
    openAgent(row) {
      this.agentForm = row ? { ...row } : { ...emptyAgent }
      this.agentDialogVisible = true
    },
    submitAgent() {
      this.$refs.agentForm.validate(valid => {
        if (!valid) return
        const action = this.agentForm.agentId ? updateAgent : addAgent
        action(this.agentForm).then(() => {
          this.$message.success('保存成功')
          this.agentDialogVisible = false
          this.getList()
        })
      })
    },
    publish(row) {
      publishAgent(row.agentId).then(() => {
        this.$message.success('发布成功')
        this.getList()
      })
    },
    disable(row) {
      disableAgent(row.agentId).then(() => {
        this.$message.success('已停用')
        this.getList()
      })
    },
    openDetail(row) {
      getAgent(row.agentId).then(res => {
        const data = res.data || {}
        this.currentAgent = data.agent
        this.steps = data.steps || []
        this.bindings = data.bindings || []
        this.detailVisible = true
      })
    },
    openStep(row) {
      this.stepForm = row ? { ...row } : {
        stepId: null,
        agentCode: this.currentAgent.agentCode,
        stepOrder: this.steps.length + 1,
        stepName: '',
        stepType: 'PROMPT',
        toolName: '',
        instruction: '',
        configJson: '',
        failurePolicy: 'STOP',
        enabled: '1'
      }
      this.stepDialogVisible = true
    },
    submitStep() {
      this.$refs.stepForm.validate(valid => {
        if (!valid) return
        saveAgentStep(this.stepForm).then(() => {
          this.$message.success('步骤已保存')
          this.stepDialogVisible = false
          this.openDetail(this.currentAgent)
        })
      })
    },
    removeStep(row) {
      this.$confirm('确定删除该编排步骤？', '提示', { type: 'warning' }).then(() => {
        deleteAgentStep(row.stepId).then(() => {
          this.$message.success('删除成功')
          this.openDetail(this.currentAgent)
        })
      }).catch(() => {})
    },
    formatStatus(status) {
      return { DRAFT: '草稿', PUBLISHED: '已发布', DISABLED: '已停用' }[status] || status
    },
    statusType(status) {
      return { DRAFT: 'info', PUBLISHED: 'success', DISABLED: 'danger' }[status] || 'info'
    }
  }
}
</script>

<style scoped>
.agent-page {
  background: #f5f7fa;
}

.drawer-body {
  padding: 0 20px 24px;
}

.toolbar {
  margin: 16px 0 10px;
}
</style>
