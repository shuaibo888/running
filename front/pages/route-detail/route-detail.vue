<template>
	<view class="page">
		<view v-if="loading" class="state">正在加载线路详情…</view>
		<view v-else-if="errorMessage" class="state error"><text>{{ errorMessage }}</text><button @tap="loadRoute">重试</button></view>
		<template v-else-if="route">
			<view class="hero" :style="heroStyle">
				<text class="code">{{ route.routeCode }}</text>
				<text class="name">{{ route.routeName }}</text>
				<text class="subtitle">{{ route.subtitle }}</text>
				<view class="journey"><text>{{ route.startCity }}</text><i /><text>{{ route.endCity }}</text></view>
			</view>
			<view class="content">
				<view class="summary card">
					<view><b>{{ distanceKm(route.accumulatedDistanceMeters) }}</b><small>已跑 km</small></view>
					<view><b>{{ percent(route.progressPercent) }}</b><small>线路进度</small></view>
					<view><b>{{ unlockedCount }}/{{ (route.nodes || []).length }}</b><small>到达节点</small></view>
				</view>
				<text class="description">{{ route.description }}</text>
				<button class="select-button" :class="{ current: route.selected }" :loading="selecting" @tap="selectRoute">
					{{ route.selected ? '当前跑步计入此线路' : '设为当前线路' }}
				</button>
				<text class="switch-tip">切换只影响之后开始的运动，已经完成和正在进行的运动不会被改写。</text>
				<view class="section-title">沿线节点</view>
				<view class="timeline">
					<view v-for="node in route.nodes" :key="node.id" class="node" :class="{ unlocked: node.unlocked }">
						<view class="rail"><i /></view>
						<view class="node-card">
							<view class="node-top"><b>{{ node.nodeName }}</b><text>{{ distanceKm(node.thresholdDistanceMeters) }} km</text></view>
							<text class="story-title">{{ node.unlocked ? node.storyTitle : '尚未到达' }}</text>
							<text class="story">{{ node.unlocked ? node.storyContent : `继续累计 ${remainingKm(node.thresholdDistanceMeters)} km 解锁` }}</text>
							<text v-if="node.unlocked && node.medalName" class="medal">勋章 · {{ node.medalName }}</text>
						</view>
					</view>
				</view>
			</view>
		</template>
	</view>
</template>

