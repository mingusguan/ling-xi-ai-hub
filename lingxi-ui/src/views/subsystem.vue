<template>
  <div class="subsystem-container">
    <div class="bg-svg">
      <svg viewBox="0 0 1920 1080" preserveAspectRatio="xMidYMid slice">
        <defs>
          <filter id="glowStar">
            <feGaussianBlur stdDeviation="5" result="coloredBlur"/>
            <feMerge>
              <feMergeNode in="coloredBlur"/>
              <feMergeNode in="SourceGraphic"/>
            </feMerge>
          </filter>
          <radialGradient id="starGold" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stop-color="#FFFFFF"/>
            <stop offset="50%" stop-color="#E8E8E8"/>
            <stop offset="100%" stop-color="#C0C0C0"/>
          </radialGradient>
          <radialGradient id="starBright" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stop-color="#FFFFFF"/>
            <stop offset="40%" stop-color="#F5F5F5"/>
            <stop offset="100%" stop-color="#D4D4D4"/>
          </radialGradient>
        </defs>
        <g filter="url(#glowStar)">
          <polygon points="200,144 202,150 208,150 203,154 205,160 200,156 195,160 197,154 192,150 198,150" fill="url(#starGold)">
            <animate attributeName="opacity" values="0.3;1;0.3" dur="2s" begin="0s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="400,244 402,250 408,250 403,254 405,260 400,256 395,260 397,254 392,250 398,250" fill="url(#starBright)">
            <animate attributeName="opacity" values="0.25;0.95;0.25" dur="2.5s" begin="0.3s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="650,120 652,128 659,128 654,133 656,140 650,136 644,140 646,133 641,128 648,128" fill="url(#starGold)">
            <animate attributeName="opacity" values="0.35;1;0.35" dur="2.3s" begin="0.5s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="900,180 902,188 909,188 904,193 906,200 900,196 894,200 896,193 891,188 898,188" fill="url(#starBright)">
            <animate attributeName="opacity" values="0.3;0.9;0.3" dur="2.2s" begin="0.7s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="1150,100 1152,108 1159,108 1154,113 1156,120 1150,116 1144,120 1146,113 1141,108 1148,108" fill="url(#starGold)">
            <animate attributeName="opacity" values="0.28;0.95;0.28" dur="2.7s" begin="0.4s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="1400,220 1402,228 1409,228 1404,233 1406,240 1400,236 1394,240 1396,233 1391,228 1398,228" fill="url(#starBright)">
            <animate attributeName="opacity" values="0.32;0.92;0.32" dur="2.4s" begin="0.6s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="1700,160 1702,168 1709,168 1704,173 1706,180 1700,176 1694,180 1696,173 1691,168 1698,168" fill="url(#starGold)">
            <animate attributeName="opacity" values="0.26;0.88;0.26" dur="2.6s" begin="0.8s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="300,550 301,555 304,555 302,558 302,561 300,559 298,561 298,558 296,555 299,555" fill="#F0F0F0">
            <animate attributeName="opacity" values="0.2;0.7;0.2" dur="3s" begin="0.2s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="550,700 551,705 554,705 552,708 552,711 550,709 548,711 548,708 546,705 549,705" fill="#E8E8E8">
            <animate attributeName="opacity" values="0.22;0.75;0.22" dur="2.8s" begin="0.1s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="800,820 801,825 804,825 802,828 802,831 800,829 798,831 798,828 796,825 799,825" fill="#F5F5F5">
            <animate attributeName="opacity" values="0.18;0.68;0.18" dur="3.2s" begin="0.5s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="1100,650 1101,655 1104,655 1102,658 1102,661 1100,659 1098,661 1098,658 1096,655 1099,655" fill="#E8E8E8">
            <animate attributeName="opacity" values="0.25;0.78;0.25" dur="2.6s" begin="0.3s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="1350,780 1351,785 1354,785 1352,788 1352,791 1350,789 1348,791 1348,788 1346,785 1349,785" fill="#F0F0F0">
            <animate attributeName="opacity" values="0.21;0.72;0.21" dur="2.9s" begin="0.7s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="1550,867 1551,870 1554,870 1552,873 1552,876 1550,874 1548,876 1548,873 1546,870 1549,870" fill="#E8E8E8">
            <animate attributeName="opacity" values="0.2;0.72;0.2" dur="2.9s" begin="0.6s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="1850,927 1851,930 1854,930 1852,933 1852,936 1850,934 1848,936 1848,933 1846,930 1849,930" fill="#FFFFFF">
            <animate attributeName="opacity" values="0.19;0.65;0.19" dur="2.5s" begin="0.8s" repeatCount="indefinite"/>
          </polygon>
        </g>
      </svg>
    </div>

    <div class="bg-gradient"></div>
    <div class="bg-glow glow-1"></div>
    <div class="bg-glow glow-2"></div>
    <div class="bg-glow glow-3"></div>

    <div class="particles">
      <div v-for="n in 50" :key="n" class="particle" :style="{
        width: Math.random() * 4 + 2 + 'px',
        height: Math.random() * 4 + 2 + 'px',
        left: Math.random() * 100 + '%',
        top: 85 + Math.random() * 20 + '%',
        animationDelay: Math.random() * 10 + 's',
        animationDuration: Math.random() * 6 + 12 + 's'
      }"></div>
    </div>

    <div class="subsystem-topbar">
      <div class="topbar-logo">
        <div class="logo-icon">
          <svg viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="50" cy="50" r="48" fill="url(#glowBg)" opacity="0.2"/>
            <ellipse cx="50" cy="50" rx="47" ry="47" stroke="url(#ring1)" stroke-width="1.5" stroke-dasharray="10 5" opacity="0.6"/>
            <path d="M18 18L18 70L42 54L42 18" fill="url(#lxGrad)"/>
            <path d="M42 54L68 54L68 70L42 70Z" fill="url(#lxGrad)"/>
            <path d="M52 18L82 66M82 18L52 66" stroke="url(#lxGrad)" stroke-width="8.5" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
            <defs>
              <radialGradient id="glowBg" cx="50%" cy="50%" r="50%">
                <stop offset="0%" stop-color="#60A5FA"/>
                <stop offset="70%" stop-color="#8B5CF6"/>
                <stop offset="100%" stop-color="#3B82F6"/>
              </radialGradient>
              <linearGradient id="ring1" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#60A5FA"/>
                <stop offset="50%" stop-color="#8B5CF6"/>
                <stop offset="100%" stop-color="#3B82F6"/>
              </linearGradient>
              <linearGradient id="lxGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#FFFFFF"/>
                <stop offset="20%" stop-color="#FFFFFF"/>
                <stop offset="50%" stop-color="#60A5FA"/>
                <stop offset="80%" stop-color="#3B82F6"/>
                <stop offset="100%" stop-color="#8B5CF6"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <span class="logo-text">灵犀AIHub</span>
      </div>
      <div class="topbar-right">
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
      <h1>智能中枢</h1>
      <p>选择要进入的业务子系统</p>
    </div>

    <div class="subsystem-cards">
      <div 
        v-if="showBasicSystem" 
        class="subsystem-card" 
        @click="goToSystem('/system/user', 'basic')"
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
        @click="goToOaSystem"
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
      <div 
        v-if="showAiSystem" 
        class="subsystem-card" 
        @click="goToSystem('/ai/document', 'ai_tool')"
      >
        <div class="card-icon ai-icon">
          <svg-icon icon-class="skill" />
        </div>
        <div class="card-content">
          <h3>AI工具库</h3>
          <p>提供公文助手、报表解读、智能问答和AI助手等智能工具</p>
        </div>
        <div class="card-arrow"><i class="el-icon-arrow-right"></i></div>
      </div>
      <div
        v-if="showMcpMarketSystem"
        class="subsystem-card"
        @click="goToSystem('/mcp-market/tools', 'mcp_market')"
      >
        <div class="card-icon mcp-icon">
          <svg-icon icon-class="list" />
        </div>
        <div class="card-content">
          <h3>MCP工具市场</h3>
          <p>统一管理 MCP 工具发现、发布、申请审批、版本治理、审计监控和助手编排复用</p>
        </div>
        <div class="card-arrow"><i class="el-icon-arrow-right"></i></div>
      </div>
    </div>

    <div class="empty-tip" v-if="!showBasicSystem && !showKnowledgeSystem && !showOaSystem && !showAiSystem && !showMcpMarketSystem">
      <i class="el-icon-warning" style="font-size: 48px; color: #94A3B8;"></i>
      <p style="margin-top: 20px; color: #64748B;">暂无可用的子系统权限，请联系管理员分配权限</p>
    </div>

    <div class="subsystem-footer"><span>© 2026 灵犀AIHub · 智能中枢</span></div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import auth from '@/plugins/auth'
import ProfileDialog from '@/components/ProfileDialog'
import { getUnreadMessageCount } from '@/api/system/message'
import { withPublicPath } from '@/utils/appPath'

const oaEntryRoutes = [
  { path: '/oa/dashboard', permissions: ['oa:dashboard:view'] },
  { path: '/oa/process', permissions: ['oa:process:list'] },
  { path: '/oa/leave', permissions: ['oa:leave:list'] },
  { path: '/oa/balance', permissions: ['oa:leave:balance'] },
  { path: '/oa/rule', permissions: ['oa:leave:rule:list'] },
  { path: '/oa/manage', permissions: ['oa:leave:quota:list'] },
  { path: '/oa/adjustment', permissions: ['oa:leave:adjustment:list'] },
  { path: '/oa/expense', permissions: ['oa:expense:list'] },
  { path: '/oa/reminder', permissions: ['oa:reminder:list'] },
  { path: '/oa/holiday', permissions: ['oa:holiday:list'] }
]

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
        'knowledge:document:list',
        'knowledge:qa:chat',
        'knowledge:operation:view'
      ])
    },
    showOaSystem() {
      return this.isAdmin || this.hasAnyEntryPermission(oaEntryRoutes)
    },
    showAiSystem() {
      return this.isAdmin || auth.hasPermiOr([
        'ai:document:view',
        'ai:report:view',
        'ai:chat:view',
        'ai:agent:list',
        'ai:governance:view'
      ])
    },
    showMcpMarketSystem() {
      return this.isAdmin || auth.hasPermiOr([
        'ai:mcp:market:list',
        'ai:mcp:market:edit',
        'ai:mcp:market:apply',
        'ai:mcp:market:approve',
        'ai:mcp:market:audit',
        'ai:mcp:market:bind'
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
      this.$store.dispatch('tagsView/delAllViews').then(() => {
        this.$store.dispatch('GenerateRoutes').then(() => {
          this.$router.push(path)
        })
      })
    },
    goToOaSystem() {
      this.goToSystem(this.resolveFirstPermittedPath(oaEntryRoutes, '/oa/dashboard'), 'oa')
    },
    hasAnyEntryPermission(routes) {
      return routes.some(route => auth.hasPermiOr(route.permissions))
    },
    resolveFirstPermittedPath(routes, fallbackPath) {
      if (this.isAdmin) {
        return fallbackPath
      }
      const matched = routes.find(route => auth.hasPermiOr(route.permissions))
      return matched ? matched.path : fallbackPath
    },
    logout() {
      this.$confirm('确定注销并退出系统吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('LogOut').then(() => {
          location.href = withPublicPath('/index')
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
.subsystem-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #0B1120;
  padding: 80px 20px 40px;
  position: relative;
  overflow: hidden;
}

// SVG星辰背景
.bg-svg {
  position: fixed;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  
  svg {
    width: 100%;
    height: 100%;
  }
}

// 深空渐变叠加层
.bg-gradient {
  position: absolute;
  inset: 0;
  z-index: 2;
  background: 
    radial-gradient(ellipse at 50% 0%, rgba(59, 130, 246, 0.08) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 80%, rgba(139, 92, 246, 0.06) 0%, transparent 50%),
    radial-gradient(ellipse at 20% 60%, rgba(59, 130, 246, 0.04) 0%, transparent 40%),
    linear-gradient(180deg, #0B1120 0%, #0F172A 50%, #0B1120 100%);
}

// 流体光晕
.bg-glow {
  position: absolute;
  z-index: 2;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.3;
  animation: floatGlow 20s ease-in-out infinite;
  will-change: transform;
}

.glow-1 {
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.4) 0%, transparent 70%);
  top: -200px;
  left: -100px;
}

.glow-2 {
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(139, 92, 246, 0.35) 0%, transparent 70%);
  bottom: -100px;
  right: -50px;
  animation-delay: -7s;
}

.glow-3 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.25) 0%, transparent 70%);
  top: 30%;
  right: 20%;
  animation-delay: -14s;
}

