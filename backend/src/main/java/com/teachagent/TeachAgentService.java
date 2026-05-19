package com.teachagent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class TeachAgentService {
  private final TeachAgentRepository repository;
  private final String mockAgentReply;

  public TeachAgentService(
    TeachAgentRepository repository,
    @Value("${teach-agent.mock-agent-reply}") String mockAgentReply
  ) {
    this.repository = repository;
    this.mockAgentReply = mockAgentReply;
  }

  @Transactional
  public Map<String, Object> createUser(Map<String, String> body) {
    String role = require(body, "role").toUpperCase();
    if (!List.of("ADMIN", "TEACHER", "STUDENT").contains(role)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ROLE", "role must be ADMIN, TEACHER, or STUDENT.");
    }

    return repository.createUser(
      role,
      require(body, "username"),
      body.getOrDefault("passwordHash", "{noop}123456"),
      body.getOrDefault("displayName", body.get("username")),
      blankToNull(body.get("email")),
      blankToNull(body.get("phone"))
    );
  }

  @Transactional
  public Map<String, Object> createCourse(Map<String, String> body) {
    long teacherId = requireLong(body, "teacherId");
    requireUserRole(teacherId, "TEACHER");

    return repository.createCourse(
      teacherId,
      require(body, "name"),
      blankToNull(body.get("subject")),
      blankToNull(body.get("gradeLevel")),
      blankToNull(body.get("description"))
    );
  }

  @Transactional
  public Map<String, Object> createAgent(Map<String, String> body) {
    long teacherId = requireLong(body, "teacherId");
    requireUserRole(teacherId, "TEACHER");
    Long courseId = optionalLong(body, "courseId");
    if (courseId != null && !repository.existsById("courses", courseId)) {
      throw new ApiException(HttpStatus.NOT_FOUND, "COURSE_NOT_FOUND", "courseId does not exist.");
    }

    return repository.createAgent(
      teacherId,
      courseId,
      require(body, "name"),
      blankToNull(body.get("description")),
      blankToNull(body.get("systemPrompt")),
      blankToNull(body.get("openingMessage")),
      blankToNull(body.get("difyAppId")),
      blankToNull(body.get("difyWorkflowId")),
      blankToNull(body.get("difyApiKeyRef"))
    );
  }

  @Transactional
  public Map<String, Object> createClass(Map<String, String> body) {
    long teacherId = requireLong(body, "teacherId");
    requireUserRole(teacherId, "TEACHER");
    Long courseId = optionalLong(body, "courseId");
    Long agentId = optionalLong(body, "agentId");

    if (courseId != null && !repository.existsById("courses", courseId)) {
      throw new ApiException(HttpStatus.NOT_FOUND, "COURSE_NOT_FOUND", "courseId does not exist.");
    }
    if (agentId != null && !repository.existsById("agents", agentId)) {
      throw new ApiException(HttpStatus.NOT_FOUND, "AGENT_NOT_FOUND", "agentId does not exist.");
    }

    return repository.createClass(
      teacherId,
      courseId,
      agentId,
      require(body, "name"),
      generateClassCode(),
      blankToNull(body.get("description"))
    );
  }

  @Transactional
  public Map<String, Object> joinClass(Map<String, String> body) {
    long studentId = requireLong(body, "studentId");
    requireUserRole(studentId, "STUDENT");
    Map<String, Object> classroom = repository.findClassByCode(require(body, "classCode"))
      .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CLASS_NOT_FOUND", "classCode does not exist."));

    return repository.joinClass(
      ((Number) classroom.get("id")).longValue(),
      studentId,
      blankToNull(body.get("nickname"))
    );
  }

  @Transactional
  public Map<String, Object> createConversation(Map<String, String> body) {
    long classId = requireLong(body, "classId");
    long agentId = requireLong(body, "agentId");
    long studentId = requireLong(body, "studentId");

    requireExists("classes", classId, "CLASS_NOT_FOUND", "classId does not exist.");
    requireExists("agents", agentId, "AGENT_NOT_FOUND", "agentId does not exist.");
    requireUserRole(studentId, "STUDENT");

    if (!repository.isClassMember(classId, studentId)) {
      throw new ApiException(HttpStatus.FORBIDDEN, "NOT_CLASS_MEMBER", "studentId has not joined this class.");
    }

    return repository.createConversation(
      classId,
      agentId,
      studentId,
      body.getOrDefault("title", "新对话")
    );
  }

  @Transactional
  public Map<String, Object> createMessage(Map<String, String> body) {
    long conversationId = requireLong(body, "conversationId");
    requireExists("conversations", conversationId, "CONVERSATION_NOT_FOUND", "conversationId does not exist.");
    String content = require(body, "content");

    Map<String, Object> studentMessage = repository.createMessage(
      conversationId,
      body.getOrDefault("senderType", "STUDENT").toUpperCase(),
      optionalLong(body, "senderId"),
      content,
      "TEXT",
      null,
      null
    );

    Map<String, Object> agentMessage = repository.createMessage(
      conversationId,
      "AGENT",
      null,
      mockAgentReply,
      "TEXT",
      null,
      UUID.randomUUID().toString()
    );
    repository.touchConversation(conversationId);

    return Map.of(
      "conversationId", conversationId,
      "studentMessage", studentMessage,
      "agentMessage", agentMessage
    );
  }

  private void requireUserRole(long userId, String role) {
    Map<String, Object> user = repository.findById("users", userId)
      .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "userId does not exist."));
    if (!role.equals(user.get("role"))) {
      throw new ApiException(HttpStatus.FORBIDDEN, "ROLE_MISMATCH", "userId must be a " + role + ".");
    }
  }

  private void requireExists(String table, long id, String code, String message) {
    if (!repository.existsById(table, id)) {
      throw new ApiException(HttpStatus.NOT_FOUND, code, message);
    }
  }

  private static String require(Map<String, String> body, String key) {
    return Optional.ofNullable(body.get(key))
      .filter(value -> !value.isBlank())
      .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "MISSING_FIELD", key + " is required."));
  }

  private static long requireLong(Map<String, String> body, String key) {
    try {
      return Long.parseLong(require(body, key));
    } catch (NumberFormatException error) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_NUMBER", key + " must be a number.");
    }
  }

  private static Long optionalLong(Map<String, String> body, String key) {
    String value = body.get(key);
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException error) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_NUMBER", key + " must be a number.");
    }
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private static String generateClassCode() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
  }
}
