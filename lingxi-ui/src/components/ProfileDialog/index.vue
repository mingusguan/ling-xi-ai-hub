<template>
  <el-dialog title="个人中心" :visible.sync="innerVisible" width="900px" append-to-body :close-on-click-modal="false">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card class="box-card">
          <div slot="header" class="clearfix">
            <span>个人信息</span>
          </div>
          <div>
            <div class="text-center">
              <userAvatar ref="avatar" />
            </div>
            <ul class="list-group list-group-striped" style="margin-top: 20px;">
              <li class="list-group-item">
                <svg-icon icon-class="user" />用户名称
                <div class="pull-right">{{ user.userName }}</div>
              </li>
              <li class="list-group-item">
                <svg-icon icon-class="phone" />手机号码
                <div class="pull-right">{{ user.phonenumber || '-' }}</div>
              </li>
              <li class="list-group-item">
                <svg-icon icon-class="email" />用户邮箱
                <div class="pull-right">{{ user.email || '-' }}</div>
              </li>
              <li class="list-group-item" v-if="user.dept">
                <svg-icon icon-class="tree" />所属部门
                <div class="pull-right">{{ user.dept.deptName }} / {{ postGroup }}</div>
              </li>
              <li class="list-group-item">
                <svg-icon icon-class="peoples" />所属角色
                <div class="pull-right">{{ roleGroup }}</div>
              </li>
              <li class="list-group-item">
                <svg-icon icon-class="date" />创建日期
                <div class="pull-right">{{ user.createTime }}</div>
              </li>
            </ul>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card>
          <div slot="header" class="clearfix">
            <span>基本资料</span>
          </div>
          <el-tabs v-model="selectedTab">
            <el-tab-pane label="基本资料" name="userinfo">
              <userInfo ref="info" :user="user" @update-success="handleUpdateSuccess" @close="handleInnerClose" />
            </el-tab-pane>
            <el-tab-pane label="修改密码" name="resetPwd">
              <resetPwd @update-success="handleUpdateSuccess" @close="handleInnerClose" />
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </el-dialog>
</template>

<script>
import userAvatar from '@/views/system/user/profile/userAvatar'
import userInfo from '@/views/system/user/profile/userInfo'
import resetPwd from '@/views/system/user/profile/resetPwd'
import { getUserProfile } from "@/api/system/user"

export default {
  name: "ProfileDialog",
  components: { userAvatar, userInfo, resetPwd },
  model: {
    prop: 'value',
    event: 'input'
  },
  props: {
    value: {
      type: Boolean,
      default: false
    },
    defaultTab: {
      type: String,
      default: 'userinfo'
    }
  },
  data() {
    return {
      user: {},
      roleGroup: {},
      postGroup: {},
      selectedTab: "userinfo",
      innerVisible: false
    }
  },
  watch: {
    value(val) {
      this.innerVisible = val
      if (val) {
        this.selectedTab = this.defaultTab
        this.getUser()
      }
    },
    innerVisible(val) {
      this.$emit('input', val)
    }
  },
  methods: {
    getUser() {
      getUserProfile().then(response => {
        this.user = response.data
        this.roleGroup = response.roleGroup
        this.postGroup = response.postGroup
      })
    },
    handleUpdateSuccess() {
      this.$refs.avatar && this.$refs.avatar.getUserProfile()
      this.$refs.info && this.$refs.info.getUser()
    },
    handleInnerClose() {
      this.innerVisible = false
    }
  }
}
</script>

<style scoped>
.text-center {
  text-align: center;
}
.list-group {
  padding-left: 0;
  margin-bottom: 20px;
}
.list-group-striped .list-group-item {
  padding: 10px 15px;
  border-bottom: 1px solid rgba(96, 165, 250, 0.2);
  overflow: hidden;
  color: #e2e8f0;
}
.list-group-striped .list-group-item:nth-of-type(odd) {
  background-color: rgba(15, 23, 42, 0.6);
}
.pull-right {
  float: right;
  color: #94a3b8;
}

/* 输入框样式 */
::v-deep .el-input__inner {
  background: rgba(15, 23, 42, 0.8);
  border-color: rgba(96, 165, 250, 0.3);
  color: #e2e8f0;
}

::v-deep .el-input__inner::placeholder {
  color: #64748b;
}

::v-deep .el-input__inner:focus {
  border-color: #60a5fa;
  box-shadow: 0 0 0 2px rgba(96, 165, 250, 0.2);
}

/* 卡片样式 */
::v-deep .el-card {
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid rgba(96, 165, 250, 0.2);
  backdrop-filter: blur(10px);
}

::v-deep .el-card__header {
  background: rgba(15, 23, 42, 0.9);
  color: #e2e8f0;
  border-bottom: 1px solid rgba(96, 165, 250, 0.2);
}

/* 标签页样式 */
::v-deep .el-tabs__header {
  margin-bottom: 0;
}

::v-deep .el-tabs__nav {
  color: #94a3b8;
}

::v-deep .el-tabs__active-bar {
  background-color: #60a5fa;
}

::v-deep .el-tabs__item.is-active {
  color: #60a5fa;
}

::v-deep .el-tabs__item:hover {
  color: #93c5fd;
}

/* 按钮样式 */
::v-deep .el-button--primary {
  background: linear-gradient(135deg, #60a5fa, #3b82f6);
  border-color: #60a5fa;
}

::v-deep .el-button--primary:hover {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  border-color: #3b82f6;
}

::v-deep .el-button--danger {
  background: rgba(239, 68, 68, 0.8);
  border-color: rgba(239, 68, 68, 0.6);
}

::v-deep .el-button--danger:hover {
  background: rgba(220, 38, 38, 0.8);
  border-color: rgba(220, 38, 38, 0.6);
}

/* 单选按钮样式 */
::v-deep .el-radio__label {
  color: #e2e8f0;
}

::v-deep .el-radio__input.is-checked .el-radio__inner {
  border-color: #60a5fa;
  background-color: #60a5fa;
}

/* 对话框样式 */
::v-deep .el-dialog {
  background: rgba(15, 23, 42, 0.95);
  border: 1px solid rgba(96, 165, 250, 0.3);
  backdrop-filter: blur(20px);
}

::v-deep .el-dialog__header {
  border-bottom: 1px solid rgba(96, 165, 250, 0.2);
}

::v-deep .el-dialog__title {
  color: #e2e8f0;
}

::v-deep .el-dialog__close {
  color: #94a3b8;
}

::v-deep .el-dialog__close:hover {
  color: #60a5fa;
}

/* 表单标签样式 */
::v-deep .el-form-item__label {
  color: #e2e8f0;
}
</style>
