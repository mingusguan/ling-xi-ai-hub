<template>
  <div class="app-container">
    <!-- P2: 查询表单 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch">
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

    <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>

    <!-- P2: 调整记录表格 -->
    <el-table v-loading="loading" :data="adjustmentList" border>
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

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

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
    }
  }
};
</script>
