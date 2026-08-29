const path = require('path');
const webpack = require('webpack');

// Taro H5 开发模式下需要强制使用 React development build 来避免 hooks 问题。
// 生产构建保持默认的 production 模式，不能带 React Fast Refresh 相关代码。
const isDev = process.env.NODE_ENV === 'development';

let reactDev;
let reactJsxRuntimeDev;
if (isDev) {
  const reactPkgDir = path.dirname(require.resolve('react/package.json'));
  reactDev = path.join(reactPkgDir, 'cjs', 'react.development.js');
  reactJsxRuntimeDev = path.join(reactPkgDir, 'cjs', 'react-jsx-runtime.development.js');
}

const config = {
  projectName: 'leyo-miniapp-user',
  date: '2026-8-9',
  designWidth: 375,
  deviceRatio: {
    375: 1,
  },
  sourceRoot: 'src',
  outputRoot: 'dist',
  plugins: [],
  defineConstants: {},
  alias: isDev
    ? {
        '@': path.resolve(__dirname, '..', 'src'),
        'react$': reactDev,
        'react/jsx-runtime$': reactJsxRuntimeDev,
      }
    : {
        '@': path.resolve(__dirname, '..', 'src'),
      },
  copy: {
    patterns: [],
    options: {},
  },
  framework: 'react',
  compiler: {
    type: 'webpack5',
    prebundle: {
      enable: false,
    },
  },
  cache: {
    enable: false,
  },
  // Workaround for Taro 4 H5 dev bug: plugin-framework-react incorrectly
  // aliases react to production.min.js in development, causing hooks like
  // useContext to be undefined. Setting harmony.debugReact prevents this.
  harmony: {
    debugReact: true,
  },
  mini: {
    postcss: {
      pxtransform: {
        enable: true,
        config: {},
      },
      url: {
        enable: true,
        config: {
          limit: 1024,
        },
      },
      cssModules: {
        enable: false,
        config: {
          namingPattern: 'module',
          generateScopedName: '[name]__[local]___[hash:base64:5]',
        },
      },
    },
  },
  h5: {
    publicPath: '/h5/user/',
    staticDirectory: 'static',
    esnextModules: ['@leyo/shared'],
    webpackChain(chain) {
      if (isDev) {
        // Force development React build in H5 dev mode. Taro's plugin-framework-react
        // only handles this for harmony/mini builds, leaving webpack5 H5 builds at the
        // mercy of NODE_ENV. Without this, react.production.min.js may be loaded and
        // hooks like useContext become undefined in the bundled output.
        chain.plugin('define-react-env').use(webpack.DefinePlugin, [
          {
            'process.env.NODE_ENV': JSON.stringify('development'),
          },
        ]);
        // Ensure webpack's own nodeEnv optimization stays in development so module
        // resolution and runtime behaviour both use development builds.
        chain.mode('development');
        chain.optimization.nodeEnv('development');
        // Override Taro's internal react$ alias to always use the development build
        // in H5 dev mode. React 18's package.json does not export ./cjs/, so we use
        // the absolute file path directly.
        chain.resolve.alias
          .set('react$', reactDev)
          .set('react/jsx-runtime$', reactJsxRuntimeDev);
      }

      // Treat SVG files as static assets so they can be used with Image src.
      chain.module
        .rule('svg')
        .test(/\.svg$/)
        .type('asset/resource')
        .set('generator', {
          filename: 'static/images/assets/calicat/icons/[name][ext]',
        });
    },
    postcss: {
      // H5 启用 pxtransform 将 px/rpx 转为 rem，配合 app.tsx 动态根字号实现响应式。
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
      hot: true,
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
};

module.exports = function () {
  return config;
};
