<template>
  <div class="xiaoling-robot" :class="{ 'sidebar-mode': isSidebarMode, collapse: collapse }" :style="positionStyle">
    <div 
      class="robot-launcher" 
      :class="{ pulse: hasNewMessage, dragging: isDragging }"
      @mousedown="startDrag"
      @click="handleLauncherClick"
    >
      <div class="robot-avatar">
        <span class="robot-emoji">🤖</span>
      </div>
      <span class="robot-name">小灵儿</span>
      <transition name="bounce">
        <div v-if="hasNewMessage" class="badge-dot"></div>
      </transition>
    </div>

    <transition name="slide-up">
      <div v-if="visible" class="chat-window" :style="chatWindowStyle">
        <div class="chat-header">
          <div class="header-left">
            <span class="robot-icon">🤖</span>
            <span class="header-title">小灵儿</span>
            <span class="header-status">在线</span>
          </div>
          <div class="header-actions">
            <!-- <i class="el-icon-minus" @click="minimizeWindow"></i> -->
            <i class="el-icon-close" @click="closeChat"></i>
          </div>
        </div>

        <div v-if="!minimized" class="chat-body">
          <div class="chat-container">
            <div class="chat-sidebar">
              <div class="sidebar-header">
                <el-button type="primary" icon="el-icon-plus" size="mini" @click="createNewSession">新建对话</el-button>
              </div>
              <div class="session-list">
                <div
                  v-for="session in sessionList"
                  :key="session.sessionId"
                  class="session-item"
                  :class="{ active: currentSessionId === session.sessionId }"
                  @click="selectSession(session)"
                >
                  <i class="el-icon-chat-line-round session-icon"></i>
                  <span class="session-title">{{ session.title }}</span>
                  <i class="el-icon-delete session-delete" @click.stop="deleteSession(session)"></i>
                </div>
              </div>
            </div>
            
            <div class="chat-main">
              <div class="chat-messages" ref="messagesRef">
                <div v-if="messageList.length === 0" class="welcome-section">
                  <div class="welcome-icon">👋</div>
                  <div class="welcome-title">您好！我是小灵儿</div>
                  <div class="welcome-desc">您的智能助手，支持 OA 查询和知识库问答</div>
                  <div class="quick-questions">
                    <div class="question-item" @click="sendQuickQuestion('帮我查一下待审批')">
                      📋 查询待审批
                    </div>
                    <div class="question-item" @click="sendQuickQuestion('我的假期还有多少')">
                      📅 假期余额
                    </div>
                    <div class="question-item" @click="sendQuickQuestion('有没有超时的审批')">
                      ⚠️ 超时预警
                    </div>
                    <div class="question-item" @click="sendQuickQuestion('知识库问答测试')">
                      📚 知识库问答
                    </div>
                  </div>
                </div>
                
                <div
                  v-for="msg in messageList"
                  :key="msg.messageId"
                  class="message-item"
                  :class="msg.role"
                >
                  <div class="message-avatar">
                    <img v-if="msg.role === 'user'" :src="avatar" class="user-avatar-img">
                    <i v-else class="el-icon-cpu"></i>
                  </div>
                  <div class="message-content">
                    <div class="message-bubble" :class="{ typing: msg.loading }">
                      <template v-if="msg.loading">
                        <span class="typing-dot"></span>
                        <span class="typing-dot"></span>
                        <span class="typing-dot"></span>
                      </template>
                      <template v-else>
                        <pre v-if="msg.role === 'assistant'" class="ai-content">{{ msg.content }}</pre>
                        <span v-else>{{ msg.content }}</span>
                      </template>
                    </div>
                    <div v-if="!msg.loading" class="message-time">
                      {{ formatTime(msg.createTime) }}
                    </div>
                  </div>
                </div>
              </div>
              
              <div class="chat-input-area">
                <el-input
                  v-model="inputMessage"
                  type="textarea"
                  :rows="2"
                  placeholder="Enter发送，Shift+Enter换行"
                  @keydown.enter.native.prevent="handleKeyDown"
                />
                <div class="input-actions">
                  <el-button type="primary" :loading="loading" size="mini" @click="sendMessage">发送</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { getChatSessions, createChatSession, getSessionMessages, deleteChatSession, sendChatMessage } from '@/api/oa/ai'

