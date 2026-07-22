/**
 * 通用错误页 - 403 / 404
 */
import React from 'react';
import { Button, Result } from 'antd';
import { history } from '@umijs/max';

export const Forbidden: React.FC = () => (
  <Result
    status="403"
    title="403"
    subTitle="抱歉，您没有访问此页面的权限。"
    extra={<Button type="primary" onClick={() => history.push('/workspace')}>返回工作台</Button>}
  />
);

export const NotFound: React.FC = () => (
  <Result
    status="404"
    title="404"
    subTitle="抱歉，您访问的页面不存在。"
    extra={<Button type="primary" onClick={() => history.push('/workspace')}>返回工作台</Button>}
  />
);

export default NotFound;
