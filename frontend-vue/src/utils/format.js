// 统一日期时间格式化：兼容后端返回的毫秒数时间戳与字符串两种形态
export function fmtDateTime(v) {
  if (v === null || v === undefined || v === '') return '-'
  if (typeof v === 'number') {
    const d = new Date(v)
    if (Number.isNaN(d.getTime())) return String(v)
    const p = (n) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
  }
  return String(v).slice(0, 19)
}
