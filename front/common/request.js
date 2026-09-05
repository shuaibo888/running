import {
	ACCESS_TOKEN_STORAGE_KEY,
	API_BASE_URL,
	RUNNING_CLIENT_ID
} from './config.js'
import { expireSession } from './session.js'

export function request(options) {
	return new Promise((resolve, reject) => {
		const accessToken = uni.getStorageSync(ACCESS_TOKEN_STORAGE_KEY)
		const header = {
			clientid: RUNNING_CLIENT_ID,
			...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
			...(options.header || {})
		}

		uni.request({
			url: `${API_BASE_URL}${options.url}`,
			method: options.method || 'GET',
			data: options.data,
			header,
			timeout: options.timeout || 10000,
			success(response) {
				const body = response.data || {}
				if (response.statusCode === 401 || body.code === 401) {
					const message = body.msg || '登录已失效，请重新登录'
					expireSession(message)
					reject(new Error(message))
					return
				}
				if (response.statusCode < 200 || response.statusCode >= 300) {
					reject(new Error(body.msg || `请求失败，HTTP ${response.statusCode}`))
					return
				}
				if (typeof body.code === 'number' && body.code !== 200) {
					reject(new Error(body.msg || '业务请求失败'))
					return
				}
				resolve(body)
			},
			fail(error) {
				reject(new Error(error.errMsg || '无法连接后端'))
			}
		})
	})
}

export function uploadFile(options) {
	return new Promise((resolve, reject) => {
		const accessToken = uni.getStorageSync(ACCESS_TOKEN_STORAGE_KEY)
		const header = {
			clientid: RUNNING_CLIENT_ID,
			...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
			...(options.header || {})
		}

		uni.uploadFile({
			url: `${API_BASE_URL}${options.url}`,
			filePath: options.filePath,
			name: options.name || 'file',
			formData: options.formData,
			header,
			timeout: options.timeout || 30000,
			success(response) {
				let body = response.data || {}
				if (typeof body === 'string') {
					try {
						body = JSON.parse(body)
					} catch (error) {
						reject(new Error('后端返回了无法识别的上传结果'))
						return
					}
				}
				if (response.statusCode === 401 || body.code === 401) {
					const message = body.msg || '登录已失效，请重新登录'
					expireSession(message)
					reject(new Error(message))
					return
				}
				if (response.statusCode < 200 || response.statusCode >= 300) {
					reject(new Error(body.msg || `上传失败，HTTP ${response.statusCode}`))
					return
				}
				if (typeof body.code === 'number' && body.code !== 200) {
					reject(new Error(body.msg || '头像上传失败'))
					return
				}
				resolve(body)
			},
			fail(error) {
				reject(new Error(error.errMsg || '无法上传文件'))
			}
		})
	})
}
