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
      this.$emit('setLayout')
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
          location.href = '/index'
        })
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
@import "~@/assets/styles/variables.scss";

.navbar.nav3 {
  .hamburger-container {
    display: none !important;
  }
}

.navbar {
  height: 52px;
  overflow: visible;
  position: relative;
  background: rgba(30, 41, 59, 0.7);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid $glass-border;
  display: flex;
  align-items: center;
  padding: 0 $spacing-md;
  box-sizing: border-box;

  .hamburger-container {
    line-height: $navbar-height;
    height: 100%;
    cursor: pointer;
    transition: all $transition-base;
    -webkit-tap-highlight-color: transparent;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    width: 40px;
    height: 40px;
    border-radius: $radius-md;
    margin-right: $spacing-md;
    color: #E2E8F0;

    &:hover {
      background: rgba(59, 130, 246, 0.1);
      color: $primary;
    }
  }

  .breadcrumb-container {
    flex-shrink: 0;

    .el-breadcrumb__item {
      font-weight: $font-weight-medium;

      &:last-child .el-breadcrumb__inner {
        color: $text-color-primary;
        font-weight: $font-weight-semibold;
      }
    }
  }

  .topmenu-container {
    position: absolute;
    left: 50px;
  }

  .topbar-container {
    flex: 1;
    min-width: 0;
    display: flex;
    align-items: center;
    overflow: hidden;
    margin-left: 8px;
  }

  .errLog-container {
    display: inline-block;
    vertical-align: top;
  }

  .right-menu {
    height: 100%;
    display: flex;
    align-items: center;
    margin-left: auto;
    overflow: visible;
    gap: $spacing-sm;

    &:focus {
      outline: none;
    }

    .reminder-badge {
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 40px;
      height: 40px;
      border-radius: $radius-md;
      transition: all $transition-base;

      &:hover {
        background: $primary-50;
      }
    }

    .reminder-icon {
      font-size: 20px;
      color: $gray-600;
      transition: color $transition-base;
    }

    .reminder-badge:hover .reminder-icon {
      color: $primary;
    }

    .right-menu-item {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 40px;
      height: 40px;
      border-radius: $radius-md;
      font-size: 20px;
      color: $gray-600;
      transition: all $transition-base;
    }

    .right-menu-item.hover-effect,
    .hover-effect {
      cursor: pointer;
      transition: all $transition-base;

      &:hover {
        background: rgba(255,255,255,0.1);
        color: $primary;
      }
    }

    .avatar-container {
      cursor: pointer;
      height: 40px;
      padding: 0 8px 0 4px;
      border-radius: $radius-full;
      transition: all $transition-base;

      &:hover {
        background: rgba(255,255,255,0.1);
      }

      .avatar-wrapper {
        display: flex;
        align-items: center;
        gap: 8px;
        height: 40px;

        .user-avatar {
          width: 36px;
          height: 36px;
          border-radius: 50%;
          box-shadow: $shadow-sm;
        }

        .user-nickname {
          max-width: 120px;
          font-size: $font-size-sm;
          font-weight: $font-weight-semibold;
          color: #E2E8F0;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .el-icon-caret-bottom {
          font-size: 12px;
          color: rgba(226, 232, 240, 0.8);
          transition: transform $transition-base;
          flex-shrink: 0;
        }
      }
    }

    .avatar-container:hover .el-icon-caret-bottom {
      transform: translateY(2px);
    }
  }
}

.screenfull-svg {
  width: 20px;
  height: 20px;
}
</style>

<style>
  .el-badge__content.is-fixed {
    position: absolute !important;
    transform: none !important;
    top: 2px !important;
    right: 2px !important;
  }

  .avatar-container .el-dropdown-link {
    display: flex !important;
    align-items: center !important;
    height: 40px !important;
  }
</style>
