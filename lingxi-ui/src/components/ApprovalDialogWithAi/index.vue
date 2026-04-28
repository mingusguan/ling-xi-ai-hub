<template>
  <el-dialog title="审批处理" :visible="dialogVisible" width="950px" append-to-body :close-on-click-modal="false" @close="handleClose">
    <div class="approval-container">
      <div class="approval-main">
        <div class="form-section">
          <h4 class="section-title">审批表单</h4>
          <div class="form-scroll">
            <div class="custom-form-card">
              <div class="form-grid">
                <div class="form-item">
                  <div class="form-label">申请人</div>
                  <div class="form-value">{{ form.applicantUserName || form.applicantName || form.applicant || '-' }}</div>
                </div>
                <div class="form-item">
                  <div class="form-label">部门</div>
                  <div class="form-value">{{ form.deptName || form.department || form.dept || '-' }}</div>
                </div>
                <div v-if="form.expenseType" class="form-item">
                  <div class="form-label">报销类型</div>
                  <div class="form-value">{{ formatExpenseType(form.expenseType) }}</div>
                </div>
                <div v-if="form.amount" class="form-item">
                  <div class="form-label">金额</div>
                  <div class="form-value">{{ form.amount }} 元</div>
                </div>
                <div v-if="form.expenseDate" class="form-item">
                  <div class="form-label">报销日期</div>
                  <div class="form-value">{{ form.expenseDate }}</div>
                </div>
                <div v-if="form.leaveType" class="form-item">
                  <div class="form-label">请假类型</div>
                  <div class="form-value">{{ formatLeaveType(form.leaveType) }}</div>
                </div>
                <div v-if="form.leaveHours" class="form-item">
                  <div class="form-label">请假时长</div>
                  <div class="form-value">{{ (form.leaveHours / 8).toFixed(1) }} 天</div>
                </div>
                <div class="form-item form-item-full">
                  <div class="form-label">事由</div>
                  <div class="form-value">{{ form.expenseReason || form.leaveReason }}</div>
                </div>
              </div>
            </div>

            <el-divider />

            <el-form label-width="80px">
              <el-form-item label="审批意见">
                <el-input
                  v-model="approvalComment"
                  type="textarea"
                  :rows="4"
                  placeholder="请输入审批意见，可从右侧选择AI推荐的意见"
                />
              </el-form-item>
            </el-form>
          </div>
        </div>

        <div slot="footer" class="dialog-footer">
          <el-button @click="handleClose">取消</el-button>
          <el-button type="primary" @click="handleApprove">通过</el-button>
          <el-button type="danger" @click="handleReject">驳回</el-button>
        </div>
      </div>

      <div class="ai-sidebar">
        <div class="ai-sidebar-header">
          <i class="el-icon-cpu"></i>
          <span>AI审批助手</span>
          <el-button
            v-if="!aiSuggestion"
            type="text"
            size="mini"
            :loading="aiLoading"
            @click="generateAiSuggestion"
          >
            生成建议
          </el-button>
        </div>

        <div v-if="aiLoading" class="ai-loading">
          <i class="el-icon-loading"></i>
          <span>AI正在分析中...</span>
        </div>

        <div v-if="aiSuggestion" class="ai-content">
          <div class="ai-summary">
            <div class="summary-header">
              <i class="el-icon-cpu"></i>
              <span>AI 智能分析</span>
            </div>
            <div class="summary-text">{{ aiSuggestion }}</div>
          </div>

          <div v-if="riskPoints.length > 0" class="ai-section">
            <div class="section-header risk">
              <i class="el-icon-warning-outline"></i>
              <span>风险提示 ({{ riskLevelText }})</span>
            </div>
            <div class="section-content">
              <div v-for="(point, index) in riskPoints" :key="index" class="risk-item">
                <i class="el-icon-warning"></i>
                <span>{{ point }}</span>
              </div>
            </div>
          </div>

          <div class="ai-section">
            <div class="section-header suggest">
              <i class="el-icon-lightbulb"></i>
              <span>AI推荐意见</span>
            </div>
            <div class="section-content">
              <div
                v-for="(template, index) in templates"
                :key="index"
                class="template-item"
                @click="useTemplate(template)"
              >
                <span class="use-icon">📋</span>
                <span class="template-text">{{ template }}</span>
              </div>
            </div>
          </div>

          <div v-if="similarCases.length > 0" class="ai-section">
            <div class="section-header case">
              <i class="el-icon-document-copy"></i>
              <span>历史审批记录</span>
              <span class="case-stat">({{ similarCases.length }} 笔, 通过率 {{ calculatePassRate }}%)</span>
            </div>
            <div class="section-content">
              <div v-for="(item, index) in similarCases" :key="index" class="case-item" :class="{ rejected: item.approvalResult === '驳回' }">
                <div class="case-header">
                  <div class="case-applicant">
                    <span class="avatar">{{ item.applicant && item.applicant.charAt(0) }}</span>
                    <div class="applicant-info">
                      <span class="name">{{ item.applicant }}</span>
                      <span class="dept" v-if="item.dept">{{ item.dept }}</span>
                    </div>
                  </div>
                  <el-tag :type="item.approvalResult === '通过' ? 'success' : 'danger'" size="mini">
                    {{ item.approvalResult }}
                  </el-tag>
                </div>
                <div class="case-info" v-if="item.type || item.value">
                  <span class="type">{{ item.type }}</span>
                  <span class="value" v-if="item.value">{{ item.value }}</span>
                </div>
                <div class="case-opinion">
                  <i class="el-icon-chat-line-round"></i>
                  <span>{{ item.approverOpinion || (item.approvalResult === '通过' ? '同意，流程合规' : '流程待优化') }}</span>
                </div>
                <div class="case-footer">
                  <span class="approver">
                    <i class="el-icon-user"></i>
                    {{ item.approverName || item.approver || '系统审批' }}
                  </span>
                  <span class="time">
                    <i class="el-icon-time"></i>
                    {{ formatCaseTime(item.applyTime) }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script>
import { getOpinionSuggest, getOpinionStatus } from '@/api/oa/ai'
import { getEnabledLeaveTypes } from '@/api/oa/leaveQuota'

export default {
  name: 'ApprovalDialogWithAi',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    form: {
      type: Object,
      default: () => ({})
    },
    businessType: {
      type: String,
      default: 'expense'
    },
    businessId: {
      type: [Number, String],
      default: null
    },
    taskId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      dialogVisible: false,
      approvalComment: '',
      aiLoading: false,
      aiSuggestion: null,
      riskLevel: 'low',
      riskPoints: [],
      templates: [],
      similarCases: [],
      pollTimer: null,
      leaveTypeOptions: []
    }
  },
  watch: {
    visible(val) {
      this.dialogVisible = val
      if (val && this.businessType === 'leave') {
        this.loadLeaveRules()
      }
    }
  },
  computed: {
    riskLevelText() {
      const map = { low: '低风险', normal: '中风险', high: '高风险' }
      return map[this.riskLevel] || '低风险'
    },
    calculatePassRate() {
      if (!this.similarCases || this.similarCases.length === 0) return 0
      const passed = this.similarCases.filter(c => c.approvalResult === '通过').length
      return Math.round((passed / this.similarCases.length) * 100)
    }
  },
  methods: {
    loadLeaveRules() {
      getEnabledLeaveTypes().then(res => {
        this.leaveTypeOptions = res.data || []
      }).catch(() => {
        this.leaveTypeOptions = []
      })
    },
    generateAiSuggestion() {
      this.aiLoading = true
      getOpinionSuggest({
        businessType: this.businessType,
        businessId: this.businessId,
        taskId: this.taskId || 'none'
      }).then(res => {
        // 先加载推荐意见和历史案例
        this.templates = res.data.templates || []
        this.similarCases = res.data.similarCases || []

        // 如果返回loading状态，开始轮询AI建议
        if (res.data.riskLevel === 'loading') {
          this.startPolling()
        } else {
          // 直接显示完整结果
          this.updateAiData(res.data)
          this.aiLoading = false
        }
      }).catch(err => {
        this.aiLoading = false
        console.error('获取AI建议失败:', err)
      })
    },

    // 开始轮询
    startPolling() {
      this.pollTimer = setInterval(() => {
        getOpinionStatus({
          businessType: this.businessType,
          businessId: this.businessId
        }).then(res => {
          if (res.data.status === 'completed') {
            // AI生成完成，更新所有AI相关内容
            clearInterval(this.pollTimer)
            this.pollTimer = null
            this.aiSuggestion = res.data.aiSuggestion
            this.riskLevel = res.data.riskLevel
            this.riskPoints = res.data.riskPoints || []
            // 更新AI生成的推荐意见
            if (res.data.templates && res.data.templates.length > 0) {
              this.templates = res.data.templates
            }
            this.aiLoading = false
            this.$message.success('AI分析完成')
          }
        }).catch(err => {
          console.error('轮询失败:', err)
        })
      }, 1000) // 每1秒查询一次
    },

    // 更新AI数据（首次加载时使用）
    updateAiData(data) {
      this.aiSuggestion = data.aiSuggestion
      this.riskLevel = data.riskLevel
      this.riskPoints = data.riskPoints || []
      this.templates = data.templates || []
      this.similarCases = data.similarCases || []
    },
    useTemplate(template) {
      this.approvalComment = template
      this.$message.success('已应用模板')
    },
    handleApprove() {
      this.$confirm('确定要通过这个审批吗？', '确认通过', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'success'
      }).then(() => {
        this.submitApproval('approve')
      }).catch(() => {})
    },

    handleReject() {
      this.$confirm('确定要驳回这个审批吗？', '确认驳回', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.submitApproval('reject')
      }).catch(() => {})
    },

    submitApproval(result) {
      this.$emit('submit', {
        result: result,
        comment: this.approvalComment,
        form: this.form
      })
      this.handleClose()
    },
    formatExpenseType(type) {
      const map = { travel: '差旅', entertain: '招待', office: '办公', other: '其他' }
      return map[type] || type
    },
    formatLeaveType(type) {
      const option = this.leaveTypeOptions.find(item => item.leaveType === type)
      if (option) return option.leaveName
      const fallback = { annual: '年假', sick: '病假', personal: '事假', casual: '事假', marriage: '婚假', maternity: '产假' }
      return fallback[type] || type
    },
    formatCaseTime(time) {
      if (!time) return ''
      const d = new Date(time)
      const pad = n => n < 10 ? '0' + n : n
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
    },
    handleClose() {
      // 清除轮询定时器
      if (this.pollTimer) {
        clearInterval(this.pollTimer)
        this.pollTimer = null
      }
      this.$emit('update:visible', false)
    }
  },
  watch: {
    visible(val) {
      this.dialogVisible = val
      if (val && !this.aiSuggestion) {
        this.$nextTick(() => {
          this.generateAiSuggestion()
        })
      }
      if (!val) {
        this.approvalComment = ''
      }
    }
  }
}
</script>

