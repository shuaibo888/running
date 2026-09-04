<template>
	<view class="page">
		<view class="brand-head">
			<view><text class="brand">燃赛路跑</text><text class="slogan">每一步，都算数</text></view>
			<view class="points-pill" @tap="goPoints"><text>燃力值</text><b>{{ points.balance || 0 }}</b></view>
		</view>

		<swiper class="announce" indicator-dots autoplay circular :interval="4500">
			<swiper-item v-for="item in banners" :key="item.title">
				<view class="banner" @tap="handleBanner(item.action)">
					<image :src="item.image" mode="aspectFill" />
					<view class="banner-mask" />
					<view class="banner-copy"><text class="tag">{{ item.tag }}</text><text class="banner-title">{{ item.title }}</text><text class="banner-sub">{{ item.subtitle }}</text></view>
				</view>
			</swiper-item>
		</swiper>

		<view v-if="loading" class="state-card">正在汇总你的燃赛数据…</view>
		<view v-else-if="errorMessage" class="state-card error"><text>{{ errorMessage }}</text><button @tap="loadDashboard">重新加载</button></view>
		<template v-else>
			<view class="card overview">
				<view><text class="num orange">{{ number(statistics.totalCaloriesKcal, 0) }}</text><text class="label">累计卡路里 kcal</text></view>
				<view class="divider" />
				<view><text class="num">{{ distanceKm(statistics.totalDistanceMeters) }}</text><text class="label">累计里程 km</text></view>
				<view class="divider" />
				<view><text class="num">{{ statistics.workoutCount || 0 }}</text><text class="label">运动次数</text></view>
			</view>

			<view class="quick-actions">
				<view class="primary-action" @tap="goRunning"><view class="run-symbol">跑</view><view><b>开始跑步</b><text>实时配速 · 轨迹 · 卡路里</text></view><text class="arrow">›</text></view>
				<view class="secondary-action" @tap="goManual"><b>其他运动</b><text>按时长记录</text></view>
			</view>

			<view class="section-head"><text>勋章展馆</text><small @tap="goAchievements">查看全部 ›</small></view>
			<view class="card medal-hall" @tap="goAchievements">
				<text class="intro">坚持运动解锁专属成就 · 已收藏 {{ unlockedCount }}/{{ achievements.length }} 枚</text>
				<view class="medals">
					<view v-for="item in featuredAchievements" :key="item.id">
						<view class="medal" :class="{ locked: !item.unlocked }">燃</view>
						<text>{{ item.achievementName }}</text>
						<text class="medal-state">{{ item.unlocked ? '已解锁' : `+${item.rewardPoints || 0}积分` }}</text>
					</view>
				</view>
			</view>

			<view class="section-head"><text>文化路跑线路</text><small @tap="goRoutes">选择线路 ›</small></view>
			<view v-if="routes.length" class="route-card" @tap="openRoute(currentRoute.id)">
				<image :src="routeImage(currentRoute)" mode="aspectFill" />
				<view class="route-mask" />
				<view class="route-copy">
					<view class="route-title-row"><text>{{ currentRoute.routeName }}</text><small>{{ currentRoute.selected ? '当前计入' : '推荐线路' }}</small></view>
					<text class="route-sub">{{ currentRoute.startCity }} 至 {{ currentRoute.endCity }} · {{ currentRoute.subtitle }}</text>
					<view class="progress"><view :style="{ width: percent(currentRoute.progressPercent), background: currentRoute.themeColor || '#ff6a00' }" /></view>
					<view class="progress-copy"><text>已跑 {{ distanceKm(currentRoute.accumulatedDistanceMeters) }} km</text><b>{{ percent(currentRoute.progressPercent) }}</b></view>
				</view>
			</view>
			<view v-else class="state-card compact">暂无已上线线路</view>
		</template>
	</view>
</template>

