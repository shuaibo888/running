<template>
	<view class="page">
		<view class="profile-hero" @tap="goProfile">
			<view class="avatar">{{ initial }}</view>
			<view class="identity">
				<view class="name-row"><text>{{ profile.nickname || '燃赛跑者' }}</text><small>编辑资料 ›</small></view>
				<text class="profile-line">{{ profileSummary }}</text>
				<text class="region">{{ regionText }}</text>
			</view>
		</view>

		<view v-if="loading" class="state">正在加载个人数据…</view>
		<view v-else-if="errorMessage" class="state error"><text>{{ errorMessage }}</text><button @tap="loadMine">重新加载</button></view>
		<template v-else>
			<view class="card health">
				<view @tap="goProfile"><b class="orange">{{ number(profile.weightKg, 1) }}</b><text>体重 kg · 编辑</text></view>
				<view><b>{{ bmi }}</b><text>BMI 指数</text></view>
				<view><b>{{ number(statistics.totalCaloriesKcal, 0) }}</b><text>累计 kcal</text></view>
			</view>

			<view class="card menu">
				<view @tap="go('/pages/achievements/achievements')"><image src="/static/running/ic_medal.png" /><text>我的成就</text><small>{{ unlockedCount }} 枚　›</small></view>
				<view @tap="go('/pages/points/points')"><image src="/static/running/ic_diamond.png" /><text>我的积分</text><small class="gold">{{ points.balance || 0 }} 分　›</small></view>
				<view @tap="go('/pages/history/history')"><view class="text-icon orange-bg">记</view><text>运动记录</text><small>{{ statistics.workoutCount || 0 }} 次　›</small></view>
				<view @tap="go('/pages/footprints/footprints')"><view class="text-icon green-bg">城</view><text>城市足迹</text><small>{{ statistics.cityCount || 0 }} 城　›</small></view>
				<view @tap="go('/pages/ranking/ranking')"><view class="text-icon dark-bg">榜</view><text>燃力排行榜</text><small>查看　›</small></view>
			</view>

			<view class="section-head"><text>线路进度</text><small @tap="goRoutes">查看线路 ›</small></view>
			<view class="card route-progress">
				<view v-for="route in routes" :key="route.id" class="route-row">
					<view class="route-label"><text>{{ route.routeName }}</text><small v-if="route.selected">当前</small></view>
					<view class="bar"><view :style="{ width: percent(route.progressPercent), background: route.themeColor || '#ff6a00' }" /></view>
					<text class="percent">{{ percent(route.progressPercent) }}</text>
				</view>
				<text v-if="!routes.length" class="empty">暂无线路数据</text>
			</view>

			<view class="card account">
				<view><text>登录方式</text><small>{{ loginMethodText }}</small></view>
				<view @tap="go('/pages/location/location')"><text>定位与地图</text><small>检查权限　›</small></view>
				<view @tap="handleLogout"><text class="logout">退出登录</text><small>{{ logoutLoading ? '处理中…' : '›' }}</small></view>
			</view>
			<text class="footer">共完成 {{ statistics.workoutCount || 0 }} 次运动 · 燃赛与你同行</text>
		</template>
	</view>
</template>

