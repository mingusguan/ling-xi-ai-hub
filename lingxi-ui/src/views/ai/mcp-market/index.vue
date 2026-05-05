<template>
  <div class="app-container mcp-market-page">
    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <el-tab-pane label="工具发现" name="tools">
        <div class="toolbar market-filter-toolbar">
          <el-form :model="toolQuery" :inline="true" size="small" class="tool-discovery-filters">
            <el-form-item label="关键词">
              <el-input v-model="toolQuery.keyword" clearable placeholder="名称 / 展示名" />
            </el-form-item>
            <el-form-item label="分类">
              <el-input v-model="toolQuery.category" clearable placeholder="知识库 / OA" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="toolQuery.status" clearable placeholder="全部" style="width: 140px">
                <el-option label="草稿" value="DRAFT" />
                <el-option label="已发布" value="PUBLISHED" />
                <el-option label="已停用" value="DISABLED" />
                <el-option label="已废弃" value="DEPRECATED" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="el-icon-search" @click="loadTools">查询</el-button>
              <el-button icon="el-icon-refresh" @click="resetToolQuery">重置</el-button>
            </el-form-item>
          </el-form>
          <div class="toolbar-actions">
            <el-button v-if="canEdit" size="small" icon="el-icon-refresh-right" @click="refreshClients">刷新连接</el-button>
            <el-button v-if="canEdit" type="primary" size="small" icon="el-icon-plus" @click="openToolDialog()">发布工具</el-button>
          </div>
        </div>

        <div class="market-table-wrap">
        <el-table v-loading="toolLoading" :data="toolList" border>
          <el-table-column prop="displayName" label="工具" min-width="160">
            <template slot-scope="scope">
              <div class="name-cell">
                <span>{{ scope.row.displayName }}</span>
                <el-tag size="mini" type="info">{{ scope.row.toolName }}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="category" label="分类" width="80" />
          <el-table-column prop="serverName" label="MCP Server" width="100" />
          <el-table-column prop="version" label="版本" width="80" />
          <el-table-column prop="ownerTeam" label="负责团队" width="120" />
          <el-table-column prop="status" label="状态" width="100">
            <template slot-scope="scope">
              <el-tag :type="statusTagType(scope.row.status)" size="mini">{{ statusText(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
          <el-table-column label="操作" width="250">
            <template slot-scope="scope">
              <el-button v-if="canEdit" size="mini" type="text" icon="el-icon-edit" @click="openToolDialog(scope.row)">编辑</el-button>
              <el-button v-if="canApply" size="mini" type="text" icon="el-icon-position" @click="submitApplication(scope.row)">申请</el-button>
              <el-button v-if="canEdit" size="mini" type="text" icon="el-icon-upload2" @click="changeToolStatus(scope.row, 'publish')">发布</el-button>
              <el-button v-if="canEdit" size="mini" type="text" icon="el-icon-switch-button" @click="changeToolStatus(scope.row, 'disable')">停用</el-button>
              <el-button v-if="canEdit" size="mini" type="text" icon="el-icon-warning-outline" @click="changeToolStatus(scope.row, 'deprecate')">废弃</el-button>
              <span v-if="!canEdit && !canApply">-</span>
            </template>
          </el-table-column>
        </el-table>
        </div>

        <div class="tool-discovery-pagination">
          <pagination
            v-show="toolTotal > 0"
            :total="toolTotal"
            :page.sync="toolQuery.pageNum"
            :limit.sync="toolQuery.pageSize"
            @pagination="loadTools"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane v-if="canApprove" label="申请审批" name="applications">
        <div class="toolbar market-filter-toolbar">
          <el-form :model="applicationQuery" :inline="true" size="small">
            <el-form-item label="工具">
              <el-input v-model="applicationQuery.toolName" clearable placeholder="工具名称" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="applicationQuery.status" clearable placeholder="全部" style="width: 140px">
                <el-option label="待审批" value="PENDING" />
                <el-option label="已通过" value="APPROVED" />
                <el-option label="已拒绝" value="REJECTED" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="el-icon-search" @click="loadApplications">查询</el-button>
            </el-form-item>
          </el-form>
        </div>
        <div class="market-table-wrap">
        <el-table v-loading="applicationLoading" :data="applicationList" border>
          <el-table-column prop="toolName" label="工具" min-width="150" />
          <el-table-column prop="applicantName" label="申请人" width="80" />
          <el-table-column prop="grantTargetName" label="授权范围" width="120">
            <template slot-scope="scope">
              <el-tag size="mini" type="info">{{ grantTypeText(scope.row.grantType) }}</el-tag>
              <span class="grant-target-name">{{ scope.row.grantTargetName || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="tenantId" label="租户" width="80" />
          <el-table-column prop="purpose" label="用途" min-width="200" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="100">
            <template slot-scope="scope">
              <el-tag :type="applyTagType(scope.row.status)" size="mini">{{ applyStatusText(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="申请时间" width="170">
            <template slot-scope="scope">
              <span>{{ formatDateTime(scope.row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template slot-scope="scope">
              <template v-if="scope.row.status === 'PENDING'">
                <el-button size="mini" type="text" icon="el-icon-check" @click="openApproval(scope.row, 'approve')">通过</el-button>
                <el-button size="mini" type="text" icon="el-icon-close" @click="openApproval(scope.row, 'reject')">拒绝</el-button>
              </template>
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>
        </div>
        <div class="tool-discovery-pagination">
          <pagination
            v-show="applicationTotal > 0"
            :total="applicationTotal"
            :page.sync="applicationQuery.pageNum"
            :limit.sync="applicationQuery.pageSize"
            @pagination="loadApplications"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane v-if="canAudit" label="审计监控" name="audit">
        <el-row :gutter="12" class="metric-row">
          <el-col :span="4" v-for="item in metricItems" :key="item.label">
            <div class="metric-panel">
              <div class="metric-label">{{ item.label }}</div>
              <div class="metric-value">{{ item.value }}</div>
            </div>
          </el-col>
        </el-row>
        <div class="toolbar market-filter-toolbar">
          <el-form :model="auditQuery" :inline="true" size="small">
            <el-form-item label="工具">
              <el-input v-model="auditQuery.toolName" clearable placeholder="工具名称" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="auditQuery.status" clearable placeholder="全部" style="width: 140px">
                <el-option label="成功" value="SUCCESS" />
                <el-option label="失败" value="FAILURE" />
              </el-select>
            </el-form-item>
            <el-form-item label="租户">
              <el-input v-model="auditQuery.tenantId" clearable placeholder="租户ID" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="el-icon-search" @click="loadAuditLogs">查询</el-button>
            </el-form-item>
          </el-form>
        </div>
        <div class="market-table-wrap">
        <el-table v-loading="auditLoading" :data="auditList" border>
          <el-table-column prop="toolName" label="工具" min-width="150" />
          <el-table-column prop="serverName" label="Server" width="130" />
          <el-table-column prop="version" label="版本" width="90" />
          <el-table-column prop="username" label="用户" width="120" />
          <el-table-column prop="tenantId" label="租户" width="120" />
          <el-table-column prop="status" label="状态" width="90">
            <template slot-scope="scope">
              <el-tag :type="scope.row.status === 'SUCCESS' ? 'success' : 'danger'" size="mini">{{ scope.row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="costMillis" label="耗时(ms)" width="100" />
          <el-table-column prop="errorMessage" label="错误" min-width="220" show-overflow-tooltip />
          <el-table-column prop="createTime" label="调用时间" width="170">
            <template slot-scope="scope">
              <span>{{ formatDateTime(scope.row.createTime) }}</span>
            </template>
          </el-table-column>
        </el-table>
        </div>
        <div class="tool-discovery-pagination">
          <pagination
            v-show="auditTotal > 0"
            :total="auditTotal"
            :page.sync="auditQuery.pageNum"
            :limit.sync="auditQuery.pageSize"
            @pagination="loadAuditLogs"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane v-if="canBind" label="复用编排" name="bindings">
        <div class="toolbar market-filter-toolbar">
          <el-form :model="bindingQuery" :inline="true" size="small">
            <el-form-item label="助手编码">
              <el-input v-model="bindingQuery.agentCode" clearable placeholder="xiaolinger" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="el-icon-search" @click="loadBindings">查询</el-button>
            </el-form-item>
          </el-form>
          <el-button type="primary" size="small" icon="el-icon-link" @click="openBindingDialog()">绑定工具</el-button>
        </div>
        <div class="market-table-wrap">
        <el-table v-loading="bindingLoading" :data="bindingList" border>
          <el-table-column prop="agentCode" label="助手/场景" width="100" />
          <el-table-column prop="toolName" label="工具" width="180" />
          <el-table-column prop="enabled" label="启用" width="90">
            <template slot-scope="scope">
              <el-tag :type="scope.row.enabled === '1' ? 'success' : 'info'" size="mini">{{ scope.row.enabled === '1' ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="configJson" label="编排配置" min-width="400" show-overflow-tooltip />
          <el-table-column prop="updateTime" label="更新时间" width="170">
            <template slot-scope="scope">
              <span>{{ formatDateTime(scope.row.updateTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template slot-scope="scope">
              <el-button size="mini" type="text" icon="el-icon-edit" @click="openBindingDialog(scope.row)">编辑</el-button>
              <el-button size="mini" type="text" icon="el-icon-delete" @click="removeBinding(scope.row)">解绑</el-button>
            </template>
          </el-table-column>
        </el-table>
        </div>
        <div class="tool-discovery-pagination">
          <pagination
            v-show="bindingTotal > 0"
            :total="bindingTotal"
            :page.sync="bindingQuery.pageNum"
            :limit.sync="bindingQuery.pageSize"
            @pagination="loadBindings"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog :title="toolForm.toolId ? '编辑 MCP 工具' : '发布 MCP 工具'" :visible.sync="toolDialogVisible" width="760px">
      <el-form ref="toolForm" :model="toolForm" :rules="toolRules" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="工具名称" prop="toolName">
              <el-input v-model="toolForm.toolName" placeholder="search_knowledge" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="展示名称" prop="displayName">
              <el-input v-model="toolForm.displayName" placeholder="知识库检索" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="服务名称" prop="serverName">
              <el-input v-model="toolForm.serverName" placeholder="knowledge" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="版本号" prop="version">
              <el-input v-model="toolForm.version" placeholder="1.0.0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="分类">
              <el-input v-model="toolForm.category" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="负责团队">
              <el-input v-model="toolForm.ownerTeam" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="服务地址">
          <el-input v-model="toolForm.serverUrl" />
        </el-form-item>
        <el-form-item label="SSE端点">
          <el-input v-model="toolForm.sseEndpoint" />
        </el-form-item>
        <el-form-item label="权限标识">
          <el-input v-model="toolForm.requiredPermission" placeholder="ai:mcp:knowledge:search" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="租户隔离">
              <el-input v-model="toolForm.tenantIds" placeholder="多个租户用英文逗号分隔" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门隔离">
              <el-input v-model="toolForm.deptIds" placeholder="多个部门用英文逗号分隔" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="授权申请">
          <el-switch
            v-model="toolApprovalRequired"
            active-text="需要申请"
            inactive-text="绑定后可用"
          />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="toolForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="Schema">
          <el-input v-model="toolForm.inputSchema" type="textarea" :rows="3" placeholder="输入参数 Schema" />
        </el-form-item>
        <el-form-item label="发布说明">
          <el-input v-model="toolForm.releaseNotes" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="toolDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTool">保存</el-button>
      </div>
    </el-dialog>

    <el-dialog title="申请工具授权" :visible.sync="applicationDialogVisible" width="560px">
      <el-form ref="applicationForm" :model="applicationForm" :rules="applicationRules" label-width="100px">
        <el-form-item label="工具">
          <span>{{ applicationTool ? applicationTool.displayName : '-' }}</span>
          <el-tag v-if="applicationTool" size="mini" type="info">{{ applicationTool.toolName }}</el-tag>
        </el-form-item>
        <el-form-item label="授权范围" prop="grantType">
          <el-radio-group v-model="applicationForm.grantType">
            <el-radio
              v-for="option in grantScopeOptions"
              :key="option.grantType"
              :label="option.grantType"
            >{{ option.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="申请用途" prop="purpose">
          <el-input v-model="applicationForm.purpose" type="textarea" :rows="4" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="applicationDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveApplication">提交</el-button>
      </div>
    </el-dialog>

    <el-dialog title="审批工具申请" :visible.sync="approvalDialogVisible" width="520px">
      <el-form :model="approvalForm" label-width="90px">
        <el-form-item label="审批意见">
          <el-input v-model="approvalForm.approvalComment" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="approvalDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApproval">提交</el-button>
      </div>
    </el-dialog>

    <el-dialog title="绑定 MCP 工具" :visible.sync="bindingDialogVisible" width="600px">
      <el-form ref="bindingForm" :model="bindingForm" :rules="bindingRules" label-width="110px">
        <el-form-item label="助手编码" prop="agentCode">
          <el-input v-model="bindingForm.agentCode" placeholder="xiaolinger" />
        </el-form-item>
        <el-form-item label="工具" prop="toolId">
          <el-select v-model="bindingForm.toolId" filterable placeholder="选择已发布工具" style="width: 100%">
            <el-option
              v-for="tool in publishedToolOptions"
              :key="tool.toolId"
              :label="`${tool.displayName} (${tool.toolName})`"
              :value="tool.toolId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="bindingEnabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="编排配置">
          <el-input v-model="bindingForm.configJson" type="textarea" :rows="5" placeholder='{"alias":"知识库检索","required":true}' />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="bindingDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveBinding">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  addMarketTool,
  applyMarketTool,
  approveToolApplication,
  deleteToolBinding,
  deprecateMarketTool,
  disableMarketTool,
  getToolMarketStats,
  listGrantScopeOptions,
  listMarketTools,
  listToolApplications,
  listToolAuditLogs,
  listToolBindings,
  publishMarketTool,
  refreshMcpClients,
  rejectToolApplication,
  saveToolBinding,
  updateMarketTool
} from '@/api/ai/mcpMarket'
import { parseTime } from '@/utils/ruoyi'
import { checkPermi } from '@/utils/permission'

const defaultToolForm = () => ({
  toolId: null,
  toolName: '',
  displayName: '',
  description: '',
  serverName: '',
  serverUrl: '',
  sseEndpoint: '',
  category: '',
  version: '1.0.0',
  ownerTeam: '',
  requiredPermission: '',
  tenantIds: '',
  deptIds: '',
  auditEnabled: '1',
  marketplaceVisible: '1',
  approvalRequired: '0',
  inputSchema: '',
  outputSchema: '',
  exampleUsage: '',
  tags: '',
  releaseNotes: ''
})

export default {
  name: 'AiMcpMarket',
  data() {
    return {
      activeTab: 'tools',
      toolLoading: false,
      toolList: [],
      toolTotal: 0,
      toolQuery: { pageNum: 1, pageSize: 10, keyword: '', category: '', status: '' },
      applicationLoading: false,
      applicationList: [],
      applicationTotal: 0,
      applicationQuery: { pageNum: 1, pageSize: 10, toolName: '', status: '' },
      auditLoading: false,
      auditList: [],
      auditTotal: 0,
      auditQuery: { pageNum: 1, pageSize: 10, toolName: '', status: '', tenantId: '' },
      bindingLoading: false,
      bindingList: [],
      bindingTotal: 0,
      bindingQuery: { pageNum: 1, pageSize: 10, agentCode: 'xiaolinger' },
      stats: {},
      toolDialogVisible: false,
      toolForm: defaultToolForm(),
      applicationDialogVisible: false,
      applicationTool: null,
      applicationForm: { toolId: null, grantType: 'USER', purpose: '' },
      grantScopeOptions: [],
      approvalDialogVisible: false,
      approvalAction: 'approve',
      approvalForm: { applicationId: null, approvalComment: '' },
      bindingDialogVisible: false,
      bindingForm: { bindingId: null, agentCode: 'xiaolinger', toolId: null, enabled: '1', configJson: '' },
      toolRules: {
        toolName: [{ required: true, message: '请输入工具名称', trigger: 'blur' }],
        displayName: [{ required: true, message: '请输入展示名称', trigger: 'blur' }],
        serverName: [{ required: true, message: '请输入服务名称', trigger: 'blur' }],
        version: [{ required: true, message: '请输入版本号', trigger: 'blur' }]
      },
      bindingRules: {
        agentCode: [{ required: true, message: '请输入助手编码', trigger: 'blur' }],
        toolId: [{ required: true, message: '请选择工具', trigger: 'change' }]
      },
      applicationRules: {
        grantType: [{ required: true, message: '请选择授权范围', trigger: 'change' }],
        purpose: [{ max: 500, message: '申请用途不能超过 500 个字符', trigger: 'blur' }]
      }
    }
  },
  computed: {
    canEdit() {
      return checkPermi(['ai:mcp:market:edit'])
    },
    canApply() {
      return checkPermi(['ai:mcp:market:apply'])
    },
    canApprove() {
      return checkPermi(['ai:mcp:market:approve'])
    },
    canAudit() {
      return checkPermi(['ai:mcp:market:audit'])
    },
    canBind() {
      return checkPermi(['ai:mcp:market:bind'])
    },
    metricItems() {
      return [
        { label: '注册工具', value: this.stats.totalTools || 0 },
        { label: '已发布', value: this.stats.publishedTools || 0 },
        { label: '待审批', value: this.stats.pendingApplications || 0 },
        { label: '今日调用', value: this.stats.todayCalls || 0 },
        { label: '今日失败', value: this.stats.todayFailures || 0 },
        { label: '平均耗时', value: `${this.stats.averageCostMillis || 0}ms` }
      ]
    },
    publishedToolOptions() {
      return this.toolList.filter(item => item.status === 'PUBLISHED')
    },
    bindingEnabled: {
      get() {
        return this.bindingForm.enabled === '1'
      },
      set(value) {
        this.bindingForm.enabled = value ? '1' : '0'
      }
    },
    toolApprovalRequired: {
      get() {
        return this.toolForm.approvalRequired === '1'
      },
      set(value) {
        this.toolForm.approvalRequired = value ? '1' : '0'
      }
    }
  },
  created() {
    this.ensureAllowedActiveTab()
    this.loadTools()
    if (this.canAudit) {
      this.loadStats()
    }
  },
  methods: {
    handleTabClick() {
      this.ensureAllowedActiveTab()
      if (this.activeTab === 'applications') this.loadApplications()
      if (this.activeTab === 'audit') {
        this.loadStats()
        this.loadAuditLogs()
      }
      if (this.activeTab === 'bindings') this.loadBindings()
    },
    ensureAllowedActiveTab() {
      const allowedTabs = ['tools']
      if (this.canApprove) allowedTabs.push('applications')
      if (this.canAudit) allowedTabs.push('audit')
      if (this.canBind) allowedTabs.push('bindings')
      if (!allowedTabs.includes(this.activeTab)) {
        this.activeTab = allowedTabs[0]
      }
    },
    loadTools() {
      this.toolLoading = true
      listMarketTools(this.toolQuery).then(res => {
        this.toolList = res.rows || []
        this.toolTotal = res.total || 0
      }).finally(() => {
        this.toolLoading = false
      })
    },
    resetToolQuery() {
      this.toolQuery = { pageNum: 1, pageSize: 10, keyword: '', category: '', status: '' }
      this.loadTools()
    },
    openToolDialog(row) {
      this.toolForm = row ? { ...defaultToolForm(), ...row } : defaultToolForm()
      this.toolDialogVisible = true
      this.$nextTick(() => this.$refs.toolForm && this.$refs.toolForm.clearValidate())
    },
    saveTool() {
      this.$refs.toolForm.validate(valid => {
        if (!valid) return
        const request = this.toolForm.toolId ? updateMarketTool(this.toolForm) : addMarketTool(this.toolForm)
        request.then(() => {
          this.$message.success('保存成功')
          this.toolDialogVisible = false
          this.loadTools()
        })
      })
    },
    changeToolStatus(row, action) {
      const actionMap = {
        publish: { label: '发布', request: publishMarketTool },
        disable: { label: '停用', request: disableMarketTool },
        deprecate: { label: '废弃', request: deprecateMarketTool }
      }
      const target = actionMap[action]
      this.$confirm(`确认${target.label}工具 ${row.displayName}？`, '提示', { type: 'warning' }).then(() => {
        target.request(row.toolId).then(() => {
          this.$message.success('操作成功')
          this.loadTools()
        })
      })
    },
    refreshClients() {
      refreshMcpClients().then(() => {
        this.$message.success('MCP 运行时连接已刷新')
      })
    },
    submitApplication(row) {
      this.applicationTool = row
      this.applicationForm = { toolId: row.toolId, grantType: 'USER', purpose: '' }
      listGrantScopeOptions().then(res => {
        this.grantScopeOptions = res.data || []
        if (this.grantScopeOptions.length > 0) {
          this.applicationForm.grantType = this.grantScopeOptions[0].grantType
        }
        this.applicationDialogVisible = true
        this.$nextTick(() => this.$refs.applicationForm && this.$refs.applicationForm.clearValidate())
      })
    },
    saveApplication() {
      this.$refs.applicationForm.validate(valid => {
        if (!valid) return
        applyMarketTool(this.applicationForm).then(() => {
          this.$message.success('申请已提交')
          this.applicationDialogVisible = false
          if (this.canAudit) {
            this.loadStats()
          }
          if (this.activeTab === 'applications') {
            this.loadApplications()
          }
        })
      })
    },
    loadApplications() {
      this.applicationLoading = true
      listToolApplications(this.applicationQuery).then(res => {
        this.applicationList = res.rows || []
        this.applicationTotal = res.total || 0
      }).finally(() => {
        this.applicationLoading = false
      })
    },
    openApproval(row, action) {
      this.approvalAction = action
      this.approvalForm = { applicationId: row.applicationId, approvalComment: '' }
      this.approvalDialogVisible = true
    },
    submitApproval() {
      const request = this.approvalAction === 'approve' ? approveToolApplication : rejectToolApplication
      request(this.approvalForm).then(() => {
        this.$message.success('审批完成')
        this.approvalDialogVisible = false
        this.loadApplications()
        if (this.canAudit) {
          this.loadStats()
        }
      })
    },
    loadStats() {
      if (!this.canAudit) {
        return
      }
      getToolMarketStats().then(res => {
        this.stats = res.data || {}
      })
    },
    loadAuditLogs() {
      this.auditLoading = true
      listToolAuditLogs(this.auditQuery).then(res => {
        this.auditList = res.rows || []
        this.auditTotal = res.total || 0
      }).finally(() => {
        this.auditLoading = false
      })
    },
    loadBindings() {
      this.bindingLoading = true
      listToolBindings(this.bindingQuery).then(res => {
        this.bindingList = res.rows || []
        this.bindingTotal = res.total || 0
      }).finally(() => {
        this.bindingLoading = false
      })
    },
    openBindingDialog(row) {
      this.bindingForm = row
        ? { bindingId: row.bindingId, agentCode: row.agentCode, toolId: row.toolId, enabled: row.enabled, configJson: row.configJson }
        : { bindingId: null, agentCode: this.bindingQuery.agentCode || 'xiaolinger', toolId: null, enabled: '1', configJson: '' }
      if (this.publishedToolOptions.length === 0) {
        this.toolQuery.status = 'PUBLISHED'
        this.loadTools()
      }
      this.bindingDialogVisible = true
      this.$nextTick(() => this.$refs.bindingForm && this.$refs.bindingForm.clearValidate())
    },
    saveBinding() {
      this.$refs.bindingForm.validate(valid => {
        if (!valid) return
        saveToolBinding(this.bindingForm).then(() => {
          this.$message.success('保存成功')
          this.bindingDialogVisible = false
          this.loadBindings()
        })
      })
    },
    removeBinding(row) {
      this.$confirm(`确认解绑工具 ${row.toolName}？`, '提示', { type: 'warning' }).then(() => {
        return deleteToolBinding(row.bindingId)
      }).then(() => {
        this.$message.success('解绑成功')
        this.loadBindings()
      })
    },
    statusText(status) {
      return { DRAFT: '草稿', PUBLISHED: '已发布', DISABLED: '已停用', DEPRECATED: '已废弃' }[status] || status
    },
    statusTagType(status) {
      return { DRAFT: 'info', PUBLISHED: 'success', DISABLED: 'warning', DEPRECATED: 'danger' }[status] || 'info'
    },
    applyStatusText(status) {
      return { PENDING: '待审批', APPROVED: '已通过', REJECTED: '已拒绝' }[status] || status
    },
    applyTagType(status) {
      return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[status] || 'info'
    },
    grantTypeText(grantType) {
      return { USER: '本人', DEPT: '部门', TENANT: '租户' }[grantType] || grantType
    },
    formatDateTime(value) {
      return value ? parseTime(value, '{y}-{m}-{d} {h}:{i}:{s}') : '-'
    }
  }
}
</script>

<style scoped>
.mcp-market-page {
  height: calc(100vh - 120px);
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 4px;
  box-sizing: border-box;
}

.toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.market-filter-toolbar {
  padding-left: 16px;
}

.tool-discovery-filters {
  flex: 1;
}

.grant-target-name {
  margin-left: 6px;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.tool-discovery-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding-right: 12px;
  box-sizing: border-box;
}

.market-table-wrap {
  width: 100%;
  overflow-x: auto;
  overflow-y: hidden;
  border-radius: 4px;
  scrollbar-width: thin;
  scrollbar-color: rgba(96, 165, 250, 0.45) rgba(15, 23, 42, 0.35);
}

::v-deep .market-table-wrap .el-table__body-wrapper {
  overflow-x: auto !important;
  overflow-y: auto !important;
  scrollbar-width: thin;
  scrollbar-color: rgba(96, 165, 250, 0.45) rgba(15, 23, 42, 0.35);
}

::v-deep .market-table-wrap .el-table__fixed,
::v-deep .market-table-wrap .el-table__fixed-right {
  box-shadow: none;
}

::v-deep .market-table-wrap .el-table__fixed-header-wrapper th,
::v-deep .market-table-wrap .el-table__fixed-right .el-table__fixed-header-wrapper th,
::v-deep .market-table-wrap .el-table__fixed-right-patch {
  background: rgba(30, 41, 59, 0.8) !important;
  color: #94A3B8 !important;
  border-bottom: 1px solid rgba(59, 130, 246, 0.2) !important;
}

::v-deep .market-table-wrap .el-table__fixed-body-wrapper td,
::v-deep .market-table-wrap .el-table__fixed-right .el-table__fixed-body-wrapper td {
  background: rgba(15, 23, 42, 0.6) !important;
  color: #E2E8F0 !important;
  border-bottom: 1px solid rgba(59, 130, 246, 0.15) !important;
}

::v-deep .market-table-wrap .el-table__body tr:hover > td,
::v-deep .market-table-wrap .el-table__fixed-body-wrapper tr:hover > td,
::v-deep .market-table-wrap .el-table__fixed-right .el-table__fixed-body-wrapper tr:hover > td {
  background: rgba(59, 130, 246, 0.15) !important;
}

::v-deep .market-table-wrap::-webkit-scrollbar,
::v-deep .market-table-wrap .el-table__body-wrapper::-webkit-scrollbar,
::v-deep .mcp-market-page::-webkit-scrollbar {
  display: block;
  width: 8px;
  height: 8px;
}

::v-deep .market-table-wrap::-webkit-scrollbar-track,
::v-deep .market-table-wrap .el-table__body-wrapper::-webkit-scrollbar-track,
::v-deep .mcp-market-page::-webkit-scrollbar-track {
  background: rgba(15, 23, 42, 0.35);
  border-radius: 999px;
}

::v-deep .market-table-wrap::-webkit-scrollbar-thumb,
::v-deep .market-table-wrap .el-table__body-wrapper::-webkit-scrollbar-thumb,
::v-deep .mcp-market-page::-webkit-scrollbar-thumb {
  background: rgba(96, 165, 250, 0.45);
  border-radius: 999px;
}

::v-deep .market-table-wrap::-webkit-scrollbar-thumb:hover,
::v-deep .market-table-wrap .el-table__body-wrapper::-webkit-scrollbar-thumb:hover,
::v-deep .mcp-market-page::-webkit-scrollbar-thumb:hover {
  background: rgba(96, 165, 250, 0.62);
}

.name-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.metric-row {
  margin-bottom: 14px;
}

.metric-panel {
  min-height: 72px;
  padding: 12px 14px;
  border: 1px solid rgba(59, 130, 246, 0.22);
  border-radius: 6px;
  background: rgba(15, 23, 42, 0.66);
  box-shadow: inset 0 1px 0 rgba(148, 163, 184, 0.08);
}

.metric-label {
  color: #94A3B8;
  font-size: 13px;
  line-height: 20px;
}

.metric-value {
  margin-top: 8px;
  color: #E2E8F0;
  font-size: 22px;
  font-weight: 600;
  line-height: 26px;
}
</style>
