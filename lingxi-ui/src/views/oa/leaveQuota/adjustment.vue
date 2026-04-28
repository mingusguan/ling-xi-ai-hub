<template>
  <div class="app-container">
    <!-- P2: 查询表单 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" style="margin-top: 15px; margin-bottom: 0px; margin-left: 10px;">
      <el-form-item label="用户姓名" prop="userName">
        <el-input
          v-model="queryParams.userName"
          placeholder="请输入用户姓名"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="假期类型" prop="leaveType">
        <el-select v-model="queryParams.leaveType" placeholder="请选择假期类型" clearable>
          <el-option label="年假" value="annual" />
          <el-option label="病假" value="sick" />
          <el-option label="事假" value="personal" />
          <el-option label="婚假" value="marriage" />
          <el-option label="产假" value="maternity" />
        </el-select>
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
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <right-toolbar :showSearch.sync="showSearch" :search="false" :refresh="false" @queryTable="getList"></right-toolbar>

    <!-- 表格容器 -->
    <div style="border: 1px solid rgba(59, 130, 246, 0.2); border-bottom: none; border-radius: 4px 4px 0 0; overflow: hidden;">
      <!-- P2: 调整记录表格 -->
      <el-table v-loading="loading" :data="adjustmentList" border style="min-width: 100%;">
        <el-table-column label="调整ID" align="center" prop="adjustmentId" width="80" />
        <el-table-column label="用户姓名" align="center" prop="userName" width="120" />
        <el-table-column label="假期类型" align="center" prop="leaveType" width="100">
          <template slot-scope="scope">
            {{ getLeaveTypeName(scope.row.leaveType) }}
          </template>
        </el-table-column>
        <el-table-column label="年度" align="center" prop="year" width="80" />
        <el-table-column label="调整天数" align="center" prop="adjustDays" width="100">
          <template slot-scope="scope">
            <span :style="{ color: scope.row.adjustDays > 0 ? '#67c23a' : '#f56c6c' }">
              {{ scope.row.adjustDays > 0 ? '+' : '' }}{{ scope.row.adjustDays }}天
            </span>
          </template>
        </el-table-column>
        <el-table-column label="调整前额度" align="center" prop="beforeDays" width="100" />
        <el-table-column label="调整后额度" align="center" prop="afterDays" width="100" />
        <el-table-column label="操作人" align="center" prop="operatorName" width="120" />
        <el-table-column label="调整时间" align="center" prop="createTime" width="160">
          <template slot-scope="scope">
            <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="调整原因" align="center" prop="reason" show-overflow-tooltip min-width="200" />
      </el-table>
    </div>

    <!-- 分页组件 -->
    <el-pagination
      v-show="total>0"
      background
      :current-page.sync="queryParams.pageNum"
      :page-size.sync="queryParams.pageSize"
      :layout="'total, sizes, prev, pager, next, jumper'"
      :page-sizes="[10, 20, 30, 50]"
      :total="total"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      class="custom-pagination"
    />
  </div>
</template>

<style scoped>
.custom-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 10px 16px;
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-top: none;
  border-radius: 0 0 4px 4px;
  margin: -1px 0 0 0;
  background: transparent;
}
</style>

<script>
import { listLeaveAdjustment } from "@/api/oa/leaveQuota";

export default {
  name: "LeaveAdjustment",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 调整记录表格数据
      adjustmentList: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userName: undefined,
        leaveType: undefined,
        year: undefined
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /**
     * P2: 查询调整记录列表
     */
    getList() {
      this.loading = true;
      listLeaveAdjustment(this.queryParams).then(response => {
        this.adjustmentList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    /**
     * P2: 搜索按钮操作
     */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /**
     * P2: 重置按钮操作
     */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    /**
     * P2: 获取假期类型名称
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
    // 分页大小变化
    handleSizeChange(val) {
      if (this.queryParams.pageNum * val > this.total) {
        this.queryParams.pageNum = 1;
      }
      this.queryParams.pageSize = val;
      this.getList();
    },
    // 分页页码变化
    handleCurrentChange(val) {
      this.queryParams.pageNum = val;
      this.getList();
    }
  }
};
</script>