<script>
	import { getLoginMethods, hasLoginToken, logout } from '../../common/auth.js'
	import { getCurrentProfile } from '../../common/profile.js'
	import { getWorkoutStatistics } from '../../common/workout.js'
	import { getAchievements, getPoints, getRoutes } from '../../common/engagement.js'

	export default {
		data() { return { profile: {}, statistics: {}, points: {}, routes: [], achievements: [], loginMethods: {}, loading: false, logoutLoading: false, errorMessage: '' } },
		computed: {
			initial() { return String(this.profile.nickname || '燃').slice(0, 1) },
			unlockedCount() { return this.achievements.filter(item => item.unlocked).length },
			bmi() {
				const weight = Number(this.profile.weightKg || 0), height = Number(this.profile.heightCm || 0) / 100
				return weight > 0 && height > 0 ? (weight / (height * height)).toFixed(1) : '--'
			},
			profileSummary() {
				const gender = { '1': '男', '2': '女' }[this.profile.gender] || '性别未填'
				const age = this.age(this.profile.birthDate)
				const height = this.profile.heightCm ? `${Number(this.profile.heightCm).toFixed(0)}cm` : '身高未填'
				const weight = this.profile.weightKg ? `${Number(this.profile.weightKg).toFixed(1)}kg` : '体重未填'
				return [gender, age ? `${age}岁` : '年龄未填', height, weight].join(' · ')
			},
			regionText() { return [this.profile.provinceName, this.profile.cityName].filter(Boolean).join(' · ') || '常住地区未设置' },
			loginMethodText() {
				const methods = []
				if (this.loginMethods.phoneLinked) methods.push('手机号')
				if (this.loginMethods.wechatLinked) methods.push('微信')
				return methods.length ? methods.join(' + ') : '已登录'
			}
		},
		onShow() {
			if (!hasLoginToken()) { uni.reLaunch({ url: '/pages/login/login' }); return }
			this.loadMine()
		},
		onPullDownRefresh() { this.loadMine(true) },
		methods: {
			async loadMine(fromPullDown = false) {
				if (this.loading) return
				this.loading = true; this.errorMessage = ''
				try {
					const [profile, statistics, points, routes, achievements, loginMethods] = await Promise.all([
						getCurrentProfile(), getWorkoutStatistics(), getPoints(), getRoutes(), getAchievements(), getLoginMethods()
					])
					this.profile = profile.data || {}; this.statistics = statistics.data || {}; this.points = points.data || {}
					this.routes = Array.isArray(routes.data) ? routes.data : []; this.achievements = Array.isArray(achievements.data) ? achievements.data : []
					this.loginMethods = loginMethods.data || {}
				} catch (error) { this.errorMessage = error.message || '个人数据加载失败' }
				finally { this.loading = false; if (fromPullDown) uni.stopPullDownRefresh() }
			},
			number(value, digits = 0) { const n = Number(value || 0); return Number.isFinite(n) ? n.toFixed(digits) : '--' },
			percent(value) { return `${Math.max(0, Math.min(100, Number(value || 0))).toFixed(1)}%` },
			age(value) {
				if (!value) return null
				const parts = String(value).slice(0, 10).split('-').map(Number), now = new Date()
				if (parts.length !== 3 || !parts[0]) return null
				let age = now.getFullYear() - parts[0]
				if (now.getMonth() + 1 < parts[1] || (now.getMonth() + 1 === parts[1] && now.getDate() < parts[2])) age--
				return Math.max(0, age)
			},
			go(url) { uni.navigateTo({ url }) },
			goProfile() { uni.navigateTo({ url: '/pages/profile/profile' }) },
			goRoutes() { uni.switchTab({ url: '/pages/routes/routes' }) },
			async handleLogout() {
				if (this.logoutLoading) return
				this.logoutLoading = true
				try { await logout() } catch (error) { uni.showToast({ title: '服务端暂不可用，已退出本地登录', icon: 'none' }) }
				finally { this.logoutLoading = false; uni.reLaunch({ url: '/pages/login/login' }) }
			}
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; padding-bottom: 55rpx; background: #f5f1ed; color: #2c211a; }
	.profile-hero { display: flex; align-items: center; padding: calc(env(safe-area-inset-top) + 45rpx) 38rpx 74rpx; background: linear-gradient(135deg, #ff9142, #f35a08); color: #fff; }
	.avatar { display: flex; flex: 0 0 auto; align-items: center; justify-content: center; width: 108rpx; height: 108rpx; border: 6rpx solid rgba(255,255,255,.38); border-radius: 50%; background: #fff; color: #f36515; font-size: 45rpx; font-weight: 900; }
	.identity { flex: 1; min-width: 0; margin-left: 25rpx; }
	.name-row { display: flex; align-items: center; }
	.name-row text { overflow: hidden; max-width: 320rpx; font-size: 38rpx; font-weight: 850; text-overflow: ellipsis; white-space: nowrap; }
	.name-row small { margin-left: 14rpx; font-size: 20rpx; opacity: .82; }
	.profile-line, .region { display: block; margin-top: 9rpx; font-size: 21rpx; opacity: .85; }
	.region { margin-top: 5rpx; opacity: .68; }
	.card { margin: 24rpx; border-radius: 25rpx; background: #fff; box-shadow: 0 9rpx 28rpx rgba(70,44,29,.07); }
	.health { position: relative; display: flex; margin-top: -34rpx; padding: 27rpx 5rpx; }
	.health>view { flex: 1; text-align: center; }
	.health b, .health text { display: block; }
	.health b { font-size: 37rpx; }
	.health b.orange { color: #ff6812; }
	.health text { margin-top: 6rpx; color: #a1958d; font-size: 18rpx; }
	.menu, .account { padding: 5rpx 27rpx; }
	.menu>view, .account>view { display: flex; align-items: center; padding: 25rpx 0; border-bottom: 1rpx solid #eee7e2; }
	.menu>view:last-child, .account>view:last-child { border-bottom: 0; }
	.menu image, .text-icon { width: 43rpx; height: 43rpx; margin-right: 19rpx; }
	.text-icon { display: flex; align-items: center; justify-content: center; border-radius: 13rpx; color: #fff; font-size: 18rpx; font-weight: 800; }
	.orange-bg { background: #f47a2d; }.green-bg { background: #3e9671; }.dark-bg { background: #4b3429; }
	.menu text, .account text { flex: 1; font-size: 27rpx; }
	.menu small, .account small { color: #9f938b; font-size: 21rpx; }
	.menu .gold { color: #d79013; }
	.section-head { display: flex; justify-content: space-between; margin: 37rpx 30rpx 15rpx; }
	.section-head text { font-size: 30rpx; font-weight: 850; }
	.section-head small { color: #e56219; font-size: 21rpx; }
	.route-progress { margin-top: 0; padding: 19rpx 27rpx; }
	.route-row { display: flex; align-items: center; padding: 15rpx 0; }
	.route-label { display: flex; align-items: center; width: 194rpx; }
	.route-label>text { overflow: hidden; font-size: 22rpx; text-overflow: ellipsis; white-space: nowrap; }
	.route-label small { margin-left: 7rpx; padding: 3rpx 7rpx; border-radius: 7rpx; background: #fff0e4; color: #ee6418; font-size: 15rpx; }
	.bar { flex: 1; height: 12rpx; overflow: hidden; border-radius: 99rpx; background: #eee9e5; }
	.bar view { height: 100%; border-radius: inherit; }
	.percent { width: 78rpx; color: #74675f; font-size: 19rpx; text-align: right; }
	.empty { display: block; padding: 20rpx; color: #a39891; text-align: center; }
	.account { margin-top: 29rpx; }
	.account .logout { color: #d74c31; }
	.footer { display: block; margin-top: 28rpx; color: #b0a59e; font-size: 20rpx; text-align: center; }
	.state { margin: -34rpx 24rpx 24rpx; padding: 52rpx 25rpx; border-radius: 25rpx; background: #fff; color: #8c7e75; text-align: center; }
	.state text { display: block; }
	.state button { margin-top: 20rpx; border-radius: 99rpx; background: #ff6a00; color: #fff; font-size: 23rpx; }
	.state.error { color: #b34a31; }
</style>
