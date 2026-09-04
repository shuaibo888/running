const DEVELOPMENT_API_BASE_URL = 'http://127.0.0.1:8080'
const PRODUCTION_API_BASE_URL = 'https://api.example.com'

export const RUNNING_CLIENT_ID = 'running-miniprogram'
export const ACCESS_TOKEN_STORAGE_KEY = 'runningAccessToken'

/**
 * 开发者工具默认访问本机；真机调试可在控制台执行：
 * uni.setStorageSync('runningApiBaseUrl', 'http://电脑局域网IP:8080')
 * 清除覆盖值：uni.removeStorageSync('runningApiBaseUrl')
 * 正式发布前必须替换 PRODUCTION_API_BASE_URL，并配置微信 request 合法域名。
 */
const storedApiBaseUrl = uni.getStorageSync('runningApiBaseUrl')

export const API_BASE_URL = storedApiBaseUrl || (
	process.env.NODE_ENV === 'production'
		? PRODUCTION_API_BASE_URL
		: DEVELOPMENT_API_BASE_URL
)
