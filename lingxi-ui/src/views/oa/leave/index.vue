<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <el-tab-pane label="待处理" name="myApplications">
        <el-alert
          title="显示我发起的未结束申请 + 需要我审批的申请"
          type="info"
          :closable="false"
          style="margin-bottom: 10px;"
        />
        <el-form :inline="true" class="mb8">
          <el-form-item label="审批状态">
            <el-select v-model="query.approvalStatus" clearable placeholder="请选择" size="mini">
              <el-option label="待审批" value="pending" />
              <el-option label="已通过" value="approved" />
              <el-option label="已驳回" value="rejected" />
              <el-option label="已取消" value="cancelled" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="mini" icon="el-icon-search" @click="getList">搜索</el-button>
            <el-button size="mini" icon="el-icon-refresh" @click="resetQuery">重置</el-button>
            <el-button type="primary" plain size="mini" icon="el-icon-plus" @click="handleAdd">发起请假</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="leaveNo" label="单号" min-width="140" />
          <el-table-column prop="leaveType" label="请假类型" width="120">
            <template slot-scope="scope">
              <span>{{ formatLeaveType(scope.row.leaveType) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="applicantUserName" label="申请人" width="120" />
          <el-table-column prop="deptName" label="部门" min-width="140" />
          <el-table-column prop="createTime" label="申请时间" width="160">
            <template slot-scope="scope">
              <span>{{ scope.row.createTime ? parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') : '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="leaveHours" label="时长" width="100">
            <template slot-scope="scope">
              <span>{{ scope.row.leaveHours ? (scope.row.leaveHours / 8).toFixed(1) : 0 }} 天</span>
            </template>
          </el-table-column>
          <el-table-column prop="approvalStatus" label="审批状态" width="120">
            <template slot-scope="scope">
              <span>{{ formatApprovalStatus(scope.row.approvalStatus) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="currentNodeName" label="当前节点" min-width="140">
            <template slot-scope="scope">
              <span v-if="scope.row.currentNodeName">{{ scope.row.currentNodeName }}</span>
              <span v-else style="color: #909399;">-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240" align="center">
            <template slot-scope="scope">
              <el-button type="text" size="mini" @click="showDetail(scope.row)">详情</el-button>
              <el-button
                v-if="scope.row.canApprove && scope.row.approvalStatus === 'pending'"
                type="text"
                size="mini"
                style="color: #409EFF"
                @click="openApprovalDialog(scope.row)">
                审批
              </el-button>
              <el-button
                v-if="scope.row.approvalStatus === 'rejected' && scope.row.applicantUserId == id"
                type="text"
                size="mini"
                style="color: #E6A23C"
                @click="handleEdit(scope.row)">
                重新编辑
              </el-button>
              <el-button
                v-if="scope.row.approvalStatus === 'rejected' && scope.row.applicantUserId == id"
                type="text"
                size="mini"
                style="color: #F56C6C"
                @click="handleCancel(scope.row)">
                取消申请
              </el-button>
              <span
                v-if="!scope.row.canApprove && scope.row.approvalStatus !== 'rejected'"
                style="color: #909399; font-size: 12px;">
                仅查看
              </span>
            </template>
          </el-table-column>
        </el-table>

        <pagination v-show="total > 0" :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="getList" />
      </el-tab-pane>

      <el-tab-pane label="已处理" name="myApproved">
        <el-alert
          title="显示我发起的已结束申请 + 我审批过的申请"
          type="info"
          :closable="false"
          style="margin-bottom: 10px;"
        />
        <el-table v-loading="approvedLoading" :data="approvedList" border>
          <el-table-column prop="leaveNo" label="单号" min-width="140" />
          <el-table-column prop="leaveType" label="请假类型" width="120">
            <template slot-scope="scope">
              <span>{{ formatLeaveType(scope.row.leaveType) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="applicantUserName" label="申请人" width="120" />
          <el-table-column prop="deptName" label="部门" min-width="140" />
          <el-table-column prop="createTime" label="申请时间" width="160">
            <template slot-scope="scope">
              <span>{{ scope.row.createTime ? parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') : '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="leaveHours" label="时长" width="100">
            <template slot-scope="scope">
              <span>{{ scope.row.leaveHours ? (scope.row.leaveHours / 8).toFixed(1) : 0 }} 天</span>
            </template>
          </el-table-column>
          <el-table-column prop="approvalStatus" label="审批状态" width="120">
            <template slot-scope="scope">
              <span>{{ formatApprovalStatus(scope.row.approvalStatus) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="currentNodeName" label="当前节点" min-width="140">
            <template slot-scope="scope">
              <span v-if="scope.row.currentNodeName">{{ scope.row.currentNodeName }}</span>
              <span v-else style="color: #909399;">-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center">
            <template slot-scope="scope">
              <el-button type="text" size="mini" @click="showDetail(scope.row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination v-show="approvedTotal > 0" :total="approvedTotal" :page.sync="approvedQuery.pageNum" :limit.sync="approvedQuery.pageSize" @pagination="getApprovedList" />
      </el-tab-pane>
    </el-tabs>

    <el-dialog title="发起请假" :visible.sync="open" width="620px" append-to-body>
      <!-- 待审批统计提示 -->
      <el-alert
        v-if="pendingStats && pendingStats.totalCount > 0"
        title="📋 您有待审批的请假申请"
        type="warning"
        :closable="false"
        style="margin-bottom: 10px;"
      >
        <div slot="default" style="margin-top: 8px; font-size: 13px;">
          <p style="margin: 4px 0;">
            共 <strong>{{ pendingStats.totalCount }}</strong> 个申请，总计 <strong>{{ pendingStats.totalDays }}</strong> 天
          </p>
          <div v-if="pendingStats.details && pendingStats.details.length > 0" style="margin-top: 5px;">
            <span v-for="(item, index) in pendingStats.details" :key="index" style="margin-right: 15px;">
              {{ getLeaveTypeName(item.leaveType, item.leaveName) }}: {{ item.days }}天({{ item.count }}个)
            </span>
          </div>
        </div>
      </el-alert>

      <!-- 余额提示区域 -->
      <el-alert
        v-if="currentBalance"
        :title="getBalanceAlertTitle()"
        :type="getAlertType()"
        :closable="false"
        style="margin-bottom: 15px;"
      >
        <div slot="default" style="margin-top: 8px; font-size: 13px;">
          <!-- 未选类型时：显示所有假期余额 -->
          <div v-if="!form.leaveType">
            <p style="margin: 4px 0; color: #606266; font-weight: 500;">我的假期余额：</p>
            <div v-for="q in currentBalance.balances" :key="q.leaveType" style="margin: 6px 0; display: inline-block; width: 48%;">
              <span style="color: #909399;">{{ getLeaveTypeName(q.leaveType, q.leaveName) }}：</span>
              <template v-if="q.initialized">
                <strong style="color: #409EFF;">{{ q.remainingDays }}</strong> 天
              </template>
              <span v-else style="color: #e6a23c;">
                <i class="el-icon-warning"></i> 假期未初始化，请联系管理员
              </span>
            </div>
          </div>
          <!-- 已选类型时：显示当前申请的详细对比 -->
          <div v-if="form.leaveType">
            <template v-if="getCurrentQuota()">
              <p v-if="getCurrentQuota().initialized" style="margin: 4px 0;">
                {{ getLeaveTypeName(form.leaveType, getCurrentQuota().leaveName) }}余额：
                <strong>{{ getCurrentQuota().remainingDays }}</strong> 天
              </p>
              <p v-else style="margin: 4px 0; color: #e6a23c;">
                <i class="el-icon-warning"></i>
                {{ getLeaveTypeName(form.leaveType, getCurrentQuota().leaveName) }}假期未初始化，请联系管理员
              </p>
              <p v-if="this.calculatedWorkDays > 0 && getCurrentQuota().initialized" style="margin: 4px 0;">
                申请天数：<strong>{{ this.calculatedWorkDays }}</strong> 天
              </p>
              <p v-if="isOverQuota" style="margin: 6px 0; padding: 8px 12px; background: #fef0f0; border-radius: 4px; border: 1px solid #fde2e2;">
                <span style="color: #F56C6C; font-weight: bold;">
                  ⚠️ 余额不足，超额 <strong>{{ (this.calculatedWorkDays - getCurrentQuota().remainingDays).toFixed(2) }}</strong> 天！
                </span>
              </p>
            </template>
            <p v-else style="margin: 4px 0; color: #e6a23c;">
              <i class="el-icon-warning"></i> 该假期类型未初始化，请联系管理员
            </p>
          </div>
        </div>
      </el-alert>

      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item v-for="field in activeFields" :key="field.code" :label="field.label" :prop="field.code">
          <el-select
            v-if="field.code === 'leaveType'"
            v-model="form.leaveType"
            style="width: 100%"
          >
            <el-option
              v-for="option in leaveTypeOptions"
              :key="option.ruleId"
              :label="option.leaveName"
              :value="option.leaveType"
            />
          </el-select>
          <el-select
            v-else-if="field.type === 'select'"
            v-model="form.params.dynamicForm[field.code]"
            style="width: 100%"
            clearable
          >
            <el-option
              v-for="option in resolveFieldOptions(field)"
              :key="`${field.code}-${option.value}`"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-date-picker
            v-else-if="field.code === 'startTime' || field.code === 'endTime'"
            v-model="form[field.code]"
            type="datetime"
            style="width: 100%"
            placeholder="请选择"
            @change="handleTimeChange"
          />
          <el-input-number
            v-else-if="field.type === 'number'"
            v-model="form.params.dynamicForm[field.code]"
            :min="0"
            :precision="2"
            style="width: 100%"
          />
          <el-date-picker
            v-else-if="field.type === 'date'"
            v-model="form.params.dynamicForm[field.code]"
            type="date"
            value-format="yyyy-MM-dd"
            style="width: 100%"
            placeholder="请选择"
          />
          <el-input
            v-else-if="field.code === 'leaveReason'"
            v-model="form.leaveReason"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
          />
          <el-input
            v-else-if="field.type === 'textarea'"
            v-model="form.params.dynamicForm[field.code]"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
          />
          <el-input
            v-else
            v-model="form.params.dynamicForm[field.code]"
          />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button type="primary" @click="submitForm">确定</el-button>
        <el-button @click="open = false">取消</el-button>
      </div>
    </el-dialog>

    <el-drawer title="请假详情" :visible.sync="detailOpen" size="40%">
      <div class="drawer-body" v-if="detail">
        <p><strong>单号：</strong>{{ detail.leaveNo }}</p>
        <p><strong>申请人：</strong>{{ detail.applicantUserName }}</p>
        <p><strong>部门：</strong>{{ detail.deptName }}</p>
        <p><strong>类型：</strong>{{ formatLeaveType(detail.leaveType) }}</p>
        <p><strong>开始：</strong>{{ formatTime(detail.startTime) }}</p>
        <p><strong>结束：</strong>{{ formatTime(detail.endTime) }}</p>
        <p><strong>状态：</strong>{{ formatStatus(detail.approvalStatus) }}</p>
        <p v-if="detail.isOverQuota === 1" style="color: #F56C6C;">
          <strong>⚠️ 超额标记：</strong>是，超额 {{ detail.overQuotaDays }} 天
        </p>
        <p><strong>事由：</strong>{{ detail.leaveReason }}</p>
        <el-divider>审批记录</el-divider>
        <el-timeline>
          <el-timeline-item v-for="(item, index) in recordList" :key="'r' + index" :timestamp="formatTime(item.createTime)">
            {{ item.approverName || '系统' }} / {{ formatActionType(item.actionType) }} / {{ item.commentText || '无意见' }}
          </el-timeline-item>
        </el-timeline>
        <el-divider>流程历史</el-divider>
        <el-timeline>
          <el-timeline-item
            v-for="(item, index) in filteredHistoryList"
            :key="index"
            :timestamp="formatTime(item.startTime)">
            {{ item.activityName || item.activityId }} / {{ formatAssignee(item) }}
            <el-tag v-if="item.completed" type="success" size="mini">已完成</el-tag>
            <el-tag v-else type="warning" size="mini">进行中</el-tag>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-drawer>

    <ApprovalDialogWithAi
      :visible.sync="approvalVisible"
      :form="approvalForm"
      business-type="leave"
      :business-id="approvalForm.leaveId"
      :task-id="approvalTaskId"
      @submit="handleApprovalSubmit"
    />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { parseTime } from '@/utils/ruoyi'
import { listLeave, listMyApprovedLeaves, saveLeave, approveLeave, getWorkflowHistory, listApprovalRecords, getActiveTemplate, listTodoTasks, cancelLeave, calculateLeaveDuration } from '@/api/oa/workflow'
import { getEnabledLeaveTypes, getMyLeaveBalance, getPendingLeaveStats } from '@/api/oa/leaveQuota'
import ApprovalDialogWithAi from '@/components/ApprovalDialogWithAi'

const defaultForm = () => ({ leaveType: '', startTime: '', endTime: '', leaveReason: '', params: { dynamicForm: {} } })

export default {
  name: 'OaLeave',
  components: { ApprovalDialogWithAi },
  data() {
    return {
      activeTab: 'myApplications',
      loading: false,
      approvedLoading: false,
      list: [],
      approvedList: [],
      total: 0,
      approvedTotal: 0,
      query: { pageNum: 1, pageSize: 10, approvalStatus: undefined },
      approvedQuery: { pageNum: 1, pageSize: 10 },
      open: false,
      detailOpen: false,
      approvalVisible: false,
      approvalForm: {},
      approvalTaskId: '',
      detail: null,
      historyList: [],
      recordList: [],
      templateMeta: null,
      form: defaultForm(),
      rules: {},
      leaveTypeOptions: [],
      currentBalance: null, // 当前假期余额
      pendingStats: null, // 待审批统计
      calculatedWorkDays: 0, // 后端计算的工作天数
      calculatedWorkHours: 0 // 后端计算的工作小时数
    }
  },
  computed: {
    ...mapGetters(['name', 'id']),
    // 实时计算是否超额
    isOverQuota() {
      const quota = this.getCurrentQuota()
      // 只有已初始化的假期才检查超额
      if (!quota || !quota.initialized) return false
      return this.calculatedWorkDays > quota.remainingDays
    },
    activeFields() {
      if (this.templateMeta && Array.isArray(this.templateMeta.formFields) && this.templateMeta.formFields.length) {
        return this.templateMeta.formFields.filter(field => !field.displayStage || field.displayStage === 'both' || field.displayStage === 'apply')
      }
      return [
        { code: 'leaveType', label: '请假类型', type: 'select', required: true, displayStage: 'both', approveReadonly: true },
        { code: 'startTime', label: '开始时间', type: 'date', required: true, displayStage: 'both', approveReadonly: true },
        { code: 'endTime', label: '结束时间', type: 'date', required: true, displayStage: 'both', approveReadonly: true },
        { code: 'leaveReason', label: '请假事由', type: 'textarea', required: true, displayStage: 'both', approveReadonly: true }
      ]
    },
    // 过滤流程历史，只显示 startEvent、userTask、endEvent
    filteredHistoryList() {
      if (!this.historyList || !Array.isArray(this.historyList)) {
        return []
      }
      const filtered = this.historyList.filter(item => {
        if (!item.activityType) return false
        const allowedTypes = ['startEvent', 'userTask', 'endEvent']
        return allowedTypes.includes(item.activityType)
      })
      return filtered.sort((a, b) => {
        const priority = { startEvent: 0, userTask: 1, endEvent: 2 }
        const priorityA = priority[a.activityType] ?? 3
        const priorityB = priority[b.activityType] ?? 3
        if (priorityA !== priorityB) {
          return priorityA - priorityB
        }
        return new Date(a.startTime) - new Date(b.startTime)
      })
    }
  },
  watch: {
    // 监听请假类型变化，重置超额状态
    'form.leaveType': function() {
      // 余额已加载，无需重复查询
    },
    // 监听时间变化，计算是否超额
    'form.startTime': function() {
      this.showBalanceReminder()
    },
    'form.endTime': function() {
      this.showBalanceReminder()
    }
  },
  created() {
    this.getList()
    this.loadTemplate()
    this.loadLeaveRules()
  },
  methods: {
    loadLeaveRules() {
      getEnabledLeaveTypes().then(res => {
        this.leaveTypeOptions = res.data || []
        if (this.leaveTypeOptions.length > 0) {
          this.form.leaveType = this.leaveTypeOptions[0].leaveType
        }
      }).catch(() => {
        this.leaveTypeOptions = []
      })
    },
    // 查询当前假期余额
    async checkBalance() {
      try {
        const res = await getMyLeaveBalance()
        this.currentBalance = res.data || null

        // 计算是否超额
        if (this.form.leaveType && this.currentBalance && this.currentBalance.quotas) {
          const quota = this.currentBalance.quotas.find(q => q.leaveType === this.form.leaveType)
          if (quota) {
            const leaveDays = this.calculatedWorkDays
            const remainingDays = parseFloat(quota.remainingDays || 0)
            const isOverQuota = leaveDays > remainingDays
            return {
              isOverQuota: isOverQuota,
              leaveDays: leaveDays,
              remainingDays: remainingDays,
              overDays: (leaveDays - remainingDays).toFixed(2)
            }
          }
        }
        return { isOverQuota: false, leaveDays: 0, remainingDays: 0, overDays: 0 }
      } catch (error) {
        console.error('查询余额失败', error)
        return { isOverQuota: false, leaveDays: 0, remainingDays: 0, overDays: 0 }
      }
    },
    // 时间选择变化时实时计算天数
    async handleTimeChange() {
      if (!this.form.startTime || !this.form.endTime) {
        return
      }

      try {
        const startTime = this.formatDate(this.form.startTime)
        const endTime = this.formatDate(this.form.endTime)

        const res = await calculateLeaveDuration(startTime, endTime)
        if (res.code === 200 && res.data) {
          // 更新表单中的工作时长（小时）和工作天数
          this.calculatedWorkHours = res.data.workHours
          this.calculatedWorkDays = res.data.workDays.toFixed(2)

          // 显示提示
          const days = this.calculatedWorkDays
          this.$message.info(`请假时长：${days} 天（${res.data.workHours} 小时）`)
        }
      } catch (error) {
        console.error('计算请假时长失败', error)
      }
    },
    // 获取当前假期类型的额度信息
    getCurrentQuota() {
      if (!this.currentBalance || !this.currentBalance.balances || !this.form.leaveType) {
        return null
      }
      return this.currentBalance.balances.find(q => String(q.leaveType) === String(this.form.leaveType))
    },
    // 获取余额提示标题
    getBalanceAlertTitle() {
      if (this.isOverQuota) {
        return '⚠️ 假期余额不足！'
      }
      if (this.hasUninitializedQuota()) {
        return '⚠️ 存在未初始化的假期类型'
      }
      if (!this.form.leaveType) {
        return '📋 我的假期余额'
      }
      return '✅ 假期余额充足'
    },
    // 获取提示框类型
    getAlertType() {
      if (this.isOverQuota) {
        return 'error'
      }
      if (this.hasUninitializedQuota()) {
        return 'warning'
      }
      return 'info'
    },
    // 检查是否存在未初始化的假期额度
    hasUninitializedQuota() {
      if (!this.currentBalance || !this.currentBalance.balances) {
        return false
      }
      // 如果选择了特定的假期类型
      if (this.form.leaveType) {
        const quota = this.getCurrentQuota()
        return quota && !quota.initialized
      }
      // 未选择类型时，检查所有假期
      return this.currentBalance.balances.some(q => !q.initialized)
    },
    // 显示余额提醒（实时提示）
    showBalanceReminder() {
      if (!this.form.startTime || !this.form.endTime || !this.form.leaveType) {
        return
      }

      const quota = this.getCurrentQuota()
      if (!quota) {
        return
      }

      const leaveDays = this.calculatedWorkDays
      const remainingDays = parseFloat(quota.remainingDays || 0)

      if (leaveDays > 0 && leaveDays > remainingDays) {
        const overDays = (leaveDays - remainingDays).toFixed(2)
        this.$message.warning(
          `当前请假 ${leaveDays.toFixed(2)} 天，${quota.leaveName}余额仅剩 ${remainingDays} 天，超额 ${overDays} 天！`
        )
      } else if (leaveDays > 0) {
        this.$message.success(
          `当前请假 ${leaveDays.toFixed(2)} 天，${quota.leaveName}余额 ${remainingDays} 天，余额充足。`
        )
      }
    },
    // 加载待审批统计
    async loadPendingStats() {
      try {
        const res = await getPendingLeaveStats()
        const data = res.data || null

        // 转换请假类型名称为中文
        if (data && data.details) {
          data.details.forEach(item => {
            item.leaveName = this.getLeaveTypeName(item.leaveType, item.leaveName)
          })
        }

        this.pendingStats = data
      } catch (error) {
        console.error('查询待审批统计失败', error)
        this.pendingStats = null
      }
    },
    // 获取请假类型中文名称(兜底逻辑)
    getLeaveTypeName(leaveType, leaveName) {
      // 如果后端返回了有效的中文名称，直接使用
      if (leaveName && leaveName !== leaveType) {
        return leaveName
      }
      // 否则从前端选项中查找
      const option = this.leaveTypeOptions.find(opt => opt.leaveType === leaveType)
      return option ? option.leaveName : leaveType
    },
    loadTemplate() {
      getActiveTemplate('leave').then(res => {
        this.templateMeta = res.data || null
        this.buildRules()
      }).catch(() => {
        this.templateMeta = null
        this.buildRules()
      })
    },
    buildRules() {
      const rules = {}
      this.activeFields.forEach(field => {
        if (field.required) {
          rules[field.code] = [{ required: true, message: `${field.label}不能为空`, trigger: field.type === 'select' || field.type === 'date' ? 'change' : 'blur' }]
        }
      })
      this.rules = rules
    },
    handleTabClick(tab) {
      if (tab.name === 'myApproved' && this.approvedList.length === 0) {
        this.getApprovedList()
      }
    },
    getList() {
      this.loading = true
      listLeave(this.query).then(res => {
        this.list = (res.rows || []).sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
        this.total = res.total || 0
      }).finally(() => {
        this.loading = false
      })
    },
    getApprovedList() {
      this.approvedLoading = true
      listMyApprovedLeaves(this.approvedQuery).then(res => {
        this.approvedList = (res.rows || []).sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
        this.approvedTotal = res.total || 0
      }).finally(() => {
        this.approvedLoading = false
      })
    },
    resetQuery() {
      this.query = { pageNum: 1, pageSize: 10, approvalStatus: undefined }
      this.getList()
    },
    async handleAdd() {
      this.form = defaultForm()
      this.activeFields.forEach(field => {
        if (!['leaveType', 'startTime', 'endTime', 'leaveReason'].includes(field.code)) {
          this.$set(this.form.params.dynamicForm, field.code, this.defaultFieldValue(field))
        }
      })
      // 打开弹窗立即请求余额
      await this.checkBalance()
      this.loadPendingStats()
      this.open = true
    },
    defaultFieldValue(field) {
      if (field.type === 'number') {
        return 0
      }
      return ''
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
    async submitForm() {
      this.$refs.form.validate(async valid => {
        if (valid) {
          // 检查是否超额
          const balanceInfo = await this.checkBalance()

          if (balanceInfo.isOverQuota) {
            // 超额时弹出确认框
            this.$confirm(
              `您的假期余额不足！\n\n` +
              `申请天数：${balanceInfo.leaveDays} 天\n` +
              `当前余额：${balanceInfo.remainingDays} 天\n` +
              `超额天数：${balanceInfo.overDays} 天\n\n` +
              `是否继续提交？`,
              '超额提示',
              {
                confirmButtonText: '继续提交',
                cancelButtonText: '取消',
                type: 'warning'
              }
            ).then(() => {
              // 用户确认后提交
              this.doSubmit()
            }).catch(() => {
              // 用户取消
            })
          } else {
            // 未超额，直接提交
            this.doSubmit()
          }
        }
      })
    },
    doSubmit() {
      // 格式化日期字段为 yyyy-MM-dd HH:mm:ss
      const submitData = { ...this.form }
      if (submitData.startTime) {
        submitData.startTime = this.formatDate(submitData.startTime)
      }
      if (submitData.endTime) {
        submitData.endTime = this.formatDate(submitData.endTime)
      }

      saveLeave(submitData).then(() => {
        this.$message.success('提交成功')
        this.open = false
        this.getList()
      })
    },
    formatApprovalStatus(status) {
      const mapping = {
        pending: '待审批',
        approved: '已通过',
        rejected: '已驳回',
        cancelled: '已取消'
      }
      return mapping[status] || status || '-'
    },
    formatDate(date) {
      if (!date) return ''
      const d = new Date(date)
      const year = d.getFullYear()
      const month = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      const hours = String(d.getHours()).padStart(2, '0')
      const minutes = String(d.getMinutes()).padStart(2, '0')
      const seconds = String(d.getSeconds()).padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
    },
    showDetail(row) {
      this.detail = row
      this.detailOpen = true
      this.historyList = []
      this.recordList = []
      listApprovalRecords({ businessKey: `leave:${row.leaveId}`, processInstanceId: row.processInstanceId }).then(res => {
        this.recordList = res.data || []
      })
      if (row.processInstanceId) {
        getWorkflowHistory(row.processInstanceId).then(res => {
          this.historyList = res.data || []
        })
      }
    },
    formatLeaveType(type) {
      const option = this.leaveTypeOptions.find(item => item.leaveType === type)
      if (option) return option.leaveName
      const fallback = { annual: '年假', sick: '病假', personal: '事假', casual: '事假', marriage: '婚假', maternity: '产假' }
      return fallback[type] || type
    },
    formatStatus(status) {
      const map = { pending: '待审批', approved: '已通过', rejected: '已驳回' }
      return map[status] || status
    },
    formatActionType(type) {
      const map = { approve: '通过', reject: '驳回', submit: '提交', cancel: '撤回' }
      return map[type] || type
    },
    formatTime(time) {
      if (!time) return ''
      const d = new Date(time)
      const pad = n => n < 10 ? '0' + n : n
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
    },
    // 格式化节点处理人
    formatAssignee(item) {
      // startEvent 节点显示申请人
      if (item.activityType === 'startEvent') {
        return this.detail?.applicantUserName || '申请人'
      }
      // userTask 节点显示审批人
      if (item.assigneeName) {
        return item.assigneeName
      }
      // endEvent 节点：根据结束事件名称显示
      if (item.activityType === 'endEvent') {
        if (item.activityName === '已取消') {
          return '申请人取消'
        }
        return '系统'
      }
      // 其他节点显示系统
      return '系统'
    },
    openApprovalDialog(row) {
      this.approvalForm = { ...row }
      // 自动查找并匹配待办任务的taskId和businessKey
      listTodoTasks({ assignee: this.id }).then(res => {
        const tasks = res.data || []
        const matched = tasks.find(t => t.businessKey === `leave:${row.leaveId}`)
        if (matched) {
          this.approvalTaskId = matched.taskId || ''
        }
      })
      this.approvalVisible = true
    },
    handleEdit(row) {
      // 重新编辑已驳回的申请
      this.form = { ...row }
      this.open = true
    },
    handleCancel(row) {
      // 取消已驳回的申请
      this.$confirm(
        '确定要取消该申请吗？取消后流程将结束，无法恢复。',
        '取消确认',
        {
          confirmButtonText: '确定取消',
          cancelButtonText: '我再想想',
          type: 'warning'
        }
      ).then(() => {
        const params = {
          businessKey: `leave:${row.leaveId}`,
          processInstanceId: row.processInstanceId
        }
        cancelLeave(params).then(() => {
          this.$message.success('取消成功')
          this.getList()
        }).catch(err => {
          this.$message.error(err.message || '取消失败')
        })
      }).catch(() => {})
    },
    handleApprovalSubmit({ result, comment }) {
      approveLeave({
        taskId: this.approvalTaskId,
        businessKey: `leave:${this.approvalForm.leaveId}`,
        processInstanceId: this.approvalForm.processInstanceId,
        comment: comment || (result === 'approve' ? '审批通过' : '审批驳回'),
        action: result
      }).then(() => {
        this.$message.success(result === 'approve' ? '审批通过' : '审批驳回')
        this.approvalVisible = false
        this.getList()
      })
    }
  }
}
</script>

<style scoped>
.drawer-body { padding: 0 20px 20px; line-height: 1.9; }
</style>
