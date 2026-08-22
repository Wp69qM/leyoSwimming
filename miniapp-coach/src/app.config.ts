export default defineAppConfig({
  pages: [
    'pages/index/index',
    'pages/booking/index',
    'pages/student/index',
    'pages/student/detail/index',
    'pages/student/package-detail/index',
    'pages/mine/index',
    'pages/coach/index',
    'pages/login/wechat/index',
    'pages/login/phone/index',
    'pages/login/protocol/index',
    'pages/profile/complete/index',
    'pages/onboarding/index/index',
    'pages/onboarding/success/index',
    'pages/onboarding/pending/index',
    'pages/resignation/apply/index',
    'pages/resignation/ticket/index',
    'pages/resignation/processing/index'
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#fff',
    navigationBarTitleText: 'leyoSwimming 教练端',
    navigationBarTextStyle: 'black'
  },
  tabBar: {
    color: '#8C8C8C',
    selectedColor: '#1890FF',
    backgroundColor: '#FFFFFF',
    borderStyle: 'white',
    list: [
      {
        pagePath: 'pages/index/index',
        text: '首页'
      },
      {
        pagePath: 'pages/booking/index',
        text: '预约'
      },
      {
        pagePath: 'pages/student/index',
        text: '学员'
      },
      {
        pagePath: 'pages/mine/index',
        text: '我的'
      }
    ]
  }
})