@keyframes floatGlow {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -30px) scale(1.1); }
  66% { transform: translate(-20px, 20px) scale(0.95); }
}



// 粒子效果
.particles {
  position: fixed;
  inset: 0;
  z-index: 5;
  pointer-events: none;
  overflow: hidden;
}

.particle {
  position: absolute;
  background: #ffffff;
  border-radius: 50%;
  box-shadow: 
    0 0 6px #60A5FA,
    0 0 12px #3B82F6,
    0 0 24px rgba(59, 130, 246, 0.8);
  animation: particleFloat ease-in-out infinite;
  will-change: transform;
}

@keyframes particleFloat {
  0% {
    transform: translate(0, 0) scale(1);
    opacity: 0;
  }
  10% {
    opacity: 1;
  }
  25% {
    transform: translate(60px, -25vh) scale(1.2);
  }
  50% {
    transform: translate(-40px, -50vh) scale(0.9);
  }
  75% {
    transform: translate(50px, -75vh) scale(1.1);
    opacity: 0.9;
  }
  100% {
    transform: translate(-20px, -120vh) scale(0.5);
    opacity: 0;
  }
}

.subsystem-topbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 72px;
  background: rgba(15, 23, 42, 0.8) !important;
  backdrop-filter: blur(20px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  border-bottom: 1px solid rgba(59, 130, 246, 0.15);
  z-index: 100;
}

