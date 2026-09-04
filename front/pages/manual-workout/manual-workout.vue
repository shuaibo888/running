<template>
	<view class="page">
		<view class="hero">
			<text class="eyebrow">ENERGY RECORD</text>
			<text class="title">每一种运动，都算数</text>
			<text class="subtitle">选择运动与时长，服务端按你的档案体重和 MET 规则统一结算卡路里。</text>
		</view>

		<view v-if="loading" class="state-card">正在加载运动类型…</view>
		<view v-else-if="errorMessage" class="state-card error">
			<text>{{ errorMessage }}</text>
			<button @tap="loadPage">重新加载</button>
		</view>
		<template v-else>
			<view class="sport-grid">
				<view v-for="item in sportTypes" :key="item.sportCode" class="sport-card"
					:class="{ active: selectedCode === item.sportCode }" @tap="selectSport(item)">
					<view class="sport-mark">{{ sportMark(item.sportCode) }}</view>
					<text>{{ item.sportName }}</text>
					<small>{{ number(item.metValue, 1) }} MET</small>
				</view>
			</view>

			<view class="duration-card">
				<view class="card-head"><text>运动时长</text><small>5—600 分钟</small></view>
				<view class="duration-input">
					<input v-model="durationMinutes" type="number" maxlength="3" @blur="normalizeDuration" />
					<text>分钟</text>
				</view>
				<view class="presets">
					<text v-for="minutes in durationPresets" :key="minutes"
						:class="{ active: Number(durationMinutes) === minutes }" @tap="durationMinutes = String(minutes)">{{ minutes }} 分钟</text>
				</view>
			</view>

			<view class="result-card">
				<text class="result-label">预计燃烧</text>
				<view class="result-value"><b>{{ estimatedCalories }}</b><small>千卡</small></view>
				<text class="formula">MET × {{ weightText }} kg × {{ durationText }} 小时</text>
				<view class="notice">此数值是运动消耗估算，不是医疗指标；提交后以服务端保存结果为准。</view>
			</view>

			<button class="submit" :disabled="!canSubmit || submitting" :loading="submitting" @tap="submitWorkout">
				记录这次运动
			</button>
			<text class="ranking-tip">手动记录计入个人累计、趋势与成就，暂不参与地区排行榜。</text>
		</template>
	</view>
</template>

<script>
	import { getDurationSportTypes, recordManualWorkout } from '../../common/workout.js'
	import { getCurrentProfile } from '../../common/profile.js'

	function requestId(prefix) {
		return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
	}

	export default {
		data() {
			return {
				sportTypes: [],
				selectedCode: '',
				durationMinutes: '30',
				durationPresets: [15, 30, 45, 60],
				weightKg: 0,
				loading: false,
				submitting: false,
				errorMessage: ''
			}
		},
		computed: {
			selectedSport() { return this.sportTypes.find(item => item.sportCode === this.selectedCode) || null },
			durationValue() { return Math.max(0, Number(this.durationMinutes || 0)) },
			durationText() { return (this.durationValue / 60).toFixed(2) },
			weightText() { return this.weightKg > 0 ? this.number(this.weightKg, 1) : '--' },
			estimatedCalories() {
				if (!this.selectedSport || this.weightKg <= 0 || this.durationValue <= 0) return '0.0'
				return this.number(Number(this.selectedSport.metValue) * this.weightKg * this.durationValue / 60, 1)
			},
			canSubmit() {
				if (!this.selectedSport || this.weightKg <= 0) return false
				return this.durationValue >= Number(this.selectedSport.minDurationMinutes)
					&& this.durationValue <= Number(this.selectedSport.maxDurationMinutes)
			}
		},
		onLoad() { this.loadPage() },
		methods: {
			async loadPage() {
				if (this.loading) return
				this.loading = true
				this.errorMessage = ''
				try {
					const [sportResult, profileResult] = await Promise.all([
						getDurationSportTypes(),
						getCurrentProfile()
					])
					this.sportTypes = Array.isArray(sportResult.data) ? sportResult.data : []
					this.weightKg = Number(profileResult.data?.weightKg || 0)
					if (!this.weightKg) throw new Error('请先在运动档案中填写体重')
					if (!this.sportTypes.length) throw new Error('暂时没有可记录的运动类型')
					if (!this.selectedCode) this.selectedCode = this.sportTypes[0].sportCode
				} catch (error) {
					this.errorMessage = error.message || '页面加载失败'
				} finally {
					this.loading = false
				}
			},
			selectSport(item) {
				this.selectedCode = item.sportCode
				this.normalizeDuration()
			},
			normalizeDuration() {
				if (!this.selectedSport) return
				const value = Math.round(Number(this.durationMinutes || 0))
				this.durationMinutes = String(Math.max(
					Number(this.selectedSport.minDurationMinutes),
					Math.min(Number(this.selectedSport.maxDurationMinutes), value || 0)
				))
			},
			async submitWorkout() {
				if (!this.canSubmit || this.submitting) return
				this.submitting = true
				try {
					const result = await recordManualWorkout(
						requestId('manual'), this.selectedCode, this.durationValue)
					uni.showToast({ title: '运动已记录', icon: 'success' })
					setTimeout(() => uni.redirectTo({ url: `/pages/workout-detail/workout-detail?id=${result.data.id}` }), 500)
				} catch (error) {
					uni.showToast({ title: error.message || '记录失败', icon: 'none' })
				} finally {
					this.submitting = false
				}
			},
			sportMark(code) {
				return ({ WALKING: '走', CYCLING: '骑', ROPE_SKIPPING: '绳', BADMINTON: '羽', STRENGTH: '力', YOGA: '瑜' })[code] || '动'
			},
			number(value, digits = 0) {
				const parsed = Number(value || 0)
				return Number.isFinite(parsed) ? parsed.toFixed(digits) : Number(0).toFixed(digits)
			}
		}
	}
