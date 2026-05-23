<template>
  <div class="qa-page">
    <div class="sidebar">
      <div class="sidebar-header">
        <el-button type="primary" class="new-chat-btn" @click="createNewSession">
          <i class="el-icon-plus" /> 新建对话
        </el-button>
      </div>
      <div class="session-list">
        <div
          v-for="session in sessionList"
          :key="session.sessionId"
          class="session-item"
          :class="{ active: currentSessionId === session.sessionId }"
          @click="selectSession(session)"
        >
          <i class="el-icon-chat-dot-round session-icon" />
          <div class="session-info">
            <div class="session-title">{{ session.title }}</div>
            <div class="session-meta">{{ session.messageCount || 0 }} 条消息</div>
          </div>
          <el-dropdown trigger="click" @command="handleSessionCommand">
            <i class="el-icon-more session-more" @click.stop />
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item :command="{ action: 'delete', session }">删除</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
        <div v-if="sessionList.length === 0" class="empty-sessions">
          <i class="el-icon-chat-line-round" />
          <p>暂无对话记录</p>
        </div>
      </div>
    </div>

    <div class="main-content">
      <div class="chat-header">
        <div class="header-title">
          <i class="el-icon-magic-stick" />
          <span>灵犀智能问答</span>
        </div>
      </div>

      <div ref="msgBox" class="message-area">
        <div v-if="messages.length === 0" class="welcome-area">
          <div class="welcome-icon">
            <i class="el-icon-magic-stick" />
          </div>
          <h2>你好，我是灵犀AI助手</h2>
          <p>我可以帮你解答问题、查询知识库、分析数据等</p>
          <div class="quick-questions">
            <div class="quick-item" @click="askQuestion('公司的考勤制度是怎样的？')">
              <i class="el-icon-time" />
              <span>公司的考勤制度是怎样的？</span>
            </div>
            <div class="quick-item" @click="askQuestion('如何申请报销？')">
              <i class="el-icon-money" />
              <span>如何申请报销？</span>
            </div>
            <div class="quick-item" @click="askQuestion('产品有哪些核心功能？')">
              <i class="el-icon-s-grid" />
              <span>产品有哪些核心功能？</span>
            </div>
          </div>
        </div>

        <div v-for="(msg, idx) in messages" :key="idx" class="message-row" :class="msg.role">
          <div class="avatar" :class="msg.role">
            <i :class="msg.role === 'user' ? 'el-icon-user' : 'el-icon-magic-stick'" />
          </div>
          <div class="message-content">
            <div class="message-text" v-html="formatContent(msg.content)" />
            <div v-if="msg.sources && msg.sources.length" class="message-sources">
              <div class="source-header" @click="toggleSources(idx)">
                <i :class="msg.showSources ? 'el-icon-arrow-down' : 'el-icon-arrow-right'" />
                <span>引用内容 ({{ msg.sources.length }}条)</span>
              </div>
              <div v-show="msg.showSources" class="source-list">
                <div v-for="(s, si) in msg.sources" :key="si" class="source-item">
                  <div class="source-meta">
                    <span class="source-index">[{{ si + 1 }}]</span>
                    <span class="source-score">相似度: {{ (s.score * 100).toFixed(0) }}%</span>
                  </div>
                  <div class="source-content">{{ s.content }}</div>
                </div>
              </div>
            </div>
            <div v-if="msg.role === 'assistant' && msg.conversationId" class="message-feedback">
              <el-tooltip content="有帮助" placement="top">
                <el-button
                  size="mini"
                  circle
                  icon="el-icon-thumb"
                  :type="msg.feedback === 'HELPFUL' ? 'primary' : 'default'"
                  @click="submitFeedback(msg, 'HELPFUL')"
                />
              </el-tooltip>
              <el-tooltip content="需要改进" placement="top">
                <el-button
                  size="mini"
                  circle
                  icon="el-icon-warning-outline"
                  :type="msg.feedback === 'UNHELPFUL' ? 'danger' : 'default'"
                  @click="submitFeedback(msg, 'UNHELPFUL')"
                />
              </el-tooltip>
            </div>
          </div>
        </div>

        <div v-if="chatLoading" class="message-row assistant">
          <div class="avatar assistant">
            <i class="el-icon-magic-stick" />
          </div>
          <div class="message-content">
            <div class="message-text thinking">
              <i class="el-icon-loading" /> 正在思考中...
            </div>
          </div>
        </div>
      </div>

      <div class="input-area">
        <div class="input-wrapper">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="1"
            :autosize="{ minRows: 1, maxRows: 4 }"
            placeholder="输入问题，按 Enter 发送，Shift+Enter 换行"
            resize="none"
            @keydown.enter.native="handleKeydown"
          />
          <el-button
            type="primary"
            circle
            icon="el-icon-s-promotion"
            :loading="chatLoading"
            :disabled="!inputText.trim()"
            @click="sendMessage"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { sendChat, listSessions, deleteSession, getSessionConversations, feedbackQa } from '@/api/knowledge/qa'