<style scoped>
::v-deep .el-dialog {
  background: linear-gradient(135deg, #1E293B 0%, #0F172A 50%, #1E293B 100%) !important;
}

::v-deep .el-dialog__title {
  color: #E2E8F0 !important;
}

::v-deep .el-dialog__headerbtn .el-dialog__close {
  color: #94A3B8 !important;
}

::v-deep .el-descriptions {
  color: #E2E8F0 !important;
}

::v-deep .el-descriptions table,
::v-deep .el-descriptions table tr {
  background: transparent !important;
}

::v-deep th.el-descriptions-item__cell,
::v-deep .el-descriptions-item__cell.is-bordered-label {
  background-color: rgba(59, 130, 246, 0.25) !important;
  color: #94A3B8 !important;
  border-color: rgba(59, 130, 246, 0.3) !important;
}

::v-deep td.el-descriptions-item__cell,
::v-deep .el-descriptions-item__cell.is-bordered-content {
  background-color: rgba(30, 41, 59, 0.85) !important;
  color: #E2E8F0 !important;
  border-color: rgba(59, 130, 246, 0.3) !important;
}

::v-deep .el-form-item__label {
  color: #94A3B8 !important;
}

::v-deep .el-textarea__inner {
  background: rgba(30, 41, 59, 0.8) !important;
  border-color: rgba(59, 130, 246, 0.2) !important;
  color: #E2E8F0 !important;
}

::v-deep .el-textarea__inner::placeholder {
  color: #64748B !important;
}

::v-deep .el-divider--horizontal {
  background-color: rgba(59, 130, 246, 0.15) !important;
}

.section-title {
  color: #E2E8F0;
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

.custom-form-card {
  background: rgba(15, 23, 42, 0.7);
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.form-item {
  background: rgba(30, 41, 59, 0.6);
  border: 1px solid rgba(59, 130, 246, 0.15);
  border-radius: 6px;
  padding: 10px 12px;
  transition: all 0.3s ease;
}

.form-item:hover {
  border-color: rgba(59, 130, 246, 0.35);
  background: rgba(30, 41, 59, 0.75);
}

.form-item-full {
  grid-column: span 2;
}

.form-label {
  color: #94A3B8;
  font-size: 12px;
  margin-bottom: 4px;
  font-weight: 500;
}

.form-value {
  color: #E2E8F0;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.4;
}

.approval-container {
  display: flex;
  gap: 20px;
  min-height: 500px;
  max-height: 550px;
}

.approval-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.form-section {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.form-scroll {
  flex: 1;
  overflow-y: auto;
  padding-right: 10px;
}

.form-scroll::-webkit-scrollbar {
  width: 6px;
}

.form-scroll::-webkit-scrollbar-thumb {
  background: rgba(59, 130, 246, 0.3);
  border-radius: 3px;
}

.form-scroll::-webkit-scrollbar-thumb:hover {
  background: rgba(59, 130, 246, 0.5);
}

.ai-sidebar {
  width: 380px;
  border-left: 1px solid rgba(59, 130, 246, 0.15);
  padding-left: 20px;
  display: flex;
  flex-direction: column;
}

.ai-content {
  flex: 1;
  overflow-y: auto;
  padding-right: 10px;
  padding-top: 10px;
}

.ai-content::-webkit-scrollbar {
  width: 6px;
}

.ai-content::-webkit-scrollbar-thumb {
  background: rgba(59, 130, 246, 0.3);
  border-radius: 3px;
}

.ai-content::-webkit-scrollbar-thumb:hover {
  background: rgba(59, 130, 246, 0.5);
}

.ai-sidebar-header {
  display: flex;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(59, 130, 246, 0.15);
  margin-bottom: 16px;
}

.ai-sidebar-header i {
  font-size: 18px;
  color: #67c23a;
  margin-right: 8px;
}

.ai-sidebar-header span {
  flex: 1;
  font-weight: 600;
  color: #E2E8F0;
}

.ai-sidebar-header .el-button {
  color: #60A5FA;
}

.ai-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
  color: #94A3B8;
}

.ai-loading i {
  font-size: 32px;
  margin-bottom: 12px;
  color: #409eff;
}

.ai-summary {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.8) 0%, rgba(118, 75, 162, 0.8) 100%);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 16px;
  color: #fff;
  border: 1px solid rgba(96, 165, 250, 0.3);
}

.summary-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
}

