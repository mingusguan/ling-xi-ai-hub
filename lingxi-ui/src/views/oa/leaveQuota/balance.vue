<template>
  <div class="app-container">
    <!-- P1: 操作栏 -->
    <el-row :gutter="10" class="mb8">
      <el-form :inline="true">
        <el-form-item label="选择年度">
          <el-date-picker
            v-model="selectedYear"
            type="year"
            placeholder="选择年度"
            value-format="yyyy"
            style="width: 150px;"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadBalance">查询余额</el-button>
        </el-form-item>
      </el-form>
    </el-row>

    <!-- P0: 假期余额展示 -->
    <el-row :gutter="20">
      <el-col :span="8" v-for="(item, index) in balanceList" :key="index">
        <el-card class="balance-card" shadow="hover">
          <div class="balance-header">
            <i :class="getLeaveTypeIcon(item.leaveType)" style="font-size: 24px;"></i>
            <span class="balance-title">{{ item.leaveName }}</span>
            <el-tag 
              :type="item.status === '1' ? 'danger' : 'success'" 
              size="mini"
              style="margin-left: 10px;"
            >
              {{ item.status === '1' ? '停用' : '正常' }}
            </el-tag>
          </div>
          
          <!-- 未初始化提示 -->
          <div v-if="!item.initialized" class="uninitialized-tip">
            <i class="el-icon-warning" style="font-size: 48px; color: #e6a23c;"></i>
            <p>假期未初始化，请联系管理员</p>
          </div>
          
          <!-- 已初始化显示数据 -->
          <template v-else>
            <div class="balance-content">
              <div class="balance-item">
                <span class="label">总额度:</span>
                <span class="value total">{{ item.totalDays }}天</span>
              </div>
              <div class="balance-item">
                <span class="label">已使用:</span>
                <span class="value used">{{ item.usedDays }}天</span>
              </div>
              <div class="balance-item">
                <span class="label">剩余额度:</span>
                <span class="value remaining">{{ item.remainingDays }}天</span>
              </div>
              <div class="balance-item" v-if="item.carryOverDays > 0">
                <span class="label">结转天数:</span>
                <span class="value carry">{{ item.carryOverDays }}天</span>
              </div>
            </div>
            <div class="progress-bar">
              <el-progress 
                :percentage="calculatePercentage(item.usedDays, item.totalDays)" 
                :color="getProgressColor(item.usedDays, item.totalDays)"
              ></el-progress>
            </div>
          </template>
        </el-card>
      </el-col>
    </el-row>

    <!-- P0: 空状态提示 -->
    <el-empty v-if="balanceList.length === 0" description="暂无假期数据" />

    <!-- P1: 生成年度假额对话框(已移除,员工不能生成) -->
  </div>
</template>

<script>
import { getMyLeaveBalance } from "@/api/oa/leaveQuota";

export default {
  name: "LeaveBalance",
  data() {
    return {
      // P0: 当前选择的年度
      selectedYear: new Date().getFullYear().toString(),
      // P0: 假期余额列表
      balanceList: [],
      // P1: 生成对话框显示状态
      generateDialogVisible: false,
      // P1: 生成表单
      generateForm: {
        year: new Date().getFullYear()
      },
      // P1: 生成中状态
      generating: false,
      generateDialogVisible: false,
      generateForm: {
        year: new Date().getFullYear()
      }
    };
  },
  created() {
    this.loadBalance();
  },
  methods: {
    /**
     * P0: 加载假期余额
     */
    loadBalance() {
      getMyLeaveBalance(this.selectedYear).then(response => {
        if (response.data && response.data.balances) {
          this.balanceList = response.data.balances;
        }
      }).catch(() => {
        this.$modal.msgError("加载假期余额失败");
      });
    },

    /**
     * P1: 打开生成年度假额对话框
     */
    handleGenerateQuota() {
      this.generateForm.year = new Date().getFullYear() + 1; // 默认下一年
      this.generateDialogVisible = true;
    },

    /**
     * P1: 提交生成年度假额
     */
    submitGenerate() {
      this.generating = true;
      generateAnnualQuota(this.generateForm.year).then(() => {
        this.$modal.msgSuccess(this.generateForm.year + "年度假期额度生成成功");
        this.generateDialogVisible = false;
        this.loadBalance(); // 重新加载余额
      }).catch(() => {
        this.$modal.msgError("生成失败");
      }).finally(() => {
        this.generating = false;
      });
    },

    /**
     * P0: 计算进度百分比
     */
    calculatePercentage(used, total) {
      if (!total || total === 0) return 0;
      return Math.round((used / total) * 100);
    },

    /**
     * P0: 获取进度条颜色
     */
    getProgressColor(used, total) {
      const percentage = (used / total) * 100;
      if (percentage < 50) return '#67c23a'; // 绿色
      if (percentage < 80) return '#e6a23c'; // 黄色
      return '#f56c6c'; // 红色
    },

    /**
     * P0: 获取假期类型图标
     */
    getLeaveTypeIcon(leaveType) {
      const iconMap = {
        'annual': 'el-icon-sunny',
        'sick': 'el-icon-first-aid-kit',
        'personal': 'el-icon-user',
        'marriage': 'el-icon-star-off',
        'maternity': 'el-icon-baby'
      };
      return iconMap[leaveType] || 'el-icon-date';
    }
  }
};
</script>

<style scoped>
.balance-card {
  position: relative;
  margin-bottom: 20px;
}

.balance-header {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}

.balance-title {
  margin-left: 10px;
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.balance-content {
  margin-bottom: 15px;
}

.balance-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  padding: 8px 0;
}

.balance-item .label {
  color: #909399;
  font-size: 14px;
}

.balance-item .value {
  font-weight: bold;
  font-size: 14px;
}

.balance-item .value.total {
  color: #409eff;
}

.balance-item .value.used {
  color: #e6a23c;
}

.balance-item .value.remaining {
  color: #67c23a;
}

.balance-item .value.carry {
  color: #909399;
}

.progress-bar {
  margin-top: 10px;
}

.uninitialized-tip {
  text-align: center;
  padding: 40px 20px;
  color: #909399;
}

.uninitialized-tip p {
  margin-top: 15px;
  font-size: 14px;
  color: #e6a23c;
}
</style>
