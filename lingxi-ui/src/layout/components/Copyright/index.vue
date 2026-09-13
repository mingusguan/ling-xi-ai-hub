<template>
  <footer v-if="visible" class="copyright">
    <span>{{ content }}</span>
  </footer>
</template>

<script>
export default {
  computed: {
    visible() {
      return this.$store.state.settings.footerVisible
    },
    content() {
      return this.$store.state.settings.footerContent
    }
  }
}
</script>

<style scoped>
.copyright {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 36px;
  padding: 10px 20px;
  text-align: right;
  background-color: rgba(11, 18, 32, 0.94);
  color: #A3B2C7;
  font-size: 14px;
  border-top: 1px solid rgba(148, 178, 255, 0.12);
  backdrop-filter: blur(12px);
  z-index: 999;
}
</style>