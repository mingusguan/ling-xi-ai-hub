<template>
  <div class="app-container">
    <el-form :inline="true" class="mb8">
      <el-form-item label="所属部门">
        <treeselect
          v-model="queryParams.deptId"
          :options="deptOptions"
          :show-count="true"
          :default-expand-level="2"
          placeholder="请选择所属部门"
          style="width: 200px"
        />
      </el-form-item>
      <el-form-item label="分类">
        <treeselect
          v-model="queryParams.categoryId"
          :options="categoryTreeOptions"
          :show-count="true"
          placeholder="全部分类"
          style="width: 200px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
        <el-button type="primary" plain icon="el-icon-upload" size="mini" @click="openUpload">上传文档</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="docList" border>
      <el-table-column prop="docId" label="ID" width="70" align="center" />
      <el-table-column prop="docName" label="文档名称" min-width="200" show-overflow-tooltip />
      <el-table-column prop="docType" label="类型" width="160" show-overflow-tooltip />
      <el-table-column prop="fileSize" label="大小" width="100" align="right">
        <template slot-scope="{ row }">{{ formatSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template slot-scope="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="上传时间" width="160" align="center">
        <template slot-scope="{ row }">{{ parseTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200">
        <template slot-scope="{ row }">
          <el-button type="text" icon="el-icon-view" size="mini" @click="handleDetail(row)">详情</el-button>
          <el-button type="text" icon="el-icon-refresh" size="mini" @click="handleReEmbed(row)">重新向量化</el-button>
          <el-button type="text" icon="el-icon-delete" size="mini" style="color:#F56C6C" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog title="上传文档" :visible.sync="uploadVisible" width="500px" append-to-body @close="resetUpload">
      <el-form ref="uploadForm" :model="uploadForm" :rules="uploadRules" label-width="100px">
        <el-form-item label="文档类型" prop="docType">
          <el-radio-group v-model="uploadForm.docType" @change="handleDocTypeChange">
            <el-radio label="public">公共文档</el-radio>
            <el-radio label="dept">部门文档</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="uploadForm.docType === 'dept'" label="可见部门" prop="visibleDeptIds">
          <treeselect
            v-model="uploadForm.visibleDeptIds"
            :options="deptOptions"
            :show-count="true"
            :default-expand-level="2"
            :multiple="true"
            :flat="true"
            :auto-select-descendants="true"
            :auto-deselect-descendants="true"
            :disable-branch-nodes="false"
            placeholder="请选择可见部门（选择父级将包含所有子级）"
          />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <treeselect
            v-model="uploadForm.categoryId"
            :options="categoryTreeOptions"
            :show-count="true"
            placeholder="请选择分类"
          />
        </el-form-item>
        <el-form-item label="文件" prop="fileUrl">
          <FileUpload
            v-model="uploadForm.fileUrl"
            :limit="1"
            :fileSize="50"
            :fileType="['pdf', 'docx', 'txt', 'doc']"
          />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button type="primary" :loading="uploading" @click="submitUpload">{{ uploading ? '上传中...' : '确 定' }}</el-button>
        <el-button @click="uploadVisible = false">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="文档详情" :visible.sync="detailVisible" width="500px" append-to-body>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="文档ID">{{ detail.docId }}</el-descriptions-item>
        <el-descriptions-item label="文档名称">{{ detail.docName }}</el-descriptions-item>
        <el-descriptions-item label="文档类型">{{ detail.docType }}</el-descriptions-item>
        <el-descriptions-item label="文件大小">{{ formatSize(detail.fileSize) }}</el-descriptions-item>
        <el-descriptions-item label="所属部门">{{ detail.deptName || detail.deptId }}</el-descriptions-item>
        <el-descriptions-item label="可见部门">
          {{ formatVisibleDepts(detail.visibleDeptIds) }}
        </el-descriptions-item>
        <el-descriptions-item label="分类">{{ detail.categoryName || detail.categoryId }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="存储地址">
          <el-link :href="detail.fileUrl" target="_blank" type="primary">{{ detail.fileUrl }}</el-link>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(detail.createTime) }}</el-descriptions-item>
      </el-descriptions>
      <div slot="footer">
        <el-button @click="detailVisible = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listDocument, getDocument, uploadDocument, delDocument, reEmbedDocument } from '@/api/knowledge/document'
import { treeCategory } from '@/api/knowledge/category'
import { deptTreeSelect } from '@/api/system/user'
import Treeselect from '@riophae/vue-treeselect'
import '@riophae/vue-treeselect/dist/vue-treeselect.css'
import FileUpload from '@/components/FileUpload'

export default {
  name: 'KnowledgeDocument',
  components: { Treeselect, FileUpload },
  data() {
    return {
      loading: false,
      docList: [],
      total: 0,
      deptOptions: [],
      allCategoryTree: [],
      categoryTreeOptions: [],
      queryParams: { 
        pageNum: 1, 
        pageSize: 10,
        deptId: undefined, 
        categoryId: undefined 
      },
      uploadVisible: false,
      uploading: false,
      uploadForm: { docType: 'public', visibleDeptIds: [], categoryId: undefined, fileUrl: '' },
      uploadRules: {
        visibleDeptIds: [
          { 
            validator: (rule, value, callback) => {
              if (this.uploadForm.docType === 'dept' && (!value || value.length === 0)) {
                callback(new Error('请选择可见部门'))
              } else {
                callback()
              }
            }, 
            trigger: 'change' 
          }
        ],
        categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }]
      },
      detailVisible: false,
      detail: {}
    }
  },
  created() {
    this.loadDeptTree()
    this.loadAllCategoryTree()
    this.handleQuery()
  },
  watch: {
    'queryParams.deptId'(newVal) {
      this.queryParams.categoryId = undefined
      this.filterCategoryTree(newVal)
    },
    'uploadForm.deptId'(newVal) {
      this.uploadForm.categoryId = undefined
      this.filterCategoryTree(newVal)
    }
  },
  methods: {
    loadDeptTree() {
      deptTreeSelect().then(res => {
        this.deptOptions = res.data || []
      })
    },
    loadAllCategoryTree() {
      treeCategory().then(res => {
        this.allCategoryTree = this.transformCategoryTree(res.data || [])
      })
    },
    filterCategoryTree(deptId) {
      if (!deptId) {
        this.categoryTreeOptions = this.allCategoryTree
        return
      }
      const deptIds = this.getAllChildDeptIds(deptId, this.deptOptions)
      deptIds.unshift(deptId)
      this.categoryTreeOptions = this.filterTreeByDeptIds(this.allCategoryTree, deptIds)
    },
    getAllChildDeptIds(deptId, nodes) {
      const ids = []
      for (const node of nodes || []) {
        if (String(node.id) === String(deptId)) {
          this.collectDeptIds(node.children || [], ids)
          return ids
        }
        if (node.children && node.children.length) {
          const childIds = this.getAllChildDeptIds(deptId, node.children)
          if (childIds.length > 0) {
            return childIds
          }
        }
      }
      return ids
    },
    collectDeptIds(nodes, ids) {
      for (const node of nodes || []) {
        ids.push(node.id)
        if (node.children && node.children.length) {
          this.collectDeptIds(node.children, ids)
        }
      }
    },
    filterTreeByDeptIds(tree, deptIds) {
      const result = []
      for (const node of tree) {
        if (deptIds.includes(node.deptId)) {
          const newNode = { ...node }
          if (node.children && node.children.length) {
            newNode.children = this.filterTreeByDeptIds(node.children, deptIds)
          }
          result.push(newNode)
        }
      }
      return result
    },
    transformCategoryTree(list) {
      return list.map(item => ({
        id: item.categoryId,
        label: item.categoryName,
        deptId: item.deptId,
        children: item.children && item.children.length ? this.transformCategoryTree(item.children) : undefined
      }))
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    getList() {
      this.loading = true
      listDocument(
        this.queryParams.deptId, 
        this.queryParams.categoryId,
        this.queryParams.pageNum,
        this.queryParams.pageSize
      ).then(res => {
        this.docList = res.rows || []
        this.total = res.total || 0
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    resetQuery() {
      this.queryParams = { 
        pageNum: 1, 
        pageSize: 10,
        deptId: undefined, 
        categoryId: undefined 
      }
      this.categoryTreeOptions = this.allCategoryTree
      this.handleQuery()
    },
    openUpload() {
      this.uploadForm = { 
        docType: 'public', 
        visibleDeptIds: [], 
        categoryId: undefined, 
        fileUrl: '' 
      }
      this.filterCategoryTree(this.queryParams.deptId)
      this.uploadVisible = true
    },
    handleDeptSelect(node) {
      if (node && node.children && node.children.length > 0) {
        const allChildIds = []
        this.collectDeptIds(node.children || [], allChildIds)
        const allIds = [node.id, ...allChildIds]
        const newIds = new Set(this.uploadForm.visibleDeptIds)
        allIds.forEach(id => newIds.add(id))
        this.uploadForm.visibleDeptIds = Array.from(newIds)
      }
    },
    handleDeptDeselect(node) {
      if (node && node.children && node.children.length > 0) {
        const allChildIds = []
        this.collectDeptIds(node.children || [], allChildIds)
        const allIds = [node.id, ...allChildIds]
        this.uploadForm.visibleDeptIds = this.uploadForm.visibleDeptIds.filter(id => !allIds.includes(id))
      }
    },
    resetUpload() {
      this.$refs.uploadForm && this.$refs.uploadForm.resetFields()
    },
    submitUpload() {
      this.$refs.uploadForm.validate(valid => {
        if (!valid) return
        if (!this.uploadForm.fileUrl) {
          this.$message.warning('请上传文件')
          return
        }
        // 部门文档必须选择可见部门
        if (this.uploadForm.docType === 'dept' && (!this.uploadForm.visibleDeptIds || this.uploadForm.visibleDeptIds.length === 0)) {
          this.$message.warning('请选择可见部门')
          return
        }
        this.uploading = true
        const fileUrl = this.uploadForm.fileUrl
        const fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1)
        fetch(fileUrl)
          .then(res => res.blob())
          .then(blob => {
            const file = new File([blob], fileName, { type: blob.type })
            const fd = new FormData()
            fd.append('file', file)
            fd.append('fileUrl', fileUrl)
            // 只有部门文档才传 deptId 和 visibleDeptIds
            if (this.uploadForm.docType === 'dept') {
              // 使用第一个可见部门作为 deptId
              fd.append('deptId', this.uploadForm.visibleDeptIds[0])
              this.uploadForm.visibleDeptIds.forEach(id => {
                fd.append('visibleDeptIds', id)
              })
            }
            fd.append('categoryId', this.uploadForm.categoryId)
            return uploadDocument(fd)
          })
          .then(() => {
            this.$message.success('上传成功，正在解析入库')
            this.uploadVisible = false
            this.getList()
          })
          .catch(() => {
            this.$message.error('上传失败')
          })
          .finally(() => { this.uploading = false })
      })
    },
    handleDetail(row) {
      getDocument(row.docId).then(res => {
        this.detail = res.data || {}
        this.detail.deptName = this.getDeptName(this.detail.deptId)
        this.detail.categoryName = this.getCategoryName(this.detail.categoryId)
        this.detailVisible = true
      })
    },
    getDeptName(deptId) {
      if (!deptId) return ''
      return this.findLabel(this.deptOptions, deptId) || ''
    },
    findLabel(nodes, id) {
      for (const node of nodes || []) {
        if (String(node.id) === String(id)) return node.label
        if (node.children && node.children.length) {
          const label = this.findLabel(node.children, id)
          if (label) return label
        }
      }
      return ''
    },
    getCategoryName(categoryId) {
      if (!categoryId) return ''
      return this.findCategoryLabel(this.allCategoryTree, categoryId) || ''
    },
    findCategoryLabel(nodes, id) {
      for (const node of nodes || []) {
        if (String(node.id) === String(id)) return node.label
        if (node.children && node.children.length) {
          const label = this.findCategoryLabel(node.children, id)
          if (label) return label
        }
      }
      return ''
    },
    handleReEmbed(row) {
      this.$confirm('确定对「' + row.docName + '」重新向量化？', '提示', { type: 'warning' }).then(() => {
        reEmbedDocument(row.docId).then(() => {
          this.$message.success('重新向量化已触发')
          this.getList()
        })
      })
    },
    handleDelete(row) {
      this.$confirm('确定删除「' + row.docName + '」？同时删除所有分块数据。', '警告', { type: 'warning' }).then(() => {
        delDocument(row.docId).then(() => {
          this.$message.success('删除成功')
          this.getList()
        })
      })
    },
    formatSize(bytes) {
      if (!bytes) return '-'
      if (bytes < 1024) return bytes + ' B'
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
      return (bytes / 1024 / 1024).toFixed(2) + ' MB'
    },
    statusLabel(status) {
      return { 0: '待解析', 1: '解析中', 2: '已入库', 3: '失败' }[status] || '-'
    },
    statusType(status) {
      return { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }[status] || 'info'
    },
    formatVisibleDepts(visibleDeptIds) {
      if (!visibleDeptIds) return '-'
      const ids = visibleDeptIds.split(',').map(id => parseInt(id.trim()))
      const names = ids.map(id => this.getDeptName(id)).filter(name => name)
      return names.length > 0 ? names.join('、') : '-'
    },
    handleDocTypeChange() {
      if (this.uploadForm.docType === 'public') {
        this.uploadForm.visibleDeptIds = []
      }
      this.$nextTick(() => {
        this.$refs.uploadForm && this.$refs.uploadForm.clearValidate('visibleDeptIds')
      })
    }
  }
}
</script>
