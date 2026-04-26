<template>
  <div class="app-container">
    <el-card>
      <div slot="header" class="card-header">
        <span>消息中心</span>
        <div class="header-actions">
          <el-button type="text" size="mini" @click="markAllRead">全部标为已读</el-button>
        </div>
      </div>

      <el-row :gutter="20" class="stat-row">
        <el-col :span="6">
          <div class="stat-card total">
            <div class="stat-icon"><i class="el-icon-message"></i></div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.total }}</div>
              <div class="stat-label">全部消息</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card unread">
            <div class="stat-icon"><i class="el-icon-bell"></i></div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.unread }}</div>
              <div class="stat-label">未读消息</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card task">
            <div class="stat-icon"><i class="el-icon-s-claim"></i></div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.pending }}</div>
              <div class="stat-label">待审批</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card timeout">
            <div class="stat-icon"><i class="el-icon-warning"></i></div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.timeout }}</div>
              <div class="stat-label">超时预警</div>
            </div>
          </div>
        </el-col>
      </el-row>

      <el-divider />

      <div class="filter-bar">
        <el-radio-group v-model="query.status" size="mini" @change="getList">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="0">未读</el-radio-button>
          <el-radio-button label="1">已读</el-radio-button>
        </el-radio-group>
        <el-select v-model="query.messageType" placeholder="消息类型" size="mini" clearable @change="getList">
          <el-option label="待审批" value="approval" />
          <el-option label="超时预警" value="timeout" />
          <el-option label="流程完成" value="complete" />
          <el-option label="流程抄送" value="cc" />
        </el-select>
      </div>

      <div class="message-list">
        <div
          v-for="item in list"
          :key="item.messageId"
          class="message-item"
          :class="{ unread: item.status === '0' }"
          @click="handleRead(item)"
        >
          <div class="message-icon">
            <i v-if="item.messageType === 'timeout'" class="el-icon-warning" style="color: #f56c6c;"></i>
            <i v-else-if="item.messageType === 'approval'" class="el-icon-s-claim" style="color: #409eff;"></i>
            <i v-else class="el-icon-message" style="color: #67c23a;"></i>
          </div>
          <div class="message-content">
            <div class="message-title">
              {{ item.title }}
              <el-tag v-if="item.priority === 3" type="danger" size="mini">紧急</el-tag>
              <el-tag v-else-if="item.priority === 2" type="warning" size="mini">重要</el-tag>
            </div>
            <div class="message-desc">{{ item.content }}</div>
            <div class="message-time">{{ formatTime(item.sendTime) }}</div>
          </div>
          <div v-if="item.status === '0'" class="unread-dot"></div>
        </div>

        <div v-if="list.length === 0 && !loading" class="empty-state">
          <i class="el-icon-tickets"></i>
          <p>暂无消息</p>
        </div>
      </div>

      <pagination v-show="total > 0" :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="getList" />
    </el-card>
  </div>
</template>

<script>
import { getMessageList, getUnreadMessageCount, markMessageAsRead } from '@/api/system/message'

export default {
  name: 'OaReminder',
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      query: {
        pageNum: 1,
        pageSize: 20,
        status: '',
        messageType: ''
      },
      stats: {
        total: 0,
        unread: 0,
        pending: 0,
        timeout: 0
      }
    }
  },
  created() {
    this.getList()
    this.loadStats()
  },
  methods: {
    getList() {
      this.loading = true
      getMessageList(this.query).then(res => {
        this.list = res.rows || []
        this.total = res.total || 0
      }).finally(() => {
        this.loading = false
      })
    },
    loadStats() {
      getUnreadMessageCount().then(res => {
        this.stats.unread = res.data.count
      })
    },
    handleRead(item) {
      if (item.status === '0') {
        markMessageAsRead(item.messageId).then(() => {
          item.status = '1'
          this.stats.unread = Math.max(0, this.stats.unread - 1)
        })
      }
    },
    markAllRead() {
      this.$confirm('确定将所有消息标为已读吗？', '提示').then(() => {
        const unreadItems = this.list.filter(item => item.status === '0')
        Promise.all(unreadItems.map(item => {
          return markMessageAsRead(item.messageId).catch(() => {})
        })).then(() => {
          this.$message.success('操作成功')
          this.getList()
          this.loadStats()
        })
      })
    },
    formatTime(time) {
      if (!time) return ''
      const date = new Date(time)
      const now = new Date()
      const diff = now - date
      const minutes = Math.floor(diff / 60000)
      const hours = Math.floor(diff / 3600000)
      const days = Math.floor(diff / 86400000)
      
      if (minutes < 1) return '刚刚'
      if (minutes < 60) return `${minutes}分钟前`
      if (hours < 24) return `${hours}小时前`
      if (days < 7) return `${days}天前`
      
      return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`
    }
  }
}
</script>

<style scoped>
.app-container {
  background: #0B1120;
  min-height: calc(100vh - 84px);
  padding: 14px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-row {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid rgba(96, 165, 250, 0.2);
}

.stat-card.total {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.8) 0%, rgba(118, 75, 162, 0.8) 100%);
  color: #fff;
  border: 1px solid rgba(102, 126, 234, 0.3);
}

.stat-card.unread {
  background: linear-gradient(135deg, rgba(240, 147, 251, 0.6) 0%, rgba(245, 87, 108, 0.6) 100%);
  color: #fff;
  border: 1px solid rgba(240, 147, 251, 0.3);
}

.stat-card.task {
  background: linear-gradient(135deg, rgba(79, 172, 254, 0.8) 0%, rgba(0, 242, 254, 0.8) 100%);
  color: #fff;
  border: 1px solid rgba(79, 172, 254, 0.3);
}

.stat-card.timeout {
  background: linear-gradient(135deg, rgba(250, 112, 154, 0.6) 0%, rgba(254, 225, 64, 0.6) 100%);
  color: #fff;
  border: 1px solid rgba(250, 112, 154, 0.3);
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-right: 16px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  opacity: 0.9;
}

.filter-bar {
  margin-bottom: 16px;
  display: flex;
  gap: 12px;
}

.message-list {
  min-height: 400px;
}

.message-item {
  display: flex;
  align-items: flex-start;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 12px;
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(96, 165, 250, 0.15);
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.message-item:hover {
  background: rgba(15, 23, 42, 0.8);
  border-color: rgba(96, 165, 250, 0.4);
}

.message-item.unread {
  background: rgba(245, 158, 11, 0.15);
  border-left: 3px solid rgba(245, 158, 11, 0.6);
}

.message-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid rgba(96, 165, 250, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  margin-right: 12px;
  flex-shrink: 0;
}

.message-content {
  flex: 1;
}

.message-title {
  font-size: 14px;
  font-weight: 600;
  color: #e2e8f0;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.message-desc {
  font-size: 13px;
  color: #94a3b8;
  line-height: 1.5;
  margin-bottom: 4px;
}

.message-time {
  font-size: 12px;
  color: #64748b;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f56c6c;
  position: absolute;
  top: 16px;
  right: 16px;
  box-shadow: 0 0 8px rgba(245, 108, 108, 0.5);
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #94a3b8;
}

.empty-state i {
  font-size: 48px;
  margin-bottom: 12px;
  display: block;
  color: #60a5fa;
}

.empty-state p {
  margin: 0;
}
</style>
