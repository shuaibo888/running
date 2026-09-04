<template>
	<view class="page">
		<view class="intro-card">
			<view class="intro-icon">⌖</view>
			<view class="intro-copy">
				<text class="intro-title">运动定位验证</text>
				<text class="intro-subtitle">仅在你点击后获取一次位置；正式运动时才会持续记录轨迹。</text>
			</view>
		</view>

		<view v-if="hasLocation" class="map-shell">
			<map
				id="running-location-map"
				class="map"
				:latitude="latitude"
				:longitude="longitude"
				:scale="16"
				show-location
			></map>
			<view class="accuracy-badge">精度约 {{ accuracyText }}</view>
		</view>

		<view v-else class="empty-map">
			<view class="radar radar-one"></view>
			<view class="radar radar-two"></view>
			<view class="radar-dot"></view>
			<text class="empty-title">等待获取当前位置</text>
			<text class="empty-copy">请在开阔区域打开手机定位服务，以获得更准确的结果。</text>
		</view>

		<view class="result-card">
			<view class="result-heading">
				<text>定位结果</text>
				<text class="coordinate-type">GCJ-02</text>
			</view>

			<view v-if="locating" class="status-line">
				<view class="loading-dot"></view>
				<text>正在请求高精度定位…</text>
			</view>
			<view v-else-if="locationError" class="error-panel">
				<text class="error-title">{{ locationError }}</text>
				<text class="error-copy">{{ errorSuggestion }}</text>
				<button v-if="permissionDenied" class="setting-button" @tap="openSettings">打开权限设置</button>
			</view>
			<template v-else-if="hasLocation">
				<view class="address-block">
					<text class="address-title">{{ displayAddress }}</text>
					<text class="address-region">{{ displayRegion }}</text>
				</view>
				<view class="data-row">
					<view>
						<text class="data-value">{{ latitude.toFixed(6) }}</text>
						<text class="data-label">纬度</text>
					</view>
					<view>
						<text class="data-value">{{ longitude.toFixed(6) }}</text>
						<text class="data-label">经度</text>
					</view>
				</view>
				<text v-if="addressError" class="address-warning">{{ addressError }}</text>
			</template>
			<view v-else class="waiting-copy">定位成功后将在这里显示坐标和腾讯位置服务解析结果。</view>
		</view>

		<button class="locate-button" :loading="locating" :disabled="locating" @tap="locate">
			{{ hasLocation ? '重新定位' : '授权并获取当前位置' }}
		</button>
		<button v-if="hasLocation" class="open-map-button" @tap="openInMap">在地图中查看</button>

		<view class="privacy-note">
			<text class="privacy-title">位置数据说明</text>
			<text>当前页面不会保存轨迹。经纬度只发送给燃赛后端，由后端调用腾讯位置服务识别省、市、区；服务端 Key 不会下发到小程序。</text>
		</view>
	</view>
</template>

