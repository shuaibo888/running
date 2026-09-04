<template>
	<view class="page">
		<view class="balance-hero">
			<text class="balance-label">我的积分</text>
			<text class="balance-value">{{ overview.balance || 0 }}</text>
			<text class="earned">累计获得 {{ overview.totalEarned || 0 }} 积分</text>
			<view class="actions">
				<button class="checkin" :class="{ done: overview.checkedInToday }" :loading="checkingIn" :disabled="checkingIn || overview.checkedInToday" @tap="handleCheckIn">
					{{ overview.checkedInToday ? '今日已签到' : `每日签到 +${overview.todayCheckinReward || 20}` }}
				</button>
				<button class="mall" @tap="showMallTip">积分商城</button>
			</view>
			<text class="streak">已连续签到 {{ overview.currentCheckinStreak || 0 }} 天</text>
		</view>

		<view v-if="loading" class="state-card">正在加载积分账户…</view>
		<view v-else-if="errorMessage" class="state-card error">
			<text>{{ errorMessage }}</text>
			<button @tap="loadPoints">重新加载</button>
		</view>
		<template v-else>
			<view class="card rules">
				<view class="card-title-row">
					<text class="card-title">积分规则</text>
					<text class="rule-badge">坚持运动</text>
				</view>
				<view class="rule-row"><text class="dot">●</text><text>每日签到 +20，连续签到第 7 天起每天 +40</text></view>
				<view class="rule-row"><text class="dot">●</text><text>抵达文化线路节点，按节点配置获得积分</text></view>
				<view class="rule-row"><text class="dot">●</text><text>解锁里程碑成就，按成就等级获得积分</text></view>
				<view class="rule-row muted"><text class="dot">●</text><text>积分商城即将上线，可兑换更多赛事权益</text></view>
			</view>

			<view class="section-head">
				<text class="section-title">积分明细</text>
				<text class="section-note">最近 50 条</text>
			</view>
			<view v-if="transactions.length" class="card ledger">
				<view v-for="item in transactions" :key="item.id" class="ledger-row">
					<view class="ledger-icon" :class="typeClass(item.bizType)">{{ typeIcon(item.bizType) }}</view>
					<view class="ledger-content">
						<text class="ledger-title">{{ item.title }}</text>
						<text class="ledger-description">{{ item.description || typeLabel(item.bizType) }}</text>
						<text class="ledger-time">{{ formatTime(item.occurredAt) }}</text>
					</view>
					<view class="ledger-points">
						<text class="delta">{{ signedPoints(item.deltaPoints) }}</text>
						<text class="after">余额 {{ item.balanceAfter }}</text>
					</view>
				</view>
			</view>
			<view v-else class="state-card empty">还没有积分记录，先完成今日签到吧</view>
		</template>
	</view>
</template>

<script>
	import { checkIn, getPoints } from '../../common/engagement.js'

	export default {
		data() {
			return {
				overview: {
					balance: 0,
					totalEarned: 0,
					currentCheckinStreak: 0,
					checkedInToday: false,
					todayCheckinReward: 20,
					recentTransactions: []
				},
				loading: false,
				checkingIn: false,
				errorMessage: ''
			}
		},
		computed: {
			transactions() {
				return Array.isArray(this.overview.recentTransactions) ? this.overview.recentTransactions : []
			}
		},
		onLoad() {
			this.loadPoints()
		},
		onPullDownRefresh() {
			this.loadPoints(true)
		},
		methods: {
			async loadPoints(fromPullDown = false) {
				if (this.loading) return
				this.loading = true
				this.errorMessage = ''
				try {
					const result = await getPoints()
					this.overview = { ...this.overview, ...(result.data || {}) }
				} catch (error) {
					this.errorMessage = error.message || '积分加载失败'
				} finally {
					this.loading = false
					if (fromPullDown) uni.stopPullDownRefresh()
				}
			},
			async handleCheckIn() {
				if (this.checkingIn || this.overview.checkedInToday) return
				this.checkingIn = true
				try {
					const result = await checkIn()
					this.overview = { ...this.overview, ...(result.data || {}) }
					const points = this.overview.todayCheckinReward || 20
					uni.showToast({ title: `签到成功 +${points}积分`, icon: 'none' })
				} catch (error) {
					uni.showToast({ title: error.message || '签到失败', icon: 'none' })
				} finally {
					this.checkingIn = false
				}
			},
			showMallTip() {
				uni.showToast({ title: '积分商城即将上线', icon: 'none' })
			},
			typeIcon(type) {
				return { CHECKIN: '签', ROUTE_NODE: '线', ACHIEVEMENT: '章' }[type] || '燃'
			},
			typeClass(type) {
				return String(type || '').toLowerCase().replace('_', '-')
			},
			typeLabel(type) {
				return { CHECKIN: '签到奖励', ROUTE_NODE: '线路节点奖励', ACHIEVEMENT: '成就奖励' }[type] || '积分奖励'
			},
			signedPoints(value) {
				const number = Number(value || 0)
				return number > 0 ? `+${number}` : String(number)
			},
			formatTime(value) {
				if (!value) return ''
				return String(value).replace('T', ' ').slice(0, 16)
			}
		}
	}
