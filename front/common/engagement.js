import { request } from './request.js'

export function getRoutes() {
	return request({ url: '/app/routes' })
}

export function getRouteDetail(routeId) {
	return request({ url: `/app/routes/${routeId}` })
}

export function selectCurrentRoute(routeId) {
	return request({
		url: '/app/routes/current',
		method: 'PUT',
		data: { routeId }
	})
}

export function getAchievements() {
	return request({ url: '/app/achievements' })
}

export function getRanking(scope = 'CITY', period = 'WEEK') {
	return request({ url: `/app/rankings?scope=${scope}&period=${period}` })
}

export function getCityFootprints() {
	return request({ url: '/app/city-footprints' })
}

export function retryCityFootprints() {
	return request({
		url: '/app/city-footprints/retry',
		method: 'POST'
	})
}

export function getPoints() {
	return request({ url: '/app/points' })
}

export function checkIn() {
	return request({
		url: '/app/points/check-in',
		method: 'POST'
	})
}
