<template>
  <div class="login-page">
    <!-- 左：青绿品牌面板（宽屏展示，含雷达装饰） -->
    <aside class="brand-panel">
      <header class="brand-top">
        <LogoMark :size="36" color="#67e8f9" />
        <div class="brand-id">
          <div class="brand-name">SENTINEL</div>
          <div class="brand-cn">物流异常智能处置平台</div>
        </div>
      </header>

      <div class="brand-mid">
        <h1 class="brand-title">异常全程处置，<br />客诉风险可控</h1>

        <ul class="brand-features">
          <li>
            <span class="ft-ic"><el-icon><Aim /></el-icon></span>
            <div class="ft-tx">
              <b>自动识别成单</b>
              <small>晚点、滞留、破损等异常，判定后直接生成工单</small>
            </div>
          </li>
          <li>
            <span class="ft-ic"><el-icon><Bell /></el-icon></span>
            <div class="ft-tx">
              <b>按角色通知</b>
              <small>商家、承运商、收货人各接收与其相关的处理信息</small>
            </div>
          </li>
          <li>
            <span class="ft-ic"><el-icon><Headset /></el-icon></span>
            <div class="ft-tx">
              <b>客服分级应答</b>
              <small>常规问题即时答复，疑难问题转交人工处理</small>
            </div>
          </li>
        </ul>
      </div>

      <footer class="brand-foot">企业级加密传输 · 操作全链路留痕</footer>

      <!-- 雷达装饰：右下角同心圆 + 脉冲点 -->
      <div class="radar" aria-hidden="true">
        <span class="ring ring-1"></span>
        <span class="ring ring-2"></span>
        <span class="ring ring-3"></span>
        <span class="blip"></span>
      </div>
    </aside>

    <!-- 右：浅色登录面板 -->
    <section class="form-panel">
      <div class="auth-shell">
        <!-- 窄屏品牌头（宽屏隐藏，品牌在左栏） -->
        <header class="auth-brand">
          <LogoMark :size="34" color="#0891b2" />
          <div class="auth-name">SENTINEL</div>
          <div class="auth-cn">物流异常智能处置平台</div>
        </header>

        <section class="auth-card">
          <h2 class="welcome">欢迎回来</h2>
          <p class="welcome-sub">登录后继续处理异常工单，跟进物流运营</p>

          <!-- 登录方式：下划线 Tab -->
          <el-tabs v-model="loginMode" class="login-tabs">
            <el-tab-pane label="账号登录" name="password" />
            <el-tab-pane name="captcha">
              <template #label>验证码登录</template>
            </el-tab-pane>
          </el-tabs>

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
            </template>

            <!-- 手机验证码登录：与账号登录同构——两行通栏输入框，发送按钮内联在验证码框右侧 -->
            <template v-else>
              <el-input
                v-model="form.phone"
                placeholder="请输入手机号"
                size="large"
                autocomplete="off"
                class="field-input"
              />
              <el-input
                v-model="form.captcha"
                placeholder="请输入验证码"
                size="large"
                autocomplete="off"
                class="field-input"
              >
                <template #suffix>
                  <span
                    class="code-link"
                    :class="{ counting: countdown > 0 }"
                    @mousedown.prevent
                    @click="sendCaptcha"
                  >
                    {{ countdown > 0 ? countdown + 's' : '获取验证码' }}
                  </span>
                </template>
              </el-input>
            </template>

            <div class="remember-row">
              <el-checkbox v-model="remember" size="small">记住我</el-checkbox>
              <span class="link-min" @click="openForgot">忘记密码？</span>
            </div>

            <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="handleLogin">
              登录
            </el-button>
          </el-form>

          <!-- 低频辅助入口 -->
          <div class="bottom-help">
            <span class="help" @click="openSupport"><el-icon><Service /></el-icon>在线客服</span>
            <span class="sep">|</span>
            <span class="help" @click="openHelp">账号问题帮助</span>
            <span class="sep">|</span>
            <span class="help" @click="openRegister">注册账号</span>
          </div>
        </section>

        <!-- 页脚：仅窄屏（宽屏左栏已有同款安全声明） -->
        <footer class="auth-foot">企业级加密传输 · 操作全链路留痕</footer>
      </div>
    </section>

    <!-- ================= 忘记密码 ================= -->
    <el-dialog v-model="forgotVisible" title="找回密码" width="420px" class="auth-dialog" destroy-on-close>
      <el-form label-position="top" class="auth-dform">
        <el-form-item label="手机号">
          <el-input v-model="forgot.phone" placeholder="请输入注册手机号" size="large" autocomplete="off">
            <template #suffix>
              <span
                class="code-link"
                :class="{ counting: forgotCd > 0 }"
                @mousedown.prevent
                @click="sendForgotCode"
              >
                {{ forgotCd > 0 ? forgotCd + 's' : '获取验证码' }}
              </span>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="验证码">
          <el-input v-model="forgot.code" placeholder="请输入短信验证码" size="large" autocomplete="off" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="forgot.pwd" placeholder="6-64 位新密码" type="password" show-password size="large" autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="forgot.pwd2" placeholder="再次输入新密码" type="password" show-password size="large" autocomplete="new-password" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="forgotVisible = false">取消</el-button>
        <el-button type="primary" :loading="forgotLoading" @click="submitForgot">重置密码</el-button>
      </template>
    </el-dialog>

    <!-- ================= 注册账号 ================= -->
    <el-dialog v-model="registerVisible" title="注册账号" width="420px" class="auth-dialog" destroy-on-close>
      <el-form label-position="top" class="auth-dform">
        <el-form-item label="用户名（中文/字母，用于登录）">
          <el-input v-model="reg.username" placeholder="如：张伟、wangfang" size="large" autocomplete="off" maxlength="32" />
        </el-form-item>
        <el-form-item label="手机号（用于验证码）">
          <el-input v-model="reg.phone" placeholder="请输入手机号" size="large" autocomplete="off">
            <template #suffix>
              <span
                class="code-link"
                :class="{ counting: regCd > 0 }"
                @mousedown.prevent
                @click="sendRegisterCode"
              >
                {{ regCd > 0 ? regCd + 's' : '获取验证码' }}
              </span>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="验证码">
          <el-input v-model="reg.code" placeholder="请输入短信验证码" size="large" autocomplete="off" />
        </el-form-item>
        <el-form-item label="昵称（选填）">
          <el-input v-model="reg.nickname" placeholder="展示名称，默认为手机号" size="large" autocomplete="off" />
        </el-form-item>
        <el-form-item label="设置密码">
          <el-input v-model="reg.pwd" placeholder="6-64 位密码" type="password" show-password size="large" autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="reg.pwd2" placeholder="再次输入密码" type="password" show-password size="large" autocomplete="new-password" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerVisible = false">取消</el-button>
        <el-button type="primary" :loading="regLoading" @click="submitRegister">注册</el-button>
      </template>
    </el-dialog>

    <!-- ================= 帮助中心（在线客服 / 账号问题帮助共用） ================= -->
    <el-dialog v-model="helpVisible" title="帮助中心" width="480px" class="auth-dialog">
      <div class="help-faq">
        <div v-for="f in faqs" :key="f.q" class="faq">
          <p class="faq-q">{{ f.q }}</p>
          <p class="faq-a">{{ f.a }}</p>
        </div>
        <p class="help-admin">如问题仍未解决，请联系系统管理员协助处理。</p>
      </div>
    </el-dialog>

    <!-- ================= 在线客服提示 ================= -->
    <el-dialog v-model="supportVisible" title="在线客服" width="440px" class="auth-dialog">
      <div class="help-faq">
        <div class="faq">
          <p class="faq-q">服务时间</p>
          <p class="faq-a">工作日 09:00-18:00，非服务时段可先提交问题反馈。</p>
        </div>
        <div class="faq">
          <p class="faq-q">登录后处理</p>
          <p class="faq-a">登录系统后可使用“智能客服”提交物流异常、账号和业务问题。</p>
        </div>
        <p class="help-admin">如账号无法登录或需要紧急处理，请联系系统管理员协助。</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, myPermissions, smsSend, smsLogin, smsRegister, smsReset } from '../api'
