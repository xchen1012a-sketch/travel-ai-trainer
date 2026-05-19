import {
  ApiOutlined,
  AppstoreOutlined,
  AuditOutlined,
  BankOutlined,
  BookOutlined,
  CheckCircleOutlined,
  CloudUploadOutlined,
  CommentOutlined,
  DashboardOutlined,
  DatabaseOutlined,
  ExperimentOutlined,
  FileDoneOutlined,
  HomeOutlined,
  LoginOutlined,
  LogoutOutlined,
  MessageOutlined,
  MonitorOutlined,
  PlusOutlined,
  RobotOutlined,
  SafetyCertificateOutlined,
  SendOutlined,
  SettingOutlined,
  SolutionOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons';
import {
  Avatar,
  Button,
  Card,
  Col,
  Divider,
  Empty,
  Form,
  Input,
  List,
  Progress,
  Row,
  Segmented,
  Space,
  Statistic,
  Steps,
  Table,
  Tabs,
  Tag,
  Timeline,
  Typography,
  Upload,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PageContainer, ProLayout } from '@ant-design/pro-components';
import { useEffect, useMemo, useState } from 'react';
import heroDashboard from './assets/hero-dashboard.png';
import {
  roleText,
  type AgentRow,
  type AuditRow,
  type ClassRow,
  type CallLogRow,
  type ConversationRow,
  type CourseRow,
  type DashboardStats,
  type AttractionSpeechRow,
  type CustomerProfileRow,
  type JobRow,
  type KnowledgeFileRow,
  type ModelConfigRow,
  type ProductRow,
  type Role,
  type RouteItem,
  type StudentClassRow,
  type StudentTaskRow,
  type TaskRow,
  type TemplateRow,
  type TrainingScenarioRow,
  type WorkflowConfigRow,
} from './data';
import { api, notifyApiPending } from './services/api';

type Session = {
  role: Role;
  name: string;
};

const { Title, Paragraph, Text } = Typography;
const { Dragger } = Upload;

function useAsyncData<T>(loader: () => Promise<T>, initialValue: T) {
  const [data, setData] = useState<T>(initialValue);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    setLoading(true);
    loader()
      .then((nextData) => {
        if (active) setData(nextData);
      })
      .catch(() => {
        if (active) message.error('数据接口请求失败');
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [loader]);

  return { data, loading };
}

function EmptyModule({ title = '暂无数据', description = '接口接入后将在这里显示业务数据。' }) {
  return (
    <div className="module-empty">
      <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={title}>
        <Text type="secondary">{description}</Text>
      </Empty>
    </div>
  );
}

function FieldTags({ fields }: { fields: string[] }) {
  return (
    <Space size={[6, 6]} wrap>
      {fields.map((field) => (
        <Tag key={field}>{field}</Tag>
      ))}
    </Space>
  );
}

function PendingResult({ title, description }: { title: string; description?: string }) {
  return (
    <div className="pending-result">
      <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={title}>
        {description ? <Text type="secondary">{description}</Text> : null}
      </Empty>
    </div>
  );
}

function pendingAction() {
  message.info(notifyApiPending());
}

const roleNames: Record<Role, string> = {
  TEACHER: '教师用户',
  STUDENT: '学生用户',
  ADMIN: '平台管理员',
};

const rolePaths: Record<Role, string> = {
  TEACHER: '/teacher',
  STUDENT: '/student',
  ADMIN: '/admin',
};

type WorkflowDefinition = {
  key: string;
  name: string;
  scene: string;
  variables: string[];
};

const workflowDefinitions: WorkflowDefinition[] = [
  {
    key: 'course-qa',
    name: '课程问答',
    scene: 'dify-workflows/01-course-qa.yml',
    variables: ['course_name', 'lesson_topic', 'student_question', 'student_profile', 'retrieved_context', 'answer_style'],
  },
  {
    key: 'simulated-customer',
    name: 'AI 模拟客户',
    scene: 'dify-workflows/02-simulated-customer.yml',
    variables: [
      'scenario',
      'customer_profile',
      'training_goal',
      'difficulty',
      'coaching_mode',
      'conversation_history',
      'student_message',
    ],
  },
  {
    key: 'ai-advisor',
    name: 'AI 参谋',
    scene: 'dify-workflows/03-ai-advisor.yml',
    variables: [
      'customer_profile',
      'discovered_needs',
      'budget',
      'travel_time',
      'companions',
      'destination_preferences',
      'constraints',
      'product_options',
      'conversation_history',
    ],
  },
  {
    key: 'attraction-speech',
    name: '景点话术',
    scene: 'dify-workflows/04-attraction-speech.yml',
    variables: [
      'attraction_name',
      'attraction_basic_info',
      'geography_type',
      'culture_tags',
      'customer_experience_level',
      'customer_scene',
      'selling_goal',
      'course_context',
    ],
  },
  {
    key: 'product-change',
    name: '产品变更',
    scene: 'dify-workflows/05-product-change.yml',
    variables: ['product_name', 'change_date', 'old_product_info', 'new_product_info', 'compare_focus', 'target_audience'],
  },
  {
    key: 'travel-simulation',
    name: '旅行模拟',
    scene: 'dify-workflows/06-travel-simulation.yml',
    variables: [
      'route_name',
      'itinerary',
      'node_name',
      'node_index',
      'customer_profile',
      'student_action',
      'simulation_state',
      'product_context',
      'learning_goal',
    ],
  },
];

const teacherRoutes: RouteItem[] = [
  { path: '/teacher', name: '工作台', icon: <DashboardOutlined /> },
  { path: '/teacher/courses', name: '课程与实训', icon: <BookOutlined /> },
  { path: '/teacher/agents', name: '智能体管理', icon: <RobotOutlined /> },
  { path: '/teacher/knowledge', name: '知识库管理', icon: <DatabaseOutlined /> },
  { path: '/teacher/templates', name: '模板中心', icon: <AppstoreOutlined /> },
  { path: '/teacher/workflows', name: '工作流配置', icon: <ApiOutlined /> },
  { path: '/teacher/products', name: '旅游产品库', icon: <BankOutlined /> },
  { path: '/teacher/attractions', name: '景点话术库', icon: <CommentOutlined /> },
  { path: '/teacher/customer-profiles', name: '客户画像库', icon: <UserOutlined /> },
  { path: '/teacher/scenarios', name: '训练场景库', icon: <ExperimentOutlined /> },
  { path: '/teacher/classes', name: '班级管理', icon: <TeamOutlined /> },
  { path: '/teacher/interactions', name: '互动记录', icon: <MessageOutlined /> },
  { path: '/teacher/tasks', name: '任务与反馈', icon: <FileDoneOutlined /> },
];

const studentRoutes: RouteItem[] = [
  { path: '/student', name: '学习首页', icon: <DashboardOutlined /> },
  { path: '/student/classes', name: '我的班级', icon: <TeamOutlined /> },
  { path: '/student/practice', name: 'AI 实训对话', icon: <CommentOutlined /> },
  { path: '/student/travel-practice', name: '旅游销售对练', icon: <ExperimentOutlined /> },
  { path: '/student/course-qa', name: '课程问答', icon: <BookOutlined /> },
  { path: '/student/customer-practice', name: '客户模拟对练', icon: <UserOutlined /> },
  { path: '/student/advisor', name: 'AI 参谋', icon: <SolutionOutlined /> },
  { path: '/student/attraction-speech', name: '景点话术训练', icon: <BankOutlined /> },
  { path: '/student/product-changes', name: '产品变更提醒', icon: <AuditOutlined /> },
  { path: '/student/travel-simulation', name: '旅行模拟', icon: <MonitorOutlined /> },
  { path: '/student/tasks', name: '任务与反馈', icon: <FileDoneOutlined /> },
];

const adminRoutes: RouteItem[] = [
  { path: '/admin', name: '管理概览', icon: <DashboardOutlined /> },
  { path: '/admin/users', name: '用户与角色', icon: <UserOutlined /> },
  { path: '/admin/system', name: '系统配置', icon: <SettingOutlined /> },
  { path: '/admin/workflows', name: 'Dify 工作流管理', icon: <ApiOutlined /> },
  { path: '/admin/models', name: '模型与密钥配置', icon: <RobotOutlined /> },
  { path: '/admin/call-logs', name: '调用日志', icon: <MessageOutlined /> },
  { path: '/admin/jobs', name: '队列/异步任务', icon: <FileDoneOutlined /> },
  { path: '/admin/audit', name: '审计日志', icon: <AuditOutlined /> },
];

function usePathname() {
  const [pathname, setPathname] = useState(window.location.pathname);

  useEffect(() => {
    const sync = () => setPathname(window.location.pathname);
    window.addEventListener('popstate', sync);
    return () => window.removeEventListener('popstate', sync);
  }, []);

  const navigate = (path: string) => {
    window.history.pushState(null, '', path);
    window.dispatchEvent(new Event('popstate'));
    window.scrollTo({ top: 0 });
  };

  return { pathname, navigate };
}

function readSession(): Session | null {
  try {
    const raw = window.localStorage.getItem('teach-agent-session');
    return raw ? (JSON.parse(raw) as Session) : null;
  } catch {
    return null;
  }
}

function inferRole(pathname: string): Role | null {
  if (pathname.startsWith('/teacher')) return 'TEACHER';
  if (pathname.startsWith('/student')) return 'STUDENT';
  if (pathname.startsWith('/admin')) return 'ADMIN';
  return null;
}

function LogoMark() {
  return (
    <div className="logo-mark" aria-hidden="true">
      <RobotOutlined />
    </div>
  );
}

function App() {
  const { pathname, navigate } = usePathname();
  const [session, setSession] = useState<Session | null>(() => readSession());

  const signIn = (role: Role) => {
    const nextSession = { role, name: roleNames[role] };
    setSession(nextSession);
    window.localStorage.setItem('teach-agent-session', JSON.stringify(nextSession));
    navigate(rolePaths[role]);
    message.success(`已进入${roleText[role]}空间`);
  };

  const signOut = () => {
    setSession(null);
    window.localStorage.removeItem('teach-agent-session');
    navigate('/');
  };

  if (pathname === '/login') {
    return <LoginPage onNavigate={navigate} onSignIn={signIn} />;
  }

  const pathRole = inferRole(pathname);
  if (pathRole) {
    const activeSession = session?.role === pathRole ? session : { role: pathRole, name: roleNames[pathRole] };
    return (
      <WorkspaceShell
        pathname={pathname}
        role={pathRole}
        session={activeSession}
        onNavigate={navigate}
        onSignOut={signOut}
      />
    );
  }

  return <LandingPage onNavigate={navigate} />;
}

function LandingPage({ onNavigate }: { onNavigate: (path: string) => void }) {
  return (
    <main className="landing">
      <header className="landing-nav">
        <button className="brand-button" type="button" onClick={() => onNavigate('/')}>
          <LogoMark />
          <span>智教实训</span>
        </button>
        <nav aria-label="首页导航">
          <a href="#workflow">教学流程</a>
          <a href="#scenes">应用场景</a>
          <Button icon={<LoginOutlined />} type="primary" onClick={() => onNavigate('/login')}>
            登录
          </Button>
        </nav>
      </header>

      <section
        className="landing-hero"
        style={{
          backgroundImage: `linear-gradient(90deg, rgba(248, 251, 255, 0.98), rgba(248, 251, 255, 0.82) 42%, rgba(248, 251, 255, 0.26) 74%), url(${heroDashboard})`,
        }}
      >
        <div className="hero-copy">
          <Title>智教实训智能体平台</Title>
          <Paragraph>
            面向教师搭建课程智能体、班级互动、知识库实训和过程反馈的一体化教学空间。
          </Paragraph>
          <Space size={12} wrap>
            <Button icon={<LoginOutlined />} type="primary" size="large" onClick={() => onNavigate('/login')}>
              进入平台
            </Button>
            <Button icon={<MonitorOutlined />} size="large" onClick={() => onNavigate('/teacher')}>
              查看教师空间
            </Button>
          </Space>
        </div>
        <div className="hero-metrics" aria-label="平台能力摘要">
          <div>
            <strong>建课</strong>
            <span>课程与知识库</span>
          </div>
          <div>
            <strong>实训</strong>
            <span>AI 对练课堂</span>
          </div>
          <div>
            <strong>反馈</strong>
            <span>过程追踪评价</span>
          </div>
        </div>
      </section>

      <section className="landing-band" id="workflow">
        <div className="section-head">
          <Text type="secondary">Workflow</Text>
          <Title level={2}>从资料到课堂互动的完整闭环</Title>
        </div>
        <Steps
          className="workflow-steps"
          items={[
            { title: '教师建课', description: '课程、实训模块、任务要求' },
            { title: '配置智能体', description: '提示词、开场白、Dify 工作流' },
            { title: '绑定知识库', description: '讲义、案例、模板、评价标准' },
            { title: '学生进班', description: '班级码加入、AI 实训对话' },
            { title: '教师管理', description: '查看记录、介入回复、反馈批阅' },
          ]}
        />
      </section>

      <section className="landing-band muted" id="scenes">
        <div className="section-head">
          <Text type="secondary">Scenes</Text>
          <Title level={2}>围绕教学可控性设计的功能入口</Title>
        </div>
        <div className="scene-grid">
          {[
            ['教师空间', '智能体、知识库、班级、任务和互动记录集中管理。', <SolutionOutlined />],
            ['学生空间', '班级码加入课程，在任务约束下完成 AI 实训。', <CommentOutlined />],
            ['管理空间', '用户角色、系统配置、审计日志和安全策略。', <SafetyCertificateOutlined />],
            ['后端服务', '接入 Spring Boot、MySQL 与 Dify 工作流。', <ApiOutlined />],
          ].map(([title, desc, icon]) => (
            <Card key={String(title)} className="scene-card">
              <div className="scene-icon">{icon}</div>
              <Title level={4}>{title}</Title>
              <Paragraph>{desc}</Paragraph>
            </Card>
          ))}
        </div>
      </section>
    </main>
  );
}

function LoginPage({
  onNavigate,
  onSignIn,
}: {
  onNavigate: (path: string) => void;
  onSignIn: (role: Role) => void;
}) {
  const [role, setRole] = useState<Role>('TEACHER');

  return (
    <main className="login-page">
      <button className="brand-button login-brand" type="button" onClick={() => onNavigate('/')}>
        <LogoMark />
        <span>智教实训</span>
      </button>
      <section className="login-shell">
        <div className="login-panel">
          <Tag color="processing">角色登录</Tag>
          <Title level={2}>登录教学智能体平台</Title>
          <Paragraph type="secondary">登录后按角色进入对应工作空间。</Paragraph>
          <Segmented
            block
            value={role}
            onChange={(value) => setRole(value as Role)}
            options={[
              { label: '教师', value: 'TEACHER' },
              { label: '学生', value: 'STUDENT' },
              { label: '管理员', value: 'ADMIN' },
            ]}
          />
          <Form
            layout="vertical"
            className="login-form"
            initialValues={{ username: role.toLowerCase(), password: '123456' }}
            onFinish={() => onSignIn(role)}
          >
            <Form.Item label="账号" name="username">
              <Input prefix={<UserOutlined />} />
            </Form.Item>
            <Form.Item label="密码" name="password">
              <Input.Password />
            </Form.Item>
            <Button block icon={<LoginOutlined />} htmlType="submit" size="large" type="primary">
              进入{roleText[role]}空间
            </Button>
          </Form>
        </div>
        <div className="login-side">
          <Title level={3}>首版前端范围</Title>
          <Timeline
            items={[
              { color: 'blue', children: '官网首页与登录入口' },
              { color: 'green', children: '教师、学生、管理员三类空间' },
              { color: 'orange', children: '核心业务页面与接口联调' },
              { color: 'gray', children: '后期接入 Spring Boot 与 Dify' },
            ]}
          />
        </div>
      </section>
    </main>
  );
}

function WorkspaceShell({
  pathname,
  role,
  session,
  onNavigate,
  onSignOut,
}: {
  pathname: string;
  role: Role;
  session: Session;
  onNavigate: (path: string) => void;
  onSignOut: () => void;
}) {
  const routes = role === 'TEACHER' ? teacherRoutes : role === 'STUDENT' ? studentRoutes : adminRoutes;
  const activeRoute = routes.find((route) => route.path === pathname) ?? routes[0];
  const safePathname = activeRoute.path;
  const primaryAction = {
    TEACHER: '新建资源',
    STUDENT: '加入班级',
    ADMIN: '新增用户',
  }[role];

  const content = useMemo(() => {
    if (role === 'TEACHER') return <TeacherPage pathname={safePathname} />;
    if (role === 'STUDENT') return <StudentPage pathname={safePathname} />;
    return <AdminPage pathname={safePathname} />;
  }, [role, safePathname]);

  return (
    <ProLayout
      title="智教实训"
      logo={<LogoMark />}
      fixedHeader
      fixSiderbar
      layout="mix"
      siderWidth={224}
      route={{
        path: '/',
        routes: routes.map((route) => ({
          path: route.path,
          name: route.name,
          icon: route.icon,
        })),
      }}
      location={{ pathname: safePathname }}
      menuItemRender={(item, dom) => (
        <button
          className="menu-link"
          type="button"
          onClick={() => {
            if (item.path) onNavigate(String(item.path));
          }}
        >
          {dom}
        </button>
      )}
      token={{
        header: {
          colorBgHeader: '#ffffff',
          colorHeaderTitle: '#172033',
        },
        sider: {
          colorMenuBackground: '#ffffff',
          colorTextMenu: '#3f4a5f',
          colorTextMenuSelected: '#1677ff',
        },
      }}
      actionsRender={() => [
        <Tag color="green" key="data-service">
          数据服务
        </Tag>,
        <Button icon={<HomeOutlined />} key="home" onClick={() => onNavigate('/')}>
          首页
        </Button>,
      ]}
      avatarProps={{
        src: undefined,
        icon: <UserOutlined />,
        title: `${session.name} · ${roleText[role]}`,
      }}
      menuFooterRender={() => (
        <Button className="sider-logout" icon={<LogoutOutlined />} onClick={onSignOut}>
          退出登录
        </Button>
      )}
      pageTitleRender={false}
    >
      <PageContainer
        className="workspace-page"
        title={activeRoute.name}
        content={<span>当前只搭建模块骨架，业务数据由后端接口提供。</span>}
        extra={
          <Button icon={<PlusOutlined />} type="primary" onClick={pendingAction}>
            {primaryAction}
          </Button>
        }
      >
        {content}
      </PageContainer>
    </ProLayout>
  );
}

function TeacherPage({ pathname }: { pathname: string }) {
  if (pathname.endsWith('/agents')) return <TeacherAgents />;
  if (pathname.endsWith('/courses')) return <TeacherCourses />;
  if (pathname.endsWith('/knowledge')) return <TeacherKnowledge />;
  if (pathname.endsWith('/templates')) return <TemplateCenter />;
  if (pathname.endsWith('/workflows')) return <TeacherWorkflowConfig />;
  if (pathname.endsWith('/products')) return <TeacherProducts />;
  if (pathname.endsWith('/attractions')) return <TeacherAttractionSpeeches />;
  if (pathname.endsWith('/customer-profiles')) return <TeacherCustomerProfiles />;
  if (pathname.endsWith('/scenarios')) return <TeacherTrainingScenarios />;
  if (pathname.endsWith('/classes')) return <TeacherClasses />;
  if (pathname.endsWith('/interactions')) return <TeacherInteractions />;
  if (pathname.endsWith('/tasks')) return <TeacherTasks />;
  return <TeacherOverview />;
}

function TeacherOverview() {
  const { data, loading } = useAsyncData<DashboardStats>(api.getDashboardStats, {
    teacherStats: [],
    adminStats: [],
  });
  const { data: conversations, loading: conversationsLoading } = useAsyncData<ConversationRow[]>(
    api.getConversations,
    [],
  );
  const { data: classes, loading: classesLoading } = useAsyncData<ClassRow[]>(api.getClasses, []);
  const stats = data.teacherStats;

  return (
    <Space direction="vertical" size={16} className="full-width">
      <Row gutter={[16, 16]}>
        {stats.map((item) => (
          <Col xs={24} sm={12} xl={6} key={item.label}>
            <Card className={`stat-card tone-${item.tone}`} loading={loading}>
              <Statistic title={item.label} value={item.value} />
              <Text type="secondary">{item.trend}</Text>
            </Card>
          </Col>
        ))}
      </Row>
      <Row gutter={[16, 16]}>
        <Col xs={24} xl={15}>
          <Card title="今日班级互动">
            <List
              loading={conversationsLoading}
              dataSource={conversations}
              locale={{ emptyText: <EmptyModule title="暂无互动记录" /> }}
              renderItem={(item) => (
                <List.Item actions={[<Tag key="status">{item.status}</Tag>]}>
                  <List.Item.Meta
                    avatar={<Avatar icon={<UserOutlined />} />}
                    title={`${item.studentName} · ${item.topic}`}
                    description={`${item.className} · ${item.updatedAt}`}
                  />
                </List.Item>
              )}
            />
          </Card>
        </Col>
        <Col xs={24} xl={9}>
          <Card title="教学闭环进度">
            <Timeline
              items={[
                { color: 'blue', children: '等待智能体发布数据' },
                { color: 'green', children: '等待知识库同步数据' },
                { color: 'orange', children: '等待任务提交数据' },
                { color: 'gray', children: 'Dify 接入层待联调' },
              ]}
            />
          </Card>
        </Col>
      </Row>
      <Card title="活跃班级">
        {classes.length === 0 && !classesLoading ? (
          <EmptyModule title="暂无班级数据" />
        ) : (
          <Row gutter={[16, 16]}>
            {classes.map((item) => (
              <Col xs={24} md={8} key={item.id}>
                <div className="class-tile">
                  <Space direction="vertical" size={6}>
                    <Tag>{item.status}</Tag>
                    <Title level={4}>{item.name}</Title>
                    <Text type="secondary">{item.courseName}</Text>
                    <Text strong>班级码 {item.code}</Text>
                    <Text>
                      {item.studentCount} 名学生 · {item.updatedAt}
                    </Text>
                  </Space>
                </div>
              </Col>
            ))}
          </Row>
        )}
      </Card>
    </Space>
  );
}

function TeacherAgents() {
  const { data: agents, loading } = useAsyncData<AgentRow[]>(api.getAgents, []);

  return (
    <Card
      title="课程智能体"
      extra={
        <Button icon={<PlusOutlined />} type="primary" onClick={pendingAction}>
          新建智能体
        </Button>
      }
    >
      {agents.length === 0 && !loading ? (
        <EmptyModule title="暂无智能体" description="后端接入后，教师创建的课程智能体会显示在这里。" />
      ) : (
        <Row gutter={[16, 16]}>
          {agents.map((agent) => (
            <Col xs={24} lg={8} key={agent.id}>
              <Card
                className="agent-card"
                title={
                  <Space>
                    <Avatar className="agent-avatar" icon={<RobotOutlined />} />
                    <span>{agent.name}</span>
                  </Space>
                }
                extra={<Tag>{agent.status}</Tag>}
              >
                <Space direction="vertical" size={10}>
                  <Text type="secondary">{agent.courseName}</Text>
                  <Text>{agent.knowledgeCount} 个知识库</Text>
                  <Text>{agent.workflowId}</Text>
                  <Divider />
                  <Text>{agent.usageCount} 次互动</Text>
                </Space>
              </Card>
            </Col>
          ))}
        </Row>
      )}
    </Card>
  );
}

function TeacherCourses() {
  const { data: courses, loading } = useAsyncData<CourseRow[]>(api.getCourses, []);
  const columns: ColumnsType<CourseRow> = [
    { title: '课程名称', dataIndex: 'title' },
    { title: '状态', dataIndex: 'status', render: (value) => <Tag>{value}</Tag> },
    { title: '实训目标', dataIndex: 'target' },
    { title: '班级数', dataIndex: 'classCount' },
    { title: '智能体数', dataIndex: 'agentCount' },
    { title: '更新时间', dataIndex: 'updatedAt' },
  ];

  return (
    <Card
      title="课程管理"
      extra={
        <Button icon={<PlusOutlined />} type="primary" onClick={pendingAction}>
          新建课程
        </Button>
      }
    >
      <Table
        rowKey="id"
        columns={columns}
        dataSource={courses}
        loading={loading}
        pagination={false}
        scroll={{ x: 920 }}
        locale={{ emptyText: <EmptyModule title="暂无课程" description="新建课程后可绑定智能体、知识库、班级和实训任务。" /> }}
      />
    </Card>
  );
}

function TeacherKnowledge() {
  const { data: knowledgeFiles, loading } = useAsyncData<KnowledgeFileRow[]>(api.getKnowledgeFiles, []);
  const columns: ColumnsType<KnowledgeFileRow> = [
    { title: '文件', dataIndex: 'name' },
    { title: '知识库', dataIndex: 'baseName' },
    {
      title: '状态',
      dataIndex: 'status',
      render: (value) => <Tag>{value}</Tag>,
    },
    { title: '大小', dataIndex: 'size' },
    { title: '更新时间', dataIndex: 'updatedAt' },
  ];

  return (
    <Space direction="vertical" size={16} className="full-width">
      <Dragger className="knowledge-uploader" beforeUpload={() => false} multiple onChange={pendingAction}>
        <p className="ant-upload-drag-icon">
          <CloudUploadOutlined />
        </p>
        <p className="ant-upload-text">拖拽教学资料到此处</p>
        <p className="ant-upload-hint">PDF、Word、Excel、PPT 等资料后续将接入后端上传与 Dify 数据集同步。</p>
      </Dragger>
      <Card title="资料解析状态">
        <Table
          rowKey="id"
          columns={columns}
          dataSource={knowledgeFiles}
          loading={loading}
          pagination={false}
          scroll={{ x: 860 }}
          locale={{ emptyText: <EmptyModule title="暂无知识库文件" description="上传接口接入后，文件解析状态会显示在这里。" /> }}
        />
      </Card>
    </Space>
  );
}

function TeacherClasses() {
  const { data: classes, loading } = useAsyncData<ClassRow[]>(api.getClasses, []);
  const columns: ColumnsType<ClassRow> = [
    { title: '班级', dataIndex: 'name' },
    { title: '班级码', dataIndex: 'code', render: (value) => <Text strong>{value}</Text> },
    { title: '绑定课程', dataIndex: 'courseName' },
    { title: '学生数', dataIndex: 'studentCount' },
    { title: '状态', dataIndex: 'status', render: (value) => <Tag>{value}</Tag> },
    { title: '更新时间', dataIndex: 'updatedAt' },
  ];

  return (
    <Card
      title="班级列表"
      extra={
        <Button icon={<PlusOutlined />} type="primary" onClick={pendingAction}>
          创建班级
        </Button>
      }
    >
      <Table
        rowKey="id"
        columns={columns}
        dataSource={classes}
        loading={loading}
        pagination={false}
        scroll={{ x: 860 }}
        locale={{ emptyText: <EmptyModule title="暂无班级" description="班级创建后可生成班级码供学生加入。" /> }}
      />
    </Card>
  );
}

function TeacherInteractions() {
  const { data: conversations, loading } = useAsyncData<ConversationRow[]>(api.getConversations, []);

  return (
    <Row gutter={[16, 16]}>
      <Col xs={24} lg={8}>
        <Card title="会话列表" className="conversation-list">
          <List
            loading={loading}
            dataSource={conversations}
            locale={{ emptyText: <EmptyModule title="暂无会话" description="学生开始 AI 实训后，会话记录会显示在这里。" /> }}
            renderItem={(item) => (
              <List.Item>
                <List.Item.Meta
                  avatar={<Avatar icon={<UserOutlined />} />}
                  title={item.studentName}
                  description={`${item.topic} · ${item.updatedAt}`}
                />
              </List.Item>
            )}
          />
        </Card>
      </Col>
      <Col xs={24} lg={16}>
        <Card title="会话详情" extra={<Tag>未选择会话</Tag>}>
          <div className="chat-window">
            <div className="empty-chat">选择左侧会话后查看完整互动记录。</div>
          </div>
          <Divider />
          <Input.TextArea rows={3} placeholder="教师干预回复或置顶提示" />
          <div className="submit-row">
            <Button icon={<SendOutlined />} type="primary" onClick={pendingAction}>
              发送干预
            </Button>
          </div>
        </Card>
      </Col>
    </Row>
  );
}

function TeacherTasks() {
  const { data: taskRows, loading } = useAsyncData<TaskRow[]>(api.getTasks, []);
  const columns: ColumnsType<TaskRow> = [
    { title: '任务', dataIndex: 'title' },
    { title: '课程', dataIndex: 'courseName' },
    { title: '班级', dataIndex: 'className' },
    { title: '截止时间', dataIndex: 'dueAt' },
    { title: '提交数', dataIndex: 'submittedCount' },
    { title: '状态', dataIndex: 'status', render: (value) => <Tag>{value}</Tag> },
  ];

  return (
    <Card
      title="实训任务与反馈"
      extra={
        <Button icon={<PlusOutlined />} type="primary" onClick={pendingAction}>
          发布任务
        </Button>
      }
    >
      <Table
        rowKey="id"
        columns={columns}
        dataSource={taskRows}
        loading={loading}
        pagination={false}
        scroll={{ x: 900 }}
        locale={{ emptyText: <EmptyModule title="暂无实训任务" description="发布任务后可跟踪学生提交和教师反馈。" /> }}
      />
    </Card>
  );
}

function TemplateCenter() {
  const { data: templates, loading } = useAsyncData<TemplateRow[]>(api.getTemplates, []);
  const columns: ColumnsType<TemplateRow> = [
    { title: '模板名称', dataIndex: 'name' },
    { title: '行业方向', dataIndex: 'industry' },
    { title: '说明', dataIndex: 'description' },
    { title: '模块数', dataIndex: 'moduleCount' },
    { title: '智能体数', dataIndex: 'agentCount' },
    { title: '状态', dataIndex: 'status', render: (value) => <Tag>{value}</Tag> },
  ];

  return (
    <Space direction="vertical" size={16} className="full-width">
      <Card title="模板配置入口">
        <Row gutter={[16, 16]}>
          <Col xs={24} lg={8}>
            <div className="module-shell">
              <AppstoreOutlined />
              <Title level={4}>通用课程模板</Title>
              <Text type="secondary">沉淀课程结构、智能体类型、知识库字段和实训任务类型。</Text>
            </div>
          </Col>
          <Col xs={24} lg={8}>
            <div className="module-shell">
              <ExperimentOutlined />
              <Title level={4}>旅游销售模板</Title>
              <Text type="secondary">首个示范模板，覆盖客户模拟、AI 参谋、产品变更和路线体验。</Text>
            </div>
          </Col>
          <Col xs={24} lg={8}>
            <div className="module-shell">
              <ApiOutlined />
              <Title level={4}>Dify 工作流绑定</Title>
              <Text type="secondary">后端接入后按模板绑定 Workflow ID、变量和知识库关系。</Text>
            </div>
          </Col>
        </Row>
      </Card>
      <Card
        title="模板列表"
        extra={
          <Button icon={<PlusOutlined />} type="primary" onClick={pendingAction}>
            新建模板
          </Button>
        }
      >
        <Table
          rowKey="id"
          columns={columns}
          dataSource={templates}
          loading={loading}
          pagination={false}
          scroll={{ x: 960 }}
          locale={{ emptyText: <EmptyModule title="暂无模板" description="模板接口接入后可在这里维护行业训练模板。" /> }}
        />
      </Card>
    </Space>
  );
}

function TeacherWorkflowConfig() {
  const { data: workflows, loading } = useAsyncData<WorkflowConfigRow[]>(api.getWorkflowConfigs, []);
  const columns: ColumnsType<WorkflowConfigRow> = [
    { title: '工作流', dataIndex: 'name' },
    { title: 'Key', dataIndex: 'workflowKey' },
    { title: 'Workflow ID', dataIndex: 'workflowId' },
    { title: '变量数', dataIndex: 'variableCount' },
    { title: '状态', dataIndex: 'status', render: (value) => <Tag>{value}</Tag> },
    { title: '更新时间', dataIndex: 'updatedAt' },
  ];

  return (
    <Space direction="vertical" size={16} className="full-width">
      <Card title="Dify Workflow ID 与变量映射">
        <Row gutter={[16, 16]}>
          {workflowDefinitions.map((workflow) => (
            <Col xs={24} xl={12} key={workflow.key}>
              <div className="workflow-config-card">
                <Space direction="vertical" size={12} className="full-width">
                  <Space align="start" className="workflow-config-head">
                    <Avatar className="admin-icon" icon={<ApiOutlined />} />
                    <div>
                      <Title level={4}>{workflow.name}</Title>
                      <Text type="secondary">{workflow.scene}</Text>
                    </div>
                  </Space>
                  <Form layout="vertical">
                    <Form.Item label="Dify Workflow ID">
                      <Input placeholder="由后端保存 Workflow ID，前端只提交配置字段" />
                    </Form.Item>
                    <Form.Item label="启用状态">
                      <Segmented block options={['停用', '启用']} />
                    </Form.Item>
                  </Form>
                  <FieldTags fields={workflow.variables} />
                  <Button icon={<CheckCircleOutlined />} onClick={pendingAction}>
                    保存映射
                  </Button>
                </Space>
              </div>
            </Col>
          ))}
        </Row>
      </Card>
      <Card
        title="已保存配置"
        extra={
          <Button icon={<PlusOutlined />} type="primary" onClick={pendingAction}>
            新增工作流配置
          </Button>
        }
      >
        <Table
          rowKey="id"
          columns={columns}
          dataSource={workflows}
          loading={loading}
          pagination={false}
          scroll={{ x: 980 }}
          locale={{
            emptyText: (
              <EmptyModule title="暂无工作流配置" description="后端保存 Workflow ID、变量映射和启用状态后会显示在这里。" />
            ),
          }}
        />
      </Card>
    </Space>
  );
}

function TeacherProducts() {
  const { data: products, loading } = useAsyncData<ProductRow[]>(api.getProducts, []);
  const columns: ColumnsType<ProductRow> = [
    { title: '产品名称', dataIndex: 'name' },
    { title: '路线名称', dataIndex: 'routeName' },
    { title: '上下文状态', dataIndex: 'contextStatus', render: (value) => <Tag>{value}</Tag> },
    { title: '状态', dataIndex: 'status', render: (value) => <Tag>{value}</Tag> },
    { title: '更新时间', dataIndex: 'updatedAt' },
  ];

  return (
    <Space direction="vertical" size={16} className="full-width">
      <Card title="产品上下文录入">
        <Form layout="vertical">
          <Row gutter={16}>
            <Col xs={24} lg={12}>
              <Form.Item label="产品名称">
                <Input placeholder="用于 product_name、product_options、product_context" />
              </Form.Item>
            </Col>
            <Col xs={24} lg={12}>
              <Form.Item label="路线名称">
                <Input placeholder="用于 route_name 与 itinerary 关联" />
              </Form.Item>
            </Col>
            <Col xs={24} lg={12}>
              <Form.Item label="旧版产品信息">
                <Input.TextArea rows={4} placeholder="对应 old_product_info" />
              </Form.Item>
            </Col>
            <Col xs={24} lg={12}>
              <Form.Item label="新版产品信息">
                <Input.TextArea rows={4} placeholder="对应 new_product_info" />
              </Form.Item>
            </Col>
            <Col xs={24}>
              <Form.Item label="产品上下文">
                <Input.TextArea rows={5} placeholder="对应 product_context，可由后端从产品库、行程和知识库汇总" />
              </Form.Item>
            </Col>
          </Row>
          <Space wrap>
            <Button icon={<CheckCircleOutlined />} type="primary" onClick={pendingAction}>
              保存产品
            </Button>
            <Button icon={<AuditOutlined />} onClick={pendingAction}>
              触发变更提醒
            </Button>
          </Space>
        </Form>
      </Card>
      <Card
        title="旅游产品库"
        extra={
          <Button icon={<PlusOutlined />} type="primary" onClick={pendingAction}>
            新增产品
          </Button>
        }
      >
        <Table
          rowKey="id"
          columns={columns}
          dataSource={products}
          loading={loading}
          pagination={false}
          scroll={{ x: 860 }}
          locale={{ emptyText: <EmptyModule title="暂无旅游产品" description="产品接口接入后会为产品变更、AI 参谋和路线模拟提供上下文。" /> }}
        />
      </Card>
    </Space>
  );
}

function TeacherAttractionSpeeches() {
  const { data: speeches, loading } = useAsyncData<AttractionSpeechRow[]>(api.getAttractionSpeeches, []);
  const columns: ColumnsType<AttractionSpeechRow> = [
    { title: '景点名称', dataIndex: 'attractionName' },
    { title: '地理类型', dataIndex: 'geographyType' },
    { title: '标签状态', dataIndex: 'tagStatus', render: (value) => <Tag>{value}</Tag> },
    { title: '话术状态', dataIndex: 'speechStatus', render: (value) => <Tag>{value}</Tag> },
    { title: '更新时间', dataIndex: 'updatedAt' },
  ];

  return (
    <Space direction="vertical" size={16} className="full-width">
      <Card title="景点话术生成配置">
        <Form layout="vertical">
          <Row gutter={16}>
            <Col xs={24} lg={12}>
              <Form.Item label="景点名称">
                <Input placeholder="对应 attraction_name" />
              </Form.Item>
            </Col>
            <Col xs={24} lg={12}>
              <Form.Item label="地理类型">
                <Input placeholder="对应 geography_type" />
              </Form.Item>
            </Col>
            <Col xs={24} lg={12}>
              <Form.Item label="历史人文标签">
                <Input placeholder="对应 culture_tags" />
              </Form.Item>
            </Col>
            <Col xs={24} lg={12}>
              <Form.Item label="客户旅游经验度">
                <Input placeholder="对应 customer_experience_level" />
              </Form.Item>
            </Col>
            <Col xs={24} lg={12}>
              <Form.Item label="客户场景">
                <Input.TextArea rows={4} placeholder="对应 customer_scene" />
              </Form.Item>
            </Col>
            <Col xs={24} lg={12}>
              <Form.Item label="销售目标">
                <Input.TextArea rows={4} placeholder="对应 selling_goal" />
              </Form.Item>
            </Col>
            <Col xs={24}>
              <Form.Item label="景点基础信息 / 课程上下文">
                <Input.TextArea rows={5} placeholder="对应 attraction_basic_info 与 course_context" />
              </Form.Item>
            </Col>
          </Row>
          <Button icon={<SendOutlined />} type="primary" onClick={pendingAction}>
            生成景点话术
          </Button>
        </Form>
      </Card>
      <Card
        title="景点话术库"
        extra={
          <Button icon={<PlusOutlined />} type="primary" onClick={pendingAction}>
            新增景点
          </Button>
        }
      >
        <Table
          rowKey="id"
          columns={columns}
          dataSource={speeches}
          loading={loading}
          pagination={false}
          scroll={{ x: 860 }}
          locale={{ emptyText: <EmptyModule title="暂无景点话术" description="接入 04-attraction-speech 工作流后，生成结果会沉淀在这里。" /> }}
        />
      </Card>
    </Space>
  );
}

function TeacherCustomerProfiles() {
  const { data: profiles, loading } = useAsyncData<CustomerProfileRow[]>(api.getCustomerProfiles, []);
  const columns: ColumnsType<CustomerProfileRow> = [
    { title: '画像名称', dataIndex: 'name' },
    { title: '画像类型', dataIndex: 'profileType' },
    { title: '共用工作流', dataIndex: 'sharedBy' },
    { title: '状态', dataIndex: 'status', render: (value) => <Tag>{value}</Tag> },
    { title: '更新时间', dataIndex: 'updatedAt' },
  ];

  return (
    <Space direction="vertical" size={16} className="full-width">
      <Card title="客户画像维护">
        <Form layout="vertical">
          <Row gutter={16}>
            <Col xs={24} lg={12}>
              <Form.Item label="画像名称">
                <Input placeholder="用于 customer_profile 引用" />
              </Form.Item>
            </Col>
            <Col xs={24} lg={12}>
              <Form.Item label="画像类型">
                <Input placeholder="如：预算敏感、需求模糊、家庭出游等，由后端字典维护" />
              </Form.Item>
            </Col>
            <Col xs={24}>
              <Form.Item label="画像内容">
                <Input.TextArea rows={6} placeholder="对应 customer_profile，供模拟客户、AI 参谋和旅行模拟共用" />
              </Form.Item>
            </Col>
          </Row>
          <Button icon={<CheckCircleOutlined />} type="primary" onClick={pendingAction}>
            保存画像
          </Button>
        </Form>
      </Card>
      <Card
        title="客户画像库"
        extra={
          <Button icon={<PlusOutlined />} type="primary" onClick={pendingAction}>
            新增画像
          </Button>
        }
      >
        <Table
          rowKey="id"
          columns={columns}
          dataSource={profiles}
          loading={loading}
          pagination={false}
          scroll={{ x: 900 }}
          locale={{ emptyText: <EmptyModule title="暂无客户画像" description="画像保存后可被客户模拟、AI 参谋和旅行模拟复用。" /> }}
        />
      </Card>
    </Space>
  );
}

function TeacherTrainingScenarios() {
  const { data: scenarios, loading } = useAsyncData<TrainingScenarioRow[]>(api.getTrainingScenarios, []);
  const columns: ColumnsType<TrainingScenarioRow> = [
    { title: '场景名称', dataIndex: 'name' },
    { title: '难度', dataIndex: 'difficulty' },
    { title: '辅导模式', dataIndex: 'coachingMode' },
    { title: '训练目标', dataIndex: 'trainingGoal' },
    { title: '状态', dataIndex: 'status', render: (value) => <Tag>{value}</Tag> },
    { title: '更新时间', dataIndex: 'updatedAt' },
  ];

  return (
    <Space direction="vertical" size={16} className="full-width">
      <Card title="训练场景配置">
        <Form layout="vertical">
          <Row gutter={16}>
            <Col xs={24} lg={12}>
              <Form.Item label="训练场景">
                <Input placeholder="对应 scenario" />
              </Form.Item>
            </Col>
            <Col xs={24} lg={12}>
              <Form.Item label="训练目标">
                <Input placeholder="对应 training_goal / learning_goal" />
              </Form.Item>
            </Col>
            <Col xs={24} lg={12}>
              <Form.Item label="难度">
                <Segmented block options={['入门', '进阶', '挑战']} />
              </Form.Item>
            </Col>
            <Col xs={24} lg={12}>
              <Form.Item label="辅导模式">
                <Segmented block options={['少提示', '即时提示', '复盘提示']} />
              </Form.Item>
            </Col>
            <Col xs={24}>
              <Form.Item label="场景说明">
                <Input.TextArea rows={5} placeholder="给 simulated-customer、advisor、travel-simulation 的场景上下文" />
              </Form.Item>
            </Col>
          </Row>
          <Button icon={<CheckCircleOutlined />} type="primary" onClick={pendingAction}>
            保存场景
          </Button>
        </Form>
      </Card>
      <Card
        title="训练场景库"
        extra={
          <Button icon={<PlusOutlined />} type="primary" onClick={pendingAction}>
            新增场景
          </Button>
        }
      >
        <Table
          rowKey="id"
          columns={columns}
          dataSource={scenarios}
          loading={loading}
          pagination={false}
          scroll={{ x: 940 }}
          locale={{ emptyText: <EmptyModule title="暂无训练场景" description="场景、难度、辅导模式和训练目标接入后会显示在这里。" /> }}
        />
      </Card>
    </Space>
  );
}

function StudentPage({ pathname }: { pathname: string }) {
  if (pathname.endsWith('/classes')) return <StudentClasses />;
  if (pathname.endsWith('/practice')) return <StudentPractice />;
  if (pathname.endsWith('/travel-practice')) return <TravelPractice />;
  if (pathname.endsWith('/course-qa')) return <StudentPractice />;
  if (pathname.endsWith('/customer-practice')) return <StudentCustomerPractice />;
  if (pathname.endsWith('/advisor')) return <StudentAdvisorPractice />;
  if (pathname.endsWith('/attraction-speech')) return <StudentAttractionSpeech />;
  if (pathname.endsWith('/product-changes')) return <StudentProductChanges />;
  if (pathname.endsWith('/travel-simulation')) return <StudentTravelSimulation />;
  if (pathname.endsWith('/tasks')) return <StudentTasks />;
  return <StudentOverview />;
}

function StudentOverview() {
  return (
    <Space direction="vertical" size={16} className="full-width">
      <Card className="join-card">
        <Row gutter={[16, 16]} align="middle">
          <Col xs={24} lg={12}>
            <Title level={3}>通过班级码加入课堂</Title>
            <Text type="secondary">输入教师提供的班级码后，即可进入对应 AI 实训空间。</Text>
          </Col>
          <Col xs={24} lg={12}>
            <Space.Compact className="join-input">
              <Input size="large" placeholder="请输入班级码" />
              <Button icon={<CheckCircleOutlined />} size="large" type="primary" onClick={pendingAction}>
                加入
              </Button>
            </Space.Compact>
          </Col>
        </Row>
      </Card>
      <StudentClasses />
    </Space>
  );
}

function StudentClasses() {
  const { data: studentClasses, loading } = useAsyncData<StudentClassRow[]>(api.getStudentClasses, []);

  return (
    <Card title="我的班级">
      {studentClasses.length === 0 && !loading ? (
        <EmptyModule title="暂无已加入班级" description="输入教师提供的班级码后，课程和智能体入口会显示在这里。" />
      ) : (
        <Row gutter={[16, 16]}>
          {studentClasses.map((item) => (
            <Col xs={24} lg={12} key={item.id}>
              <Card title={item.name} extra={<Tag>{item.status}</Tag>}>
                <Space direction="vertical" size={10} className="full-width">
                  <Text>
                    {item.teacherName} · {item.courseName}
                  </Text>
                  <Text type="secondary">{item.agentName}</Text>
                  <Button icon={<CommentOutlined />} type="primary" onClick={pendingAction}>
                    进入实训对话
                  </Button>
                </Space>
              </Card>
            </Col>
          ))}
        </Row>
      )}
    </Card>
  );
}

function StudentPractice() {
  return (
    <Row gutter={[16, 16]}>
      <Col xs={24} lg={7}>
        <Card title="当前任务">
          <Tag>未选择任务</Tag>
          <Title level={4}>当前暂无任务</Title>
          <Paragraph>加入班级后，可在这里查看教师发布的训练任务。</Paragraph>
          <Progress percent={0} />
        </Card>
      </Col>
      <Col xs={24} lg={17}>
        <Card title="AI 实训对话" extra={<Tag>等待连接</Tag>}>
          <div className="chat-window student-practice">
            <div className="empty-chat">加入班级并选择任务后开始对话。</div>
          </div>
          <Space.Compact className="chat-input">
            <Input placeholder="输入你的回复" />
            <Button icon={<SendOutlined />} type="primary" onClick={pendingAction}>
              发送
            </Button>
          </Space.Compact>
        </Card>
      </Col>
    </Row>
  );
}

function TravelPractice() {
  return (
    <Space direction="vertical" size={16} className="full-width">
      <Card title="旅游销售模板对练">
        <Row gutter={[16, 16]}>
          <Col xs={24} lg={8}>
            <div className="module-shell">
              <UserOutlined />
              <Title level={4}>AI 模拟客户</Title>
              <Text type="secondary">后端接入后按客户画像生成犹豫、预算敏感、需求模糊等状态。</Text>
            </div>
          </Col>
          <Col xs={24} lg={8}>
            <div className="module-shell">
              <SolutionOutlined />
              <Title level={4}>AI 参谋</Title>
              <Text type="secondary">根据预算、同行人、偏好和风险判断跟团、定制或自由行组合。</Text>
            </div>
          </Col>
          <Col xs={24} lg={8}>
            <div className="module-shell">
              <BankOutlined />
              <Title level={4}>轻量路线模拟</Title>
              <Text type="secondary">用行程节点、景点卡片、体验标签和客户反馈辅助理解产品体验。</Text>
            </div>
          </Col>
        </Row>
      </Card>
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={7}>
          <Card title="训练配置">
            <Form layout="vertical">
              <Form.Item label="客户类型">
                <Input placeholder="如：亲子游、银发游、毕业旅行" />
              </Form.Item>
              <Form.Item label="目的地或产品">
                <Input placeholder="如：西安亲子 4 日游" />
              </Form.Item>
              <Form.Item label="训练目标">
                <Input.TextArea rows={4} placeholder="如：需求挖掘、异议处理、产品推荐" />
              </Form.Item>
              <Button block icon={<CheckCircleOutlined />} type="primary" onClick={pendingAction}>
                创建训练场景
              </Button>
            </Form>
          </Card>
        </Col>
        <Col xs={24} lg={17}>
          <Card title="对练窗口" extra={<Tag>等待 Dify 工作流接入</Tag>}>
            <div className="chat-window student-practice">
              <div className="empty-chat">创建训练场景后，AI 客户对话和参谋建议会显示在这里。</div>
            </div>
            <Space.Compact className="chat-input">
              <Input placeholder="输入你的销售回复" />
              <Button icon={<SendOutlined />} type="primary" onClick={pendingAction}>
                发送
              </Button>
            </Space.Compact>
          </Card>
        </Col>
      </Row>
    </Space>
  );
}

function StudentTasks() {
  const { data: studentTasks, loading } = useAsyncData<StudentTaskRow[]>(api.getStudentTasks, []);

  return (
    <Card title="我的任务与反馈">
      <List
        loading={loading}
        dataSource={studentTasks}
        locale={{ emptyText: <EmptyModule title="暂无任务" description="教师发布任务后，会在这里显示截止时间和反馈状态。" /> }}
        renderItem={(item) => (
          <List.Item actions={[<Tag key="status">{item.status}</Tag>]}>
            <List.Item.Meta
              avatar={<Avatar icon={<FileDoneOutlined />} />}
              title={item.title}
              description={`截止 ${item.dueAt} · ${item.feedbackStatus}`}
            />
          </List.Item>
        )}
      />
    </Card>
  );
}

function AdminPage({ pathname }: { pathname: string }) {
  if (pathname.endsWith('/users')) return <AdminUsers />;
  if (pathname.endsWith('/system')) return <AdminSystem />;
  if (pathname.endsWith('/audit')) return <AdminAudit />;
  return <AdminOverview />;
}

function AdminOverview() {
  const { data, loading } = useAsyncData<DashboardStats>(api.getDashboardStats, {
    teacherStats: [],
    adminStats: [],
  });
  const iconMap = {
    user: <UserOutlined />,
    team: <TeamOutlined />,
    api: <ApiOutlined />,
    security: <SafetyCertificateOutlined />,
  };

  return (
    <Row gutter={[16, 16]}>
      {data.adminStats.map((item) => (
        <Col xs={24} sm={12} xl={6} key={item.label}>
          <Card className="admin-stat" loading={loading}>
            <Space>
              <Avatar className="admin-icon" icon={iconMap[item.icon]} />
              <Statistic title={item.label} value={item.value} />
            </Space>
          </Card>
        </Col>
      ))}
    </Row>
  );
}

function AdminUsers() {
  return (
    <Card title="用户与角色">
      <Row gutter={[16, 16]}>
        {[
          ['管理员', '系统配置、审计、账号管理', '0 人'],
          ['教师', '智能体、课程、班级、任务管理', '0 人'],
          ['学生', '加入班级、参与实训、查看反馈', '0 人'],
        ].map(([title, desc, count]) => (
          <Col xs={24} lg={8} key={title}>
            <div className="role-box">
              <Title level={4}>{title}</Title>
              <Text type="secondary">{desc}</Text>
              <Text strong>{count}</Text>
            </div>
          </Col>
        ))}
      </Row>
    </Card>
  );
}

function AdminSystem() {
  return (
    <Row gutter={[16, 16]}>
      <Col xs={24} lg={12}>
        <Card title="Dify 接入配置">
          <Form layout="vertical">
            <Form.Item label="API Base URL">
              <Input placeholder="由后端服务保存，不在前端暴露密钥" />
            </Form.Item>
            <Form.Item label="工作流密钥引用">
              <Input.Password placeholder="如：dify-secret-ref" />
            </Form.Item>
            <Button icon={<SettingOutlined />} type="primary" onClick={pendingAction}>
              保存配置
            </Button>
          </Form>
        </Card>
      </Col>
      <Col xs={24} lg={12}>
        <Card title="平台策略">
          <Timeline
            items={[
              { color: 'blue', children: '学生只能访问已加入班级' },
              { color: 'green', children: '教师只能管理本人课程资源' },
              { color: 'orange', children: '敏感会话进入教师关注列表' },
            ]}
          />
        </Card>
      </Col>
    </Row>
  );
}

function AdminAudit() {
  const { data: auditRows, loading } = useAsyncData<AuditRow[]>(api.getAuditLogs, []);
  const columns: ColumnsType<AuditRow> = [
    { title: '动作', dataIndex: 'action' },
    { title: '操作者', dataIndex: 'actor' },
    { title: '对象', dataIndex: 'target' },
    { title: '时间', dataIndex: 'createdAt' },
  ];

  return (
    <Card title="审计日志">
      <Table
        rowKey="id"
        columns={columns}
        dataSource={auditRows}
        loading={loading}
        pagination={false}
        scroll={{ x: 720 }}
        locale={{ emptyText: <EmptyModule title="暂无审计日志" description="后端记录用户、资源和系统配置操作后会显示在这里。" /> }}
      />
    </Card>
  );
}

export default App;
