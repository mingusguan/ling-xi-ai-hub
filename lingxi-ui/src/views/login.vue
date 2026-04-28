<template>
  <div class="login">
    <div class="bg-svg">
      <svg viewBox="0 0 1920 1080" preserveAspectRatio="xMidYMid slice">
        <defs>
          <filter id="glowStar">
            <feGaussianBlur stdDeviation="6" result="coloredBlur"/>
            <feMerge>
              <feMergeNode in="coloredBlur"/>
              <feMergeNode in="SourceGraphic"/>
            </feMerge>
          </filter>
          <radialGradient id="starGold" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stop-color="#FFFFFF"/>
            <stop offset="50%" stop-color="#E8E8E8"/>
            <stop offset="100%" stop-color="#C0C0C0"/>
          </radialGradient>
          <radialGradient id="starBright" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stop-color="#FFFFFF"/>
            <stop offset="40%" stop-color="#F5F5F5"/>
            <stop offset="100%" stop-color="#D4D4D4"/>
          </radialGradient>
        </defs>
        <g filter="url(#glowStar)">
          <polygon points="200,144 202,150 208,150 203,154 205,160 200,156 195,160 197,154 192,150 198,150" fill="url(#starGold)">
            <animate attributeName="opacity" values="0.3;1;0.3" dur="2s" begin="0s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="400,244 402,250 408,250 403,254 405,260 400,256 395,260 397,254 392,250 398,250" fill="url(#starBright)">
            <animate attributeName="opacity" values="0.25;0.95;0.25" dur="2.5s" begin="0.3s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="600,192 602,200 610,200 604,205 606,213 600,208 594,213 596,205 590,200 598,200" fill="url(#starGold)">
            <animate attributeName="opacity" values="0.35;1;0.35" dur="1.8s" begin="0.6s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="500,340 503,350 513,350 505,357 508,367 500,361 492,367 495,357 487,350 497,350" fill="url(#starGold)">
            <animate attributeName="opacity" values="0.4;1;0.4" dur="1.5s" begin="0.2s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="960,282 964,294 978,294 967,303 970,316 960,309 950,316 953,303 942,294 956,294" fill="url(#starGold)">
            <animate attributeName="opacity" values="0.5;1;0.5" dur="1.2s" begin="0s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="960,516 965,531 983,531 970,542 974,558 960,549 946,558 950,542 937,531 955,531" fill="url(#starBright)">
            <animate attributeName="opacity" values="0.6;1;0.6" dur="1s" begin="0.3s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="1200,438 1204,450 1215,450 1206,458 1209,470 1200,463 1191,470 1194,458 1185,450 1196,450" fill="url(#starGold)">
            <animate attributeName="opacity" values="0.4;1;0.4" dur="1.4s" begin="0.5s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="1300,292 1303,300 1311,300 1304,306 1306,314 1300,309 1294,314 1296,306 1289,300 1297,300" fill="url(#starBright)">
            <animate attributeName="opacity" values="0.35;0.95;0.35" dur="1.9s" begin="0.8s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="1700,341 1703,350 1712,350 1705,356 1707,365 1700,359 1693,365 1695,356 1688,350 1697,350" fill="url(#starGold)">
            <animate attributeName="opacity" values="0.3;1;0.3" dur="1.6s" begin="0.1s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="800,837 804,850 816,850 807,859 810,872 800,864 790,872 793,859 784,850 796,850" fill="url(#starBright)">
            <animate attributeName="opacity" values="0.4;1;0.4" dur="1.7s" begin="0.4s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="400,790 403,800 411,800 404,807 406,815 400,810 394,815 396,807 389,800 397,800" fill="url(#starGold)">
            <animate attributeName="opacity" values="0.3;0.9;0.3" dur="2.2s" begin="0.7s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="1550,742 1552,750 1559,750 1554,755 1556,762 1550,758 1544,762 1546,755 1541,750 1548,750" fill="url(#starBright)">
            <animate attributeName="opacity" values="0.3;0.95;0.3" dur="2s" begin="0.9s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="100,96 101,100 105,100 102,103 103,107 100,104 97,107 98,103 95,100 99,100" fill="#FFFFFF">
            <animate attributeName="opacity" values="0.2;0.8;0.2" dur="3s" begin="0s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="300,77 301,80 305,80 302,83 303,86 300,84 297,86 298,83 295,80 299,80" fill="#F0F0F0">
            <animate attributeName="opacity" values="0.15;0.7;0.15" dur="2.5s" begin="0.5s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="700,86 701,90 705,90 702,93 703,97 700,94 697,97 698,93 695,90 699,90" fill="#FFFFFF">
            <animate attributeName="opacity" values="0.2;0.75;0.2" dur="2.8s" begin="1s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="1300,101 1301,105 1305,105 1302,108 1303,111 1300,108 1297,111 1298,108 1295,105 1299,105" fill="#E8E8E8">
            <animate attributeName="opacity" values="0.18;0.7;0.18" dur="3.2s" begin="0.3s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="1850,126 1852,130 1856,130 1853,133 1854,137 1850,134 1846,137 1847,133 1844,130 1848,130" fill="#FFFFFF">
            <animate attributeName="opacity" values="0.2;0.8;0.2" dur="2.7s" begin="0.7s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="450,277 451,280 454,280 452,283 452,286 450,284 448,286 448,283 446,280 449,280" fill="#F5F5F5">
            <animate attributeName="opacity" values="0.15;0.65;0.15" dur="2.9s" begin="0.2s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="1650,306 1652,310 1655,310 1653,313 1654,316 1650,314 1646,316 1647,313 1645,310 1648,310" fill="#FFFFFF">
            <animate attributeName="opacity" values="0.2;0.7;0.2" dur="3.1s" begin="0.6s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="100,496 101,500 105,500 102,503 103,507 100,504 97,507 98,503 95,500 99,500" fill="#F0F0F0">
            <animate attributeName="opacity" values="0.22;0.8;0.22" dur="2.6s" begin="0.4s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="1800,467 1801,470 1804,470 1802,473 1802,476 1800,474 1798,476 1798,473 1796,470 1799,470" fill="#FFFFFF">
            <animate attributeName="opacity" values="0.18;0.75;0.18" dur="2.4s" begin="0.8s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="500,716 501,720 505,720 502,723 503,727 500,724 497,727 498,723 495,720 499,720" fill="#E8E8E8">
            <animate attributeName="opacity" values="0.2;0.7;0.2" dur="2.3s" begin="0.1s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="1450,687 1451,690 1454,690 1452,693 1452,696 1450,694 1448,696 1448,693 1446,690 1449,690" fill="#FFFFFF">
            <animate attributeName="opacity" values="0.17;0.68;0.17" dur="2.8s" begin="0.5s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="100,896 101,900 105,900 102,903 103,907 100,904 97,907 98,903 95,900 99,900" fill="#F0F0F0">
            <animate attributeName="opacity" values="0.2;0.75;0.2" dur="3s" begin="0.9s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="650,917 651,920 654,920 652,923 652,926 650,924 648,926 648,923 646,920 649,920" fill="#FFFFFF">
            <animate attributeName="opacity" values="0.18;0.7;0.18" dur="2.6s" begin="0.3s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="1550,867 1551,870 1554,870 1552,873 1552,876 1550,874 1548,876 1548,873 1546,870 1549,870" fill="#E8E8E8">
            <animate attributeName="opacity" values="0.2;0.72;0.2" dur="2.9s" begin="0.6s" repeatCount="indefinite"/>
          </polygon>
          <polygon points="1850,927 1851,930 1854,930 1852,933 1852,936 1850,934 1848,936 1848,933 1846,930 1849,930" fill="#FFFFFF">
            <animate attributeName="opacity" values="0.19;0.65;0.19" dur="2.5s" begin="0.8s" repeatCount="indefinite"/>
          </polygon>
        </g>
      </svg>
    </div>

    <div class="bg-gradient"></div>
    <div class="bg-glow glow-1"></div>
    <div class="bg-glow glow-2"></div>
    <div class="bg-glow glow-3"></div>
    <div class="bg-grid"></div>

    <div class="particles">
      <div v-for="n in 60" :key="n" class="particle" :style="{
        width: Math.random() * 5 + 2 + 'px',
        height: Math.random() * 5 + 2 + 'px',
        left: Math.random() * 100 + '%',
        top: 85 + Math.random() * 20 + '%',
        animationDelay: Math.random() * 10 + 's',
        animationDuration: Math.random() * 6 + 10 + 's'
      }"></div>
    </div>

    <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form" @keyup.enter.native="handleLogin">
      <div class="bg-stars">
        <div class="star star-1"></div>
        <div class="star star-2"></div>
        <div class="star star-3"></div>
      </div>
      <div class="corner decoration-tl"></div>
      <div class="corner decoration-tr"></div>
      <div class="corner decoration-bl"></div>
      <div class="corner decoration-br"></div>
      <div class="login-header">
        <div class="logo-icon">
          <svg viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="50" cy="50" r="48" fill="url(#glowBg)" opacity="0.2"/>
            <ellipse cx="50" cy="50" rx="47" ry="47" stroke="url(#ring1)" stroke-width="1.5" stroke-dasharray="10 5" opacity="0.6"/>
            <ellipse cx="50" cy="50" rx="42" ry="42" stroke="url(#ring2)" stroke-width="1" stroke-dasharray="6 4" opacity="0.4">
              <animateTransform attributeName="transform" type="rotate" from="0 50 50" to="360 50 50" dur="25s" repeatCount="indefinite"/>
            </ellipse>
            <ellipse cx="50" cy="50" rx="36" ry="36" stroke="url(#ring3)" stroke-width="1" stroke-dasharray="8 4" opacity="0.45">
              <animateTransform attributeName="transform" type="rotate" from="360 50 50" to="0 50 50" dur="18s" repeatCount="indefinite"/>
            </ellipse>
            <path d="M18 18L18 70L42 54L42 18" fill="url(#lxGrad)"/>
            <path d="M42 54L68 54L68 70L42 70Z" fill="url(#lxGrad)"/>
            <path d="M52 18L82 66M82 18L52 66" stroke="url(#lxGrad)" stroke-width="8.5" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
            <circle cx="82" cy="20" r="2.5" fill="#60A5FA">
              <animate attributeName="opacity" values="0.3;1;0.3" dur="2s" repeatCount="indefinite"/>
            </circle>
            <circle cx="16" cy="80" r="2" fill="#8B5CF6">
              <animate attributeName="opacity" values="0.5;1;0.5" dur="1.5s" repeatCount="indefinite"/>
            </circle>
            <circle cx="85" cy="62" r="2" fill="#3B82F6">
              <animate attributeName="opacity" values="0.4;0.9;0.4" dur="2.5s" repeatCount="indefinite"/>
            </circle>
            <line x1="14" y1="40" x2="6" y2="35" stroke="url(#nodeLine)" stroke-width="1.2" opacity="0.6"/>
            <line x1="86" y1="45" x2="94" y2="40" stroke="url(#nodeLine)" stroke-width="1.2" opacity="0.6"/>
            <line x1="50" y1="3" x2="50" y2="10" stroke="url(#nodeLine)" stroke-width="1.2" opacity="0.5"/>
            <line x1="50" y1="90" x2="50" y2="97" stroke="url(#nodeLine)" stroke-width="1.2" opacity="0.5"/>
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
              <linearGradient id="ring2" x1="100%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stop-color="#3B82F6"/>
                <stop offset="50%" stop-color="#60A5FA"/>
                <stop offset="100%" stop-color="#8B5CF6"/>
              </linearGradient>
              <linearGradient id="ring3" x1="0%" y1="100%" x2="100%" y2="0%">
                <stop offset="0%" stop-color="#8B5CF6"/>
                <stop offset="50%" stop-color="#3B82F6"/>
                <stop offset="100%" stop-color="#60A5FA"/>
              </linearGradient>
              <linearGradient id="lxGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#FFFFFF"/>
                <stop offset="20%" stop-color="#FFFFFF"/>
                <stop offset="50%" stop-color="#60A5FA"/>
                <stop offset="80%" stop-color="#3B82F6"/>
                <stop offset="100%" stop-color="#8B5CF6"/>
              </linearGradient>
              <linearGradient id="nodeLine" x1="0%" y1="0%" x2="100%" y2="0%">
                <stop offset="0%" stop-color="#3B82F6" stop-opacity="0"/>
                <stop offset="50%" stop-color="#60A5FA"/>
                <stop offset="100%" stop-color="#8B5CF6" stop-opacity="0"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <h3 class="title">{{title}}</h3>
        <p class="subtitle">灵犀AIHub · 智能中枢</p>
      </div>

      <el-form-item prop="username">
        <el-input
          v-model="loginForm.username"
          type="text"
          auto-complete="off"
          placeholder="请输入账号"
        >
          <svg-icon slot="prefix" icon-class="user" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          v-model="loginForm.password"
          type="password"
          auto-complete="off"
          placeholder="请输入密码"
        >
          <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item prop="code" v-if="captchaEnabled">
        <el-input
          v-model="loginForm.code"
          auto-complete="off"
          placeholder="请输入验证码"
          class="captcha-input"
          style="width: 63%"
        >
          <svg-icon slot="prefix" icon-class="validCode" class="el-input__icon input-icon"/>
        </el-input>
        <div class="login-code">
          <div class="corner decoration-tl"></div>
          <div class="corner decoration-tr"></div>
          <div class="corner decoration-bl"></div>
          <div class="corner decoration-br"></div>
          <img :src="codeUrl" @click="getCode" class="login-code-img"/>
        </div>
      </el-form-item>
      <el-form-item style="width:100%;">
        <el-button
          :loading="loading"
          size="medium"
          type="primary"
          class="login-btn"
          @click.native.prevent="handleLogin"
        >
          <span v-if="!loading">登 录</span>
          <span v-else>登 录 中...</span>
        </el-button>
        <div style="float: right;" v-if="register">
          <router-link class="link-type" :to="'/register'">立即注册</router-link>
        </div>
      </el-form-item>
    </el-form>

    <div class="el-login-footer">
      <span>{{ footerContent }}</span>
    </div>
  </div>
</template>

<script>
import { getCodeImg } from "@/api/login"
import Cookies from "js-cookie"
import { encrypt, decrypt } from '@/utils/jsencrypt'
import defaultSettings from '@/settings'

export default {
  name: "Login",
  data() {
    return {
      title: process.env.VUE_APP_TITLE,
      footerContent: defaultSettings.footerContent,
      codeUrl: "",
      loginForm: {
        username: "murphy",
        password: "123456",
        rememberMe: false,
        code: "",
        uuid: ""
      },
      loginRules: {
        username: [
          { required: true, trigger: "blur", message: "请输入您的账号" }
        ],
        password: [
          { required: true, trigger: "blur", message: "请输入您的密码" }
        ],
        code: [{ required: true, trigger: "change", message: "请输入验证码" }]
      },
      loading: false,
      captchaEnabled: true,
      register: false,
      redirect: undefined
    }
  },
  watch: {
    $route: {
      handler: function(route) {
        this.redirect = route.query && route.query.redirect
      },
      immediate: true
    }
  },
  created() {
    this.getCode()
    this.getCookie()
  },
  methods: {
    getCode() {
      getCodeImg().then(res => {
        this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled
        if (this.captchaEnabled) {
          this.codeUrl = "data:image/gif;base64," + res.img
          this.loginForm.uuid = res.uuid
        }
      })
    },
    getCookie() {
      const username = Cookies.get("username")
      const password = Cookies.get("password")
      const rememberMe = Cookies.get('rememberMe')
      this.loginForm = {
        username: username === undefined ? this.loginForm.username : username,
        password: password === undefined ? this.loginForm.password : decrypt(password),
        rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
      }
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true
          if (this.loginForm.rememberMe) {
            Cookies.set("username", this.loginForm.username, { expires: 30 })
            Cookies.set("password", encrypt(this.loginForm.password), { expires: 30 })
            Cookies.set('rememberMe', this.loginForm.rememberMe, { expires: 30 })
          } else {
            Cookies.remove("username")
            Cookies.remove("password")
            Cookies.remove('rememberMe')
          }
          this.$store.dispatch("Login", this.loginForm).then(() => {
            this.$router.push({ path: '/subsystem' }).catch(()=>{})
          }).catch(() => {
            this.loading = false
            if (this.captchaEnabled) {
              this.getCode()
            }
          })
        }
      })
    }
  }
}
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
.login {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  position: relative;
  overflow: hidden;
  background: #0B1120;
}

// 深空渐变叠加层
.bg-gradient {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 50% 0%, rgba(59, 130, 246, 0.08) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 80%, rgba(139, 92, 246, 0.06) 0%, transparent 50%),
    radial-gradient(ellipse at 20% 60%, rgba(6, 182, 212, 0.05) 0%, transparent 40%);
  mix-blend-mode: screen;
}

// 发光光晕 - 与背景图叠加增强
.bg-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.8;
  animation: floatGlow 20s ease-in-out infinite;
  mix-blend-mode: screen;

  &.glow-1 {
    width: 600px;
    height: 600px;
    background: radial-gradient(circle, rgba(59, 130, 246, 0.35) 0%, transparent 70%);
    top: -200px;
    left: 5%;
  }

  &.glow-2 {
    width: 500px;
    height: 500px;
    background: radial-gradient(circle, rgba(139, 92, 246, 0.3) 0%, transparent 70%);
    top: 40%;
    right: -150px;
    animation-delay: -7s;
  }

  &.glow-3 {
    width: 450px;
    height: 450px;
    background: radial-gradient(circle, rgba(6, 182, 212, 0.25) 0%, transparent 70%);
    bottom: -150px;
    left: 25%;
    animation-delay: -14s;
  }
}

// 暗角遮罩
.bg-grid {
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at center, transparent 0%, rgba(11, 17, 32, 0.3) 100%);
  pointer-events: none;
}

// SVG星辰背景
.bg-svg {
  position: fixed;
  inset: 0;
  z-index: 1;
  pointer-events: none;

  svg {
    width: 100%;
    height: 100%;
  }
}

// 粒子效果 - 全屏自然漂浮
.particles {
  position: fixed;
  inset: 0;
  z-index: 5;
  pointer-events: none;
  overflow: hidden;
}

.particle {
  position: absolute;
  background: #ffffff;
  border-radius: 50%;
  box-shadow:
    0 0 6px #60A5FA,
    0 0 12px #3B82F6,
    0 0 24px rgba(59, 130, 246, 0.8);
  animation: particleFloat ease-in-out infinite;
  will-change: transform;
}

// 登录框 - 科技感发光边框
.login-form {
  position: relative;
  z-index: 10;
  border-radius: 8px;
  background: rgba(8, 18, 32, 0.02);
  backdrop-filter: blur(0px);
  -webkit-backdrop-filter: blur(0px);
  border: 2px solid rgba(59, 130, 246, 0.15);
  box-shadow:
    0 0 20px rgba(59, 130, 246, 0.05),
    0 0 40px rgba(59, 130, 246, 0.01) inset;
  width: 380px;
  padding: 36px 40px;
  transition: all 0.3s ease;

  // 背景星星装饰
  .bg-stars {
    position: absolute;
    inset: 0;
    overflow: hidden;
    border-radius: 8px;
    pointer-events: none;

    &::before,
    &::after {
      content: '';
      position: absolute;
      width: 4px;
      height: 4px;
      background: #60A5FA;
      border-radius: 50%;
      box-shadow:
        0 0 6px #60A5FA,
        0 0 12px #3B82F6;
    }

    &::before {
      top: 20%;
      left: 15%;
      animation: twinkle 2s ease-in-out infinite;
    }

    &::after {
      top: 60%;
      right: 20%;
      animation: twinkle 2.5s ease-in-out infinite;
      animation-delay: 0.5s;
    }

    .star {
      position: absolute;
      width: 3px;
      height: 3px;
      background: #8B5CF6;
      border-radius: 50%;
      box-shadow:
        0 0 5px #8B5CF6,
        0 0 10px rgba(139, 92, 246, 0.5);

      &.star-1 { top: 30%; right: 25%; animation: twinkle 1.8s ease-in-out infinite; }
      &.star-2 { bottom: 35%; left: 20%; animation: twinkle 2.2s ease-in-out infinite; animation-delay: 0.3s; }
      &.star-3 { top: 50%; left: 10%; animation: twinkle 2s ease-in-out infinite; animation-delay: 0.8s; }
    }
  }

  // 四个角的装饰
  .corner {
    position: absolute;
    width: 20px;
    height: 20px;
    border-color: #3B82F6;
    border-style: solid;
  }

  .decoration-tl {
    top: -2px;
    left: -2px;
    border-width: 1px 0 0 1px;
  }

  .decoration-tr {
    top: -2px;
    right: -2px;
    border-width: 1px 1px 0 0;
  }

  .decoration-bl {
    bottom: -2px;
    left: -2px;
    border-width: 0 0 1px 1px;
  }

  .decoration-br {
    bottom: -2px;
    right: -2px;
    border-width: 0 1px 1px 0;
  }

  &:hover {
    box-shadow:
      0 0 30px rgba(59, 130, 246, 0.3),
      0 0 60px rgba(59, 130, 246, 0.15) inset;
    border-color: rgba(96, 165, 250, 0.6);
  }
}

// 登录头部 - 发光标题
.login-header {
  text-align: center;
  margin-bottom: 36px;

  .logo-icon {
    display: none;
  }

  .title {
    margin: 0 0 24px;
    font-size: 28px;
    font-weight: 700;
    color: #60A5FA;
    letter-spacing: 2px;
    text-shadow:
      0 0 10px rgba(96, 165, 250, 0.8),
      0 0 20px rgba(59, 130, 246, 0.6),
      0 0 30px rgba(59, 130, 246, 0.4);
  }

  .subtitle {
    display: none;
  }
}

// Element UI 输入框样式 - 半透明
.login-form :deep(.el-input) {
  .el-input__inner {
    background: rgba(15, 23, 42, 0.01) !important;
    border: 1px solid rgba(59, 130, 246, 0.1) !important;
    border-radius: 4px !important;
    color: #E2E8F0 !important;
    -webkit-text-fill-color: #E2E8F0 !important;
    font-weight: 400 !important;
    font-size: 14px !important;
    transition: all 0.3s ease !important;

    &:hover {
      border-color: rgba(96, 165, 250, 0.3) !important;
      box-shadow: 0 0 8px rgba(59, 130, 246, 0.1) !important;
    }
  }

  &.is-focus .el-input__inner {
    border-color: #60A5FA !important;
    background: rgba(15, 23, 42, 0.1) !important;
    box-shadow: 0 0 12px rgba(59, 130, 246, 0.2) !important;
  }

  .el-input__inner::placeholder {
    color: rgba(148, 163, 184, 0.9) !important;
    -webkit-text-fill-color: rgba(148, 163, 184, 0.9) !important;
  }
}

// Element UI 输入框图标颜色
.login-form :deep(.el-input__prefix .el-input__icon),
.login-form :deep(.el-input__icon) {
  color: #60A5FA !important;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 4px;
  background: rgb(96, 165, 250);
  border: 1px solid rgb(96, 165, 250);
  border-radius: 4px;
  box-shadow: 
    0 0 16px rgba(96, 165, 250, 0.5);
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    background: rgba(96, 165, 250, 0.9);
    box-shadow: 
      0 0 24px rgba(96, 165, 250, 0.7);
    filter: brightness(1.1);
  }

  &:active {
    transform: translateY(0);
  }
}

.login-tip {
  font-size: 13px;
  text-align: center;
  color: #64748B;
}

.login-code {
  width: 33%;
  height: 40px;
  float: right;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(8, 18, 32, 0.02);
  border-radius: 8px;
  position: relative;
  box-sizing: border-box;

  .corner {
    position: absolute;
    width: 12px;
    height: 12px;
    border-color: #3B82F6;
    border-style: solid;
  }

  .decoration-tl {
    top: -2px;
    left: -2px;
    border-width: 1px 0 0 1px;
  }

  .decoration-tr {
    top: -2px;
    right: -2px;
    border-width: 1px 1px 0 0;
  }

  .decoration-bl {
    bottom: -2px;
    left: -2px;
    border-width: 0 0 1px 1px;
  }

  .decoration-br {
    bottom: -2px;
    right: -2px;
    border-width: 0 1px 1px 0;
  }

  img {
    cursor: pointer;
    vertical-align: middle;
    height: 36px;
    border-radius: 4px;
  }
}

.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: rgba(148, 163, 184, 0.6);
  font-family: Arial;
  font-size: 12px;
  letter-spacing: 1px;
  z-index: 1;
}

@keyframes floatGlow {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  33% {
    transform: translate(30px, -20px) scale(1.1);
  }
  66% {
    transform: translate(-20px, 15px) scale(0.95);
  }
}

@keyframes particleFloat {
  0% {
    transform: translate(0, 0) scale(1);
    opacity: 0;
  }
  10% {
    opacity: 1;
  }
  25% {
    transform: translate(60px, -25vh) scale(1.2);
  }
  50% {
    transform: translate(-40px, -50vh) scale(0.9);
  }
  75% {
    transform: translate(50px, -75vh) scale(1.1);
    opacity: 0.9;
  }
  100% {
    transform: translate(-20px, -120vh) scale(0.5);
    opacity: 0;
  }
}

@keyframes twinkle {
  0%, 100% {
    opacity: 0.3;
    transform: scale(0.8);
  }
  50% {
    opacity: 1;
    transform: scale(1.2);
  }
}
</style>

<!-- 强制覆盖Element UI输入框样式 -->
<style rel="stylesheet/scss" lang="scss">
.login .login-form .el-input__inner {
  background-color: rgba(15, 25, 40, 0.35) !important;
  border-color: rgba(59, 130, 246, 0.4) !important;
  color: #E2E8F0 !important;
  -webkit-text-fill-color: #E2E8F0 !important;
}

.login .login-form .el-input__inner:hover {
  border-color: rgba(96, 165, 250, 0.7) !important;
  box-shadow: 0 0 12px rgba(59, 130, 246, 0.3) !important;
}

.login .login-form .el-input.is-focus .el-input__inner {
  background-color: rgba(15, 25, 40, 0.5) !important;
  border-color: #60A5FA !important;
  box-shadow: 0 0 18px rgba(59, 130, 246, 0.4) !important;
}

.login .login-form .el-input__inner::placeholder {
  color: rgba(148, 163, 184, 0.85) !important;
  -webkit-text-fill-color: rgba(148, 163, 184, 0.85) !important;
}

.login .login-form .el-input__prefix .el-input__icon,
.login .login-form .el-input__prefix .input-icon,
.login .login-form .el-input__prefix .svg-icon,
.login .login-form .el-input__prefix svg {
  color: #60A5FA !important;
  fill: #60A5FA !important;
  box-sizing: content-box !important;
}

.login input:-webkit-autofill,
.login input:-webkit-autofill:hover,
.login input:-webkit-autofill:focus,
.login input:-webkit-autofill:active {
  -webkit-text-fill-color: #E2E8F0 !important;
  -webkit-box-shadow: 0 0 0 1000px #0F1928 inset !important;
  transition: background-color 50000s ease-in-out 0s !important;
  background-color: #0F1928 !important;
  caret-color: #E2E8F0 !important;
}

.login .login-form .captcha-input .el-input__prefix .el-input__icon,
.login .login-form .captcha-input .el-input__prefix .input-icon,
.login .login-form .captcha-input .el-input__prefix .svg-icon,
.login .login-form .captcha-input .el-input__prefix svg {
  color: #60A5FA !important;
  fill: #60A5FA !important;
  box-sizing: content-box !important;
}

.login .login-form .captcha-input .el-input__inner {
  background-color: rgba(15, 25, 40, 0.35) !important;
  border-color: rgba(59, 130, 246, 0.4) !important;
  color: #E2E8F0 !important;
  -webkit-text-fill-color: #E2E8F0 !important;
}

.login .login-form .captcha-input .el-input__inner:hover {
  border-color: rgba(96, 165, 250, 0.7) !important;
  box-shadow: 0 0 12px rgba(59, 130, 246, 0.3) !important;
}

.login .login-form .captcha-input.is-focus .el-input__inner {
  background-color: rgba(15, 25, 40, 0.5) !important;
  border-color: #60A5FA !important;
  box-shadow: 0 0 18px rgba(59, 130, 246, 0.4) !important;
}

.login .login-form .captcha-input .el-input__prefix .el-input__icon,
.login .login-form .captcha-input .el-input__prefix .input-icon {
  color: #60A5FA !important;
}

.login .login-code {
  background: rgba(8, 18, 32, 0.02) !important;
  border-radius: 10px;
  border: 1px solid rgba(59, 130, 246, 0.15);
  padding: 2px;
}

.login .login-code-img {
  background: rgba(8, 18, 32, 0.02) !important;
  border-radius: 8px;
  border: 1px solid rgba(59, 130, 246, 0.15) !important;
  display: block;
}
</style>
