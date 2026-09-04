<template>
	<view class="page">
		<view class="hero">
			<text class="eyebrow">MY RUNNING STORY</text>
			<text class="title">我的燃赛足迹</text>
			<text class="subtitle">所有累计值只统计服务端已完成并结算的运动</text>
		</view>

		<view class="overview-card">
			<view class="calorie-block">
				<text class="calorie">{{ totalCalories }}</text>
				<text class="unit">累计千卡</text>
			</view>
			<view class="overview-grid">
				<view><text>{{ totalDistance }}</text><small>公里</small></view>
				<view><text>{{ stats.workoutCount || 0 }}</text><small>次运动</small></view>
				<view><text>{{ totalDuration }}</text><small>累计时长</small></view>
			</view>
		</view>
		<view class="insight-strip">
			<view><text>{{ stats.longestConsecutiveDays || 0 }}</text><small>最长连续运动天数</small></view>
			<view class="insight-divider" />
			<view><text>{{ stats.cityCount || 0 }}</text><small>留下足迹的城市</small></view>
		</view>

		<view class="trend-card">
			<view class="trend-head">
				<view><text>卡路里燃烧趋势</text><small>{{ trendRangeText }}</small></view>
				<view class="trend-tabs">
					<text :class="{ active: trendDays === 7 }" @tap="changeTrendDays(7)">7天</text>
					<text :class="{ active: trendDays === 30 }" @tap="changeTrendDays(30)">30天</text>
				</view>
			</view>
			<view v-if="trendLoading" class="trend-state">正在汇总真实运动数据…</view>
			<view v-else-if="trendError" class="trend-state trend-error" @tap="loadTrends">{{ trendError }}，点击重试</view>
			<scroll-view v-else scroll-x class="trend-scroll" :show-scrollbar="false">
				<view class="bars" :style="{ width: trendChartWidth }">
					<view v-for="point in trends" :key="point.trendDate" class="bar-item">
						<view class="bar-value">{{ compactCalories(point.totalCaloriesKcal) }}</view>
						<view class="bar-track"><view class="bar-fill" :style="{ height: barHeight(point.totalCaloriesKcal) }" /></view>
						<text>{{ shortDate(point.trendDate) }}</text>
					</view>
				</view>
			</scroll-view>
			<view v-if="!trendLoading && !trendError" class="trend-total">
				<text>本周期燃烧</text><b>{{ number(trendTotalCalories, 1) }} 千卡</b>
			</view>
		</view>

		<view class="section-head">
			<text>运动记录</text>
			<small>共 {{ total }} 次</small>
		</view>

		<view v-if="loading && !workouts.length" class="state-card">正在读取真实运动记录…</view>
		<view v-else-if="errorMessage && !workouts.length" class="state-card error">
			<text>{{ errorMessage }}</text>
			<button @tap="refresh">重新加载</button>
		</view>
		<view v-else-if="!workouts.length" class="state-card">
			<text>还没有完成的运动</text>
			<small>完成跑步或记录一次其他运动后，服务端结算会出现在这里。</small>
			<button class="orange" @tap="goRunning">开始跑步</button>
		</view>

		<view v-else class="list">
			<view v-for="item in workouts" :key="item.id" class="workout-card" @tap="openDetail(item.id)">
				<view class="card-top">
					<view>
						<text class="sport">{{ item.sportName || sportName(item.sportType) }}</text>
						<text class="date">{{ formatDate(item.startedAt) }}</text>
						<text v-if="item.recordSource === 'MANUAL'" class="source-tip">按时长记录 · 不参与排行榜</text>
					</view>
					<text class="arrow">›</text>
				</view>
				<view class="main-result">
					<text>{{ number(item.caloriesKcal, 1) }}</text><small>千卡</small>
				</view>
				<view class="card-metrics">
					<view v-if="item.recordSource !== 'MANUAL'"><b>{{ distanceKm(item.distanceMeters) }}</b><small>公里</small></view>
					<view v-else><b>{{ number(item.metValue, 1) }}</b><small>MET</small></view>
					<view><b>{{ formatDuration(item.elapsedSeconds) }}</b><small>时长</small></view>
					<view v-if="item.recordSource !== 'MANUAL'"><b>{{ formatPace(item.avgPaceSeconds) }}</b><small>平均配速</small></view>
					<view v-else><b>个人</b><small>统计范围</small></view>
				</view>
			</view>
		</view>

		<view v-if="workouts.length" class="load-more" @tap="loadMore">
			{{ loading ? '加载中…' : (hasMore ? '加载更多' : '已经到底了') }}
		</view>
	</view>
