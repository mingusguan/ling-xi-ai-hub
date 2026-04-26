<template>
  <div class="ai-chat-container">
    <!-- 左侧会话列表 -->
    <div class="session-sidebar">
      <div class="sidebar-header">
        <el-button type="primary" icon="el-icon-plus" @click="createNewSession" size="small">
          新对话
        </el-button>
      </div>
      <div class="session-list">
        <div
          v-for="session in sessions"
          :key="session.sessionId"
          class="session-item"
          :class="{ active: currentSessionId === session.sessionId }"
          @click="switchSession(session.sessionId)"
        >
          <div class="session-title">{{ session.title }}</div>
          <el-button
            type="text"
            icon="el-icon-delete"
            size="mini"
            @click.stop="deleteSession(session.sessionId)"
          />
        </div>
      </div>
    </div>

    <!-- 右侧聊天区域 -->
    <div class="chat-main">
      <!-- 消息列表 -->
      <div class="message-list" ref="messageList">
        <div v-if="messages.length === 0" class="empty-tip">
          <i class="el-icon-chat-dot-round"></i>
          <p>我是小灵儿助手，可以帮您：</p>
          <ul>
            <li>📚 查询知识库文档</li>
            <li>💼 查看待办任务</li>
            <li>🏖️ 查询假期余额</li>
            <li>💰 查看报销状态</li>
          </ul>
        </div>
        <div
          v-for="(msg, index) in messages"
          :key="index"
          class="message-item"
          :class="msg.role"
        >
          <div class="message-avatar">
            <i :class="msg.role === 'user' ? 'el-icon-user' : 'el-icon-s-custom'"></i>
          </div>
          <div class="message-content">
            <div class="message-text" v-html="formatMessage(msg.content)"></div>
            <div class="message-time">{{ formatTime(msg.createTime) }}</div>
          </div>
        </div>
        <div v-if="loading" class="message-item assistant">
          <div class="message-avatar">
            <i class="el-icon-s-custom"></i>
          </div>
          <div class="message-content">
            <div class="message-text">
              <i class="el-icon-loading"></i> 思考中...
            </div>
          </div>
        </div>
      </div>

      <!-- 输入框 -->
      <div class="input-area">
        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="3"
          placeholder="请输入您的问题..."
          @keyup.enter.native="sendMessage"
          :disabled="loading"
        />
        <el-button
          type="primary"
          @click="sendMessage"
          :loading="loading"
          :disabled="!inputMessage.trim()"
        >
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script>
import {
  getChatSessions,
  createChatSession,
  getSessionMessages,
  deleteChatSession,
  sendChatMessage
} from '@/api/oa/ai'

export default {
  name: 'AiChat',
  data() {
    return {
      sessions: [],
      currentSessionId: null,
      messages: [],
      inputMessage: '',
      loading: false
    }
  },
  created() {
    this.loadSessions()
  },
  methods: {
    // 加载会话列表
    async loadSessions() {
      try {
        const res = await getChatSessions()
        this.sessions = res.data || []
        if (this.sessions.length > 0 && !this.currentSessionId) {
          this.switchSession(this.sessions[0].sessionId)
        }
      } catch (error) {
        this.$modal.msgError('加载会话失败')
      }
    },

    // 创建新会话
    async createNewSession() {
      try {
        const res = await createChatSession()
        const newSession = res.data
        this.sessions.unshift(newSession)
        this.switchSession(newSession.sessionId)
      } catch (error) {
        this.$modal.msgError('创建会话失败')
      }
    },

    // 切换会话
    async switchSession(sessionId) {
      this.currentSessionId = sessionId
      try {
        const res = await getSessionMessages(sessionId)
        this.messages = res.data || []
        this.$nextTick(() => {
          this.scrollToBottom()
        })
      } catch (error) {
        this.$modal.msgError('加载消息失败')
      }
    },

    // 删除会话
    async deleteSession(sessionId) {
      this.$confirm('确定删除该会话吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteChatSession(sessionId)
          this.loadSessions()
          if (this.currentSessionId === sessionId) {
            this.currentSessionId = null
            this.messages = []
          }
          this.$modal.msgSuccess('删除成功')
        } catch (error) {
          this.$modal.msgError('删除失败')
        }
      })
    },

    // 发送消息
    async sendMessage() {
      if (!this.inputMessage.trim() || this.loading) return
      if (!this.currentSessionId) {
        await this.createNewSession()
      }

      const message = this.inputMessage.trim()
      this.inputMessage = ''
      this.loading = true

      try {
        const res = await sendChatMessage({
          sessionId: this.currentSessionId,
          message: message
        })
        
        // 添加用户消息和AI回复
        if (res.data) {
          this.messages.push(res.data.userMessage)
          this.messages.push(res.data.aiMessage)
          this.$nextTick(() => {
            this.scrollToBottom()
          })
        }
      } catch (error) {
        this.$modal.msgError('发送失败：' + (error.message || '未知错误'))
      } finally {
        this.loading = false
      }
    },

    // 滚动到底部
    scrollToBottom() {
      const container = this.$refs.messageList
      if (container) {
        container.scrollTop = container.scrollHeight
      }
    },

    // 格式化消息内容（支持Markdown）
    formatMessage(content) {
      if (!content) return ''
      // 简单的Markdown格式化处理
      let formatted = content
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        .replace(/\n/g, '<br>')
      return formatted
    },

    // 格式化时间
    formatTime(time) {
      if (!time) return ''
      return this.parseTime(time, '{y}-{m}-{d} {h}:{i}')
    }
  }
}
</script>

