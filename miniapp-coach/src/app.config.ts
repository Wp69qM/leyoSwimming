export default defineAppConfig({
  pages: [
    'pages/index/index',
    'pages/login/wechat/index',
    'pages/login/phone/index',
    'pages/login/protocol/index',
    'pages/profile/complete/index',
    'pages/resignation/apply/index',
    'pages/resignation/ticket/index',
    'pages/resignation/processing/index'
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#fff',
    navigationBarTitleText: 'leyoSwimming 教练端',
    navigationBarTextStyle: 'black'
  }
})
