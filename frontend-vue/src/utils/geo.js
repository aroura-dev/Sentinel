// 物流物理定位：真实城市经纬度 + 节点进度插值（深圳发货仓 → 收件城市）

// 深圳发货仓（默认出发地）
export const ORIGIN = { lat: 22.5431, lng: 114.0579, label: '深圳仓（出发）' }

// 各发货仓真实坐标（随订单仓库变化起点）
export const WAREHOUSE_COORDS = {
  'WH-SZ': { lat: 22.5431, lng: 114.0579, label: '深圳仓（出发）' },
  'WH-SH': { lat: 31.2304, lng: 121.4737, label: '上海仓（出发）' }
}

// 目的地省份中心坐标（兜底）
const PROVINCE_CENTER = {
  GD: { lat: 23.1291, lng: 113.2644 },
  ZJ: { lat: 30.2741, lng: 120.1551 },
  JS: { lat: 32.0603, lng: 118.7969 },
  SH: { lat: 31.2304, lng: 121.4737 },
  BJ: { lat: 39.9042, lng: 116.4074 },
  SC: { lat: 30.5728, lng: 104.0668 },
  HB: { lat: 30.5928, lng: 114.3055 },
  HEN: { lat: 34.7466, lng: 113.6254 },
  SD: { lat: 36.6512, lng: 117.1201 },
  FJ: { lat: 26.0745, lng: 119.2965 },
  HN: { lat: 28.2282, lng: 112.9388 },
  AH: { lat: 31.8206, lng: 117.2272 },
  HEB: { lat: 38.0428, lng: 114.5149 },
  LN: { lat: 41.8057, lng: 123.4315 },
  SN: { lat: 34.3416, lng: 108.9398 },
  CQ: { lat: 29.563, lng: 106.5516 }
}

// 收件城市 → 真实经纬度
const CITY_COORDS = {
  '广州': { lat: 23.1291, lng: 113.2644 },
  '深圳': { lat: 22.5431, lng: 114.0579 },
  '东莞': { lat: 23.0207, lng: 113.7518 },
  '佛山': { lat: 23.0218, lng: 113.1219 },
  '珠海': { lat: 22.271, lng: 113.5767 },
  '杭州': { lat: 30.2741, lng: 120.1551 },
  '宁波': { lat: 29.8683, lng: 121.544 },
  '温州': { lat: 27.9938, lng: 120.6994 },
  '南京': { lat: 32.0603, lng: 118.7969 },
  '苏州': { lat: 31.2989, lng: 120.5853 },
  '无锡': { lat: 31.4912, lng: 120.3119 },
  '上海': { lat: 31.2304, lng: 121.4737 },
  '北京': { lat: 39.9042, lng: 116.4074 },
  '天津': { lat: 39.3434, lng: 117.3616 },
  '成都': { lat: 30.5728, lng: 104.0668 },
  '重庆': { lat: 29.563, lng: 106.5516 },
  '武汉': { lat: 30.5928, lng: 114.3055 },
  '郑州': { lat: 34.7466, lng: 113.6254 },
  '济南': { lat: 36.6512, lng: 117.1201 },
  '青岛': { lat: 36.0671, lng: 120.3826 },
  '福州': { lat: 26.0745, lng: 119.2965 },
  '厦门': { lat: 24.4798, lng: 118.0894 },
  '长沙': { lat: 28.2282, lng: 112.9388 },
  '合肥': { lat: 31.8206, lng: 117.2272 },
  '石家庄': { lat: 38.0428, lng: 114.5149 },
  '沈阳': { lat: 41.8057, lng: 123.4315 },
  '大连': { lat: 38.914, lng: 121.6147 },
  '西安': { lat: 34.3416, lng: 108.9398 },
  '昆明': { lat: 25.0389, lng: 102.7183 },
  '贵阳': { lat: 26.6477, lng: 106.6302 }
}

// 物流节点 → 全程进度（0 出发 → 1 妥投）
const NODE_PROGRESS = {
  CREATED: 0,
  WAREHOUSE_OUT: 0.06,
  DOMESTIC_PICKED: 0.16,
  EXPORT_CUSTOMS: 0.3,
  IN_TRANSIT: 0.5,
  CUSTOMS_DELAY: 0.55,
  IMPORT_CUSTOMS: 0.75,
  DELIVERY_FAILED: 0.78,
  LAST_MILE: 0.9,
  DELIVERED: 1,
  LOST: 0.6,
  RETURNED: 0.8
}

/** 收件城市经纬度（未知回退到省份中心） */
export function destCoord(city, province) {
  if (city && CITY_COORDS[city]) return CITY_COORDS[city]
  const p = (province || '').toUpperCase()
  return PROVINCE_CENTER[p] || { lat: 30.0, lng: 103.0 }
}

/** 当前节点对应全程进度 */
export function nodeProgress(node) {
  return NODE_PROGRESS[node] ?? 0.5
}

/** 沿 出发地→目的地 直线插值定位 */
export function routePoint(origin, dest, progress) {
  return {
    lat: origin.lat + (dest.lat - origin.lat) * progress,
    lng: origin.lng + (dest.lng - origin.lng) * progress
  }
}
