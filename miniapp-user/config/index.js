const path = require('path');
const webpack = require('webpack');

// Force development mode for the build process so React resolves its
// development builds (react.development.js, react-jsx-runtime.development.js)
// instead of the production minified versions that break hooks in Taro H5 dev.
process.env.NODE_ENV = 'development';

const reactPkgDir = path.dirname(require.resolve('react/package.json'));
const reactDev = path.join(reactPkgDir, 'cjs', 'react.development.js');
const reactJsxRuntimeDev = path.join(reactPkgDir, 'cjs', 'react-jsx-runtime.development.js');

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
  alias: {
    '@': path.resolve(__dirname, '..', 'src'),
    'react$': reactDev,
    'react/jsx-runtime$': reactJsxRuntimeDev,
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
    publicPath: '/',
    staticDirectory: 'static',
    esnextModules: ['@leyo/shared'],
    webpackChain(chain) {
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
