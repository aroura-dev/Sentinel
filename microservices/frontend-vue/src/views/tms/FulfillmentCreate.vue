<template>
  <div class="create-page">
    <!-- 创建成功：顶部展示这笔订单的信息 -->
    <el-card v-if="createdOrder" shadow="never" class="result-card">
      <el-result icon="success" title="订单创建成功" :sub-title="`订单号：${createdOrder.order.order_no}`">
        <template #extra>
          <el-button type="primary" @click="goOrderDetail">查看订单</el-button>
          <el-button @click="continueCreate">继续创建</el-button>
          <el-button @click="$router.push('/tms/order')">返回订单管理</el-button>
        </template>
      </el-result>
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="商家">{{ createdOrder.order.merchant_name }}</el-descriptions-item>
        <el-descriptions-item label="目的地">{{ countryLabel(createdOrder.order.destination_country) }}</el-descriptions-item>
        <el-descriptions-item label="运费">¥{{ Number(createdOrder.quote.freight).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="预计送达时间">{{ createdOrder.order.promise_eta ? fmt(createdOrder.order.promise_eta) : '以物流跟踪为准' }}</el-descriptions-item>
        <el-descriptions-item label="收件人姓名">{{ createdOrder.order.buyer_name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="收件人编号">{{ createdOrder.order.buyer_id }}</el-descriptions-item>
        <el-descriptions-item label="收货地址">{{ [createdOrder.order.buyer_city, createdOrder.order.buyer_address].filter(Boolean).join(' ') || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- ① 基础信息 -->
    <el-card shadow="never" class="sec-card">
      <template #header><b><el-icon class="hd-icon"><Setting /></el-icon> 1 · 基础信息</b><span class="tip">商家与渠道决定运费与时效</span></template>
      <el-form label-width="100px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="商家" required>
            <el-select v-model="form.merchantId" filterable style="width: 100%" placeholder="选择商家" @change="onMerchantChange">
              <el-option v-for="m in merchants" :key="m.id" :label="`${m.merchant_name} (${m.merchant_code})`" :value="m.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="目的地" required>
            <el-select v-model="form.destinationCountry" placeholder="请选择目的地" style="width: 100%" @change="onCountryChange">
              <el-option v-for="c in COUNTRY_CODES" :key="c" :label="countryLabel(c)" :value="c" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="物流渠道" required>
            <el-select v-model="form.channelId" filterable placeholder="请选择渠道" style="width: 100%">
              <el-option v-for="c in channels" :key="c.id" :label="`${c.channel_name}（${c.transit_days_min}-${c.transit_days_max}天）`" :value="c.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="发货仓">
            <el-select v-model="form.warehouseId" clearable style="width: 100%">
              <el-option v-for="w in warehouses" :key="w.id" :label="`${w.warehouse_name} (${w.city})`" :value="w.id" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      </el-form>
    </el-card>

    <!-- ② 商品明细 -->
    <el-card shadow="never" class="sec-card">
      <template #header><b><el-icon class="hd-icon"><Box /></el-icon> 2 · 商品明细</b><span class="tip">合计重量 {{ totalWeight.toFixed(2) }} kg</span></template>
      <div v-for="(item, i) in items" :key="i" class="item-block">
        <div class="item-row">
          <div class="item-field">
            <div class="item-label">商品</div>
            <el-select v-model="item.sku" filterable placeholder="请选择商品" style="width: 300px" @change="onItemsChange">
              <el-option v-for="p in products" :key="p.id" :label="`${p.name}（单件 ${p.weight_kg} kg）`" :value="p.sku" />
            </el-select>
          </div>
          <div class="item-field">
            <div class="item-label">数量</div>
            <el-input-number v-model="item.qty" :min="1" :max="99" />
          </div>
          <el-button link type="danger" class="item-remove" @click="items.splice(i, 1)">移除</el-button>
        </div>
        <!-- 选中商品后的信息：图片 / 名称 / 单件规格 / 合计（面向用户、有温度） -->
        <div v-if="prod(item.sku)" class="item-info">
          <el-image v-if="prod(item.sku)" :src="piImg(item.sku)" class="pi-img" fit="cover"
            :preview-src-list="[piImg(item.sku)]" preview-teleported />
          <div class="pi-main">
            <div class="pi-name">{{ prod(item.sku).name }}</div>
            <div class="pi-specs">
              <span>单件重量 <b>{{ prod(item.sku).weight_kg }} kg</b></span>
              <span>单件体积 <b>{{ prod(item.sku).volume_l }} L</b></span>
              <span>单价 <b>¥{{ Number(prod(item.sku).declared_value).toFixed(2) }}</b></span>
            </div>
            <div class="pi-total">共 <b>{{ item.qty }}</b> 件 · 合计重量 <b>{{ rowWeight(item) }} kg</b> · 合计价格 <b>¥{{ rowValue(item) }}</b></div>
          </div>
        </div>
      </div>
      <el-button type="primary" plain size="small" @click="items.push({ sku: '', qty: 1 })">添加商品</el-button>
    </el-card>

    <!-- ③ 收件人与确认 -->
    <el-card shadow="never" class="sec-card">
      <template #header><b><el-icon class="hd-icon"><Location /></el-icon> 3 · 收件人与确认</b><span class="tip">填写收件人与收货信息</span></template>
      <!-- 收件人表单：标签在外、图标在内、长字段独占一行 -->
      <el-form label-width="108px" class="buyer-form">
        <el-form-item label="收件人姓名" required>
          <el-input v-model="form.buyerName" placeholder="请填写收件人姓名" clearable>
            <template #prefix><el-icon><UserFilled /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="手机号" required>
          <el-input v-model="form.buyerPhone" placeholder="请填写收件人手机号" clearable>
            <template #prefix><el-icon><Iphone /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="收货地址" required>
          <el-input v-model="form.buyerAddress" placeholder="请填写收货地址" clearable>
            <template #prefix><el-icon><Location /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="收件人编号">
          <el-input v-model="form.buyerId" placeholder="收件人编号" clearable>
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="城市">
          <el-input v-model="form.buyerCity" placeholder="请填写所在城市" clearable>
            <template #prefix><el-icon><OfficeBuilding /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="邮编">
          <el-input v-model="form.buyerPostal" placeholder="请填写邮政编码" clearable>
            <template #prefix><el-icon><Postcard /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="业务备注">
          <el-input v-model="form.businessNotes" type="textarea" :rows="2" placeholder="对承运商/仓库的交接说明，如易碎品轻放" />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 分析结果区：运费试算 / 渠道比价 / AI 推荐（置于所有表单之下） -->
    <div ref="resultRef" class="result-area">
      <!-- 运费试算结果：头部突出运费 + 明细网格 -->
      <div v-if="quote" class="quote-box">
        <div class="quote-head">
          <div>
            <div class="quote-title">运费试算结果</div>
            <div class="quote-sub">{{ quote.quote.channelName }} · {{ countryLabel(quote.quote.zone) }}</div>
          </div>
          <div class="quote-freight">预计运费 <b>¥{{ quote.quote.freight }}</b></div>
        </div>
        <el-descriptions :column="4" border size="small">
          <el-descriptions-item label="实重">{{ quote.weightKg }} kg</el-descriptions-item>
          <el-descriptions-item label="体积">{{ quote.volumeL }} L</el-descriptions-item>
          <el-descriptions-item label="收费重量">{{ quote.quote.billableWeight }} kg</el-descriptions-item>
          <el-descriptions-item label="计费方式">{{ modeLabel(quote.quote.mode) }}</el-descriptions-item>
          <el-descriptions-item label="货值">¥{{ quote.declaredValue }}</el-descriptions-item>
          <el-descriptions-item label="区域">{{ countryLabel(quote.quote.zone) }}</el-descriptions-item>
          <el-descriptions-item label="单价">{{ quote.quote.price }}</el-descriptions-item>
          <el-descriptions-item label="渠道">{{ quote.quote.channelName }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <el-card v-if="compareRows.length" shadow="never" class="quote-box">
        <template #header>
          <b>渠道比价</b><span class="tip">点击一行即选中该渠道；按运费升序</span>
        </template>
        <el-table :data="compareRows" border size="small" highlight-current-row @row-click="selectChannel">
          <el-table-column prop="channelName" label="渠道" min-width="180">
            <template #default="{ row }">{{ row.channelName }}（{{ row.channelCode }}）</template>
          </el-table-column>
          <el-table-column label="方式" width="70">
            <template #default="{ row }">
              <el-tag size="small" :type="row.type === 'air' ? 'warning' : row.type === 'sea' ? 'info' : 'primary'">
                {{ { air: '空运', rail: '铁路', sea: '海运', express: '快递' }[row.type] || row.type }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="时效" width="100">
            <template #default="{ row }">{{ row.transitDaysMin }}-{{ row.transitDaysMax }} 天</template>
          </el-table-column>
          <el-table-column label="计费重" width="90">
            <template #default="{ row }">{{ row.billableWeight ?? '-' }} kg</template>
          </el-table-column>
          <el-table-column label="运费" width="110">
            <template #default="{ row }">
              <b v-if="row.freight != null" style="color: #0891b2">¥{{ Number(row.freight).toFixed(2) }}</b>
              <span v-else style="color: #9ca3af">未报价</span>
            </template>
          </el-table-column>
          <el-table-column label="选中" width="70">
            <template #default="{ row }">
              <el-tag v-if="form.channelId === row.channelId" type="success" size="small">✓</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- AI 智能推荐：面向用户、有温度（置于最下方） -->
      <div v-if="aiResult" class="ai-card">
        <div class="ai-head">
          <el-icon class="ai-icon"><MagicStick /></el-icon>
          <div>
            <div class="ai-title">AI 智能推荐</div>
            <div class="ai-sub">根据您的目的地与货物，为您挑选了更合适的渠道</div>
          </div>
          <el-button class="ai-close" link type="info" size="small" @click="closeAi">
            <el-icon><Close /></el-icon>关闭
          </el-button>
        </div>
        <div class="ai-reco">
          <b>{{ aiResult.channelName || aiResult.channelCode }}</b>
          <span v-if="aiResult.carrierName" class="ai-carrier">{{ aiResult.carrierName }}</span>
        </div>
        <div class="ai-badges">
          <span v-if="aiResult.freight != null" class="ai-badge">运费 <b>¥{{ aiResult.freight }}</b></span>
          <span v-if="aiResult.transitDaysMax" class="ai-badge">承诺 <b>{{ aiResult.transitDaysMax }}</b> 天内送达</span>
        </div>
        <div v-if="aiResult.reason" class="ai-reason">{{ aiResult.reason }}</div>
        <div class="ai-foot">
          <el-button v-if="aiResult.recommendedChannelId" type="primary" size="small" @click="useAiChannel">使用该渠道</el-button>
          <span class="ai-note">AI 建议仅供参考，您可随时更换</span>
        </div>
      </div>
    </div>

    <!-- 底部固定操作条：创建是唯一主操作 -->
    <div class="action-bar">
      <el-tooltip content="调用 AI 分析最优渠道，约需数秒" placement="top">
        <el-button type="warning" plain :loading="aiLoading" @click="aiAdvice"><el-icon><MagicStick /></el-icon>AI 渠道推荐</el-button>
      </el-tooltip>
      <el-button :loading="quoting" @click="calcQuote"><el-icon><TrendCharts /></el-icon>运费试算</el-button>
      <el-button :loading="comparing" @click="compareChannels"><el-icon><DataAnalysis /></el-icon>渠道比价</el-button>
      <span class="spacer"></span>
      <el-button @click="leaveCreate">取消</el-button>
      <el-button @click="resetForm"><el-icon><RefreshLeft /></el-icon>清空</el-button>
      <el-button type="primary" :loading="creating" @click="createOrder"><el-icon><Promotion /></el-icon>创建订单</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, inject, nextTick, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { merchantAll, tmsChannelByCountry, warehouseAll, productByMerchant,
  tmsQuote, tmsQuoteCompare, tmsOrderCreate, routeAdvice } from '../../api'
import { countryLabel, COUNTRY_CODES } from '../../utils/country'
import { fmtDateTime } from '../../utils/format'
import { productImage } from '../../utils/productImage'

const router = useRouter()
const fmt = fmtDateTime

// 创建成功结果卡：顶部展示订单信息 + 后续动作
const createdOrder = ref(null)
function goOrderDetail() {
  if (createdOrder.value) router.push(`/tms/order/${createdOrder.value.order.order_no}`)
}
function continueCreate() {
  createdOrder.value = null
  resetForm()
}

// 取消创建：有已填内容时先确认，避免误点丢数据
async function leaveCreate() {
  const hasContent = form.merchantId || form.buyerId || form.buyerAddress ||
    items.value.some((i) => i.sku) || (form.buyerCity || '') || (form.buyerPhone || '')
  if (hasContent) {
    await ElMessageBox.confirm('放弃已填写的订单信息并退出？', '取消创建', {
      type: 'warning', confirmButtonText: '放弃并退出', cancelButtonText: '继续填写'
    })
  }
  router.push('/tms/order')
}

const form = reactive({ merchantId: null, destinationCountry: '', channelId: null, warehouseId: null,
  buyerId: '', buyerName: '', buyerPhone: '', buyerLanguage: 'zh', buyerAddress: '', buyerCity: '', buyerPostal: '', businessNotes: '' })
const items = ref([{ sku: '', qty: 1 }])
const merchants = ref([])
const channels = ref([])
const warehouses = ref([])
const products = ref([])
const quote = ref(null)
const aiResult = ref(null)
const compareRows = ref([])
const resultRef = ref(null)

// 试算/比价结果出现在③卡上方按钮区外，成功后滚动到结果并给出明确提示
function showResult(msg) {
  nextTick(() => resultRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' }))
  ElMessage.success(msg)
}
const quoting = ref(false)
const creating = ref(false)
const aiLoading = ref(false)
const comparing = ref(false)

// 计费方式英文码 → 中文（企业化、看得懂）
const MODE_LABEL = { FIRST_CONTINUED: '首重+续重' }
function modeLabel(m) { return MODE_LABEL[m] || m }

const totalWeight = computed(() => {
  let w = 0
  for (const it of items.value) {
    const p = products.value.find((x) => x.sku === it.sku)
    if (p && it.qty) w += Number(p.weight_kg) * it.qty
  }
  return w
})

// 商品明细：选中商品后的详细信息（单件 / 本行合计）
function prod(sku) { return products.value.find((p) => p.sku === sku) || null }
function piImg(sku) { return `/images/products/${sku}.svg` }
function rowWeight(item) {
  const p = prod(item.sku)
  return p ? (Number(p.weight_kg) * item.qty).toFixed(2) : '-'
}
function rowValue(item) {
  const p = prod(item.sku)
  return p ? (Number(p.declared_value) * item.qty).toFixed(2) : '-'
}

const setDesc = inject('setPageDesc')

onMounted(async () => {
  setDesc('选渠道 → 算运费 / 渠道比价 → 创建订单')
  merchants.value = (await merchantAll()) || []
  warehouses.value = (await warehouseAll()) || []
  await loadChannels()
})

async function loadChannels() {
  channels.value = (await tmsChannelByCountry(form.destinationCountry)) || []
  form.channelId = null // 不自动选第一个，由用户手动选择
}

async function onCountryChange() { await loadChannels(); aiResult.value = null }
async function onMerchantChange() {
  if (form.merchantId) products.value = (await productByMerchant(form.merchantId)) || []
  else products.value = []
  aiResult.value = null
}
function onItemsChange() { quote.value = null; aiResult.value = null }

function resetForm() {
  form.merchantId = null
  form.warehouseId = null
  form.buyerId = ''
  form.buyerName = ''
  form.buyerPhone = ''
  form.buyerLanguage = 'zh'
  form.buyerAddress = ''
  form.buyerCity = ''
  form.buyerPostal = ''
  form.businessNotes = ''
  items.value = [{ sku: '', qty: 1 }]
  products.value = []
  quote.value = null
  aiResult.value = null
  compareRows.value = []
  loadChannels()
}

function buildQuoteBody() {
  return {
    merchantId: form.merchantId,
    channelId: form.channelId,
    destinationCountry: form.destinationCountry,
    items: items.value.filter((i) => i.sku)
  }
}

async function calcQuote() {
  if (!form.merchantId || !form.channelId) return ElMessage.warning('请选择商家与渠道')
  const body = buildQuoteBody()
  if (!body.items.length) return ElMessage.warning('请添加商品明细')
  quoting.value = true
  try { quote.value = await tmsQuote(body) } catch { quote.value = null } finally { quoting.value = false }
}

async function compareChannels() {
  if (!form.merchantId || !form.destinationCountry) return ElMessage.warning('请选择商家与目的地')
  const chosenItems = items.value.filter((i) => i.sku)
  if (!chosenItems.length) return ElMessage.warning('请添加商品明细')
  comparing.value = true
  try {
    compareRows.value = await tmsQuoteCompare({ merchantId: form.merchantId, destinationCountry: form.destinationCountry, items: chosenItems })
    if (compareRows.value.length) showResult(`已为您比价 ${compareRows.value.length} 个渠道`)
    else ElMessage.info('没有可比的渠道，请检查目的地或商品')
  } catch { compareRows.value = []; ElMessage.error('渠道比价失败，请稍后再试') } finally { comparing.value = false }
}

function selectChannel(row) {
  form.channelId = row.channelId
  quote.value = null
  ElMessage.success(`已选中渠道：${row.channelName}`)
}

async function aiAdvice() {
  if (!form.merchantId || !form.destinationCountry) return ElMessage.warning('请选择商家与目的地')
  if (!totalWeight.value) return ElMessage.warning('请先添加商品以计算重量')
  aiLoading.value = true
  try {
    aiResult.value = await routeAdvice({ destCountry: form.destinationCountry, weightKg: String(totalWeight.value) })
  } catch {
    ElMessage.warning('AI 服务暂不可用，已改用人工比价')
  } finally { aiLoading.value = false }
}

function useAiChannel() {
  form.channelId = aiResult.value.recommendedChannelId
  ElMessage.success('已选用 AI 推荐渠道')
}

// 关闭 AI 推荐卡片
function closeAi() { aiResult.value = null }

// 面向用户校验：把"哪不对"说清楚
const PHONE_RE = /^1[3-9]\d{9}$/
function validateForm() {
  if (!form.merchantId) return '请先选择商家'
  if (!form.channelId) return '请先选择物流渠道'
  if (!items.value.some((i) => i.sku)) return '请添加商品明细'
  if (!form.buyerName) return '请填写收件人姓名'
  if (!form.buyerPhone) return '请填写收件人手机号'
  if (!PHONE_RE.test(form.buyerPhone)) return '收件人手机号格式不正确，应为 11 位大陆号码'
  if (!form.buyerAddress) return '请填写收货地址'
  return ''
}

async function createOrder() {
  const err = validateForm()
  if (err) return ElMessage.warning(err)
  const body = buildQuoteBody()
  creating.value = true
  try {
    const res = await tmsOrderCreate({ ...body, ...form })
    // create 返回的 quote 是扁平结构，按「运费试算结果」卡片的
    // { weightKg, volumeL, declaredValue, quote: {...} } 结构回填，避免渲染报错
    const sumItems = (field) => items.value.reduce((s, it) => {
      const p = products.value.find((x) => x.sku === it.sku)
      return s + (p ? Number(p[field]) * it.qty : 0)
    }, 0)
    quote.value = { weightKg: totalWeight.value, volumeL: sumItems('volume_l'), declaredValue: sumItems('declared_value'), quote: res.quote }
    createdOrder.value = { order: res.order, quote: res.quote }
    ElMessage.success(`订单创建成功，订单号 ${res.order.order_no}，运费 ¥${res.quote.freight}`)
    // 清空本单内容，商家/渠道保留，方便继续创建
    form.buyerId = ''
    form.buyerPhone = ''
    form.buyerAddress = ''
    form.buyerCity = ''
    form.buyerPostal = ''
    items.value = [{ sku: '', qty: 1 }]
  } finally { creating.value = false }
}
</script>

<style scoped>
.tip { margin-left: 12px; color: #9ca3af; font-weight: 400; font-size: 12px; }
.hd-icon { vertical-align: -2px; margin-right: 3px; }
.sec-card { margin-bottom: 16px; }
.item-block { margin-bottom: 12px; }
.item-row { display: flex; gap: 16px; align-items: flex-end; }
.item-field { display: flex; flex-direction: column; gap: 6px; }
.item-label { font-size: 13px; color: #606266; }
.item-remove { margin-bottom: 4px; }
.item-info {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  margin: 8px 0 0 4px;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #eef1f4;
  border-radius: 8px;
}
.pi-img { width: 56px; height: 56px; border-radius: 8px; flex: none; border: 1px solid #e2edf2; }
.pi-main { flex: 1; min-width: 0; }
.pi-name { font-size: 14px; font-weight: 600; color: #1d2129; }
.pi-specs { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 6px; font-size: 12px; color: #5f6368; }
.pi-specs b, .pi-total b { color: #0891b2; font-weight: 600; }
.pi-total { margin-top: 6px; font-size: 12px; color: #5f6368; }
/* 收件人表单：输入框限宽，提示词一行放得下 */
.buyer-form .el-form-item { max-width: 560px; margin-bottom: 18px; }
.quote-box { margin-top: 18px; }
.result-area { margin-top: 18px; }

/* 运费试算结果：头部 */
.quote-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  background: linear-gradient(135deg, #eefafd, #ffffff);
  border: 1px solid #d4edf7;
  border-radius: 8px;
  margin-bottom: 12px;
}
.quote-title { font-size: 15px; font-weight: 600; color: #1d2129; }
.quote-sub { font-size: 12px; color: #86909c; margin-top: 2px; }
.quote-freight { font-size: 13px; color: #5f6368; }
.quote-freight b { font-size: 24px; color: #0891b2; font-weight: 700; margin-left: 4px; }

/* AI 智能推荐卡片：面向用户、有温度 */
.ai-card {
  margin-top: 18px;
  border: 1px solid #d4edf7;
  background: linear-gradient(135deg, #eefafd 0%, #ffffff 100%);
  border-radius: 10px;
  padding: 16px 18px;
}
.ai-head { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.ai-close { margin-left: auto; }
.ai-icon { font-size: 22px; color: #0891b2; }
.ai-title { font-size: 15px; font-weight: 600; color: #1d2129; }
.ai-sub { font-size: 12px; color: #86909c; margin-top: 2px; }
.ai-reco { font-size: 16px; font-weight: 600; color: #1d2129; }
.ai-carrier { font-size: 13px; color: #5f6368; font-weight: 400; margin-left: 8px; }
.ai-badges { display: flex; gap: 10px; margin-top: 10px; flex-wrap: wrap; }
.ai-badge {
  background: #fff;
  border: 1px solid #e2edf2;
  border-radius: 6px;
  padding: 3px 10px;
  font-size: 12px;
  color: #5f6368;
}
.ai-badge b { color: #0891b2; font-weight: 600; }
.ai-reason {
  margin-top: 10px;
  font-size: 13px;
  color: #5f6368;
  line-height: 1.6;
  background: rgba(255, 255, 255, 0.6);
  padding: 8px 12px;
  border-radius: 6px;
}
.ai-foot { display: flex; align-items: center; gap: 12px; margin-top: 12px; }
.ai-note { font-size: 12px; color: #9ca3af; }
.create-page { padding-bottom: 8px; }
.result-card { margin-bottom: 16px; }
.result-card :deep(.el-result__title) { font-size: 18px; }
.result-card :deep(.el-result__subtitle) { margin-top: 4px; }
/* 底部操作条：跟随内容自然排布在页面末尾 */
.action-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 12px 16px;
  margin-top: 16px;
}
.spacer { flex: 1; }
</style>