<script>
	import { hasLoginToken } from '../../common/auth.js'
	import { getWorkoutStatistics } from '../../common/workout.js'
	import { getAchievements, getPoints, getRoutes } from '../../common/engagement.js'

	export default {
		data() {
			return {
				banners: [
					{ tag: '文化远征', title: '沿千年水脉向前', subtitle: '真实里程推进京杭大运河线路', image: '/static/running/banner_canal.png', action: 'routes' },
					{ tag: '燃力激励', title: '运动与坚持都有回报', subtitle: '签到、线路节点、成就均可获得积分', image: '/static/running/banner_points.png', action: 'points' },
					{ tag: '多种运动', title: '不止跑步，也能燃烧', subtitle: '健走、骑行、跳绳等统一记录卡路里', image: '/static/running/banner_football.jpg', action: 'manual' }
				],
				statistics: {}, points: {}, achievements: [], routes: [], loading: false, errorMessage: ''
			}
		},
		computed: {
			unlockedCount() { return this.achievements.filter(item => item.unlocked).length },
			featuredAchievements() { return this.achievements.slice(0, 3) },
			currentRoute() { return this.routes.find(item => item.selected) || this.routes[0] || {} }
		},
		onShow() {
			if (!hasLoginToken()) { uni.reLaunch({ url: '/pages/login/login' }); return }
			this.loadDashboard()
		},
		methods: {
			async loadDashboard() {
				if (this.loading) return
				this.loading = true
				this.errorMessage = ''
				try {
					const [stats, points, achievements, routes] = await Promise.all([
						getWorkoutStatistics(), getPoints(), getAchievements(), getRoutes()
					])
					this.statistics = stats.data || {}
					this.points = points.data || {}
					this.achievements = Array.isArray(achievements.data) ? achievements.data : []
					this.routes = Array.isArray(routes.data) ? routes.data : []
				} catch (error) { this.errorMessage = error.message || '首页数据加载失败' }
				finally { this.loading = false }
			},
			number(value, digits = 0) { const n = Number(value || 0); return Number.isFinite(n) ? n.toFixed(digits) : '0' },
			distanceKm(value) { return this.number(Number(value || 0) / 1000, 1) },
			percent(value) { return `${Math.max(0, Math.min(100, Number(value || 0))).toFixed(1)}%` },
			routeImage(route) { return ({ GRAND_CANAL: '/static/running/scene_canal.jpg', GREAT_WALL: '/static/running/scene_greatwall.jpg', LONG_MARCH: '/static/running/scene_longmarch.jpg' })[route.routeCode] || '/static/running/scene_canal.jpg' },
			handleBanner(action) { ({ routes: this.goRoutes, points: this.goPoints, manual: this.goManual }[action] || (() => {}))() },
			goRunning() { uni.navigateTo({ url: '/pages/running/running' }) },
			goManual() { uni.navigateTo({ url: '/pages/manual-workout/manual-workout' }) },
			goPoints() { uni.navigateTo({ url: '/pages/points/points' }) },
			goAchievements() { uni.navigateTo({ url: '/pages/achievements/achievements' }) },
			goRoutes() { uni.switchTab({ url: '/pages/routes/routes' }) },
			openRoute(id) { if (id) uni.navigateTo({ url: `/pages/route-detail/route-detail?id=${id}` }) }
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; padding: calc(env(safe-area-inset-top) + 18rpx) 0 42rpx; background: #f6f2ee; color: #2d211a; }
	.brand-head { display: flex; align-items: center; justify-content: space-between; padding: 12rpx 28rpx 8rpx; }
	.brand, .slogan { display: block; }
	.brand { font-size: 36rpx; font-weight: 900; letter-spacing: 3rpx; }
	.slogan { margin-top: 4rpx; color: #99877c; font-size: 19rpx; letter-spacing: 3rpx; }
	.points-pill { display: flex; align-items: center; padding: 11rpx 19rpx; border-radius: 99rpx; background: #fff1e5; color: #ee6112; }
	.points-pill text { font-size: 19rpx; }
	.points-pill b { margin-left: 9rpx; font-size: 27rpx; }
	.announce { height: 310rpx; margin: 22rpx 24rpx 0; overflow: hidden; border-radius: 28rpx; box-shadow: 0 13rpx 34rpx rgba(77,43,24,.13); }
	.banner, .banner image, .banner-mask { position: absolute; width: 100%; height: 100%; }
	.banner { position: relative; }
	.banner-mask { background: linear-gradient(90deg, rgba(33,16,7,.82), rgba(33,16,7,.12)); }
	.banner-copy { position: absolute; left: 31rpx; right: 31rpx; bottom: 40rpx; color: #fff; }
	.tag { display: inline-block; padding: 5rpx 14rpx; border-radius: 7rpx; background: #ff6a00; font-size: 19rpx; }
	.banner-title, .banner-sub { display: block; }
	.banner-title { margin-top: 11rpx; font-size: 36rpx; font-weight: 850; }
	.banner-sub { margin-top: 7rpx; font-size: 22rpx; opacity: .82; }
	.card { margin: 24rpx; border-radius: 25rpx; background: #fff; box-shadow: 0 8rpx 26rpx rgba(73,46,30,.06); }
	.overview { display: flex; align-items: center; padding: 27rpx 8rpx; text-align: center; }
	.overview>view { flex: 1; }
	.overview .divider { flex: 0 0 1rpx; height: 68rpx; background: #eee7e1; }
	.num, .label { display: block; }
	.num { font-size: 40rpx; font-weight: 850; }
	.num.orange { color: #ff6811; }
	.label { margin-top: 7rpx; color: #a0958d; font-size: 18rpx; }
	.quick-actions { display: flex; gap: 16rpx; margin: 0 24rpx; }
	.primary-action { display: flex; flex: 1; align-items: center; padding: 23rpx; border-radius: 25rpx; background: linear-gradient(135deg, #ff8a39, #f45a04); color: #fff; }
	.run-symbol { display: flex; align-items: center; justify-content: center; width: 70rpx; height: 70rpx; border-radius: 22rpx; background: rgba(255,255,255,.2); font-size: 28rpx; font-weight: 900; }
	.primary-action>view:nth-child(2) { flex: 1; margin-left: 16rpx; }
	.primary-action b, .primary-action text { display: block; }
	.primary-action b { font-size: 27rpx; }
	.primary-action text { margin-top: 4rpx; font-size: 17rpx; opacity: .8; }
	.primary-action .arrow { font-size: 42rpx; }
	.secondary-action { display: flex; flex: 0 0 176rpx; flex-direction: column; justify-content: center; padding: 20rpx; border-radius: 25rpx; background: #2b1c15; color: #fff; }
	.secondary-action b { font-size: 25rpx; }
	.secondary-action text { margin-top: 7rpx; font-size: 18rpx; opacity: .57; }
	.section-head { display: flex; align-items: center; justify-content: space-between; margin: 38rpx 30rpx 16rpx; }
	.section-head>text { font-size: 31rpx; font-weight: 850; }
	.section-head small { color: #e75f14; font-size: 21rpx; }
	.medal-hall { margin-top: 0; padding: 28rpx 24rpx; }
	.intro { display: block; color: #978980; font-size: 21rpx; }
	.medals { display: flex; margin-top: 22rpx; }
	.medals>view { display: flex; flex: 1; min-width: 0; align-items: center; flex-direction: column; text-align: center; }
	.medal { display: flex; align-items: center; justify-content: center; width: 92rpx; height: 92rpx; border: 8rpx double #ffbc67; border-radius: 50%; background: linear-gradient(145deg, #dc7818, #9c4a10); color: #fff7d9; font-size: 31rpx; font-weight: 900; }
	.medal.locked { border-color: #cbc2bc; background: #aaa29d; filter: grayscale(1); opacity: .48; }
	.medals>view>text { overflow: hidden; width: 100%; margin-top: 11rpx; font-size: 21rpx; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }
	.medals .medal-state { margin-top: 4rpx; color: #ed6618; font-size: 17rpx; font-weight: 400; }
	.route-card { position: relative; height: 360rpx; margin: 0 24rpx; overflow: hidden; border-radius: 28rpx; background: #332015; color: #fff; box-shadow: 0 13rpx 35rpx rgba(72,40,22,.17); }
	.route-card>image, .route-mask { position: absolute; width: 100%; height: 100%; }
	.route-mask { background: linear-gradient(180deg, rgba(21,10,5,.1), rgba(21,10,5,.86)); }
	.route-copy { position: absolute; right: 28rpx; bottom: 27rpx; left: 28rpx; }
	.route-title-row { display: flex; align-items: center; justify-content: space-between; }
	.route-title-row>text { font-size: 39rpx; font-weight: 900; }
	.route-title-row small { padding: 6rpx 14rpx; border-radius: 99rpx; background: rgba(255,106,0,.9); font-size: 18rpx; }
	.route-sub { display: block; margin-top: 6rpx; font-size: 21rpx; opacity: .78; }
	.progress { height: 12rpx; margin-top: 22rpx; overflow: hidden; border-radius: 99rpx; background: rgba(255,255,255,.23); }
	.progress view { height: 100%; border-radius: inherit; }
	.progress-copy { display: flex; justify-content: space-between; margin-top: 10rpx; font-size: 20rpx; opacity: .83; }
	.progress-copy b { color: #ffb37e; }
	.state-card { margin: 24rpx; padding: 45rpx 26rpx; border-radius: 25rpx; background: #fff; color: #8d8077; text-align: center; }
	.state-card text { display: block; }
	.state-card button { margin-top: 20rpx; border-radius: 99rpx; background: #ff6a00; color: #fff; font-size: 23rpx; }
	.state-card.error { color: #b44b31; }
	.state-card.compact { margin-top: 0; }
</style>
