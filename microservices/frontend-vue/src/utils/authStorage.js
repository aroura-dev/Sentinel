// 登录态存储：记住我(勾选)→localStorage(跨会话)；不勾→sessionStorage(关浏览器即退出)
// 读取时两处都查，保证会话期间与刷新后一致。
const AUTH_KEYS = ['sentinel_token', 'sentinel_username', 'sentinel_role', 'sentinel_nickname', 'sentinel_permissions']

export function readKey(key) {
  return sessionStorage.getItem(key) ?? localStorage.getItem(key)
}

export function getToken() {
  return readKey('sentinel_token') || ''
}

export function readRole() {
  return readKey('sentinel_role') || ''
}

export function readUsername() {
  return readKey('sentinel_username') || ''
}

function setOne(storage, key, value) {
  if (value == null || value === '') {
    storage.removeItem(key)
  } else {
    storage.setItem(key, String(value))
  }
}

// 写入前先清两处旧值（防“记住→不记住”残留），再按 remember 写入目标存储
export function writeAuth(auth, remember) {
  clearAuth()
  const store = remember ? localStorage : sessionStorage
  setOne(store, 'sentinel_token', auth.token)
  setOne(store, 'sentinel_username', auth.username)
  setOne(store, 'sentinel_role', auth.role)
  setOne(store, 'sentinel_nickname', auth.nickname)
}

export function writePerms(paths) {
  localStorage.setItem('sentinel_permissions', JSON.stringify(Array.isArray(paths) ? paths : []))
}

export function readPerms() {
  try {
    const v = JSON.parse(localStorage.getItem('sentinel_permissions') || '[]')
    return Array.isArray(v) ? v : []
  } catch (e) {
    return []
  }
}

export function clearAuth() {
  for (const k of AUTH_KEYS) {
    sessionStorage.removeItem(k)
    localStorage.removeItem(k)
  }
}
