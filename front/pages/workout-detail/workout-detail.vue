<template>
	<view class="page">
		<view v-if="loading && !workout" class="state">正在加载运动详情…</view>
		<view v-else-if="errorMessage && !workout" class="state error">
			<text>{{ errorMessage }}</text>
			<button @tap="loadDetail">重新加载</button>
		</view>
		<template v-else-if="workout">
			<view v-if="workout.recordSource !== 'MANUAL'" class="map-wrap">
				<map class="map" :latitude="latitude" :longitude="longitude" :scale="16" :polyline="polyline" />
				<cover-view class="badge">有效轨迹 {{ points.length }} 点</cover-view>
			</view>
			<view v-else class="manual-hero">
				<text class="manual-mark">{{ sportMark(workout.sportType) }}</text>
				<text class="manual-name">{{ workout.sportName || '其他运动' }}</text>
				<small>按运动时长与档案体重结算</small>
			</view>
			<view class="sheet">
				<text class="date">{{ formatDate(workout.startedAt) }}</text>
				<view class="calorie"><text>{{ number(workout.caloriesKcal, 1) }}</text><small>千卡</small></view>
				<view class="metrics">
					<view v-if="workout.recordSource !== 'MANUAL'"><b>{{ distanceKm(workout.distanceMeters) }}</b><small>公里</small></view>
					<view v-else><b>{{ number(workout.metValue, 1) }}</b><small>MET</small></view>
					<view><b>{{ formatDuration(workout.elapsedSeconds) }}</b><small>运动时长</small></view>
					<view v-if="workout.recordSource !== 'MANUAL'"><b>{{ formatPace(workout.avgPaceSeconds) }}</b><small>平均配速</small></view>
					<view v-else><b>{{ number(workout.weightKg, 1) }}</b><small>计算体重 kg</small></view>
				</view>
				<view class="audit">
					<view><text>记录方式</text><b>{{ workout.recordSource === 'MANUAL' ? '手动时长' : 'GPS轨迹' }}</b></view>
					<view v-if="workout.recordSource !== 'MANUAL'"><text>服务端轨迹点</text><b>{{ workout.pointCount || 0 }}</b></view>
					<view v-if="workout.recordSource !== 'MANUAL'"><text>过滤异常点</text><b>{{ workout.invalidPointCount || 0 }}</b></view>
					<view><text>地区排行榜</text><b>{{ workout.rankingEligible ? '计入' : '不计入' }}</b></view>
					<view><text>卡路里算法</text><b>{{ workout.calorieAlgorithm || '--' }}</b></view>
				</view>
				<button v-if="workout.recordSource !== 'MANUAL' && hasMorePoints" class="more" :loading="pointsLoading" @tap="loadMorePoints">继续加载轨迹点</button>
				<text v-else-if="workout.recordSource !== 'MANUAL'" class="complete-tip">轨迹回放数据已加载完成</text>
				<text v-else class="complete-tip">本次记录计入个人累计、趋势和成就</text>
			</view>
		</template>
	</view>
</template>

