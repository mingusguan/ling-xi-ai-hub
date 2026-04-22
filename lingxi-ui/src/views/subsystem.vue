<template>
  <div class="subsystem-container">
    <div class="subsystem-topbar">
      <div class="topbar-logo">灵犀智能中枢</div>
      <div class="topbar-right">
        <!-- <el-badge :value="unreadMessageCount" :hidden="unreadMessageCount === 0" type="primary" class="message-badge">
          <i class="el-icon-bell message-icon right-menu-item" @click="goToMessageCenter"></i>
        </el-badge> -->
        <el-dropdown class="avatar-dropdown" trigger="hover" @command="handleDropdownCommand">
          <div class="avatar-wrapper">
            <img :src="avatar" class="user-avatar">
            <span class="user-nickname">{{ nickName }}</span>
            <i class="el-icon-arrow-down arrow-icon"></i>
          </div>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item command="profile">个人中心</el-dropdown-item>
            <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
        <ProfileDialog ref="profileDialog" v-model="profileVisible" />
      </div>
    </div>
    <div class="subsystem-header">
      <h1>灵犀智能中枢</h1>
      <p>请选择要进入的业务子系统</p>
    </div>
    <div class="subsystem-cards">
      <div 
        v-if="showBasicSystem" 
        class="subsystem-card" 
        @click="goToSystem('/index', 'basic')"
      >
        <div class="card-icon basic-icon">
          <svg-icon icon-class="system" />
        </div>
        <div class="card-content">
          <h3>基础管理系统</h3>
          <p>提供用户、角色、菜单、组织、日志与平台配置等基础能力</p>
        </div>
        <div class="card-arrow"><i class="el-icon-arrow-right"></i></div>
      </div>
      <div 
        v-if="showKnowledgeSystem" 
        class="subsystem-card" 
        @click="goToSystem('/knowledge/document', 'knowledge')"
      >
        <div class="card-icon knowledge-icon">
          <svg-icon icon-class="education" />
        </div>
        <div class="card-content">
          <h3>智能知识库</h3>
          <p>提供知识沉淀、文档处理、问答检索和知识运营能力</p>
        </div>
        <div class="card-arrow"><i class="el-icon-arrow-right"></i></div>
      </div>
      <div 
        v-if="showOaSystem" 
        class="subsystem-card" 
        @click="goToSystem('/oa/dashboard', 'oa')"
      >
        <div class="card-icon oa-icon">
          <svg-icon icon-class="dashboard" />
        </div>
        <div class="card-content">
          <h3>OA子系统</h3>
          <p>提供工作台、审批流程、请假报销、公告任务和会议管理等能力</p>
        </div>
        <div class="card-arrow"><i class="el-icon-arrow-right"></i></div>
      </div>
    </div>
    <div class="empty-tip" v-if="!showBasicSystem && !showKnowledgeSystem && !showOaSystem">
      <i class="el-icon-warning" style="font-size: 48px; color: #909399;"></i>
      <p style="margin-top: 20px; color: #606266;">暂无可用的子系统权限，请联系管理员分配权限</p>
    </div>
    <div class="subsystem-footer"><span>© 2026 灵犀智能中枢</span></div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import auth from '@/plugins/auth'
import ProfileDialog from '@/components/ProfileDialog'
import { getUnreadMessageCount } from '@/api/system/message'

export default {
  name: 'Subsystem',
  components: { ProfileDialog },
  data() {
    return {
      profileVisible: false,
      unreadMessageCount: 0
    }
  },
  mounted() {
    window.addEventListener('openProfileDialog', this.handleOpenProfileDialog)
    this.loadUnreadMessageCount()
  },
  beforeDestroy() {
    window.removeEventListener('openProfileDialog', this.handleOpenProfileDialog)
  },
  computed: {
    ...mapGetters(['avatar', 'nickName', 'name', 'roles']),
    showBasicSystem() {
      return this.isAdmin || auth.hasPermiOr([
        'system:user:list',
        'system:role:list',
        'system:menu:list',
        'system:dept:list',
        'system:post:list',
        'system:dict:list',
        'monitor:operlog:list',
        'monitor:logininfor:list',
        'monitor:job:list',
        'tool:gen:list'
      ])
    },
    showKnowledgeSystem() {
      return this.isAdmin || auth.hasPermiOr([
        'knowledge:category:list',
        'knowledge:document:list'
      ])
    },
    showOaSystem() {
      return this.isAdmin || auth.hasPermiOr([
        'oa:dashboard:view',
        'oa:process:list',
        'oa:leave:list',
        'oa:leave:balance',
        'oa:leave:rule:list',
        'oa:leave:quota:list',
        'oa:leave:adjustment:list',
        'oa:expense:list',
        'oa:reminder:list'
      ])
    },
    isAdmin() {
      return this.roles.includes('admin')
    }
  },
  methods: {
    handleDropdownCommand(command) {
      if (command === 'profile') {
        this.profileVisible = true
      } else if (command === 'logout') {
        this.logout()
      }
    },
    handleOpenProfileDialog(e) {
      this.profileVisible = true
      if (e.detail && e.detail.tab) {
        this.$nextTick(() => {
          this.$refs.profileDialog.selectedTab = e.detail.tab
        })
      }
    },
    goToSystem(path, sysCode) {
      this.$store.commit('SET_SYS_CODE', sysCode)
      this.$store.dispatch('GenerateRoutes').then(() => {
        this.$router.push(path)
      })
    },
    logout() {
      this.$confirm('确定注销并退出系统吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('LogOut').then(() => {
          location.href = '/index'
        })
      }).catch(() => {})
    },
    loadUnreadMessageCount() {
      getUnreadMessageCount().then(res => {
        this.unreadMessageCount = res.data.count || 0
      }).catch(() => {
        this.unreadMessageCount = 0
      })
    },
    goToMessageCenter() {
      this.$router.push('/message/center')
    }
  }
}
</script>

