/**
 * 登录页 - JWT 登录
 */
import React, { useState } from 'react';
import { Card, Form, Input, Button, message, Typography, Layout } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { history, useModel } from '@umijs/max';
import { login } from '@/api/auth';
import { setTokens } from '@/utils/auth';

const { Title } = Typography;

const Login: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const { setInitialState } = useModel('@@initialState');

  const onFinish = async (values: { username: string; password: string }) => {
    setLoading(true);
    try {
      const result = await login(values);
      setTokens(result.accessToken, result.refreshToken);
      const user = result.userInfo;
      localStorage.setItem('scfs_current_user', JSON.stringify(user));
      await setInitialState({ currentUser: user });
      message.success('登录成功');
      history.push('/workspace');
    } catch (e: any) {
      message.error(e.message || '登录失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Layout style={{ minHeight: '100vh', background: 'linear-gradient(135deg, #1890ff 0%, #001529 100%)' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh' }}>
        <Card style={{ width: 380, boxShadow: '0 8px 24px rgba(0,0,0,0.2)' }}>
          <div style={{ textAlign: 'center', marginBottom: 24 }}>
            <Title level={3}>SCFS 供应链金融风控平台</Title>
            <Typography.Text type="secondary">智能风控与尽调辅助系统</Typography.Text>
          </div>
          <Form name="login" onFinish={onFinish} size="large">
            <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
              <Input prefix={<UserOutlined />} placeholder="用户名" />
            </Form.Item>
            <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
              <Input.Password prefix={<LockOutlined />} placeholder="密码" />
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit" loading={loading} block>
                登录
              </Button>
            </Form.Item>
          </Form>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            默认账号：admin / admin123
          </Typography.Text>
        </Card>
      </div>
    </Layout>
  );
};

export default Login;
