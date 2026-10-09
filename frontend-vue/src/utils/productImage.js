const PRODUCT_IMAGES = {
  'SKU-LJ-001': '/images/products/SKU-LJ-001.webp',
  'SKU-LJ-002': '/images/products/SKU-LJ-002.webp',
  'SKU-LJ-003': '/images/products/SKU-LJ-003.jpg',
  'SKU-BL-001': '/images/products/SKU-BL-001.jpg',
  'SKU-BL-002': '/images/products/SKU-BL-002.jpg',
  'SKU-BL-003': '/images/products/SKU-BL-003.jpg',
  'SKU-QH-001': '/images/products/SKU-QH-001.jpg',
  'SKU-QH-002': '/images/products/SKU-QH-002.jpg',
  'SKU-YT-001': '/images/products/SKU-YT-001.jpg',
  'SKU-YT-002': '/images/products/SKU-YT-002.jpg'
}

export function productImage(sku) {
  return sku ? (PRODUCT_IMAGES[sku] || '') : ''
}