<script>
	import { hasLoginToken } from '../../common/auth.js'
	import { reverseGeocode } from '../../common/map.js'

	function getCurrentLocation() {
		return new Promise((resolve, reject) => {
			uni.getLocation({
				type: 'gcj02',
				isHighAccuracy: true,
				highAccuracyExpireTime: 5000,
				success: resolve,
				fail: reject
			})
		})
	}

	export default {
		data() {
			return {
				locating: false,
				latitude: 0,
				longitude: 0,
				accuracy: 0,
				address: null,
				locationError: '',
				addressError: '',
				permissionDenied: false
			}
		},
		computed: {
			hasLocation() {
				return Number.isFinite(this.latitude) && Number.isFinite(this.longitude)
					&& (this.latitude !== 0 || this.longitude !== 0)
			},
			accuracyText() {
				return this.accuracy > 0 ? `${Math.round(this.accuracy)} 米` : '未知'
			},
			displayAddress() {
				if (!this.address) return '坐标获取成功'
				return this.address.roughAddress || this.address.formattedAddress || '当前位置'
			},
			displayRegion() {
				if (!this.address) return '正在等待地址解析'
				const region = [this.address.province, this.address.city, this.address.district]
					.filter(Boolean).join(' · ')
				return this.address.adcode ? `${region}（${this.address.adcode}）` : region
			},
			errorSuggestion() {
				return this.permissionDenied
					? '请在小程序设置中允许使用位置信息，然后重新定位。'
					: '请确认手机定位服务已开启，并在室外或靠近窗户的位置重试。'
			}
		},
		onLoad() {
			if (!hasLoginToken()) {
				uni.reLaunch({ url: '/pages/login/login' })
			}
		},
		methods: {
			async locate() {
				if (this.locating) return
				this.locating = true
				this.locationError = ''
				this.addressError = ''
				this.permissionDenied = false
				try {
					const location = await getCurrentLocation()
					this.latitude = Number(location.latitude)
					this.longitude = Number(location.longitude)
					this.accuracy = Number(location.accuracy || location.horizontalAccuracy || 0)
					this.address = null
					await this.resolveAddress()
				} catch (error) {
					const message = error.errMsg || error.message || '定位失败'
					this.permissionDenied = /deny|denied|authorize|permission/i.test(message)
					this.locationError = this.permissionDenied ? '未获得定位权限' : '暂时无法获取位置'
				} finally {
					this.locating = false
				}
			},
			async resolveAddress() {
				try {
					const response = await reverseGeocode(this.latitude, this.longitude)
					this.address = response.data || null
				} catch (error) {
					if (!hasLoginToken()) {
						uni.reLaunch({ url: '/pages/login/login' })
						return
					}
					this.addressError = error.message || '地址解析失败，坐标仍可用于地图展示'
				}
			},
			openSettings() {
				uni.openSetting({
					success: result => {
						if (result.authSetting && result.authSetting['scope.userLocation']) this.locate()
					}
				})
			},
			openInMap() {
				if (!this.hasLocation) return
				uni.openLocation({
					latitude: this.latitude,
					longitude: this.longitude,
					name: this.displayAddress,
					address: this.displayRegion,
					scale: 17
				})
			}
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; padding: 22rpx 24rpx calc(env(safe-area-inset-bottom) + 42rpx); background: #f5f3f1; color: #2f2722; }
	.intro-card { display: flex; align-items: center; padding: 28rpx; border-radius: 24rpx; background: linear-gradient(135deg, #ff8f3d, #ff5c00); box-shadow: 0 12rpx 30rpx rgba(255,92,0,.2); }
	.intro-icon { display: flex; align-items: center; justify-content: center; width: 76rpx; height: 76rpx; margin-right: 20rpx; border-radius: 23rpx; background: rgba(255,255,255,.2); color: #fff; font-size: 48rpx; font-weight: 800; }
	.intro-copy { flex: 1; }
	.intro-title, .intro-subtitle { display: block; color: #fff; }
	.intro-title { font-size: 31rpx; font-weight: 800; }
	.intro-subtitle { margin-top: 7rpx; font-size: 22rpx; line-height: 1.5; opacity: .88; }
	.map-shell, .empty-map { position: relative; height: 560rpx; margin-top: 22rpx; overflow: hidden; border-radius: 26rpx; background: #f7e9dd; box-shadow: 0 7rpx 25rpx rgba(68,44,28,.08); }
	.map { width: 100%; height: 100%; }
	.accuracy-badge { position: absolute; top: 18rpx; right: 18rpx; padding: 10rpx 16rpx; border-radius: 99rpx; background: rgba(255,255,255,.92); color: #65564c; font-size: 21rpx; box-shadow: 0 4rpx 14rpx rgba(0,0,0,.09); }
	.empty-map { display: flex; flex-direction: column; align-items: center; justify-content: center; }
	.radar { position: absolute; border: 2rpx solid rgba(255,106,0,.18); border-radius: 50%; }
	.radar-one { width: 300rpx; height: 300rpx; }
	.radar-two { width: 190rpx; height: 190rpx; }
	.radar-dot { position: relative; width: 42rpx; height: 42rpx; border: 14rpx solid rgba(255,106,0,.2); border-radius: 50%; background: #ff6a00; box-shadow: 0 0 0 16rpx rgba(255,106,0,.08); }
	.empty-title, .empty-copy { position: relative; display: block; text-align: center; }
	.empty-title { margin-top: 56rpx; font-size: 30rpx; font-weight: 800; }
	.empty-copy { width: 470rpx; margin-top: 12rpx; color: #8e7f75; font-size: 23rpx; line-height: 1.6; }
	.result-card { margin-top: 22rpx; padding: 28rpx; border-radius: 24rpx; background: #fff; box-shadow: 0 7rpx 25rpx rgba(68,44,28,.06); }
	.result-heading { display: flex; align-items: center; justify-content: space-between; font-size: 29rpx; font-weight: 800; }
	.coordinate-type { padding: 6rpx 13rpx; border-radius: 99rpx; background: #fff0e5; color: #e65d00; font-size: 19rpx; font-weight: 700; }
	.status-line { display: flex; align-items: center; padding: 60rpx 0 38rpx; color: #83756c; font-size: 25rpx; }
	.loading-dot { width: 20rpx; height: 20rpx; margin-right: 15rpx; border: 5rpx solid #ffd0ae; border-top-color: #ff6a00; border-radius: 50%; }
	.error-panel { padding: 32rpx 0 8rpx; }
	.error-title, .error-copy { display: block; }
	.error-title { color: #c84b3c; font-size: 28rpx; font-weight: 700; }
	.error-copy { margin-top: 10rpx; color: #8f8178; font-size: 23rpx; line-height: 1.6; }
	.setting-button { height: 66rpx; margin: 22rpx 0 0; border: 0; border-radius: 99rpx; background: #fff0e5; color: #e65d00; font-size: 24rpx; line-height: 66rpx; }
	.setting-button::after, .locate-button::after, .open-map-button::after { border: 0; }
	.address-block { padding: 28rpx 0 22rpx; border-bottom: 1rpx solid #eee9e5; }
	.address-title, .address-region { display: block; }
	.address-title { font-size: 28rpx; font-weight: 750; line-height: 1.45; }
	.address-region { margin-top: 9rpx; color: #968980; font-size: 22rpx; }
	.data-row { display: flex; padding-top: 23rpx; }
	.data-row > view { flex: 1; }
	.data-row > view + view { padding-left: 26rpx; border-left: 1rpx solid #eee9e5; }
	.data-value, .data-label { display: block; }
	.data-value { color: #ff6300; font-size: 27rpx; font-weight: 750; }
	.data-label { margin-top: 5rpx; color: #a0958e; font-size: 20rpx; }
	.address-warning { display: block; margin-top: 20rpx; padding: 16rpx; border-radius: 14rpx; background: #fff7e9; color: #9c6a28; font-size: 21rpx; line-height: 1.5; }
	.waiting-copy { padding: 42rpx 0 15rpx; color: #9c9088; font-size: 23rpx; line-height: 1.6; }
	.locate-button, .open-map-button { height: 90rpx; border: 0; border-radius: 99rpx; font-size: 28rpx; font-weight: 750; line-height: 90rpx; }
	.locate-button { margin-top: 26rpx; background: linear-gradient(135deg, #ff913e, #ff5c00); box-shadow: 0 12rpx 25rpx rgba(255,92,0,.19); color: #fff; }
	.open-map-button { margin-top: 15rpx; background: #fff; color: #df5b00; }
	.privacy-note { margin-top: 28rpx; padding: 24rpx 26rpx; border-radius: 20rpx; background: #ece9e6; color: #82766e; font-size: 21rpx; line-height: 1.65; }
	.privacy-title { display: block; margin-bottom: 5rpx; color: #62564e; font-size: 23rpx; font-weight: 750; }
</style>