import LogoMark from '../components/LogoMark.vue'
import { useAuthStore } from '../store/auth'
import { landingFor } from '../utils/menu'

const router = useRouter()
const authStore = useAuthStore()
const form = ref({ username: '', password: '', phone: '', captcha: '' })
const remember = ref(true)
const loading = ref(false)
const loginMode = ref('password')
const countdown = ref(0)
let timer = null

// 忘记密码 / 注册 / 帮助中心
const forgotVisible = ref(false)
const registerVisible = ref(false)
const helpVisible = ref(false)
const supportVisible = ref(false)
const forgot = ref({ phone: '', code: '', pwd: '', pwd2: '' })
const reg = ref({ username: '', phone: '', code: '', nickname: '', pwd: '', pwd2: '' })
const forgotCd = ref(0)
const regCd = ref(0)
let forgotTimer = null
let regTimer = null
const forgotLoading = ref(false)
const regLoading = ref(false)

const faqs = [
  { q: '忘记登录密码怎么办？', a: '点击“忘记密码？”，输入注册手机号并获取验证码，即可设置新密码。' },
  { q: '如何注册账号？', a: '点击“注册账号”，用手机号获取验证码并设置密码即可完成；填写中文用户名；手机号用于验证码校验。' },
  { q: '支持哪几种登录方式？', a: '支持用户名密码登录与手机号验证码登录。勾选“记住我”可在本次登录后保持在线。' },
  { q: '登录后看不到部分菜单怎么办？', a: '菜单按账号角色展示。如确有权限需要，请联系系统管理员调整角色。' }
]