<script>
	import { getRouteDetail, selectCurrentRoute } from '../../common/engagement.js'

	export default {
		data() { return { routeId: '', route: null, loading: false, selecting: false, errorMessage: '' } },
		computed: {
			heroStyle() {
				const color = /^#[0-9a-fA-F]{6}$/.test(this.route?.themeColor || '') ? this.route.themeColor : '#8b4a2f'
				return { background: `linear-gradient(145deg, ${color}, #21140e)` }
			},
			unlockedCount() { return (this.route?.nodes || []).filter(node => node.unlocked).length }
		},
		onLoad(query) { this.routeId = String(query.id || ''); this.loadRoute() },
		methods: {
			async loadRoute() {
				if (!this.routeId || this.loading) return
				this.loading = true
				this.errorMessage = ''
				try {
					const result = await getRouteDetail(this.routeId)
					this.route = result.data
				} catch (error) { this.errorMessage = error.message || '线路详情加载失败' }
				finally { this.loading = false }
			},
			async selectRoute() {
				if (this.selecting || this.route?.selected) return
				this.selecting = true
				try {
					const result = await selectCurrentRoute(this.routeId)
					this.route = result.data
					uni.showToast({ title: '已设为当前线路', icon: 'success' })
				} catch (error) { uni.showToast({ title: error.message || '线路切换失败', icon: 'none' }) }
				finally { this.selecting = false }
			},
			percent(value) { return `${Math.max(0, Math.min(100, Number(value || 0))).toFixed(1)}%` },
			distanceKm(value) { return (Number(value || 0) / 1000).toFixed(1) },
			remainingKm(threshold) {
				return (Math.max(0, Number(threshold || 0) - Number(this.route?.accumulatedDistanceMeters || 0)) / 1000).toFixed(1)
			}
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; background: #f4efe9; color: #2c211a; }
	.state { margin: 80rpx 30rpx; padding: 60rpx 30rpx; border-radius: 28rpx; background: #fff; color: #83766e; text-align: center; }
	.state text { display: block; }
	.state button { margin-top: 24rpx; border-radius: 999rpx; font-size: 24rpx; }
	.state.error { color: #b14633; }
	.hero { box-sizing: border-box; min-height: 470rpx; padding: 64rpx 42rpx 80rpx; color: #fff; }
	.code, .name, .subtitle { display: block; }
	.code { color: rgba(255,255,255,.5); font-size: 19rpx; letter-spacing: 4rpx; }
	.name { margin-top: 44rpx; font-size: 62rpx; font-weight: 900; letter-spacing: 5rpx; }
	.subtitle { margin-top: 12rpx; color: rgba(255,255,255,.7); font-size: 25rpx; }
	.journey { display: flex; align-items: center; margin-top: 55rpx; font-size: 23rpx; }
	.journey i { flex: 1; height: 2rpx; margin: 0 20rpx; background: rgba(255,255,255,.4); }
	.content { position: relative; z-index: 2; margin-top: -34rpx; padding: 0 30rpx 80rpx; }
	.card { border-radius: 26rpx; background: #fff; box-shadow: 0 12rpx 32rpx rgba(59,38,24,.08); }
	.summary { display: flex; padding: 30rpx 14rpx; }
	.summary view { flex: 1; text-align: center; }
	.summary b, .summary small { display: block; }
	.summary b { font-size: 31rpx; }
	.summary small { margin-top: 7rpx; color: #a0958d; font-size: 19rpx; }
	.description { display: block; margin: 30rpx 12rpx 0; color: #766a62; font-size: 24rpx; line-height: 1.75; }
	.select-button { margin-top: 30rpx; border: 0; border-radius: 999rpx; color: #fff; background: #ff6a00; font-size: 27rpx; font-weight: 750; }
	.select-button.current { color: #aa5a1d; background: #fff1e5; }
	.select-button::after { border: 0; }
	.switch-tip { display: block; margin: 14rpx 18rpx 0; color: #a3978f; font-size: 20rpx; line-height: 1.5; text-align: center; }
	.section-title { margin: 46rpx 8rpx 22rpx; font-size: 32rpx; font-weight: 850; }
	.node { display: flex; }
	.rail { width: 44rpx; position: relative; }
	.rail::after { content: ''; position: absolute; left: 20rpx; top: 34rpx; bottom: -10rpx; width: 3rpx; background: #ddd3cc; }
	.node:last-child .rail::after { display: none; }
	.rail i { position: absolute; z-index: 1; top: 28rpx; left: 11rpx; width: 20rpx; height: 20rpx; border: 5rpx solid #cfc4bc; border-radius: 50%; background: #f4efe9; }
	.node.unlocked .rail i { border-color: #ff6a00; background: #ffb57f; }
	.node-card { flex: 1; margin-bottom: 22rpx; padding: 26rpx; border-radius: 22rpx; background: rgba(255,255,255,.72); }
	.node.unlocked .node-card { background: #fff; box-shadow: 0 8rpx 24rpx rgba(59,38,24,.06); }
	.node-top { display: flex; justify-content: space-between; align-items: center; }
	.node-top b { font-size: 28rpx; }
	.node-top text { color: #a3978f; font-size: 20rpx; }
	.story-title { display: block; margin-top: 13rpx; color: #b05219; font-size: 23rpx; font-weight: 750; }
	.story { display: block; margin-top: 8rpx; color: #83766e; font-size: 21rpx; line-height: 1.6; }
	.medal { display: inline-block; margin-top: 14rpx; padding: 6rpx 14rpx; border-radius: 999rpx; color: #b25a18; background: #fff0df; font-size: 19rpx; }
</style>
