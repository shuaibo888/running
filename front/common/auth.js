import { ACCESS_TOKEN_STORAGE_KEY } from './config.js'
import { request } from './request.js'

function getWechatLoginCode() {
	return new Promise((resolve, reject) => {
		uni.login({
			provider: 'weixin',
			success(result) {
				if (result.code) {
					resolve(result.code)
					return
				}
				reject(new Error('微信未返回登录凭证'))
			},
			fail(error) {
				reject(new Error(error.errMsg || '微信登录失败'))
			}
		})
	})
}

/**
 * 使用微信身份 code 与官方手机号授权 code 一次换取燃赛路跑登录态。
 * openid、手机号、session_key 和 AppSecret 均不会进入小程序存储。
 */
export async function wechatLogin(phoneCode) {
	if (!phoneCode) throw new Error('未获得微信手机号授权')
	const code = await getWechatLoginCode()
	const response = await request({
		url: '/app/auth/wechat-login',
		method: 'POST',
		data: { code, phoneCode }
	})
	const loginResult = response.data
	if (!loginResult || !loginResult.accessToken) {
		throw new Error('后端未返回有效登录令牌')
	}
	uni.setStorageSync(ACCESS_TOKEN_STORAGE_KEY, loginResult.accessToken)
	return loginResult
}

export function sendSmsCode(phone) {
	return request({
		url: '/app/auth/sms-code',
		method: 'POST',
		data: { phone }
	})
}

export async function phoneLogin(phone, code) {
	const response = await request({
		url: '/app/auth/phone-login',
		method: 'POST',
		data: { phone, code }
	})
	const loginResult = response.data
	if (!loginResult || !loginResult.accessToken) {
		throw new Error('后端未返回有效登录令牌')
	}
	uni.setStorageSync(ACCESS_TOKEN_STORAGE_KEY, loginResult.accessToken)
	return loginResult
}

export function getLoginMethods() {
	return request({
		url: '/app/user/login-methods'
	})
}

export async function logout() {
	try {
		await request({
			url: '/app/auth/logout',
			method: 'POST'
		})
	} finally {
		uni.removeStorageSync(ACCESS_TOKEN_STORAGE_KEY)
	}
}

export function hasLoginToken() {
	return Boolean(uni.getStorageSync(ACCESS_TOKEN_STORAGE_KEY))
}
