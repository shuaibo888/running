<template>
	<view class="page">
		<view class="profile-hero">
			<view class="avatar" :class="{ uploading: uploadingAvatar }" @tap="chooseAvatarFile">
				<image v-if="form.avatarUrl" class="avatar-image" :src="form.avatarUrl" mode="aspectFill" />
				<text v-else>{{ avatarLetter }}</text>
				<view class="avatar-edit">{{ uploadingAvatar ? '上传中' : '更换' }}</view>
			</view>
			<view class="hero-copy">
				<text class="hero-name">{{ form.nickname || '燃赛跑者' }}</text>
				<text class="hero-subtitle">{{ profileSummary }}</text>
			</view>
			<view class="completion-badge" :class="{ complete: profileCompleted }">
				{{ profileCompleted ? '资料完整' : '待完善' }}
			</view>
		</view>

		<view v-if="loading" class="state-card">
			<text>正在读取运动档案…</text>
		</view>

		<template v-else>
			<view class="tip-card" :class="{ complete: profileCompleted }">
				<text class="tip-title">{{ profileCompleted ? '运动档案已完善' : '完善资料，让卡路里更准确' }}</text>
				<text class="tip-copy">身高、体重、性别和年龄只用于运动消耗估算；每次运动会记录当时采用的计算参数。</text>
			</view>

			<view class="section-title">基础资料</view>
			<view class="form-card">
				<view class="form-row">
					<text class="form-label">昵称</text>
					<input v-model.trim="form.nickname" class="form-input" maxlength="64" placeholder="请输入昵称" />
				</view>
				<picker mode="selector" :range="genderOptions" range-key="label" :value="genderIndex" @change="handleGenderChange">
					<view class="form-row">
						<text class="form-label">性别</text>
						<text class="form-value" :class="{ placeholder: !form.gender }">{{ genderLabel }}</text>
						<text class="arrow">›</text>
					</view>
				</picker>
				<picker mode="date" :value="form.birthDate" :end="today" @change="handleBirthDateChange">
					<view class="form-row">
						<text class="form-label">出生日期</text>
						<text class="form-value" :class="{ placeholder: !form.birthDate }">{{ form.birthDate || '请选择' }}</text>
						<text class="arrow">›</text>
					</view>
				</picker>
				<view class="form-row">
					<text class="form-label">身高</text>
					<input v-model="form.heightCm" class="number-input" type="digit" maxlength="6" placeholder="50–300" />
					<text class="unit">cm</text>
				</view>
				<view class="form-row">
					<text class="form-label">体重</text>
					<input v-model="form.weightKg" class="number-input" type="digit" maxlength="6" placeholder="20–500" />
					<text class="unit">kg</text>
				</view>
				<picker mode="region" :value="regionValue" @change="handleRegionChange">
					<view class="form-row last-row">
						<text class="form-label">常住地区</text>
						<text class="form-value" :class="{ placeholder: !form.cityName }">{{ regionLabel }}</text>
						<text class="arrow">›</text>
					</view>
				</picker>
			</view>

			<button class="save-button" :loading="saving" :disabled="saving" @tap="saveProfile">保存运动档案</button>

			<view class="section-title account-heading">登录方式</view>
			<view class="account-card">
				<view class="account-row">
					<view class="method-icon wechat">微</view>
					<view class="method-copy">
						<text class="method-name">微信一键登录</text>
					<text class="method-state">{{ loginMethods.wechatBound ? '已关联当前账号' : '下次微信一键登录时自动关联' }}</text>
					</view>
					<text class="method-badge">{{ loginMethods.wechatBound ? '可用' : '待关联' }}</text>
				</view>
				<view class="account-row last-row">
					<view class="method-icon phone">手</view>
					<view class="method-copy">
						<text class="method-name">手机号验证码登录</text>
					<text class="method-state">{{ loginMethods.phoneBound ? '已关联当前账号' : '首次登录时自动关联' }}</text>
					</view>
					<text class="method-badge">{{ loginMethods.phoneBound ? '可用' : '待关联' }}</text>
				</view>
			</view>

			<text class="security-tip">账号统一按微信官方验证或短信验证的手机号识别，同一手机号始终进入同一个燃赛路跑账号。</text>
		</template>
	</view>
