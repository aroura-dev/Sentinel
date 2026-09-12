<template>
  <div class="login-page">
    <!-- 简约几何装饰元素 -->
    <div class="decor" aria-hidden="true">
      <span class="deco deco-1"></span>
      <span class="deco deco-2"></span>
      <span class="deco deco-3"></span>
      <span class="deco deco-4"></span>
    </div>

    <div class="login-main">
      <!-- 顶部品牌区：Logo + 平台名 + slogan，居中 -->
      <header class="login-brand">
        <LogoMark :size="48" />
        <div class="brand-name">SENTINEL</div>
        <div class="brand-cn">物流异常智能处置平台</div>
        <div class="brand-sub">异常自动诊断建单 · 三端主动触达 · AI 客服先行兜底</div>
      </header>

      <!-- 登录卡片：水平垂直居中 -->
      <section class="login-card">
        <!-- 卡片头部：Tab 切换 + 右上角扫码入口 -->
        <div class="card-head">
          <el-tabs v-model="loginMode" class="login-tabs">
            <el-tab-pane label="账号登录" name="password" />
            <el-tab-pane name="captcha">
              <template #label>验证码登录<span class="soon-tag">即将上线</span></template>
            </el-tab-pane>
          </el-tabs>
          <div class="scan-entry" @click="onScan">
            <el-icon><FullScreen /></el-icon>
            <span>扫码登录</span>
          </div>
        </div>

        <el-form :model="form" class="login-form" @keyup.enter="handleLogin" autocomplete="off">
          <!-- 账号密码登录 -->
          <template v-if="loginMode === 'password'">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              size="large"
              autocomplete="off"
              class="field-input"
            />
            <el-input
              v-model="form.password"
              placeholder="请输入密码"
              type="password"
              size="large"
              show-password
              autocomplete="new-password"
              class="field-input"
            />
            <div class="remember-row">
              <el-checkbox v-model="remember" size="small">记住我</el-checkbox>
            </div>
          </template>

          <!-- 手机验证码登录（预留位） -->
          <template v-else>
            <el-input
              v-model="form.phone"
              placeholder="请输入手机号"
              size="large"
              autocomplete="off"
              class="field-input"
            />
            <div class="captcha-row">
              <el-input
                v-model="form.captcha"
                placeholder="请输入验证码"
                size="large"
                class="field-input"
              />
              <el-button class="get-code" size="large" @click="sendCaptcha">获取验证码</el-button>
            </div>
            <div class="remember-row">
              <el-checkbox v-model="remember" size="small">记住我</el-checkbox>
            </div>
          </template>

          <!-- 登录按钮：通栏铺满，品牌主色，最高视觉权重 -->
          <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="handleLogin">
            登录
          </el-button>

          <!-- 次要链接：降权，放按钮下方 -->
          <div class="sub-links">
            <span class="link" @click="onHelp">忘记密码？</span>
            <span class="dot">·</span>
            <span class="link" @click="onHelp">注册账号</span>
          </div>
        </el-form>

        <!-- 卡片底部：低频辅助入口 -->
        <div class="card-bottom">
          <span class="help" @click="onHelp"><el-icon><Service /></el-icon>在线客服</span>
          <span class="sep">|</span>
          <span class="help" @click="onHelp">账号问题帮助</span>
        </div>
      </section>
    </div>

    <!-- 页面底部浅灰小字 -->
    <footer class="login-footer">企业级安全传输 · 遇到问题请联系管理员</footer>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, myPermissions } from '../api'
import LogoMark from '../components/LogoMark.vue'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const authStore = useAuthStore()
const form = ref({ username: '', password: '', phone: '', captcha: '' })
const remember = ref(true)
const loading = ref(false)
const loginMode = ref('password')

// 预留位功能：验证码 / 扫码 / 帮助链接均不触达业务接口
function sendCaptcha() { ElMessage.info('验证码登录建设中，敬请期待') }
function onScan() { ElMessage.info('扫码登录建设中，敬请期待') }
function onHelp() { ElMessage.info('功能建设中，敬请期待') }