<style scoped lang="scss">
.ai-chat-container {
  display: flex;
  height: calc(100vh - 84px);
  background: #0B1120;
}

.session-sidebar {
  width: 260px;
  background: rgba(15, 23, 42, 0.9);
  border-right: 1px solid rgba(96, 165, 250, 0.2);
  display: flex;
  flex-direction: column;

  .sidebar-header {
    padding: 16px;
    border-bottom: 1px solid rgba(96, 165, 250, 0.2);
  }

  .session-list {
    flex: 1;
    overflow-y: auto;
    padding: 8px;

    .session-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 12px;
      margin-bottom: 4px;
      border-radius: 4px;
      cursor: pointer;
      transition: all 0.3s;
      color: #e2e8f0;

      &:hover {
        background: rgba(96, 165, 250, 0.1);
      }

      &.active {
        background: rgba(96, 165, 250, 0.2);
        color: #60a5fa;
      }

      .session-title {
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        font-size: 14px;
      }
    }
  }
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: rgba(15, 23, 42, 0.8);

  .message-list {
    flex: 1;
    overflow-y: auto;
    padding: 20px;

    .empty-tip {
      text-align: center;
      padding: 60px 20px;
      color: #94a3b8;

      i {
        font-size: 64px;
        margin-bottom: 16px;
        color: #60a5fa;
      }

      p {
        font-size: 16px;
        margin: 16px 0;
      }

      ul {
        list-style: none;
        padding: 0;
        text-align: left;
        display: inline-block;

        li {
          padding: 8px 0;
          font-size: 14px;
        }
      }
    }

    .message-item {
      display: flex;
      margin-bottom: 20px;

      &.user {
        flex-direction: row-reverse;

        .message-content {
          align-items: flex-end;
        }

        .message-text {
          background: linear-gradient(135deg, #60a5fa, #3b82f6);
          color: #fff;
        }
      }

      .message-avatar {
        width: 40px;
        height: 40px;
        border-radius: 50%;
        background: rgba(96, 165, 250, 0.3);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;
        flex-shrink: 0;
      }

      .message-content {
        max-width: 60%;
        margin: 0 12px;
        display: flex;
        flex-direction: column;

        .message-text {
          background: rgba(30, 41, 59, 0.8);
          padding: 12px 16px;
          border-radius: 8px;
          line-height: 1.6;
          word-break: break-word;
          color: #e2e8f0;
          border: 1px solid rgba(96, 165, 250, 0.2);
        }

        .message-time {
          font-size: 12px;
          color: #64748b;
          margin-top: 4px;
        }
      }
    }
  }

  .input-area {
    padding: 16px;
    border-top: 1px solid rgba(96, 165, 250, 0.2);
    display: flex;
    gap: 12px;
    align-items: flex-end;
    background: rgba(15, 23, 42, 0.9);

    ::v-deep .el-textarea__inner {
      resize: none;
      background: rgba(15, 23, 42, 0.8) !important;
      border-color: rgba(96, 165, 250, 0.3) !important;
      color: #e2e8f0;
    }

    .el-button {
      height: 40px;
      padding: 0 24px;
    }
  }
}
</style>
