import { describe, it, expect, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from './auth'

describe('auth store（RBAC 会话）', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
    setActivePinia(createPinia())
  })

  it('setAuth 持久化 token/username/role/nickname', () => {
    const store = useAuthStore()
    store.setAuth('token-1', 'admin', 'ADMIN', '系统管理员')
    expect(store.token).toBe('token-1')
    expect(store.username).toBe('admin')
    expect(store.role).toBe('ADMIN')
    expect(localStorage.getItem('sentinel_token')).toBe('token-1')
    expect(localStorage.getItem('sentinel_role')).toBe('ADMIN')
    expect(localStorage.getItem('sentinel_nickname')).toBe('系统管理员')
  })

  it('setAuth 未传角色时角色为空（向后兼容旧登录）', () => {
    const store = useAuthStore()
    store.setAuth('token-2', 'cs')
    expect(store.role).toBe('')
  })

  it('logout 清空所有会话 key', () => {
    const store = useAuthStore()
    store.setAuth('token-1', 'admin', 'ADMIN')
    store.logout()
    expect(store.token).toBe('')
    expect(store.role).toBe('')
    expect(localStorage.getItem('sentinel_token')).toBeNull()
    expect(localStorage.getItem('sentinel_role')).toBeNull()
    expect(localStorage.getItem('sentinel_username')).toBeNull()
  })

  it('remember=false 仅写 sessionStorage（关浏览器即退出）', () => {
    const store = useAuthStore()
    store.setAuth('token-s', 'operator', 'OPERATOR', '运营', false)
    expect(sessionStorage.getItem('sentinel_token')).toBe('token-s')
    expect(sessionStorage.getItem('sentinel_role')).toBe('OPERATOR')
    expect(localStorage.getItem('sentinel_token')).toBeNull()
    // 刷新后从 sessionStorage 恢复
    setActivePinia(createPinia())
    const store2 = useAuthStore()
    expect(store2.token).toBe('token-s')
    expect(store2.role).toBe('OPERATOR')
  })
})
