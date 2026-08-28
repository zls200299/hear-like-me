import { post } from '@/utils/request'

export const sendCode = (phone) => post('/api/auth/send-code', { phone })

export const phoneLogin = (phone, code) => post('/api/auth/phone-login', { phone, code })

export const getCurrentUser = () => post('/api/auth/current-user')

export const logout = () => post('/api/auth/logout')
