<template>
  <el-drawer size="280px" :visible="showSettings" :with-header="false" :append-to-body="true" :before-close="closeSetting" :lock-scroll="false">
    <div class="drawer-container">
      <div>
        <h3 class="drawer-title">系统布局配置</h3>

        <div class="drawer-item">
          <span>开启 Tags-Views</span>
          <el-switch v-model="tagsView" class="drawer-switch" />
        </div>

        <div class="drawer-item">
          <span>显示页签图标</span>
          <el-switch v-model="tagsIcon" :disabled="!tagsView" class="drawer-switch" />
        </div>

        <div class="drawer-item">
          <span>固定 Header</span>
          <el-switch v-model="fixedHeader" class="drawer-switch" />
        </div>

        <div class="drawer-item">
          <span>显示 Logo</span>
          <el-switch v-model="sidebarLogo" class="drawer-switch" />
        </div>

        <div class="drawer-item">
          <span>动态标题</span>
          <el-switch v-model="dynamicTitle" class="drawer-switch" />
        </div>

        <div class="drawer-item">
          <span>底部版权</span>
          <el-switch v-model="footerVisible" class="drawer-switch" />
        </div>

        <el-divider/>

        <el-button size="small" type="primary" plain icon="el-icon-document-add" @click="saveSetting">保存配置</el-button>
        <el-button size="small" plain icon="el-icon-refresh" @click="resetSetting">重置配置</el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script>
export default {
  expose: ['openSetting'],
  data() {
    return {
      theme: this.$store.state.settings.theme,
      sideTheme: this.$store.state.settings.sideTheme,
      navType: this.$store.state.settings.navType,
      showSettings: false
    }
  },
  computed: {
    fixedHeader: {
      get() {
        return this.$store.state.settings.fixedHeader
      },
      set(val) {
        this.$store.dispatch('settings/changeSetting', {
          key: 'fixedHeader',
          value: val
        })
      }
    },
    tagsView: {
      get() {
        return this.$store.state.settings.tagsView
      },
      set(val) {
        this.$store.dispatch('settings/changeSetting', {
          key: 'tagsView',
          value: val
        })
      }
    },
    tagsIcon: {
      get() {
        return this.$store.state.settings.tagsIcon
      },
      set(val) {
        this.$store.dispatch('settings/changeSetting', {
          key: 'tagsIcon',
          value: val
        })
      }
    },
    sidebarLogo: {
      get() {
        return this.$store.state.settings.sidebarLogo
      },
      set(val) {
        this.$store.dispatch('settings/changeSetting', {
          key: 'sidebarLogo',
          value: val
        })
      }
    },
    dynamicTitle: {
      get() {
        return this.$store.state.settings.dynamicTitle
      },
      set(val) {
        this.$store.dispatch('settings/changeSetting', {
          key: 'dynamicTitle',
          value: val
        })
        this.$store.dispatch('settings/setTitle', this.$store.state.settings.title)
      }
    },
    footerVisible: {
      get() {
        return this.$store.state.settings.footerVisible
      },
      set(val) {
        this.$store.dispatch('settings/changeSetting', {
          key: 'footerVisible',
          value: val
        })
      }
    }
  },
  watch: {
    navType: {
      handler(val) {
        if (val == 1) {
          this.$store.dispatch("app/toggleSideBarHide", false)
        }
        if (val == 3) {
          this.$store.dispatch("app/toggleSideBarHide", true)
        }
        if ([1, 3].includes(val)) {
          this.$store.commit("SET_SIDEBAR_ROUTERS",this.$store.state.permission.defaultRoutes)
        }
      },
      immediate: true,
      deep: true
    }
  },
  methods: {
    handleTheme(val) {
      this.$store.dispatch('settings/changeSetting', {
        key: 'sideTheme',
        value: val
      })
      this.sideTheme = val
    },
    handleNavType(val) {
      this.$store.dispatch('settings/changeSetting', {
        key: 'navType',
        value: val
      })
      this.navType = val
    },
    openSetting() {
      this.showSettings = true
    },
    closeSetting(){
      this.showSettings = false
    },
    saveSetting() {
      this.$modal.loading("正在保存到本地，请稍候...")
      this.$cache.local.set(
        "layout-setting",
        `{
            "navType":${this.navType},
            "tagsView":${this.tagsView},
            "tagsIcon":${this.tagsIcon},
            "fixedHeader":${this.fixedHeader},
            "sidebarLogo":${this.sidebarLogo},
            "dynamicTitle":${this.dynamicTitle},
            "footerVisible":${this.footerVisible},
            "sideTheme":"${this.sideTheme}",
            "theme":"${this.theme}"
          }`
      )
      setTimeout(this.$modal.closeLoading(), 1000)
    },
    resetSetting() {
      this.$modal.loading("正在清除设置缓存并刷新，请稍候...")
      this.$cache.local.remove("layout-setting")
      setTimeout("window.location.reload()", 1000)
    }
  }
}
</script>

<style lang="scss" scoped>
.drawer-container {
  padding: 20px;
  font-size: 14px;
  line-height: 1.5;
  word-wrap: break-word;

  .drawer-title {
    margin-bottom: 12px;
    color: rgba(0, 0, 0, .85);
    font-size: 14px;
    line-height: 22px;
  }

  .drawer-item {
    color: rgba(0, 0, 0, .65);
    font-size: 14px;
    padding: 12px 0;
  }

  .drawer-switch {
    float: right
  }
}
</style>
