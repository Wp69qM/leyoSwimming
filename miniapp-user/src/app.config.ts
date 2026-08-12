export default {
  pages: [
    'pages/index/index',
    'pages/coach/index',
    'pages/booking/index',
    'pages/mine/index',
    'pages/login/wechat/index',
    'pages/login/phone/index',
    'pages/login/protocol/index',
    'pages/profile/complete/index',
    'pages/account/cancel/index',
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#ffffff',
    navigationBarTitleText: 'leyoSwimming',
    navigationBarTextStyle: 'black',
  },
  tabBar: {
    color: '#999999',
    selectedColor: '#1890ff',
    backgroundColor: '#ffffff',
    borderStyle: 'black',
    list: [
      {
        pagePath: 'pages/index/index',
        text: '首页',
      },
      {
        pagePath: 'pages/coach/index',
        text: '教练',
      },
      {
        pagePath: 'pages/booking/index',
        text: '预约',
      },
      {
        pagePath: 'pages/mine/index',
        text: '我的',
      },
    ],
  },
};
