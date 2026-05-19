import type {
  AgentRow,
  AuditRow,
  ClassRow,
  CallLogRow,
  ConversationRow,
  CourseRow,
  DashboardStats,
  AttractionSpeechRow,
  CustomerProfileRow,
  JobRow,
  KnowledgeFileRow,
  ModelConfigRow,
  ProductRow,
  StudentClassRow,
  StudentTaskRow,
  TaskRow,
  TemplateRow,
  TrainingScenarioRow,
  WorkflowConfigRow,
} from '../data';

const emptyStats: DashboardStats = {
  teacherStats: [
    { label: '已发布智能体', value: '0', trend: '等待后端接入', tone: 'blue' },
    { label: '活跃班级', value: '0', trend: '等待后端接入', tone: 'green' },
    { label: '今日互动', value: '0', trend: '等待后端接入', tone: 'amber' },
    { label: '待批阅任务', value: '0', trend: '等待后端接入', tone: 'coral' },
  ],
  adminStats: [
    { label: '教师账号', value: '0', icon: 'user' },
    { label: '学生账号', value: '0', icon: 'team' },
    { label: 'Dify 工作流', value: '0', icon: 'api' },
    { label: '安全事件', value: '0', icon: 'security' },
  ],
};

function pendingList<T>(): Promise<T[]> {
  return Promise.resolve([]);
}

export const api = {
  getDashboardStats: () => Promise.resolve(emptyStats),
  getAgents: () => pendingList<AgentRow>(),
  getCourses: () => pendingList<CourseRow>(),
  getKnowledgeFiles: () => pendingList<KnowledgeFileRow>(),
  getClasses: () => pendingList<ClassRow>(),
  getConversations: () => pendingList<ConversationRow>(),
  getTasks: () => pendingList<TaskRow>(),
  getStudentClasses: () => pendingList<StudentClassRow>(),
  getStudentTasks: () => pendingList<StudentTaskRow>(),
  getTemplates: () => pendingList<TemplateRow>(),
  getAuditLogs: () => pendingList<AuditRow>(),
  getWorkflowConfigs: () => pendingList<WorkflowConfigRow>(),
  getProducts: () => pendingList<ProductRow>(),
  getAttractionSpeeches: () => pendingList<AttractionSpeechRow>(),
  getCustomerProfiles: () => pendingList<CustomerProfileRow>(),
  getTrainingScenarios: () => pendingList<TrainingScenarioRow>(),
  getModelConfigs: () => pendingList<ModelConfigRow>(),
  getCallLogs: () => pendingList<CallLogRow>(),
  getJobs: () => pendingList<JobRow>(),
};

export function notifyApiPending() {
  return '接口待接入';
}
