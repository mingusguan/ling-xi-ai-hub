<template>
  <div class="app-container oa-dashboard">
    <div class="hero-panel">
      <div>
        <h2>OA工作台</h2>
        <p>统一查看公告、审批、任务与会议安排，作为 OA 子系统的统一入口。</p>
      </div>
      <div></div>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col v-for="card in cards" :key="card.key" :xs="24" :sm="12" :md="12" :lg="6">
        <div class="stat-card" :style="'border-left: 4px solid ' + card.color">
          <div class="label">{{ card.label }}</div>
          <div class="value" :style="'color: ' + card.color">{{ summary[card.key] || 0 }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="16">
        <el-card shadow="never">
          <div slot="header"><span>我的待办流程</span></div>
          <el-table :data="todoList" size="small" border>
            <el-table-column prop="taskName" label="任务名称" min-width="140" />
            <el-table-column prop="templateName" label="流程模板" min-width="140" />
            <el-table-column prop="assignee" label="处理人" width="120" />
            <el-table-column prop="businessKey" label="业务标识" min-width="180" />
            <el-table-column label="操作" width="120" align="center">
              <template slot-scope="scope">
                <el-button 
                  v-if="scope.row.approvalStatus !== 'rejected'"
                  type="text" 
                  size="mini" 
                  @click="showTodoDetail(scope.row)">
                  处理
                </el-button>
                <span v-else style="color: #909399; font-size: 12px;">已驳回</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="timeout-card">
          <div slot="header" class="timeout-header">
            <span>超时预警</span>
            <el-badge :value="timeoutWarningList.length" :hidden="timeoutWarningList.length === 0" class="item" />
          </div>
          <div class="timeout-list">
            <div
              v-for="item in timeoutWarningList"
              :key="item.warningId"
              class="timeout-item"
              :class="'level-' + item.warningLevel"
              @click="handleTimeoutWarningClick(item)"
              style="cursor: pointer;"
            >
              <div class="timeout-title">
                {{ item.taskName }}
                <el-tag size="mini" :type="item.businessType === 'leave' ? 'success' : 'warning'" style="margin-left: 8px;">
                  {{ item.businessType === 'leave' ? '请假' : '报销' }}
                </el-tag>
              </div>
              <div class="timeout-desc">
                <span>申请人: {{ item.applicant }}</span>
                <span>已耗时: {{ item.durationHours }}小时</span>
              </div>
              <div class="timeout-level">
                <el-tag :type="item.warningLevel === 3 ? 'danger' : item.warningLevel === 2 ? 'warning' : 'info'" size="mini">
                  {{ item.warningLevel === 3 ? '已超时' : item.warningLevel === 2 ? '二级预警' : '一级预警' }}
                </el-tag>
              </div>
            </div>
            <div v-if="timeoutWarningList.length === 0" class="timeout-empty">
              <i class="el-icon-circle-check"></i>
              <span>暂无超时预警</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-drawer title="待办处理" :visible.sync="todoOpen" size="40%">
      <div v-if="currentTodo" class="todo-drawer">
        <p><strong>任务名称：</strong>{{ currentTodo.taskName }}</p>
        <p><strong>流程模板：</strong>{{ currentTodo.templateName || '-' }}</p>
        <p><strong>业务标识：</strong>{{ currentTodo.businessKey || '-' }}</p>
        <p><strong>节点标识：</strong>{{ currentTodo.taskDefinitionKey || '-' }}</p>
        <el-divider>审批规则</el-divider>
        <div class="todo-rule-grid">
          <div class="todo-rule-item">
            <span class="todo-rule-label">审批类型</span>
            <span class="todo-rule-value">{{ approverTypeText }}</span>
          </div>
          <div class="todo-rule-item">
            <span class="todo-rule-label">审批配置</span>
            <span class="todo-rule-value">{{ approverValueText }}</span>
          </div>
          <div class="todo-rule-item">
            <span class="todo-rule-label">Flowable Assignee</span>
            <span class="todo-rule-value">{{ currentTodo.nodeConfig && currentTodo.nodeConfig.assigneeExpr ? currentTodo.nodeConfig.assigneeExpr : '-' }}</span>
          </div>
          <div class="todo-rule-item">
            <span class="todo-rule-label">候选组</span>
            <span class="todo-rule-value">{{ currentTodo.nodeConfig && currentTodo.nodeConfig.candidateGroups ? currentTodo.nodeConfig.candidateGroups : '-' }}</span>
          </div>
          <div class="todo-rule-item">
            <span class="todo-rule-label">表单 Key</span>
            <span class="todo-rule-value">{{ currentTodo.nodeConfig && currentTodo.nodeConfig.formKey ? currentTodo.nodeConfig.formKey : '-' }}</span>
          </div>
        </div>
        <el-divider>流程数据</el-divider>
        <div v-if="currentFieldRows.length">
          <div v-for="field in currentFieldRows" :key="field.code" class="todo-field-row">
            <div class="todo-field-meta">
              <span class="todo-field-label">{{ field.label || field.code }}</span>
              <span class="todo-field-code">{{ field.code }}</span>
            </div>
            <div class="todo-field-value-wrap">
              <span class="todo-field-type">{{ fieldTypeText(field.type) }}</span>
              <span class="todo-field-badge">{{ field.approveReadonly === false ? '审批可编辑' : '审批只读' }}</span>
              <el-input-number
                v-if="field.approveReadonly === false && field.type === 'number'"
                v-model="editableValues[field.code]"
                :min="0"
                :precision="2"
                size="mini"
                class="todo-field-input"
              />
              <el-date-picker
                v-else-if="field.approveReadonly === false && field.type === 'date'"
                v-model="editableValues[field.code]"
                type="date"
                value-format="yyyy-MM-dd"
                size="mini"
                class="todo-field-input"
              />
              <el-select
                v-else-if="field.approveReadonly === false && field.type === 'select'"
                v-model="editableValues[field.code]"
                size="mini"
                class="todo-field-input"
                clearable
              >
                <el-option
                  v-for="option in resolveFieldOptions(field)"
                  :key="`${field.code}-${option.value}`"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
              <el-input
                v-else-if="field.approveReadonly === false && field.type === 'textarea'"
                v-model="editableValues[field.code]"
                type="textarea"
                :rows="2"
                size="mini"
                class="todo-field-input"
              />
              <el-input
                v-else-if="field.approveReadonly === false"
                v-model="editableValues[field.code]"
                size="mini"
                class="todo-field-input"
              />
              <span v-else class="todo-field-value">{{ formatFieldValue(field) }}</span>
            </div>
          </div>
        </div>
        <el-empty v-else :image-size="80" description="暂无字段数据" />
        <el-divider>审批意见</el-divider>
        <el-input v-model="approveComment" type="textarea" :rows="4" placeholder="请输入审批意见" />
        <div class="todo-actions">
          <el-button type="primary" @click="submitTodo('approve')">通过</el-button>
          <el-button type="danger" plain @click="submitTodo('reject')">驳回</el-button>
        </div>
      </div>
    </el-drawer>

    <!-- 审批弹窗 -->
    <ApprovalDialogWithAi
      :visible.sync="approvalVisible"
      :form="approvalForm"
      :business-type="approvalBusinessType"
      :business-id="approvalBusinessId"
      :task-id="approvalTaskId"
      @submit="handleApprovalSubmit"
    />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { getDashboardSummary, getUnreadReminderCount } from '@/api/oa/dashboard'
import { listTodoTasks, approveLeave, approveExpense, getTimeoutWarningList, listLeave, listExpense, getLeaveDetail, getExpenseDetail } from '@/api/oa/workflow'
import ApprovalDialogWithAi from '@/components/ApprovalDialogWithAi/index.vue'

export default {
  name: 'OaDashboard',
  components: { ApprovalDialogWithAi },
  data() {
    return {
      summary: {},
      todoList: [],
      todoOpen: false,
      currentTodo: null,
      approveComment: '',
      editableValues: {},
      cards: [
        { key: 'unreadCount', label: '未读消息', color: '#f56c6c' },
        { key: 'todoWorkflowCount', label: '待办审批', color: '#409eff' },
        { key: 'timeoutWarningCount', label: '超时预警', color: '#e6a23c' },
        { key: 'approvedTodayCount', label: '今日已审批', color: '#67c23a' }
      ],
      unreadCount: 0,
      timeoutWarningList: [],
      // 审批弹窗相关
      approvalVisible: false,
      approvalForm: {},
      approvalBusinessType: '',
      approvalBusinessId: null,
      approvalTaskId: ''
    }
  },
  computed: {
    ...mapGetters(['name', 'id']),
    currentFieldRows() {
      return this.resolveApproveFields(this.currentTodo)
    },
    approverTypeText() {
      const type = this.currentTodo && this.currentTodo.nodeConfig ? this.currentTodo.nodeConfig.approverType : ''
      const mapping = {
        user: '指定用户',
        role: '指定角色',
      }
      return mapping[type] || '-'
    },
    approverValueText() {
      if (!this.currentTodo || !this.currentTodo.nodeConfig) {
        return '-'
      }
      return this.currentTodo.nodeConfig.approverValue || '-'
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    loadData() {
      getDashboardSummary().then(res => {
        this.summary = res.data || {}
      }).catch(() => {
        this.summary = {}
      })
      listTodoTasks({ assignee: this.id }).then(res => {
        this.todoList = res.data || []
        this.summary.todoWorkflowCount = this.todoList.length
      }).catch(() => {
        this.todoList = []
      })
      getUnreadReminderCount().then(res => {
        this.unreadCount = res.data.count || 0
        this.summary.unreadCount = this.unreadCount
      })
      // 获取超时预警列表（从工作流待办任务中查询超过24小时的）
      getTimeoutWarningList().then(res => {
        this.timeoutWarningList = res.data || []
        this.summary.timeoutWarningCount = this.timeoutWarningList.length
      })
    },
    showTodoDetail(row) {
      this.todoOpen = false
      const businessType = row.businessKey && row.businessKey.startsWith('leave:') ? 'leave' : 'expense'
      const businessId = row.businessKey ? row.businessKey.split(':')[1] : null
      this.fetchApprovalFormData(businessType, businessId)
    },
    buildEditableValues(row) {
      const values = {}
      const fields = this.resolveApproveFields(row)
      const variables = row && row.variables ? row.variables : {}
      fields.forEach(field => {
        if (field.approveReadonly === false) {
          values[field.code] = variables[field.code] === undefined || variables[field.code] === null
            ? this.defaultFieldValue(field)
            : variables[field.code]
        }
      })
      return values
    },
    defaultFieldValue(field) {
      if (field.type === 'number') {
        return 0
      }
      return ''
    },
    resolveApproveFields(row) {
      if (!row || !row.nodeConfig) {
        return []
      }
      const processFields = Array.isArray(row.nodeConfig.processFormFields) ? row.nodeConfig.processFormFields : []
      const taskFields = Array.isArray(row.nodeConfig.formFields) ? row.nodeConfig.formFields : []
      const rows = taskFields.length ? taskFields : processFields
      return rows.filter(field => !field.displayStage || field.displayStage === 'both' || field.displayStage === 'approve')
    },
    formatFieldValue(field) {
      if (!this.currentTodo || !this.currentTodo.variables) {
        return '-'
      }
      const value = this.currentTodo.variables[field.code]
      if (value === undefined || value === null || value === '') {
        return '-'
      }
      if (field.type === 'select') {
        const option = this.resolveFieldOptions(field).find(item => item.value === value)
        return option ? option.label : value
      }
      if (field.type === 'number') {
        return Number.isNaN(Number(value)) ? value : Number(value).toLocaleString('zh-CN')
      }
      if (field.type === 'date') {
        const date = new Date(value)
        return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN')
      }
      if (Array.isArray(value)) {
        return value.join('，')
      }
      if (typeof value === 'object') {
        try {
          return JSON.stringify(value)
        } catch (e) {
          return String(value)
        }
      }
      return String(value)
    },
    resolveFieldOptions(field) {
      if (!field || !Array.isArray(field.options)) {
        return []
      }
      return field.options
        .map(item => ({
          value: item && item.value !== undefined && item.value !== null ? String(item.value) : '',
          label: item && item.label ? item.label : (item && item.value !== undefined && item.value !== null ? String(item.value) : '')
        }))
        .filter(item => item.value || item.label)
    },
    fieldTypeText(type) {
      const mapping = {
        text: '文本',
        textarea: '长文本',
        number: '数字',
        date: '日期',
        select: '选择'
      }
      return mapping[type] || '字段'
    },
    submitTodo(action) {
      if (!this.currentTodo || !this.currentTodo.businessKey) {
        return
      }
      const payload = {
        taskId: this.currentTodo.taskId,
        businessKey: this.currentTodo.businessKey,
        processInstanceId: this.currentTodo.processInstanceId,
        comment: this.approveComment || (action === 'approve' ? '审批通过' : '审批驳回'),
        action,
        variables: { ...this.editableValues }
      }
      const api = this.currentTodo.businessKey.startsWith('leave:') ? approveLeave : approveExpense
      api(payload).then(() => {
        this.$message.success(action === 'approve' ? '审批通过' : '审批驳回')
        this.todoOpen = false
        this.loadData()
      })
    },
    // 点击超时预警记录
    handleTimeoutWarningClick(item) {
      // 根据业务类型跳转到对应页面并打开审批弹窗
      const businessType = item.businessType
      const businessId = item.businessId
      
      if (!businessType || !businessId) {
        this.$message.warning('无法识别业务类型')
        return
      }
      
      // 设置审批表单数据
      this.approvalBusinessType = businessType
      this.approvalBusinessId = parseInt(businessId)
      
      // 构造审批表单数据（需要从后端获取完整信息）
      this.fetchApprovalFormData(businessType, businessId)
    },
    // 获取审批表单数据
    fetchApprovalFormData(businessType, businessId) {
      // 先从后端获取完整的业务数据（包含申请人和部门信息）
      const detailApi = businessType === 'leave' ? getLeaveDetail : getExpenseDetail
      detailApi(businessId).then(detailRes => {
        const detail = detailRes.data
        if (!detail) {
          this.$message.warning('未找到对应的业务数据')
          return
        }
        
        // 通过待办任务列表获取任务ID等信息
        listTodoTasks({ assignee: this.id }).then(res => {
          const tasks = res.data || []
          // 找到对应的任务
          const task = tasks.find(t => {
            if (t.businessKey) {
              const parts = t.businessKey.split(':')
              return parts.length === 2 && parts[0] === businessType && parts[1] === String(businessId)
            }
            return false
          })
          
          if (task) {
            // 以业务详情数据为基础，补充任务相关信息
            this.approvalForm = {
              ...detail,
              taskId: task.taskId,
              processInstanceId: task.processInstanceId || detail.processInstanceId,
              businessKey: task.businessKey || `${businessType}:${businessId}`
            }
            this.approvalBusinessType = businessType
            this.approvalBusinessId = parseInt(businessId)
            this.approvalTaskId = task.taskId || ''
            
            // 如果是请假，补充leaveId；如果是报销，补充expenseId
            if (businessType === 'leave') {
              this.approvalForm.leaveId = parseInt(businessId)
            } else if (businessType === 'expense') {
              this.approvalForm.expenseId = parseInt(businessId)
            }
            
            this.approvalVisible = true
          } else {
            this.$message.warning('未找到对应的待办任务，可能已被处理')
          }
        }).catch(() => {
          this.$message.error('获取任务数据失败')
        })
      }).catch(() => {
        this.$message.error('获取业务数据失败')
      })
    },
    // 审批提交
    handleApprovalSubmit({ result, comment }) {
      const api = this.approvalBusinessType === 'leave' ? approveLeave : approveExpense
      const payload = {
        taskId: this.approvalTaskId,
        businessKey: `${this.approvalBusinessType}:${this.approvalBusinessId}`,
        processInstanceId: this.approvalForm.processInstanceId,
        comment: comment || (result === 'approve' ? '审批通过' : '审批驳回'),
        action: result
      }
      api(payload).then(() => {
        this.$message.success('审批成功')
        this.approvalVisible = false
        this.loadData()
      })
    }
  }
}
</script>

<style scoped>
.hero-panel { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-radius: 16px; background: linear-gradient(120deg, #0f766e, #2563eb); color: #fff; margin-bottom: 16px; }
.hero-panel h2 { margin: 0 0 8px; }
.hero-panel p { margin: 0; opacity: 0.9; }
.stat-row { margin-bottom: 16px; }
.stat-card { background: #fff; border-radius: 12px; padding: 18px; margin-bottom: 16px; box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06); }
.stat-card .label { color: #6b7280; margin-bottom: 10px; }
.stat-card .value { font-size: 28px; font-weight: 700; color: #111827; }

.timeout-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.timeout-list {
  min-height: 200px;
}

.timeout-item {
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 10px;
  background: #f5f7fa;
}

.timeout-item.level-3 {
  background: #fef0f0;
  border-left: 3px solid #f56c6c;
}

.timeout-item.level-2 {
  background: #fdf6ec;
  border-left: 3px solid #e6a23c;
}

.timeout-item.level-1 {
  background: #ecf5ff;
  border-left: 3px solid #409eff;
}

.timeout-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.timeout-desc {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #606266;
  margin-bottom: 8px;
}

.timeout-level {
  text-align: right;
}

.timeout-empty {
  text-align: center;
  padding: 40px 20px;
  color: #909399;
}

.timeout-empty i {
  font-size: 36px;
  color: #67c23a;
  display: block;
  margin-bottom: 8px;
}

.quick-links { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.quick-link { padding: 14px 16px; background: #f3f7fb; border-radius: 10px; cursor: pointer; color: #1f2937; }
.quick-link:hover { background: #e0ecff; }
.todo-drawer { padding: 0 20px 20px; line-height: 1.8; }
.todo-rule-grid { display: grid; grid-template-columns: 1fr; gap: 10px; margin-bottom: 6px; }
.todo-rule-item { display: flex; justify-content: space-between; gap: 16px; padding: 10px 12px; background: #f8fafc; border-radius: 10px; }
.todo-rule-label { color: #6b7280; }
.todo-rule-value { color: #111827; text-align: right; word-break: break-word; }
.todo-field-row { display: flex; justify-content: space-between; gap: 16px; padding: 8px 0; border-bottom: 1px dashed #ebeef5; }
.todo-field-meta { display: flex; flex-direction: column; min-width: 0; }
.todo-field-label { color: #374151; font-weight: 600; }
.todo-field-code { color: #9ca3af; font-size: 12px; }
.todo-field-value-wrap { display: flex; flex-direction: column; align-items: flex-end; min-width: 120px; }
.todo-field-type { color: #0f766e; font-size: 12px; }
.todo-field-badge { color: #2563eb; font-size: 12px; }
.todo-field-input { width: 180px; margin-top: 4px; }
.todo-field-value { color: #111827; text-align: right; word-break: break-word; }
.todo-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 16px; }
</style>
