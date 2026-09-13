<template>
  <section class="app-main">
    <transition name="fade-transform" mode="out-in">
      <keep-alive :include="cachedViews">
        <router-view v-if="showMainRouterView" :key="key" />
      </keep-alive>
    </transition>
    <iframe-toggle />
    <copyright />
  </section>
</template>

<script>
import copyright from "./Copyright/index"
import iframeToggle from "./IframeToggle/index"
import { isHttp } from '@/utils/validate'

export default {
  name: 'AppMain',
  components: { iframeToggle, copyright },
  computed: {
    cachedViews() {
      return this.$store.state.tagsView.cachedViews
    },
    key() {
      return this.$route.path
    },
    /** 仅在外链 iframe（http/https）时隐藏主 router-view；meta.link 若为内部路径，不能再隐藏否则页面空白且无请求 */
    showMainRouterView() {
      const link = this.$route.meta && this.$route.meta.link
      return !link || !isHttp(link)
    }
  },
  watch: {
    $route() {
      this.addIframe()
    }
  },
  mounted() {
    this.addIframe()
  },
  methods: {
    addIframe() {
      const { name } = this.$route
      const link = this.$route.meta && this.$route.meta.link
      if (name && link && isHttp(link)) {
        this.$store.dispatch('tagsView/addIframeView', this.$route)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.app-main {
  min-height: calc(100vh - 64px);
  width: 100%;
  position: relative;
  // 页面内容的滚动容器。此处原为 overflow: hidden，配合 fixed-header 下的固定高度，
  // 会把超出视口的内容直接裁掉且无处可滚（运营总览底部卡片、安全门禁列表被截断即此原因）。
  overflow-y: auto;
  overflow-x: hidden;
  box-sizing: border-box;
  background: transparent !important;
}

.fixed-header + .app-main {
  overflow-y: auto;
  overflow-x: hidden;
  height: calc(100vh - 64px);
  min-height: 0px;
  box-sizing: border-box;
  background: transparent !important;
}

.app-main:has(.copyright) {
}

.hasTagsView {
  .app-main {
    min-height: calc(100vh - 98px);
    background: transparent !important;
  }

  .fixed-header + .app-main {
    overflow-y: auto;
    overflow-x: hidden;
    height: calc(100vh - 98px);
    min-height: 0px;
    box-sizing: border-box;
    background: transparent !important;
  }
}

/* 移动端fixed-header优化 */
@media screen and (max-width: 991px) {
  .fixed-header + .app-main {
    padding-bottom: max(60px, calc(constant(safe-area-inset-bottom) + 40px));
    padding-bottom: max(60px, calc(env(safe-area-inset-bottom) + 40px));
    overscroll-behavior-y: none;
  }

  .hasTagsView .fixed-header + .app-main {
    padding-bottom: max(60px, calc(constant(safe-area-inset-bottom) + 40px));
    padding-bottom: max(60px, calc(env(safe-area-inset-bottom) + 40px));
    overscroll-behavior-y: none;
  }
}

@supports (-webkit-touch-callout: none) {
  @media screen and (max-width: 991px) {
    .fixed-header + .app-main {
      padding-bottom: max(17px, calc(constant(safe-area-inset-bottom) + 10px));
      padding-bottom: max(17px, calc(env(safe-area-inset-bottom) + 10px));
      height: calc(100svh - 50px);
      height: calc(100dvh - 50px);
    }

    .hasTagsView .fixed-header + .app-main {
      padding-bottom: max(17px, calc(constant(safe-area-inset-bottom) + 10px));
      padding-bottom: max(17px, calc(env(safe-area-inset-bottom) + 10px));
      height: calc(100svh - 84px);
      height: calc(100dvh - 84px);
    }
  }
}
</style>

<style lang="scss">
// 滚动条配色：深色主题下原实现（浅色 track + 亮蓝 thumb）反差过大，
// 统一为细的浅蓝半透明滑块、透明轨道。
// 说明：global.scss 用 * 把滚动条整体 display:none，该规则由 dark-theme.scss
// 在可滚动容器内恢复为可见的细滚动条。
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background-color: transparent;
}

::-webkit-scrollbar-thumb {
  background-color: rgba(148, 178, 255, 0.3);
  border-radius: 9999px;
}

::-webkit-scrollbar-thumb:hover {
  background-color: rgba(148, 178, 255, 0.5);
}
</style>
