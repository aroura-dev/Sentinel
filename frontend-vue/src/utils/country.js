// 目的地省份码 → 中文名（展示层转换，库内存省份码驱动业务逻辑）
const PROVINCES = {
  GD: '广东', ZJ: '浙江', JS: '江苏', SH: '上海', BJ: '北京',
  SC: '四川', HB: '湖北', HEN: '河南', SD: '山东', FJ: '福建',
  HN: '湖南', AH: '安徽', HEB: '河北', LN: '辽宁', SN: '陕西', CQ: '重庆'
}

/** 目的地中文名（无映射回退代码，兼容已存中文省名） */
export function countryName(code) {
  if (!code) return '-'
  return PROVINCES[code] || code
}

/** 目的地中文名（纯中文） */
export function countryLabel(code) {
  if (!code) return '-'
  return PROVINCES[code] || code
}

/** 目的地省份码列表（建单目的地下拉用） */
export const COUNTRY_CODES = ['GD', 'ZJ', 'JS', 'SH', 'BJ', 'SC', 'HB', 'HEN', 'SD', 'FJ', 'HN', 'AH', 'HEB', 'LN', 'SN', 'CQ']

/** 价卡区域显示：DEFAULT→全域，省份码→中文，其余原样 */
export function zoneLabel(zone) {
  if (!zone) return '-'
  if (zone === 'DEFAULT') return '全域'
  return countryLabel(zone)
}