<script>
	import { getWorkoutDetail, getWorkoutTrackPoints } from '../../common/workout.js'

	const TRACK_PAGE_SIZE = 500

	export default {
		data() {
			return {
				workoutId: '',
				workout: null,
				points: [],
				loading: false,
				pointsLoading: false,
				hasMorePoints: true,
				errorMessage: ''
			}
		},
		computed: {
			latitude() { return Number(this.points[0]?.latitude ?? this.workout?.startLatitude ?? 39.9042) },
			longitude() { return Number(this.points[0]?.longitude ?? this.workout?.startLongitude ?? 116.4074) },
			polyline() {
				if (this.points.length < 2) return []
				const segments = [[]]
				this.points.forEach((point, index) => {
					const previous = this.points[index - 1]
					if (previous && this.secondsBetween(previous.recordedAt, point.recordedAt) > 120) segments.push([])
					segments[segments.length - 1].push(point)
				})
				return segments.filter(segment => segment.length >= 2).map(segment => ({
					points: segment.map(point => ({
						latitude: Number(point.latitude),
						longitude: Number(point.longitude)
					})),
					color: '#ff6a00',
					width: 7,
					arrowLine: true,
					borderColor: '#ffffff',
					borderWidth: 2
				}))
			}
		},
		onLoad(query) {
			this.workoutId = String(query.id || '')
			this.loadDetail()
		},
		methods: {
			async loadDetail() {
				if (!this.workoutId || this.loading) return
				this.loading = true
				this.errorMessage = ''
				this.points = []
				this.hasMorePoints = true
				try {
					const result = await getWorkoutDetail(this.workoutId)
					this.workout = result.data
					if (this.workout?.recordSource !== 'MANUAL') await this.loadMorePoints()
				} catch (error) {
					this.errorMessage = error.message || '运动详情加载失败'
				} finally {
					this.loading = false
				}
			},
			async loadMorePoints() {
				if (this.pointsLoading || !this.hasMorePoints) return
				this.pointsLoading = true
				try {
					const last = this.points[this.points.length - 1]
					const afterSequence = last ? Number(last.sequenceNo) : -1
					const result = await getWorkoutTrackPoints(this.workoutId, afterSequence, TRACK_PAGE_SIZE)
					const rows = Array.isArray(result.data) ? result.data : []
					this.points = this.points.concat(rows)
					this.hasMorePoints = rows.length === TRACK_PAGE_SIZE
				} catch (error) {
					uni.showToast({ title: error.message || '轨迹加载失败', icon: 'none' })
				} finally {
					this.pointsLoading = false
				}
			},
			number(value, digits = 0) {
				const parsed = Number(value || 0)
				return Number.isFinite(parsed) ? parsed.toFixed(digits) : Number(0).toFixed(digits)
			},
			distanceKm(meters) { return this.number(Number(meters || 0) / 1000, 2) },
			formatDuration(value) {
				const seconds = Math.max(0, Number(value || 0))
				return [Math.floor(seconds / 3600), Math.floor((seconds % 3600) / 60), Math.floor(seconds % 60)]
					.map(item => String(item).padStart(2, '0')).join(':')
			},
			formatPace(value) {
				const seconds = Number(value)
				if (!Number.isFinite(seconds) || seconds <= 0) return "--'--''"
				return `${Math.floor(seconds / 60)}'${String(Math.floor(seconds % 60)).padStart(2, '0')}''`
			},
			sportMark(code) {
				return ({ WALKING: '走', CYCLING: '骑', ROPE_SKIPPING: '绳', BADMINTON: '羽', STRENGTH: '力', YOGA: '瑜' })[code] || '动'
			},
			secondsBetween(first, second) {
				const parse = value => new Date(typeof value === 'string' ? value.replace(/-/g, '/') : value).getTime()
				const difference = parse(second) - parse(first)
				return Number.isFinite(difference) ? difference / 1000 : 0
			},
			formatDate(value) { return value ? String(value).replace('T', ' ').slice(0, 19) : '--' }
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; background: #f5f1ed; color: #2b211b; }
	.state { margin: 80rpx 30rpx; padding: 60rpx 30rpx; border-radius: 28rpx; background: #fff; color: #83776f; text-align: center; }
	.state text { display: block; }
	.state button { margin-top: 24rpx; border-radius: 999rpx; font-size: 24rpx; }
	.state.error { color: #b14633; }
	.map-wrap { position: relative; height: 48vh; min-height: 600rpx; }
	.map { width: 100%; height: 100%; }
	.badge { position: absolute; left: 28rpx; bottom: 28rpx; padding: 12rpx 24rpx; border-radius: 999rpx; color: #fff; background: rgba(32,22,17,.76); font-size: 22rpx; }
	.manual-hero { display: flex; min-height: 560rpx; flex-direction: column; align-items: center; justify-content: center; color: #fff; background: radial-gradient(circle at 50% 36%, #8a3b17, #24140d 67%); }
	.manual-mark { display: flex; width: 180rpx; height: 180rpx; align-items: center; justify-content: center; border: 2rpx solid rgba(255,255,255,.2); border-radius: 50%; color: #ff8a3d; background: rgba(255,255,255,.06); font-size: 74rpx; font-weight: 900; box-shadow: 0 0 70rpx rgba(255,106,0,.18); }
	.manual-name { margin-top: 28rpx; font-size: 42rpx; font-weight: 900; }
	.manual-hero small { margin-top: 12rpx; color: rgba(255,255,255,.55); font-size: 22rpx; }
	.sheet { position: relative; z-index: 2; box-sizing: border-box; min-height: 52vh; margin-top: -34rpx; padding: 36rpx 34rpx 70rpx; border-radius: 36rpx 36rpx 0 0; background: #fff; }
	.date { display: block; color: #8d8179; font-size: 23rpx; text-align: center; }
	.calorie { margin-top: 18rpx; color: #ff6a00; text-align: center; }
	.calorie text { font-size: 86rpx; font-weight: 900; }
	.calorie small { margin-left: 8rpx; font-size: 24rpx; }
	.metrics { display: flex; margin-top: 28rpx; }
	.metrics view { flex: 1; text-align: center; border-right: 1rpx solid #eee8e3; }
	.metrics view:last-child { border-right: 0; }
	.metrics b, .metrics small { display: block; }
	.metrics b { font-size: 29rpx; }
	.metrics small { margin-top: 8rpx; color: #9c918a; font-size: 19rpx; }
	.audit { margin-top: 34rpx; padding: 10rpx 28rpx; border-radius: 22rpx; background: #faf7f4; }
	.audit view { display: flex; justify-content: space-between; padding: 20rpx 0; border-bottom: 1rpx solid #eee8e3; font-size: 22rpx; }
	.audit view:last-child { border-bottom: 0; }
	.audit text { color: #8d8179; }
	.audit b { max-width: 60%; text-align: right; word-break: break-all; }
	.more { margin-top: 28rpx; border: 0; border-radius: 999rpx; color: #fff; background: #ff6a00; font-size: 25rpx; }
	.more::after { border: 0; }
	.complete-tip { display: block; margin-top: 28rpx; color: #aaa099; font-size: 21rpx; text-align: center; }
</style>
