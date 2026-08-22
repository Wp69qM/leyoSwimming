const path = require('path')

const config = {
  projectName: 'leyo-miniapp-coach',
  date: '2026-8-9',
  designWidth: 375,
  deviceRatio: {
    // H5 rem 模式下 rootValue = (baseFontSize / deviceRatio) * 2；
    // 设 baseFontSize=16、deviceRatio=2，可使 rootValue=16，1rpx 约等于 1px。
    375: 2,
  },
  sourceRoot: 'src',
  outputRoot: 'dist',
  plugins: [],
  defineConstants: {},
  alias: {
    '@': path.resolve(__dirname, '..', 'src'),
  },
  copy: {
    patterns: [],
    options: {},
  },
  framework: 'react',
  compiler: 'webpack5',
  cache: {
    enable: false,
  },
  mini: {
    postcss: {
      pxtransform: {
        enable: true,
        config: {
          baseFontSize: 16,
        },
      },
    },
  },
  h5: {
    publicPath: '/',
    staticDirectory: 'static',
    esnextModules: [],
    postcss: {
      // H5 启用 pxtransform 将 rpx 转为 rem；baseFontSize:16 + deviceRatio:2 使 rootValue=16，1rpx 约等于 1px。
      pxtransform: {
        enable: true,
        config: {
          baseFontSize: 16,
        },
      },
      autoprefixer: {
        enable: true,
        config: {},
      },
      cssModules: {
        enable: false,
        config: {
          namingPattern: 'module',
          generateScopedName: '[name]__[local]___[hash:base64:5]',
        },
      },
    },
    devServer: {
      port: 10086,
      proxy: {
        '/api': {
          target: 'http://127.0.0.1:8080',
          changeOrigin: true,
        },
        '/uploads': {
          target: 'http://127.0.0.1:8080',
          changeOrigin: true,
        },
      },
    },
  },
}

module.exports = function () {
  return config
}