export default {
  name: 'XiaoLingRobot',
  props: {
    mode: {
      type: String,
      default: 'float'
    },
    collapse: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    ...mapGetters(['avatar']),
    isSidebarMode() {
      return this.mode === 'sidebar'
    },
    positionStyle() {
      if (this.isSidebarMode) {
        if (this.hasCustomPosition) {
          return {
            position: 'fixed',
            left: this.dragPosition.x + 'px',
            right: 'auto',
            top: this.dragPosition.y + 'px',
            bottom: 'auto',
            width: this.collapse ? '48px' : '236px',
            zIndex: 1002
          }
        }
        return {
          position: 'fixed',
          left: this.collapse ? '8px' : '12px',
          right: 'auto',
          top: 'auto',
          bottom: '14px',
          width: this.collapse ? '48px' : '236px',
          zIndex: 1002
        }
      }
      return {
        right: 'auto',
        bottom: 'auto',
        left: this.dragPosition.x + 'px',
        top: this.dragPosition.y + 'px'
      }
    },
    chatWindowStyle() {
      const windowWidth = 700
      const windowHeight = 500

      if (this.isSidebarMode) {
        return {
          left: this.collapse ? '72px' : '272px',
          bottom: '16px',
          top: 'auto'
        }
      }
      
      let left = -windowWidth + 50
      let top = -windowHeight - 20
      
      if (this.dragPosition.x < windowWidth) {
        left = 50
      }
      
      if (this.dragPosition.y < windowHeight) {
        top = 100
      }
      
      return {
        right: 'auto',
        bottom: 'auto',
        left: left + 'px',
        top: top + 'px'
      }
    }
  },
  data() {
    return {
      visible: false,
      minimized: false,
      hasNewMessage: false,
      sessionList: [],
      currentSessionId: null,
      messageList: [],
      inputMessage: '',
      loading: false,
      isDragging: false,
      dragPosition: { x: 24, y: 24 },
      dragStart: { x: 0, y: 0 },
      dragStartPosition: { x: 0, y: 0 },
      isDragged: false,
      hasCustomPosition: false
    }
  },
  mounted() {
    this.loadSessions()
    this.initPosition()
  },
  beforeDestroy() {
    if (this.pollTimer) {
      clearInterval(this.pollTimer)
    }
    this.removeDragListeners()
  },
  methods: {
    initPosition() {
      if (this.isSidebarMode) {
        return
      }
      this.dragPosition = {
        x: 24,
        y: window.innerHeight - 120
      }
    },
    startDrag(e) {
      if (this.isSidebarMode && !this.hasCustomPosition) {
        const rect = this.$el.getBoundingClientRect()
        this.dragPosition = {
          x: rect.left,
          y: rect.top
        }
      }
      this.isDragging = true
      this.isDragged = false
      this.dragStart = { x: e.clientX, y: e.clientY }
      this.dragStartPosition = { ...this.dragPosition }
      
      document.addEventListener('mousemove', this.onDrag)
      document.addEventListener('mouseup', this.stopDrag)
      document.addEventListener('mouseleave', this.stopDrag)
      
      e.preventDefault()
    },
    onDrag(e) {
      if (!this.isDragging) return
      
      const deltaX = e.clientX - this.dragStart.x
      const deltaY = e.clientY - this.dragStart.y
      
      if (Math.abs(deltaX) > 3 || Math.abs(deltaY) > 3) {
        this.isDragged = true
        this.hasCustomPosition = true
      }
      
      let newX = this.dragStartPosition.x + deltaX
      let newY = this.dragStartPosition.y + deltaY
      
      const launcherWidth = this.isSidebarMode ? (this.collapse ? 48 : 236) : 80
      const launcherHeight = this.isSidebarMode ? 48 : 76
      const bottomGap = this.isSidebarMode ? 14 : 24
      const maxX = window.innerWidth - launcherWidth
      const maxY = window.innerHeight - launcherHeight - bottomGap
      newX = Math.max(10, Math.min(newX, maxX))
      newY = Math.max(10, Math.min(newY, maxY))
      
      this.dragPosition = { x: newX, y: newY }
    },
    stopDrag() {
      this.isDragging = false
      this.removeDragListeners()
    },
    removeDragListeners() {
      document.removeEventListener('mousemove', this.onDrag)
      document.removeEventListener('mouseup', this.stopDrag)
      document.removeEventListener('mouseleave', this.stopDrag)
    },
    handleLauncherClick() {
      if (!this.isDragged) {
        this.toggleChat()
      }
    },
    toggleChat() {
      this.visible = !this.visible
      if (this.visible) {
        this.minimized = false
        this.hasNewMessage = false
        this.$nextTick(() => {
          this.scrollToBottom()
        })
      }
    },
    closeChat() {
      this.visible = false
      this.minimized = false
    },
    minimizeWindow() {
      this.minimized = !this.minimized
    },
    loadSessions() {
      getChatSessions().then(res => {
        this.sessionList = res.data
        if (this.sessionList.length > 0) {
          this.selectSession(this.sessionList[0])
        }
      })
    },
    createNewSession() {
      return createChatSession().then(res => {
        this.sessionList.unshift(res.data)
        return this.selectSession(res.data).then(() => {
          this.$nextTick(() => {
            this.scrollToBottom()
          })
          return res.data
        })
      })
    },
    selectSession(session) {
      return new Promise((resolve) => {
        this.currentSessionId = session.sessionId
        this.messageList = []
        getSessionMessages(session.sessionId).then(res => {
          this.messageList = res.data
          this.$nextTick(() => {
            this.scrollToBottom()
          })
          resolve()
        })
      })
    },
    deleteSession(session) {
      this.closeChat()
      this.$nextTick(() => {
        this.$confirm('确定删除此会话吗？', '提示').then(() => {
          deleteChatSession(session.sessionId).then(() => {
            this.$message.success('删除成功')
            if (session.sessionId === this.currentSessionId) {
              this.messageList = []
              this.currentSessionId = null
            }
            this.loadSessions()
            this.$nextTick(() => {
              this.toggleChat()
            })
          })
        }).catch(() => {
          this.toggleChat()
        })
      })
    },
    sendQuickQuestion(question) {
      this.inputMessage = question
      this.sendMessage()
    },
    handleKeyDown(e) {
      if (!e.shiftKey) {
        e.preventDefault()
        this.sendMessage()
      }
    },
    sendMessage() {
      if (!this.inputMessage.trim() || this.loading) return
      
      if (!this.currentSessionId) {
        this.createNewSession().then(() => {
          this.doSendMessage()
        })
      } else {
        this.doSendMessage()
      }
    },
    doSendMessage() {
      this.loading = true
      const message = this.inputMessage
      this.inputMessage = ''
      
      // 新对话第一条消息，更新会话标题
      const isNewSession = this.messageList.length === 0
      
      // 立即显示用户消息
      const userMsg = {
        messageId: Date.now(),
        role: 'user',
        content: message,
        createTime: new Date()
      }
      this.messageList.push(userMsg)
      
      // 添加AI回复loading占位符
      const loadingMsgId = Date.now() + 1
      const loadingMsg = {
        messageId: loadingMsgId,
        role: 'assistant',
        content: '',
        loading: true,
        createTime: new Date()
      }
      this.messageList.push(loadingMsg)
      
      this.$nextTick(() => {
        this.scrollToBottom()
      })
      
      // 更新会话列表中的标题
      if (isNewSession) {
        const title = message.length > 20 ? message.substring(0, 20) + '...' : message
        const session = this.sessionList.find(s => s.sessionId === this.currentSessionId)
        if (session) {
          session.title = title
        }
      }
      
      sendChatMessage({
        sessionId: this.currentSessionId,
        message: message
      }).then(res => {
        // 移除loading，添加实际AI回复
        this.messageList = this.messageList.filter(msg => msg.messageId !== loadingMsgId)
        this.messageList.push(res.data.aiMessage)
        this.$nextTick(() => {
          this.scrollToBottom()
        })
      }).finally(() => {
        this.loading = false
      })
    },
    scrollToBottom() {
      const ref = this.$refs.messagesRef
      if (ref) {
        ref.scrollTop = ref.scrollHeight
      }
    },
    formatTime(time) {
      if (!time) return ''
      const date = new Date(time)
      return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
    }
  }
}
</script>