.topbar-logo {
  display: flex;
  align-items: center;
  gap: 12px;

  .logo-icon {
    width: 44px;
    height: 44px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, rgba(59, 130, 246, 0.25) 0%, rgba(139, 92, 246, 0.15) 100%);
    border: 1px solid rgba(59, 130, 246, 0.25);

    svg {
      width: 34px;
      height: 34px;
    }
  }

  .logo-text {
    font-size: 18px;
    font-weight: 600;
    background: linear-gradient(135deg, #60A5FA 0%, #8B5CF6 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    letter-spacing: 1px;
  }
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-dropdown {
  cursor: pointer;
}

.avatar-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  border-radius: 24px;
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(59, 130, 246, 0.2);
  transition: all 0.25s ease;

  &:hover {
    background: rgba(15, 23, 42, 0.8);
    border-color: rgba(59, 130, 246, 0.35);
  }
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.user-nickname {
  font-size: 14px;
  color: #E2E8F0;
  font-weight: 500;
}

.arrow-icon {
    font-size: 12px;
    color: #94A3B8;
  }

.subsystem-header {
  text-align: center;
  margin-bottom: 56px;
  z-index: 10;
  position: relative;
}

.subsystem-header h1 {
  margin: 0 0 14px;
  font-size: 48px;
  font-weight: 700;
  background: linear-gradient(135deg, #FFFFFF 0%, #60A5FA 50%, #8B5CF6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 4px;
}

.subsystem-header p {
  margin: 0;
  font-size: 18px;
  color: #94A3B8;
  letter-spacing: 2px;
}

.subsystem-cards {
  display: flex;
  gap: 28px;
  flex-wrap: wrap;
  justify-content: center;
  max-width: 1240px;
  z-index: 10;
  position: relative;
}

.subsystem-card {
  width: 360px;
  background: rgba(15, 23, 42, 0.02) !important;
  backdrop-filter: blur(10px);
  border: 1.5px solid rgba(96, 165, 250, 0.4);
  border-radius: 20px;
  padding: 32px;
  display: flex;
  align-items: center;
  cursor: pointer;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  transition: all 0.25s ease;
  z-index: 1;

  &:hover {
    transform: translateY(-8px);
    border-color: rgba(96, 165, 250, 0.6);
    box-shadow: 0 20px 60px rgba(96, 165, 250, 0.2);
    background: rgba(15, 23, 42, 0.05) !important;
  }
}

.card-icon {
  width: 72px;
  height: 72px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.card-icon .svg-icon {
  width: 36px;
  height: 36px;
  color: #fff;
}

.basic-icon {
  background: linear-gradient(135deg, #3B82F6 0%, #1D4ED8 100%);
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.35);
}

.knowledge-icon {
  background: linear-gradient(135deg, #8B5CF6 0%, #6D28D9 100%);
  box-shadow: 0 8px 24px rgba(139, 92, 246, 0.35);
}

.oa-icon {
  background: linear-gradient(135deg, #10B981 0%, #059669 100%);
  box-shadow: 0 8px 24px rgba(16, 185, 129, 0.35);
}

.ai-icon {
  background: linear-gradient(135deg, #F59E0B 0%, #D97706 100%);
  box-shadow: 0 8px 24px rgba(245, 158, 11, 0.35);
}

.mcp-icon {
  background: linear-gradient(135deg, #14B8A6 0%, #0F766E 100%);
  box-shadow: 0 8px 24px rgba(20, 184, 166, 0.35);
}

.card-content {
  flex: 1;
  margin-left: 20px;
}

.card-content h3 {
  margin: 0 0 8px;
  font-size: 20px;
  color: #F1F5F9;
  font-weight: 600;
}

.card-content p {
  margin: 0;
  color: #94A3B8;
  line-height: 1.6;
  font-size: 14px;
}

.card-arrow {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(59, 130, 246, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #60A5FA;
  transition: all 0.25s ease;

  .subsystem-card:hover & {
    background: rgba(59, 130, 246, 0.25);
    transform: translateX(4px);
  }
}

.empty-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 60px;
  background: rgba(15, 23, 42, 0.02) !important;
  backdrop-filter: blur(10px);
  border: 1.5px solid rgba(96, 165, 250, 0.4);
  border-radius: 20px;
  text-align: center;
  z-index: 1;
  position: relative;
}

.subsystem-footer {
  margin-top: 56px;
  color: #64748B;
  font-size: 14px;
  z-index: 10;
  position: relative;
}

@media (max-width: 768px) {
  .subsystem-header h1 {
    font-size: 32px;
  }
  .subsystem-card {
    width: 100%;
    max-width: 380px;
  }
}
</style>
