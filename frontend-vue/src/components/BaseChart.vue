<template>
  <div ref="el" class="base-chart" :style="{ height }"></div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, watch, ref } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: String, default: '320px' }
})

const el = ref(null)
let chart = null

function render() {
  if (!chart) chart = echarts.init(el.value)
  chart.setOption(props.option, true)
}

onMounted(() => {
  render()
  window.addEventListener('resize', resize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  if (chart) { chart.dispose(); chart = null }
})
function resize() { if (chart) chart.resize() }
watch(() => props.option, render, { deep: true })
</script>

<style scoped>
.base-chart { width: 100%; }
</style>
