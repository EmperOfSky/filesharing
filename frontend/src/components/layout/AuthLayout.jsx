import React from 'react';
import { Layout, Card } from 'antd';
import { useNavigate } from 'react-router-dom';

const { Content } = Layout;

const AuthLayout = ({ children }) => {
  const navigate = useNavigate();

  return (
    <Layout className="auth-layout" style={{ minHeight: '100vh' }}>
      <Content style={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
      }}>
        <Card 
          style={{ 
            width: 400,
            borderRadius: 8,
            boxShadow: '0 4px 12px rgba(0,0,0,0.15)'
          }}
          title={
            <div style={{ textAlign: 'center', fontSize: 24, fontWeight: 'bold' }}>
              文件共享系统
            </div>
          }
        >
          {children}
        </Card>
      </Content>
    </Layout>
  );
};

export default AuthLayout;