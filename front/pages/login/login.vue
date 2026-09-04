<template>
	<view class="login-page">
		<view class="hero">
			<view class="sun sun-one"></view>
			<view class="sun sun-two"></view>
			<view class="brand-mark">
				<text class="brand-fire">燃</text>
			</view>
			<text class="brand-name">燃赛路跑</text>
			<text class="brand-slogan">每一步，都算数</text>
			<view class="track">
				<view class="track-line track-line-one"></view>
				<view class="track-line track-line-two"></view>
				<view class="runner">
					<view class="runner-head"></view>
					<view class="runner-body"></view>
					<view class="runner-arm"></view>
					<view class="runner-leg runner-leg-one"></view>
					<view class="runner-leg runner-leg-two"></view>
				</view>
			</view>
		</view>

		<view class="login-card">
			<view class="mode-tabs">
				<view class="mode-tab" :class="{ active: loginMode === 'wechat' }" @tap="switchMode('wechat')">
					<text>微信一键登录</text>
				</view>
				<view class="mode-tab" :class="{ active: loginMode === 'phone' }" @tap="switchMode('phone')">
					<text>手机号登录</text>
				</view>
			</view>

			<view v-if="loginMode === 'wechat'" class="mode-content wechat-content">
				<view class="wechat-icon">
					<view class="bubble bubble-back"></view>
					<view class="bubble bubble-front"><text>••</text></view>
				</view>
				<text class="mode-title">使用微信快速进入</text>
				<text class="mode-description">授权微信验证手机号后直接登录；与验证码登录手机号相同时进入同一个账号。</text>
				<button class="primary-button wechat-button" open-type="getPhoneNumber" :disabled="wechatLoading || !agreed" :loading="wechatLoading" @getphonenumber="handleWechatLogin">
					微信一键登录
				</button>
			</view>

			<view v-else class="mode-content phone-content">
				<text class="mode-title">验证码登录</text>
				<text class="mode-description">未注册的手机号验证后将自动创建账号。</text>
				<view class="input-shell">
					<text class="country-code">+86</text>
					<input v-model="phone" class="phone-input" type="number" maxlength="11" placeholder="请输入手机号" placeholder-class="input-placeholder" />
				</view>
				<view class="input-shell code-shell">
					<input v-model="smsCode" class="code-input" type="number" :maxlength="8" placeholder="请输入验证码" placeholder-class="input-placeholder" />
					<button class="code-button" :disabled="sendingCode || countdown > 0 || !phoneValid" @tap="handleSendCode">
						{{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
					</button>
				</view>
				<button class="primary-button" :disabled="phoneLoading || !canPhoneLogin" :loading="phoneLoading" @tap="handlePhoneLogin">
					手机号登录
				</button>
			</view>

			<view class="agreement" @tap="agreed = !agreed">
				<view class="agreement-check" :class="{ checked: agreed }">
					<text v-if="agreed">✓</text>
				</view>
				<text class="agreement-text">我已阅读并同意《用户协议》和《隐私政策》</text>
			</view>
		</view>

		<text class="footer-tip">运动数据只用于记录、统计和成就计算</text>
	</view>
</template>

<script>
	import { hasLoginToken, phoneLogin, sendSmsCode, wechatLogin } from '../../common/auth.js'

	export default {
		data() {
			return {
				loginMode: 'wechat',
				phone: '',
				smsCode: '',
				agreed: false,
				wechatLoading: false,
				phoneLoading: false,
				sendingCode: false,
				countdown: 0,
				countdownTimer: null
			}
		},
		computed: {
			phoneValid() {
				return /^1[3-9]\d{9}$/.test(this.phone)
			},
			canPhoneLogin() {
				return this.agreed && this.phoneValid && /^\d{4,8}$/.test(this.smsCode)
			}
		},
		onLoad() {
			if (hasLoginToken()) {
				uni.switchTab({ url: '/pages/index/index' })
			}
		},
		onUnload() {
			this.clearCountdown()
		},
		methods: {
			switchMode(mode) {
				this.loginMode = mode
			},
			ensureAgreement() {
				if (this.agreed) return true
				uni.showToast({ title: '请先阅读并同意用户协议和隐私政策', icon: 'none' })
				return false
			},
			async handleWechatLogin(event) {
				if (!this.ensureAgreement() || this.wechatLoading) return
				const phoneCode = event && event.detail ? event.detail.code : ''
				if (!phoneCode) {
					uni.showToast({ title: '需要允许获取微信手机号，或改用验证码登录', icon: 'none', duration: 2600 })
					return
				}
				this.wechatLoading = true
				try {
					await wechatLogin(phoneCode)
					this.finishLogin()
				} catch (error) {
					this.showError(error)
				} finally {
					this.wechatLoading = false
				}
			},
			async handleSendCode() {
				if (!this.phoneValid || this.sendingCode || this.countdown > 0) return
				this.sendingCode = true
				try {
					await sendSmsCode(this.phone)
					uni.showToast({ title: '验证码已发送', icon: 'success' })
					this.startCountdown()
				} catch (error) {
					this.showError(error)
				} finally {
					this.sendingCode = false
				}
			},
			async handlePhoneLogin() {
				if (!this.ensureAgreement() || !this.canPhoneLogin || this.phoneLoading) return
				this.phoneLoading = true
				try {
					await phoneLogin(this.phone, this.smsCode)
					this.finishLogin()
				} catch (error) {
					this.showError(error)
				} finally {
					this.phoneLoading = false
				}
			},
			startCountdown() {
				this.clearCountdown()
				this.countdown = 60
				this.countdownTimer = setInterval(() => {
					this.countdown -= 1
					if (this.countdown <= 0) this.clearCountdown()
				}, 1000)
			},
			clearCountdown() {
				if (this.countdownTimer) clearInterval(this.countdownTimer)
				this.countdownTimer = null
				if (this.countdown < 0) this.countdown = 0
			},
			finishLogin() {
				uni.showToast({ title: '登录成功', icon: 'success' })
				setTimeout(() => uni.switchTab({ url: '/pages/index/index' }), 350)
			},
			showError(error) {
				uni.showToast({ title: error.message || '操作失败，请稍后重试', icon: 'none', duration: 2500 })
			}
		}
	}
</script>

<style scoped>
	.login-page {
		box-sizing: border-box;
		min-height: 100vh;
		padding: calc(env(safe-area-inset-top) + 36rpx) 32rpx calc(env(safe-area-inset-bottom) + 28rpx);
		background: linear-gradient(180deg, #fff2e2 0%, #fffaf5 48%, #f7f7f7 100%);
		color: #2b211b;
	}

	.hero {
		position: relative;
		height: 390rpx;
		overflow: hidden;
		text-align: center;
	}

	.sun {
		position: absolute;
		border-radius: 50%;
		background: rgba(255, 106, 0, 0.08);
	}

	.sun-one { width: 360rpx; height: 360rpx; top: -170rpx; right: -110rpx; }
	.sun-two { width: 180rpx; height: 180rpx; top: 110rpx; left: -90rpx; }

	.brand-mark {
		position: relative;
		z-index: 2;
		width: 104rpx;
		height: 104rpx;
		margin: 10rpx auto 14rpx;
		border-radius: 30rpx 30rpx 44rpx 30rpx;
		background: linear-gradient(145deg, #ff9c45, #ff5c00);
		box-shadow: 0 18rpx 34rpx rgba(255, 94, 0, 0.25);
		transform: rotate(-8deg);
	}

	.brand-fire {
		display: block;
		line-height: 104rpx;
		font-size: 58rpx;
		font-weight: 900;
		color: #fff;
		transform: rotate(8deg);
	}

	.brand-name, .brand-slogan { position: relative; z-index: 2; display: block; }
	.brand-name { font-size: 48rpx; font-weight: 900; letter-spacing: 5rpx; }
	.brand-slogan { margin-top: 10rpx; font-size: 25rpx; color: #9b6a4e; letter-spacing: 8rpx; }

	.track { position: absolute; left: 0; right: 0; bottom: 0; height: 125rpx; }
	.track-line { position: absolute; left: -10%; width: 120%; height: 68rpx; border: 4rpx solid rgba(255, 106, 0, 0.16); border-color: rgba(255, 106, 0, 0.16) transparent transparent; border-radius: 50%; }
	.track-line-one { top: 54rpx; transform: rotate(-4deg); }
	.track-line-two { top: 84rpx; transform: rotate(3deg); }
	.runner { position: absolute; right: 104rpx; bottom: 42rpx; width: 74rpx; height: 100rpx; transform: rotate(-8deg); }
	.runner-head { position: absolute; top: 0; left: 31rpx; width: 25rpx; height: 25rpx; border-radius: 50%; background: #ff6a00; }
	.runner-body, .runner-arm, .runner-leg { position: absolute; height: 11rpx; border-radius: 99rpx; background: #ff6a00; transform-origin: left center; }
	.runner-body { top: 31rpx; left: 35rpx; width: 51rpx; transform: rotate(105deg); }
	.runner-arm { top: 44rpx; left: 43rpx; width: 53rpx; transform: rotate(-22deg); }
	.runner-leg { top: 72rpx; left: 26rpx; width: 63rpx; }
	.runner-leg-one { transform: rotate(28deg); }
	.runner-leg-two { transform: rotate(142deg); }

	.login-card {
		position: relative;
		z-index: 3;
		padding: 12rpx 34rpx 32rpx;
		border-radius: 34rpx;
		background: #fff;
		box-shadow: 0 18rpx 56rpx rgba(97, 56, 25, 0.11);
	}

	.mode-tabs { display: flex; border-bottom: 1rpx solid #f1e9e2; }
	.mode-tab { position: relative; flex: 1; padding: 30rpx 0 25rpx; text-align: center; font-size: 27rpx; color: #99877c; }
	.mode-tab.active { color: #f56000; font-weight: 700; }
	.mode-tab.active::after { content: ''; position: absolute; left: 50%; bottom: -2rpx; width: 58rpx; height: 6rpx; border-radius: 99rpx; background: #ff6a00; transform: translateX(-50%); }

	.mode-content { min-height: 420rpx; padding-top: 34rpx; }
	.wechat-content { display: flex; flex-direction: column; align-items: center; text-align: center; }
	.mode-title { display: block; font-size: 34rpx; font-weight: 800; }
	.mode-description { display: block; margin: 12rpx auto 0; max-width: 570rpx; font-size: 24rpx; line-height: 1.6; color: #9b8c83; }

	.wechat-icon { position: relative; width: 112rpx; height: 88rpx; margin: 5rpx auto 25rpx; }
	.bubble { position: absolute; border-radius: 50%; }
	.bubble-back { width: 70rpx; height: 58rpx; right: 0; bottom: 0; background: #8ddf8a; }
	.bubble-front { display: flex; align-items: center; justify-content: center; width: 78rpx; height: 66rpx; left: 0; top: 0; background: #17b75e; color: #fff; font-size: 28rpx; letter-spacing: 8rpx; }

	.primary-button {
		width: 100%;
		height: 92rpx;
		margin-top: 42rpx;
		border: 0;
		border-radius: 999rpx;
		background: linear-gradient(135deg, #ff913e, #ff5c00);
		box-shadow: 0 14rpx 30rpx rgba(255, 92, 0, 0.22);
		color: #fff;
		font-size: 30rpx;
		font-weight: 700;
		line-height: 92rpx;
	}
	.primary-button::after, .code-button::after { border: 0; }
	.primary-button[disabled] { background: #dfd7d1; box-shadow: none; color: #fff; }
	.wechat-button { margin-top: 46rpx; }

	.phone-content { padding-top: 40rpx; }
	.phone-content .mode-title, .phone-content .mode-description { text-align: left; }
	.input-shell { display: flex; align-items: center; height: 94rpx; margin-top: 28rpx; padding: 0 26rpx; border: 2rpx solid #eee5de; border-radius: 20rpx; background: #fffcf9; }
	.country-code { padding-right: 23rpx; border-right: 1rpx solid #e5d9d0; font-size: 28rpx; font-weight: 700; }
	.phone-input { flex: 1; height: 94rpx; padding-left: 22rpx; font-size: 29rpx; }
	.code-shell { margin-top: 20rpx; }
	.code-input { flex: 1; height: 94rpx; font-size: 29rpx; }
	.input-placeholder { color: #c2b7af; }
	.code-button { flex-shrink: 0; min-width: 190rpx; height: 66rpx; margin: 0; padding: 0 18rpx; border: 0; border-radius: 99rpx; background: #fff0e4; color: #f56000; font-size: 24rpx; line-height: 66rpx; }
	.code-button[disabled] { background: #f3efec; color: #bcb2ab; }
	.phone-content .primary-button { margin-top: 28rpx; }

	.agreement { display: flex; align-items: center; justify-content: center; padding-top: 28rpx; }
	.agreement-check { display: flex; align-items: center; justify-content: center; width: 28rpx; height: 28rpx; margin-right: 10rpx; border: 2rpx solid #d6c8be; border-radius: 50%; color: #fff; font-size: 19rpx; }
	.agreement-check.checked { border-color: #ff6a00; background: #ff6a00; }
	.agreement-text { font-size: 21rpx; color: #94877f; }
	.footer-tip { display: block; margin-top: 30rpx; text-align: center; font-size: 21rpx; color: #b1a49b; }
</style>
