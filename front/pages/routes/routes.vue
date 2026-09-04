<template>
	<view class="page">
		<view class="header">
			<text class="eyebrow">VIRTUAL EXPEDITION</text>
			<text class="title">选择文化线路</text>
			<text class="intro">每次真实跑步结束后，服务端有效里程会计入开始运动时选定的线路。</text>
		</view>

		<view v-if="loading" class="state">正在加载线路配置…</view>
		<view v-else-if="errorMessage" class="state error"><text>{{ errorMessage }}</text><button @tap="loadRoutes">重试</button></view>
		<view v-else-if="!routes.length" class="state">暂无已上线的文化线路</view>

		<view v-else class="route-list">
			<view
				v-for="route in routes"
				:key="route.id"
				class="route-card"
				:style="routeCardStyle(route)"
				@tap="openRoute(route.id)"
			>
				<view class="shade" />
				<view class="card-content">
					<view class="topline">
						<text class="route-code">{{ route.routeCode }}</text>
						<text v-if="route.selected" class="selected">当前线路</text>
					</view>
					<text class="route-name">{{ route.routeName }}</text>
					<text class="subtitle">{{ route.subtitle }}</text>
					<view class="cities"><text>{{ route.startCity }}</text><i /><text>{{ route.endCity }}</text></view>
					<view class="progress"><view :style="{ width: percent(route.progressPercent) }" /></view>
					<view class="progress-text">
						<text>已完成 {{ distanceKm(route.accumulatedDistanceMeters) }} km</text>
						<text>{{ percent(route.progressPercent) }}</text>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import { getRoutes } from '../../common/engagement.js'

	export default {
		data() {
			return { routes: [], loading: false, errorMessage: '' }
		},
		onLoad() { this.loadRoutes() },
		onShow() { if (this.routes.length) this.loadRoutes() },
		methods: {
			async loadRoutes() {
				if (this.loading) return
				this.loading = true
				this.errorMessage = ''
				try {
					const result = await getRoutes()
					this.routes = Array.isArray(result.data) ? result.data : []
				} catch (error) {
					this.errorMessage = error.message || '线路加载失败'
				} finally {
					this.loading = false
				}
			},
			routeCardStyle(route) {
				const color = /^#[0-9a-fA-F]{6}$/.test(route.themeColor || '') ? route.themeColor : '#8b4a2f'
				return { background: `linear-gradient(135deg, ${color}, #241711)` }
			},
			percent(value) {
				const parsed = Math.max(0, Math.min(100, Number(value || 0)))
				return `${parsed.toFixed(1)}%`
			},
			distanceKm(value) { return (Number(value || 0) / 1000).toFixed(1) },
			openRoute(id) { uni.navigateTo({ url: `/pages/route-detail/route-detail?id=${id}` }) }
		}
	}
</script>

<style scoped>
	.page { box-sizing: border-box; min-height: 100vh; padding: 48rpx 30rpx 80rpx; background: #f3eee8; color: #2b2019; }
	.header { padding: 12rpx 8rpx 32rpx; }
	.eyebrow, .title, .intro { display: block; }
	.eyebrow { color: #dc5f13; font-size: 20rpx; font-weight: 800; letter-spacing: 4rpx; }
	.title { margin-top: 10rpx; font-size: 48rpx; font-weight: 900; }
	.intro { margin-top: 14rpx; color: #887b72; font-size: 23rpx; line-height: 1.6; }
	.state { padding: 60rpx 30rpx; border-radius: 28rpx; background: #fff; color: #867970; text-align: center; }
	.state text { display: block; }
	.state button { margin-top: 22rpx; border-radius: 999rpx; font-size: 24rpx; }
	.state.error { color: #b14633; }
	.route-card { position: relative; min-height: 350rpx; margin-bottom: 26rpx; overflow: hidden; border-radius: 30rpx; color: #fff; box-shadow: 0 16rpx 34rpx rgba(63, 38, 22, .17); }
	.shade { position: absolute; inset: 0; background: radial-gradient(circle at 82% 18%, rgba(255,255,255,.22), transparent 30%), linear-gradient(0deg, rgba(15,8,4,.28), transparent 60%); }
	.card-content { position: relative; z-index: 1; padding: 32rpx; }
	.topline { display: flex; justify-content: space-between; align-items: center; }
	.route-code { color: rgba(255,255,255,.58); font-size: 18rpx; letter-spacing: 3rpx; }
	.selected { padding: 7rpx 18rpx; border-radius: 999rpx; color: #4a2a18; background: #fff1df; font-size: 20rpx; font-weight: 800; }
	.route-name { display: block; margin-top: 36rpx; font-size: 50rpx; font-weight: 900; letter-spacing: 3rpx; }
	.subtitle { display: block; margin-top: 8rpx; color: rgba(255,255,255,.73); font-size: 23rpx; }
	.cities { display: flex; align-items: center; margin-top: 32rpx; font-size: 22rpx; }
	.cities i { flex: 1; height: 2rpx; margin: 0 18rpx; background: rgba(255,255,255,.42); }
	.progress { height: 12rpx; margin-top: 22rpx; overflow: hidden; border-radius: 999rpx; background: rgba(255,255,255,.2); }
	.progress view { height: 100%; border-radius: inherit; background: #fff; }
	.progress-text { display: flex; justify-content: space-between; margin-top: 12rpx; color: rgba(255,255,255,.7); font-size: 20rpx; }
</style>
