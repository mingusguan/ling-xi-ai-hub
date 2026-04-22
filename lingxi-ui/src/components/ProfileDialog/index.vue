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
  border-bottom: 1px solid #ebeef5;
  overflow: hidden;
}
.list-group-striped .list-group-item:nth-of-type(odd) {
  background-color: #f9f9f9;
}
.pull-right {
  float: right;
}
</style>
