<template>
  <div class="navbar" :class="'nav' + navType">
    <hamburger id="hamburger-container" :is-active="sidebar.opened" class="hamburger-container" @toggleClick="toggleSideBar" />

    <breadcrumb v-if="navType == 1" id="breadcrumb-container" class="breadcrumb-container" />
    <top-nav v-if="navType == 2" id="topmenu-container" class="topmenu-container" />
    <template v-if="navType == 3">
      <logo v-show="showLogo" :collapse="false"></logo>
      <top-bar id="topbar-container" class="topbar-container" />
    </template>
    <div class="right-menu">
      <template v-if="device!=='mobile'">
        <search id="header-search" class="right-menu-item" />

        <el-badge :value="unreadReminderCount" :hidden="unreadReminderCount === 0" type="primary" class="reminder-badge">
          <el-tooltip content="消息中心" effect="dark" placement="bottom">
            <i class="el-icon-bell reminder-icon right-menu-item hover-effect" @click="goToReminder"></i>
          </el-tooltip>
        </el-badge>

        <screenfull id="screenfull" class="right-menu-item hover-effect" />

      </template>

      <el-dropdown class="avatar-container hover-effect" trigger="hover">
        <div class="avatar-wrapper">
          <img :src="avatar" class="user-avatar">
          <span class="user-nickname"> {{ nickName }} </span>
          <i class="el-icon-caret-bottom"></i>
        </div>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item @click.native="openProfile">个人中心</el-dropdown-item>
          <el-dropdown-item @click.native="setLayout" v-if="setting">
            <span>布局设置</span>
          </el-dropdown-item>
          <el-dropdown-item divided @click.native="logout">
            <span>退出登录</span>
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
    <ProfileDialog ref="profileDialog" v-model="profileVisible" />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import Breadcrumb from '@/components/Breadcrumb'
import TopNav from '@/components/TopNav'
import TopBar from './TopBar'
import Logo from './Sidebar/Logo'
import Hamburger from '@/components/Hamburger'
import Screenfull from '@/components/Screenfull'
import SizeSelect from '@/components/SizeSelect'
import Search from '@/components/HeaderSearch'
import RuoYiGit from '@/components/RuoYi/Git'
import RuoYiDoc from '@/components/RuoYi/Doc'
import ProfileDialog from '@/components/ProfileDialog'
import { getUnreadMessageCount } from '@/api/system/message'
import { withPublicPath } from '@/utils/appPath'

export default {
  emits: ['setLayout'],
  components: {
    Breadcrumb,
    Logo,
    TopNav,
    TopBar,
    Hamburger,
    Screenfull,
    SizeSelect,
    Search,
    RuoYiGit,
    RuoYiDoc,
    ProfileDialog
  },
  data() {
    return {
      unreadReminderCount: 0,
      profileVisible: false
    }
  },
  computed: {
    ...mapGetters([
      'sidebar',
      'avatar',
      'device',
      'nickName'
    ]),
    setting: {
      get() {
        return this.$store.state.settings.showSettings
      }
    },
    navType: {
      get() {
        return this.$store.state.settings.navType
      }
    },
    showLogo: {
      get() {
        return this.$store.state.settings.sidebarLogo
      }
    }
  },
  mounted() {
    this.loadUnreadReminderCount()
    window.addEventListener('openProfileDialog', this.handleOpenProfileDialog)
  },
  beforeDestroy() {
    window.removeEventListener('openProfileDialog', this.handleOpenProfileDialog)
  },
  methods: {
    handleOpenProfileDialog(e) {
      this.profileVisible = true
      if (e.detail && e.detail.tab) {
        this.$nextTick(() => {
          this.$refs.profileDialog.selectedTab = e.detail.tab
        })
      }
    },
    toggleSideBar() {
      this.$store.dispatch('app/toggleSideBar')
    },
    setLayout(event) {
      this.$parent.$refs.settingRef.openSetting()
    },
    loadUnreadReminderCount() {
      getUnreadMessageCount().then(res => {
        this.unreadReminderCount = res.data.count || 0
      }).catch(() => {
        this.unreadReminderCount = 0
      })
    },
    goToReminder() {
      this.$router.push('/message/center')
    },
    openProfile() {
      this.profileVisible = true
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
    }
  }
}
</script>

<style lang="scss" scoped>
.navbar {
  height: 64px;
  line-height: 64px;
  overflow: hidden;
  position: relative;
  background: linear-gradient(90deg, #0F172A 0%, #1E293B 100%) !important;
  border-bottom: 1px solid rgba(59, 130, 246, 0.15) !important;
  box-sizing: border-box;

  .hamburger-container {
    line-height: 64px;
    height: 100%;
    float: left;
    cursor: pointer;
    transition: background .3s;
    -webkit-tap-highlight-color:transparent;

    &:hover {
      background: rgba(59, 130, 246, 0.15)
    }
  }

  .breadcrumb-container {
    float: left;
    line-height: 64px;
  }

  .errLog-container {
    display: inline-block;
    vertical-align: top;
  }

  .right-menu {
    display: flex;
    align-items: center;
    float: right;
    height: 100%;

    &:focus {
      outline: none;
    }

    .right-menu-item {
      display: flex;
      align-items: center;
      padding: 0 8px;
      height: 64px;
      font-size: 18px;
      color: #E2E8F0;

      &.hover-effect {
        cursor: pointer;
        transition: background .2s;

        &:hover {
          background: rgba(59, 130, 246, 0.15)
        }
      }

      & >>> svg {
        vertical-align: middle;
      }
    }

    .reminder-badge {
      display: flex;
      align-items: center;
    }

    .avatar-container {
      margin-right: 30px;
      margin-left: 8px;
      display: flex;
      align-items: center;

      .avatar-wrapper {
        display: flex;
        align-items: center;
        gap: 8px;
        position: relative;

        .user-avatar {
          cursor: pointer;
          width: 40px;
          height: 40px;
          border-radius: 10px;
        }

        .user-nickname {
          color: #E2E8F0;
          font-size: 14px;
          line-height: 1;
        }

        .el-icon-caret-bottom {
          cursor: pointer;
          font-size: 12px;
          color: #E2E8F0;
          line-height: 1;
        }
      }
    }
  }
}

// 深色导航栏适配
.navbar {
  ::v-deep .el-breadcrumb__item {
    color: #94A3B8 !important;
  }

  ::v-deep .el-breadcrumb__item:last-child {
    color: #E2E8F0 !important;
  }

  ::v-deep .el-breadcrumb__separator {
    color: #64748B !important;
  }

  ::v-deep .hamburger {
    color: #E2E8F0 !important;
  }

  .el-icon-caret-bottom {
    color: #E2E8F0 !important;
  }
}
</style>
