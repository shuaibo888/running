<template>
	<view class="page">
		<view class="hero">
			<text class="eyebrow">RUNNING SERVICE</text>
			<text class="title">{{ document.title }}</text>
			<text class="version">更新日期：2026年9月4日 · 生效日期：正式上线之日</text>
		</view>
		<view class="notice">
			<text class="notice-title">请在使用前仔细阅读</text>
			<text>不同意本文件不会影响浏览本页；但登录、记录运动等功能需要在您主动同意后使用。</text>
		</view>
		<view v-for="section in document.sections" :key="section.title" class="section">
			<text class="section-title">{{ section.title }}</text>
			<text v-for="(paragraph, index) in section.paragraphs" :key="index" class="paragraph">{{ paragraph }}</text>
		</view>
		<view class="switch-document">
			<text>还可查看</text>
			<text class="switch-link" @tap="switchDocument">{{ alternateTitle }}</text>
		</view>
	</view>
</template>

<script>
	const terms = {
		title: '燃赛路跑用户协议',
		sections: [
			{ title: '一、服务说明', paragraphs: [
				'燃赛路跑为用户提供运动记录、统计趋势、文化线路、城市足迹、成就、积分及排行榜等服务。部分能力依赖微信、定位、网络、地图服务和设备状态，实际可用范围以页面提示为准。',
				'运动距离、配速和卡路里均为算法估算结果，仅供日常运动记录与激励，不构成医疗、诊断、训练或健康建议。'
			] },
			{ title: '二、账号与登录', paragraphs: [
				'完整且经过验证的手机号是平台用户的唯一归并身份。微信一键登录会同时使用微信登录凭证和官方手机号授权凭证；验证码登录与微信登录验证出相同手机号时进入同一账号。',
				'请妥善保管手机、验证码和登录状态，不得冒用他人手机号、转让账号或利用服务实施违法违规行为。发现异常登录时应及时退出并联系运营方处理。'
			] },
			{ title: '三、运动记录规则', paragraphs: [
				'跑步轨迹统一采用 GCJ-02 坐标。客户端展示为过程估算，服务端会校验定位精度、时间顺序、速度、漂移和轨迹完整性，并以服务端结算结果作为最终记录。',
				'请勿伪造轨迹、篡改运动数据或利用自动化工具获取积分、成就及排名。异常数据可以被标记为无效，并不计入线路、积分或排行榜。'
			] },
			{ title: '四、积分、成就与线路', paragraphs: [
				'积分和成就由签到、有效运动、线路节点等真实业务事件触发。积分仅用于平台展示和后续明确开放的权益，不等同于现金，不支持提现或私下交易。',
				'线路、节点、奖励和排行榜规则可能因运营活动依法调整；已经形成的运动事实不会因用户后来修改常住地区或切换线路而被改写。'
			] },
			{ title: '五、服务边界与变更', paragraphs: [
				'小程序当前只承诺在可运行的前台状态下采集轨迹。锁屏、后台运行、弱网、系统回收或未授予权限可能导致轨迹中断，页面会在能力范围内提供恢复和补传。',
				'运营方可以为修复故障、保障安全、遵守监管要求或升级功能而维护、调整或暂停部分服务，并会通过合理方式提示重大变化。'
			] },
			{ title: '六、协议接受', paragraphs: [
				'点击登录页勾选框并继续登录，表示您已阅读并同意本协议与隐私政策。若您不同意，请勿勾选或继续登录。未成年人应在监护人阅读并同意后使用。'
			] }
		]
	}

	const privacy = {
		title: '燃赛路跑隐私政策',
		sections: [
			{ title: '一、信息收集与使用', paragraphs: [
				'登录时，我们处理经短信或微信官方能力验证的完整手机号，用于创建和归并唯一平台账号；微信登录还会处理微信登录凭证、手机号授权凭证及由微信服务端验证的身份标识。上述凭证仅用于完成本次认证。',
				'您主动填写档案时，我们处理昵称、头像、性别、生日、身高、体重及常住省市，用于个人展示、运动估算和地区排行榜归属。昵称和头像可为空；开始需要卡路里结算的运动前，需要补齐页面明确提示的必要资料。',
				'您开始跑步后，我们处理 GCJ-02 经纬度、定位时间、精度、速度、方向、轨迹序号以及运动时长，用于绘制轨迹、结算距离、识别异常数据和生成城市足迹。只有在您主动使用相关功能并授权定位后才会采集。'
			] },
			{ title: '二、权限调用', paragraphs: [
				'位置权限用于开始前定位、前台轨迹记录、地图展示及城市识别；相册或相机能力仅在您主动选择头像时调用。拒绝非必要权限不影响浏览无需该权限的页面，您可以在微信设置中随时关闭权限。',
				'关闭定位后无法继续记录跑步轨迹。当前版本不承诺锁屏或后台持续定位；离开跑步页时会停止位置监听，返回后按页面提示恢复。'
			] },
			{ title: '三、存储与安全', paragraphs: [
				'手机号和微信身份映射采用服务端稳定密钥生成不可逆摘要后保存；不会向小程序返回原始 openid、unionid、session_key 或服务端密钥。MySQL 是业务事实来源，Redis 仅承载登录态、短缓存、幂等和排行榜加速。',
				'运动记录会保存本次结算采用的体重、算法版本和地区快照，以保证历史结果可审计。我们采用访问控制、参数校验、频率限制和幂等约束降低泄露、篡改与重复记账风险。'
			] },
			{ title: '四、第三方处理', paragraphs: [
				'微信提供登录、手机号授权及小程序运行能力；腾讯位置服务用于必要的逆地址解析；对象存储服务用于保存您主动上传的头像。我们仅传递实现对应功能所必需的信息，并不会把服务端 AppSecret、短信凭据或腾讯地图服务端 Key 下发给小程序。'
			] },
			{ title: '五、您的权利', paragraphs: [
				'您可以在运动档案中查看和更正个人资料，可以退出当前登录。账号注销、个人信息副本或删除申请将在正式上线前提供可验证的办理入口与处理规则；该能力未完成前，本项目不得发布。',
				'排行榜使用运动创建时的常住地区快照。修改当前常住地区只影响之后创建的运动，不会迁移既有排行事实。'
			] },
			{ title: '六、保存期限与政策更新', paragraphs: [
				'我们仅在实现服务目的、履行法定义务及处理争议所需期限内保存信息；超过必要期限后依法删除或匿名化。具体期限、运营主体名称、联系渠道和账号注销机制须在正式上线前经运营与法律审核补齐。',
				'如处理目的、信息类型或共享对象发生重大变化，我们会更新本政策并依法重新取得同意。当前文本是研发验收版本，不代表已经完成微信平台隐私审核。'
			] }
		]
	}

	export default {
		data() { return { type: 'terms' } },
		computed: {
			document() { return this.type === 'privacy' ? privacy : terms },
			alternateTitle() { return this.type === 'privacy' ? '《用户协议》' : '《隐私政策》' }
		},
		onLoad(query) { this.setType(query.type) },
		methods: {
			setType(type) {
				this.type = type === 'privacy' ? 'privacy' : 'terms'
				uni.setNavigationBarTitle({ title: this.document.title })
			},
			switchDocument() {
				this.setType(this.type === 'privacy' ? 'terms' : 'privacy')
				uni.pageScrollTo({ scrollTop: 0, duration: 200 })
			}
		}
	}