</template>

<script>
	import { getWorkoutHistory, getWorkoutStatistics, getWorkoutTrends } from '../../common/workout.js'

	export default {
		data() {
			return {
				stats: {},
				trends: [],
				trendDays: 7,
				trendLoading: false,
				trendError: '',
				workouts: [],
				total: 0,
				pageNum: 1,
				pageSize: 10,
				loading: false,
				hasMore: true,
				errorMessage: ''
			}
		},
		computed: {
			totalCalories() { return this.number(this.stats.totalCaloriesKcal, 1) },
			totalDistance() { return this.distanceKm(this.stats.totalDistanceMeters) },
			totalDuration() { return this.formatLongDuration(this.stats.totalElapsedSeconds) },
			trendTotalCalories() { return this.trends.reduce((sum, item) => sum + Number(item.totalCaloriesKcal || 0), 0) },
			trendMaxCalories() { return Math.max(0, ...this.trends.map(item => Number(item.totalCaloriesKcal || 0))) },
			trendChartWidth() { return `${Math.max(630, this.trendDays * 68)}rpx` },
			trendRangeText() {
				if (!this.trends.length) return `近 ${this.trendDays} 天`
				return `${this.shortDate(this.trends[0].trendDate)} - ${this.shortDate(this.trends[this.trends.length - 1].trendDate)}`
			}
		},
		onLoad() {
			this.refresh()
		},
		onPullDownRefresh() {
			this.refresh().finally(() => uni.stopPullDownRefresh())
		},
		onReachBottom() {
			this.loadMore()
		},
		methods: {
			async refresh() {
				if (this.loading) return
				this.pageNum = 1
				this.hasMore = true
				this.workouts = []
				this.errorMessage = ''
				this.loading = true
				try {
					const [statistics, history] = await Promise.all([
						getWorkoutStatistics(),
						getWorkoutHistory(1, this.pageSize)
					])
					this.stats = statistics.data || {}
					this.applyHistoryPage(history, false)
					this.loadTrends()
				} catch (error) {
					this.errorMessage = error.message || '运动记录加载失败'
				} finally {
					this.loading = false
				}
			},
			async loadTrends() {
				if (this.trendLoading) return
				this.trendLoading = true
				this.trendError = ''
				try {
					const result = await getWorkoutTrends(this.trendDays)
					this.trends = Array.isArray(result.data?.points) ? result.data.points : []
				} catch (error) {
					this.trendError = error.message || '趋势加载失败'
				} finally {
					this.trendLoading = false
				}
			},
			changeTrendDays(days) {
				if (this.trendDays === days || this.trendLoading) return
				this.trendDays = days
				this.loadTrends()
			},
			async loadMore() {
				if (this.loading || !this.hasMore) return
				this.loading = true
				this.errorMessage = ''
				try {
					const nextPage = this.pageNum + 1
					const history = await getWorkoutHistory(nextPage, this.pageSize)
					this.pageNum = nextPage
					this.applyHistoryPage(history, true)
				} catch (error) {
					this.errorMessage = error.message || '加载更多失败'
					uni.showToast({ title: this.errorMessage, icon: 'none' })
				} finally {
					this.loading = false
				}
			},
			applyHistoryPage(result, append) {
				const rows = Array.isArray(result.rows) ? result.rows : []
				this.total = Number(result.total || 0)
				this.workouts = append ? this.workouts.concat(rows) : rows
				this.hasMore = this.workouts.length < this.total && rows.length > 0
			},
			number(value, digits = 0) {
				const parsed = Number(value || 0)
				return Number.isFinite(parsed) ? parsed.toFixed(digits) : Number(0).toFixed(digits)
			},
			distanceKm(meters) { return this.number(Number(meters || 0) / 1000, 2) },
			compactCalories(value) {
				const calories = Number(value || 0)
				return calories >= 1000 ? `${(calories / 1000).toFixed(1)}k` : Math.round(calories)
			},
			barHeight(value) {
				const calories = Number(value || 0)
				if (calories <= 0 || this.trendMaxCalories <= 0) return '6rpx'
				return `${Math.max(16, Math.round(calories / this.trendMaxCalories * 148))}rpx`
			},
			shortDate(value) { return value ? String(value).slice(5).replace('-', '/') : '--' },
			formatDuration(value) {
				const seconds = Math.max(0, Number(value || 0))
				const hours = Math.floor(seconds / 3600)
				const minutes = Math.floor((seconds % 3600) / 60)
				const remain = Math.floor(seconds % 60)
				return [hours, minutes, remain].map(item => String(item).padStart(2, '0')).join(':')
			},
			formatLongDuration(value) {
				const minutes = Math.floor(Number(value || 0) / 60)
				return minutes >= 60 ? `${Math.floor(minutes / 60)}小时${minutes % 60}分` : `${minutes}分钟`
			},
			formatPace(value) {
				const seconds = Number(value)
				if (!Number.isFinite(seconds) || seconds <= 0) return "--'--''"
				return `${Math.floor(seconds / 60)}'${String(Math.floor(seconds % 60)).padStart(2, '0')}''`
			},
			sportName(code) {
				return ({ RUNNING: '户外跑步', WALKING: '健走', CYCLING: '骑行', ROPE_SKIPPING: '跳绳', BADMINTON: '羽毛球', STRENGTH: '力量训练', YOGA: '瑜伽' })[code] || '其他运动'
			},
			formatDate(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '--' },
			openDetail(id) { uni.navigateTo({ url: `/pages/workout-detail/workout-detail?id=${id}` }) },
			goRunning() { uni.navigateTo({ url: '/pages/running/running' }) }
		}
	}
