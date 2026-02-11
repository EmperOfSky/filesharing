import React, { useState, useEffect } from 'react';
import { Row, Col, Card, Statistic, Table, Button, Space } from 'antd';
import {
  FileOutlined,
  FolderOutlined,
  ShareAltOutlined,
  CloudUploadOutlined,
  BarChartOutlined,
  DownloadOutlined
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';

const Dashboard = () => {
  const navigate = useNavigate();
  const [stats, setStats] = useState({
    totalFiles: 0,
    totalFolders: 0,
    totalShares: 0,
    usedStorage: 0,
    totalStorage: 1073741824 // 1GB
  });

  const recentFiles = [
    {
      key: '1',
      name: '项目文档.pdf',
      type: 'PDF',
      size: '2.3 MB',
      date: '2024-01-15 14:30'
    },
    {
      key: '2',
      name: '数据分析.xlsx',
      type: 'Excel',
      size: '1.1 MB',
      date: '2024-01-14 16:45'
    },
    {
      key: '3',
      name: '产品介绍.pptx',
      type: 'PowerPoint',
      size: '5.7 MB',
      date: '2024-01-13 10:20'
    }
  ];

  const columns = [
    {
      title: '文件名',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
    },
    {
      title: '大小',
      dataIndex: 'size',
      key: 'size',
    },
    {
      title: '日期',
      dataIndex: 'date',
      key: 'date',
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space size="middle">
          <Button type="link" icon={<DownloadOutlined />}>下载</Button>
          <Button type="link">预览</Button>
        </Space>
      ),
    },
  ];

  const storagePercentage = Math.round((stats.usedStorage / stats.totalStorage) * 100);

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <h1 style={{ fontSize: 24, fontWeight: 'bold', marginBottom: 8 }}>仪表板</h1>
        <p>欢迎回来！这里是您的文件管理中心。</p>
      </div>

      {/* 统计卡片 */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="总文件数"
              value={stats.totalFiles}
              prefix={<FileOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="文件夹数"
              value={stats.totalFolders}
              prefix={<FolderOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="分享数"
              value={stats.totalShares}
              prefix={<ShareAltOutlined />}
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="存储使用率"
              value={storagePercentage}
              suffix="%"
              prefix={<BarChartOutlined />}
              valueStyle={{ color: storagePercentage > 80 ? '#ff4d4f' : '#1890ff' }}
            />
          </Card>
        </Col>
      </Row>

      {/* 快捷操作 */}
      <Card title="快捷操作" style={{ marginBottom: 24 }}>
        <Space>
          <Button 
            type="primary" 
            icon={<CloudUploadOutlined />}
            onClick={() => navigate('/files')}
          >
            上传文件
          </Button>
          <Button icon={<FolderOutlined />}>新建文件夹</Button>
          <Button icon={<ShareAltOutlined />}>创建分享</Button>
        </Space>
      </Card>

      {/* 最近文件 */}
      <Card title="最近文件">
        <Table 
          columns={columns} 
          dataSource={recentFiles} 
          pagination={false}
          size="small"
        />
      </Card>
    </div>
  );
};

export default Dashboard;