<style scoped>
.xiaoling-robot {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 9999;
}

.xiaoling-robot.sidebar-mode {
  right: auto;
  top: auto;
}

.xiaoling-robot.sidebar-mode.collapse {
  right: auto;
}

.robot-launcher {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: grab;
  user-select: none;
  transition: all 0.3s ease;
}

.sidebar-mode .robot-launcher {
  flex-direction: row;
  justify-content: flex-start;
  gap: 10px;
  height: 48px;
  padding: 0 12px;
  border-radius: 8px;
  cursor: pointer;
  background: transparent;
  border: 1px solid transparent;
}

.sidebar-mode.collapse .robot-launcher {
  justify-content: center;
  padding: 0;
}

.robot-launcher.dragging {
  cursor: grabbing;
  z-index: 10000;
}

.robot-launcher:hover {
  transform: scale(1.05);
}

.sidebar-mode .robot-launcher:hover {
  transform: none;
  background: transparent;
  border-color: transparent;
}

.robot-launcher.pulse .robot-avatar {
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(37, 99, 235, 0.3), 0 0 0 0 rgba(8, 145, 178, 0.18);
  }
  50% {
    box-shadow: 0 0 0 10px rgba(37, 99, 235, 0), 0 0 0 18px rgba(8, 145, 178, 0);
  }
}

