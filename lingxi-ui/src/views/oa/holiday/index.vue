<template>
  <div class="app-container">
    <el-card class="box-card" style="background: rgba(15, 23, 42, 0.8); border: 1px solid rgba(59, 130, 246, 0.2);">
      <!-- 年份选择和月份导航 -->
      <div class="calendar-header">
        <div class="header-left">
          <!-- 图例说明 -->
          <div class="legend-inline">
            <span class="legend-item">
              <i class="legend-color legal-holiday"></i>
              法定休息日
            </span>
            <span class="legend-item">
              <i class="legend-color fixed-rest"></i>
              固定休息日
            </span>
            <span class="legend-item">
              <i class="legend-color workday"></i>
              工作日
            </span>
          </div>
        </div>
        <div class="calendar-header-center">
          <el-button icon="el-icon-arrow-left" circle size="small" @click="prevMonth"></el-button>
          <el-date-picker
            v-model="currentDate"
            type="month"
            placeholder="选择年月"
            format="yyyy年MM月"
            value-format="yyyy-MM-dd"
            :picker-options="pickerOptions"
            @change="loadMonthData"
            style="margin: 0 12px; width: 150px"
          ></el-date-picker>
          <el-button icon="el-icon-arrow-right" circle size="small" @click="nextMonth"></el-button>
          <el-button size="small" @click="backToToday" style="margin-left: 8px">今天</el-button>
        </div>
        <div class="header-actions">
          <el-button size="small" type="primary" @click="handleSync">从万年历同步</el-button>
          <el-button size="small" type="danger" @click="handleDeleteYear">删除本年数据</el-button>
        </div>
      </div>

      <!-- 日历组件 -->
      <el-calendar v-model="currentDate" class="holiday-calendar">
        <template slot="dateCell" slot-scope="{ date, data }">
          <div class="calendar-day" :class="getDayClass(data.day)" :data-day="data.day" @contextmenu.prevent="handleContextMenu($event, data.day)">
            <div class="day-badge" v-if="getHolidayInfo(data.day) && getHolidayInfo(data.day).holidayType === 0">
              休
            </div>
            <div class="day-badge work" v-else-if="getHolidayInfo(data.day) && getHolidayInfo(data.day).holidayType === 2">
              班
            </div>
            <div class="day-number">{{ data.day.split('-').slice(2).join('') }}</div>
            <div class="day-lunar" v-if="getHolidayInfo(data.day)">
              {{ getHolidayInfo(data.day).lunarMonth }}
            </div>
            <div class="day-holiday" v-if="getHolidayInfo(data.day) && getHolidayInfo(data.day).holidayName">
              {{ getHolidayInfo(data.day).holidayName }}
            </div>
          </div>
        </template>
      </el-calendar>

      <!-- 右键菜单 -->
      <div v-show="contextMenuVisible" class="context-menu" ref="contextMenuRef" :style="{ left: contextMenuLeft + 'px', top: contextMenuTop + 'px' }">
        <div class="menu-item" @click="setHolidayType(selectedDay, 0)" v-if="!isRestDay(selectedDay)">
          <i class="el-icon-time"></i> 设为休息日
        </div>
        <div class="menu-item" @click="setHolidayType(selectedDay, 2)" v-if="isRestDay(selectedDay)">
          <i class="el-icon-s-tools"></i> 设为工作日
        </div>
      </div>



      <!-- 详情对话框 -->
      <el-dialog title="日期详情" :visible.sync="detailDialogVisible" width="600px">
        <el-descriptions :column="2" border v-if="selectedHoliday">
          <el-descriptions-item label="日期">
            {{ parseTime(selectedHoliday.holidayDate, '{y}-{m}-{d}') }}
          </el-descriptions-item>
          <el-descriptions-item label="星期">
            {{ selectedHoliday.weekDay }}
          </el-descriptions-item>
          <el-descriptions-item label="农历">
            {{ selectedHoliday.lunarMonth }}
          </el-descriptions-item>
          <el-descriptions-item label="类型">
            <el-tag v-if="selectedHoliday.holidayType === 0" type="danger" size="small">
              法定休息日
            </el-tag>
            <el-tag v-else-if="selectedHoliday.holidayType === 1" type="info" size="small">
              固定休息日
            </el-tag>
            <el-tag v-else-if="selectedHoliday.holidayType === 2" type="success" size="small">
              工作日
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="节日" :span="2">
            {{ selectedHoliday.holidayName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="宜" :span="2">
            {{ formatYiJi(selectedHoliday.yi) }}
          </el-descriptions-item>
          <el-descriptions-item label="忌" :span="2">
            {{ formatYiJi(selectedHoliday.ji) }}
          </el-descriptions-item>
          <el-descriptions-item label="星座">
            {{ selectedHoliday.xingzuo || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="生肖">
            {{ selectedHoliday.shengxiao || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="年干支">
            {{ selectedHoliday.gzNian || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="年五行">
            {{ selectedHoliday.wxNian || '-' }}
          </el-descriptions-item>
        </el-descriptions>
        <div slot="footer" class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关 闭</el-button>
        </div>
      </el-dialog>
    </el-card>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'HolidayCalendar',
  data() {
    return {
      currentDate: new Date(),
      holidayMap: {}, // 日期 -> 节假日信息的映射
      detailDialogVisible: false,
      selectedHoliday: null,
      contextMenuVisible: false,
      contextMenuLeft: 0,
      contextMenuTop: 0,
      selectedDay: '',
      pickerOptions: {
        disabledDate(time) {
          const currentYear = new Date().getFullYear()
          return time.getFullYear() < currentYear - 2 || time.getFullYear() > currentYear + 2
        }
      }
    }
  },
  created() {
    this.loadMonthData()
    document.addEventListener('click', this.handleDocumentClick)
    // 初始化时将菜单移到 body 下，避开所有父级容器的 transform/overflow 影响
    this.$nextTick(() => {
      if (this.$refs.contextMenuRef) {
        document.body.appendChild(this.$refs.contextMenuRef);
      }
    })
  },
  beforeDestroy() {
    document.removeEventListener('click', this.handleDocumentClick)
  },
  methods: {
    /** 处理右键菜单 */
    handleContextMenu(event, day) {
      this.selectedDay = day;
      
      // 直接使用 pageX/Y，因为菜单现在在 body 下，position: fixed 会相对于视口定位
      let left = event.pageX;
      let top = event.pageY;

      this.$nextTick(() => {
        const menu = this.$refs.contextMenuRef;
        if (!menu) return;

        const menuWidth = menu.offsetWidth || 140;
        const menuHeight = menu.offsetHeight || 80;
        
        // 边界检查
        if (left + menuWidth > window.innerWidth + window.scrollX) {
          left = window.innerWidth + window.scrollX - menuWidth - 5;
        }
        if (top + menuHeight > window.innerHeight + window.scrollY) {
          top = window.innerHeight + window.scrollY - menuHeight - 5;
        }
        
        this.contextMenuLeft = left;
        this.contextMenuTop = top;
        this.contextMenuVisible = true;
      });
    },

    /** 点击 document 关闭右键菜单 */
    handleDocumentClick() {
      this.contextMenuVisible = false
    },



    /** 判断是否为休息日 */
    isRestDay(day) {
      const info = this.holidayMap[day]
      if (!info) return false
      return info.holidayType === 0 || info.holidayType === 1
    },

    /** 判断是否为周末 */
    isWeekend(day) {
      const date = new Date(day)
      const weekDay = date.getDay()
      return weekDay === 0 || weekDay === 6
    },

    /** 修改日期类型 */
    async setHolidayType(day, holidayType) {
      this.contextMenuVisible = false
      try {
        const info = this.holidayMap[day]
        
        let finalType = holidayType
        // 周六周末设置为休息日时传1（固定休息日）
        if (holidayType === 0 && this.isWeekend(day)) {
          finalType = 1
        }
        
        await request({
          url: `/oa/holiday/updateType`,
          method: 'put',
          params: {
            holidayId: info.id,
            holidayType: finalType
          }
        })
        
        this.$message.success('设置成功')
        this.loadMonthData()
      } catch (error) {
        this.$message.error(error.message || '设置失败')
      }
    },
    /** 加载当月数据 */
    async loadMonthData() {
      const year = this.currentDate.getFullYear()
      const month = this.currentDate.getMonth() + 1
      
      try {
        const response = await request({
          url: `/oa/holiday/list/${year}`,
          method: 'get'
        })
        
        // 构建日期映射表
        this.holidayMap = {}
        if (response.data && Array.isArray(response.data)) {
          response.data.forEach(item => {
            const dateKey = this.parseTime(item.holidayDate, '{y}-{m}-{d}')
            this.holidayMap[dateKey] = item
          })
        }
      } catch (error) {
        console.error('加载数据失败:', error)
      }
    },

    /** 获取日期对应的节假日信息 */
    getHolidayInfo(day) {
      return this.holidayMap[day] || null
    },

    /** 获取日期的样式类 */
    getDayClass(day) {
      const info = this.holidayMap[day]
      if (!info) {
        return 'is-workday'
      }
      
      if (info.holidayType === 0) {
        return 'is-legal-holiday'
      } else if (info.holidayType === 1) {
        return 'is-fixed-rest'
      } else if (info.holidayType === 2) {
        return 'is-workday'
      }
      return 'is-workday'
    },

    /** 上一月 */
    prevMonth() {
      const date = new Date(this.currentDate)
      date.setMonth(date.getMonth() - 1)
      this.currentDate = date
      this.loadMonthData()
    },

    /** 下一月 */
    nextMonth() {
      const date = new Date(this.currentDate)
      date.setMonth(date.getMonth() + 1)
      this.currentDate = date
      this.loadMonthData()
    },

    /** 回到今天 */
    backToToday() {
      this.currentDate = new Date()
      this.loadMonthData()
    },

    /** 格式化宜忌 */
    formatYiJi(jsonStr) {
      if (!jsonStr) return '-'
      try {
        const arr = JSON.parse(jsonStr)
        return Array.isArray(arr) ? arr.join('、') : jsonStr
      } catch (e) {
        return jsonStr
      }
    },

    /** 从万年历同步 */
    handleSync() {
      const year = this.currentDate.getFullYear()
      this.$confirm(`确定要从万年历同步${year}年的数据吗？\n注意：该操作会覆盖已有数据，且不支持重复同步。`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const loading = this.$loading({
            lock: true,
            text: `正在同步${year}年数据，请稍候...`,
            spinner: 'el-icon-loading',
            background: 'rgba(0, 0, 0, 0.7)'
          })
          
          const response = await request({
            url: `/oa/holiday/sync/${year}`,
            method: 'post'
          })
          
          loading.close()
          this.$message.success(response.msg || '同步成功')
          this.loadMonthData()
        } catch (error) {
          this.$message.error(error.message || '同步失败')
        }
      }).catch(() => {})
    },

    /** 生成本年周末 */
    handleGenerateWeekends() {
      const year = this.currentDate.getFullYear()
      this.$confirm(`确定要生成${year}年的所有周末数据吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }).then(async () => {
        try {
          const response = await request({
            url: `/oa/holiday/generate/${year}`,
            method: 'post'
          })
          
          this.$message.success(response.msg || '生成成功')
          this.loadMonthData()
        } catch (error) {
          this.$message.error(error.message || '生成失败')
        }
      }).catch(() => {})
    },

    /** 删除本年数据 */
    handleDeleteYear() {
      const year = this.currentDate.getFullYear()
      this.$confirm(`确定要删除${year}年的所有数据吗？此操作不可恢复！`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'danger'
      }).then(async () => {
        try {
          const response = await request({
            url: `/oa/holiday/deleteYear/${year}`,
            method: 'delete'
          })
          
          this.$message.success(response.msg || '删除成功')
          this.holidayMap = {}
        } catch (error) {
          this.$message.error(error.message || '删除失败')
        }
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.calendar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  padding: 6px 10px;
  background: linear-gradient(135deg, rgba(30, 58, 138, 0.6), rgba(15, 23, 42, 0.8));
  border-radius: 4px;
  position: relative;
}

.calendar-header-center {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.legend-inline {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-right: 12px;
  margin-right: 12px;
  border-right: 1px solid rgba(59, 130, 246, 0.3);
}

.header-actions {
  padding-top: 3px;
}

.app-container {
  padding-top: 1px;
}

.box-card ::v-deep .el-card__body {
  padding: 8px 15px !important;
}

.calendar-header-center {
  display: flex;
  align-items: center;
  position: absolute;
  top: 6px;
  left: 50%;
  transform: translateX(-50%);
}

.header-actions {
  display: flex;
  gap: 8px;
}

.holiday-calendar {
  background: rgba(15, 23, 42, 0.9) !important;
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 4px;
}

.holiday-calendar ::v-deep .el-calendar-table {
  background: transparent !important;
}

.holiday-calendar ::v-deep .el-calendar-table td {
  background: transparent !important;
}

.holiday-calendar ::v-deep .el-calendar-table .el-calendar-day {
  background: rgba(30, 41, 59, 0.6) !important;
}

.holiday-calendar ::v-deep .el-calendar__header {
  display: none;
}

.holiday-calendar ::v-deep .el-calendar__body {
  padding: 0px 20px 35px;
}

.holiday-calendar ::v-deep .el-calendar-table {
  height: auto;
}

.holiday-calendar ::v-deep .el-calendar-table .el-calendar-day {
  min-height: 78px;
  height: auto;
  padding: 0 !important;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid rgba(59, 130, 246, 0.15);
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.4), rgba(15, 23, 42, 0.6));
}

.holiday-calendar ::v-deep .el-calendar-table .el-calendar-day > div {
  padding: 6px 8px;
  width: 100%;
  height: 100%;
  min-height: 78px;
}

.holiday-calendar ::v-deep .el-calendar-table .el-calendar-day:hover {
  background: rgba(59, 130, 246, 0.2) !important;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

.holiday-calendar ::v-deep .el-calendar-table .el-calendar-day.is-fixed-rest:hover {
  background: rgba(99, 102, 241, 0.3) !important;
}

.holiday-calendar ::v-deep .el-calendar-table .el-calendar-day.is-fixed-rest:hover .day-number {
  color: #e0e7ff !important;
}

.holiday-calendar ::v-deep .el-calendar-table .el-calendar-day.is-fixed-rest:hover .day-lunar {
  color: #eef2ff !important;
}

.holiday-calendar ::v-deep .el-calendar-table th {
  padding: 8px 0;
  background: linear-gradient(135deg, rgba(30, 58, 138, 0.5), rgba(15, 23, 42, 0.7));
  color: #94a3b8;
  font-weight: 500;
}

.holiday-calendar ::v-deep .el-calendar-table td.is-selected .el-calendar-day {
  background: rgba(59, 130, 246, 0.3);
}

.holiday-calendar ::v-deep .el-calendar-table td.is-today .el-calendar-day {
  border: 2px solid #409eff;
}

.holiday-calendar ::v-deep .el-calendar-table td.is-today .day-number {
  color: #409eff !important;
}

.calendar-day {
  width: 100%;
  height: 100%;
  min-height: 78px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  position: relative;
  user-select: none;
}

.day-badge {
  position: absolute;
  top: 5px;
  right: 5px;
  width: 22px;
  height: 22px;
  line-height: 22px;
  text-align: center;
  font-size: 11px;
  color: #fff;
  background: linear-gradient(135deg, #f56c6c, #f05050);
  border-radius: 3px;
  font-weight: 600;
  z-index: 10;
  box-shadow: 0 2px 4px rgba(245, 108, 108, 0.3);
}

.day-badge.work {
  background: linear-gradient(135deg, #67c23a, #5daf34);
  box-shadow: 0 2px 4px rgba(103, 194, 58, 0.3);
}

.day-number {
  font-size: 18px;
  font-weight: 600;
  color: #f1f5f9;
  line-height: 1.1;
}

.day-lunar {
  font-size: 11px;
  color: #cbd5e1;
  margin-top: 2px;
}

.day-holiday {
  font-size: 10px;
  color: #f87171;
  font-weight: 500;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  background: rgba(239, 68, 68, 0.15);
  padding: 1px 4px;
  border-radius: 2px;
}

/* 法定休息日样式 */
.is-legal-holiday {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.2), rgba(239, 68, 68, 0.08), rgba(15, 23, 42, 0.6)) !important;
  box-shadow: inset 0 0 20px rgba(239, 68, 68, 0.1);
}

.is-legal-holiday .day-number {
  color: #ffffff;
  text-shadow: 0 0 8px rgba(239, 68, 68, 0.8);
}

.is-legal-holiday .day-lunar {
  color: #fef2f2;
}

.is-legal-holiday .day-holiday {
  color: #ffffff;
  background: rgba(239, 68, 68, 0.4);
}

/* 固定休息日样式 */
.is-fixed-rest {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.18), rgba(79, 70, 229, 0.08), rgba(15, 23, 42, 0.6)) !important;
  box-shadow: inset 0 0 15px rgba(99, 102, 241, 0.1);
}

.is-fixed-rest .day-number {
  color: #c7d2fe;
}

.is-fixed-rest .day-lunar {
  color: #ddd6fe;
}

.is-fixed-rest:hover .day-number {
  color: #e0e7ff;
}

.is-fixed-rest:hover .day-lunar {
  color: #eef2ff;
}

/* 工作日样式 */
.is-workday {
  background: linear-gradient(135deg, rgba(51, 65, 85, 0.3), rgba(15, 23, 42, 0.5)) !important;
}

.is-workday .day-number {
  color: #e2e8f0;
}

/* 其他月份日期弱化 */
.holiday-calendar ::v-deep .el-calendar-table td.other-month .el-calendar-day {
  background: rgba(30, 41, 59, 0.3) !important;
  opacity: 0.5;
}

.legend {
  display: flex;
  justify-content: center;
  margin-top: 8px;
  padding: 8px 15px;
  background: linear-gradient(135deg, rgba(30, 58, 138, 0.5), rgba(15, 23, 42, 0.7));
  border-radius: 8px;
  gap: 40px;
}

.legend-item {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #94a3b8;
  font-weight: 500;
}

.legend-color {
  display: inline-block;
  width: 20px;
  height: 20px;
  border-radius: 4px;
  margin-right: 10px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.legend-color.legal-holiday {
  background: rgba(239, 68, 68, 0.2);
  border: 1px solid rgba(239, 68, 68, 0.5);
}

.legend-color.fixed-rest {
  background: rgba(100, 116, 139, 0.3);
  border: 1px solid rgba(148, 163, 184, 0.5);
}

.legend-color.workday {
  background: rgba(30, 41, 59, 0.8);
  border: 1px solid rgba(59, 130, 246, 0.3);
}

.context-menu {
  position: fixed;
  background: rgba(15, 23, 42, 0.95);
  border: 1px solid rgba(59, 130, 246, 0.3);
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.4);
  padding: 6px 0;
  z-index: 9999;
  min-width: 140px;
}

.menu-item {
  padding: 8px 16px;
  cursor: pointer;
  font-size: 14px;
  color: #e2e8f0;
  transition: all 0.2s;
}

.menu-item:hover:not(.disabled) {
  background: rgba(59, 130, 246, 0.2);
  color: #60a5fa;
}

.menu-item.disabled {
  color: #64748b;
  cursor: not-allowed;
  background: rgba(30, 41, 59, 0.5);
}

.menu-item i {
  margin-right: 8px;
}

.box-card {
  margin-bottom: 1px;
}
</style>
