<template>
  <div ref="mapEl" class="map-container" :style="{ height: height + 'px' }"></div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const props = defineProps({
  origin: { type: Object, required: true }, // {lat, lng, label}
  dest: { type: Object, required: true },   // {lat, lng, label}
  current: { type: Object, default: null }, // 当前包裹位置（物理定位点）
  originAddr: { type: String, default: '' }, // 发货仓详细地址
  destAddr: { type: String, default: '' },   // 收件人详细地址
  height: { type: Number, default: 320 }
})

const mapEl = ref(null)
let map = null
let routeLine = null
let destMarker = null
let currentMarker = null

// 自定义图标（避免默认图标路径问题）
function makeIcon(color) {
  return L.divIcon({
    className: 'ship-marker',
    html: `<div style="width:16px;height:16px;border-radius:50%;background:${color};border:2px solid #fff;box-shadow:0 0 6px rgba(0,0,0,.4)"></div>`,
    iconSize: [16, 16],
    iconAnchor: [8, 8]
  })
}

function render() {
  if (!map || !props.origin || !props.dest) return
  const origin = props.origin
  const dest = props.dest
  const current = props.current || origin

  if (!routeLine) {
    L.marker([origin.lat, origin.lng], { icon: makeIcon('#0891b2') })
      .addTo(map)
      .bindPopup(`<b>📦 ${origin.label}</b><br/>${props.originAddr || ''}`)
  }
  if (!destMarker) {
    destMarker = L.marker([dest.lat, dest.lng], { icon: makeIcon('#10b981') })
      .addTo(map)
      .bindPopup(`<b>🏠 收件人</b><br/>${props.destAddr || dest.label}`)
  }
  if (!currentMarker) {
    currentMarker = L.marker([current.lat, current.lng], { icon: makeIcon('#ef4444') })
      .addTo(map)
      .bindPopup('<b>📌 包裹当前位置</b>')
      .openPopup()
  } else {
    currentMarker.setLatLng([current.lat, current.lng])
  }

  if (routeLine) routeLine.setLatLngs([[origin.lat, origin.lng], [dest.lat, dest.lng]])
  else routeLine = L.polyline([[origin.lat, origin.lng], [dest.lat, dest.lng]], { color: '#0891b2', weight: 2, dashArray: '6,4' }).addTo(map)

  const bounds = L.latLngBounds([[origin.lat, origin.lng], [dest.lat, dest.lng]])
  map.fitBounds(bounds, { padding: [30, 30] })
}

onMounted(() => {
  if (!mapEl.value) return
  map = L.map(mapEl.value).setView([22.5, 114.0], 4)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '© OpenStreetMap contributors'
  }).addTo(map)
  render()
})

watch(() => props.current, render, { deep: true })

onBeforeUnmount(() => {
  if (map) { map.remove(); map = null }
})
</script>

<style scoped>
.map-container {
  width: 100%;
  border-radius: 8px;
  z-index: 1;
}
</style>
