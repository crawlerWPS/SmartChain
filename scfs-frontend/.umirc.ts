import { defineConfig } from '@umijs/max';
import routes from './src/routes';

const PROXY_TARGET = process.env.PROXY_TARGET || 'http://localhost:8080';

export default defineConfig({
  antd: {},
  access: {},
  model: {},
  initialState: {},
  request: {},
  layout: {
    title: 'SCFS 供应链金融风控平台',
    logo: '/logo.svg',
    menu: { locale: false },
    siderWidth: 230,
    layout: 'mix',
    fixedHeader: true,
    fixSiderbar: true,
  },
  routes,
  proxy: {
    '/api/v1': {
      target: PROXY_TARGET,
      changeOrigin: true,
    },
  },
  npmClient: 'npm',
  hash: true,
  define: {
    API_BASE: '/api/v1',
  },
  fastRefresh: true,
  mfsu: { strategy: 'normal' },
});
