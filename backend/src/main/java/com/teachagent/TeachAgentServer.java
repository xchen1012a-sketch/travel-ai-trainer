package com.teachagent;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class TeachAgentServer {
  private static final ApiStore STORE = new ApiStore();

  public static void main(String[] args) throws IOException {
    int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);

    server.createContext("/api/health", exchange -> route(exchange, () -> ok(Map.of(
      "status", "ok",
      "service", "teach-agent-backend",
      "time", Instant.now().toString()
    ))));

    server.createContext("/api/users", exchange -> route(exchange, () -> {
      requireMethod(exchange, "POST");
      Map<String, String> body = Json.readObject(exchange.getRequestBody());
      return created(STORE.createUser(body));
    }));

    server.createContext("/api/courses", exchange -> route(exchange, () -> {
      requireMethod(exchange, "POST");
      Map<String, String> body = Json.readObject(exchange.getRequestBody());
      return created(STORE.createCourse(body));
    }));

    server.createContext("/api/agents", exchange -> route(exchange, () -> {
      requireMethod(exchange, "POST");
      Map<String, String> body = Json.readObject(exchange.getRequestBody());
      return created(STORE.createAgent(body));
    }));

    server.createContext("/api/classes", exchange -> route(exchange, () -> {
      requireMethod(exchange, "POST");
      Map<String, String> body = Json.readObject(exchange.getRequestBody());
      return created(STORE.createClass(body));
    }));

    server.createContext("/api/class-members/join", exchange -> route(exchange, () -> {
      requireMethod(exchange, "POST");
      Map<String, String> body = Json.readObject(exchange.getRequestBody());
      return ok(STORE.joinClass(body));
    }));

    server.createContext("/api/conversations", exchange -> route(exchange, () -> {
      requireMethod(exchange, "POST");
      Map<String, String> body = Json.readObject(exchange.getRequestBody());
      return created(STORE.createConversation(body));
    }));

    server.createContext("/api/messages", exchange -> route(exchange, () -> {
      requireMethod(exchange, "POST");
      Map<String, String> body = Json.readObject(exchange.getRequestBody());
      return created(STORE.createMessage(body));
    }));

    server.createContext("/", exchange -> route(exchange, () -> {
      if (!"GET".equals(exchange.getRequestMethod())) {
        throw new ApiException(405, "METHOD_NOT_ALLOWED", "Only GET is supported.");
      }
      return ok(Map.of(
        "name", "teach-agent-backend",
        "docs", "/api/health",
        "endpoints", List.of(
          "POST /api/users",
          "POST /api/courses",
          "POST /api/agents",
          "POST /api/classes",
          "POST /api/class-members/join",
          "POST /api/conversations",
          "POST /api/messages"
        )
      ));
    }));

    server.setExecutor(null);
    server.start();
    System.out.println("TeachAgent backend listening on http://localhost:" + port);
  }

  private static void route(HttpExchange exchange, Handler handler) throws IOException {
    try {
      ApiResponse response = handler.handle();
      write(exchange, response.statusCode(), response.body());
    } catch (ApiException error) {
      write(exchange, error.statusCode(), Json.stringify(Map.of(
        "success", false,
        "error", Map.of(
          "code", error.code(),
          "message", error.getMessage()
        )
      )));
    } catch (Exception error) {
      write(exchange, 500, Json.stringify(Map.of(
        "success", false,
        "error", Map.of(
          "code", "INTERNAL_ERROR",
          "message", error.getMessage() == null ? "Internal server error." : error.getMessage()
        )
      )));
    } finally {
      exchange.close();
    }
  }

  private static ApiResponse ok(Object data) {
    return new ApiResponse(200, Json.stringify(Map.of("success", true, "data", data)));
  }

  private static ApiResponse created(Object data) {
    return new ApiResponse(201, Json.stringify(Map.of("success", true, "data", data)));
  }

  private static void requireMethod(HttpExchange exchange, String method) {
    if (!method.equals(exchange.getRequestMethod())) {
      throw new ApiException(405, "METHOD_NOT_ALLOWED", "Expected " + method + ".");
    }
  }

  private static void write(HttpExchange exchange, int statusCode, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
    exchange.sendResponseHeaders(statusCode, bytes.length);
    try (OutputStream output = exchange.getResponseBody()) {
      output.write(bytes);
    }
  }

  @FunctionalInterface
  private interface Handler {
    ApiResponse handle() throws IOException;
  }

  private record ApiResponse(int statusCode, String body) {
  }

  private static final class ApiException extends RuntimeException {
    private final int statusCode;
    private final String code;

    private ApiException(int statusCode, String code, String message) {
      super(message);
      this.statusCode = statusCode;
      this.code = code;
    }

    private int statusCode() {
      return statusCode;
    }

    private String code() {
      return code;
    }
  }

  private static final class ApiStore {
    private final AtomicLong userIds = new AtomicLong(1000);
    private final AtomicLong courseIds = new AtomicLong(2000);
    private final AtomicLong agentIds = new AtomicLong(3000);
    private final AtomicLong classIds = new AtomicLong(4000);
    private final AtomicLong memberIds = new AtomicLong(5000);
    private final AtomicLong conversationIds = new AtomicLong(6000);
    private final AtomicLong messageIds = new AtomicLong(7000);

    private final Map<Long, Map<String, Object>> users = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Object>> courses = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Object>> agents = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Object>> classes = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Object>> classMembers = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Object>> conversations = new ConcurrentHashMap<>();
    private final Map<Long, List<Map<String, Object>>> messagesByConversation = new ConcurrentHashMap<>();

    private Map<String, Object> createUser(Map<String, String> body) {
      String role = require(body, "role").toUpperCase();
      if (!List.of("ADMIN", "TEACHER", "STUDENT").contains(role)) {
        throw new ApiException(400, "INVALID_ROLE", "role must be ADMIN, TEACHER, or STUDENT.");
      }

      long id = userIds.incrementAndGet();
      Map<String, Object> user = entity(id);
      user.put("role", role);
      user.put("username", require(body, "username"));
      user.put("displayName", body.getOrDefault("displayName", body.get("username")));
      user.put("status", "ACTIVE");
      users.put(id, user);
      return user;
    }

    private Map<String, Object> createCourse(Map<String, String> body) {
      long teacherId = requireLong(body, "teacherId");
      requireUserRole(teacherId, "TEACHER");

      long id = courseIds.incrementAndGet();
      Map<String, Object> course = entity(id);
      course.put("teacherId", teacherId);
      course.put("name", require(body, "name"));
      course.put("subject", body.getOrDefault("subject", ""));
      course.put("gradeLevel", body.getOrDefault("gradeLevel", ""));
      course.put("status", "DRAFT");
      courses.put(id, course);
      return course;
    }

    private Map<String, Object> createAgent(Map<String, String> body) {
      long teacherId = requireLong(body, "teacherId");
      requireUserRole(teacherId, "TEACHER");

      Long courseId = optionalLong(body, "courseId");
      if (courseId != null && !courses.containsKey(courseId)) {
        throw new ApiException(404, "COURSE_NOT_FOUND", "courseId does not exist.");
      }

      long id = agentIds.incrementAndGet();
      Map<String, Object> agent = entity(id);
      agent.put("teacherId", teacherId);
      agent.put("courseId", courseId);
      agent.put("name", require(body, "name"));
      agent.put("description", body.getOrDefault("description", ""));
      agent.put("difyWorkflowId", body.getOrDefault("difyWorkflowId", ""));
      agent.put("status", "DRAFT");
      agents.put(id, agent);
      return agent;
    }

    private Map<String, Object> createClass(Map<String, String> body) {
      long teacherId = requireLong(body, "teacherId");
      requireUserRole(teacherId, "TEACHER");

      Long agentId = optionalLong(body, "agentId");
      if (agentId != null && !agents.containsKey(agentId)) {
        throw new ApiException(404, "AGENT_NOT_FOUND", "agentId does not exist.");
      }

      long id = classIds.incrementAndGet();
      Map<String, Object> classroom = entity(id);
      classroom.put("teacherId", teacherId);
      classroom.put("courseId", optionalLong(body, "courseId"));
      classroom.put("agentId", agentId);
      classroom.put("name", require(body, "name"));
      classroom.put("classCode", generateClassCode());
      classroom.put("joinEnabled", true);
      classroom.put("status", "ACTIVE");
      classes.put(id, classroom);
      return classroom;
    }

    private Map<String, Object> joinClass(Map<String, String> body) {
      long studentId = requireLong(body, "studentId");
      requireUserRole(studentId, "STUDENT");
      String classCode = require(body, "classCode");

      Map<String, Object> classroom = classes.values().stream()
        .filter(item -> classCode.equals(item.get("classCode")))
        .findFirst()
        .orElseThrow(() -> new ApiException(404, "CLASS_NOT_FOUND", "classCode does not exist."));

      long id = memberIds.incrementAndGet();
      Map<String, Object> member = entity(id);
      member.put("classId", classroom.get("id"));
      member.put("studentId", studentId);
      member.put("nickname", body.getOrDefault("nickname", ""));
      member.put("status", "ACTIVE");
      classMembers.put(id, member);
      return member;
    }

    private Map<String, Object> createConversation(Map<String, String> body) {
      long classId = requireLong(body, "classId");
      long agentId = requireLong(body, "agentId");
      long studentId = requireLong(body, "studentId");
      requireExists(classes, classId, "CLASS_NOT_FOUND", "classId does not exist.");
      requireExists(agents, agentId, "AGENT_NOT_FOUND", "agentId does not exist.");
      requireUserRole(studentId, "STUDENT");

      boolean isMember = classMembers.values().stream()
        .anyMatch(member -> classId == (long) member.get("classId") && studentId == (long) member.get("studentId"));
      if (!isMember) {
        throw new ApiException(403, "NOT_CLASS_MEMBER", "studentId has not joined this class.");
      }

      long id = conversationIds.incrementAndGet();
      Map<String, Object> conversation = entity(id);
      conversation.put("classId", classId);
      conversation.put("agentId", agentId);
      conversation.put("studentId", studentId);
      conversation.put("title", body.getOrDefault("title", "新对话"));
      conversation.put("status", "OPEN");
      conversation.put("lastMessageAt", null);
      conversations.put(id, conversation);
      messagesByConversation.put(id, new ArrayList<>());
      return conversation;
    }

    private Map<String, Object> createMessage(Map<String, String> body) {
      long conversationId = requireLong(body, "conversationId");
      Map<String, Object> conversation = requireExists(conversations, conversationId, "CONVERSATION_NOT_FOUND", "conversationId does not exist.");
      String content = require(body, "content");

      long id = messageIds.incrementAndGet();
      Map<String, Object> studentMessage = entity(id);
      studentMessage.put("conversationId", conversationId);
      studentMessage.put("senderType", body.getOrDefault("senderType", "STUDENT").toUpperCase());
      studentMessage.put("content", content);
      studentMessage.put("contentType", "TEXT");

      List<Map<String, Object>> messages = messagesByConversation.computeIfAbsent(conversationId, key -> new ArrayList<>());
      messages.add(studentMessage);

      String now = Instant.now().toString();
      conversation.put("lastMessageAt", now);
      conversation.put("updatedAt", now);

      return Map.of(
        "conversationId", conversationId,
        "studentMessage", studentMessage,
        "agentMessageStatus", "PENDING_DIFY"
      );
    }

    private void requireUserRole(long userId, String role) {
      Map<String, Object> user = requireExists(users, userId, "USER_NOT_FOUND", "userId does not exist.");
      if (!role.equals(user.get("role"))) {
        throw new ApiException(403, "ROLE_MISMATCH", "userId must be a " + role + ".");
      }
    }

    private static Map<String, Object> requireExists(
      Map<Long, Map<String, Object>> source,
      long id,
      String code,
      String message
    ) {
      Map<String, Object> value = source.get(id);
      if (value == null) {
        throw new ApiException(404, code, message);
      }
      return value;
    }

    private static Map<String, Object> entity(long id) {
      String now = Instant.now().toString();
      Map<String, Object> entity = new LinkedHashMap<>();
      entity.put("id", id);
      entity.put("createdAt", now);
      entity.put("updatedAt", now);
      return entity;
    }

    private static String require(Map<String, String> body, String key) {
      return Optional.ofNullable(body.get(key))
        .filter(value -> !value.isBlank())
        .orElseThrow(() -> new ApiException(400, "MISSING_FIELD", key + " is required."));
    }

    private static long requireLong(Map<String, String> body, String key) {
      try {
        return Long.parseLong(require(body, key));
      } catch (NumberFormatException error) {
        throw new ApiException(400, "INVALID_NUMBER", key + " must be a number.");
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
        throw new ApiException(400, "INVALID_NUMBER", key + " must be a number.");
      }
    }

    private static String generateClassCode() {
      return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
  }

  private static final class Json {
    private static Map<String, String> readObject(InputStream input) throws IOException {
      String raw = new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();
      if (raw.isEmpty()) {
        return Map.of();
      }
      if (!raw.startsWith("{") || !raw.endsWith("}")) {
        throw new ApiException(400, "INVALID_JSON", "Request body must be a JSON object.");
      }

      Map<String, String> values = new LinkedHashMap<>();
      int index = 1;
      while (index < raw.length() - 1) {
        index = skipWhitespaceAndComma(raw, index);
        if (index >= raw.length() - 1) {
          break;
        }
        Parsed key = parseString(raw, index);
        index = skipWhitespace(raw, key.nextIndex());
        if (index >= raw.length() || raw.charAt(index) != ':') {
          throw new ApiException(400, "INVALID_JSON", "Expected ':' after JSON key.");
        }
        index = skipWhitespace(raw, index + 1);
        Parsed value = parseValue(raw, index);
        values.put(key.value(), value.value());
        index = value.nextIndex();
      }
      return values;
    }

    private static String stringify(Object value) {
      if (value == null) {
        return "null";
      }
      if (value instanceof String text) {
        return "\"" + escape(text) + "\"";
      }
      if (value instanceof Number || value instanceof Boolean) {
        return String.valueOf(value);
      }
      if (value instanceof Map<?, ?> map) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
          parts.add(stringify(String.valueOf(entry.getKey())) + ":" + stringify(entry.getValue()));
        }
        return "{" + String.join(",", parts) + "}";
      }
      if (value instanceof Iterable<?> iterable) {
        List<String> parts = new ArrayList<>();
        for (Object item : iterable) {
          parts.add(stringify(item));
        }
        return "[" + String.join(",", parts) + "]";
      }
      return stringify(String.valueOf(value));
    }

    private static Parsed parseValue(String raw, int index) {
      if (index < raw.length() && raw.charAt(index) == '"') {
        return parseString(raw, index);
      }
      int end = index;
      while (end < raw.length() && raw.charAt(end) != ',' && raw.charAt(end) != '}') {
        end++;
      }
      String value = raw.substring(index, end).trim();
      if (value.isEmpty()) {
        throw new ApiException(400, "INVALID_JSON", "Expected JSON value.");
      }
      return new Parsed(value, end);
    }

    private static Parsed parseString(String raw, int index) {
      if (index >= raw.length() || raw.charAt(index) != '"') {
        throw new ApiException(400, "INVALID_JSON", "Expected JSON string.");
      }

      StringBuilder value = new StringBuilder();
      boolean escaped = false;
      for (int cursor = index + 1; cursor < raw.length(); cursor++) {
        char current = raw.charAt(cursor);
        if (escaped) {
          value.append(switch (current) {
            case '"', '\\', '/' -> current;
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            default -> current;
          });
          escaped = false;
          continue;
        }
        if (current == '\\') {
          escaped = true;
          continue;
        }
        if (current == '"') {
          return new Parsed(value.toString(), cursor + 1);
        }
        value.append(current);
      }
      throw new ApiException(400, "INVALID_JSON", "Unclosed JSON string.");
    }

    private static int skipWhitespaceAndComma(String raw, int index) {
      int cursor = index;
      while (cursor < raw.length()) {
        char current = raw.charAt(cursor);
        if (Character.isWhitespace(current) || current == ',') {
          cursor++;
          continue;
        }
        return cursor;
      }
      return cursor;
    }

    private static int skipWhitespace(String raw, int index) {
      int cursor = index;
      while (cursor < raw.length() && Character.isWhitespace(raw.charAt(cursor))) {
        cursor++;
      }
      return cursor;
    }

    private static String escape(String value) {
      return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
    }

    private record Parsed(String value, int nextIndex) {
    }
  }
}