</script>

<style scoped>
	.page { box-sizing: border-box; min-height: 100vh; padding: 42rpx 30rpx calc(env(safe-area-inset-bottom) + 60rpx); background: #fff7ef; color: #30231b; }
	.hero { padding: 18rpx 10rpx 34rpx; }
	.eyebrow, .title, .version { display: block; }
	.eyebrow { color: #ee6814; font-size: 19rpx; font-weight: 800; letter-spacing: 4rpx; }
	.title { margin-top: 12rpx; font-size: 45rpx; font-weight: 900; }
	.version { margin-top: 14rpx; color: #a08f84; font-size: 21rpx; }
	.notice, .section { margin-bottom: 22rpx; padding: 30rpx; border-radius: 26rpx; background: #fff; box-shadow: 0 8rpx 28rpx rgba(92, 56, 30, .06); }
	.notice { border-left: 7rpx solid #ff6a00; background: #fff1e4; color: #7b5944; font-size: 23rpx; line-height: 1.7; }
	.notice-title { display: block; margin-bottom: 6rpx; color: #bd4f0b; font-size: 27rpx; font-weight: 800; }
	.section-title { display: block; color: #422c1e; font-size: 29rpx; font-weight: 850; }
	.paragraph { display: block; margin-top: 17rpx; color: #75675e; font-size: 24rpx; line-height: 1.82; text-align: justify; }
	.switch-document { padding: 20rpx 0; color: #9a8a80; font-size: 24rpx; text-align: center; }
	.switch-link { margin-left: 8rpx; color: #ef6410; font-weight: 750; }
</style>