.robot-avatar {
  width: 60px;
  height: 60px;
  position: relative;
  background:
    radial-gradient(circle at 30% 28%, rgba(219, 234, 254, 0.92) 0%, rgba(96, 165, 250, 0.86) 18%, rgba(37, 99, 235, 0.94) 46%, rgba(15, 23, 42, 0.98) 100%);
  border: 1px solid rgba(191, 219, 254, 0.28);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  box-shadow:
    0 12px 30px rgba(15, 23, 42, 0.42),
    0 0 0 1px rgba(37, 99, 235, 0.14),
    0 0 24px rgba(8, 145, 178, 0.16);
}

.sidebar-mode .robot-avatar {
  width: 34px;
  height: 34px;
  box-shadow:
    0 8px 18px rgba(15, 23, 42, 0.32),
    0 0 16px rgba(8, 145, 178, 0.12);
}

.robot-avatar::before {
  content: '';
  position: absolute;
  top: 8px;
  left: 10px;
  width: 24px;
  height: 14px;
  background: rgba(255, 255, 255, 0.26);
  border-radius: 999px;
  filter: blur(8px);
  transform: rotate(-22deg);
}

.robot-avatar::after {
  content: '';
  position: absolute;
  inset: 3px;
  border-radius: 50%;
  border: 1px solid rgba(219, 234, 254, 0.14);
  pointer-events: none;
}

.robot-launcher:hover .robot-avatar {
  box-shadow:
    0 16px 38px rgba(15, 23, 42, 0.5),
    0 0 0 1px rgba(59, 130, 246, 0.2),
    0 0 28px rgba(8, 145, 178, 0.22);
}

.robot-emoji {
  font-size: 28px;
  position: relative;
  z-index: 1;
  text-shadow: 0 4px 12px rgba(15, 23, 42, 0.4);
}

.sidebar-mode .robot-emoji {
  font-size: 18px;
}

.robot-name {
  font-size: 12px;
  color: #dbeafe;
  font-weight: 600;
  line-height: 1;
  padding: 6px 10px;
  background: rgba(15, 23, 42, 0.78);
  border: 1px solid rgba(96, 165, 250, 0.18);
  border-radius: 999px;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.24);
}

.sidebar-mode .robot-name {
  color: #dbeafe;
  background: transparent;
  border: 0;
  box-shadow: none;
  padding: 0;
  font-size: 13px;
}

.sidebar-mode.collapse .robot-name {
  display: none;
}

.badge-dot {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 12px;
  height: 12px;
  background: #f56c6c;
  border-radius: 50%;
  border: 2px solid #0f172a;
  box-shadow: 0 0 0 2px rgba(248, 250, 252, 0.08);
}

.bounce-enter-active {
  animation: bounce-in 0.5s;
}

@keyframes bounce-in {
  0% {
    transform: scale(0);
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
  }
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.3s ease;
}

.slide-up-enter,
.slide-up-leave-to {
  opacity: 0;
  transform: translateY(30px);
}

.chat-window {
  position: absolute;
  width: 700px;
  height: 500px;
  background: rgba(15, 23, 42, 0.96);
  border: 1px solid rgba(96, 165, 250, 0.2);
  border-radius: 12px;
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.35);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.sidebar-mode .chat-window {
  position: fixed;
}

