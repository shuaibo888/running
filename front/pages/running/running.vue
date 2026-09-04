<template>
	<view class="page">
		<view class="map-wrap">
			<map
				class="map"
				:latitude="mapLatitude"
				:longitude="mapLongitude"
				:scale="17"
				:polyline="mapPolyline"
				show-location
			/>
			<cover-view class="map-badge">{{ statusText }}</cover-view>
			<cover-view class="map-tip">腾讯地图 · GCJ-02 前台定位</cover-view>
		</view>

		<view class="dashboard">
			<view class="route-tag">计入线路：{{ selectedRouteName }}</view>
			<view class="hero">
				<text class="hero-number">{{ displayCalories }}</text>
				<text class="hero-unit">千卡 · 卡路里</text>
			</view>

			<view class="metrics">
				<view class="metric">
					<text class="metric-value">{{ displayDistance }}</text>
					<text class="metric-label">距离 km</text>
				</view>
				<view class="metric">
					<text class="metric-value">{{ displayPace }}</text>
					<text class="metric-label">平均配速 /km</text>
				</view>
				<view class="metric">
					<text class="metric-value">{{ displayDuration }}</text>
					<text class="metric-label">有效时长</text>
				</view>
			</view>

			<view class="sync-card">
				<view class="sync-row">
					<text class="sync-title">轨迹记录</text>
					<text class="sync-state" :class="uploadState">{{ syncText }}</text>
				</view>
				<view class="progress"><view :style="{ width: progressWidth }" /></view>
				<text class="sync-detail">已采集 {{ totalPointCount }} 个点，待上传 {{ pendingPoints.length }} 个</text>
			</view>

			<view v-if="!workout" class="start-area">
				<text class="privacy-tip">开始后仅在小程序位于前台时持续记录位置；切到后台的持续定位将在后续阶段单独接入并做真机验证。</text>
				<button class="main-button start-button" :loading="starting" @tap="startNewWorkout">开始跑步</button>
			</view>

			<view v-else-if="workout.status !== 'COMPLETED'" class="controls">
				<button v-if="workout.status === 'RUNNING'" class="main-button pause-button" :loading="actionLoading" @tap="pauseCurrentWorkout">暂停</button>
				<button v-else class="main-button resume-button" :loading="actionLoading" @tap="resumeCurrentWorkout">继续</button>
				<button class="main-button finish-button" :loading="actionLoading" @tap="confirmFinish">结束</button>
			</view>

			<view v-else class="completed-card">
				<text>本次运动已完成</text>
				<button class="main-button start-button" @tap="resetCompleted">再跑一次</button>
			</view>
		</view>
	</view>
</template>