.summary-text {
  font-size: 12px;
  line-height: 1.6;
  opacity: 0.95;
  white-space: pre-wrap;
}

.ai-section {
  margin-bottom: 20px;
}

.section-header {
  display: flex;
  align-items: center;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 10px;
}

.section-header i {
  margin-right: 6px;
}

.section-header.risk {
  color: #e6a23c;
}

.section-header.suggest {
  color: #409eff;
}

.section-header.case {
  color: #67c23a;
  justify-content: flex-start;
}

.case-stat {
  margin-left: auto;
  font-weight: normal;
  font-size: 11px;
  color: #64748B;
}

.section-content {
  background: rgba(30, 41, 59, 0.6);
  border-radius: 8px;
  padding: 8px;
  max-height: 280px;
  overflow-y: auto;
  border: 1px solid rgba(59, 130, 246, 0.15);
}

.section-content::-webkit-scrollbar {
  width: 6px;
}

.section-content::-webkit-scrollbar-thumb {
  background: rgba(59, 130, 246, 0.3);
  border-radius: 3px;
}

.section-content::-webkit-scrollbar-thumb:hover {
  background: rgba(59, 130, 246, 0.5);
}

.risk-item {
  display: flex;
  align-items: flex-start;
  padding: 6px 0;
  font-size: 12px;
  color: #e6a23c;
}

