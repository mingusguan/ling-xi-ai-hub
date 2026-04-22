<template>
  <div class="app-container">
    <!-- P1: 查询表单 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch">
      <el-form-item label="假期类型" prop="leaveType">
        <el-input
          v-model="queryParams.leaveType"
          placeholder="请输入假期类型编码"
          clearable
          @keyup.enter.native="handleQuery"
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

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['oa:leave:add']"
        >新增</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- P1: 规则列表表格 -->
    <el-table v-loading="loading" :data="ruleList" border>
      <el-table-column label="规则ID" align="center" prop="ruleId" width="80" />
      <el-table-column label="假期类型编码" align="center" prop="leaveType" width="120" />
      <el-table-column label="假期类型名称" align="center" prop="leaveName" width="120" />
      <el-table-column label="默认天数" align="center" prop="defaultDays" width="100" />
      <el-table-column label="计算规则" align="center" prop="calculationRule" width="120">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.oa_calculation_rule" :value="scope.row.calculationRule"/>
        </template>
      </el-table-column>
      <el-table-column label="可结转" align="center" prop="canCarryOver" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.canCarryOver === '1' ? 'success' : 'info'">
            {{ scope.row.canCarryOver === '1' ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="结转比例" align="center" prop="carryOverRatio" width="100">
        <template slot-scope="scope">
          {{ scope.row.carryOverRatio ? (scope.row.carryOverRatio * 100).toFixed(0) + '%' : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="最大结转天数" align="center" prop="maxCarryOverDays" width="120" />
      <el-table-column label="显示顺序" align="center" prop="sortOrder" width="100" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template slot-scope="scope">
          <el-switch
            v-model="scope.row.status"
            active-value="0"
            inactive-value="1"
            @change="handleStatusChange(scope.row)"
          ></el-switch>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" show-overflow-tooltip />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['oa:leave:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['oa:leave:remove']"
          >删除</el-button>
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

    <!-- P1: 添加或修改对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="假期类型编码" prop="leaveType">
              <el-input v-model="form.leaveType" placeholder="如: annual, sick" :disabled="form.ruleId != null" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="假期类型名称" prop="leaveName">
              <el-input v-model="form.leaveName" placeholder="如: 年假, 病假" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="默认天数" prop="defaultDays">
              <el-input-number v-model="form.defaultDays" :min="0" :precision="1" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计算规则" prop="calculationRule">
              <el-select v-model="form.calculationRule" placeholder="请选择" style="width: 100%;">
                <el-option label="固定天数" value="fixed" />
                <el-option label="按工龄计算" value="by_seniority" />
                <el-option label="按职级计算" value="by_level" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="是否可结转" prop="canCarryOver">
              <el-radio-group v-model="form.canCarryOver">
                <el-radio label="1">是</el-radio>
                <el-radio label="0">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结转比例" prop="carryOverRatio" v-if="form.canCarryOver === '1'">
              <el-input-number 
                v-model="form.carryOverRatio" 
                :min="0" 
                :max="1" 
                :step="0.1"
                :precision="2"
                style="width: 100%;"
              />
              <span style="margin-left: 10px; color: #909399;">{{ (form.carryOverRatio * 100).toFixed(0) }}%</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="最大结转天数" prop="maxCarryOverDays" v-if="form.canCarryOver === '1'">
              <el-input-number v-model="form.maxCarryOverDays" :min="0" :precision="1" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示顺序" prop="sortOrder">
              <el-input-number v-model="form.sortOrder" :min="0" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio label="0">正常</el-radio>
                <el-radio label="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listLeaveRule, getLeaveRule, addLeaveRule, updateLeaveRule, delLeaveRule } from "@/api/oa/leaveQuota";

export default {
  name: "LeaveRule",
  dicts: ['oa_calculation_rule'],
  data() {
    return {
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 规则表格数据
      ruleList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        leaveType: undefined,
        status: undefined
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        leaveType: [
          { required: true, message: "假期类型编码不能为空", trigger: "blur" }
        ],
        leaveName: [
          { required: true, message: "假期类型名称不能为空", trigger: "blur" }
        ],
        defaultDays: [
          { required: true, message: "默认天数不能为空", trigger: "blur" }
        ]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /**
     * P1: 查询规则列表
     */
    getList() {
      this.loading = true;
      listLeaveRule(this.queryParams).then(response => {
        this.ruleList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    /**
     * P1: 搜索按钮操作
     */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /**
     * P1: 重置按钮操作
     */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    /**
     * P1: 新增按钮操作
     */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加假期规则";
    },
    /**
     * P1: 修改按钮操作
     */
    handleUpdate(row) {
      this.reset();
      const ruleId = row.ruleId || this.ids[0];
      getLeaveRule(ruleId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改假期规则";
      });
    },
    /**
     * P1: 提交表单
     */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.ruleId != null) {
            updateLeaveRule(this.form).then(() => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addLeaveRule(this.form).then(() => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /**
     * P1: 删除按钮操作
     */
    handleDelete(row) {
      this.$modal.confirm('是否确认删除假期规则"' + row.leaveName + '"？').then(() => {
        return delLeaveRule(row.ruleId);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /**
     * P1: 状态修改
     */
    handleStatusChange(row) {
      let text = row.status === "0" ? "启用" : "停用";
      this.$modal.confirm('确认要"' + text + '""' + row.leaveName + '"规则吗？').then(() => {
        return updateLeaveRule(row);
      }).then(() => {
        this.$modal.msgSuccess(text + "成功");
      }).catch(() => {
        row.status = row.status === "0" ? "1" : "0";
      });
    },
    /**
     * P1: 取消按钮
     */
    cancel() {
      this.open = false;
      this.reset();
    },
    /**
     * P1: 表单重置
     */
    reset() {
      this.form = {
        ruleId: undefined,
        leaveType: undefined,
        leaveName: undefined,
        defaultDays: 0,
        calculationRule: 'fixed',
        canCarryOver: '0',
        carryOverRatio: 0,
        maxCarryOverDays: 0,
        sortOrder: 0,
        status: "0",
        remark: undefined
      };
      this.resetForm("form");
    }
  }
};
</script>