<script>
	import {
		finishWorkout,
		getActiveWorkout,
		pauseWorkout,
		resumeWorkout,
		startWorkout,
		uploadTrackBatch
	} from '../../common/workout.js'
	import { getRouteDetail, getRoutes } from '../../common/engagement.js'

	const LOCAL_STATE_KEY = 'running_active_workout_v1'
	const MAX_ACCURACY_METERS = 60
	const MAX_LOCAL_SPEED_MPS = 12
	const MIN_MOVEMENT_METERS = 2
	const MAX_TRACK_GAP_SECONDS = 120
	const AUTO_UPLOAD_POINT_COUNT = 10

	function createClientId(prefix) {
		return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
	}

	function toFiniteNumber(value, fallback = null) {
		const number = Number(value)
		return Number.isFinite(number) ? number : fallback
	}

	function getLocationOnce() {
		return new Promise((resolve, reject) => {
			uni.getLocation({
				type: 'gcj02',
				isHighAccuracy: true,
				highAccuracyExpireTime: 4000,
				success: resolve,
				fail: error => reject(new Error(error.errMsg || '无法获取当前位置'))
			})
		})
	}

	function formatBackendDate(timestamp) {
		const date = new Date(timestamp)
		const pad = value => String(value).padStart(2, '0')
		return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} `
			+ `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
	}

	function haversineMeters(first, second) {
		const radius = 6371008.8
		const toRadians = degree => degree * Math.PI / 180
		const latitudeDelta = toRadians(second.latitude - first.latitude)
		const longitudeDelta = toRadians(second.longitude - first.longitude)
		const a = Math.sin(latitudeDelta / 2) ** 2
			+ Math.cos(toRadians(first.latitude)) * Math.cos(toRadians(second.latitude))
			* Math.sin(longitudeDelta / 2) ** 2
		return radius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
	}

	export default {
		data() {
			return {
				workout: null,
				pendingPoints: [],
				routePoints: [],
				nextSequence: 1,
				lastSampleAt: 0,
				mapLatitude: 39.9042,
				mapLongitude: 116.4074,
				serverSyncedAt: Date.now(),
				now: Date.now(),
				starting: false,
				actionLoading: false,
				collecting: false,
				uploadState: 'idle',
				uploadingPromise: null,
				locationHandler: null,
				timer: null,
				selectedRouteName: '正在读取…',
				loadedRouteId: ''
			}
		},
		computed: {
			pendingDistanceMeters() {
				return this.pendingPoints.reduce((total, point) => total + Number(point._segmentMeters || 0), 0)
			},
			totalDistanceMeters() {
				return Number(this.workout?.distanceMeters || 0) + this.pendingDistanceMeters
			},
			currentElapsedSeconds() {
				if (!this.workout) return 0
				const base = Number(this.workout.elapsedSeconds || 0)
				if (this.workout.status !== 'RUNNING') return base
				return base + Math.max(0, Math.floor((this.now - this.serverSyncedAt) / 1000))
			},
			displayCalories() {
				const weight = Number(this.workout?.weightKg || 0)
				const calories = weight * (this.totalDistanceMeters / 1000) * 1.036
				return calories.toFixed(1)
			},
			displayDistance() {
				return (this.totalDistanceMeters / 1000).toFixed(2)
			},
			displayPace() {
				if (this.totalDistanceMeters < 10) return "--'--''"
				const seconds = Math.round(this.currentElapsedSeconds * 1000 / this.totalDistanceMeters)
				const minutes = Math.floor(seconds / 60)
				return `${minutes}'${String(seconds % 60).padStart(2, '0')}''`
			},
			displayDuration() {
				const seconds = this.currentElapsedSeconds
				const hours = Math.floor(seconds / 3600)
				const minutes = Math.floor((seconds % 3600) / 60)
				const remain = seconds % 60
				return [hours, minutes, remain].map(value => String(value).padStart(2, '0')).join(':')
			},
			mapPolyline() {
				if (this.routePoints.length < 2) return []
				const segments = [[]]
				this.routePoints.forEach(point => {
					if (point._breakBefore && segments[segments.length - 1].length) segments.push([])
					segments[segments.length - 1].push(point)
				})
				return segments.filter(segment => segment.length >= 2).map(segment => ({
					points: segment.map(point => ({ latitude: point.latitude, longitude: point.longitude })),
					color: '#ff6a00',
					width: 7,
					arrowLine: true,
					borderColor: '#ffffff',
					borderWidth: 2
				}))
			},
			statusText() {
				if (this.starting) return '正在获取起点…'
				if (!this.workout) return '尚未开始'
				if (this.workout.status === 'RUNNING') return this.collecting ? '● 正在记录' : '等待前台定位'
				if (this.workout.status === 'PAUSED') return '已暂停'
				return '已完成'
			},
			syncText() {
				if (this.uploadState === 'uploading') return '正在同步'
				if (this.uploadState === 'error') return '等待重试'
				if (this.pendingPoints.length) return '本地安全暂存'
				return '已同步'
			},
			progressWidth() {
				const count = Math.min(AUTO_UPLOAD_POINT_COUNT, this.pendingPoints.length)
				return `${count / AUTO_UPLOAD_POINT_COUNT * 100}%`
			},
			totalPointCount() {
				return Number(this.workout?.pointCount || 0) + this.pendingPoints.length
			}
		},
		onLoad() {
			this.timer = setInterval(() => { this.now = Date.now() }, 1000)
			this.restoreActiveWorkout()
			this.loadCurrentRouteName()
		},
		onShow() {
			if (this.workout?.status === 'RUNNING' && !this.collecting) {
				this.startLocationTracking().catch(() => {})
			}
		},
		onHide() {
			this.stopLocationTracking()
		},
		onUnload() {
			this.stopLocationTracking()
			if (this.timer) clearInterval(this.timer)
		},
		methods: {
			setWorkout(workout) {
				this.workout = workout
				this.serverSyncedAt = Date.now()
				this.now = this.serverSyncedAt
				if (workout?.routeId) this.loadWorkoutRouteName(workout.routeId)
			},
			async loadCurrentRouteName() {
				try {
					const result = await getRoutes()
					const routes = Array.isArray(result.data) ? result.data : []
					const selected = routes.find(route => route.selected) || routes[0]
					this.selectedRouteName = selected?.routeName || '自由跑'
				} catch (error) {
					this.selectedRouteName = '自由跑'
				}
			},
			async loadWorkoutRouteName(routeId) {
				if (String(routeId) === this.loadedRouteId) return
				this.loadedRouteId = String(routeId)
				try {
					const result = await getRouteDetail(routeId)
					this.selectedRouteName = result.data?.routeName || '文化远征'
				} catch (error) {
					this.selectedRouteName = '文化远征'
				}
			},
			async restoreActiveWorkout() {
				try {
					const result = await getActiveWorkout()
					if (!result.data) {
						uni.removeStorageSync(LOCAL_STATE_KEY)
						return
					}
					this.setWorkout(result.data)
					const saved = uni.getStorageSync(LOCAL_STATE_KEY)
					const sameWorkout = saved && String(saved.workoutId) === String(result.data.id)
					const serverLastSequence = Number(result.data.lastTrackSeq || 0)
					if (sameWorkout) {
						const remaining = Array.isArray(saved.pendingPoints)
							? saved.pendingPoints.filter(point => Number(point.sequenceNo) > serverLastSequence)
							: []
						if (remaining.length && Number(remaining[0].sequenceNo) !== serverLastSequence + 1) {
							this.pendingPoints = []
							uni.showToast({ title: '本地轨迹序号有缺口，已从服务端进度继续', icon: 'none' })
						} else {
							this.pendingPoints = remaining
						}
						this.routePoints = Array.isArray(saved.routePoints) ? saved.routePoints.slice(-500) : []
					}
					this.nextSequence = this.pendingPoints.length
						? Number(this.pendingPoints[this.pendingPoints.length - 1].sequenceNo) + 1
						: serverLastSequence + 1
					this.restoreMapAnchor(result.data)
					this.persistLocalState()
					if (result.data.status === 'RUNNING') await this.startLocationTracking()
				} catch (error) {
					uni.showToast({ title: error.message || '恢复运动失败', icon: 'none' })
				}
			},
			restoreMapAnchor(workout) {
				const latitude = toFiniteNumber(workout.endLatitude ?? workout.startLatitude)
				const longitude = toFiniteNumber(workout.endLongitude ?? workout.startLongitude)
				if (latitude === null || longitude === null) return
				this.mapLatitude = latitude
				this.mapLongitude = longitude
				if (!this.routePoints.length) {
					this.routePoints = [{ latitude, longitude, recordedAt: Date.now() }]
				}
			},
			buildPoint(location, sequenceNo) {
				const accuracy = toFiniteNumber(location.accuracy ?? location.horizontalAccuracy)
				const speed = toFiniteNumber(location.speed)
				const altitude = toFiniteNumber(location.altitude)
				const direction = toFiniteNumber(location.direction)
				return {
					sequenceNo,
					recordedAt: Date.now(),
					latitude: toFiniteNumber(location.latitude),
					longitude: toFiniteNumber(location.longitude),
					accuracyMeters: accuracy !== null && accuracy >= 0 ? accuracy : null,
					reportedSpeedMps: speed !== null && speed >= 0 ? speed : null,
					altitudeMeters: altitude !== null && altitude >= -1000 && altitude <= 10000 ? altitude : null,
					directionDegrees: direction !== null && direction >= 0 && direction <= 360 ? direction : null,
					_segmentMeters: 0
				}
			},
			async startNewWorkout() {
				if (this.starting) return
				this.starting = true
				try {
					const location = await getLocationOnce()
					const initialPoint = this.buildPoint(location, 0)
					if (initialPoint.accuracyMeters === null || initialPoint.accuracyMeters > MAX_ACCURACY_METERS) {
						throw new Error('当前定位精度不足，请到开阔区域后重试')
					}
					const result = await startWorkout(createClientId('workout'), this.toApiPoint(initialPoint))
					this.setWorkout(result.data)
					this.pendingPoints = []
					this.nextSequence = 1
					this.lastSampleAt = initialPoint.recordedAt
					this.routePoints = [initialPoint]
					this.mapLatitude = initialPoint.latitude
					this.mapLongitude = initialPoint.longitude
					this.persistLocalState()
					await this.startLocationTracking()
				} catch (error) {
					uni.showToast({ title: error.message || '开始运动失败', icon: 'none', duration: 3000 })
					await this.restoreActiveWorkout()
				} finally {
					this.starting = false
				}
			},
			startLocationTracking() {
				if (this.collecting || this.workout?.status !== 'RUNNING') return Promise.resolve()
				return new Promise((resolve, reject) => {
					uni.startLocationUpdate({
						type: 'gcj02',
						success: () => {
							this.locationHandler = location => this.captureLocation(location)
							uni.onLocationChange(this.locationHandler)
							this.collecting = true
							resolve()
						},
						fail: error => reject(new Error(error.errMsg || '持续定位启动失败'))
					})
				})
			},
			stopLocationTracking() {
				if (this.locationHandler) {
					uni.offLocationChange(this.locationHandler)
					this.locationHandler = null
				}
				if (this.collecting) uni.stopLocationUpdate({})
				this.collecting = false
			},
			captureLocation(location) {
				if (!this.collecting || this.workout?.status !== 'RUNNING') return
				const sampledAt = Date.now()
				if (sampledAt - this.lastSampleAt < 2000) return
				const point = this.buildPoint(location, this.nextSequence)
				if (point.latitude === null || point.longitude === null) return
				this.lastSampleAt = sampledAt
				this.nextSequence += 1
				this.calculateLocalSegment(point)
				this.pendingPoints.push(point)
				this.mapLatitude = point.latitude
				this.mapLongitude = point.longitude
				this.persistLocalState()
				if (this.pendingPoints.length >= AUTO_UPLOAD_POINT_COUNT) {
					this.flushPending(true).catch(() => {})
				}
			},
			calculateLocalSegment(point) {
				const previous = this.routePoints[this.routePoints.length - 1]
				if (!previous || point.accuracyMeters === null || point.accuracyMeters > MAX_ACCURACY_METERS) return
				const seconds = (point.recordedAt - previous.recordedAt) / 1000
				if (seconds <= 0 || seconds > MAX_TRACK_GAP_SECONDS) {
					point._breakBefore = true
					this.routePoints.push(point)
					return
				}
				const distance = haversineMeters(previous, point)
				const previousAccuracy = previous.accuracyMeters ?? MAX_ACCURACY_METERS
				const accuracyNoiseFloor = Math.min(15, (previousAccuracy + point.accuracyMeters) * .25)
				if (distance >= Math.max(MIN_MOVEMENT_METERS, accuracyNoiseFloor)
					&& distance / seconds <= MAX_LOCAL_SPEED_MPS) {
					point._segmentMeters = distance
					this.routePoints.push(point)
					if (this.routePoints.length > 500) this.routePoints.shift()
				}
			},
			toApiPoint(point) {
				return {
					sequenceNo: point.sequenceNo,
					recordedAt: formatBackendDate(point.recordedAt),
					latitude: point.latitude,
					longitude: point.longitude,
					accuracyMeters: point.accuracyMeters,
					reportedSpeedMps: point.reportedSpeedMps,
					altitudeMeters: point.altitudeMeters,
					directionDegrees: point.directionDegrees
				}
			},
			async flushPending(silent = false) {
				if (!this.workout || !this.pendingPoints.length) return
				if (this.uploadingPromise) {
					await this.uploadingPromise
					if (this.pendingPoints.length >= AUTO_UPLOAD_POINT_COUNT) return this.flushPending(silent)
					return
				}
				// 固定十点一个批次；即使“服务端成功但响应丢失”，重试边界和幂等 ID 也保持不变。
				const points = this.pendingPoints.slice(0, AUTO_UPLOAD_POINT_COUNT)
				const firstSequence = points[0].sequenceNo
				const lastSequence = points[points.length - 1].sequenceNo
				const batchId = `track-${this.workout.id}-${firstSequence}-${lastSequence}`
				this.uploadState = 'uploading'
				this.uploadingPromise = uploadTrackBatch(
					this.workout.id,
					batchId,
					points.map(point => this.toApiPoint(point))
				)
				try {
					const result = await this.uploadingPromise
					this.pendingPoints = this.pendingPoints.filter(point => point.sequenceNo > lastSequence)
					this.setWorkout(result.data)
					this.uploadState = 'idle'
					this.persistLocalState()
				} catch (error) {
					this.uploadState = 'error'
					if (!silent) uni.showToast({ title: error.message || '轨迹同步失败', icon: 'none' })
					throw error
				} finally {
					this.uploadingPromise = null
				}
				if (this.pendingPoints.length && (!silent || this.pendingPoints.length >= AUTO_UPLOAD_POINT_COUNT)) {
					return this.flushPending(silent)
				}
			},
			async pauseCurrentWorkout() {
				if (this.actionLoading) return
				this.actionLoading = true
				this.stopLocationTracking()
				try {
					await this.flushPending(false)
					const result = await pauseWorkout(this.workout.id)
					this.setWorkout(result.data)
					this.persistLocalState()
				} catch (error) {
					uni.showToast({ title: error.message || '暂停失败', icon: 'none' })
					if (this.workout?.status === 'RUNNING') this.startLocationTracking().catch(() => {})
				} finally {
					this.actionLoading = false
				}
			},
			async resumeCurrentWorkout() {
				if (this.actionLoading) return
				this.actionLoading = true
				try {
					const result = await resumeWorkout(this.workout.id)
					this.setWorkout(result.data)
					this.persistLocalState()
					await this.startLocationTracking()
				} catch (error) {
					uni.showToast({ title: error.message || '继续运动失败', icon: 'none' })
				} finally {
					this.actionLoading = false
				}
			},
			confirmFinish() {
				uni.showModal({
					title: '结束本次运动？',
					content: '将先上传本地暂存轨迹，再由服务器结算最终距离、配速和卡路里。',
					confirmText: '确认结束',
					confirmColor: '#ff6a00',
					success: result => { if (result.confirm) this.finishCurrentWorkout() }
				})
			},
			async finishCurrentWorkout() {
				if (this.actionLoading) return
				this.actionLoading = true
				this.stopLocationTracking()
				try {
					await this.flushPending(false)
					const result = await finishWorkout(this.workout.id, createClientId('finish'))
					this.setWorkout(result.data)
					uni.removeStorageSync(LOCAL_STATE_KEY)
					uni.showModal({
						title: '本次运动已完成',
						content: `消耗 ${Number(result.data.caloriesKcal || 0).toFixed(1)} 千卡，运动 ${(Number(result.data.distanceMeters || 0) / 1000).toFixed(2)} 公里`,
						showCancel: false
					})
				} catch (error) {
					uni.showToast({ title: error.message || '结束运动失败', icon: 'none' })
					if (this.workout?.status === 'RUNNING') this.startLocationTracking().catch(() => {})
				} finally {
					this.actionLoading = false
				}
			},
			resetCompleted() {
				this.workout = null
				this.pendingPoints = []
				this.routePoints = []
				this.nextSequence = 1
				this.uploadState = 'idle'
			},
			persistLocalState() {
				if (!this.workout || this.workout.status === 'COMPLETED') return
				uni.setStorageSync(LOCAL_STATE_KEY, {
					workoutId: String(this.workout.id),
					pendingPoints: this.pendingPoints,
					routePoints: this.routePoints.slice(-500),
					nextSequence: this.nextSequence
				})
			}
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; background: #17110d; color: #fff; }
	.map-wrap { position: relative; height: 39vh; min-height: 520rpx; overflow: hidden; background: #e9e4df; }
	.map { width: 100%; height: 100%; }
	.map-badge, .map-tip { position: absolute; left: 28rpx; color: #fff; background: rgba(24, 17, 13, .78); border-radius: 999rpx; padding: 12rpx 24rpx; font-size: 24rpx; }
	.map-badge { top: calc(24rpx + env(safe-area-inset-top)); font-weight: 700; }
	.map-tip { bottom: 24rpx; color: rgba(255, 255, 255, .86); }
	.dashboard { box-sizing: border-box; min-height: 61vh; margin-top: -34rpx; position: relative; z-index: 2; padding: 34rpx 36rpx calc(42rpx + env(safe-area-inset-bottom)); border-radius: 36rpx 36rpx 0 0; background: linear-gradient(160deg, #3c1c0b 0%, #1d140f 55%, #15100d 100%); box-shadow: 0 -16rpx 40rpx rgba(31, 15, 7, .22); }
	.route-tag { width: max-content; max-width: 90%; margin: 0 auto; padding: 8rpx 24rpx; border: 2rpx solid rgba(255, 163, 90, .45); border-radius: 999rpx; color: #ffd2b0; font-size: 23rpx; text-align: center; }
	.hero { margin-top: 26rpx; text-align: center; }
	.hero-number, .hero-unit { display: block; }
	.hero-number { color: #ff7a19; font-size: 120rpx; line-height: 1.05; font-weight: 900; letter-spacing: -5rpx; text-shadow: 0 8rpx 32rpx rgba(255, 106, 0, .2); }
	.hero-unit { margin-top: 8rpx; color: rgba(255, 255, 255, .68); font-size: 25rpx; }
	.metrics { display: flex; margin-top: 32rpx; }
	.metric { flex: 1; text-align: center; border-right: 1rpx solid rgba(255, 255, 255, .12); }
	.metric:last-child { border-right: 0; }
	.metric-value, .metric-label { display: block; }
	.metric-value { min-height: 56rpx; font-size: 36rpx; font-weight: 750; }
	.metric-label { margin-top: 8rpx; color: rgba(255, 255, 255, .5); font-size: 21rpx; }
	.sync-card { margin-top: 32rpx; padding: 24rpx 28rpx; border: 1rpx solid rgba(255, 255, 255, .12); border-radius: 22rpx; background: rgba(255, 255, 255, .07); }
	.sync-row { display: flex; justify-content: space-between; align-items: center; }
	.sync-title { font-size: 27rpx; font-weight: 700; }
	.sync-state { color: #91d7a8; font-size: 22rpx; }
	.sync-state.uploading { color: #ffd18b; }
	.sync-state.error { color: #ff9e91; }
	.progress { height: 10rpx; margin-top: 18rpx; overflow: hidden; border-radius: 999rpx; background: rgba(255, 255, 255, .12); }
	.progress view { height: 100%; border-radius: inherit; background: linear-gradient(90deg, #ff9d55, #ff6300); transition: width .25s; }
	.sync-detail { display: block; margin-top: 13rpx; color: rgba(255, 255, 255, .47); font-size: 21rpx; }
	.start-area { margin-top: 30rpx; }
	.privacy-tip { display: block; color: rgba(255, 255, 255, .48); font-size: 21rpx; line-height: 1.55; text-align: center; }
	.controls { display: flex; gap: 24rpx; margin-top: 32rpx; }
	.main-button { margin: 0; border: 0; border-radius: 999rpx; color: #fff; font-size: 31rpx; font-weight: 750; line-height: 94rpx; }
	.main-button::after { border: 0; }
	.start-button { width: 100%; margin-top: 22rpx; background: linear-gradient(135deg, #ff8e3c, #ff5e00); box-shadow: 0 12rpx 32rpx rgba(255, 94, 0, .26); }
	.pause-button, .resume-button, .finish-button { flex: 1; }
	.pause-button { background: #fff; color: #3a2113; }
	.resume-button { background: linear-gradient(135deg, #ff8e3c, #ff5e00); }
	.finish-button { background: rgba(255, 255, 255, .13); }
	.completed-card { margin-top: 28rpx; text-align: center; color: #ffd3b2; }
</style>