</script>

<style scoped>
	.page { box-sizing: border-box; min-height: 100vh; padding: 40rpx 30rpx 70rpx; background: #f6f2ed; color: #2a201a; }
	.hero { padding: 26rpx 10rpx 32rpx; }
	.eyebrow, .title, .subtitle { display: block; }
	.eyebrow { color: #ff6a00; font-size: 20rpx; font-weight: 800; letter-spacing: 5rpx; }
	.title { margin-top: 10rpx; font-size: 48rpx; font-weight: 900; }
	.subtitle { margin-top: 12rpx; color: #8b7d74; font-size: 23rpx; }
	.overview-card { padding: 36rpx; border-radius: 30rpx; color: #fff; background: linear-gradient(145deg, #4b2410, #1d130e); box-shadow: 0 16rpx 38rpx rgba(72, 35, 15, .18); }
	.calorie-block { text-align: center; }
	.calorie, .unit { display: block; }
	.calorie { color: #ff7b20; font-size: 84rpx; font-weight: 900; line-height: 1.1; }
	.unit { color: rgba(255,255,255,.58); font-size: 23rpx; }
	.overview-grid { display: flex; margin-top: 32rpx; padding-top: 28rpx; border-top: 1rpx solid rgba(255,255,255,.12); }
	.overview-grid view { flex: 1; text-align: center; }
	.overview-grid text, .overview-grid small { display: block; }
	.overview-grid text { font-size: 30rpx; font-weight: 800; }
	.overview-grid small { margin-top: 7rpx; color: rgba(255,255,255,.48); font-size: 20rpx; }
	.insight-strip { display: flex; align-items: center; margin-top: 18rpx; padding: 24rpx 20rpx; border-radius: 24rpx; background: #fff; box-shadow: 0 8rpx 24rpx rgba(70, 47, 32, .05); }
	.insight-strip view:not(.insight-divider) { flex: 1; text-align: center; }
	.insight-strip text, .insight-strip small { display: block; }
	.insight-strip text { color: #cc4d18; font-size: 38rpx; font-weight: 900; }
	.insight-strip small { margin-top: 5rpx; color: #8f8178; font-size: 20rpx; }
	.insight-divider { width: 1rpx; height: 55rpx; background: #eee5df; }
	.trend-card { margin-top: 22rpx; padding: 28rpx 24rpx 22rpx; overflow: hidden; border-radius: 28rpx; color: #fff; background: linear-gradient(145deg, #502710, #22150f); box-shadow: 0 12rpx 30rpx rgba(72, 35, 15, .14); }
	.trend-head { display: flex; align-items: center; justify-content: space-between; }
	.trend-head text, .trend-head small { display: block; }
	.trend-head > view:first-child > text { font-size: 28rpx; font-weight: 850; }
	.trend-head small { margin-top: 7rpx; color: rgba(255,255,255,.45); font-size: 19rpx; }
	.trend-tabs { display: flex; padding: 5rpx; border-radius: 999rpx; background: rgba(255,255,255,.08); }
	.trend-tabs text { padding: 9rpx 17rpx; border-radius: 999rpx; color: rgba(255,255,255,.48); font-size: 19rpx; }
	.trend-tabs text.active { color: #fff; background: #f4691d; }
	.trend-scroll { width: 100%; margin-top: 22rpx; white-space: nowrap; }
	.bars { display: flex; align-items: flex-end; height: 218rpx; }
	.bar-item { display: inline-flex; flex-direction: column; align-items: center; justify-content: flex-end; width: 56rpx; height: 210rpx; margin-right: 12rpx; }
	.bar-value { height: 27rpx; color: rgba(255,255,255,.55); font-size: 16rpx; line-height: 27rpx; }
	.bar-track { display: flex; align-items: flex-end; justify-content: center; width: 20rpx; height: 148rpx; margin-top: 4rpx; overflow: hidden; border-radius: 999rpx; background: rgba(255,255,255,.08); }
	.bar-fill { width: 100%; min-height: 6rpx; border-radius: inherit; background: linear-gradient(180deg, #ffb36d, #ff5b0b); }
	.bar-item > text { margin-top: 8rpx; color: rgba(255,255,255,.38); font-size: 16rpx; }
	.trend-state { padding: 78rpx 10rpx; color: rgba(255,255,255,.5); font-size: 21rpx; text-align: center; }
	.trend-error { color: #ffb3a2; }
	.trend-total { display: flex; justify-content: space-between; padding-top: 18rpx; border-top: 1rpx solid rgba(255,255,255,.09); color: rgba(255,255,255,.5); font-size: 20rpx; }
	.trend-total b { color: #ff9a52; font-size: 23rpx; }
	.section-head { display: flex; justify-content: space-between; align-items: center; margin: 42rpx 8rpx 20rpx; }
	.section-head text { font-size: 32rpx; font-weight: 850; }
	.section-head small { color: #9a8e86; font-size: 22rpx; }
	.state-card { padding: 55rpx 32rpx; border-radius: 26rpx; background: #fff; color: #756b65; text-align: center; }
	.state-card text, .state-card small { display: block; }
	.state-card small { margin-top: 14rpx; font-size: 22rpx; line-height: 1.6; }
	.state-card button { margin-top: 24rpx; border: 0; border-radius: 999rpx; font-size: 24rpx; }
	.state-card button::after { border: 0; }
	.state-card .orange { color: #fff; background: #ff6a00; }
	.state-card.error { color: #b14633; }
	.workout-card { margin-bottom: 22rpx; padding: 30rpx; border-radius: 26rpx; background: #fff; box-shadow: 0 8rpx 24rpx rgba(70, 47, 32, .06); }
	.card-top { display: flex; justify-content: space-between; align-items: center; }
	.sport, .date { display: block; }
	.sport { font-size: 29rpx; font-weight: 800; }
	.date { margin-top: 8rpx; color: #9a8e86; font-size: 21rpx; }
	.source-tip { display: block; margin-top: 8rpx; color: #d8652a; font-size: 19rpx; }
	.arrow { color: #c7bdb6; font-size: 48rpx; }
	.main-result { margin-top: 24rpx; color: #ff6a00; }
	.main-result text { font-size: 58rpx; font-weight: 900; }
	.main-result small { margin-left: 8rpx; font-size: 22rpx; }
	.card-metrics { display: flex; margin-top: 24rpx; padding-top: 22rpx; border-top: 1rpx solid #f0ebe7; }
	.card-metrics view { flex: 1; }
	.card-metrics b, .card-metrics small { display: block; }
	.card-metrics b { font-size: 27rpx; }
	.card-metrics small { margin-top: 5rpx; color: #a0958e; font-size: 19rpx; }
	.load-more { padding: 25rpx; color: #9a8e86; font-size: 22rpx; text-align: center; }
</style>
