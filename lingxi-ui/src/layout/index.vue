<template>
  <div :class="classObj" class="app-wrapper" :style="{'--current-color': theme}">
    <div class="glow-layer glow-layer-1"></div>
    <div class="glow-layer glow-layer-2"></div>
    <div class="glow-layer glow-layer-3"></div>
    <div v-if="device==='mobile'&&sidebar.opened" class="drawer-bg" @click="handleClickOutside"/>
    <sidebar v-if="!sidebar.hide" class="sidebar-container"/>
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
import ResizeMixin from './mixin/ResizeHandler'
import { mapState } from 'vuex'

export default {
  name: 'Layout',
  components: {
    AppMain,
    Navbar,
    Settings,
    Sidebar,
    TagsView
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
  overflow: hidden;
  background: $bg-color;
}

.drawer-bg {
  background: #000;
  opacity: 0.3;
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