import { mapGetters } from 'vuex'

export default {
  name: 'KnowledgeQa',
  data() {
    return {
      chatForm: { userId: '', deptId: '100' },
      inputText: '',
      messages: [],
      chatLoading: false,
      sessionList: [],
      currentSessionId: null
    }
  },
  computed: {
    ...mapGetters(['id'])
  },
  created() {
    this.chatForm.userId = this.id || '1'
    this.loadSessions()
  },
  methods: {
    loadSessions() {
      listSessions(this.chatForm.userId).then(res => {
        this.sessionList = res.data || []
      })
    },
    createNewSession() {
      this.currentSessionId = null
      this.messages = []
    },
    selectSession(session) {
      this.currentSessionId = session.sessionId
      getSessionConversations(session.sessionId).then(res => {
        const conversations = res.data || []
        this.messages = []
        conversations.forEach(conv => {
          this.messages.push({ role: 'user', content: conv.question })
          this.messages.push({
            role: 'assistant',
            content: conv.answer,
            sources: this.parseSources(conv.sourceChunks),
            showSources: false,
            conversationId: conv.conversationId,
            feedback: conv.feedback
          })
        })
        this.scrollBottom()
      })
    },
    handleSessionCommand(command) {
      if (command.action === 'delete') {
        this.$confirm('确定删除该对话？', '提示', { type: 'warning' }).then(() => {
          deleteSession(command.session.sessionId).then(() => {
            this.$message.success('删除成功')
            if (this.currentSessionId === command.session.sessionId) {
              this.createNewSession()
            }
            this.loadSessions()
          })
        }).catch(() => {})
      }
    },
    askQuestion(question) {
      this.inputText = question
      this.sendMessage()
    },
    handleKeydown(e) {
      if (!e.shiftKey) {
        e.preventDefault()
        this.sendMessage()
      }
    },
    sendMessage() {
      const question = this.inputText.trim()
      if (!question || this.chatLoading) return

      this.messages.push({ role: 'user', content: question })
      this.inputText = ''
      this.scrollBottom()
      this.chatLoading = true

      sendChat({
        question,
        userId: Number(this.chatForm.userId),
        deptId: Number(this.chatForm.deptId),
        sessionId: this.currentSessionId
      }).then(res => {
        const data = res.data || {}
        this.currentSessionId = data.sessionId
        this.messages.push({
          role: 'assistant',
          content: data.answer || '暂无答案',
          sources: this.parseSources(data.source),
          showSources: false,
          conversationId: data.conversationId,
          feedback: null
        })
        this.scrollBottom()
        this.loadSessions()
      }).catch(() => {
        this.messages.push({ role: 'assistant', content: '请求失败，请稍后重试' })
      }).finally(() => { this.chatLoading = false })
    },
    parseSources(sourceStr) {
      if (!sourceStr) return []
      try {
        return JSON.parse(sourceStr)
      } catch {
        return []
      }
    },
    toggleSources(idx) {
      this.$set(this.messages[idx], 'showSources', !this.messages[idx].showSources)
    },
    submitFeedback(message, feedback) {
      if (!message.conversationId) return
      feedbackQa({
        conversationId: message.conversationId,
        feedback
      }).then(() => {
        this.$set(message, 'feedback', feedback)
        this.$message.success('反馈已记录')
      })
    },
    scrollBottom() {
      this.$nextTick(() => {
        const box = this.$refs.msgBox
        if (box) box.scrollTop = box.scrollHeight
      })
    },
    formatContent(text) {
      if (!text) return ''
      return text.replace(/\n/g, '<br/>')
    }
  }
}
</script>

