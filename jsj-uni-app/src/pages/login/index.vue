<template>
  <view class="login-page">
    <view class="logo-area">
      <text class="logo-text">JSJ</text>
    </view>

    <view class="form">
      <view class="form-item">
        <input
          v-model="phone"
          type="number"
          maxlength="11"
          placeholder="请输入手机号"
          class="input"
        />
      </view>

      <view class="form-item code-row">
        <input
          v-model="code"
          type="number"
          maxlength="6"
          placeholder="请输入验证码"
          class="input code-input"
        />
        <button
          class="code-btn"
          :disabled="countdown > 0"
          @click="handleSendCode"
        >
          {{ countdown > 0 ? countdown + 's' : '获取验证码' }}
        </button>
      </view>

      <button class="submit-btn" :loading="loading" @click="handleLogin">
        登录
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { useUserStore } from '@/store/modules/user'
import { sendCode } from '@/api/auth'

const userStore = useUserStore()

const phone = ref('')
const code = ref('')
const loading = ref(false)
const countdown = ref(0)

let timer = null

const handleSendCode = async () => {
  if (!phone.value || phone.value.length !== 11) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  try {
    await sendCode(phone.value)
    uni.showToast({ title: '验证码已发送', icon: 'none' })
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (e) {
    // request.js 已处理 toast
  }
}

const handleLogin = async () => {
  if (!phone.value || phone.value.length !== 11) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  if (!code.value) {
    uni.showToast({ title: '请输入验证码', icon: 'none' })
    return
  }
  loading.value = true
  try {
    await userStore.login(phone.value, code.value)
    uni.reLaunch({ url: '/pages/index/index' })
  } catch (e) {
    // request.js 已处理 toast
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  padding: 80rpx 60rpx;
  min-height: 100vh;
  background-color: #fff;
}
.logo-area {
  text-align: center;
  margin-bottom: 100rpx;
  padding-top: 80rpx;
}
.logo-text {
  font-size: 72rpx;
  font-weight: bold;
  color: #2979ff;
}
.form-item {
  margin-bottom: 40rpx;
}
.input {
  height: 88rpx;
  border: 1rpx solid #e5e5e5;
  border-radius: 12rpx;
  padding: 0 30rpx;
  font-size: 30rpx;
}
.code-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.code-input {
  flex: 1;
}
.code-btn {
  width: 240rpx;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 26rpx;
  background-color: #f5f5f5;
  color: #333;
  border: none;
  border-radius: 12rpx;
  padding: 0;
}
.code-btn[disabled] {
  color: #999;
}
.submit-btn {
  margin-top: 60rpx;
  height: 88rpx;
  line-height: 88rpx;
  background-color: #2979ff;
  color: #fff;
  border: none;
  border-radius: 12rpx;
  font-size: 32rpx;
}
</style>
