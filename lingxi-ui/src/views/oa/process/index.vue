<template>
  <div class="app-container">
    <el-form :inline="true" class="mb8">
      <el-form-item label="业务类型">
        <el-select v-model="query.businessType" clearable placeholder="请选择" size="mini">
          <el-option label="请假" value="leave" />
          <el-option label="报销" value="expense" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="mini" icon="el-icon-search" @click="getList">搜索</el-button>
        <el-button size="mini" icon="el-icon-refresh" @click="resetQuery">重置</el-button>
        <el-button type="primary" plain size="mini" icon="el-icon-plus" @click="handleAdd">新建模板</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="templateName" label="模板名称" min-width="160" />
      <el-table-column prop="businessType" label="业务类型" width="120">
        <template slot-scope="scope">
          <span>{{ scope.row.businessType === 'leave' ? '请假' : scope.row.businessType === 'expense' ? '报销' : scope.row.businessType }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="processDefinitionKey" label="流程标识" min-width="180" />
      <el-table-column prop="deployStatus" label="部署状态" width="120" />
      <el-table-column label="流程XML" min-width="120">
        <template slot-scope="scope">
          <span>{{ scope.row.processContent ? '已配置' : '未配置' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="formRoute" label="表单路由" min-width="140" />
      <el-table-column label="操作" width="320" align="center">
        <template slot-scope="scope">
          <el-button type="text" size="mini" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button type="text" size="mini" @click="handleDeploy(scope.row)">部署</el-button>
          <el-button 
            type="text" 
            size="mini" 
            style="color: #F56C6C"
            :loading="cleanupLoading"
            @click="handleCleanup(scope.row)">
            清理流程
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="1400px" :close-on-click-modal="false" append-to-body destroy-on-close custom-class="process-dialog">
      <div class="dialog-content">
        <el-form ref="form" :model="form" :rules="rules" label-width="120px">
          <el-form-item label="模板名称" prop="templateName"><el-input v-model="form.templateName" /></el-form-item>
          <el-form-item label="业务类型" prop="businessType">
            <el-select v-model="form.businessType" style="width:100%" @change="handleBusinessTypeChange">
              <el-option label="请假" value="leave" />
              <el-option label="报销" value="expense" />
            </el-select>
          </el-form-item>
          <el-form-item label="流程标识" prop="processDefinitionKey"><el-input v-model="form.processDefinitionKey" /></el-form-item>
          <el-form-item label="流程名称" prop="processDefinitionName"><el-input v-model="form.processDefinitionName" /></el-form-item>
          <el-form-item label="版本" prop="processVersion"><el-input v-model="form.processVersion" /></el-form-item>
          <el-form-item label="表单路由" prop="formRoute"><el-input v-model="form.formRoute" /></el-form-item>
          <el-form-item label="流程内容" prop="processContent" class="process-content-item">
            <bpmn-editor
              ref="processEditor"
              v-model="form.processContent"
              :process-key="form.processDefinitionKey || 'oa_process'"
              :process-name="form.processDefinitionName || form.templateName || 'OA流程'"
            />
          </el-form-item>
          <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
        </el-form>
      </div>
      <div slot="footer">
        <el-button type="primary" @click="submitForm">保存</el-button>
        <el-button @click="open = false">取消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<style>
.process-dialog .el-dialog__body {
  padding: 20px;
  overflow-y: hidden;
}
.process-dialog .el-dialog {
  margin-top: 5vh !important;
  max-height: 90vh;
}
.process-content-item .el-form-item__content {
  line-height: 1;
}
</style>

<script>
import { listTemplate, saveTemplate, deployTemplate, getTemplateDetail, cleanupProcessDefinitions } from '@/api/oa/workflow'
import BpmnEditor from '@/components/BpmnEditor'

const defaultForm = () => ({
  templateId: undefined,
  templateName: '',
  businessType: 'leave',
  processDefinitionKey: '',
  processDefinitionName: '',
  processVersion: 'v1',
  formRoute: '',
  processContent: '',
  description: ''
})

export default {
  name: 'OaProcess',
  components: { BpmnEditor },
  data() {
    return {
      loading: false,
      cleanupLoading: false,
      list: [],
      total: 0,
      query: { pageNum: 1, pageSize: 10, businessType: undefined },
      open: false,
      title: '新建模板',
      form: defaultForm(),
      rules: {
        templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
        businessType: [{ required: true, message: '请选择业务类型', trigger: 'change' }],
        processDefinitionKey: [{ required: true, message: '请输入流程标识', trigger: 'blur' }],
        processDefinitionName: [{ required: true, message: '请输入流程名称', trigger: 'blur' }],
        formRoute: [{ required: true, message: '请输入表单路由', trigger: 'blur' }],
        processContent: [{ required: true, message: '请配置流程内容', trigger: 'change' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listTemplate(this.query).then(res => {
        this.list = res.rows || []
        this.total = res.total || 0
      }).finally(() => {
        this.loading = false
      })
    },
    resetQuery() {
      this.query = { pageNum: 1, pageSize: 10, businessType: undefined }
      this.getList()
    },
    getBusinessDefaults(type) {
      if (type === 'expense') {
        return { templateName: '报销流程模板', processDefinitionKey: 'oa_expense_approval', processDefinitionName: '报销审批流程', formRoute: '/oa/expense' }
      }
      return { templateName: '请假流程模板', processDefinitionKey: 'oa_leave_approval', processDefinitionName: '请假审批流程', formRoute: '/oa/leave' }
    },
    applyBusinessDefaults(overwrite = true) {
      const d = this.getBusinessDefaults(this.form.businessType)
      if (overwrite || !this.form.templateName) this.form.templateName = d.templateName
      if (overwrite || !this.form.processDefinitionKey) this.form.processDefinitionKey = d.processDefinitionKey
      if (overwrite || !this.form.processDefinitionName) this.form.processDefinitionName = d.processDefinitionName
      if (overwrite || !this.form.formRoute) this.form.formRoute = d.formRoute
      if (!this.form.processVersion) this.form.processVersion = 'v1'
    },
    handleBusinessTypeChange() {
      this.applyBusinessDefaults()
    },
    handleAdd() {
      this.form = defaultForm()
      this.applyBusinessDefaults()
      this.title = '新建模板'
      this.open = true
    },
    handleEdit(row) {
      if (!row || !row.templateId) {
        this.$message.error('模板ID缺失')
        return
      }
      getTemplateDetail(row.templateId).then(res => {
        this.form = { ...defaultForm(), ...(res.data || row) }
        this.applyBusinessDefaults(false)
        this.title = '编辑模板'
        this.open = true
      })
    },
    submitForm() {
      const editor = this.$refs.processEditor
      const xmlPromise = editor && editor.getXml ? editor.getXml() : Promise.resolve(this.form.processContent)
      xmlPromise.then(xml => {
        this.form.processContent = xml
        this.$refs.form.validate(valid => {
          if (!valid) return
          saveTemplate(this.form).then(() => {
            this.$message.success('保存成功')
            this.open = false
            this.getList()
          })
        })
      })
    },
    handleDeploy(row) {
      deployTemplate(row.templateId).then(() => {
        this.$message.success('部署已提交')
        this.getList()
      })
    },
    handleCleanup(row) {
      if (!row.processDefinitionKey) {
        this.$message.warning('该模板没有配置流程标识')
        return
      }
      
      this.$confirm(
        `确定要清理流程 "${row.processDefinitionName || row.templateName}" 吗？\n\n` +
        `⚠️ 警告：这将删除该流程的所有版本和正在运行的流程实例，包括历史数据！`,
        '清理流程',
        {
          confirmButtonText: '确定清理',
          cancelButtonText: '取消',
          type: 'warning',
          dangerouslyUseHTMLString: false
        }
      ).then(() => {
        this.cleanupLoading = true
        cleanupProcessDefinitions(row.processDefinitionKey).then(() => {
          this.$message.success('流程清理成功')
          this.getList()
        }).catch(error => {
          this.$message.error('清理失败：' + (error.msg || error.message || '未知错误'))
        }).finally(() => {
          this.cleanupLoading = false
        })
      }).catch(() => {
        // 用户取消操作
      })
    }
  }
}
</script>