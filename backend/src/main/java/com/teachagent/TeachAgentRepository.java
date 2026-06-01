package com.teachagent;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class TeachAgentRepository {
  private static final List<String> ALLOWED_TABLES = List.of(
    "users", "courses", "agents", "agent_workflows", "classes", "class_members", "conversations", "messages", "workflow_configs",
    "products", "attraction_speeches", "customer_profiles", "training_scenarios"
  );

  private final JdbcTemplate jdbcTemplate;

  public TeachAgentRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Map<String, Object> getDashboardCounts() {
    Map<String, Object> counts = new LinkedHashMap<>();
    counts.put("publishedAgents", count("""
      SELECT COUNT(*)
      FROM agents
      WHERE status = 'PUBLISHED' AND deleted_at IS NULL
      """));
    counts.put("activeClasses", count("""
      SELECT COUNT(*)
      FROM classes
      WHERE status = 'ACTIVE' AND deleted_at IS NULL
      """));
    counts.put("todayInteractions", count("""
      SELECT COUNT(*)
      FROM messages
      WHERE deleted_at IS NULL AND DATE(created_at) = CURRENT_DATE()
      """));
    counts.put("pendingTasks", count("""
      SELECT COUNT(*)
      FROM interaction_tasks
      WHERE status = 'PUBLISHED' AND deleted_at IS NULL
      """));
    counts.put("teacherUsers", count("""
      SELECT COUNT(*)
      FROM users
      WHERE role = 'TEACHER' AND deleted_at IS NULL
      """));
    counts.put("studentUsers", count("""
      SELECT COUNT(*)
      FROM users
      WHERE role = 'STUDENT' AND deleted_at IS NULL
      """));
    counts.put("workflowConfigs", count("""
      SELECT COUNT(*)
      FROM workflow_configs
      WHERE deleted_at IS NULL
      """));
    counts.put("securityEvents", count("""
      SELECT COUNT(*)
      FROM audit_logs
      WHERE action LIKE 'SECURITY_%'
      """));
    return counts;
  }

  public List<Map<String, Object>> listUsers(String role) {
    if (role == null || role.isBlank()) {
      return jdbcTemplate.queryForList("""
        SELECT id, role, username, display_name, email, phone, status, last_login_at, created_at, updated_at
        FROM users
        WHERE deleted_at IS NULL
        ORDER BY id DESC
        LIMIT 200
        """);
    }

    return jdbcTemplate.queryForList("""
      SELECT id, role, username, display_name, email, phone, status, last_login_at, created_at, updated_at
      FROM users
      WHERE role = ? AND deleted_at IS NULL
      ORDER BY id DESC
      LIMIT 200
      """, role);
  }

  public List<Map<String, Object>> listCourses(long teacherId) {
    return jdbcTemplate.queryForList("""
      SELECT
        c.id,
        c.name,
        c.subject,
        c.grade_level,
        c.description,
        c.status,
        c.updated_at,
        COUNT(DISTINCT cls.id) AS class_count,
        COUNT(DISTINCT a.id) AS agent_count
      FROM courses c
      LEFT JOIN classes cls ON cls.course_id = c.id AND cls.deleted_at IS NULL
      LEFT JOIN agents a ON a.course_id = c.id AND a.deleted_at IS NULL
      WHERE c.teacher_id = ? AND c.deleted_at IS NULL
      GROUP BY c.id, c.name, c.subject, c.grade_level, c.description, c.status, c.updated_at
      ORDER BY c.updated_at DESC, c.id DESC
      LIMIT 200
      """, teacherId);
  }

  public List<Map<String, Object>> listAgents(long teacherId) {
    return jdbcTemplate.queryForList("""
      SELECT
        a.id,
        a.name,
        a.course_id,
        a.description,
        a.opening_message,
        a.dify_workflow_id,
        a.status,
        a.updated_at,
        COALESCE(c.name, '未绑定课程') AS course_name,
        COUNT(DISTINCT kb.id) AS knowledge_count,
        COUNT(DISTINCT cv.id) AS usage_count,
        COUNT(DISTINCT aw.id) AS workflow_count
      FROM agents a
      LEFT JOIN courses c ON c.id = a.course_id AND c.deleted_at IS NULL
      LEFT JOIN knowledge_bases kb ON kb.agent_id = a.id AND kb.deleted_at IS NULL
      LEFT JOIN conversations cv ON cv.agent_id = a.id AND cv.deleted_at IS NULL
      LEFT JOIN agent_workflows aw ON aw.agent_id = a.id AND aw.enabled = 1
      WHERE a.teacher_id = ? AND a.deleted_at IS NULL
      GROUP BY
        a.id, a.name, a.course_id, a.description, a.opening_message, a.dify_workflow_id,
        a.status, a.updated_at, c.name
      ORDER BY a.updated_at DESC, a.id DESC
      LIMIT 200
      """, teacherId);
  }

  public List<Map<String, Object>> listAgentWorkflows(long agentId) {
    return jdbcTemplate.queryForList("""
      SELECT
        aw.id,
        aw.agent_id,
        aw.workflow_key,
        aw.workflow_config_id,
        aw.custom_workflow_id,
        aw.custom_api_key_ciphertext,
        aw.custom_api_key_last4,
        aw.enabled,
        aw.sort_order,
        wc.name AS config_name,
        wc.workflow_id AS config_workflow_id,
        wc.api_key_ciphertext AS config_api_key_ciphertext,
        wc.api_key_last4 AS config_api_key_last4,
        wc.base_url AS config_base_url,
        wc.enabled AS config_enabled
      FROM agent_workflows aw
      LEFT JOIN workflow_configs wc ON wc.id = aw.workflow_config_id AND wc.deleted_at IS NULL
      WHERE aw.agent_id = ?
      ORDER BY aw.sort_order ASC, aw.id ASC
      """, agentId);
  }

  @org.springframework.transaction.annotation.Transactional
  public void replaceAgentWorkflows(long agentId, List<Map<String, Object>> rows) {
    jdbcTemplate.update("DELETE FROM agent_workflows WHERE agent_id = ?", agentId);
    if (rows == null || rows.isEmpty()) {
      return;
    }
    int order = 0;
    for (Map<String, Object> row : rows) {
      Long workflowConfigId = toLongOrNull(row.get("workflowConfigId"));
      String customWorkflowId = toStringOrNull(row.get("customWorkflowId"));
      String customApiKeyCiphertext = toStringOrNull(row.get("customApiKeyCiphertext"));
      String customApiKeyLast4 = toStringOrNull(row.get("customApiKeyLast4"));
      Boolean enabled = row.get("enabled") == null ? Boolean.TRUE : Boolean.TRUE.equals(row.get("enabled"));
      insert("""
        INSERT INTO agent_workflows (
          agent_id, workflow_key, workflow_config_id,
          custom_workflow_id, custom_api_key_ciphertext, custom_api_key_last4,
          enabled, sort_order
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """,
        agentId,
        toStringOrNull(row.get("workflowKey")),
        workflowConfigId,
        customWorkflowId,
        customApiKeyCiphertext,
        customApiKeyLast4,
        enabled,
        order++
      );
    }
  }

  private static Long toLongOrNull(Object value) {
    if (value == null) return null;
    if (value instanceof Number n) return n.longValue();
    String s = String.valueOf(value).trim();
    if (s.isEmpty()) return null;
    try {
      return Long.parseLong(s);
    } catch (NumberFormatException error) {
      return null;
    }
  }

  private static String toStringOrNull(Object value) {
    if (value == null) return null;
    String s = String.valueOf(value);
    return s.isBlank() ? null : s;
  }

  public List<Map<String, Object>> listKnowledgeFiles(long teacherId) {
    return jdbcTemplate.queryForList("""
      SELECT
        kf.id,
        kf.original_name,
        kb.name AS base_name,
        kf.parse_status,
        kf.file_size,
        kf.updated_at,
        c.name AS course_name
      FROM knowledge_files kf
      JOIN knowledge_bases kb ON kb.id = kf.knowledge_base_id
      LEFT JOIN courses c ON c.id = kb.course_id AND c.deleted_at IS NULL
      WHERE kb.teacher_id = ? AND kb.deleted_at IS NULL AND kf.deleted_at IS NULL
      ORDER BY kf.updated_at DESC, kf.id DESC
      LIMIT 200
      """, teacherId);
  }

  public List<Map<String, Object>> listClasses(long teacherId) {
    return jdbcTemplate.queryForList("""
      SELECT
        cls.id,
        cls.name,
        cls.class_code,
        cls.status,
        cls.updated_at,
        cls.course_id,
        COALESCE(c.name, '未绑定课程') AS course_name,
        COALESCE(class_agent.id, course_agent.id) AS agent_id,
        COALESCE(class_agent.name, course_agent.name, '未绑定智能体') AS agent_name,
        COUNT(DISTINCT cm.id) AS student_count,
        COUNT(DISTINCT it.id) AS pending_tasks
      FROM classes cls
      LEFT JOIN courses c ON c.id = cls.course_id AND c.deleted_at IS NULL
      LEFT JOIN agents class_agent ON class_agent.id = cls.agent_id AND class_agent.deleted_at IS NULL
      LEFT JOIN agents course_agent ON course_agent.id = (
        SELECT ca.id
        FROM agents ca
        WHERE ca.teacher_id = cls.teacher_id
          AND ca.course_id = cls.course_id
          AND ca.deleted_at IS NULL
          AND ca.status <> 'DISABLED'
        ORDER BY
          CASE ca.status WHEN 'PUBLISHED' THEN 0 WHEN 'DRAFT' THEN 1 ELSE 2 END,
          ca.updated_at DESC,
          ca.id DESC
        LIMIT 1
      )
      LEFT JOIN class_members cm ON cm.class_id = cls.id AND cm.status = 'ACTIVE'
      LEFT JOIN interaction_tasks it ON it.class_id = cls.id AND it.status = 'PUBLISHED' AND it.deleted_at IS NULL
      WHERE cls.teacher_id = ? AND cls.deleted_at IS NULL
      GROUP BY
        cls.id, cls.name, cls.class_code, cls.status, cls.updated_at, cls.course_id,
        c.name, class_agent.id, class_agent.name, course_agent.id, course_agent.name
      ORDER BY cls.updated_at DESC, cls.id DESC
      LIMIT 200
      """, teacherId);
  }

  public List<Map<String, Object>> listClassMembers(long classId) {
    return jdbcTemplate.queryForList("""
      SELECT
        cm.id,
        cm.student_id,
        cm.nickname,
        cm.status,
        cm.joined_at,
        cm.removed_at,
        u.username,
        u.display_name,
        u.email,
        u.phone
      FROM class_members cm
      JOIN users u ON u.id = cm.student_id
      WHERE cm.class_id = ?
      ORDER BY cm.status ASC, cm.joined_at DESC, cm.id DESC
      LIMIT 500
      """, classId);
  }

  public List<Map<String, Object>> listStudentClasses(long studentId) {
    return jdbcTemplate.queryForList("""
      SELECT
        cls.id,
        cls.name,
        cls.status,
        COALESCE(class_agent.id, course_agent.id) AS agent_id,
        teacher.display_name AS teacher_name,
        COALESCE(c.name, '未绑定课程') AS course_name,
        COALESCE(class_agent.name, course_agent.name, '未绑定智能体') AS agent_name
      FROM class_members cm
      JOIN classes cls ON cls.id = cm.class_id
      JOIN users teacher ON teacher.id = cls.teacher_id
      LEFT JOIN courses c ON c.id = cls.course_id AND c.deleted_at IS NULL
      LEFT JOIN agents class_agent ON class_agent.id = cls.agent_id AND class_agent.deleted_at IS NULL
      LEFT JOIN agents course_agent ON course_agent.id = (
        SELECT ca.id
        FROM agents ca
        WHERE ca.teacher_id = cls.teacher_id
          AND ca.course_id = cls.course_id
          AND ca.deleted_at IS NULL
          AND ca.status <> 'DISABLED'
        ORDER BY
          CASE ca.status WHEN 'PUBLISHED' THEN 0 WHEN 'DRAFT' THEN 1 ELSE 2 END,
          ca.updated_at DESC,
          ca.id DESC
        LIMIT 1
      )
      WHERE cm.student_id = ?
        AND cm.status = 'ACTIVE'
        AND cls.deleted_at IS NULL
      ORDER BY cm.joined_at DESC, cm.id DESC
      LIMIT 200
      """, studentId);
  }

  public List<Map<String, Object>> listConversations(long teacherId) {
    return jdbcTemplate.queryForList("""
      SELECT
        cv.id,
        student.display_name AS student_name,
        cls.name AS class_name,
        COALESCE(cv.title, a.name, 'AI 实训对话') AS topic,
        cv.status,
        COALESCE(cv.last_message_at, cv.updated_at, cv.created_at) AS updated_at
      FROM conversations cv
      JOIN classes cls ON cls.id = cv.class_id
      JOIN agents a ON a.id = cv.agent_id
      JOIN users student ON student.id = cv.student_id
      WHERE cls.teacher_id = ?
        AND cv.deleted_at IS NULL
        AND cls.deleted_at IS NULL
      ORDER BY COALESCE(cv.last_message_at, cv.updated_at, cv.created_at) DESC, cv.id DESC
      LIMIT 50
      """, teacherId);
  }

  public Optional<Map<String, Object>> findConversationRuntimeContext(long conversationId) {
    List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
      SELECT
        cv.id AS conversation_id,
        cv.title,
        cv.class_id,
        cv.agent_id,
        cv.student_id,
        cls.name AS class_name,
        cls.teacher_id,
        c.name AS course_name,
        c.description AS course_description,
        a.name AS agent_name,
        a.description AS agent_description,
        a.opening_message,
        a.system_prompt,
        student.display_name AS student_name
      FROM conversations cv
      JOIN classes cls ON cls.id = cv.class_id AND cls.deleted_at IS NULL
      JOIN agents a ON a.id = cv.agent_id AND a.deleted_at IS NULL
      LEFT JOIN courses c ON c.id = cls.course_id AND c.deleted_at IS NULL
      JOIN users student ON student.id = cv.student_id
      WHERE cv.id = ? AND cv.deleted_at IS NULL
      LIMIT 1
      """, conversationId);
    return rows.stream().findFirst();
  }

  public List<Map<String, Object>> listConversationMessages(long conversationId, int limit) {
    return jdbcTemplate.queryForList("""
      SELECT sender_type, content, created_at
      FROM messages
      WHERE conversation_id = ? AND deleted_at IS NULL
      ORDER BY created_at DESC, id DESC
      LIMIT ?
      """, conversationId, limit);
  }

  public List<Map<String, Object>> listConversationMessagesAsc(long conversationId) {
    return jdbcTemplate.queryForList("""
      SELECT sender_type, content, created_at
      FROM messages
      WHERE conversation_id = ? AND deleted_at IS NULL
      ORDER BY created_at ASC, id ASC
      """, conversationId);
  }

  public List<Map<String, Object>> listTasks(long teacherId) {
    return jdbcTemplate.queryForList("""
      SELECT
        it.id,
        it.title,
        it.due_at,
        it.status,
        c.name AS course_name,
        cls.name AS class_name,
        COUNT(DISTINCT cm.id) AS total_count,
        SUM(CASE WHEN ts.status IN ('SUBMITTED', 'REVIEWED') THEN 1 ELSE 0 END) AS submitted_count
      FROM interaction_tasks it
      JOIN classes cls ON cls.id = it.class_id
      LEFT JOIN courses c ON c.id = cls.course_id AND c.deleted_at IS NULL
      LEFT JOIN class_members cm ON cm.class_id = cls.id AND cm.status = 'ACTIVE'
      LEFT JOIN task_submissions ts ON ts.task_id = it.id
      WHERE it.teacher_id = ? AND it.deleted_at IS NULL
      GROUP BY it.id, it.title, it.due_at, it.status, c.name, cls.name
      ORDER BY it.updated_at DESC, it.id DESC
      LIMIT 200
      """, teacherId);
  }

  public List<Map<String, Object>> listStudentTasks(long studentId) {
    return jdbcTemplate.queryForList("""
      SELECT
        it.id,
        it.title,
        COALESCE(ts.status, 'NOT_STARTED') AS status,
        it.due_at,
        CASE
          WHEN ts.status = 'REVIEWED' THEN '已反馈'
          WHEN ts.status = 'SUBMITTED' THEN '待教师反馈'
          ELSE '未提交'
        END AS feedback_status
      FROM interaction_tasks it
      JOIN classes cls ON cls.id = it.class_id
      JOIN class_members cm ON cm.class_id = cls.id AND cm.student_id = ? AND cm.status = 'ACTIVE'
      LEFT JOIN task_submissions ts ON ts.task_id = it.id AND ts.student_id = ?
      WHERE it.deleted_at IS NULL
        AND it.status IN ('PUBLISHED', 'CLOSED')
        AND cls.deleted_at IS NULL
      ORDER BY it.due_at IS NULL, it.due_at ASC, it.id DESC
      LIMIT 200
      """, studentId, studentId);
  }

  public List<Map<String, Object>> listCallLogs() {
    return jdbcTemplate.queryForList("""
      SELECT
        m.id,
        COALESCE(wc.name, a.name, 'AI 调用') AS workflow_name,
        COALESCE(u.display_name, m.sender_type) AS caller,
        CASE WHEN m.error_code IS NULL THEN '成功' ELSE '失败' END AS status,
        COALESCE(m.latency_ms, 0) AS latency_ms,
        m.created_at
      FROM messages m
      JOIN conversations cv ON cv.id = m.conversation_id
      JOIN agents a ON a.id = cv.agent_id
      LEFT JOIN workflow_configs wc ON wc.workflow_id = a.dify_workflow_id AND wc.deleted_at IS NULL
      LEFT JOIN users u ON u.id = m.sender_id
      WHERE m.deleted_at IS NULL
      ORDER BY m.created_at DESC, m.id DESC
      LIMIT 100
      """);
  }

  public List<Map<String, Object>> listAuditLogs() {
    return jdbcTemplate.queryForList("""
      SELECT
        al.id,
        al.action,
        COALESCE(u.display_name, '系统') AS actor,
        CONCAT(al.target_type, COALESCE(CONCAT('#', al.target_id), '')) AS target,
        al.created_at
      FROM audit_logs al
      LEFT JOIN users u ON u.id = al.actor_id
      ORDER BY al.created_at DESC, al.id DESC
      LIMIT 100
      """);
  }

  public Map<String, Object> createUser(
    String role,
    String username,
    String passwordHash,
    String displayName,
    String email,
    String phone
  ) {
    long id = insert("""
      INSERT INTO users (role, username, password_hash, display_name, email, phone)
      VALUES (?, ?, ?, ?, ?, ?)
      """, role, username, passwordHash, displayName, email, phone);
    return findById("users", id).orElseThrow();
  }

  public Map<String, Object> createCourse(
    long teacherId,
    String name,
    String subject,
    String gradeLevel,
    String description
  ) {
    long id = insert("""
      INSERT INTO courses (teacher_id, name, subject, grade_level, description)
      VALUES (?, ?, ?, ?, ?)
      """, teacherId, name, subject, gradeLevel, description);
    return findById("courses", id).orElseThrow();
  }

  public Map<String, Object> updateCourse(
    long courseId,
    long teacherId,
    String name,
    String subject,
    String gradeLevel,
    String description,
    String status
  ) {
    int updated = jdbcTemplate.update("""
      UPDATE courses
      SET name = ?, subject = ?, grade_level = ?, description = ?, status = ?, updated_at = CURRENT_TIMESTAMP
      WHERE id = ? AND teacher_id = ? AND deleted_at IS NULL
      """, name, subject, gradeLevel, description, status, courseId, teacherId);
    if (updated == 0) {
      throw new IllegalStateException("Course update did not affect any row.");
    }
    return findById("courses", courseId).orElseThrow();
  }

  public Map<String, Object> createAgent(
    long teacherId,
    Long courseId,
    String name,
    String description,
    String systemPrompt,
    String openingMessage,
    String difyAppId,
    String difyWorkflowId,
    String difyApiKeyRef,
    String status
  ) {
    long id = insert("""
      INSERT INTO agents (
        teacher_id, course_id, name, description, system_prompt, opening_message,
        dify_app_id, dify_workflow_id, dify_api_key_ref, status
      )
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """, teacherId, courseId, name, description, systemPrompt, openingMessage, difyAppId, difyWorkflowId, difyApiKeyRef, status);
    return findById("agents", id).orElseThrow();
  }

  public Map<String, Object> updateAgent(
    long agentId,
    long teacherId,
    Long courseId,
    String name,
    String description,
    String openingMessage,
    String difyWorkflowId,
    String status
  ) {
    int updated = jdbcTemplate.update("""
      UPDATE agents
      SET course_id = ?, name = ?, description = ?, opening_message = ?, dify_workflow_id = ?, status = ?, updated_at = CURRENT_TIMESTAMP
      WHERE id = ? AND teacher_id = ? AND deleted_at IS NULL
      """, courseId, name, description, openingMessage, difyWorkflowId, status, agentId, teacherId);
    if (updated == 0) {
      throw new IllegalStateException("Agent update did not affect any row.");
    }
    return findById("agents", agentId).orElseThrow();
  }

  public Map<String, Object> createClass(
    long teacherId,
    Long courseId,
    Long agentId,
    String name,
    String classCode,
    String description
  ) {
    long id = insert("""
      INSERT INTO classes (teacher_id, course_id, agent_id, name, class_code, description)
      VALUES (?, ?, ?, ?, ?, ?)
      """, teacherId, courseId, agentId, name, classCode, description);
    return findById("classes", id).orElseThrow();
  }

  public void updateClass(long classId, long teacherId, Long courseId, Long agentId, String name) {
    jdbcTemplate.update("""
      UPDATE classes
      SET course_id = ?, agent_id = ?, name = ?, updated_at = CURRENT_TIMESTAMP
      WHERE id = ? AND teacher_id = ? AND deleted_at IS NULL
      """, courseId, agentId, name, classId, teacherId);
  }

  public void softDeleteClass(long id) {
    jdbcTemplate.update("""
      UPDATE classes
      SET deleted_at = CURRENT_TIMESTAMP
      WHERE id = ? AND deleted_at IS NULL
      """, id);
  }

  public Optional<Long> findDefaultAgentIdForCourse(long teacherId, long courseId) {
    List<Long> rows = jdbcTemplate.queryForList("""
      SELECT id
      FROM agents
      WHERE teacher_id = ?
        AND course_id = ?
        AND deleted_at IS NULL
        AND status <> 'DISABLED'
      ORDER BY
        CASE status WHEN 'PUBLISHED' THEN 0 WHEN 'DRAFT' THEN 1 ELSE 2 END,
        updated_at DESC,
        id DESC
      LIMIT 1
      """, Long.class, teacherId, courseId);
    return rows.stream().findFirst();
  }

  public Map<String, Object> joinClass(long classId, long studentId, String nickname) {
    Long existingId = jdbcTemplate.query("""
        SELECT id FROM class_members
        WHERE class_id = ? AND student_id = ?
        LIMIT 1
        """,
      rs -> rs.next() ? rs.getLong("id") : null,
      classId,
      studentId
    );

    if (existingId != null) {
      jdbcTemplate.update("""
        UPDATE class_members
        SET nickname = ?, status = 'ACTIVE', removed_at = NULL
        WHERE id = ?
        """, nickname, existingId);
      return findById("class_members", existingId).orElseThrow();
    }

    long id = insert("""
      INSERT INTO class_members (class_id, student_id, nickname)
      VALUES (?, ?, ?)
      """, classId, studentId, nickname);
    return findById("class_members", id).orElseThrow();
  }

  public Map<String, Object> createConversation(long classId, long agentId, long studentId, String title) {
    long id = insert("""
      INSERT INTO conversations (class_id, agent_id, student_id, title)
      VALUES (?, ?, ?, ?)
      """, classId, agentId, studentId, title);
    return findById("conversations", id).orElseThrow();
  }

  public Map<String, Object> createMessage(
    long conversationId,
    String senderType,
    Long senderId,
    String content,
    String contentType,
    String difyMessageId,
    String difyTaskId
  ) {
    long id = insert("""
      INSERT INTO messages (
        conversation_id, sender_type, sender_id, content, content_type, dify_message_id, dify_task_id
      )
      VALUES (?, ?, ?, ?, ?, ?, ?)
      """, conversationId, senderType, senderId, content, contentType, difyMessageId, difyTaskId);
    return findById("messages", id).orElseThrow();
  }

  public void updateMessageCallResult(
    long messageId,
    Integer latencyMs,
    String errorCode,
    String errorMessage
  ) {
    jdbcTemplate.update("""
      UPDATE messages
      SET latency_ms = ?, error_code = ?, error_message = ?
      WHERE id = ?
      """, latencyMs, errorCode, errorMessage, messageId);
  }

  public void touchConversation(long conversationId) {
    jdbcTemplate.update("""
      UPDATE conversations
      SET last_message_at = CURRENT_TIMESTAMP
      WHERE id = ?
      """, conversationId);
  }

  public List<Map<String, Object>> listWorkflowConfigs(long teacherId) {
    return jdbcTemplate.queryForList("""
      SELECT
        id, teacher_id, workflow_key, name, base_url, workflow_id,
        api_key_last4, enabled, last_test_status, last_test_message,
        last_test_at, created_at, updated_at
      FROM workflow_configs
      WHERE teacher_id = ? AND deleted_at IS NULL
      ORDER BY id DESC
      """, teacherId);
  }

  public Optional<Map<String, Object>> findWorkflowConfig(long teacherId, String workflowKey) {
    List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
      SELECT *
      FROM workflow_configs
      WHERE teacher_id = ? AND workflow_key = ? AND deleted_at IS NULL
      LIMIT 1
      """, teacherId, workflowKey);
    return rows.stream().findFirst();
  }

  public Optional<Map<String, Object>> findWorkflowConfigIncludingDeleted(long teacherId, String workflowKey) {
    List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
      SELECT *
      FROM workflow_configs
      WHERE teacher_id = ? AND workflow_key = ?
      LIMIT 1
      """, teacherId, workflowKey);
    return rows.stream().findFirst();
  }

  public Map<String, Object> upsertWorkflowConfig(
    long teacherId,
    String workflowKey,
    String name,
    String baseUrl,
    String workflowId,
    String apiKeyCiphertext,
    String apiKeyLast4,
    boolean enabled
  ) {
    Optional<Map<String, Object>> existing = findWorkflowConfigIncludingDeleted(teacherId, workflowKey);
    if (existing.isPresent()) {
      long id = ((Number) existing.get().get("id")).longValue();
      if (apiKeyCiphertext == null) {
        jdbcTemplate.update("""
          UPDATE workflow_configs
          SET name = ?, base_url = ?, workflow_id = ?, enabled = ?, deleted_at = NULL,
              last_test_status = 'UNTESTED', last_test_message = NULL, last_test_at = NULL
          WHERE id = ?
          """, name, baseUrl, workflowId, enabled, id);
      } else {
        jdbcTemplate.update("""
          UPDATE workflow_configs
          SET name = ?, base_url = ?, workflow_id = ?, api_key_ciphertext = ?, api_key_last4 = ?, enabled = ?, deleted_at = NULL,
              last_test_status = 'UNTESTED', last_test_message = NULL, last_test_at = NULL
          WHERE id = ?
          """, name, baseUrl, workflowId, apiKeyCiphertext, apiKeyLast4, enabled, id);
      }
      return findById("workflow_configs", id).orElseThrow();
    }

    long id = insert("""
      INSERT INTO workflow_configs (
        teacher_id, workflow_key, name, base_url, workflow_id, api_key_ciphertext, api_key_last4, enabled,
        last_test_status, last_test_message, last_test_at
      )
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'UNTESTED', NULL, NULL)
      """, teacherId, workflowKey, name, baseUrl, workflowId, apiKeyCiphertext, apiKeyLast4, enabled);
    return findById("workflow_configs", id).orElseThrow();
  }

  public void softDeleteWorkflowConfig(long id) {
    jdbcTemplate.update("""
      UPDATE workflow_configs
      SET deleted_at = CURRENT_TIMESTAMP
      WHERE id = ? AND deleted_at IS NULL
      """, id);
  }

  public void softDeleteCourse(long id) {
    jdbcTemplate.update("""
      UPDATE courses
      SET deleted_at = CURRENT_TIMESTAMP
      WHERE id = ? AND deleted_at IS NULL
      """, id);
  }

  public void softDeleteAgent(long id) {
    jdbcTemplate.update("""
      UPDATE agents
      SET deleted_at = CURRENT_TIMESTAMP
      WHERE id = ? AND deleted_at IS NULL
      """, id);
  }

  public void updateWorkflowTestResult(long id, boolean success, String message) {
    jdbcTemplate.update("""
      UPDATE workflow_configs
      SET last_test_status = ?, last_test_message = ?, last_test_at = CURRENT_TIMESTAMP
      WHERE id = ?
      """, success ? "SUCCESS" : "FAILED", message, id);
  }

  public boolean isClassMember(long classId, long studentId) {
    Integer count = jdbcTemplate.queryForObject("""
      SELECT COUNT(*)
      FROM class_members
      WHERE class_id = ? AND student_id = ? AND status = 'ACTIVE'
      """, Integer.class, classId, studentId);
    return count != null && count > 0;
  }

  public Optional<Map<String, Object>> findClassByCode(String classCode) {
    List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
      SELECT *
      FROM classes
      WHERE class_code = ? AND join_enabled = 1 AND status = 'ACTIVE' AND deleted_at IS NULL
      LIMIT 1
      """, classCode);
    return rows.stream().findFirst();
  }

  public Optional<Map<String, Object>> findById(String table, long id) {
    assertAllowedTable(table);
    List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM " + table + " WHERE id = ? LIMIT 1", id);
    return rows.stream().findFirst();
  }

  public boolean existsById(String table, long id) {
    assertAllowedTable(table);
    Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE id = ?", Integer.class, id);
    return count != null && count > 0;
  }

  public List<Map<String, Object>> listProducts(long teacherId) {
    return jdbcTemplate.queryForList("""
      SELECT id, name, route_name, context_status, change_status, status, updated_at
      FROM products
      WHERE teacher_id = ? AND deleted_at IS NULL
      ORDER BY updated_at DESC, id DESC
      LIMIT 200
      """, teacherId);
  }

  public Map<String, Object> createProduct(
    long teacherId,
    String name,
    String routeName,
    String oldInfo,
    String newInfo,
    String productContext
  ) {
    long id = insert("""
      INSERT INTO products (teacher_id, name, route_name, old_info, new_info, product_context, context_status)
      VALUES (?, ?, ?, ?, ?, ?, ?)
      """, teacherId, name, routeName, oldInfo, newInfo, productContext,
      productContext == null || productContext.isBlank() ? "DRAFT" : "READY");
    return findById("products", id).orElseThrow();
  }

  public Map<String, Object> updateProductChangeResult(long productId, String changeSummary, String changeStatus) {
    int updated = jdbcTemplate.update("""
      UPDATE products
      SET change_summary = ?, change_status = ?, updated_at = CURRENT_TIMESTAMP
      WHERE id = ? AND deleted_at IS NULL
      """, changeSummary, changeStatus, productId);
    if (updated == 0) {
      throw new IllegalStateException("Product change result update did not affect any row.");
    }
    return findById("products", productId).orElseThrow();
  }

  public List<Map<String, Object>> listAttractionSpeeches(long teacherId) {
    return jdbcTemplate.queryForList("""
      SELECT id, attraction_name, geography_type, tag_status, speech_status, updated_at
      FROM attraction_speeches
      WHERE teacher_id = ? AND deleted_at IS NULL
      ORDER BY updated_at DESC, id DESC
      LIMIT 200
      """, teacherId);
  }

  public Map<String, Object> createAttractionSpeech(
    long teacherId,
    String attractionName,
    String geographyType,
    String cultureTags,
    String customerExperienceLevel,
    String customerScene,
    String sellingGoal,
    String attractionBasicInfo,
    String courseContext
  ) {
    long id = insert("""
      INSERT INTO attraction_speeches (
        teacher_id, attraction_name, geography_type, culture_tags, customer_experience_level,
        customer_scene, selling_goal, attraction_basic_info, course_context, tag_status
      )
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """, teacherId, attractionName, geographyType, cultureTags, customerExperienceLevel,
      customerScene, sellingGoal, attractionBasicInfo, courseContext,
      cultureTags == null || cultureTags.isBlank() ? "PENDING" : "TAGGED");
    return findById("attraction_speeches", id).orElseThrow();
  }

  public Map<String, Object> updateAttractionSpeechResult(long speechId, String speechContent, String speechStatus) {
    int updated = jdbcTemplate.update("""
      UPDATE attraction_speeches
      SET speech_content = ?, speech_status = ?, updated_at = CURRENT_TIMESTAMP
      WHERE id = ? AND deleted_at IS NULL
      """, speechContent, speechStatus, speechId);
    if (updated == 0) {
      throw new IllegalStateException("Attraction speech result update did not affect any row.");
    }
    return findById("attraction_speeches", speechId).orElseThrow();
  }

  public List<Map<String, Object>> listCustomerProfiles(long teacherId) {
    return jdbcTemplate.queryForList("""
      SELECT id, name, profile_type, shared_by, status, updated_at
      FROM customer_profiles
      WHERE teacher_id = ? AND deleted_at IS NULL
      ORDER BY updated_at DESC, id DESC
      LIMIT 200
      """, teacherId);
  }

  public Map<String, Object> createCustomerProfile(
    long teacherId,
    String name,
    String profileType,
    String profileContent,
    String sharedBy
  ) {
    long id = insert("""
      INSERT INTO customer_profiles (teacher_id, name, profile_type, profile_content, shared_by)
      VALUES (?, ?, ?, ?, ?)
      """, teacherId, name, profileType, profileContent, sharedBy);
    return findById("customer_profiles", id).orElseThrow();
  }

  public List<Map<String, Object>> listTrainingScenarios(long teacherId) {
    return jdbcTemplate.queryForList("""
      SELECT id, name, training_goal, difficulty, coaching_mode, status, updated_at
      FROM training_scenarios
      WHERE teacher_id = ? AND deleted_at IS NULL
      ORDER BY updated_at DESC, id DESC
      LIMIT 200
      """, teacherId);
  }

  public Map<String, Object> createTrainingScenario(
    long teacherId,
    String name,
    String trainingGoal,
    String difficulty,
    String coachingMode,
    String description
  ) {
    long id = insert("""
      INSERT INTO training_scenarios (teacher_id, name, training_goal, difficulty, coaching_mode, description)
      VALUES (?, ?, ?, ?, ?, ?)
      """, teacherId, name, trainingGoal,
      difficulty == null || difficulty.isBlank() ? "入门" : difficulty,
      coachingMode == null || coachingMode.isBlank() ? "即时提示" : coachingMode,
      description);
    return findById("training_scenarios", id).orElseThrow();
  }

  private long insert(String sql, Object... args) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      for (int i = 0; i < args.length; i++) {
        statement.setObject(i + 1, args[i]);
      }
      return statement;
    }, keyHolder);

    Number key = keyHolder.getKey();
    if (key == null) {
      throw new IllegalStateException("Insert did not return a generated key.");
    }
    return key.longValue();
  }

  private long count(String sql, Object... args) {
    Long result = jdbcTemplate.queryForObject(sql, Long.class, args);
    return result == null ? 0 : result;
  }

  private static void assertAllowedTable(String table) {
    if (!ALLOWED_TABLES.contains(table)) {
      throw new IllegalArgumentException("Unsupported table: " + table);
    }
  }
}
