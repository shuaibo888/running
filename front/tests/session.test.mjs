import test from 'node:test'
import assert from 'node:assert/strict'

const removed = []
const toasts = []
const launches = []

globalThis.uni = {
	getStorageSync() { return '' },
	removeStorageSync(key) { removed.push(key) },
	showToast(options) { toasts.push(options) },
	reLaunch(options) { launches.push(options) }
}
globalThis.process = { env: { NODE_ENV: 'test' } }

const {
	clearLocalSession,
	expireSession,
	markSessionActive
} = await import('../common/session.js')

function resetObservations() {
	removed.length = 0
	toasts.length = 0
	launches.length = 0
	markSessionActive()
}

test('clearLocalSession removes both identity and account-bound workout cache', () => {
	resetObservations()
	clearLocalSession()
	assert.deepEqual(removed, ['runningAccessToken', 'running_active_workout_v1'])
})

test('concurrent expired requests result in one notification and redirect', async () => {
	resetObservations()
	expireSession('会话过期')
	expireSession('重复响应')
	await new Promise(resolve => setTimeout(resolve, 120))

	assert.deepEqual(removed, [
		'runningAccessToken', 'running_active_workout_v1',
		'runningAccessToken', 'running_active_workout_v1'
	])
	assert.equal(toasts.length, 1)
	assert.equal(toasts[0].title, '会话过期')
	assert.equal(launches.length, 1)
	assert.equal(launches[0].url, '/pages/login/login')
})

test('a successful login allows a future expiry redirect', async () => {
	markSessionActive()
	expireSession()
	await new Promise(resolve => setTimeout(resolve, 120))
	assert.equal(launches.length, 2)
})
