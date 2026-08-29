const path = require('path')

const config = {
  projectName: 'leyo-miniapp-coach',
  date: '2026-8-9',
  designWidth: 375,
  deviceRatio: {
    // 与 miniapp-user 保持一致：deviceRatio=1，1rpx 约等于 1px，
    // 避免教练端 H5 字体/图标被放大两倍。
    375: 1,
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
        config: {},
      },
    },
  },
  h5: {
    publicPath: '/h5/coach/',
    staticDirectory: 'static',
    esnextModules: [],
    postcss: {
      // 与 miniapp-user 保持一致，不再设置 baseFontSize，由 deviceRatio=1 保证 1rpx≈1px。
      pxtransform: {
        enable: true,
        config: {},
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
