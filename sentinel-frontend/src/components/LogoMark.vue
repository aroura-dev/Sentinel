<template>
  <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg" :width="size" :height="size" role="img" aria-label="Sentinel">
    <!-- 经纬斜线（最淡层） -->
    <path
      d="M9.8 18.5L38.2 29.5M29.5 9.8L18.5 38.2"
      stroke="#0891b2"
      stroke-width="0.9"
      opacity="0.3"
      fill="none"
    />
    <!-- 水平纬线 -->
    <path d="M8 24h32" stroke="#0891b2" stroke-width="1" opacity="0.4" fill="none" />
    <!-- 经线弧（左右两条） -->
    <path
      d="M24 8c-5.6 5.6-5.6 26 0 31.6M24 8c5.6 5.6 5.6 26 0 31.6"
      stroke="#0891b2"
      stroke-width="1"
      opacity="0.4"
      fill="none"
    />
    <!-- 内圈（次层） -->
    <circle cx="24" cy="24" r="12.6" stroke="#0891b2" stroke-width="1" opacity="0.5" fill="none" />
    <!-- 外圈（主层） -->
    <circle cx="24" cy="24" r="16" stroke="#0891b2" stroke-width="2.4" fill="none" />

    <!-- 起点圆点（发运） -->
    <circle cx="27" cy="27" r="1.8" fill="#0891b2" />
    <!-- 箭杆（细线） -->
    <path d="M27 27L37 11" stroke="#0891b2" stroke-width="2" stroke-linecap="round" fill="none" />
    <!-- 实心三角箭头（飞出） -->
    <path d="M37 11L30.5 9L34.5 3.5Z" fill="#0891b2" />
  </svg>
</template>

<script setup>
defineProps({
  size: { type: [Number, String], default: 40 }
})
</script>
