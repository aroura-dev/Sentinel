import { defineStore } from 'pinia'

const PERMS_KEY = 'sentinel_permissions'

function loadPerms() {
  try {
    const v = JSON.parse(localStorage.getItem(PERMS_KEY) || '[]')
    return Array.isArray(v) ? v : []
  } catch (e) {
    return []
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('sentinel_token') || '',
    username: localStorage.getItem('sentinel_username') || '',
    role: localStorage.getItem('sentinel_role') || '',
    nickname: localStorage.getItem('sentinel_nickname') || '',
    avatar: localStorage.getItem('sentinel_avatar') || '',
    permissions: loadPerms()
  }),
  actions: {
    setAuth(token, username, role = '', nickname = '', avatar = '') {
      this.token = token || ''
      this.username = username || ''
      this.role = role || ''
      this.nickname = nickname || ''
      this.avatar = avatar || ''
      localStorage.setItem('sentinel_token', this.token)
      localStorage.setItem('sentinel_username', this.username)
      localStorage.setItem('sentinel_role', this.role)
      localStorage.setItem('sentinel_nickname', this.nickname)
      localStorage.setItem('sentinel_avatar', this.avatar)
    },
    setProfile(username, role = '', nickname = '', avatar = '') {
      if (username) this.username = username
      if (role) this.role = role
      if (nickname) this.nickname = nickname
      if (avatar !== undefined) this.avatar = avatar || ''
      localStorage.setItem('sentinel_username', this.username)
      localStorage.setItem('sentinel_role', this.role)
      localStorage.setItem('sentinel_nickname', this.nickname)
      localStorage.setItem('sentinel_avatar', this.avatar)
    },
    setAvatar(avatar) {
      this.avatar = avatar || ''
      localStorage.setItem('sentinel_avatar', this.avatar)
    },
    setPermissions(paths) {
      this.permissions = Array.isArray(paths) ? paths : []
      localStorage.setItem(PERMS_KEY, JSON.stringify(this.permissions))
    },
    logout() {
      this.token = ''
      this.username = ''
      this.role = ''
      this.nickname = ''
      this.avatar = ''
      this.permissions = []
      localStorage.removeItem('sentinel_token')
      localStorage.removeItem('sentinel_username')
      localStorage.removeItem('sentinel_role')
      localStorage.removeItem('sentinel_nickname')
      localStorage.removeItem('sentinel_avatar')
      localStorage.removeItem(PERMS_KEY)
    }
  }
})