/**
 * 认证相关API
 */
import request from '@/utils/request'

/**
 * 用户登录
 * @param {Object} data - 登录信息
 * @param {string} data.username - 用户名
 * @param {string} data.password - 密码
 * @returns {Promise} 登录结果
 */
export function login(data) {
  return request({
    url: '/api/v1/auth/login',
    method: 'post',
    data
  })
}

/**
 * 用户注册
 * @param {Object} data - 注册信息
 * @returns {Promise} 注册结果
 */
export function register(data) {
  return request({
    url: '/api/v1/auth/register',
    method: 'post',
    data
  })
}

/**
 * 用户登出
 * @returns {Promise} 登出结果
 */
export function logout() {
  return request({
    url: '/api/v1/auth/logout',
    method: 'post'
  })
}

/**
 * 获取用户信息
 * @returns {Promise} 用户信息
 */
export function getUserInfo() {
  return request({
    url: '/api/v1/auth/info',
    method: 'get'
  })
}

/**
 * 刷新Token
 * @returns {Promise} 新Token
 */
export function refreshToken() {
  return request({
    url: '/api/v1/auth/refresh',
    method: 'post'
  })
}
