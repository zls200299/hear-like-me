<template>
  <view class="container">
    <view class="header">
      <text class="title">首页</text>
      <text class="subtitle" v-if="userStore.isLoggedIn">你好，{{ userStore.nickname || '用户' }}</text>
    </view>

    <view class="content">
      <text class="placeholder">业务页面由 Generator 生成</text>
    </view>

    <button v-if="!userStore.isLoggedIn" class="login-btn" @click="goLogin">去登录</button>
    <button v-else class="logout-btn" @click="handleLogout">退出登录</button>
  </view>
</template>

<script setup>
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()

const goLogin = () => {
  uni.navigateTo({ url: '/pages/login/index' })
}

const handleLogout = () => {
  uni.showModal({
    title: '提示',
    content: '确定退出登录？',
    success: (res) => {
      if (res.confirm) userStore.logout()
    }
  })
}
</script>

<style scoped>
.container {
  padding: 40rpx;
}
.header {
  margin-bottom: 60rpx;
}
.title {
  font-size: 48rpx;
  font-weight: bold;
  display: block;
  margin-bottom: 16rpx;
}
.subtitle {
  font-size: 28rpx;
  color: #999;
}
.content {
  padding: 80rpx 0;
  text-align: center;
}
.placeholder {
  font-size: 28rpx;
  color: #ccc;
}
.login-btn {
  margin-top: 60rpx;
  background-color: #2979ff;
  color: #fff;
}
.logout-btn {
  margin-top: 60rpx;
  background-color: #f5f5f5;
  color: #666;
}
</style>
