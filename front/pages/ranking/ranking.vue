<template>
	<view class="page">
		<view class="hero">
			<text class="eyebrow">CALORIE RANKING</text>
			<text class="title">燃力排行榜</text>
			<view class="scope-tabs">
				<view v-for="item in scopes" :key="item.value" :class="{ active: scope === item.value }" @tap="changeScope(item.value)">{{ item.label }}</view>
			</view>
			<view class="period-tabs">
				<view v-for="item in periods" :key="item.value" :class="{ active: period === item.value }" @tap="changePeriod(item.value)">{{ item.label }}</view>
			</view>
		</view>

		<view class="content">
			<view v-if="ranking" class="region-row">
				<view><b>{{ ranking.regionName }}</b><text>{{ periodLabel }}有效卡路里</text></view>
				<small>更新于 {{ formatTime(ranking.generatedAt) }}</small>
			</view>
			<view v-if="loading" class="state">正在计算真实排名…</view>
			<view v-else-if="errorMessage" class="state error">
				<text>{{ errorMessage }}</text>
				<button v-if="needsProfile" @tap="goProfile">完善运动档案</button>
				<button v-else @tap="loadRanking">重新加载</button>
			</view>
			<view v-else-if="ranking && !ranking.entries.length" class="state">当前周期还没有已完成的运动记录</view>

			<view v-else-if="ranking" class="ranking-card">
				<view v-for="entry in ranking.entries" :key="entry.rankNo" class="entry" :class="{ me: entry.currentUser }">
					<view class="rank" :class="`rank-${entry.rankNo}`">{{ entry.rankNo }}</view>
					<view class="avatar">{{ initial(entry.nickname) }}</view>
					<view class="identity"><b>{{ entry.nickname }}</b><small>{{ distanceKm(entry.distanceMeters) }} km · {{ entry.workoutCount }} 次</small></view>
					<view class="calories"><b>{{ number(entry.caloriesKcal, 1) }}</b><small>kcal</small></view>
				</view>
			</view>

			<view v-if="ranking?.currentUserEntry" class="my-rank">
				<view><text>我的名次</text><b>第 {{ ranking.currentUserEntry.rankNo }} 名</b></view>
				<strong>{{ number(ranking.currentUserEntry.caloriesKcal, 1) }} kcal</strong>
			</view>
			<view v-else-if="ranking && ranking.entries.length" class="my-rank muted">完成本周期第一次有效运动后即可上榜</view>
			<text v-if="ranking" class="rule">并列规则：{{ ranking.tieBreakRule }}</text>
			<text v-if="ranking" class="rule region-rule">地区规则：{{ ranking.regionRule }}</text>
		</view>
	</view>
</template>

