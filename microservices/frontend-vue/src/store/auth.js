import { defineStore } from 'pinia'
import { readKey, readPerms, writeAuth, writePerms, clearAuth } from '../utils/authStorage'

function persistProfile(state) {
  const store = sessionStorage.getItem('sentinel_token') ? sessionStorage : localStorage
  store.setItem('sentinel_username', state.username || '')
  store.setItem('sentinel_role', state.role || '')
  store.setItem('sentinel_nickname', state.nickname || '')
  store.setItem('sentinel_avatar', state.avatar || '')
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: readKey('sentinel_token') || '',
    username: readKey('sentinel_username') || '',
    role: readKey('sentinel_role') || '',
    nickname: readKey('sentinel_nickname') || '',
    avatar: readKey('sentinel_avatar') || '',
    permissions: readPerms()
  }),
  actions: {
    // remember=true 持久到 localStorage（跨会话）；false 仅 sessionStorage（关浏览器即退出）
    setAuth(token, username, role = '', nickname = '', remember = true, avatar = '') {
      this.token = token || ''
      this.username = username || ''
      this.role = role || ''
      this.nickname = nickname || ''
      this.avatar = avatar || ''
      writeAuth({
        token: this.token,
        username: this.username,
        role: this.role,
        nickname: this.nickname,
        avatar: this.avatar
      }, remember)
    },
    setProfile(username, role = '', nickname = '', avatar = '') {
      if (username) this.username = username
      if (role) this.role = role
      if (nickname) this.nickname = nickname
      if (avatar !== undefined) this.avatar = avatar || ''
      persistProfile(this)
    },
    setAvatar(avatar) {
      this.avatar = avatar || ''
      persistProfile(this)
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
      this.avatar = ''
      this.permissions = []
      clearAuth()
    }
  }
})