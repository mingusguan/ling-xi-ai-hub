<template>
    <div :class="{'has-logo':showLogo}" :style="{ backgroundColor: settings.sideTheme === 'theme-dark' ? variables.menuBackground : variables.menuLightBackground }">
        <logo v-if="showLogo" :collapse="isCollapse" />
        <el-scrollbar :class="settings.sideTheme" wrap-class="scrollbar-wrapper">
            <el-menu
                v-if="menuReady"
                :default-active="$route.path.replace(/\/$/, '')"
                :collapse="isCollapse"
                :background-color="settings.sideTheme === 'theme-dark' ? variables.menuBackground : variables.menuLightBackground"
                :text-color="settings.sideTheme === 'theme-dark' ? variables.menuColor : variables.menuLightColor"
                :unique-opened="true"
                :active-text-color="settings.theme"
                :collapse-transition="false"
                mode="vertical"
                :key="$route.path.replace(/\/$/, '')"
            >
                <sidebar-item
                    v-for="(route, index) in sidebarRouters"
                    :key="route.path  + index"
                    :item="route"
                    :base-path="route.path"
                />
            </el-menu>
        </el-scrollbar>
    </div>
</template>

<script>
import { mapGetters, mapState } from "vuex"
import Logo from "./Logo"
import SidebarItem from "./SidebarItem"
// 临时注释：从 JS 导入 scss 会导致 sass-loader 无限循环！
// import variables from "@/assets/styles/variables.scss"

export default {
    components: { SidebarItem, Logo },
    data() {
        return {
            menuReady: false
        }
    },
    mounted() {
        // 确保菜单渲染完成后设置激活状态
        this.$nextTick(() => {
            this.menuReady = true
        })
    },
    watch: {
        sidebarRouters: {
            immediate: true,
            handler(val) {
                if (val && val.length > 0 && !this.menuReady) {
                    // 关键：等 DOM 渲染菜单后再匹配高亮
                    this.$nextTick(() => {
                        setTimeout(() => {
                            this.menuReady = true
                        }, 10)
                    })
                }
            }
        },
        // 监听路由变化，确保菜单高亮
        '$route.path': {
            handler(newPath) {
                if (this.menuReady) {
                    this.$nextTick(() => {
                        // 强制更新菜单激活状态
                        const menu = this.$el.querySelector('.el-menu')
                        if (menu && menu.__vue__) {
                            // 去除尾部斜杠，统一路径格式
                            let activePath = newPath.replace(/\/$/, '')
                            // 处理一级菜单的重定向情况
                            if (activePath === '/oa') {
                                activePath = '/oa/dashboard'
                            }
                            menu.__vue__.activeIndex = activePath
                        }
                    })
                }
            },
            immediate: true
        }
    },
    computed: {
        ...mapState(["settings"]),
        ...mapGetters(["sidebarRouters", "sidebar"]),
        showLogo() {
            return this.$store.state.settings.sidebarLogo
        },
        variables() {
            return {
                menuBackground: 'transparent',
                menuLightBackground: '#ffffff',
                menuColor: '#bfcbd9',
                menuLightColor: '#303133'
            }
        },
        isCollapse() {
            return !this.sidebar.opened
        }
  }
}
</script>

<style lang="scss" scoped>
.has-logo,
.el-scrollbar {
  background: 
    radial-gradient(ellipse at 100px 0px, 
      rgba(59, 130, 246, 0.15) 0%, 
      rgba(139, 92, 246, 0.08) 15%, 
      rgba(59, 130, 246, 0.04) 25%, 
      rgba(59, 130, 246, 0.02) 40%, 
      transparent 60%),
    rgba(17, 25, 40, 0.9);
}

::v-deep .el-menu {
  background-color: transparent !important;
}

::v-deep .el-submenu .el-menu {
  background-color: transparent !important;
}

::v-deep .el-submenu__title:hover {
  background-color: rgba(59, 130, 246, 0.15) !important;
}

::v-deep .el-menu-item:hover,
::v-deep .el-menu-item:focus {
  background-color: rgba(59, 130, 246, 0.15) !important;
}
</style>


