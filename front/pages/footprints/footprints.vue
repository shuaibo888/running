<template>
	<view class="page">
		<view class="hero">
			<view class="map-orbit orbit-one"></view>
			<view class="map-orbit orbit-two"></view>
			<text class="eyebrow">RUNNING FOOTPRINTS</text>
			<text class="title">我的城市足迹</text>
			<text class="subtitle">用真实运动，点亮走过的每一座城</text>
			<view class="total-card">
				<text class="total-number">{{ summary.cityCount }}</text>
				<text class="total-unit">座城市已点亮</text>
			</view>
		</view>

		<view v-if="summary.pendingWorkoutCount" class="notice pending">
			<text class="notice-title">足迹正在生成</text>
			<text class="notice-copy">还有 {{ summary.pendingWorkoutCount }} 次运动正在进行腾讯地图城市解析，完成后会自动出现。</text>
		</view>
		<view v-if="summary.failedWorkoutCount" class="notice failed">
			<view>
				<text class="notice-title">{{ summary.failedWorkoutCount }} 次运动暂未解析成功</text>
				<text class="notice-copy">可能是地图服务或网络暂时不可用，可以重新加入解析队列。</text>
			</view>
			<button :loading="retrying" :disabled="retrying" @tap="retryFailed">重新解析</button>
		</view>

		<view v-if="loading" class="state-card">正在读取真实城市足迹…</view>
		<view v-else-if="errorMessage" class="state-card error">
			<text>{{ errorMessage }}</text>
			<button @tap="loadFootprints">重新加载</button>
		</view>
		<view v-else-if="!summary.cities.length" class="state-card empty">
			<view class="empty-pin">燃</view>
			<text class="empty-title">还没有点亮城市</text>
			<text class="empty-copy">完成一次有效跑步后，系统会异步抽取轨迹并识别运动经过的城市。</text>
		</view>
		<view v-else class="city-list">
			<view v-for="(city, index) in summary.cities" :key="city.cityCode" class="city-card">
				<view class="city-index">{{ String(index + 1).padStart(2, '0') }}</view>
				<view class="city-copy">
					<text class="city-name">{{ city.cityName }}</text>
					<text class="province">{{ city.provinceName || '中国' }} · 行政区划 {{ city.cityCode }}</text>
					<view class="city-meta">
						<text>首次 {{ formatDate(city.firstReachedAt) }}</text>
						<text>最近 {{ formatDate(city.lastReachedAt) }}</text>
					</view>
				</view>
				<view class="count-badge"><b>{{ city.workoutCount }}</b><text>次</text></view>
			</view>
		</view>
		<text class="source-tip">城市来源于服务端有效轨迹抽样和腾讯位置服务解析，不以用户手填地区代替。</text>
	</view>
</template>

<script>
	import { getCityFootprints, retryCityFootprints } from '../../common/engagement.js'

	function emptySummary() {
		return { cityCount: 0, pendingWorkoutCount: 0, failedWorkoutCount: 0, cities: [] }
	}

	export default {
		data() {
			return { summary: emptySummary(), loading: false, retrying: false, errorMessage: '' }
		},
		onLoad() { this.loadFootprints() },
		onPullDownRefresh() { this.loadFootprints(true) },
		methods: {
			async loadFootprints(fromPullDown = false) {
				if (this.loading) return
				this.loading = true
				this.errorMessage = ''
				try {
					const response = await getCityFootprints()
					const data = response.data || {}
					this.summary = {
						cityCount: Number(data.cityCount || 0),
						pendingWorkoutCount: Number(data.pendingWorkoutCount || 0),
						failedWorkoutCount: Number(data.failedWorkoutCount || 0),
						cities: Array.isArray(data.cities) ? data.cities : []
					}
				} catch (error) {
					this.errorMessage = error.message || '城市足迹加载失败'
				} finally {
					this.loading = false
					if (fromPullDown) uni.stopPullDownRefresh()
				}
			},
			async retryFailed() {
				if (this.retrying) return
				this.retrying = true
				try {
					const response = await retryCityFootprints()
					const count = Number(response.data || 0)
					uni.showToast({ title: count ? '已重新加入解析队列' : '没有待重试记录', icon: 'none' })
					await this.loadFootprints()
				} catch (error) {
					uni.showToast({ title: error.message || '重新解析失败', icon: 'none' })
				} finally { this.retrying = false }
			},
			formatDate(value) { return value ? String(value).slice(0, 10) : '—' }
		}
	}
</script>