<style lang="scss" scoped>
.subsystem-container { min-height: 100vh; display: flex; flex-direction: column; align-items: center; justify-content: center; background: linear-gradient(135deg, #21409a 0%, #0f8a9d 100%); padding: 40px 20px; }
.subsystem-topbar { position: fixed; top: 0; left: 0; right: 0; height: 64px; background: rgba(255, 255, 255, 0.1); backdrop-filter: blur(10px); display: flex; align-items: center; justify-content: space-between; padding: 0 32px; border-bottom: 1px solid rgba(255, 255, 255, 0.1); z-index: 100; }
.topbar-logo { font-size: 18px; font-weight: 600; color: #fff; }
.topbar-right { display: flex; align-items: center; gap: 16px; }
.message-badge { cursor: pointer; }
.message-icon { font-size: 20px; color: #fff; padding: 8px; border-radius: 50%; background: rgba(255, 255, 255, 0.15); transition: all 0.2s ease; }
.message-icon:hover { background: rgba(255, 255, 255, 0.25); }
.avatar-dropdown { cursor: pointer; }
.avatar-wrapper { display: flex; align-items: center; gap: 10px; padding: 6px 12px; border-radius: 20px; background: rgba(255, 255, 255, 0.15); transition: all 0.2s ease; }
.avatar-wrapper:hover { background: rgba(255, 255, 255, 0.25); }
.user-avatar { width: 32px; height: 32px; border-radius: 50%; object-fit: cover; }
.user-nickname { font-size: 14px; color: #fff; font-weight: 500; }
.arrow-icon { font-size: 12px; color: rgba(255, 255, 255, 0.8); }
.subsystem-header { text-align: center; margin-bottom: 56px; }
.subsystem-header h1 { margin: 0 0 14px; font-size: 42px; color: #fff; letter-spacing: 2px; }
.subsystem-header p { margin: 0; font-size: 18px; color: rgba(255, 255, 255, 0.85); }
.subsystem-cards { display: flex; gap: 28px; flex-wrap: wrap; justify-content: center; max-width: 1240px; }
.subsystem-card { width: 360px; background: rgba(255, 255, 255, 0.96); border-radius: 18px; padding: 28px; display: flex; align-items: center; cursor: pointer; box-shadow: 0 18px 50px rgba(0, 0, 0, 0.15); transition: transform 0.25s ease, box-shadow 0.25s ease; }
.subsystem-card:hover { transform: translateY(-6px); box-shadow: 0 24px 60px rgba(0, 0, 0, 0.22); }
.card-icon { width: 72px; height: 72px; border-radius: 18px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.card-icon .svg-icon { width: 36px; height: 36px; color: #fff; }
.basic-icon { background: linear-gradient(135deg, #2b5876 0%, #4e4376 100%); }
.knowledge-icon { background: linear-gradient(135deg, #fc5c7d 0%, #6a82fb 100%); }
.oa-icon { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); }
.card-content { flex: 1; margin-left: 20px; }
.card-content h3 { margin: 0 0 8px; font-size: 20px; color: #1f2d3d; }
.card-content p { margin: 0; color: #6b7280; line-height: 1.6; font-size: 14px; }
.card-arrow { width: 40px; height: 40px; border-radius: 50%; background: #f3f4f6; display: flex; align-items: center; justify-content: center; color: #21409a; }
.empty-tip { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 80px 40px; background: rgba(255, 255, 255, 0.9); border-radius: 18px; text-align: center; }
.subsystem-footer { margin-top: 48px; color: rgba(255, 255, 255, 0.75); font-size: 14px; }
@media (max-width: 768px) { .subsystem-header h1 { font-size: 28px; } .subsystem-card { width: 100%; max-width: 380px; } }
</style>
