<template>
  <div class="app-container user-management" style="background: rgba(15, 23, 42, 0.8);">
    <el-row :gutter="0">
      <el-col :span="4" style="height: 100%;">
        <div class="head-container" style="background: rgba(15, 23, 42, 0.8); padding: 5px; border-radius: 4px; height: 100%;">
          <el-input
            v-model="deptName"
            placeholder="请输入部门名称"
            clearable
            size="small"
            prefix-icon="el-icon-search"
            style="margin-bottom: 5px"
          />
          <el-tree
            ref="tree"
            :data="deptOptions"
            :props="defaultProps"
            :expand-on-click-node="false"
            :filter-node-method="filterNode"
            node-key="id"
            default-expand-all
            highlight-current
            @node-click="handleNodeClick"
            style="background: rgba(15, 23, 42, 0.8); color: #E2E8F0;"
            class="custom-tree"
          />
        </div>
      </el-col>
      <el-col :span="20" style="height: 100%;">
        <el-form
              ref="queryForm"
              :model="queryParams"
              size="small"
              :inline="true"
              v-show="showSearch"
              style="background: rgba(15, 23, 42, 0.8); padding: 6px 4px 0 4px; border-radius: 4px; margin-bottom: 0;"
            >
              <el-form-item label="用户名" prop="userName">
                <el-input
                  v-model="queryParams.userName"
                  placeholder="请输入用户名称"
                  clearable
                  style="width: 140px"
                  @keyup.enter.native="handleQuery"
                />
              </el-form-item>
              <el-form-item label="手机" prop="phonenumber">
                <el-input
                  v-model="queryParams.phonenumber"
                  placeholder="请输入手机号码"
                  clearable
                  style="width: 140px"
                  @keyup.enter.native="handleQuery"
                />
              </el-form-item>
              <el-form-item label="状态" prop="status">
                <el-select v-model="queryParams.status" placeholder="用户状态" clearable style="width: 110px">
                  <el-option
                    v-for="dict in dict.type.sys_normal_disable"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="创建时间">
                <el-date-picker
                  v-model="dateRange"
                  style="width: 220px"
                  value-format="yyyy-MM-dd"
                  type="daterange"
                  range-separator="-"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
                <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
              </el-form-item>
            </el-form>

            <div style="background: rgba(15, 23, 42, 0.8); padding: 10px; border-radius: 4px; margin-bottom: 0;">
              <el-button
                type="primary"
                plain
                icon="el-icon-plus"
                size="mini"
                @click="handleAdd"
                v-hasPermi="['system:user:add']"
              >新增</el-button>
              <el-button
                type="success"
                plain
                icon="el-icon-edit"
                size="mini"
                :disabled="single"
                @click="handleUpdate"
                v-hasPermi="['system:user:edit']"
              >修改</el-button>
              <el-button
                type="danger"
                plain
                icon="el-icon-delete"
                size="mini"
                :disabled="multiple"
                @click="handleDelete"
                v-hasPermi="['system:user:remove']"
              >删除</el-button>
              <el-button
                type="info"
                plain
                icon="el-icon-upload2"
                size="mini"
                @click="handleImport"
                v-hasPermi="['system:user:import']"
              >导入</el-button>
              <el-button
                type="warning"
                plain
                icon="el-icon-download"
                size="mini"
                @click="handleExport"
                v-hasPermi="['system:user:export']"
              >导出</el-button>
              <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" :columns="columns" />
            </div>

            <div style="overflow-x: auto; background: rgba(15, 23, 42, 0.8); border-radius: 4px;">
              <el-table v-loading="loading" :data="userList" @selection-change="handleSelectionChange" height="calc(100vh - 290px)">
                <el-table-column type="selection" width="50" align="center" />
                <el-table-column
                  label="用户编号"
                  align="center"
                  key="userId"
                  prop="userId"
                  v-if="columns.userId.visible"
                />
                <el-table-column
                  label="用户名称"
                  align="center"
                  key="userName"
                  prop="userName"
                  v-if="columns.userName.visible"
                  :show-overflow-tooltip="true"
                />
                <el-table-column
                  label="用户昵称"
                  align="center"
                  key="nickName"
                  prop="nickName"
                  v-if="columns.nickName.visible"
                  :show-overflow-tooltip="true"
                />
                <el-table-column
                  label="部门"
                  align="center"
                  key="deptName"
                  prop="dept.deptName"
                  v-if="columns.deptName.visible"
                  :show-overflow-tooltip="true"
                />
                <el-table-column label="状态" align="center" key="status" v-if="columns.status.visible">
                  <template slot-scope="scope">
                    <el-switch
                      v-model="scope.row.status"
                      active-value="0"
                      inactive-value="1"
                      @change="handleStatusChange(scope.row)"
                    />
                  </template>
                </el-table-column>
                <el-table-column
                  label="创建时间"
                  align="center"
                  prop="createTime"
                  v-if="columns.createTime.visible"
                  width="160"
                >
                  <template slot-scope="scope">
                    <span>{{ parseTime(scope.row.createTime) }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" align="center" min-width="200" class-name="small-padding">
                  <template slot-scope="scope">
                    <el-button
                      size="mini"
                      type="text"
                      icon="el-icon-edit"
                      @click="handleUpdate(scope.row)"
                      v-hasPermi="['system:user:edit']"
                    >修改</el-button>
                    <el-button
                      v-if="scope.row.userId !== 1"
                      size="mini"
                      type="text"
                      icon="el-icon-delete"
                      style="color: #F56C6C"
                      @click="handleDelete(scope.row)"
                      v-hasPermi="['system:user:remove']"
                    >删除</el-button>
                    <el-button
                      size="mini"
                      type="text"
                      icon="el-icon-key"
                      @click="handleResetPwd(scope.row)"
                      v-hasPermi="['system:user:resetPwd']"
                    >重置密码</el-button>
                    <el-button
                      size="mini"
                      type="text"
                      icon="el-icon-circle-check"
                      @click="handleAuthRole(scope.row)"
                      v-hasPermi="['system:user:edit']"
                    >分配角色</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>

            <div style="text-align: right; padding-right: 10px;">
              <pagination
                v-show="total > 0"
                :total="total"
                :page.sync="queryParams.pageNum"
                :limit.sync="queryParams.pageSize"
                @pagination="getList"
              />
            </div>
        </el-col>
      </el-row>

    <el-dialog :title="title" :visible.sync="open" width="700px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户昵称" prop="nickName">
              <el-input v-model="form.nickName" placeholder="请输入用户昵称" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="归属部门" prop="deptId">
              <treeselect
                v-model="form.deptId"
                :options="enabledDeptOptions"
                :show-count="true"
                placeholder="请选择归属部门"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="手机号码" prop="phonenumber">
              <el-input v-model="form.phonenumber" placeholder="请输入手机号码" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item v-if="form.userId === undefined" label="用户名称" prop="userName">
              <el-input v-model="form.userName" placeholder="请输入用户名称" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.userId === undefined" label="用户密码" prop="password">
              <el-input
                v-model="form.password"
                placeholder="请输入用户密码"
                type="password"
                maxlength="20"
                show-password
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户性别">
              <el-select v-model="form.sex" placeholder="请选择性别">
                <el-option
                  v-for="dict in dict.type.sys_user_sex"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio
                  v-for="dict in dict.type.sys_normal_disable"
                  :key="dict.value"
                  :label="dict.value"
                >{{ dict.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="岗位">
              <el-select v-model="form.postIds" multiple placeholder="请选择岗位">
                <el-option
                  v-for="item in postOptions"
                  :key="item.postId"
                  :label="item.postName"
                  :value="item.postId"
                  :disabled="item.status === 1"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色">
              <el-select v-model="form.roleIds" multiple placeholder="请选择角色">
                <el-option
                  v-for="item in roleOptions"
                  :key="item.roleId"
                  :label="item.roleName"
                  :value="item.roleId"
                  :disabled="item.status === 1"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">任职信息</el-divider>
        <el-row>
          <el-col :span="12">
            <el-form-item label="入职日期" prop="entryDate">
              <el-date-picker
                v-model="form.entryDate"
                type="date"
                placeholder="选择入职日期"
                value-format="yyyy-MM-dd"
                style="width: 100%;"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="离职日期" prop="resignationDate">
              <el-date-picker
                v-model="form.resignationDate"
                type="date"
                placeholder="选择离职日期(留空表示在职)"
                value-format="yyyy-MM-dd"
                clearable
                style="width: 100%;"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="职级" prop="jobLevel">
              <el-select v-model="form.jobLevel" placeholder="请选择职级" clearable style="width: 100%;">
                <el-option
                  v-for="dict in dict.type.sys_job_level"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="在职状态" prop="employmentStatus">
              <el-radio-group v-model="form.employmentStatus">
                <el-radio label="0">在职</el-radio>
                <el-radio label="1">离职</el-radio>
                <el-radio label="2">试用期</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-alert
          v-if="form.employmentStatus === '1'"
          title="注意：标记为离职后，该用户的假期额度将自动停用"
          type="warning"
          :closable="false"
          show-icon
          style="margin: 0 10px 15px 10px;"
        />
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确定</el-button>
        <el-button @click="cancel">取消</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="upload.title" :visible.sync="upload.open" width="400px" append-to-body>
      <el-upload
        ref="upload"
        :limit="1"
        accept=".xlsx, .xls"
        :headers="upload.headers"
        :action="upload.url + '?updateSupport=' + upload.updateSupport"
        :disabled="upload.isUploading"
        :on-progress="handleFileUploadProgress"
        :on-success="handleFileSuccess"
        :auto-upload="false"
        drag
      >
        <i class="el-icon-upload" />
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <div slot="tip" class="el-upload__tip text-center">
          <div class="el-upload__tip">
            <el-checkbox v-model="upload.updateSupport" />是否更新已经存在的用户数据
          </div>
          <span>仅允许导入 `xls`、`xlsx` 格式文件。</span>
          <el-link
            type="primary"
            :underline="false"
            style="font-size: 12px; vertical-align: baseline"
            @click="importTemplate"
          >下载模板</el-link>
        </div>
      </el-upload>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitFileForm">确定</el-button>
        <el-button @click="upload.open = false">取消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
::v-deep(.custom-tree .el-tree-node__content:hover) {
  background-color: rgba(59, 130, 246, 0.3) !important;
}
::v-deep(.custom-tree .el-tree-node__content) {
  background-color: transparent !important;
}
::v-deep(.custom-tree.el-tree.el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content) {
  background-color: rgba(59, 130, 246, 0.4) !important;
  color: #E2E8F0 !important;
}
::v-deep(.custom-tree.el-tree.el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content:hover) {
  background-color: rgba(59, 130, 246, 0.5) !important;
}
::v-deep(.custom-tree.el-tree.el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content:focus) {
  background-color: rgba(59, 130, 246, 0.4) !important;
}
::v-deep .el-loading-mask {
  background-color: rgba(15, 23, 42, 0.8) !important;
}
::v-deep .el-loading-spinner .circular {
  color: #3B82F6 !important;
}
::v-deep .el-date-editor {
  background: rgba(30, 41, 59, 0.8) !important;
  border: 1px solid rgba(59, 130, 246, 0.25) !important;
  color: #E2E8F0 !important;
}
::v-deep .el-date-editor .el-range-input {
  color: #E2E8F0 !important;
  background: transparent !important;
}
::v-deep .el-date-editor .el-range-separator {
  color: #E2E8F0 !important;
}
::v-deep .el-picker-panel {
  background: rgba(15, 23, 42, 0.95) !important;
  border-color: rgba(59, 130, 246, 0.3) !important;
}
::v-deep .el-picker-panel .el-date-table {
  color: #E2E8F0 !important;
}
::v-deep .el-picker-panel .el-date-table td {
  color: #E2E8F0 !important;
}
::v-deep .el-picker-panel .el-date-table td.in-range {
  background: rgba(59, 130, 246, 0.35) !important;
}
::v-deep .el-picker-panel .el-date-table td.in-range::before {
  background: transparent !important;
}
::v-deep .el-picker-panel .el-date-table td.start-date, ::v-deep .el-picker-panel .el-date-table td.end-date {
  background: #3B82F6 !important;
  color: #ffffff !important;
}
::v-deep .el-picker-panel .el-date-table td.start-date::before, ::v-deep .el-picker-panel .el-date-table td.end-date::before {
  background: transparent !important;
}
::v-deep .el-picker-panel .el-date-table td.today {
  color: #3B82F6 !important;
}
::v-deep .el-picker-panel .el-date-table td.available:hover {
  background: rgba(59, 130, 246, 0.4) !important;
}
::v-deep .el-picker-panel .el-date-table td.is-selected {
  background: #3B82F6 !important;
  color: #ffffff !important;
}
::v-deep .el-picker-panel .el-calendar-table__row td.in-range {
  background: rgba(59, 130, 246, 0.35) !important;
}
::v-deep .el-picker-panel .el-calendar-table__row td.is-selected {
  background: #3B82F6 !important;
  color: #ffffff !important;
}
::v-deep .el-date-range-picker__content {
  background: rgba(15, 23, 42, 0.95) !important;
}
::v-deep .el-date-range-picker__header {
  background: rgba(30, 41, 59, 0.8) !important;
}
::v-deep .el-picker-panel__header {
  background: rgba(30, 41, 59, 0.8) !important;
}
::v-deep .el-picker-panel__body {
  background: rgba(15, 23, 42, 0.95) !important;
}
::v-deep .el-date-table td {
  background: transparent !important;
}
::v-deep .el-date-table td.prev-month, ::v-deep .el-date-table td.next-month {
  color: #666 !important;
}
::v-deep .el-month-table td, ::v-deep .el-year-table td {
  background: transparent !important;
}
::v-deep .el-month-table td:hover, ::v-deep .el-year-table td:hover {
  background: rgba(59, 130, 246, 0.3) !important;
}
::v-deep .el-date-editor .el-range-input {
  background: transparent !important;
}
::v-deep .el-input__inner {
  background: rgba(30, 41, 59, 0.8) !important;
  color: #E2E8F0 !important;
}
::v-deep .el-table__body-wrapper {
  border-radius: 0 0 4px 4px;
  overflow: hidden;
}
::v-deep .el-table__body tr:last-child td:first-child {
  border-bottom-left-radius: 4px;
}
::v-deep .el-table__body tr:last-child td:last-child {
  border-bottom-right-radius: 4px;
}
</style>

<script>
import { listUser, getUser, delUser, addUser, updateUser, resetUserPwd, changeUserStatus, deptTreeSelect } from '@/api/system/user'
import { getToken } from '@/utils/auth'
import Treeselect from '@riophae/vue-treeselect'
import '@riophae/vue-treeselect/dist/vue-treeselect.css'

export default {
  name: 'User',
  dicts: ['sys_normal_disable', 'sys_user_sex', 'sys_job_level'],
  components: { Treeselect },
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      userList: [],
      title: '',
      deptOptions: [],
      enabledDeptOptions: [],
      open: false,
      deptName: undefined,
      initPassword: undefined,
      dateRange: [],
      postOptions: [],
      roleOptions: [],
      form: {},
      defaultProps: {
        children: 'children',
        label: 'label'
      },
      upload: {
        open: false,
        title: '',
        isUploading: false,
        updateSupport: 0,
        headers: { Authorization: 'Bearer ' + getToken() },
        url: process.env.VUE_APP_BASE_API + '/system/user/importData'
      },
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userName: undefined,
        phonenumber: undefined,
        status: undefined,
        deptId: undefined
      },
      columns: {
        userId: { label: '用户编号', visible: true },
        userName: { label: '用户名称', visible: true },
        nickName: { label: '用户昵称', visible: true },
        deptName: { label: '部门', visible: true },
        phonenumber: { label: '手机号码', visible: true },
        status: { label: '状态', visible: true },
        createTime: { label: '创建时间', visible: true }
      },
      rules: {
        userName: [
          { required: true, message: '用户名称不能为空', trigger: 'blur' },
          { min: 2, max: 20, message: '用户名称长度必须介于 2 和 20 之间', trigger: 'blur' }
        ],
        nickName: [
          { required: true, message: '用户昵称不能为空', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '用户密码不能为空', trigger: 'blur' },
          { min: 5, max: 20, message: '用户密码长度必须介于 5 和 20 之间', trigger: 'blur' },
          { pattern: /^[^<>"'|\\]+$/, message: '不能包含非法字符：< > " \' \\ |', trigger: 'blur' }
        ],
        email: [
          { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
        ],
        phonenumber: [
          { pattern: /^1[3-9][0-9]\d{8}$/, message: '请输入正确的手机号码', trigger: 'blur' }
        ],
        entryDate: [
          { required: true, message: '入职日期不能为空', trigger: 'change' }
        ],
        employmentStatus: [
          { required: true, message: '在职状态不能为空', trigger: 'change' }
        ]
      }
    }
  },
  watch: {
    deptName(val) {
      if (this.$refs.tree) {
        this.$refs.tree.filter(val)
      }
    }
  },
  created() {
    this.getList()
    this.getDeptTree()
    this.getConfigKey('sys.user.initPassword').then(response => {
      this.initPassword = response.msg
    })
  },
  methods: {
    getList() {
      this.loading = true
      listUser(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
        this.userList = response.rows || []
        this.total = response.total || 0
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    getDeptTree() {
      deptTreeSelect().then(response => {
        this.deptOptions = response.data || []
        this.enabledDeptOptions = this.filterDisabledDept(JSON.parse(JSON.stringify(response.data || [])))
      })
    },
    filterDisabledDept(deptList) {
      return deptList.filter(dept => {
        if (dept.disabled) {
          return false
        }
        if (dept.children && dept.children.length) {
          dept.children = this.filterDisabledDept(dept.children)
        }
        return true
      })
    },
    filterNode(value, data) {
      if (!value) return true
      return data.label.indexOf(value) !== -1
    },
    handleNodeClick(data) {
      this.queryParams.deptId = data.id
      this.handleQuery()
    },
    handleStatusChange(row) {
      const text = row.status === '0' ? '启用' : '停用'
      this.$modal.confirm('确认要"' + text + '""' + row.userName + '"用户吗？').then(() => {
        return changeUserStatus(row.userId, row.status)
      }).then(() => {
        this.$modal.msgSuccess(text + '成功')
      }).catch(() => {
        row.status = row.status === '0' ? '1' : '0'
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        userId: undefined,
        deptId: undefined,
        userName: undefined,
        nickName: undefined,
        password: undefined,
        phonenumber: undefined,
        email: undefined,
        sex: undefined,
        status: '0',
        remark: undefined,
        postIds: [],
        roleIds: []
      }
      this.resetForm('form')
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.dateRange = []
      this.resetForm('queryForm')
      this.queryParams.deptId = undefined
      if (this.$refs.tree) {
        this.$refs.tree.setCurrentKey(null)
      }
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.userId)
      this.single = selection.length !== 1
      this.multiple = selection.length === 0
    },
    handleCommand(command, row) {
      switch (command) {
        case 'handleResetPwd':
          this.handleResetPwd(row)
          break
        case 'handleAuthRole':
          this.handleAuthRole(row)
          break
        default:
          break
      }
    },
    handleAdd() {
      this.reset()
      getUser().then(response => {
        this.postOptions = response.posts || []
        this.roleOptions = response.roles || []
        this.open = true
        this.title = '添加用户'
        this.form.password = this.initPassword
        // 新增用户默认在职状态
        this.form.employmentStatus = '0'
      })
    },
    handleUpdate(row) {
      this.reset()
      const userId = row.userId || this.ids
      getUser(userId).then(response => {
        this.form = response.data
        this.postOptions = response.posts || []
        this.roleOptions = response.roles || []
        this.$set(this.form, 'postIds', response.postIds || [])
        this.$set(this.form, 'roleIds', response.roleIds || [])
        // 已有用户如果没有任职状态，默认在职
        if (!this.form.employmentStatus) {
          this.form.employmentStatus = '0'
        }
        this.open = true
        this.title = '修改用户'
        this.form.password = ''
      })
    },
    handleResetPwd(row) {
      this.$prompt('请输入"' + row.userName + '"的新密码', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        closeOnClickModal: false,
        inputPattern: /^.{5,20}$/,
        inputErrorMessage: '用户密码长度必须介于 5 和 20 之间',
        inputValidator: value => {
          if (/<|>|"|'|\||\\/.test(value)) {
            return '不能包含非法字符：< > " \' \\ |'
          }
        }
      }).then(({ value }) => {
        resetUserPwd(row.userId, value).then(() => {
          this.$modal.msgSuccess('修改成功，新密码是：' + value)
        })
      }).catch(() => {})
    },
    handleAuthRole(row) {
      this.$router.push('/system/user-auth/role/' + row.userId)
    },
    /**
     * 打开员工信息对话框(直接打开修改弹窗)
     */
    handleEmployeeInfo(row) {
      this.reset()
      getUser(row.userId).then(response => {
        this.form = response.data
        // 新增用户时默认在职状态
        if (!this.form.employmentStatus) {
          this.form.employmentStatus = '0'
        }
        this.open = true
        this.title = '修改用户'
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        if (this.form.userId !== undefined) {
          updateUser(this.form).then(() => {
            this.$modal.msgSuccess('修改成功')
            this.open = false
            this.getList()
          })
        } else {
          addUser(this.form).then(() => {
            this.$modal.msgSuccess('新增成功')
            this.open = false
            this.getList()
          })
        }
      })
    },
    handleDelete(row) {
      const userIds = row.userId || this.ids
      this.$modal.confirm('是否确认删除用户编号为"' + userIds + '"的数据项？').then(() => {
        return delUser(userIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    handleExport() {
      this.download('system/user/export', {
        ...this.queryParams
      }, `user_${new Date().getTime()}.xlsx`)
    },
    handleImport() {
      this.upload.title = '用户导入'
      this.upload.open = true
    },
    importTemplate() {
      this.download('system/user/importTemplate', {}, `user_template_${new Date().getTime()}.xlsx`)
    },
    handleFileUploadProgress() {
      this.upload.isUploading = true
    },
    handleFileSuccess(response) {
      this.upload.open = false
      this.upload.isUploading = false
      this.$refs.upload.clearFiles()
      this.$alert(
        "<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" + response.msg + '</div>',
        '导入结果',
        { dangerouslyUseHTMLString: true }
      )
      this.getList()
    },
    submitFileForm() {
      const file = this.$refs.upload.uploadFiles
      if (!file || file.length === 0 || (!file[0].name.toLowerCase().endsWith('.xls') && !file[0].name.toLowerCase().endsWith('.xlsx'))) {
        this.$modal.msgError('请选择后缀为 “xls” 或 “xlsx” 的文件。')
        return
      }
      this.$refs.upload.submit()
    }
  }
}
</script>