<style scoped>
	.page { box-sizing: border-box; min-height: 100vh; padding-bottom: calc(env(safe-area-inset-bottom) + 60rpx); background: #f5efe7; color: #322219; }
	.hero { position: relative; overflow: hidden; padding: 62rpx 34rpx 54rpx; background: linear-gradient(145deg, #7f2518, #b64821 56%, #d9762b); color: #fff7e9; text-align: center; }
	.map-orbit { position: absolute; border: 2rpx solid rgba(255,238,206,.18); border-radius: 50%; }
	.orbit-one { width: 470rpx; height: 220rpx; top: 40rpx; left: -170rpx; transform: rotate(22deg); }
	.orbit-two { width: 420rpx; height: 190rpx; right: -180rpx; bottom: 20rpx; transform: rotate(-18deg); }
	.eyebrow, .title, .subtitle { position: relative; z-index: 1; display: block; }
	.eyebrow { color: #ffd39c; font-size: 18rpx; font-weight: 800; letter-spacing: 5rpx; }
	.title { margin-top: 13rpx; font-family: serif; font-size: 48rpx; font-weight: 900; letter-spacing: 5rpx; }
	.subtitle { margin-top: 12rpx; color: rgba(255,245,224,.72); font-size: 22rpx; letter-spacing: 2rpx; }
	.total-card { position: relative; z-index: 1; display: inline-flex; align-items: baseline; margin-top: 34rpx; padding: 16rpx 30rpx; border: 1rpx solid rgba(255,244,219,.3); border-radius: 999rpx; background: rgba(72,16,9,.25); }
	.total-number { color: #ffd07e; font-size: 54rpx; font-weight: 900; }
	.total-unit { margin-left: 9rpx; font-size: 22rpx; }
	.notice, .state-card, .city-card { box-sizing: border-box; margin: 22rpx 24rpx 0; border-radius: 24rpx; background: #fffdf9; box-shadow: 0 8rpx 25rpx rgba(77,45,27,.07); }
	.notice { display: flex; align-items: center; padding: 24rpx 26rpx; border-left: 7rpx solid #d77a28; }
	.notice.failed { border-left-color: #b83b2b; }
	.notice > view { flex: 1; }
	.notice-title, .notice-copy { display: block; }
	.notice-title { font-size: 25rpx; font-weight: 800; }
	.notice-copy { margin-top: 7rpx; color: #8f7b6c; font-size: 20rpx; line-height: 1.5; }
	.notice button { flex-shrink: 0; margin: 0 0 0 18rpx; padding: 0 22rpx; border-radius: 999rpx; background: #a73224; color: #fff; font-size: 21rpx; }
	.notice button::after, .state-card button::after { border: 0; }
	.state-card { padding: 70rpx 36rpx; color: #8e7a6b; text-align: center; }
	.state-card text { display: block; }
	.state-card button { width: 220rpx; margin-top: 24rpx; border-radius: 999rpx; background: #a63b22; color: #fff; font-size: 23rpx; }
	.state-card.error { color: #aa4031; }
	.empty-pin { display: flex; align-items: center; justify-content: center; width: 92rpx; height: 92rpx; margin: 0 auto; border-radius: 50% 50% 50% 12%; background: linear-gradient(145deg, #dc7b2d, #9b2e1e); color: #fff; font-size: 36rpx; font-weight: 900; transform: rotate(-45deg); }
	.empty-pin::first-letter { transform: rotate(45deg); }
	.empty-title { margin-top: 24rpx; color: #3b291f; font-size: 29rpx; font-weight: 800; }
	.empty-copy { margin-top: 10rpx; font-size: 21rpx; line-height: 1.6; }
	.city-list { padding-top: 2rpx; }
	.city-card { display: flex; align-items: center; padding: 27rpx 25rpx; border: 1rpx solid #efe3d7; }
	.city-index { display: flex; align-items: center; justify-content: center; width: 68rpx; height: 68rpx; margin-right: 20rpx; border-radius: 20rpx 20rpx 20rpx 5rpx; background: linear-gradient(145deg, #d87028, #9d3020); color: #ffe5b8; font-size: 22rpx; font-weight: 900; }
	.city-copy { flex: 1; min-width: 0; }
	.city-name, .province { display: block; }
	.city-name { font-family: serif; font-size: 31rpx; font-weight: 900; }
	.province { margin-top: 5rpx; color: #9a8475; font-size: 19rpx; }
	.city-meta { display: flex; flex-wrap: wrap; gap: 15rpx; margin-top: 12rpx; color: #b09c8d; font-size: 18rpx; }
	.count-badge { flex-shrink: 0; margin-left: 14rpx; color: #a63a23; text-align: center; }
	.count-badge b, .count-badge text { display: block; }
	.count-badge b { font-size: 32rpx; }
	.count-badge text { font-size: 18rpx; }
	.source-tip { display: block; margin: 28rpx 40rpx 0; color: #9f8b7d; font-size: 19rpx; line-height: 1.6; text-align: center; }
</style>
