<template>
  <div class="sidebar-logo-container" :class="{'collapse':collapse}">
    <transition name="sidebarLogoFade">
      <router-link v-if="collapse" key="collapse" class="sidebar-logo-link" to="/">
        <div class="logo-icon-collapse">
          <svg viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg" style="filter: drop-shadow(0 0 6px rgba(96, 165, 250, 0.9));">
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
      </router-link>
      <router-link v-else key="expand" class="sidebar-logo-link" to="/">
        <div class="logo-icon">
          <svg viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg" style="filter: drop-shadow(0 0 8px rgba(96, 165, 250, 0.8));">
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
        <h1 class="sidebar-title">灵犀AIHub</h1>
      </router-link>
    </transition>
  </div>
</template>

<script>
export default {
  name: 'SidebarLogo',
  props: {
    collapse: {
      type: Boolean,
      required: true
    }
  }
}
</script>

<style lang="scss" scoped>

.sidebarLogoFade-enter-active {
  transition: opacity 1.5s;
}

.sidebarLogoFade-enter,
.sidebarLogoFade-leave-to {
  opacity: 0;
}
</style>