<script>
	import { getRanking } from '../../common/engagement.js'

	export default {
		data() {
			return {
				scopes: [{ value: 'CITY', label: '全市榜' }, { value: 'PROVINCE', label: '全省榜' }],
				periods: [
					{ value: 'DAY', label: '今日' },
					{ value: 'WEEK', label: '本周' },
					{ value: 'MONTH', label: '本月' },
					{ value: 'ALL', label: '累计' }
				],
				scope: 'CITY',
				period: 'WEEK',
				ranking: null,
				loading: false,
				errorMessage: ''
			}
		},
		computed: {
			periodLabel() { return this.periods.find(item => item.value === this.period)?.label || '' },
			needsProfile() { return this.errorMessage.includes('档案') || this.errorMessage.includes('地区') }
		},
		onLoad() { this.loadRanking() },
		methods: {
			async loadRanking() {
				if (this.loading) return
				this.loading = true
				this.errorMessage = ''
				try {
					const result = await getRanking(this.scope, this.period)
					this.ranking = result.data
				} catch (error) {
					this.ranking = null
					this.errorMessage = error.message || '排行榜加载失败'
				} finally { this.loading = false }
			},
			changeScope(value) { if (this.scope !== value) { this.scope = value; this.loadRanking() } },
			changePeriod(value) { if (this.period !== value) { this.period = value; this.loadRanking() } },
			initial(name) { return String(name || '燃').slice(0, 1) },
			number(value, digits = 0) {
				const parsed = Number(value || 0)
				return Number.isFinite(parsed) ? parsed.toFixed(digits) : Number(0).toFixed(digits)
			},
			distanceKm(value) { return this.number(Number(value || 0) / 1000, 1) },
			formatTime(value) { return value ? String(value).replace('T', ' ').slice(5, 16) : '--' },
			goProfile() { uni.navigateTo({ url: '/pages/profile/profile' }) }
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; background: #f5f1ed; color: #2b211b; }
	.hero { padding: 54rpx 30rpx 68rpx; color: #fff; background: radial-gradient(circle at 80% 0, #a84817, #29160e 62%); }
	.eyebrow, .title { display: block; text-align: center; }
	.eyebrow { color: #ffac70; font-size: 19rpx; font-weight: 800; letter-spacing: 5rpx; }
	.title { margin-top: 12rpx; font-size: 50rpx; font-weight: 900; letter-spacing: 4rpx; }
	.scope-tabs { display: flex; width: 390rpx; margin: 34rpx auto 0; padding: 7rpx; border-radius: 999rpx; background: rgba(255,255,255,.1); }
	.scope-tabs view { flex: 1; padding: 13rpx 0; border-radius: 999rpx; color: rgba(255,255,255,.55); font-size: 24rpx; text-align: center; }
	.scope-tabs view.active { color: #4a2512; background: #fff; font-weight: 800; }
	.period-tabs { display: flex; margin-top: 28rpx; }
	.period-tabs view { flex: 1; color: rgba(255,255,255,.45); font-size: 22rpx; text-align: center; }
	.period-tabs view.active { color: #ff9b58; font-weight: 800; }
	.content { position: relative; z-index: 2; margin-top: -30rpx; padding: 0 28rpx 80rpx; }
	.region-row { display: flex; justify-content: space-between; align-items: center; padding: 28rpx 30rpx; border-radius: 25rpx; background: #fff; box-shadow: 0 10rpx 28rpx rgba(62,39,25,.08); }
	.region-row b, .region-row text { display: block; }
	.region-row b { font-size: 30rpx; }
	.region-row text, .region-row small { margin-top: 5rpx; color: #9b8f87; font-size: 20rpx; }
	.state { margin-top: 22rpx; padding: 55rpx 28rpx; border-radius: 25rpx; background: #fff; color: #877a72; text-align: center; }
	.state text { display: block; }
	.state button { margin-top: 22rpx; border-radius: 999rpx; color: #fff; background: #ff6a00; font-size: 24rpx; }
	.state.error { color: #b14633; }
	.ranking-card { margin-top: 22rpx; overflow: hidden; border-radius: 25rpx; background: #fff; box-shadow: 0 8rpx 26rpx rgba(62,39,25,.06); }
	.entry { display: flex; align-items: center; padding: 24rpx 22rpx; border-bottom: 1rpx solid #f0ebe7; }
	.entry:last-child { border-bottom: 0; }
	.entry.me { background: #fff5ed; }
	.rank { width: 55rpx; color: #9d928b; font-size: 25rpx; font-weight: 800; text-align: center; }
	.rank-1 { color: #e6a317; font-size: 34rpx; }
	.rank-2 { color: #8895a0; font-size: 31rpx; }
	.rank-3 { color: #b66c45; font-size: 29rpx; }
	.avatar { display: flex; align-items: center; justify-content: center; width: 70rpx; height: 70rpx; margin-left: 8rpx; border-radius: 50%; color: #fff; background: linear-gradient(145deg, #ff9b55, #e95c00); font-size: 27rpx; font-weight: 850; }
	.identity { flex: 1; min-width: 0; margin-left: 20rpx; }
	.identity b, .identity small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
	.identity b { font-size: 26rpx; }
	.identity small { margin-top: 7rpx; color: #a0958e; font-size: 19rpx; }
	.calories { text-align: right; }
	.calories b, .calories small { display: block; }
	.calories b { color: #ff6a00; font-size: 29rpx; }
	.calories small { color: #ae9f96; font-size: 17rpx; }
	.my-rank { display: flex; justify-content: space-between; align-items: center; margin-top: 22rpx; padding: 27rpx 30rpx; border-radius: 24rpx; color: #fff; background: linear-gradient(135deg, #ff8129, #ec5900); }
	.my-rank text, .my-rank b { display: block; }
	.my-rank text { font-size: 20rpx; opacity: .72; }
	.my-rank b { margin-top: 4rpx; font-size: 29rpx; }
	.my-rank strong { font-size: 28rpx; }
	.my-rank.muted { display: block; color: #8c8078; background: #fff; font-size: 22rpx; text-align: center; }
	.rule { display: block; margin: 18rpx 14rpx 0; color: #a39890; font-size: 18rpx; line-height: 1.5; text-align: center; }
	.region-rule { margin-top: 8rpx; }
</style>
