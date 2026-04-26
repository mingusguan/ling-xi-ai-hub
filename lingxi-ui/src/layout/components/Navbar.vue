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

<style lang="scss" src="./Navbar.scss" scoped></style>
