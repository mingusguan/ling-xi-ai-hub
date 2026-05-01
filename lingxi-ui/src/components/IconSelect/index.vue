<!-- @author zhengjie -->
<template>
  <div class="icon-body">
    <el-input v-model="name" class="icon-search" clearable placeholder="请输入图标名称" @clear="filterIcons" @input="filterIcons">
      <i slot="suffix" class="el-icon-search el-input__icon" />
    </el-input>
    <div class="icon-list">
      <div class="list-container">
        <div v-for="(item, index) in iconList" class="icon-item-wrapper" :key="index" @click="selectedIcon(item)">
          <div :class="['icon-item', { active: activeIcon === item }]">
            <svg-icon :icon-class="item" class-name="icon" style="height: 25px;width: 16px;"/>
            <span>{{ item }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import icons from './requireIcons'
export default {
  name: 'IconSelect',
  props: {
    activeIcon: {
      type: String
    }
  },
  data() {
    return {
      name: '',
      iconList: icons
    }
  },
  methods: {
    filterIcons() {
      this.iconList = icons
      if (this.name) {
        this.iconList = this.iconList.filter(item => item.includes(this.name))
      }
    },
    selectedIcon(name) {
      this.$emit('selected', name)
      document.body.click()
    },
    reset() {
      this.name = ''
      this.iconList = icons
    }
  }
}
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
  .icon-body {
    width: 100%;
    padding: 10px;
    background: rgba(15, 23, 42, 0.95);
    border-radius: 8px;
    .icon-search {
      position: relative;
      margin-bottom: 5px;
      :deep(.el-input__inner) {
        background: rgba(30, 41, 59, 0.8) !important;
        border: 1px solid rgba(59, 130, 246, 0.25) !important;
        color: #E2E8F0 !important;
        border-radius: 4px;
      }
      :deep(.el-input__inner::placeholder) {
        color: #64748B !important;
      }
      :deep(.el-input__suffix) {
        color: #94A3B8 !important;
      }
    }
    .icon-list {
      height: 200px;
      overflow: auto;
      .list-container {
        display: flex;
        flex-wrap: wrap;
        .icon-item-wrapper {
          width: calc(100% / 3);
          height: 25px;
          line-height: 25px;
          cursor: pointer;
          display: flex;
          .icon-item {
            display: flex;
            max-width: 100%;
            height: 100%;
            padding: 0 5px;
            color: #E2E8F0;
            &:hover {
              background: rgba(59, 130, 246, 0.2);
              border-radius: 5px;
            }
            .icon {
              flex-shrink: 0;
              color: #94A3B8;
            }
            span {
              display: inline-block;
              vertical-align: -0.15em;
              fill: currentColor;
              padding-left: 2px;
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
              font-size: 12px;
            }
          }
          .icon-item.active {
            background: rgba(59, 130, 246, 0.3);
            border-radius: 5px;
            .icon {
              color: #60A5FA;
            }
          }
        }
      }
    }
  }
</style>