.risk-item i {
  margin-right: 6px;
  flex-shrink: 0;
  margin-top: 2px;
}

.template-item {
  display: flex;
  align-items: flex-start;
  padding: 8px;
  background: rgba(15, 23, 42, 0.8);
  border-radius: 6px;
  margin-bottom: 8px;
  cursor: pointer;
  border: 1px solid rgba(59, 130, 246, 0.15);
  transition: all 0.2s;
}

.template-item:hover {
  border-color: #409eff;
  background: rgba(59, 130, 246, 0.1);
}

.template-item:last-child {
  margin-bottom: 0;
}

.use-icon {
  margin-right: 8px;
  font-size: 14px;
}

.template-text {
  flex: 1;
  font-size: 12px;
  color: #E2E8F0;
  line-height: 1.5;
}

.case-item {
  padding: 12px;
  background: rgba(15, 23, 42, 0.8);
  border-radius: 6px;
  margin-bottom: 8px;
  border: 1px solid rgba(59, 130, 246, 0.15);
  transition: all 0.2s;
}

.case-item:hover {
  border-color: rgba(59, 130, 246, 0.3);
}

.case-item.rejected {
  border-left: 3px solid #f56c6c;
  background: linear-gradient(to right, rgba(245, 108, 108, 0.1), rgba(15, 23, 42, 0.8));
}

