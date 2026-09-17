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
            <el-tab-pane label="邮箱验证码" name="email" />
            <el-tab-pane label="手机验证码" name="phone" />
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

          <!-- 邮箱验证码登录 -->
          <template v-else-if="loginMode === 'email'">
            <el-input
              v-model="form.email"
              placeholder="请输入邮箱"
              size="large"
              autocomplete="email"
              class="field-input"
            />
            <div class="captcha-row">
              <el-input
                v-model="form.captcha"
                placeholder="请输入验证码"
                size="large"
                class="field-input"
              />
              <el-button class="get-code" size="large" :disabled="countdown > 0" @click="sendEmailCaptcha">{{ countdown > 0 ? `${countdown}s` : `获取验证码` }}</el-button>
            </div>
            <div class="remember-row">
              <el-checkbox v-model="remember" size="small">记住我</el-checkbox>
            </div>
          </template>

          <!-- 手机验证码登录 -->
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
              <el-button class="get-code" size="large" :disabled="countdown > 0" @click="sendCaptcha">{{ countdown > 0 ? `${countdown}s` : `获取验证码` }}</el-button>
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
            <span class="link" @click="openReset">忘记密码？</span>
            <span class="dot">·</span>
            <span class="link" @click="openRegister">注册账号</span>
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

    <el-dialog v-model="registerVisible" title="注册账号" width="420px" append-to-body>
      <el-form :model="registerForm" label-position="top" class="auth-dform">
        <el-form-item label="注册方式">
          <el-radio-group v-model="registerType" @change="onRegisterTypeChange">
            <el-radio label="email">邮箱</el-radio>
            <el-radio label="phone">手机号</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="registerType === 'email'" label="邮箱">
          <el-input v-model="registerForm.email" maxlength="128" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item v-else label="手机号">
          <el-input v-model="registerForm.phone" maxlength="11" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="captcha-row">
            <el-input v-model="registerForm.code" maxlength="6" placeholder="请输入验证码" />
            <el-button class="get-code" :disabled="registerCountdown > 0" @click="sendRegisterCaptcha">
              {{ registerCountdown > 0 ? `${registerCountdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="registerForm.password" type="password" show-password placeholder="至少 6 位密码" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="registerForm.nickname" maxlength="30" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerVisible = false">取消</el-button>
        <el-button type="primary" :loading="registerLoading" @click="submitRegister">注册</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetVisible" title="重置密码" width="420px" append-to-body>
      <el-form :model="resetForm" label-position="top" class="auth-dform">
        <el-form-item label="验证方式">
          <el-radio-group v-model="resetType" @change="onResetTypeChange">
            <el-radio label="email">邮箱</el-radio>
            <el-radio label="phone">手机号</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="resetType === 'email' ? '邮箱' : '手机号'">
          <el-input v-model="resetForm.account" :maxlength="resetType === 'email' ? 128 : 11" :placeholder="resetType === 'email' ? '请输入注册邮箱' : '请输入注册手机号'" />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="captcha-row">
            <el-input v-model="resetForm.code" maxlength="6" placeholder="请输入验证码" />
            <el-button class="get-code" :disabled="resetCountdown > 0" @click="sendResetCaptcha">
              {{ resetCountdown > 0 ? `${resetCountdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="resetForm.newPassword" type="password" show-password placeholder="至少 6 位密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetLoading" @click="submitReset">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { emailLogin, emailRegister, emailReset, emailSend, login, myPermissions, smsLogin, smsRegister, smsSend, smsReset } from '../api'
import { landingPath } from '../utils/menu'
import LogoMark from '../components/LogoMark.vue'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const authStore = useAuthStore()
const form = ref({ username: '', password: '', phone: '', email: '', captcha: '' })
const remember = ref(true)
const loading = ref(false)
const loginMode = ref('password')
const countdown = ref(0)
let countdownTimer = null
const registerVisible = ref(false)
const registerLoading = ref(false)
const registerCountdown = ref(0)
const registerType = ref('email')
let registerTimer = null
const registerForm = ref({ phone: '', email: '', code: '', password: '', nickname: '' })
const resetVisible = ref(false)
const resetLoading = ref(false)
const resetCountdown = ref(0)
const resetType = ref('email')
let resetTimer = null
const resetForm = ref({ account: '', code: '', newPassword: '' })

function isValidEmailValue(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(value || '').trim())
}

function isValidPhoneValue(value) {
  return /^1[3-9]\d{9}$/.test(String(value || '').trim())
}

function startLoginCountdown() {
  countdown.value = 30
  countdownTimer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      window.clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

async function sendEmailCaptcha() {
  if (countdown.value > 0) return
  if (!isValidEmailValue(form.value.email)) {
    ElMessage.warning('请输入正确的邮箱')
    return
  }
  try {
    await emailSend(form.value.email, 'login')
    ElMessage.success('验证码已发送，请查收邮箱')
    startLoginCountdown()
  } catch (e) {
    // 请求拦截器已提示错误
  }
}

async function sendCaptcha() {
  if (countdown.value > 0) return
  if (!isValidPhoneValue(form.value.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  try {
    const data = await smsSend(form.value.phone, 'login')
    ElMessage.success('验证码已发送')
    if (data && data.devCode) {
      ElMessage.info(`开发验证码：${data.devCode}`)
    }
    startLoginCountdown()
  } catch (e) {
    // 请求拦截器已提示错误
  }
}

function openRegister() {
  registerType.value = 'email'
  registerForm.value = { phone: '', email: '', code: '', password: '', nickname: '' }
  registerVisible.value = true
}

function onRegisterTypeChange() {
  registerForm.value.code = ''
  registerCountdown.value = 0
  if (registerTimer) {
    window.clearInterval(registerTimer)
    registerTimer = null
  }
}

function startRegisterCountdown() {
  registerCountdown.value = 30
  registerTimer = window.setInterval(() => {
    registerCountdown.value -= 1
    if (registerCountdown.value <= 0) {
      window.clearInterval(registerTimer)
      registerTimer = null
    }
  }, 1000)
}

async function sendRegisterCaptcha() {
  if (registerCountdown.value > 0) return
  if (registerType.value === 'email') {
    if (!isValidEmailValue(registerForm.value.email)) {
      ElMessage.warning('请输入正确的邮箱')
      return
    }
    try {
      await emailSend(registerForm.value.email, 'register')
      ElMessage.success('验证码已发送，请查收邮箱')
      startRegisterCountdown()
    } catch (e) {
      // 请求拦截器已提示错误
    }
    return
  }

  if (!isValidPhoneValue(registerForm.value.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  try {
    const data = await smsSend(registerForm.value.phone, 'register')
    ElMessage.success('验证码已发送')
    if (data && data.devCode) {
      ElMessage.info(`开发验证码：${data.devCode}`)
    }
    startRegisterCountdown()
  } catch (e) {
    // 请求拦截器已提示错误
  }
}

async function submitRegister() {
  if (registerType.value === 'email' && !isValidEmailValue(registerForm.value.email)) {
    ElMessage.warning('请输入正确的邮箱')
    return
  }
  if (registerType.value === 'phone' && !isValidPhoneValue(registerForm.value.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  if (!registerForm.value.code || !registerForm.value.password) {
    ElMessage.warning('请填写验证码和密码')
    return
  }
  registerLoading.value = true
  try {
    if (registerType.value === 'email') {
      await emailRegister(
        registerForm.value.email,
        registerForm.value.code,
        registerForm.value.password,
        registerForm.value.nickname,
      )
      ElMessage.success('注册成功，请使用邮箱验证码登录')
      form.value.email = registerForm.value.email
      loginMode.value = 'email'
    } else {
      await smsRegister(
        registerForm.value.phone,
        registerForm.value.code,
        registerForm.value.password,
        registerForm.value.nickname,
      )
      ElMessage.success('注册成功，请使用手机验证码登录')
      form.value.phone = registerForm.value.phone
      loginMode.value = 'phone'
    }
    registerVisible.value = false
  } catch (e) {
    // 请求拦截器已提示错误
  } finally {
    registerLoading.value = false
  }
}

function openReset() {
  resetType.value = 'email'
  resetForm.value = { account: '', code: '', newPassword: '' }
  resetVisible.value = true
}

function onResetTypeChange() {
  resetForm.value.account = ''
  resetForm.value.code = ''
  resetCountdown.value = 0
  if (resetTimer) {
    window.clearInterval(resetTimer)
    resetTimer = null
  }
}

function startResetCountdown() {
  resetCountdown.value = 30
  resetTimer = window.setInterval(() => {
    resetCountdown.value -= 1
    if (resetCountdown.value <= 0) {
      window.clearInterval(resetTimer)
      resetTimer = null
    }
  }, 1000)
}

async function sendResetCaptcha() {
  if (resetCountdown.value > 0) return
  if (resetType.value === 'email') {
    if (!isValidEmailValue(resetForm.value.account)) {
      ElMessage.warning('请输入正确的邮箱')
      return
    }
    try {
      await emailSend(resetForm.value.account, 'reset')
      ElMessage.success('验证码已发送，请查收邮箱')
      startResetCountdown()
    } catch (e) {
      // 请求拦截器已提示错误
    }
    return
  }

  if (!isValidPhoneValue(resetForm.value.account)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  try {
    const data = await smsSend(resetForm.value.account, 'reset')
    ElMessage.success('验证码已发送')
    if (data && data.devCode) {
      ElMessage.info(`开发验证码：${data.devCode}`)
    }
    startResetCountdown()
  } catch (e) {
    // 请求拦截器已提示错误
  }
}

async function submitReset() {
  if (resetType.value === 'email' && !isValidEmailValue(resetForm.value.account)) {
    ElMessage.warning('请输入正确的邮箱')
    return
  }
  if (resetType.value === 'phone' && !isValidPhoneValue(resetForm.value.account)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  if (!resetForm.value.code || !resetForm.value.newPassword) {
    ElMessage.warning('请填写验证码和新密码')
    return
  }
  resetLoading.value = true
  try {
    if (resetType.value === 'email') {
      await emailReset(resetForm.value.account, resetForm.value.code, resetForm.value.newPassword)
    } else {
      await smsReset(resetForm.value.account, resetForm.value.code, resetForm.value.newPassword)
    }
    ElMessage.success('密码已重置，请使用新密码登录')
    resetVisible.value = false
  } catch (e) {
    // 请求拦截器已提示错误
  } finally {
    resetLoading.value = false
  }
}

function onScan() { ElMessage.info('扫码登录建设中，敬请期待') }
function onHelp() { ElMessage.info('功能建设中，敬请期待') }

onUnmounted(() => {
  if (countdownTimer) window.clearInterval(countdownTimer)
  if (registerTimer) window.clearInterval(registerTimer)
  if (resetTimer) window.clearInterval(resetTimer)
})

async function handleLogin() {
  if (loginMode.value === 'email' || loginMode.value === 'phone') {
    if (loginMode.value === 'email' && !isValidEmailValue(form.value.email)) {
      ElMessage.warning('请输入正确的邮箱')
      return
    }
    if (loginMode.value === 'phone' && !isValidPhoneValue(form.value.phone)) {
      ElMessage.warning('请输入正确的手机号')
      return
    }
    if (!form.value.captcha) {
      ElMessage.warning('请输入验证码')
      return
    }
    loading.value = true
    try {
      const data = loginMode.value === 'email'
        ? await emailLogin(form.value.email, form.value.captcha, remember.value)
        : await smsLogin(form.value.phone, form.value.captcha, remember.value)
      await finishLogin(data)
    } catch (e) {
      // 错误已由拦截器提示
    } finally {
      loading.value = false
    }
    return
  }

  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const data = await login(form.value.username, form.value.password)
    await finishLogin(data)
  } catch (e) {
    // 错误已由拦截器提示
  } finally {
    loading.value = false
  }
}

async function finishLogin(data) {
  authStore.setAuth(data.token, data.username, data.role, data.nickname, data.avatar)
  try {
    const perms = await myPermissions()
    authStore.setPermissions((perms && perms.paths) || [])
  } catch (e) {
    // 权限接口失败不阻塞登录，菜单回退硬编码角色过滤
  }
  ElMessage.success('登录成功')
  router.push(landingPath(authStore.permissions, authStore.role))
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
