import React, { useState, useEffect } from 'react';
import { 
  Table, 
  Button, 
  Space, 
  Input, 
  Modal, 
  Form, 
  message,
  Dropdown,
  Menu,
  Breadcrumb
} from 'antd';
import {
  FileOutlined,
  FolderOutlined,
  PlusOutlined,
  UploadOutlined,
  SearchOutlined,
  MoreOutlined,
  DownloadOutlined,
  DeleteOutlined,
  EditOutlined,
  ShareAltOutlined
} from '@ant-design/icons';
import { useDropzone } from 'react-dropzone';

const FileManager = () => {
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchText, setSearchText] = useState('');
  const [createModalVisible, setCreateModalVisible] = useState(false);
  const [form] = Form.useForm();

  // 模拟文件数据
  const mockFiles = [
    {
      key: '1',
      id: '1',
      name: '项目文档.pdf',
      type: 'file',
      fileType: 'PDF',
      size: '2.3 MB',
      modified: '2024-01-15 14:30',
      path: '/'
    },
    {
      key: '2',
      id: '2',
      name: '数据分析.xlsx',
      type: 'file',
      fileType: 'Excel',
      size: '1.1 MB',
      modified: '2024-01-14 16:45',
      path: '/'
    },
    {
      key: '3',
      id: '3',
      name: '图片素材',
      type: 'folder',
      fileType: '文件夹',
      size: '-',
      modified: '2024-01-13 10:20',
      path: '/'
    }
  ];

  useEffect(() => {
    loadFiles();
  }, []);

  const loadFiles = async () => {
    setLoading(true);
    try {
      // 模拟API调用
      setTimeout(() => {
        setFiles(mockFiles);
        setLoading(false);
      }, 500);
    } catch (error) {
      message.error('加载文件失败');
      setLoading(false);
    }
  };

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop: (acceptedFiles) => {
      message.success(`成功上传 ${acceptedFiles.length} 个文件`);
      // 这里应该调用上传API
    }
  });

  const columns = [
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      render: (text, record) => (
        <Space>
          {record.type === 'folder' ? 
            <FolderOutlined style={{ color: '#1890ff' }} /> : 
            <FileOutlined style={{ color: '#52c41a' }} />
          }
          <span>{text}</span>
        </Space>
      ),
    },
    {
      title: '类型',
      dataIndex: 'fileType',
      key: 'fileType',
    },
    {
      title: '大小',
      dataIndex: 'size',
      key: 'size',
    },
    {
      title: '修改时间',
      dataIndex: 'modified',
      key: 'modified',
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Dropdown
          menu={{
            items: [
              {
                key: 'download',
                icon: <DownloadOutlined />,
                label: '下载',
                onClick: () => handleDownload(record)
              },
              {
                key: 'rename',
                icon: <EditOutlined />,
                label: '重命名',
                onClick: () => handleRename(record)
              },
              {
                key: 'share',
                icon: <ShareAltOutlined />,
                label: '分享',
                onClick: () => handleShare(record)
              },
              {
                key: 'delete',
                icon: <DeleteOutlined />,
                label: '删除',
                danger: true,
                onClick: () => handleDelete(record)
              }
            ]
          }}
          trigger={['click']}
        >
          <Button icon={<MoreOutlined />} />
        </Dropdown>
      ),
    },
  ];

  const handleDownload = (record) => {
    message.info(`下载文件: ${record.name}`);
  };

  const handleRename = (record) => {
    // 实现重命名逻辑
    message.info(`重命名文件: ${record.name}`);
  };

  const handleShare = (record) => {
    // 实现分享逻辑
    message.info(`分享文件: ${record.name}`);
  };

  const handleDelete = (record) => {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除 "${record.name}" 吗？`,
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: () => {
        setFiles(files.filter(file => file.id !== record.id));
        message.success('删除成功');
      }
    });
  };

  const handleCreateFolder = () => {
    setCreateModalVisible(true);
  };

  const handleCreateSubmit = (values) => {
    // 实现创建文件夹逻辑
    message.success(`创建文件夹: ${values.name}`);
    setCreateModalVisible(false);
    form.resetFields();
  };

  return (
    <div>
      {/* 面包屑导航 */}
      <Breadcrumb style={{ marginBottom: 16 }}>
        <Breadcrumb.Item>首页</Breadcrumb.Item>
        <Breadcrumb.Item>文件管理</Breadcrumb.Item>
      </Breadcrumb>

      {/* 操作栏 */}
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
        <Space>
          <Button 
            type="primary" 
            icon={<PlusOutlined />}
            onClick={handleCreateFolder}
          >
            新建文件夹
          </Button>
          <Button icon={<UploadOutlined />}>
            <span {...getRootProps()}>
              <input {...getInputProps()} />
              {isDragActive ? '拖拽文件到这里' : '上传文件'}
            </span>
          </Button>
        </Space>
        <Input
          placeholder="搜索文件..."
          prefix={<SearchOutlined />}
          style={{ width: 300 }}
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
        />
      </div>

      {/* 文件表格 */}
      <div {...getRootProps()} style={{ minHeight: 400 }}>
        <input {...getInputProps()} />
        <Table
          columns={columns}
          dataSource={files}
          loading={loading}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 项`
          }}
          locale={{
            emptyText: isDragActive ? '释放文件以上传' : '暂无文件'
          }}
        />
      </div>

      {/* 创建文件夹模态框 */}
      <Modal
        title="新建文件夹"
        open={createModalVisible}
        onCancel={() => {
          setCreateModalVisible(false);
          form.resetFields();
        }}
        footer={null}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleCreateSubmit}
        >
          <Form.Item
            name="name"
            label="文件夹名称"
            rules={[{ required: true, message: '请输入文件夹名称' }]}
          >
            <Input placeholder="请输入文件夹名称" />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                创建
              </Button>
              <Button onClick={() => {
                setCreateModalVisible(false);
                form.resetFields();
              }}>
                取消
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default FileManager;