</template>

<script>
	import { getLoginMethods, hasLoginToken } from '../../common/auth.js'
	import { getCurrentProfile, saveCurrentProfile, uploadProfileAvatar } from '../../common/profile.js'

	function formatLocalDate(date) {
		const year = date.getFullYear()
		const month = String(date.getMonth() + 1).padStart(2, '0')
		const day = String(date.getDate()).padStart(2, '0')
		return `${year}-${month}-${day}`
	}

	function emptyProfile() {
		return {
			nickname: '',
			avatarUrl: '',
			avatarOssId: '',
			gender: '',
			birthDate: '',
			heightCm: '',
			weightKg: '',
			provinceCode: '',
			provinceName: '',
			cityCode: '',
			cityName: ''
		}
	}

	export default {
		data() {
			return {
				loading: true,
				saving: false,
				uploadingAvatar: false,
				profileCompleted: false,
				form: emptyProfile(),
				genderOptions: [
					{ label: '未知', value: '0' },
					{ label: '男', value: '1' },
					{ label: '女', value: '2' }
				],
				today: formatLocalDate(new Date()),
				loginMethods: { wechatBound: false, phoneBound: false }
			}
		},
		computed: {
			avatarLetter() {
				return (this.form.nickname || '燃').slice(0, 1)
			},
			genderIndex() {
				const index = this.genderOptions.findIndex(item => item.value === this.form.gender)
				return index < 0 ? 0 : index
			},
			genderLabel() {
				const option = this.genderOptions.find(item => item.value === this.form.gender)
				return option ? option.label : '请选择'
			},
			regionValue() {
				return this.form.provinceName && this.form.cityName
					? [this.form.provinceName, this.form.cityName, '']
					: []
			},
			regionLabel() {
				if (!this.form.cityName) return '请选择'
				return [this.form.provinceName, this.form.cityName].filter(Boolean).join(' ')
			},
			profileSummary() {
				const values = []
				if (this.form.gender) values.push(this.genderLabel)
				if (this.form.heightCm) values.push(`${this.form.heightCm}cm`)
				if (this.form.weightKg) values.push(`${this.form.weightKg}kg`)
				return values.length ? values.join(' · ') : '完善身体资料，开启准确记录'
			}
		},
		onLoad() {
			if (!hasLoginToken()) {
				uni.reLaunch({ url: '/pages/login/login' })
				return
			}
			this.loadPage()
		},
		methods: {
			async chooseAvatarFile() {
				if (this.uploadingAvatar) return
				try {
					const chooseResult = await new Promise((resolve, reject) => {
						uni.chooseMedia({
							count: 1,
							mediaType: ['image'],
							sourceType: ['album', 'camera'],
							sizeType: ['compressed'],
							success: resolve,
							fail: reject
						})
					})
					const selected = chooseResult.tempFiles && chooseResult.tempFiles[0]
					if (!selected || !selected.tempFilePath) {
						throw new Error('没有读取到所选图片')
					}
					if (selected.size && selected.size > 5 * 1024 * 1024) {
						throw new Error('头像不能超过 5MB')
					}
					this.uploadingAvatar = true
					uni.showLoading({ title: '上传头像…', mask: true })
					const response = await uploadProfileAvatar(selected.tempFilePath)
					this.applyProfile(response.data || {})
					uni.showToast({ title: '头像已更新', icon: 'success' })
				} catch (error) {
					const message = error && (error.errMsg || error.message) || ''
					if (!message.includes('cancel')) {
						this.handleRequestError(error instanceof Error ? error : new Error(message || '头像上传失败'))
					}
				} finally {
					this.uploadingAvatar = false
					uni.hideLoading()
				}
			},
			async loadPage() {
				this.loading = true
				try {
					const [profileResponse, methodResponse] = await Promise.all([
						getCurrentProfile(),
						getLoginMethods()
					])
					this.applyProfile(profileResponse.data || {})
					this.loginMethods = methodResponse.data || { wechatBound: false, phoneBound: false }
				} catch (error) {
					this.handleRequestError(error)
				} finally {
					this.loading = false
				}
			},
			applyProfile(profile) {
				const next = emptyProfile()
				Object.keys(next).forEach(key => {
					next[key] = profile[key] == null ? '' : String(profile[key])
				})
				this.form = next
				this.profileCompleted = Boolean(profile.profileCompleted)
			},
			handleGenderChange(event) {
				const option = this.genderOptions[Number(event.detail.value)]
				this.form.gender = option ? option.value : '0'
			},
			handleBirthDateChange(event) {
				this.form.birthDate = event.detail.value
			},
			handleRegionChange(event) {
				const region = event.detail.value || []
				const codes = event.detail.code || []
				this.form.provinceName = region[0] || ''
				this.form.cityName = region[1] || ''
				this.form.provinceCode = codes[0] || ''
				this.form.cityCode = codes[1] || ''
			},
			validateProfile() {
				const height = this.form.heightCm === '' ? null : Number(this.form.heightCm)
				const weight = this.form.weightKg === '' ? null : Number(this.form.weightKg)
				if (height !== null && (!Number.isFinite(height) || height < 50 || height > 300)) {
					throw new Error('身高请输入 50–300 厘米')
				}
				if (weight !== null && (!Number.isFinite(weight) || weight < 20 || weight > 500)) {
					throw new Error('体重请输入 20–500 千克')
				}
				return { height, weight }
			},
			async saveProfile() {
				if (this.saving) return
				try {
					const values = this.validateProfile()
					this.saving = true
					const editableProfile = { ...this.form }
					delete editableProfile.avatarUrl
					delete editableProfile.avatarOssId
					const response = await saveCurrentProfile({
						...editableProfile,
						gender: this.form.gender || null,
						birthDate: this.form.birthDate || null,
						heightCm: values.height,
						weightKg: values.weight,
						provinceCode: this.form.provinceCode || null,
						provinceName: this.form.provinceName || null,
						cityCode: this.form.cityCode || null,
						cityName: this.form.cityName || null
					})
					this.applyProfile(response.data || {})
					uni.showToast({ title: '档案保存成功', icon: 'success' })
				} catch (error) {
					this.handleRequestError(error)
				} finally {
					this.saving = false
				}
			},
			handleRequestError(error) {
				if (!hasLoginToken()) {
					uni.reLaunch({ url: '/pages/login/login' })
					return
				}
				uni.showToast({ title: error.message || '请求失败，请稍后重试', icon: 'none', duration: 2600 })
			}
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; padding-bottom: calc(env(safe-area-inset-bottom) + 50rpx); background: #f7f4f1; color: #2d241f; }
	.profile-hero { position: relative; display: flex; align-items: center; padding: 44rpx 36rpx 72rpx; background: linear-gradient(135deg, #ff913e, #ff5c00); overflow: hidden; }
	.profile-hero::after { content: ''; position: absolute; right: -90rpx; top: -120rpx; width: 300rpx; height: 300rpx; border: 36rpx solid rgba(255,255,255,.1); border-radius: 50%; }
	.avatar { position: relative; z-index: 1; display: flex; align-items: center; justify-content: center; width: 112rpx; height: 112rpx; margin-right: 25rpx; border: 6rpx solid rgba(255,255,255,.7); border-radius: 50%; background: #fff; color: #ff6500; font-size: 48rpx; font-weight: 900; }
	.avatar.uploading { opacity: .78; }
	.avatar-image { width: 100%; height: 100%; border-radius: 50%; }
	.avatar-edit { position: absolute; right: -8rpx; bottom: -8rpx; min-width: 54rpx; padding: 5rpx 9rpx; border: 2rpx solid #fff; border-radius: 99rpx; background: rgba(55,37,27,.86); color: #fff; font-size: 17rpx; font-weight: 600; line-height: 1.2; text-align: center; }
	.hero-copy { position: relative; z-index: 1; flex: 1; min-width: 0; }
	.hero-name, .hero-subtitle { display: block; color: #fff; }
	.hero-name { max-width: 330rpx; overflow: hidden; font-size: 37rpx; font-weight: 800; text-overflow: ellipsis; white-space: nowrap; }
	.hero-subtitle { margin-top: 10rpx; font-size: 23rpx; opacity: .88; }
	.completion-badge { position: relative; z-index: 1; padding: 9rpx 16rpx; border: 1rpx solid rgba(255,255,255,.52); border-radius: 99rpx; color: #fff; font-size: 21rpx; }
	.completion-badge.complete { background: rgba(255,255,255,.18); }
	.state-card, .tip-card, .form-card, .account-card { box-sizing: border-box; margin: 22rpx 24rpx 0; border-radius: 24rpx; background: #fff; box-shadow: 0 7rpx 24rpx rgba(67,44,29,.06); }
	.state-card { padding: 80rpx 30rpx; text-align: center; color: #8e8179; font-size: 26rpx; }
	.tip-card { position: relative; margin-top: -34rpx; padding: 27rpx 30rpx; border-left: 7rpx solid #ff6a00; }
	.tip-card.complete { border-left-color: #2bad68; }
	.tip-title, .tip-copy { display: block; }
	.tip-title { font-size: 28rpx; font-weight: 750; }
	.tip-copy { margin-top: 9rpx; color: #968981; font-size: 22rpx; line-height: 1.55; }
	.section-title { margin: 34rpx 32rpx 16rpx; font-size: 29rpx; font-weight: 800; }
	.form-card { margin-top: 0; padding: 0 28rpx; }
	.form-row { display: flex; align-items: center; min-height: 100rpx; border-bottom: 1rpx solid #eee9e5; }
	.form-row.last-row { border-bottom: 0; }
	.form-label { width: 170rpx; font-size: 27rpx; font-weight: 600; }
	.form-input, .number-input { flex: 1; height: 100rpx; text-align: right; font-size: 27rpx; }
	.number-input { min-width: 0; }
	.unit { width: 54rpx; margin-left: 12rpx; color: #8e8179; font-size: 24rpx; text-align: right; }
	.form-value { flex: 1; color: #302722; font-size: 27rpx; text-align: right; }
	.form-value.placeholder { color: #b6aba4; }
	.arrow { width: 28rpx; margin-left: 13rpx; color: #b7aca5; font-size: 38rpx; text-align: right; }
	.save-button { height: 90rpx; margin: 28rpx 24rpx 0; border: 0; border-radius: 999rpx; background: linear-gradient(135deg, #ff913e, #ff5c00); box-shadow: 0 12rpx 25rpx rgba(255,92,0,.2); color: #fff; font-size: 29rpx; font-weight: 750; line-height: 90rpx; }
	.save-button::after { border: 0; }
	.account-heading { margin-top: 46rpx; }
	.account-card { margin-top: 0; padding: 0 28rpx; }
	.account-row { display: flex; align-items: center; min-height: 114rpx; border-bottom: 1rpx solid #eee9e5; }
	.account-row.last-row { border-bottom: 0; }
	.method-icon { display: flex; align-items: center; justify-content: center; width: 66rpx; height: 66rpx; margin-right: 20rpx; border-radius: 20rpx; color: #fff; font-size: 25rpx; font-weight: 800; }
	.method-icon.wechat { background: #18b760; }
	.method-icon.phone { background: #ff7b25; }
	.method-copy { flex: 1; }
	.method-name, .method-state { display: block; }
	.method-name { font-size: 27rpx; font-weight: 700; }
	.method-state { margin-top: 6rpx; color: #9d9189; font-size: 21rpx; }
	.method-badge { flex-shrink: 0; padding: 9rpx 16rpx; border-radius: 99rpx; background: #fff0e5; color: #e65c00; font-size: 21rpx; }
	.security-tip { display: block; margin: 25rpx 38rpx 0; color: #9f938b; font-size: 21rpx; line-height: 1.6; text-align: center; }
</style>