.case-item:last-child {
  margin-bottom: 0;
}

.case-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.case-applicant {
  display: flex;
  align-items: center;
  gap: 8px;
}

.case-applicant .avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3B82F6, #8B5CF6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}

.applicant-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.applicant-info .name {
  font-size: 12px;
  font-weight: 600;
  color: #E2E8F0;
}

.applicant-info .dept {
  font-size: 10px;
  color: #64748B;
}

.case-info {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
  padding: 4px 8px;
  background: rgba(30, 41, 59, 0.6);
  border-radius: 4px;
}

.case-info .type,
.case-info .value {
  font-size: 11px;
  color: #94A3B8;
}

.case-info .type {
  font-weight: 600;
}

.case-opinion {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 6px 0;
  font-size: 11px;
  color: #94A3B8;
  line-height: 1.5;
  margin-bottom: 8px;
}

.case-opinion i {
  color: #64748B;
  flex-shrink: 0;
  margin-top: 1px;
}

.case-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 8px;
  border-top: 1px dashed rgba(59, 130, 246, 0.15);
}

.case-footer .approver,
.case-footer .time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  color: #64748B;
}

.case-footer i {
  font-size: 11px;
}

.dialog-footer {
  margin-top: 20px;
  text-align: right;
}

/* 驳回按钮悬浮样式 */
::v-deep .el-button--danger:hover {
  background-color: #F78989 !important;
  border-color: #F78989 !important;
  color: #ffffff !important;
  box-shadow: 0 0 12px rgba(245, 108, 108, 0.4) !important;
  transform: translateY(-1px) !important;
  transition: all 0.3s ease !important;
}

::v-deep .el-button--danger {
  transition: all 0.3s ease !important;
}

</style>

<style>
/* 全局强制覆盖样式 - 必须生效！ */
.el-dialog .el-descriptions table th.el-descriptions-item__cell,
.el-dialog .el-descriptions table th.el-descriptions-item__cell.is-bordered-label {
  background-color: rgba(59, 130, 246, 0.3) !important;
  color: #94A3B8 !important;
  border-color: rgba(59, 130, 246, 0.35) !important;
}

.el-dialog .el-descriptions table td.el-descriptions-item__cell,
.el-dialog .el-descriptions table td.el-descriptions-item__cell.is-bordered-content {
  background-color: rgba(30, 41, 59, 0.9) !important;
  color: #E2E8F0 !important;
  border-color: rgba(59, 130, 246, 0.35) !important;
}

.el-dialog .el-descriptions table,
.el-dialog .el-descriptions table tr {
  background: transparent !important;
}
</style>