.chat-header {
  background: linear-gradient(135deg, #1e3a8a 0%, #0f766e 100%);
  padding: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.robot-icon {
  font-size: 20px;
}

.header-title {
  color: #fff;
  font-weight: 600;
  font-size: 15px;
}

.header-status {
  color: rgba(219, 234, 254, 0.92);
  font-size: 12px;
  background: rgba(96, 165, 250, 0.18);
  padding: 2px 8px;
  border-radius: 10px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.header-actions i {
  color: rgba(255, 255, 255, 0.8);
  font-size: 18px;
  cursor: pointer;
  transition: all 0.2s;
}

.header-actions i:hover {
  color: #fff;
}

.chat-body {
  flex: 1;
  overflow: hidden;
}

.chat-container {
  display: flex;
  height: 100%;
}

.chat-sidebar {
  width: 180px;
  border-right: 1px solid rgba(96, 165, 250, 0.14);
  display: flex;
  flex-direction: column;
  background: rgba(15, 23, 42, 0.72);
}

.sidebar-header {
  padding: 12px;
  border-bottom: 1px solid rgba(96, 165, 250, 0.14);
}

.sidebar-header .el-button {
  width: 100%;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.session-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 12px;
}

.session-item:hover {
  background: rgba(37, 99, 235, 0.16);
}

.session-item.active {
  background: rgba(37, 99, 235, 0.24);
  border-right: 3px solid #60a5fa;
}

.session-icon {
  color: #93c5fd;
  margin-right: 6px;
  font-size: 14px;
}

.session-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #cbd5e1;
}

.session-delete {
  color: #94a3b8;
  font-size: 14px;
  opacity: 0;
  transition: all 0.2s;
}

.session-item:hover .session-delete {
  opacity: 1;
}

.session-delete:hover {
  color: #f56c6c;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: rgba(15, 23, 42, 0.52);
}

.chat-messages {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

.welcome-section {
  text-align: center;
  padding: 30px 20px;
}

.welcome-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.welcome-title {
  font-size: 18px;
  font-weight: 600;
  color: #e2e8f0;
  margin-bottom: 8px;
}

.welcome-desc {
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 20px;
}

.quick-questions {
  display: grid;
  gap: 10px;
}

.question-item {
  padding: 10px 16px;
  background: rgba(30, 41, 59, 0.72);
  border: 1px solid rgba(96, 165, 250, 0.14);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 13px;
  color: #cbd5e1;
}

.question-item:hover {
  background: rgba(37, 99, 235, 0.2);
  color: #bfdbfe;
  border-color: rgba(96, 165, 250, 0.3);
}

.message-item {
  display: flex;
  margin-bottom: 16px;
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message-item.assistant .message-avatar {
  background: linear-gradient(135deg, #2563eb 0%, #0891b2 100%);
  color: #fff;
  margin-right: 10px;
}

.message-item.user .message-avatar {
  background: transparent;
  margin-left: 10px;
  overflow: hidden;
  padding: 0;
}

.user-avatar-img {
  width: 32px;
  height: 32px;
  object-fit: cover;
  border-radius: 50%;
  display: block;
}

.message-content {
  max-width: 70%;
}

.message-bubble {
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.6;
}

.message-item.assistant .message-bubble {
  background: rgba(30, 41, 59, 0.78);
  border: 1px solid rgba(96, 165, 250, 0.12);
  color: #e2e8f0;
}

.message-item.user .message-bubble {
  background: linear-gradient(135deg, #2563eb 0%, #0f766e 100%);
  color: #fff;
}

.message-bubble .ai-content {
  margin: 0;
  font-family: inherit;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-time {
  font-size: 11px;
  color: #64748b;
  margin-top: 4px;
}

.message-item.user .message-time {
  text-align: right;
}

.message-bubble.typing {
  display: flex;
  gap: 4px;
  padding: 12px 16px;
}

.typing-dot {
  width: 6px;
  height: 6px;
  background: #93c5fd;
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.typing-dot:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-6px);
  }
}

.chat-input-area {
  padding: 12px 16px;
  border-top: 1px solid rgba(96, 165, 250, 0.14);
  background: rgba(15, 23, 42, 0.88);
}

.chat-input-area .el-textarea__inner {
  background: #fff !important;
  color: #1f2937 !important;
  font-size: 13px;
  resize: none;
}

.chat-input-area .el-textarea__inner::placeholder {
  color: #9ca3af;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}
</style>