<style scoped>
.qa-page {
  display: flex;
  height: calc(100vh - 84px);
  background: #f5f7fa;
  overflow: hidden;
}

.sidebar {
  width: 260px;
  background: #fff;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #ebeef5;
}

.new-chat-btn {
  width: 100%;
  height: 40px;
  font-size: 14px;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: all 0.2s;
}

.session-item:hover {
  background: #f5f7fa;
}

.session-item.active {
  background: #ecf5ff;
}

.session-icon {
  font-size: 18px;
  color: #409eff;
  margin-right: 10px;
}

.session-info {
  flex: 1;
  overflow: hidden;
}

.session-title {
  font-size: 14px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-meta {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.session-more {
  font-size: 14px;
  color: #c0c4cc;
  padding: 4px;
  border-radius: 4px;
}

.session-more:hover {
  background: #e4e7ed;
  color: #606266;
}

.empty-sessions {
  text-align: center;
  padding: 40px 20px;
  color: #c0c4cc;
}

.empty-sessions i {
  font-size: 48px;
  margin-bottom: 12px;
}

.empty-sessions p {
  font-size: 14px;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.chat-header {
  height: 56px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #ebeef5;
}

.header-title {
  display: flex;
  align-items: center;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.header-title i {
  font-size: 20px;
  color: #409eff;
  margin-right: 8px;
}

.header-actions {
  display: flex;
  align-items: center;
}

.message-area {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.welcome-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #606266;
}

.welcome-icon {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}

.welcome-icon i {
  font-size: 40px;
  color: #fff;
}

.welcome-area h2 {
  font-size: 24px;
  color: #303133;
  margin: 0 0 8px 0;
}

.welcome-area p {
  font-size: 14px;
  color: #909399;
  margin: 0 0 32px 0;
}

.quick-questions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
}

.quick-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  color: #606266;
}

.quick-item:hover {
  background: #ecf5ff;
  color: #409eff;
}

.quick-item i {
  margin-right: 8px;
  font-size: 16px;
}

.message-row {
  display: flex;
  margin-bottom: 24px;
}

.message-row.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.avatar.user {
  background: #409eff;
  color: #fff;
}

.avatar.assistant {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.message-content {
  max-width: 70%;
  margin: 0 12px;
}

.message-text {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
}

.message-row.user .message-text {
  background: #409eff;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.message-row.assistant .message-text {
  background: #f5f7fa;
  color: #303133;
  border-bottom-left-radius: 4px;
}

.message-text.thinking {
  color: #909399;
}

.message-sources {
  margin-top: 12px;
}

.message-feedback {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.source-header {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #409eff;
  cursor: pointer;
  padding: 6px 0;
}

.source-header:hover {
  color: #66b1ff;
}

.source-list {
  margin-top: 8px;
  border-left: 3px solid #409eff;
  padding-left: 12px;
}

.source-item {
  background: #f0f7ff;
  border-radius: 6px;
  padding: 10px 12px;
  margin-bottom: 8px;
}

.source-item:last-child {
  margin-bottom: 0;
}

.source-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.source-index {
  font-weight: 600;
  color: #409eff;
  font-size: 12px;
}

.source-score {
  font-size: 12px;
  color: #909399;
}

.source-content {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

.input-area {
  padding: 16px 24px 24px;
  border-top: 1px solid #ebeef5;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  background: #f5f7fa;
  border-radius: 12px;
  padding: 12px 16px;
}

.input-wrapper .el-textarea {
  flex: 1;
}

.input-wrapper .el-textarea :deep(.el-textarea__inner) {
  background: transparent;
  border: none;
  padding: 0;
  font-size: 14px;
  line-height: 1.5;
  resize: none;
}

.input-wrapper .el-textarea :deep(.el-textarea__inner:focus) {
  outline: none;
  box-shadow: none;
}
</style>