</script>

<style scoped>
	.page { min-height: 100vh; padding-bottom: 80rpx; background: #f5f1ed; color: #2f241e; }
	.balance-hero { padding: 62rpx 36rpx 44rpx; background: linear-gradient(138deg, #ff9349 0%, #ff6814 62%, #ed4b0a 100%); color: #fff; text-align: center; box-shadow: 0 18rpx 48rpx rgba(219,78,10,.22); }
	.balance-label, .balance-value, .earned, .streak { display: block; }
	.balance-label { font-size: 25rpx; opacity: .88; letter-spacing: 2rpx; }
	.balance-value { margin-top: 6rpx; font-size: 94rpx; font-weight: 900; line-height: 1.16; text-shadow: 0 5rpx 14rpx rgba(126,31,0,.16); }
	.earned { font-size: 21rpx; opacity: .78; }
	.actions { display: flex; justify-content: center; gap: 22rpx; margin-top: 32rpx; }
	.actions button { width: auto; min-width: 210rpx; margin: 0; padding: 0 28rpx; border-radius: 999rpx; font-size: 25rpx; font-weight: 700; line-height: 72rpx; }
	.actions button::after { border: 0; }
	.checkin { background: #fff; color: #f05c0b; box-shadow: 0 8rpx 20rpx rgba(126,31,0,.15); }
	.checkin.done { color: rgba(255,255,255,.82); background: rgba(89,31,10,.25); box-shadow: none; }
	.mall { color: #fff; background: rgba(89,31,10,.25); }
	.streak { margin-top: 20rpx; font-size: 21rpx; opacity: .8; }
	.card { margin: 28rpx 28rpx 0; border-radius: 26rpx; background: #fff; box-shadow: 0 10rpx 32rpx rgba(78,48,31,.07); }
	.rules { padding: 32rpx 30rpx 26rpx; }
	.card-title-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 19rpx; }
	.card-title { font-size: 31rpx; font-weight: 850; }
	.rule-badge { padding: 8rpx 16rpx; border-radius: 99rpx; background: #fff0e5; color: #e95c12; font-size: 19rpx; }
	.rule-row { display: flex; gap: 15rpx; color: #5d514a; font-size: 24rpx; line-height: 1.85; }
	.rule-row .dot { color: #ff7424; font-size: 13rpx; line-height: 44rpx; }
	.rule-row.muted { color: #a19790; }
	.section-head { display: flex; align-items: flex-end; justify-content: space-between; margin: 38rpx 34rpx 16rpx; }
	.section-title { font-size: 32rpx; font-weight: 850; }
	.section-note { color: #a1948b; font-size: 21rpx; }
	.ledger { margin-top: 0; padding: 0 28rpx; }
	.ledger-row { display: flex; align-items: center; padding: 27rpx 0; border-bottom: 1rpx solid #eee7e1; }
	.ledger-row:last-child { border-bottom: 0; }
	.ledger-icon { display: flex; flex: 0 0 auto; align-items: center; justify-content: center; width: 70rpx; height: 70rpx; border-radius: 22rpx; background: #fff0e4; color: #ee641a; font-size: 27rpx; font-weight: 850; }
	.ledger-icon.route-node { background: #e9f5ef; color: #27845d; }
	.ledger-icon.achievement { background: #fff5d8; color: #b27807; }
	.ledger-content { flex: 1; min-width: 0; margin-left: 20rpx; }
	.ledger-title, .ledger-description, .ledger-time, .delta, .after { display: block; }
	.ledger-title { font-size: 27rpx; font-weight: 750; }
	.ledger-description { overflow: hidden; margin-top: 5rpx; color: #756a63; font-size: 22rpx; text-overflow: ellipsis; white-space: nowrap; }
	.ledger-time { margin-top: 6rpx; color: #aca29b; font-size: 19rpx; }
	.ledger-points { flex: 0 0 auto; margin-left: 14rpx; text-align: right; }
	.delta { color: #f06416; font-size: 31rpx; font-weight: 850; }
	.after { margin-top: 5rpx; color: #ada39d; font-size: 18rpx; }
	.state-card { margin: 32rpx 28rpx; padding: 54rpx 28rpx; border-radius: 26rpx; background: #fff; color: #8b7e76; text-align: center; box-shadow: 0 10rpx 32rpx rgba(78,48,31,.07); }
	.state-card text { display: block; }
	.state-card button { width: 220rpx; margin-top: 24rpx; border-radius: 99rpx; background: #ff6a16; color: #fff; font-size: 24rpx; }
	.state-card button::after { border: 0; }
	.state-card.error { color: #b64c31; }
	.state-card.empty { margin-top: 0; color: #a0958e; }
</style>
