<template>
  <div class="app-container">
    <!-- 查询表单 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="用户姓名" prop="userName">
        <el-input
          v-model="queryParams.userName"
          placeholder="请输入用户姓名"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="年度" prop="year">
        <el-date-picker
          v-model="queryParams.year"
          type="year"
          placeholder="选择年度"
          value-format="yyyy"
          style="width: 150px;"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="正常" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-refresh"
          size="mini"
          @click="handleGenerateQuota"
          v-hasPermi="['oa:leave:generate']"
        >重置年度假额</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row> -->

    <!-- 额度列表表格 -->
    <el-table v-loading="loading" :data="quotaList" border>
      <el-table-column label="额度ID" align="center" prop="quotaId" width="80" />
      <el-table-column label="用户姓名" align="center" prop="userName" width="120" />
      <el-table-column label="假期类型" align="center" prop="leaveType" width="100">
        <template slot-scope="scope">
          {{ getLeaveTypeName(scope.row.leaveType) }}
        </template>
      </el-table-column>
      <el-table-column label="年度" align="center" prop="year" width="80" />
      <el-table-column label="总额度" align="center" prop="totalDays" width="100">
        <template slot-scope="scope">
          <span style="color: #409eff;">{{ scope.row.totalDays }}天</span>
        </template>
      </el-table-column>
      <el-table-column label="已使用" align="center" prop="usedDays" width="100">
        <template slot-scope="scope">
          <span style="color: #e6a23c;">{{ scope.row.usedDays }}天</span>
        </template>
      </el-table-column>
      <el-table-column label="剩余额度" align="center" prop="remainingDays" width="100">
        <template slot-scope="scope">
          <span style="color: #67c23a;">{{ scope.row.remainingDays }}天</span>
        </template>
      </el-table-column>
      <el-table-column label="结转天数" align="center" prop="carryOverDays" width="100" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === '1' ? 'danger' : 'success'" size="small">
            {{ scope.row.status === '1' ? '停用' : '正常' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleAdjust(scope.row)"
            v-hasPermi="['oa:leave:adjust']"
          >调整</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 生成年度假额对话框 -->
    <el-dialog title="生成年度假额" :visible.sync="generateDialogVisible" width="500px">
      <el-form ref="generateForm" :model="generateForm" label-width="100px">
        <el-form-item label="年度" required>
          <el-input-number 
            v-model="generateForm.year" 
            :min="2020" 
            :max="2030"
            style="width: 100%;"
          />
        </el-form-item>
        <el-alert
          title="注意: 此操作将为所有员工生成指定年度的假期额度"
          type="warning"
          :closable="false"
          show-icon
        />
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="generateDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitGenerate" :loading="generating">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 调整额度对话框 -->
    <el-dialog title="调整假期额度" :visible.sync="adjustDialogVisible" width="500px">
      <el-form ref="adjustForm" :model="adjustForm" :rules="adjustRules" label-width="100px">
        <el-form-item label="用户ID">
          <el-input v-model="adjustForm.userId" disabled />
        </el-form-item>
        <el-form-item label="假期类型">
          <el-input v-model="adjustForm.leaveName" disabled />
        </el-form-item>
        <el-form-item label="当前额度">
          <el-input v-model="adjustForm.totalDays" disabled />
        </el-form-item>
        <el-form-item label="调整天数" prop="adjustDays">
          <el-input-number 
            v-model="adjustForm.adjustDays" 
            :precision="1"
            :step="0.5"
            style="width: 100%;"
          />
          <div style="margin-top: 5px; color: #909399; font-size: 12px;">
            正数增加额度，负数减少额度
          </div>
        </el-form-item>
        <el-form-item label="调整原因" prop="reason">
          <el-input 
            v-model="adjustForm.reason" 
            type="textarea" 
            :rows="3"
            placeholder="请输入调整原因"
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="adjustDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitAdjust" :loading="adjusting">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listLeaveQuota, generateAnnualQuota, adjustLeaveQuota, updateLeaveQuotaStatus } from "@/api/oa/leaveQuota";

export default {
  name: "LeaveQuotaManage",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 额度表格数据
      quotaList: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userName: undefined,
        year: new Date().getFullYear().toString(),
        status: undefined
      },
      // 生成对话框
      generateDialogVisible: false,
      generateForm: {
        year: new Date().getFullYear() + 1
      },
      generating: false,
      // 调整对话框
      adjustDialogVisible: false,
      adjustForm: {
        userId: null,
        leaveType: '',
        leaveName: '',
        year: null,
        totalDays: 0,
        adjustDays: 0,
        reason: ''
      },
      adjustRules: {
        adjustDays: [
          { required: true, message: "调整天数不能为空", trigger: "blur" }
        ],
        reason: [
          { required: true, message: "调整原因不能为空", trigger: "blur" }
        ]
      },
      adjusting: false
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /**
     * 查询列表
     */
    getList() {
      this.loading = true;
      listLeaveQuota(this.queryParams).then(response => {
        this.quotaList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    /**
     * 搜索按钮操作
     */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /**
     * 重置按钮操作
     */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    /**
     * 获取假期类型名称
     */
    getLeaveTypeName(leaveType) {
      const typeMap = {
        'annual': '年假',
        'sick': '病假',
        'personal': '事假',
        'marriage': '婚假',
        'maternity': '产假'
      };
      return typeMap[leaveType] || leaveType;
    },
    /**
     * 生成年度假额
     */
    handleGenerateQuota() {
      this.generateDialogVisible = true;
    },
    submitGenerate() {
      this.generating = true;
      generateAnnualQuota(this.generateForm.year).then(() => {
        this.$modal.msgSuccess(this.generateForm.year + "年度假期额度生成成功");
        this.generateDialogVisible = false;
        this.getList();
      }).catch(() => {
        this.$modal.msgError("生成失败");
      }).finally(() => {
        this.generating = false;
      });
    },
    /**
     * 调整额度
     */
    handleAdjust(row) {
      this.adjustForm = {
        userId: row.userId,
        leaveType: row.leaveType,
        leaveName: this.getLeaveTypeName(row.leaveType),
        year: row.year,
        totalDays: row.totalDays,
        adjustDays: 0,
        reason: ''
      };
      this.adjustDialogVisible = true;
    },
    submitAdjust() {
      this.$refs["adjustForm"].validate(valid => {
        if (valid) {
          if (this.adjustForm.adjustDays === 0) {
            this.$modal.msgWarning("调整天数不能为0");
            return;
          }
          
          this.adjusting = true;
          adjustLeaveQuota(this.adjustForm).then(() => {
            this.$modal.msgSuccess("调整成功");
            this.adjustDialogVisible = false;
            this.getList();
          }).catch(() => {
            this.$modal.msgError("调整失败");
          }).finally(() => {
            this.adjusting = false;
          });
        }
      });
    },
    /**
     * 修改额度状态
     */
    handleStatusChange(row) {
      let text = row.status === "0" ? "启用" : "停用";
      let leaveName = this.getLeaveTypeName(row.leaveType);
      this.$modal.confirm('确认要"' + text + '""' + leaveName + '"额度吗？').then(() => {
        return updateLeaveQuotaStatus(row);
      }).then(() => {
        this.$modal.msgSuccess(text + "成功");
      }).catch(() => {
        row.status = row.status === "0" ? "1" : "0";
      });
    }
  }
};
</script>
