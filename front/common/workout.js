import { request } from './request.js'

export function startWorkout(clientWorkoutId, initialPoint) {
	return request({
		url: '/app/workouts',
		method: 'POST',
		data: {
			clientWorkoutId,
			initialPoint
		}
	})
}

export function getDurationSportTypes() {
	return request({ url: '/app/sport-types' })
}

export function recordManualWorkout(clientWorkoutId, sportType, durationMinutes) {
	return request({
		url: '/app/workouts/manual',
		method: 'POST',
		data: { clientWorkoutId, sportType, durationMinutes }
	})
}

export function getActiveWorkout() {
	return request({ url: '/app/workouts/active' })
}

export function getWorkoutHistory(pageNum = 1, pageSize = 10) {
	return request({ url: `/app/workouts?pageNum=${pageNum}&pageSize=${pageSize}` })
}

export function getWorkoutDetail(workoutId) {
	return request({ url: `/app/workouts/${workoutId}` })
}

export function getWorkoutTrackPoints(workoutId, afterSequence = -1, limit = 500) {
	return request({
		url: `/app/workouts/${workoutId}/track-points?afterSequence=${afterSequence}&limit=${limit}`
	})
}

export function getWorkoutStatistics() {
	return request({ url: '/app/statistics/overview' })
}

export function getWorkoutTrends(days = 7) {
	return request({ url: `/app/statistics/trends?days=${days}` })
}

export function uploadTrackBatch(workoutId, clientBatchId, points) {
	return request({
		url: `/app/workouts/${workoutId}/track-batches`,
		method: 'POST',
		data: { clientBatchId, points },
		timeout: 15000
	})
}

export function pauseWorkout(workoutId) {
	return request({ url: `/app/workouts/${workoutId}/pause`, method: 'POST' })
}

export function resumeWorkout(workoutId) {
	return request({ url: `/app/workouts/${workoutId}/resume`, method: 'POST' })
}

export function abandonWorkout(workoutId) {
	return request({ url: `/app/workouts/${workoutId}/abandon`, method: 'POST' })
}

export function finishWorkout(workoutId, clientFinishId) {
	return request({
		url: `/app/workouts/${workoutId}/finish`,
		method: 'POST',
		data: { clientFinishId }
	})
}