</script>

<style scoped>
	.page { box-sizing: border-box; min-height: 100vh; padding: 38rpx 28rpx 70rpx; color: #2c201a; background: #f5efe8; }
	.hero { padding: 16rpx 8rpx 34rpx; }
	.hero text { display: block; }
	.eyebrow { color: #f56a20; font-size: 19rpx; font-weight: 900; letter-spacing: 5rpx; }
	.title { margin-top: 10rpx; font-size: 46rpx; font-weight: 900; }
	.subtitle { max-width: 650rpx; margin-top: 14rpx; color: #86776f; font-size: 23rpx; line-height: 1.65; }
	.state-card { padding: 70rpx 30rpx; border-radius: 28rpx; color: #7e7068; background: #fff; text-align: center; }
	.state-card text { display: block; }
	.state-card button { margin-top: 24rpx; border: 0; border-radius: 999rpx; font-size: 24rpx; }
	.state-card button::after { border: 0; }
	.state-card.error { color: #b14633; }
	.sport-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18rpx; }
	.sport-card { display: flex; min-height: 190rpx; flex-direction: column; align-items: center; justify-content: center; border: 2rpx solid transparent; border-radius: 26rpx; background: #fff; box-shadow: 0 8rpx 24rpx rgba(70,47,32,.05); }
	.sport-card.active { border-color: #ff6a00; background: #fff6ef; box-shadow: 0 12rpx 30rpx rgba(255,106,0,.14); }
	.sport-mark { display: flex; width: 68rpx; height: 68rpx; align-items: center; justify-content: center; border-radius: 50%; color: #fff; background: linear-gradient(145deg, #ff9549, #f35b0a); font-size: 29rpx; font-weight: 900; }
	.sport-card text { margin-top: 13rpx; font-size: 25rpx; font-weight: 800; }
	.sport-card small { margin-top: 5rpx; color: #a2938a; font-size: 18rpx; }
	.duration-card, .result-card { margin-top: 22rpx; padding: 30rpx; border-radius: 28rpx; background: #fff; box-shadow: 0 8rpx 24rpx rgba(70,47,32,.05); }
	.card-head { display: flex; align-items: center; justify-content: space-between; }
	.card-head text { font-size: 29rpx; font-weight: 850; }
	.card-head small { color: #a09289; font-size: 19rpx; }
	.duration-input { display: flex; align-items: flex-end; justify-content: center; margin: 28rpx 0; }
	.duration-input input { width: 210rpx; height: 90rpx; border-bottom: 3rpx solid #ff6a00; color: #2c1d15; font-size: 72rpx; font-weight: 900; text-align: center; }
	.duration-input text { margin: 0 0 14rpx 14rpx; color: #88786e; font-size: 23rpx; }
	.presets { display: flex; justify-content: space-between; gap: 12rpx; }
	.presets text { flex: 1; padding: 16rpx 0; border-radius: 999rpx; color: #8e7c72; background: #f7f1ed; font-size: 20rpx; text-align: center; }
	.presets text.active { color: #fff; background: #ff6a00; }
	.result-card { color: #fff; background: linear-gradient(145deg, #4b2410, #1d130e); }
	.result-label { display: block; color: rgba(255,255,255,.55); font-size: 22rpx; text-align: center; }
	.result-value { margin-top: 8rpx; color: #ff8739; text-align: center; }
	.result-value b { font-size: 84rpx; line-height: 1.2; }
	.result-value small { margin-left: 9rpx; font-size: 23rpx; }
	.formula { display: block; margin-top: 5rpx; color: rgba(255,255,255,.52); font-size: 20rpx; text-align: center; }
	.notice { margin-top: 24rpx; padding-top: 22rpx; border-top: 1rpx solid rgba(255,255,255,.1); color: rgba(255,255,255,.47); font-size: 19rpx; line-height: 1.6; }
	.submit { margin-top: 28rpx; border: 0; border-radius: 999rpx; color: #fff; background: linear-gradient(135deg, #ff8a3d, #f55a08); font-size: 28rpx; font-weight: 800; box-shadow: 0 14rpx 28rpx rgba(239,86,8,.22); }
	.submit::after { border: 0; }
	.submit[disabled] { opacity: .46; }
	.ranking-tip { display: block; margin-top: 20rpx; color: #9a8d85; font-size: 19rpx; line-height: 1.6; text-align: center; }
</style>
