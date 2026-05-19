package com.teachagent;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class TeachAgentRepository {
  private static final List<String> ALLOWED_TABLES = List.of(
    "users", "courses", "agents", "classes", "class_members", "conversations", "messages"
  );

  private final JdbcTemplate jdbcTemplate;

  public TeachAgentRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
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

  public Map<String, Object> createAgent(
    long teacherId,
    Long courseId,
    String name,
    String description,
    String systemPrompt,
    String openingMessage,
    String difyAppId,
    String difyWorkflowId,
    String difyApiKeyRef
  ) {
    long id = insert("""
      INSERT INTO agents (
        teacher_id, course_id, name, description, system_prompt, opening_message,
        dify_app_id, dify_workflow_id, dify_api_key_ref
      )
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      """, teacherId, courseId, name, description, systemPrompt, openingMessage, difyAppId, difyWorkflowId, difyApiKeyRef);
    return findById("agents", id).orElseThrow();
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

  public void touchConversation(long conversationId) {
    jdbcTemplate.update("""
      UPDATE conversations
      SET last_message_at = CURRENT_TIMESTAMP
      WHERE id = ?
      """, conversationId);
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

  private static void assertAllowedTable(String table) {
    if (!ALLOWED_TABLES.contains(table)) {
      throw new IllegalArgumentException("Unsupported table: " + table);
    }
  }
}
