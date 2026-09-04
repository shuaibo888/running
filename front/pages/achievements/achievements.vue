<template>
	<view class="page">
		<view class="hero">
			<text class="eyebrow">HONOR GALLERY</text>
			<text class="title">燃赛成就馆</text>
			<view class="count"><b>{{ unlockedCount }}</b><text>/ {{ achievements.length }} 枚已解锁</text></view>
		</view>
		<view v-if="loading" class="state">正在核对服务端成就…</view>
		<view v-else-if="errorMessage" class="state error"><text>{{ errorMessage }}</text><button @tap="loadAchievements">重试</button></view>
		<view v-else class="grid">
			<view v-for="item in achievements" :key="item.id" class="achievement" :class="{ unlocked: item.unlocked }">
				<view class="medal" :class="String(item.medalLevel || '').toLowerCase()"><text>{{ item.unlocked ? '燃' : '锁' }}</text></view>
				<text class="name">{{ item.achievementName }}</text>
				<text class="description">{{ item.description }}</text>
				<text v-if="item.rewardPoints" class="reward">解锁奖励 +{{ item.rewardPoints }} 积分</text>
				<view class="progress"><view :style="{ width: percent(item.progressPercent) }" /></view>
				<text class="progress-text">{{ metricValue(item) }} / {{ metricThreshold(item) }}</text>
				<text v-if="item.unlocked" class="unlocked-at">{{ formatDate(item.unlockedAt) }} 解锁</text>
			</view>
		</view>
	</view>
</template>

<script>
	import { getAchievements } from '../../common/engagement.js'

	export default {
		data() { return { achievements: [], loading: false, errorMessage: '' } },
		computed: { unlockedCount() { return this.achievements.filter(item => item.unlocked).length } },
		onLoad() { this.loadAchievements() },
		methods: {
			async loadAchievements() {
				if (this.loading) return
				this.loading = true
				this.errorMessage = ''
				try {
					const result = await getAchievements()
					this.achievements = Array.isArray(result.data) ? result.data : []
				} catch (error) { this.errorMessage = error.message || '成就加载失败' }
				finally { this.loading = false }
			},
			percent(value) { return `${Math.max(0, Math.min(100, Number(value || 0))).toFixed(1)}%` },
			metricValue(item) {
				const value = Number(item.currentValue || 0)
				return item.metricType === 'TOTAL_DISTANCE' ? `${(value / 1000).toFixed(1)}km` : value.toFixed(item.metricType === 'TOTAL_CALORIES' ? 1 : 0)
			},
			metricThreshold(item) {
				const value = Number(item.thresholdValue || 0)
				return item.metricType === 'TOTAL_DISTANCE' ? `${(value / 1000).toFixed(1)}km` : value.toFixed(0)
			},
			formatDate(value) { return value ? String(value).slice(0, 10) : '' }
		}
	}
</script>

<style scoped>
	.page { box-sizing: border-box; min-height: 100vh; padding: 48rpx 28rpx 80rpx; background: radial-gradient(circle at 50% 0, #4c2512, #17100c 500rpx); color: #fff; }
	.hero { padding: 20rpx 10rpx 42rpx; text-align: center; }
	.eyebrow, .title { display: block; }
	.eyebrow { color: #ff9a52; font-size: 19rpx; font-weight: 800; letter-spacing: 5rpx; }
	.title { margin-top: 12rpx; font-size: 50rpx; font-weight: 900; letter-spacing: 4rpx; }
	.count { margin-top: 20rpx; color: rgba(255,255,255,.6); }
	.count b { color: #ff7a1c; font-size: 48rpx; }
	.count text { margin-left: 7rpx; font-size: 22rpx; }
	.state { padding: 60rpx 30rpx; border-radius: 28rpx; background: rgba(255,255,255,.08); color: rgba(255,255,255,.65); text-align: center; }
	.state text { display: block; }
	.state button { margin-top: 22rpx; border-radius: 999rpx; font-size: 24rpx; }
	.state.error { color: #ffae9e; }
	.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20rpx; }
	.achievement { padding: 28rpx 22rpx; border: 1rpx solid rgba(255,255,255,.1); border-radius: 26rpx; background: rgba(255,255,255,.055); text-align: center; opacity: .58; }
	.achievement.unlocked { border-color: rgba(255,145,61,.38); background: linear-gradient(150deg, rgba(255,122,28,.18), rgba(255,255,255,.07)); opacity: 1; }
	.medal { display: flex; align-items: center; justify-content: center; width: 100rpx; height: 100rpx; margin: 0 auto; border: 8rpx double #777; border-radius: 50%; background: #39322e; box-shadow: inset 0 0 0 5rpx rgba(255,255,255,.08); }
	.medal text { font-size: 34rpx; font-weight: 900; }
	.unlocked .medal.bronze { border-color: #bd6e45; background: #8d482b; }
	.unlocked .medal.silver { border-color: #d7dce2; background: #89929d; }
	.unlocked .medal.gold { border-color: #ffd56a; background: #b77a15; }
	.name { display: block; margin-top: 20rpx; font-size: 27rpx; font-weight: 850; }
	.description { display: block; min-height: 64rpx; margin-top: 8rpx; color: rgba(255,255,255,.53); font-size: 19rpx; line-height: 1.55; }
	.reward { display: block; margin-top: 10rpx; color: #ffb984; font-size: 19rpx; }
	.progress { height: 9rpx; margin-top: 18rpx; overflow: hidden; border-radius: 999rpx; background: rgba(255,255,255,.11); }
	.progress view { height: 100%; border-radius: inherit; background: linear-gradient(90deg, #ffb16f, #ff6810); }
	.progress-text, .unlocked-at { display: block; margin-top: 9rpx; color: rgba(255,255,255,.48); font-size: 18rpx; }
	.unlocked-at { color: #ffb984; }
</style>
