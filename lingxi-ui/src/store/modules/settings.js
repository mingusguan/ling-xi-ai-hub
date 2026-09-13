import defaultSettings from '@/settings'
import { useDynamicTitle } from '@/utils/dynamicTitle'

const { sideTheme, showSettings, navType, tagsView, tagsIcon, fixedHeader, sidebarLogo, dynamicTitle, footerVisible, footerContent } = defaultSettings

// 主色与 assets/styles/variables.scss 的 $primary (#3B82F6) 保持一致。
// 旧值 #409EFF 是 RuoYi 默认蓝，与深色主题里的 $primary 不同，
// 导致菜单高亮、标签、按钮同屏出现两种蓝，视觉上"不统一"。
export const DEFAULT_THEME = '#3B82F6'

const storageSetting = JSON.parse(localStorage.getItem('layout-setting')) || ''
// 早期版本把主色存成 RuoYi 默认蓝 #409EFF，会让同屏出现两种蓝，读取时统一纠正到当前主色
const storedTheme = String(storageSetting.theme || '').toLowerCase()
const state = {
  title: '',
  theme: storedTheme && storedTheme !== '#409eff' ? storageSetting.theme : DEFAULT_THEME,
  sideTheme: storageSetting.sideTheme || sideTheme,
  showSettings: showSettings,
  navType: storageSetting.navType === undefined ? navType : storageSetting.navType,
  tagsView: storageSetting.tagsView === undefined ? tagsView : storageSetting.tagsView,
  tagsIcon: storageSetting.tagsIcon === undefined ? tagsIcon : storageSetting.tagsIcon,
  fixedHeader: storageSetting.fixedHeader === undefined ? fixedHeader : storageSetting.fixedHeader,
  sidebarLogo: storageSetting.sidebarLogo === undefined ? sidebarLogo : storageSetting.sidebarLogo,
  dynamicTitle: storageSetting.dynamicTitle === undefined ? dynamicTitle : storageSetting.dynamicTitle,
  footerVisible: storageSetting.footerVisible === undefined ? footerVisible : storageSetting.footerVisible,
  footerContent: footerContent
}
const mutations = {
  CHANGE_SETTING: (state, { key, value }) => {
    if (state.hasOwnProperty(key)) {
      state[key] = value
    }
  },
  SET_TITLE: (state, title) => {
    state.title = title
  }
}

const actions = {
  // 修改布局设置
  changeSetting({ commit }, data) {
    commit('CHANGE_SETTING', data)
  },
  // 设置网页标题
  setTitle({ commit }, title) {
    commit('SET_TITLE', title)
    useDynamicTitle()
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}

