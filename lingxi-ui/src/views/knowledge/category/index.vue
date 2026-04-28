<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="68px" style="margin-top: 5px; margin-bottom: 2px; margin-left: 5px;">
      <el-form-item label="所属部门">
        <treeselect
          v-model="queryDeptId"
          :options="deptOptions"
          :show-count="true"
          :default-expand-level="2"
          placeholder="请选择所属部门"
          style="width: 220px;"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="getList">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd">新增</el-button>
        <el-button type="info" plain icon="el-icon-sort" size="mini" @click="toggleExpand">展开/折叠</el-button>
      </el-form-item>
    </el-form>

    <div style="overflow-x: auto;">
      <el-table
        v-if="refreshTable"
        v-loading="loading"
        :data="categoryList"
        row-key="categoryId"
        :default-expand-all="isExpandAll"
        :tree-props="{ children: 'children' }"
        border
        height="calc(100vh - 220px)"
        style="min-width: 100%;"
      >
      <el-table-column prop="categoryName" label="分类名称" min-width="120" />
      <el-table-column prop="deptNameText" label="所属部门" min-width="80" show-overflow-tooltip />
      <el-table-column prop="sort" label="排序" width="80" align="center" />
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template slot-scope="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" min-width="100">
        <template slot-scope="{ row }">
          <el-button type="text" icon="el-icon-edit" size="mini" @click="handleEdit(row)">修改</el-button>
          <el-button type="text" icon="el-icon-plus" size="mini" @click="handleAddSub(row)">新增子分类</el-button>
          <el-button type="text" icon="el-icon-delete" size="mini" style="color: #F56C6C" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    </div>

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="480px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="所属部门" prop="deptId">
          <treeselect
            v-model="form.deptId"
            :options="deptOptions"
            :show-count="true"
            :default-expand-level="2"
            placeholder="请选择所属部门"
          />
        </el-form-item>
        <el-form-item label="上级分类">
          <el-input v-model="parentName" disabled placeholder="根分类" />
        </el-form-item>
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="form.categoryName" placeholder="请输入分类名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button type="primary" @click="submitForm">确定</el-button>
        <el-button @click="dialogVisible = false">取消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { treeCategory, addCategory, updateCategory, delCategory } from '@/api/knowledge/category'
import { deptTreeSelect } from '@/api/system/user'
import Treeselect from '@riophae/vue-treeselect'
import '@riophae/vue-treeselect/dist/vue-treeselect.css'

function buildDefaultForm(deptId) {
  return {
    categoryId: undefined,
    categoryName: '',
    parentId: 0,
    deptId: deptId || undefined,
    sort: 0,
    status: 1
  }
}

export default {
  name: 'KnowledgeCategory',
  components: { Treeselect },
  data() {
    return {
      loading: false,
      categoryList: [],
      deptOptions: [],
      queryDeptId: undefined,
      isExpandAll: true,
      refreshTable: true,
      dialogVisible: false,
      dialogTitle: '新增分类',
      parentName: '根分类',
      form: buildDefaultForm(),
      rules: {
        deptId: [{ required: true, message: '请选择所属部门', trigger: 'change' }],
        categoryName: [{ required: true, message: '分类名称不能为空', trigger: 'blur' }]
      }
    }
  },
  computed: {
    ...mapGetters(['deptId'])
  },
  created() {
    this.initPage()
  },
  methods: {
    initPage() {
      this.loadDeptTree().then(() => {
        this.getList()
      })
    },
    loadDeptTree() {
      return deptTreeSelect().then(res => {
        this.deptOptions = res.data || []
      })
    },
    getList() {
      this.loading = true
      treeCategory(this.queryDeptId).then(res => {
        this.categoryList = this.attachDeptName(res.data || [])
        this.loading = false
        this.refreshTable = false
        this.$nextTick(() => {
          this.refreshTable = true
        })
      }).catch(() => {
        this.loading = false
      })
    },
    resetQuery() {
      this.queryDeptId = undefined
      this.getList()
    },
    toggleExpand() {
      this.isExpandAll = !this.isExpandAll
      this.refreshTable = false
      this.$nextTick(() => {
        this.refreshTable = true
      })
    },
    resetForm() {
      this.form = buildDefaultForm(this.queryDeptId || this.deptId)
      this.parentName = '根分类'
      if (this.$refs.form) {
        this.$refs.form.clearValidate()
      }
    },
    handleAdd() {
      this.resetForm()
      this.dialogTitle = '新增分类'
      this.dialogVisible = true
    },
    handleAddSub(row) {
      this.resetForm()
      this.form.parentId = row.categoryId
      this.form.deptId = row.deptId || this.queryDeptId || this.deptId
      this.parentName = row.categoryName
      this.dialogTitle = '新增子分类'
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.form = {
        categoryId: row.categoryId,
        categoryName: row.categoryName,
        parentId: row.parentId,
        deptId: row.deptId || this.queryDeptId || this.deptId,
        sort: row.sort,
        status: row.status
      }
      this.parentName = row.parentId === 0 ? '根分类' : `ID:${row.parentId}`
      this.dialogTitle = '修改分类'
      this.dialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.form) {
          this.$refs.form.clearValidate()
        }
      })
    },
    handleDelete(row) {
      this.$confirm(`确认删除“${row.categoryName}”吗？删除后无法恢复。`, '提示', {
        type: 'warning'
      }).then(() => {
        delCategory(row.categoryId).then(() => {
          this.$message.success('删除成功')
          this.getList()
        })
      }).catch(() => {})
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const action = this.form.categoryId ? updateCategory : addCategory
        action(this.form).then(() => {
          this.$message.success(this.form.categoryId ? '修改成功' : '新增成功')
          this.dialogVisible = false
          this.getList()
        })
      })
    },
    attachDeptName(list) {
      return (list || []).map(item => {
        const row = {
          ...item,
          deptNameText: this.getDeptLabel(item.deptId)
        }
        if (item.children && item.children.length) {
          row.children = this.attachDeptName(item.children)
        }
        return row
      })
    },
    getDeptLabel(deptId) {
      if (!deptId) {
        return ''
      }
      return this.findDeptLabel(this.deptOptions, deptId) || String(deptId)
    },
    findDeptLabel(nodes, deptId) {
      for (const node of nodes || []) {
        if (String(node.id) === String(deptId)) {
          return node.label
        }
        if (node.children && node.children.length) {
          const label = this.findDeptLabel(node.children, deptId)
          if (label) {
            return label
          }
        }
      }
      return ''
    }
  }
}
</script>

<style scoped>
</style>
