import { request, uploadFile } from './request.js'

export function getCurrentProfile() {
	return request({
		url: '/app/user/profile'
	})
}

export function saveCurrentProfile(profile) {
	return request({
		url: '/app/user/profile',
		method: 'PUT',
		data: profile
	})
}

export function uploadProfileAvatar(filePath) {
	return uploadFile({
		url: '/app/user/profile/avatar',
		filePath,
		name: 'file'
	})
}
