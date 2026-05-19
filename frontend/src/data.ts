import type { ReactNode } from 'react';

export type Role = 'TEACHER' | 'STUDENT' | 'ADMIN';

export type RouteItem = {
  path: string;
  name: string;
  icon: ReactNode;
};

export type TeacherStat = {
  label: string;
  value: string;
  trend: string;
  tone: string;
};

export type AgentRow = {
  id: string;
  name: string;
  courseName: string;
  status: string;
  knowledgeCount: number;
  workflowId: string;
  usageCount: number;
};

export type CourseRow = {
  id: string;
  title: string;
  status: string;
  target: string;
  classCount: number;
  agentCount: number;
  updatedAt: string;
};

export type KnowledgeFileRow = {
  id: string;
  name: string;
  baseName: string;
  status: string;
  size: string;
  updatedAt: string;
};

export type ClassRow = {
  id: string;
  name: string;
  code: string;
  courseName: string;
  studentCount: number;
  status: string;
  updatedAt: string;
};

export type ConversationRow = {
  id: string;
  studentName: string;
  className: string;
  topic: string;
  status: string;
  updatedAt: string;
};

export type TaskRow = {
  id: string;
  title: string;
  courseName: string;
  className: string;
  dueAt: string;
  submittedCount: number;
  status: string;
};

export type StudentClassRow = {
  id: string;
  name: string;
  teacherName: string;
  courseName: string;
  agentName: string;
  status: string;
};

export type StudentTaskRow = {
  id: string;
  title: string;
  status: string;
  dueAt: string;
  feedbackStatus: string;
};

export type AuditRow = {
  id: string;
  action: string;
  actor: string;
  target: string;
  createdAt: string;
};

export type TemplateRow = {
  id: string;
  name: string;
  status: string;
  industry: string;
  description: string;
  moduleCount: number;
  agentCount: number;
};

export type WorkflowConfigRow = {
  id: string;
  name: string;
  workflowKey: string;
  workflowId: string;
  variableCount: number;
  status: string;
  updatedAt: string;
};

export type ProductRow = {
  id: string;
  name: string;
  routeName: string;
  status: string;
  contextStatus: string;
  updatedAt: string;
};

export type AttractionSpeechRow = {
  id: string;
  attractionName: string;
  geographyType: string;
  tagStatus: string;
  speechStatus: string;
  updatedAt: string;
};

export type CustomerProfileRow = {
  id: string;
  name: string;
  profileType: string;
  sharedBy: string;
  status: string;
  updatedAt: string;
};

export type TrainingScenarioRow = {
  id: string;
  name: string;
  difficulty: string;
  coachingMode: string;
  trainingGoal: string;
  status: string;
  updatedAt: string;
};

export type ModelConfigRow = {
  id: string;
  provider: string;
  modelName: string;
  credentialRef: string;
  status: string;
  updatedAt: string;
};

export type CallLogRow = {
  id: string;
  workflowName: string;
  caller: string;
  status: string;
  latencyMs: number;
  createdAt: string;
};

export type JobRow = {
  id: string;
  jobType: string;
  target: string;
  status: string;
  retryCount: number;
  updatedAt: string;
};

export type DashboardStats = {
  teacherStats: TeacherStat[];
  adminStats: Array<{
    label: string;
    value: string;
    icon: 'user' | 'team' | 'api' | 'security';
  }>;
};

export const roleText: Record<Role, string> = {
  TEACHER: '教师',
  STUDENT: '学生',
  ADMIN: '管理员',
};
