<template>
  <div class="app-container">
    <div class="dark-card">
      <div class="card-header">
        <span class="card-title">消息中心</span>
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
        <el-select v-model="query.sourceType" placeholder="子系统" size="mini" clearable @change="getList">
          <el-option label="系统通知" value="system" />
          <el-option label="OA系统" value="oa" />
          <el-option label="知识库" value="knowledge" />
          <el-option label="AI助手" value="ai" />
        </el-select>
        <el-select v-model="query.messageType" placeholder="消息类型" size="mini" clearable @change="getList">
          <el-option label="待审批" value="task" />
          <el-option label="超时预警" value="timeout" />
          <el-option label="流程完成" value="complete" />
          <el-option label="流程抄送" value="cc" />
          <el-option label="系统通知" value="notice" />
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
            <i v-if="item.sourceType === 'oa'" class="el-icon-s-order" style="color: #409eff;"></i>
            <i v-else-if="item.sourceType === 'system'" class="el-icon-message-solid" style="color: #67c23a;"></i>
            <i v-else-if="item.sourceType === 'knowledge'" class="el-icon-notebook-2" style="color: #e6a23c;"></i>
            <i v-else-if="item.sourceType === 'ai'" class="el-icon-chat-line-round" style="color: #f56c6c;"></i>
            <i v-else class="el-icon-message" style="color: #909399;"></i>
          </div>
          <div class="message-content">
            <div class="message-title">
              {{ item.title }}
              <el-tag v-if="item.messageType === 'timeout'" type="danger" size="mini">超时预警</el-tag>
              <el-tag v-else-if="item.messageType === 'task'" type="primary" size="mini">待审批</el-tag>
              <el-tag v-else-if="item.messageType === 'complete'" type="success" size="mini">流程完成</el-tag>
              <el-tag v-else-if="item.messageType === 'cc'" type="info" size="mini">抄送</el-tag>
              <el-tag v-else-if="item.messageType === 'notice'" size="mini">通知</el-tag>
              <el-tag v-if="item.priority === 3" type="danger" size="mini" effect="plain">紧急</el-tag>
              <el-tag v-else-if="item.priority === 2" type="warning" size="mini" effect="plain">重要</el-tag>
            </div>
            <div class="message-desc">{{ item.content }}</div>
            <div class="message-meta">
              <span class="message-source">
                <span v-if="item.sourceType === 'oa'"><i class="el-icon-s-order"></i> OA系统</span>
                <span v-else-if="item.sourceType === 'system'"><i class="el-icon-message-solid"></i> 系统通知</span>
                <span v-else-if="item.sourceType === 'knowledge'"><i class="el-icon-notebook-2"></i> 知识库</span>
                <span v-else-if="item.sourceType === 'ai'"><i class="el-icon-chat-line-round"></i> AI助手</span>
                <span v-else><i class="el-icon-message"></i> 其他</span>
              </span>
              <span class="message-time">{{ formatTime(item.sendTime) }}</span>
            </div>
          </div>
          <div v-if="item.status === '0'" class="unread-dot"></div>
        </div>

        <div v-if="list.length === 0 && !loading" class="empty-state">
          <i class="el-icon-tickets"></i>
          <p>暂无消息</p>
        </div>
      </div>

      <pagination v-show="total > 0" :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="getList" />
    </div>
  </div>
</template>

<script>
import { getMessageList, getUnreadMessageCount, markMessageAsRead } from '@/api/system/message'

export default {
  name: 'MessageCenter',
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      query: {
        pageNum: 1,
        pageSize: 20,
        status: '',
        sourceType: '',
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
        // 更新统计数据
        this.stats.total = res.total || 0
        // 统计待审批和超时预警数量
        this.stats.pending = (this.list || []).filter(item => item.messageType === 'task' && item.status === '0').length
        this.stats.timeout = (this.list || []).filter(item => item.messageType === 'timeout' && item.status === '0').length
      }).catch(err => {
        console.error('获取消息列表失败:', err)
        this.list = []
        this.total = 0
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
  background: transparent !important;
}

.dark-card {
  background: rgba(15, 23, 42, 0.7);
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 12px;
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(59, 130, 246, 0.15);
}

.card-title {
  color: #E2E8F0;
  font-size: 18px;
  font-weight: 600;
}

.card-header .el-button {
  color: #60A5FA;
}

.stat-row {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  border-radius: 10px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.15) 0%, rgba(139, 92, 246, 0.1) 100%);
  border: 1px solid rgba(59, 130, 246, 0.15);
  transition: all 0.3s ease;
}

.stat-card:hover {
  border-color: rgba(59, 130, 246, 0.35);
  transform: translateY(-2px);
}

.stat-card.total {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.3) 0%, rgba(118, 75, 162, 0.25) 100%);
  border-color: rgba(102, 126, 234, 0.35);
  color: #fff;
}

.stat-card.unread {
  background: linear-gradient(135deg, rgba(240, 147, 251, 0.3) 0%, rgba(245, 87, 108, 0.25) 100%);
  border-color: rgba(240, 147, 251, 0.35);
  color: #fff;
}

.stat-card.task {
  background: linear-gradient(135deg, rgba(79, 172, 254, 0.3) 0%, rgba(0, 242, 254, 0.25) 100%);
  border-color: rgba(79, 172, 254, 0.35);
  color: #fff;
}

.stat-card.timeout {
  background: linear-gradient(135deg, rgba(250, 112, 154, 0.3) 0%, rgba(254, 225, 64, 0.25) 100%);
  border-color: rgba(250, 112, 154, 0.35);
  color: #fff;
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
  flex-wrap: wrap;
  gap: 12px;
  padding: 12px;
  background: rgba(30, 41, 59, 0.6);
  border: 1px solid rgba(59, 130, 246, 0.15);
  border-radius: 8px;
}

.filter-bar .el-radio-group,
.filter-bar .el-select {
  margin-right: 0;
}

.message-list {
  min-height: 400px;
}

.message-item {
  display: flex;
  align-items: flex-start;
  padding: 16px;
  border-radius: 10px;
  margin-bottom: 12px;
  background: rgba(30, 41, 59, 0.6);
  border: 1px solid rgba(59, 130, 246, 0.15);
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.message-item:hover {
  background: rgba(59, 130, 246, 0.1);
  border-color: rgba(59, 130, 246, 0.35);
  transform: translateX(4px);
}

.message-item.unread {
  background: rgba(230, 162, 60, 0.15);
  border-left: 3px solid #e6a23c;
  border-top: 1px solid rgba(230, 162, 60, 0.3);
  border-right: 1px solid rgba(230, 162, 60, 0.1);
  border-bottom: 1px solid rgba(230, 162, 60, 0.1);
}

.message-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid rgba(59, 130, 246, 0.2);
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
  color: #E2E8F0;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
}

.message-title .el-tag {
  margin-left: 8px;
}

.message-desc {
  font-size: 13px;
  color: #94A3B8;
  line-height: 1.5;
  margin-bottom: 4px;
}

.message-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #64748B;
}

.message-source {
  display: flex;
  align-items: center;
}

.message-source i {
  font-size: 14px;
  margin-right: 4px;
}

.message-time {
  font-size: 12px;
  color: #64748B;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f56c6c;
  box-shadow: 0 0 10px rgba(245, 108, 108, 0.5);
  position: absolute;
  top: 18px;
  right: 18px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #64748B;
}

.empty-state i {
  font-size: 48px;
  color: #60A5FA;
  margin-bottom: 12px;
  display: block;
  opacity: 0.6;
}

.empty-state p {
  margin: 0;
  font-size: 14px;
}
</style>
