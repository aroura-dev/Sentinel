<template>
  <router-view />
</template>

<style>
:root {
  --page-bg: #f7f8fa;
  --card-bg: #ffffff;
  --text-title: #1d2129;
  --text-body: #4e5969;
  --text-secondary: #86909c;
  --border-color: #e5e6eb;
  /* 全站统一主色：青绿（物流青绿主题） */
  --brand: #0891b2;
  --brand-light: #06b6d4;
  /* Element Plus 主色覆盖：让按钮/链接/标签/分页等全部跟随青绿，替代默认蓝 #409eff */
  --el-color-primary: #0891b2;
  --el-color-primary-light-1: #219cba;
  --el-color-primary-light-3: #52b2c9;
  --el-color-primary-light-5: #84c8d8;
  --el-color-primary-light-7: #b5dee8;
  --el-color-primary-light-8: #cee9f0;
  --el-color-primary-light-9: #e6f4f7;
  --el-color-primary-dark-2: #06748e;
}
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
html,
body,
#app {
  height: 100%;
  /* 系统无衬线字体栈：Windows 微软雅黑 / Mac 苹方 */
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', 'Segoe UI', 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  background: var(--page-bg);
  color: var(--text-body);
  font-size: 14px;
  font-weight: 400;
}
/* 标题统一半粗 600（不用粗黑 700），长时间阅读舒适 */
h1, h2, h3, h4, h5, h6,
.el-card__header,
.el-dialog__title,
.el-message-box__title,
.el-table__header {
  font-weight: 600;
}
/* Element Plus 卡片/弹窗统一：白底 + 圆角 + 轻量浅阴影 + 细边框（轻量化 B 端） */
.el-card {
  border-radius: 10px;
  border: 1px solid var(--border-color);
  box-shadow: 0 1px 2px rgba(29, 33, 41, 0.03);
}
.el-card.is-always-shadow,
.el-card.is-hover-shadow:focus,
.el-card.is-hover-shadow:hover {
  box-shadow: 0 2px 8px rgba(29, 33, 41, 0.04);
}
.el-dialog {
  border-radius: 8px;
}
/* 表格：默认内容居中对齐；长编号类列（订单号/运单号/单号等）保持左对齐便于阅读 */
.el-table .el-table__cell {
  text-align: center;
}
.el-table .el-table__cell.is-left {
  text-align: left !important;
}
/* 行内“更多操作”图标按钮：灰色、居中、大小适中 */
.el-table .row-more {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border-radius: 8px;
  color: #9aa3ad;
}
.el-table .row-more .el-icon { font-size: 18px; }
.el-table .row-more:hover { color: #0891b2; background: #eef8fa; }
/* 下拉菜单：选项间距均匀，不太挤也不太散 */
.el-dropdown-menu__item { padding: 7px 18px; font-size: 13px; }
</style>
