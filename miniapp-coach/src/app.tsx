import { Component, PropsWithChildren } from 'react'
import Taro from '@tarojs/taro'
import { useAuthStore } from '@/stores/authStore'
import { isTokenExpired } from '@/api/request'
import './app.scss'

class App extends Component<PropsWithChildren<unknown>> {
  onShow() {
    const { restoreFromStorage, isLoggedIn, logout } = useAuthStore.getState()
    restoreFromStorage()

    if (isLoggedIn && isTokenExpired()) {
      logout()
      Taro.redirectTo({ url: '/pages/login/wechat/index' })
    }
  }

  render() {
    return this.props.children
  }
}

export default App