async function handleLogin() {
  if (loginMode.value === 'captcha') {
    ElMessage.info('验证码登录建设中，请使用账号密码登录')
    return
  }
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const data = await login(form.value.username, form.value.password)
    authStore.setAuth(data.token, data.username, data.role, data.nickname)
    try {
      const perms = await myPermissions()
      authStore.setPermissions((perms && perms.paths) || [])
    } catch (e) {
      // 权限接口失败不阻塞登录，菜单回退硬编码角色过滤
    }
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (e) {
    // 错误已由拦截器提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  /* 淡雅主题背景，主界面同款浅灰 */
  background: #f7f8fa;
  overflow: hidden;
}

/* —— 简约几何装饰（品牌色低透明度） —— */
.deco {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.deco-1 {
  position: absolute;
  top: 12%;
  left: 9%;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  border: 1px solid rgba(8, 145, 178, 0.12);
}
.deco-2 {
  position: absolute;
  bottom: 14%;
  right: 10%;
  width: 150px;
  height: 150px;
  border-radius: 32px;
  background: rgba(8, 145, 178, 0.05);
  transform: rotate(45deg);
}
.deco-3 {
  position: absolute;
  top: 22%;
  right: 20%;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: rgba(8, 145, 178, 0.08);
}
.deco-4 {
  position: absolute;
  bottom: 24%;
  left: 16%;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(8, 145, 178, 0.1);
}

.login-main {
  position: relative;
  width: 380px;
  max-width: 100%;
}

/* —— 品牌区 —— */
.login-brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 26px;
}
.brand-name {
  font-size: 24px;
  font-weight: 600;
  letter-spacing: 2px;
  color: #1d2129;
  line-height: 1.1;
  margin-top: 10px;
}
.brand-cn {
  font-size: 13px;
  color: #4e5969;
  letter-spacing: 3px;
  margin-top: 5px;
}
.brand-sub {
  font-size: 11px;
  color: #86909c;
  margin-top: 6px;
}

/* —— 登录卡片：白底主内容，浅灰辅助文字 —— */
.login-card {
  position: relative;
  background: #fff;
  border: 1px solid #eef0f3;
  border-radius: 16px;
  padding: 22px 30px 18px;
  box-shadow: 0 8px 32px rgba(16, 24, 40, 0.05);
  animation: rise 0.4s ease-out both;
}
@keyframes rise {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 卡片头部：Tab 左 + 扫码入口右 */
.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}
.login-tabs :deep(.el-tabs__header) {
  margin: 0 0 18px;
}
.login-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: #eceff3;
}
.login-tabs :deep(.el-tabs__item) {
  height: 32px;
  line-height: 32px;
  font-size: 14px;
  font-weight: 500;
}
.login-tabs :deep(.el-tabs__content) {
  display: none;
}
.soon-tag {
  margin-left: 4px;
  font-size: 10px;
  line-height: 1;
  color: #b8c0cc;
  border: 1px solid #dfe3ea;
  border-radius: 3px;
  padding: 2px 4px;
  vertical-align: 1px;
}
.scan-entry {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding-top: 8px;
  font-size: 12px;
  color: #86909c;
  cursor: pointer;
  flex: none;
}
.scan-entry:hover {
  color: #0891b2;
}

/* —— 表单：输入框占位提示，密码框右侧眼睛图标 —— */
.login-form {
  margin-top: 0;
}
.field-input {
  display: block;
  width: 100%;
  margin-bottom: 14px;
}
.field-input :deep(.el-input) {
  --el-input-height: 42px;
}
.field-input :deep(.el-input__wrapper) {
  background: #fff;
  border: 1px solid #d6dbe3;
  border-radius: 9px;
  box-shadow: none;
  padding: 0 12px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}
.field-input :deep(.el-input__wrapper:hover) {
  border-color: #a9b3c2;
}
.field-input :deep(.el-input__wrapper.is-focus) {
  border-color: #0891b2;
  box-shadow: 0 0 0 3px rgba(8, 145, 178, 0.08);
}
.field-input :deep(.el-input__inner) {
  font-size: 14px;
}
.field-input :deep(.el-input__suffix) {
  color: #9aa3b2;
}

/* 记住我：密码输入框右下角 */
.remember-row {
  display: flex;
  justify-content: flex-end;
  margin: 0 0 14px;
}

/* 验证码行 */
.captcha-row {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
.captcha-row .field-input {
  flex: 1;
  min-width: 0;
  margin-bottom: 0;
}
.get-code {
  height: 42px;
  border: 1px solid #d6dbe3;
  border-radius: 9px;
  background: #fff;
  color: #6b7280;
  font-size: 13px;
  flex: none;
}
.get-code:hover {
  border-color: #0891b2;
  color: #0891b2;
}

/* 登录按钮：通栏铺满，品牌主色 */
.submit-btn {
  width: 100%;
  height: 42px;
  border: none;
  border-radius: 9px;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 2px;
  background: #0891b2;
  box-shadow: none;
  transition: background 0.2s ease, transform 0.15s ease;
}
.submit-btn:hover {
  background: #06748e;
  transform: translateY(-1px);
}
.submit-btn:active {
  transform: translateY(0);
}

/* 次要链接：按钮下方，低视觉权重 */
.sub-links {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 14px;
  font-size: 13px;
}
.sub-links .link {
  color: #4e5969;
  cursor: pointer;
}
.sub-links .link:hover {
  color: #0891b2;
}
.sub-links .dot {
  color: #c0c4cc;
}

/* 卡片底部：低频辅助入口 */
.card-bottom {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid #f1f3f6;
  font-size: 12px;
  color: #9aa3b2;
}
.card-bottom .help {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}
.card-bottom .help:hover {
  color: #0891b2;
}
.card-bottom .sep {
  color: #dfe3ea;
}

/* —— 页面底部浅灰小字 —— */
.login-footer {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 22px;
  text-align: center;
  font-size: 12px;
  color: #c0c4cc;
}
</style>
