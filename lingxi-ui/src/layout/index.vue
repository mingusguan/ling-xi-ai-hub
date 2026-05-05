<template>
  <div :class="classObj" class="app-wrapper" :style="{'--current-color': theme}">
    <div v-if="device==='mobile'&&sidebar.opened" class="drawer-bg" @click="handleClickOutside"/>
    <sidebar v-if="!sidebar.hide" class="sidebar-container"/>
    <xiao-ling-robot/>
    <div :class="{hasTagsView:needTagsView,sidebarHide:sidebar.hide}" class="main-container">
      <div :class="{'fixed-header':fixedHeader}">
        <navbar @setLayout="setLayout"/>
        <tags-view v-if="needTagsView"/>
      </div>
      <app-main/>
      <settings ref="settingRef"/>
    </div>
  </div>
</template>

<script>
import { AppMain, Navbar, Settings, Sidebar, TagsView } from './components'
import XiaoLingRobot from '@/components/XiaoLingRobot'
import ResizeMixin from './mixin/ResizeHandler'
import { mapState } from 'vuex'

export default {
  name: 'Layout',
  components: {
    AppMain,
    Navbar,
    Settings,
    Sidebar,
    TagsView,
    XiaoLingRobot
  },
  mixins: [ResizeMixin],
  computed: {
    ...mapState({
      theme: state => state.settings.theme,
      sideTheme: state => state.settings.sideTheme,
      sidebar: state => state.app.sidebar,
      device: state => state.app.device,
      needTagsView: state => state.settings.tagsView,
      fixedHeader: state => state.settings.fixedHeader
    }),
    classObj() {
      return {
        hideSidebar: !this.sidebar.opened,
        openSidebar: this.sidebar.opened,
        withoutAnimation: this.sidebar.withoutAnimation,
        mobile: this.device === 'mobile'
      }
    }
  },
  methods: {
    handleClickOutside() {
      this.$store.dispatch('app/closeSideBar', { withoutAnimation: false })
    },
    setLayout(setting) {
      this.$refs.settingRef.setLayout(setting)
    }
  }
}
</script>

<style lang="scss">
@import "~@/assets/styles/mixin.scss";
@import "~@/assets/styles/variables.scss";

.app-wrapper {
  position: relative;
  height: 100%;
  width: 100%;
  background: 
    radial-gradient(ellipse at 110px 70px, 
      rgba(59, 130, 246, 0.12) 0%, 
      rgba(139, 92, 246, 0.06) 15%, 
      rgba(59, 130, 246, 0.03) 25%, 
      rgba(59, 130, 246, 0.015) 35%, 
      rgba(59, 130, 246, 0.008) 50%, 
      transparent 70%),
    linear-gradient(135deg, #0F172A 0%, #1E293B 50%, #0F172A 100%);
}

// 整体统一深色科技风
.main-container,
.app-main,
.el-main,
.el-container {
  background: transparent !important;
}

// 深色科技风卡片
.el-card {
  background: rgba(30, 41, 59, 0.7) !important;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(59, 130, 246, 0.15) !important;
  border-radius: 8px !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1), 0 0 20px rgba(59, 130, 246, 0.03);
  color: #E2E8F0;
  transition: all 0.3s ease;

  &:hover {
    border-color: rgba(59, 130, 246, 0.25) !important;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15), 0 0 30px rgba(59, 130, 246, 0.08);
  }

  .el-card__header {
    border-bottom: 1px solid rgba(59, 130, 246, 0.1) !important;
    color: #E2E8F0;
    font-weight: 600;
    background: rgba(59, 130, 246, 0.02);
  }

  .el-card__body {
    color: #94A3B8;
  }
}

.drawer-bg {
  background: #000;
  opacity: 0.5;
  width: 100%;
  top: 0;
  height: 100%;
  position: absolute;
  z-index: 999;
}

.main-container {
  min-height: 100%;
  transition: margin-left $transition-base;
  margin-left: $sidebar-width;
  position: relative;
  overflow: hidden;
}

.hasTagsView {
  .main-container {
    min-height: calc(100vh - 34px);
  }
}

.hideSidebar .main-container {
  margin-left: $sidebar-collapse-width;
}

.sidebarHide .main-container {
  margin-left: 0;
}

.mobile {
  .main-container {
    margin-left: 0;
  }
  .hideSidebar .main-container {
    margin-left: $sidebar-width;
  }
}
</style>
