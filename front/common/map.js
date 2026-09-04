import { request } from './request.js'

export function reverseGeocode(latitude, longitude) {
	return request({
		url: '/app/map/reverse-geocode',
		method: 'POST',
		data: { latitude, longitude }
	})
}