const isPhone = (p) => /^1\d{10}$/.test(p)
const isPwd = (p) => p && p.length >= 6 && p.length <= 64
const isUsername = (u) => /^[\p{Script=Han}A-Za-z][\p{Script=Han}A-Za-z0-9._-]{1,31}$/u.test(String(u || '').trim())

function openForgot() {
  forgot.value = { phone: form.value.phone || '', code: '', pwd: '', pwd2: '' }
  forgotVisible.value = true
}
function openRegister() {
  reg.value = { username: '', phone: '', code: '', nickname: '', pwd: '', pwd2: '' }
  registerVisible.value = true
}
function openHelp() { helpVisible.value = true }
function openSupport() { supportVisible.value = true }

// 主登录：验证码发送
async function sendCaptcha() {
  if (countdown.value > 0) return
  if (!isPhone(form.value.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  try {
    await smsSend(form.value.phone, 'login')
    ElMessage.success('验证码已发送，请查收')
    countdown.value = 60
    if (timer) clearInterval(timer)
    timer = setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (e) { /* 错误已由拦截器提示 */ }
}

// 找回密码：发送重置验证码（scene=reset，须手机号已注册）
async function sendForgotCode() {
  if (forgotCd.value > 0) return
  if (!isPhone(forgot.value.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  try {
    await smsSend(forgot.value.phone, 'reset')
    ElMessage.success('验证码已发送，请查收')
    forgotCd.value = 60
    if (forgotTimer) clearInterval(forgotTimer)
    forgotTimer = setInterval(() => {
      forgotCd.value -= 1
      if (forgotCd.value <= 0) clearInterval(forgotTimer)
    }, 1000)
  } catch (e) { /* 错误已由拦截器提示 */ }
}

// 注册：发送验证码（场景 register，须手机号未注册）
async function sendRegisterCode() {
  if (regCd.value > 0) return
  if (!isPhone(reg.value.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  try {
    await smsSend(reg.value.phone, 'register')
    ElMessage.success('验证码已发送，请查收')
    regCd.value = 60
    if (regTimer) clearInterval(regTimer)
    regTimer = setInterval(() => {
      regCd.value -= 1
      if (regCd.value <= 0) clearInterval(regTimer)
    }, 1000)
  } catch (e) { /* 错误已由拦截器提示 */ }
}

async function submitForgot() {
  if (!isPhone(forgot.value.phone)) return ElMessage.warning('请输入正确的手机号')
  if (!forgot.value.code) return ElMessage.warning('请输入验证码')
  if (!isPwd(forgot.value.pwd)) return ElMessage.warning('新密码至少 6 位')
  if (forgot.value.pwd !== forgot.value.pwd2) return ElMessage.warning('两次输入的密码不一致')
  forgotLoading.value = true
  try {
    await smsReset(forgot.value.phone, forgot.value.code, forgot.value.pwd)
    ElMessage.success('密码已重置，请使用新密码登录')
    forgotVisible.value = false
  } catch (e) { /* 错误已由拦截器提示 */ } finally {
    forgotLoading.value = false
  }
}

async function submitRegister() {
  const username = reg.value.username.trim()
  if (!isUsername(username)) return ElMessage.warning('用户名需为中文或字母开头，长度 2-32 位')
  if (!isPhone(reg.value.phone)) return ElMessage.warning('请输入正确的手机号')
  if (!reg.value.code) return ElMessage.warning('请输入验证码')
  if (!isPwd(reg.value.pwd)) return ElMessage.warning('密码至少 6 位')
  if (reg.value.pwd !== reg.value.pwd2) return ElMessage.warning('两次输入的密码不一致')
  regLoading.value = true
  try {
    await smsRegister(reg.value.phone, reg.value.code, username, reg.value.pwd, reg.value.nickname.trim())
    ElMessage.success('注册成功，请使用用户名密码登录')
    registerVisible.value = false
    form.value.username = username
    form.value.password = ''
    loginMode.value = 'password'
  } catch (e) { /* 错误已由拦截器提示 */ } finally {
    regLoading.value = false
  }
}
async function afterAuth(data) {
  authStore.setAuth(data.token, data.username, data.role, data.nickname, remember.value, data.avatar)
  try {
    const perms = await myPermissions()
    authStore.setPermissions((perms && perms.paths) || [])
  } catch (e) {
    // 权限接口失败不阻塞登录，菜单回退硬编码角色过滤
  }
  ElMessage.success('登录成功')
  router.push(landingFor(data.role))
}

async function handleLogin() {
  if (loginMode.value === 'captcha') {
    if (!isPhone(form.value.phone) || !form.value.captcha) {
      ElMessage.warning('请输入手机号和验证码')
      return
    }
    loading.value = true
    try {
      const data = await smsLogin(form.value.phone, form.value.captcha, remember.value)
      await afterAuth(data)
    } catch (e) { /* 错误已由拦截器提示 */ } finally { loading.value = false }
    return
  }
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const data = await login(form.value.username, form.value.password, remember.value)
    await afterAuth(data)
  } catch (e) {
    // 错误已由拦截器提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* —— 左右分栏：左深青品牌面板 + 右白色登录卡片 —— */
.login-page {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 1fr);
  min-height: 100vh;
  background: #fff;
}

/* ============ 左：品牌面板 ============ */
.brand-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 48px 56px 40px;
  color: #e9f4f7;
  background:
    radial-gradient(760px 480px at 88% -8%, rgba(34, 211, 238, 0.22), transparent 62%),
    radial-gradient(520px 420px at -10% 112%, rgba(8, 145, 178, 0.16), transparent 58%),
    linear-gradient(140deg, #0a2836 0%, #0d4658 55%, #106070 100%);
}

.brand-top {
  display: flex;
  align-items: center;
  gap: 14px;
}
.brand-id .brand-name {
  font-family: 'Space Grotesk Variable', Candara, 'Century Gothic', 'Segoe UI', Arial, sans-serif;
  font-size: 23px;
  font-weight: 600;
  letter-spacing: 0.16em;
  line-height: 1.1;
  color: #fff;
}
/* 深色面板上的标识字：白→淡青渐变，制造细腻光泽感 */
@supports ((-webkit-background-clip: text) or (background-clip: text)) {
  .brand-id .brand-name {
    background-image: linear-gradient(180deg, #ffffff 35%, #9be0f0 100%);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    color: transparent;
  }
}
.brand-id .brand-cn {
  margin-top: 4px;
  font-size: 12px;
  letter-spacing: 2px;
  color: #9fb8c2;
}

/* 中部内容垂直居中 */
.brand-mid {
  margin: auto 0;
  padding: 48px 0 44px;
  max-width: 480px;
}
.brand-title {
  margin: 0;
  font-size: clamp(28px, 3.2vw, 44px);
  font-weight: 600;
  line-height: 1.3;
  letter-spacing: 0.5px;
  color: #f4fafc;
}

.brand-features {
  list-style: none;
  margin: 34px 0 0;
  padding: 0;
}
.brand-features li {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 2px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}
.brand-features li:last-child { border-bottom: 1px solid rgba(255, 255, 255, 0.08); }
.ft-ic {
  display: grid;
  place-items: center;
  flex: none;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  color: #67e8f9;
  background: rgba(34, 211, 238, 0.12);
  border: 1px solid rgba(103, 232, 249, 0.25);
}
.ft-ic .el-icon { font-size: 17px; }
.ft-tx b { display: block; font-size: 15px; font-weight: 500; color: #eef6f9; }
.ft-tx small { display: block; margin-top: 3px; font-size: 12px; color: #8aa6b1; }

.brand-foot {
  font-size: 12px;
  letter-spacing: 1px;
  color: rgba(165, 187, 196, 0.55);
}

/* 雷达装饰 */
.radar {
  position: absolute;
  right: -120px;
  bottom: -150px;
  width: 360px;
  height: 360px;
  pointer-events: none;
}
.radar .ring {
  position: absolute;
  left: 50%;
  top: 50%;
  border-radius: 50%;
  border: 1px solid rgba(148, 233, 255, 0.14);
  transform: translate(-50%, -50%);
}
.ring-1 { width: 300px; height: 300px; }
.ring-2 { width: 205px; height: 205px; border-color: rgba(148, 233, 255, 0.12); }
.ring-3 { width: 110px; height: 110px; border-color: rgba(148, 233, 255, 0.1); }
.blip {
  position: absolute;
  top: 47%;
  left: 66%;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #7de9ff;
  animation: blip 2.8s ease-out infinite;
}
@keyframes blip {
  0% { box-shadow: 0 0 0 0 rgba(125, 233, 255, 0.5); }
  70% { box-shadow: 0 0 0 24px rgba(125, 233, 255, 0); }
  100% { box-shadow: 0 0 0 0 rgba(125, 233, 255, 0); }
}

/* ============ 右：登录面板 ============ */
.form-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 40px;
  background: #fff;
}
.auth-shell {
  width: min(408px, 100%);
  display: flex;
  flex-direction: column;
  animation: rise 0.4s ease-out both;
}
@keyframes rise {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 窄屏品牌头：宽屏隐藏 */
.auth-brand { display: none; }

/* 登录卡片 */
.auth-card {
  background: #fff;
  border: 1px solid var(--border-color, #e5e6eb);
  border-radius: 14px;
  padding: 30px 32px 24px;
  box-shadow: 0 2px 16px rgba(29, 33, 41, 0.05);
}
.welcome { margin: 0; font-size: 24px; font-weight: 600; color: #1d2129; }
.welcome-sub { margin: 8px 0 20px; font-size: 13px; color: #86909c; }

/* 登录方式：下划线 Tab（品牌青高亮） */
.login-tabs :deep(.el-tabs__nav-wrap::after) { height: 1px; background: #eceff3; }
.login-tabs :deep(.el-tabs__item) { height: 36px; line-height: 36px; font-size: 14px; color: #5a6472; }
.login-tabs :deep(.el-tabs__item.is-active) { color: #0891b2; font-weight: 500; }
.login-tabs :deep(.el-tabs__active-bar) { background: #0891b2; height: 2px; }
.login-tabs :deep(.el-tabs__content) { display: none; }
.login-form { margin-top: 12px; }

/* 输入框：同种登录方式内两行统一 width:100%、等高/内边距，外框左侧对齐 */
.field-input { display: block; width: 100%; margin-bottom: 16px; }
.field-input :deep(.el-input) { --el-input-height: 44px; }
.field-input :deep(.el-input__wrapper) {
  width: 100%;
  box-sizing: border-box;
  background: #f7f8fa;
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  box-shadow: none;
  padding: 1px 12px;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, background 0.15s ease;
}
.field-input :deep(.el-input__wrapper:hover) { border-color: #d0d5dd; }
.field-input :deep(.el-input__wrapper.is-focus) {
  background: #fff;
  border-color: #0891b2;
  box-shadow: 0 0 0 3px rgba(8, 145, 178, 0.12);
}
.field-input :deep(.el-input__inner) { color: #1d2129; caret-color: #0891b2; }
.field-input :deep(.el-input__inner::placeholder) { color: #9aa3b2; }
.field-input :deep(.el-input__suffix) { color: #9aa3b2; }

/* 验证码发送：内联文字动作，随输入框通栏，保持两模式几何一致 */
.code-link {
  font-size: 13px;
  color: #0891b2;
  cursor: pointer;
  user-select: none;
  white-space: nowrap;
}
.code-link:hover { color: #06b6d4; }
.code-link.counting { color: #9aa3b2; cursor: default; }

/* 记住我 / 忘记密码 */
.remember-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 2px 0 18px;
}
.remember-row :deep(.el-checkbox__inner) {
  background: #fff;
  border-color: #d0d5dd;
}
.remember-row :deep(.el-checkbox__input:hover .el-checkbox__inner),
.remember-row :deep(.el-checkbox__input.is-focus .el-checkbox__inner) {
  border-color: rgba(8, 145, 178, 0.6);
}
.remember-row :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background: #0891b2;
  border-color: #0891b2;
}
.remember-row :deep(.el-checkbox__label) { font-size: 13px; color: #4e5969; }
.link-min { font-size: 13px; color: #86909c; cursor: pointer; user-select: none; }
.link-min:hover { color: #0891b2; }

/* 主 CTA：品牌青渐变 */
.submit-btn {
  width: 100%;
  height: 44px;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 4px;
  background: linear-gradient(90deg, #0891b2, #06b6d4);
  color: #fff;
  box-shadow: 0 8px 18px -10px rgba(6, 182, 212, 0.6);
  transition: filter 0.15s ease, transform 0.15s ease;
}
.submit-btn:hover { filter: brightness(1.06); color: #fff; transform: translateY(-1px); }
.submit-btn:active { transform: translateY(0); }

/* 底部辅助入口 */
.bottom-help {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 22px;
  padding-top: 16px;
  border-top: 1px solid #eef0f3;
  font-size: 12px;
}
.bottom-help .help {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
  color: #9aa3b2;
  transition: color 0.15s ease;
}
.bottom-help .help:hover { color: #0891b2; }
.bottom-help .sep { color: #e0e3e8; }

/* 页脚：仅窄屏展示 */
.auth-foot {
  display: none;
  margin-top: 20px;
  text-align: center;
  font-size: 12px;
  letter-spacing: 1px;
  color: #c0c4cc;
}

/* ============ 响应式：窄屏退化为顶部品牌条 + 居中卡片 ============ */
@media (max-width: 1023.98px) {
  .login-page { grid-template-columns: 1fr; background: #f7f8fa; }
  .brand-panel { display: none; }
  .form-panel { padding: 40px 20px; background: #f7f8fa; }
  .auth-brand { display: flex; flex-direction: column; align-items: center; margin-bottom: 26px; }
  .auth-name {
    margin-top: 13px;
    font-family: 'Space Grotesk Variable', Candara, 'Century Gothic', 'Segoe UI', Arial, sans-serif;
    font-size: 21px;
    font-weight: 600;
    letter-spacing: 0.16em;
    color: #1d2129;
    line-height: 1.1;
  }
  .auth-cn {
    margin-top: 8px;
    font-size: 12px;
    letter-spacing: 2px;
    color: #86909c;
  }
  .auth-foot { display: block; }
}

@media (max-width: 480px) {
  .auth-card { padding: 22px 20px 20px; }
}
</style>

<style>
/* 登录弹窗（el-dialog 内容渲染在 body 上，用非 scoped 样式兜底）：
   顶部标签 + 通栏输入框，与登录卡片视觉一致，各字段宽度对齐 */
.auth-dform { width: 100%; }
.auth-dform .el-form-item { margin-bottom: 18px; }
.auth-dform .el-form-item__label {
  font-size: 13px;
  color: #4e5969;
  line-height: 1.2;
  padding-bottom: 6px;
}
.auth-dform .el-input { --el-input-height: 44px; width: 100%; }
.auth-dform .el-input__wrapper {
  width: 100%;
  box-sizing: border-box;
  background: #f7f8fa;
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  box-shadow: none;
  padding: 1px 12px;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, background 0.15s ease;
}
.auth-dform .el-input__wrapper:hover { border-color: #d0d5dd; }
.auth-dform .el-input__wrapper.is-focus {
  background: #fff;
  border-color: #0891b2;
  box-shadow: 0 0 0 3px rgba(8, 145, 178, 0.12);
}
.auth-dform .el-input__inner { color: #1d2129; caret-color: #0891b2; }
.auth-dform .el-input__inner::placeholder { color: #9aa3b2; }
.auth-dform .code-link {
  font-size: 13px;
  color: #0891b2;
  cursor: pointer;
  user-select: none;
  white-space: nowrap;
}
.auth-dform .code-link:hover { color: #06b6d4; }
.auth-dform .code-link.counting { color: #9aa3b2; cursor: default; }

/* 帮助中心 FAQ */
.help-faq .faq { margin-bottom: 16px; }
.help-faq .faq-q { margin: 0 0 6px; font-size: 14px; font-weight: 600; color: #1d2129; }
.help-faq .faq-a { margin: 0; font-size: 13px; line-height: 1.7; color: #4e5969; }
.help-admin {
  margin: 8px 0 0;
  padding-top: 12px;
  border-top: 1px solid #eef0f3;
  font-size: 12px;
  color: #86909c;
}
</style>
