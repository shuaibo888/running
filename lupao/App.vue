<script>
export default { onLaunch() { console.log('燃赛路跑视觉预览启动') } }
</script>
<style>
page { background:#f7f7f7; color:#2b2b2b; font-family:-apple-system,BlinkMacSystemFont,"PingFang SC","Microsoft YaHei",sans-serif; font-size:28rpx; }
view,text,button,image,swiper,swiper-item { box-sizing:border-box; }
.card { background:#fff; border-radius:20rpx; padding:28rpx; margin:24rpx; box-shadow:0 4rpx 16rpx rgba(0,0,0,.055); }
.section-title { display:flex; align-items:center; margin:30rpx 24rpx 12rpx; font-size:32rpx; font-weight:700; }
.section-title::before { content:''; width:8rpx; height:30rpx; margin-right:12rpx; border-radius:4rpx; background:#ff6a00; }
.text-primary { color:#ff6a00; } .text-gold { color:#f5a623; } .text-muted { color:#999; }
.pill-btn { border:0; border-radius:999rpx; background:linear-gradient(135deg,#ff8a3d,#ff6a00); color:#fff; }
.pill-btn::after,button::after { border:0; }
</style>
