import { defineStore } from 'pinia'
import { readKey, readPerms, writeAuth, writePerms, clearAuth } from '../utils/authStorage'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: readKey('sentinel_token') || '',
    username: readKey('sentinel_username') || '',
    role: readKey('sentinel_role') || '',
    nickname: readKey('sentinel_nickname') || '',
    permissions: readPerms()
  }),
  actions: {
    // remember=true 持久到 localStorage（跨会话）；false 仅 sessionStorage（关浏览器即退出）
    setAuth(token, username, role = '', nickname = '', remember = true) {
      this.token = token || ''
      this.username = username || ''
      this.role = role || ''
      this.nickname = nickname || ''
      writeAuth({ token: this.token, username: this.username, role: this.role, nickname: this.nickname }, remember)
    },
    setPermissions(paths) {
      this.permissions = Array.isArray(paths) ? paths : []
      writePerms(this.permissions)
    },
    logout() {
      this.token = ''
      this.username = ''
      this.role = ''
      this.nickname = ''
      this.permissions = []
      clearAuth()
    }
  }
})
