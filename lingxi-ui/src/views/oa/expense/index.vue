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
        <el-form :inline="true" style="margin-top: 15px; margin-bottom: 0px; margin-left: 10px;">
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
            <el-button type="primary" plain size="mini" icon="el-icon-plus" @click="handleAdd">发起报销</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="expenseNo" label="单号" min-width="140" />
          <el-table-column prop="expenseType" label="报销类型" width="100">
            <template slot-scope="scope">
              <span>{{ scope.row.expenseType === 'travel' ? '差旅' : scope.row.expenseType === 'entertain' ? '招待' : scope.row.expenseType === 'office' ? '办公' : scope.row.expenseType }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="amount" label="金额" width="100" />
          <el-table-column prop="expenseDate" label="报销日期" width="180" />
          <el-table-column prop="applicantUserName" label="申请人" width="120" />
          <el-table-column prop="deptName" label="部门" min-width="100" />
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

        <div style="padding: 10px 16px; border: 1px solid rgba(59, 130, 246, 0.2); border-top: none; border-radius: 0 0 4px 4px; margin: -1px 0 0 0; background: transparent; display: flex; justify-content: flex-end;">
          <el-pagination
            v-show="total > 0"
            background
            :current-page.sync="query.pageNum"
            :page-size.sync="query.pageSize"
            :layout="'total, sizes, prev, pager, next, jumper'"
            :page-sizes="[10, 20, 30, 50]"
            :total="total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="已处理" name="myApproved">
        <el-alert
          title="显示我发起的已结束申请 + 我审批过的申请"
          type="info"
          :closable="false"
          style="margin-bottom: 10px;"
        />
        <el-form :inline="true" style="margin-top: 15px; margin-bottom: 0px; margin-left: 10px;">
          <el-form-item label="审批状态">
            <el-select v-model="approvedQuery.approvalStatus" clearable placeholder="请选择" size="mini">
              <el-option label="待审批" value="pending" />
              <el-option label="已通过" value="approved" />
              <el-option label="已驳回" value="rejected" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="mini" icon="el-icon-search" @click="getApprovedList">搜索</el-button>
            <el-button size="mini" icon="el-icon-refresh" @click="resetApprovedQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="approvedLoading" :data="approvedList" border>
          <el-table-column prop="expenseNo" label="单号" min-width="80" />
          <el-table-column prop="expenseType" label="报销类型" width="120">
            <template slot-scope="scope">
              <span>{{ scope.row.expenseType === 'travel' ? '差旅' : scope.row.expenseType === 'entertain' ? '招待' : scope.row.expenseType === 'office' ? '办公' : scope.row.expenseType }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="amount" label="金额" width="120" />
          <el-table-column prop="expenseDate" label="报销日期" width="180" />
          <el-table-column prop="applicantUserName" label="申请人" width="120" />
          <el-table-column prop="deptName" label="部门" min-width="80" />
          <el-table-column prop="approvalStatus" label="审批状态" width="120">
            <template slot-scope="scope">
              <span>{{ formatApprovalStatus(scope.row.approvalStatus) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center">
            <template slot-scope="scope">
              <el-button type="text" size="mini" @click="showDetail(scope.row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div style="padding: 10px 16px; border: 1px solid rgba(59, 130, 246, 0.2); border-top: none; border-radius: 0 0 4px 4px; margin: -1px 0 0 0; background: transparent; display: flex; justify-content: flex-end;">
          <el-pagination
            v-show="approvedTotal > 0"
            background
            :current-page.sync="approvedQuery.pageNum"
            :page-size.sync="approvedQuery.pageSize"
            :layout="'total, sizes, prev, pager, next, jumper'"
            :page-sizes="[10, 20, 30, 50]"
            :total="approvedTotal"
            @size-change="handleApprovedSizeChange"
            @current-change="handleApprovedCurrentChange"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog title="发起报销" :visible.sync="open" width="620px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item v-for="field in activeFields" :key="field.code" :label="field.label" :prop="field.code">
          <el-select
            v-if="field.code === 'expenseType'"
            v-model="form.expenseType"
            style="width: 100%"
          >
            <el-option label="差旅" value="travel" />
            <el-option label="招待" value="entertain" />
            <el-option label="办公" value="office" />
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
          <el-input-number
            v-else-if="field.code === 'amount'"
            v-model="form.amount"
            :min="0"
            :precision="2"
            style="width: 100%"
          />
          <el-input-number
            v-else-if="field.type === 'number'"
            v-model="form.params.dynamicForm[field.code]"
            :min="0"
            :precision="2"
            style="width: 100%"
          />
          <el-date-picker
            v-else-if="field.code === 'expenseDate'"
            v-model="form.expenseDate"
            type="date"
            value-format="yyyy-MM-dd"
            style="width: 100%"
            placeholder="请选择日期"
          />
          <el-date-picker
            v-else-if="field.type === 'date'"
            v-model="form.params.dynamicForm[field.code]"
            type="date"
            value-format="yyyy-MM-dd"
            style="width: 100%"
            placeholder="请选择日期"
          />
          <el-input
            v-else-if="field.code === 'expenseReason'"
            v-model="form.expenseReason"
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
        <el-form-item label="发票附件">
          <el-upload
            ref="upload"
            :action="uploadAction"
            :headers="uploadHeaders"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :before-upload="beforeUpload"
            :file-list="fileList"
            :limit="5"
            accept=".jpg,.jpeg,.png,.pdf"
            list-type="picture-card"
          >
            <i class="el-icon-plus"></i>
            <div slot="tip" class="el-upload__tip">支持上传 jpg/png/pdf 格式，最多5张发票</div>
          </el-upload>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button type="primary" @click="submitForm">确定</el-button>
        <el-button @click="open = false">取消</el-button>
      </div>
    </el-dialog>

    <el-drawer title="报销详情" :visible.sync="detailOpen" size="40%">
      <div class="drawer-body" v-if="detail">
        <p><strong>单号：</strong>{{ detail.expenseNo }}</p>
        <p><strong>申请人：</strong>{{ detail.applicantUserName }}</p>
        <p><strong>部门：</strong>{{ detail.deptName }}</p>
        <p><strong>类型：</strong>{{ formatExpenseType(detail.expenseType) }}</p>
        <p><strong>金额：</strong><span class="amount-tag">{{ detail.amount }} 元</span></p>
        <p><strong>日期：</strong>{{ formatTime(detail.expenseDate) }}</p>
        <p><strong>状态：</strong><span :class="['status-tag', detail.approvalStatus]">{{ formatStatus(detail.approvalStatus) }}</span></p>
        <p><strong>事由：</strong>{{ detail.expenseReason }}</p>
        <div v-if="detail.invoiceUrl">
          <strong>发票附件：</strong>
          <div style="margin-top: 10px;">
            <el-image
              v-for="(url, index) in parseInvoiceUrls(detail.invoiceUrl)"
              :key="index"
              :src="url"
              :preview-src-list="parseInvoiceUrls(detail.invoiceUrl)"
              style="width: 100px; height: 100px; margin-right: 10px; cursor: pointer;"
              fit="cover"
            >
            </el-image>
            <a v-if="isPdf(detail.invoiceUrl)" :href="getFirstInvoiceUrl(detail.invoiceUrl)" target="_blank" style="color: #409EFF;">
              <i class="el-icon-document"></i> 查看PDF发票
            </a>
          </div>
        </div>
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
      business-type="expense"
      :business-id="approvalForm.expenseId"
      :task-id="approvalTaskId"
      @submit="handleApprovalSubmit"
    />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { listExpense, listMyApprovedExpenses, saveExpense, approveExpense, getWorkflowHistory, listApprovalRecords, getActiveTemplate, listTodoTasks, cancelExpense } from '@/api/oa/workflow'
import ApprovalDialogWithAi from '@/components/ApprovalDialogWithAi'
import { filePreviewUrl } from '@/utils/appPath'

const defaultForm = () => ({ expenseType: 'travel', amount: 0, expenseDate: '', expenseReason: '', invoiceUrl: '', params: { dynamicForm: {} } })

export default {
  name: 'OaExpense',
  components: { ApprovalDialogWithAi },
  data() {
    return {
      activeTab: 'myApplications',
      loading: false,
      list: [],
      total: 0,
      query: { pageNum: 1, pageSize: 10, approvalStatus: undefined },
      approvedLoading: false,
      approvedList: [],
      approvedTotal: 0,
      approvedQuery: { pageNum: 1, pageSize: 10, approvalStatus: undefined },
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
      uploadAction: process.env.VUE_APP_BASE_API + '/file/upload',
      uploadHeaders: {
        Authorization: 'Bearer ' + this.$store.getters.token
      },
      fileList: []
    }
  },
  computed: {
    ...mapGetters(['name', 'id']),
    activeFields() {
      if (this.templateMeta && Array.isArray(this.templateMeta.formFields) && this.templateMeta.formFields.length) {
        return this.templateMeta.formFields.filter(field => !field.displayStage || field.displayStage === 'both' || field.displayStage === 'apply')
      }
      return [
        { code: 'expenseType', label: '报销类型', type: 'select', required: true, displayStage: 'both', approveReadonly: true },
        { code: 'amount', label: '金额', type: 'number', required: true, displayStage: 'both', approveReadonly: true },
        { code: 'expenseDate', label: '费用日期', type: 'date', required: true, displayStage: 'both', approveReadonly: true },
        { code: 'expenseReason', label: '报销事由', type: 'textarea', required: true, displayStage: 'both', approveReadonly: true }
      ]
    },
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
  created() {
    this.getList()
    this.loadTemplate()
  },
  methods: {
    loadTemplate() {
      getActiveTemplate('expense').then(res => {
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
    getList() {
      this.loading = true
      listExpense(this.query).then(res => {
        this.list = res.rows || []
        this.total = res.total || 0
      }).finally(() => {
        this.loading = false
      })
    },
    getApprovedList() {
      this.approvedLoading = true
      listMyApprovedExpenses(this.approvedQuery).then(res => {
        this.approvedList = (res.rows || []).sort((a, b) => new Date(b.expenseDate) - new Date(a.expenseDate))
        this.approvedTotal = res.total || 0
      }).finally(() => {
        this.approvedLoading = false
      })
    },
    handleTabClick(tab) {
      if (tab.name === 'myApproved') {
        this.getApprovedList()
      }
    },
    resetQuery() {
      this.query = { pageNum: 1, pageSize: 10, approvalStatus: undefined }
      this.getList()
    },
    resetApprovedQuery() {
      this.approvedQuery = { pageNum: 1, pageSize: 10, approvalStatus: undefined }
      this.getApprovedList()
    },
    handleAdd() {
      this.form = defaultForm()
      this.activeFields.forEach(field => {
        if (!['expenseType', 'amount', 'expenseDate', 'expenseReason'].includes(field.code)) {
          this.$set(this.form.params.dynamicForm, field.code, this.defaultFieldValue(field))
        }
      })
      this.fileList = []
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
    submitForm() {
      this.$refs.form.validate(valid => {
        if (valid) {
          // 格式化日期字段
          const submitData = { ...this.form }
          if (submitData.expenseDate) {
            submitData.expenseDate = this.formatDate(submitData.expenseDate)
          }
          
          saveExpense(submitData).then(() => {
            this.$message.success('提交成功')
            this.open = false
            this.getList()
          })
        }
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
      listApprovalRecords({ businessKey: `expense:${row.expenseId}`, processInstanceId: row.processInstanceId }).then(res => {
        this.recordList = res.data || []
      })
      if (row.processInstanceId) {
        getWorkflowHistory(row.processInstanceId).then(res => {
          this.historyList = res.data || []
        })
      }
    },
    formatExpenseType(type) {
      const map = { travel: '差旅', entertain: '招待', office: '办公' }
      return map[type] || type
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
    formatAssignee(item) {
      if (item.activityType === 'startEvent') {
        return this.detail?.applicantUserName || '申请人'
      }
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
      return '系统'
    },
    openApprovalDialog(row) {
      this.approvalForm = { ...row }
      // 自动查找并匹配待办任务的taskId
      listTodoTasks({ assignee: this.id }).then(res => {
        const tasks = res.data || []
        const matched = tasks.find(t => t.businessKey === `expense:${row.expenseId}`)
        if (matched) {
          this.approvalTaskId = matched.taskId || ''
        }
      })
      this.approvalVisible = true
    },
    handleApprovalSubmit({ result, comment }) {
      approveExpense({
        taskId: this.approvalTaskId,
        businessKey: `expense:${this.approvalForm.expenseId}`,
        processInstanceId: this.approvalForm.processInstanceId,
        comment: comment || (result === 'approve' ? '审批通过' : '审批驳回'),
        action: result
      }).then(() => {
        this.$message.success(result === 'approve' ? '审批通过' : '审批驳回')
        this.approvalVisible = false
        this.getList()
      })
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
          businessKey: `expense:${row.expenseId}`,
          processInstanceId: row.processInstanceId
        }
        cancelExpense(params).then(() => {
          this.$message.success('取消成功')
          this.getList()
        }).catch(err => {
          this.$message.error(err.message || '取消失败')
        })
      }).catch(() => {})
    },
    beforeUpload(file) {
      const isImage = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'application/pdf'
      const isLt5M = file.size / 1024 / 1024 < 5
      if (!isImage) {
        this.$message.error('只能上传 JPG/PNG/PDF 格式的文件!')
        return false
      }
      if (!isLt5M) {
        this.$message.error('上传文件大小不能超过 5MB!')
        return false
      }
      return true
    },
    handleUploadSuccess(response, file, fileList) {
      if (response.code === 200) {
        // 将多个发票 URL 用逗号分隔存储
        this.form.invoiceUrl = fileList.map(f => {
          if (f.response && f.response.data) {
            return f.response.data.url || f.response.fileName
          }
          return f.url || ''
        }).filter(u => u).join(',')
        this.$message.success('上传成功')
      } else {
        this.$message.error(response.msg || '上传失败')
      }
    },
    handleUploadError(err, file, fileList) {
      this.$message.error('上传失败，请重试')
    },
    parseInvoiceUrls(urlStr) {
      if (!urlStr) return []
      return urlStr.split(',').filter(u => u && !u.endsWith('.pdf')).map(filePreviewUrl)
    },
    getFirstInvoiceUrl(urlStr) {
      if (!urlStr) return ''
      const urls = urlStr.split(',')
      return filePreviewUrl(urls.find(u => u.endsWith('.pdf')) || urls[0])
    },
    isPdf(urlStr) {
      if (!urlStr) return false
      return urlStr.includes('.pdf')
    },
    // 分页大小变化
    handleSizeChange(val) {
      if (this.query.pageNum * val > this.total) {
        this.query.pageNum = 1
      }
      this.query.pageSize = val
      this.getList()
    },
    // 分页页码变化
    handleCurrentChange(val) {
      this.query.pageNum = val
      this.getList()
    },
    // 已处理列表分页大小变化
    handleApprovedSizeChange(val) {
      if (this.approvedQuery.pageNum * val > this.approvedTotal) {
        this.approvedQuery.pageNum = 1
      }
      this.approvedQuery.pageSize = val
      this.getApprovedList()
    },
    // 已处理列表分页页码变化
    handleApprovedCurrentChange(val) {
      this.approvedQuery.pageNum = val
      this.getApprovedList()
    }
  }
}
</script>

<style scoped>
.drawer-body { padding: 0 20px 20px; line-height: 1.9; }

/* 深色主题表单样式 */
::v-deep .el-dialog {
  background: linear-gradient(135deg, #1E293B 0%, #0F172A 50%, #1E293B 100%) !important;
  border: 1px solid rgba(59, 130, 246, 0.25) !important;
  border-radius: 12px !important;
}

::v-deep .el-dialog__header {
  border-bottom: 1px solid rgba(59, 130, 246, 0.15) !important;
  color: #E2E8F0 !important;
}

::v-deep .el-dialog__title {
  color: #E2E8F0 !important;
}

::v-deep .el-dialog__body {
  color: #E2E8F0 !important;
}

::v-deep .el-form-item__label {
  color: #94A3B8 !important;
}

::v-deep .el-input__inner,
::v-deep .el-textarea__inner {
  background: rgba(30, 41, 59, 0.8) !important;
  border: 1px solid rgba(59, 130, 246, 0.25) !important;
  color: #E2E8F0 !important;
}

::v-deep .el-input__inner::placeholder,
::v-deep .el-textarea__inner::placeholder {
  color: #64748B !important;
}

::v-deep .el-select .el-input__inner {
  background: rgba(30, 41, 59, 0.8) !important;
  border: 1px solid rgba(59, 130, 246, 0.25) !important;
  color: #E2E8F0 !important;
}

::v-deep .el-date-picker__editor-wrap {
  background: rgba(30, 41, 59, 0.8) !important;
}

::v-deep .el-date-picker__header {
  background: rgba(30, 41, 59, 0.8) !important;
  color: #E2E8F0 !important;
}

::v-deep .el-date-picker__body {
  background: rgba(15, 23, 42, 0.95) !important;
  color: #E2E8F0 !important;
}

::v-deep .el-date-picker__cell {
  color: #E2E8F0 !important;
}

::v-deep .el-date-picker__cell:hover {
  background: rgba(59, 130, 246, 0.2) !important;
}

::v-deep .el-date-picker__cell.is-selected {
  background: #3B82F6 !important;
  color: #ffffff !important;
}

::v-deep .el-input-number__decrease,
::v-deep .el-input-number__increase {
  background: rgba(30, 41, 59, 0.8) !important;
  border: 1px solid rgba(59, 130, 246, 0.25) !important;
  color: #E2E8F0 !important;
}

::v-deep .el-input-number__decrease:hover,
::v-deep .el-input-number__increase:hover {
  background: rgba(59, 130, 246, 0.2) !important;
  border-color: rgba(59, 130, 246, 0.4) !important;
}

/* 上传组件样式 */
::v-deep .el-upload {
  background: transparent !important;
}

::v-deep .el-upload-list__item {
  background: rgba(30, 41, 59, 0.8) !important;
  border: 1px solid rgba(59, 130, 246, 0.25) !important;
  color: #E2E8F0 !important;
  border-radius: 8px !important;
}

::v-deep .el-upload-list__item:hover {
  border-color: rgba(59, 130, 246, 0.4) !important;
  box-shadow: 0 0 12px rgba(59, 130, 246, 0.2) !important;
}

::v-deep .el-upload-list__item-name {
  color: #E2E8F0 !important;
  font-size: 12px !important;
}

::v-deep .el-upload-list__item-status-label {
  color: #94A3B8 !important;
  font-size: 12px !important;
}

::v-deep .el-upload-list__item-actions {
  background: rgba(30, 41, 59, 0.9) !important;
  opacity: 0.8 !important;
  border-radius: 0 0 8px 8px !important;
}

::v-deep .el-upload-list__item-actions:hover {
  opacity: 1 !important;
  background: rgba(30, 41, 59, 0.95) !important;
}

::v-deep .el-upload-list__item-action {
  color: #E2E8F0 !important;
  font-size: 14px !important;
  margin: 0 5px !important;
}

::v-deep .el-upload-list__item-action:hover {
  color: #60A5FA !important;
}

/* 上传按钮 */
::v-deep .el-upload--picture-card {
  background: rgba(30, 41, 59, 0.5) !important;
  border: 2px dashed rgba(59, 130, 246, 0.3) !important;
  border-radius: 8px !important;
  width: 100px !important;
  height: 100px !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  transition: all 0.3s ease !important;
}

::v-deep .el-upload--picture-card:hover {
  background: rgba(30, 41, 59, 0.7) !important;
  border-color: rgba(59, 130, 246, 0.5) !important;
  box-shadow: 0 0 12px rgba(59, 130, 246, 0.3) !important;
  transform: translateY(-2px) !important;
}

::v-deep .el-upload--picture-card i {
  color: #94A3B8 !important;
  font-size: 24px !important;
  transition: color 0.3s ease !important;
}

::v-deep .el-upload--picture-card:hover i {
  color: #60A5FA !important;
}

::v-deep .el-upload__tip {
  color: #94A3B8 !important;
  font-size: 12px !important;
  margin-top: 8px !important;
  text-align: center !important;
}

/* 详情页样式 */
::v-deep .el-timeline-item {
  color: #E2E8F0 !important;
}

::v-deep .el-timeline-item .el-timeline-item__content {
  color: #E2E8F0 !important;
}

::v-deep .el-timeline-item__timestamp {
  color: #94A3B8 !important;
}

::v-deep .el-divider {
  background-color: rgba(59, 130, 246, 0.15) !important;
}

/* 强制覆盖所有时间线文字 */
::v-deep .el-timeline-item * {
  color: #E2E8F0 !important;
}

::v-deep .el-timeline-item__timestamp * {
  color: #94A3B8 !important;
}

/* 详情页内容文字 */
.drawer-body p {
  color: #E2E8F0 !important;
}

.drawer-body strong {
  color: #ffffff !important;
}

/* 数据显示颜色 */
.drawer-body p {
  display: flex;
  align-items: center;
}

.drawer-body p strong {
  min-width: 80px;
  color: #94A3B8 !important;
  margin-right: 10px;
}

.drawer-body p span {
  color: #E2E8F0 !important;
  flex: 1;
}

/* 标签样式 */
::v-deep .el-tag {
  background-color: rgba(59, 130, 246, 0.2) !important;
  border-color: rgba(59, 130, 246, 0.4) !important;
  color: #60A5FA !important;
}

::v-deep .el-tag--success {
  background-color: rgba(103, 194, 58, 0.2) !important;
  border-color: rgba(103, 194, 58, 0.4) !important;
  color: #67c23a !important;
}

::v-deep .el-tag--warning {
  background-color: rgba(230, 162, 60, 0.2) !important;
  border-color: rgba(230, 162, 60, 0.4) !important;
  color: #e6a23c !important;
}

/* 状态标签样式 */
.status-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  border: 1px solid transparent;
}

.status-tag.pending {
  background-color: rgba(230, 162, 60, 0.2) !important;
  border-color: rgba(230, 162, 60, 0.4) !important;
  color: #e6a23c !important;
}

.status-tag.approved {
  background-color: rgba(103, 194, 58, 0.2) !important;
  border-color: rgba(103, 194, 58, 0.4) !important;
  color: #67c23a !important;
}

.status-tag.rejected {
  background-color: rgba(245, 108, 108, 0.2) !important;
  border-color: rgba(245, 108, 108, 0.4) !important;
  color: #F56C6C !important;
}

.status-tag.cancelled {
  background-color: rgba(148, 163, 184, 0.2) !important;
  border-color: rgba(148, 163, 184, 0.4) !important;
  color: #94A3B8 !important;
}

/* 金额标签样式 */
.amount-tag {
  color: #67c23a !important;
  font-weight: 600;
  font-size: 14px;
  display: inline-block;
  padding: 2px 8px;
  background-color: rgba(103, 194, 58, 0.1) !important;
  border: 1px solid rgba(103, 194, 58, 0.3) !important;
  border-radius: 6px;
}

/* 发票附件链接 */
.drawer-body a {
  color: #60A5FA !important;
}

.drawer-body a:hover {
  color: #3B82F6 !important;
  text-decoration: underline !important;
}
</style>
