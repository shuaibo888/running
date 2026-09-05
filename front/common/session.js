import { ACCESS_TOKEN_STORAGE_KEY, ACTIVE_WORKOUT_STORAGE_KEY } from './config.js'

let redirectingToLogin = false

/**
 * 清理只属于当前账号的本地数据。轨迹待上传点不能在账号失效后留给下一位用户。
 */
export function clearLocalSession() {
	uni.removeStorageSync(ACCESS_TOKEN_STORAGE_KEY)
	uni.removeStorageSync(ACTIVE_WORKOUT_STORAGE_KEY)
}

/**
 * 将并发接口产生的一组 401 收敛为一次提示和一次登录页跳转。
 */
export function expireSession(message = '登录已失效，请重新登录') {
	clearLocalSession()
	if (redirectingToLogin) return
	redirectingToLogin = true
	uni.showToast({ title: message, icon: 'none', duration: 2200 })
	setTimeout(() => {
		uni.reLaunch({
			url: '/pages/login/login',
			fail: () => { redirectingToLogin = false }
		})
	}, 80)
}

export function markSessionActive() {
	redirectingToLogin = false
}
