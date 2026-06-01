package com.teachagent;

import org.springframework.http.HttpStatus;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class TeachAgentService {
  private final TeachAgentRepository repository;
  private final RestClient restClient;

  public TeachAgentService(TeachAgentRepository repository) {
    this.repository = repository;
    this.restClient = RestClient.builder().build();
  }

  public Map<String, Object> getDashboardStats() {
    Map<String, Object> counts = repository.getDashboardCounts();
    return Map.of(
      "teacherStats", List.of(
        stat("已发布智能体", counts.get("publishedAgents"), "来自后端实时统计", "blue"),
        stat("活跃班级", counts.get("activeClasses"), "来自后端实时统计", "green"),
        stat("今日互动", counts.get("todayInteractions"), "来自后端实时统计", "amber"),
        stat("待批阅任务", counts.get("pendingTasks"), "来自后端实时统计", "coral")
      ),
      "adminStats", List.of(
        adminStat("教师账号", counts.get("teacherUsers"), "user"),
        adminStat("学生账号", counts.get("studentUsers"), "team"),
        adminStat("Dify 工作流", counts.get("workflowConfigs"), "api"),
        adminStat("安全事件", counts.get("securityEvents"), "security")
      )
    );
  }

  public List<Map<String, Object>> listUsers(String role) {
    String normalizedRole = blankToNull(role) == null ? null : role.toUpperCase();
    if (normalizedRole != null && !List.of("ADMIN", "TEACHER", "STUDENT").contains(normalizedRole)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ROLE", "role must be ADMIN, TEACHER, or STUDENT.");
    }
    return repository.listUsers(normalizedRole);
  }

  public List<Map<String, Object>> listCourses(long teacherId) {
    return repository.listCourses(teacherId).stream().map(this::toCourseResponse).toList();
  }

  public List<Map<String, Object>> listAgents(long teacherId) {
    return repository.listAgents(teacherId).stream().map(this::toAgentResponse).toList();
  }

  public List<Map<String, Object>> listKnowledgeFiles(long teacherId) {
    return repository.listKnowledgeFiles(teacherId).stream().map(this::toKnowledgeFileResponse).toList();
  }

  public List<Map<String, Object>> listClasses(long teacherId) {
    return repository.listClasses(teacherId).stream().map(this::toClassResponse).toList();
  }

  public List<Map<String, Object>> listStudentClasses(long studentId) {
    return repository.listStudentClasses(studentId).stream().map(this::toStudentClassResponse).toList();
  }

  public List<Map<String, Object>> listClassMembers(long classId) {
    if (!repository.existsById("classes", classId)) {
      throw new ApiException(HttpStatus.NOT_FOUND, "CLASS_NOT_FOUND", "class not found: " + classId);
    }
    return repository.listClassMembers(classId).stream().map(this::toClassMemberResponse).toList();
  }

  public List<Map<String, Object>> listConversations(long teacherId) {
    return repository.listConversations(teacherId).stream().map(this::toConversationResponse).toList();
  }

  public List<Map<String, Object>> getConversationMessages(long conversationId) {
    return repository.listConversationMessagesAsc(conversationId).stream().map(this::toMessageResponse).toList();
  }

  public List<Map<String, Object>> listTasks(long teacherId) {
    return repository.listTasks(teacherId).stream().map(this::toTaskResponse).toList();
  }

  public List<Map<String, Object>> listStudentTasks(long studentId) {
    return repository.listStudentTasks(studentId).stream().map(this::toStudentTaskResponse).toList();
  }

  public List<Map<String, Object>> listProducts(long teacherId) {
    return repository.listProducts(teacherId).stream().map(this::toProductResponse).toList();
  }

  public List<Map<String, Object>> listAttractionSpeeches(long teacherId) {
    return repository.listAttractionSpeeches(teacherId).stream().map(this::toAttractionSpeechResponse).toList();
  }

  public List<Map<String, Object>> listCustomerProfiles(long teacherId) {
    return repository.listCustomerProfiles(teacherId).stream().map(this::toCustomerProfileResponse).toList();
  }

  public List<Map<String, Object>> listTrainingScenarios(long teacherId) {
    return repository.listTrainingScenarios(teacherId).stream().map(this::toTrainingScenarioResponse).toList();
  }

  @Transactional
  public Map<String, Object> createProduct(Map<String, String> body) {
    long teacherId = requireLong(body, "teacherId");
    requireUserRole(teacherId, "TEACHER");
    return toProductResponse(repository.createProduct(
      teacherId,
      require(body, "name"),
      blankToNull(body.get("routeName")),
      blankToNull(body.get("oldInfo")),
      blankToNull(body.get("newInfo")),
      blankToNull(body.get("productContext"))
    ));
  }

  @Transactional
  public Map<String, Object> triggerProductChange(Map<String, String> body) {
    long teacherId = requireLong(body, "teacherId");
    requireUserRole(teacherId, "TEACHER");
    Map<String, Object> product = repository.createProduct(
      teacherId,
      require(body, "name"),
      blankToNull(body.get("routeName")),
      blankToNull(body.get("oldInfo")),
      blankToNull(body.get("newInfo")),
      blankToNull(body.get("productContext"))
    );
    long productId = ((Number) product.get("id")).longValue();

    Map<String, Object> outputs = runDifyWorkflow(
      teacherId,
      "product-change",
      mapOf(
        "product_name", product.get("name"),
        "change_date", body.getOrDefault("changeDate", java.time.LocalDate.now().toString()),
        "old_product_info", textOrFallback(product.get("old_info"), "暂无旧版产品信息"),
        "new_product_info", textOrFallback(product.get("new_info"), "暂无新版产品信息"),
        "compare_focus", body.getOrDefault("compareFocus", "价格、景点、路线、酒店、交通、餐食、服务标准、退改规则、适合人群"),
        "target_audience", body.getOrDefault("targetAudience", "旅游咨询实训学生")
      )
    );
    String changeSummary = outputText(outputs, "change_summary");
    Map<String, Object> updatedProduct = repository.updateProductChangeResult(productId, changeSummary, "GENERATED");
    return mapOf(
      "product", toProductResponse(updatedProduct),
      "changeSummary", changeSummary
    );
  }

  @Transactional
  public Map<String, Object> createAttractionSpeech(Map<String, String> body) {
    long teacherId = requireLong(body, "teacherId");
    requireUserRole(teacherId, "TEACHER");
    return toAttractionSpeechResponse(repository.createAttractionSpeech(
      teacherId,
      require(body, "attractionName"),
      blankToNull(body.get("geographyType")),
      blankToNull(body.get("cultureTags")),
      blankToNull(body.get("customerExperienceLevel")),
      blankToNull(body.get("customerScene")),
      blankToNull(body.get("sellingGoal")),
      blankToNull(body.get("attractionBasicInfo")),
      blankToNull(body.get("courseContext"))
    ));
  }

  @Transactional
  public Map<String, Object> generateAttractionSpeech(Map<String, String> body) {
    long teacherId = requireLong(body, "teacherId");
    requireUserRole(teacherId, "TEACHER");
    Map<String, Object> speech = repository.createAttractionSpeech(
      teacherId,
      require(body, "attractionName"),
      blankToNull(body.get("geographyType")),
      blankToNull(body.get("cultureTags")),
      blankToNull(body.get("customerExperienceLevel")),
      blankToNull(body.get("customerScene")),
      blankToNull(body.get("sellingGoal")),
      blankToNull(body.get("attractionBasicInfo")),
      blankToNull(body.get("courseContext"))
    );
    long speechId = ((Number) speech.get("id")).longValue();

    Map<String, Object> outputs = runDifyWorkflow(
      teacherId,
      "attraction-speech",
      mapOf(
        "attraction_name", speech.get("attraction_name"),
        "attraction_basic_info", textOrFallback(speech.get("attraction_basic_info"), "暂无景点基础信息"),
        "geography_type", textOrFallback(speech.get("geography_type"), "未指定"),
        "culture_tags", textOrFallback(speech.get("culture_tags"), "未指定"),
        "customer_experience_level", textOrFallback(speech.get("customer_experience_level"), "普通游客"),
        "customer_scene", textOrFallback(speech.get("customer_scene"), "旅游咨询接待"),
        "selling_goal", textOrFallback(speech.get("selling_goal"), "帮助客户理解景点价值并完成自然推荐"),
        "course_context", textOrFallback(speech.get("course_context"), "旅游咨询实训课程")
      )
    );
    String speechContent = outputText(outputs, "speech_result");
    Map<String, Object> updatedSpeech = repository.updateAttractionSpeechResult(speechId, speechContent, "GENERATED");
    return mapOf(
      "speech", toAttractionSpeechResponse(updatedSpeech),
      "speechContent", speechContent
    );
  }

  @Transactional
  public Map<String, Object> createCustomerProfile(Map<String, String> body) {
    long teacherId = requireLong(body, "teacherId");
    requireUserRole(teacherId, "TEACHER");
    return toCustomerProfileResponse(repository.createCustomerProfile(
      teacherId,
      require(body, "name"),
      blankToNull(body.get("profileType")),
      blankToNull(body.get("profileContent")),
      blankToNull(body.get("sharedBy"))
    ));
  }

  @Transactional
  public Map<String, Object> createTrainingScenario(Map<String, String> body) {
    long teacherId = requireLong(body, "teacherId");
    requireUserRole(teacherId, "TEACHER");
    return toTrainingScenarioResponse(repository.createTrainingScenario(
      teacherId,
      require(body, "name"),
      blankToNull(body.get("trainingGoal")),
      blankToNull(body.get("difficulty")),
      blankToNull(body.get("coachingMode")),
      blankToNull(body.get("description"))
    ));
  }

  public List<Map<String, Object>> listModelConfigs() {
    return List.of();
  }

  public List<Map<String, Object>> listCallLogs() {
    return repository.listCallLogs().stream().map(this::toCallLogResponse).toList();
  }

  public List<Map<String, Object>> listJobs() {
    return List.of();
  }

  public List<Map<String, Object>> listAuditLogs() {
    return repository.listAuditLogs().stream().map(this::toAuditLogResponse).toList();
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
  public Map<String, Object> updateCourse(long courseId, Map<String, String> body) {
    long teacherId = requireLong(body, "teacherId");
    requireUserRole(teacherId, "TEACHER");
    Map<String, Object> existing = repository.findById("courses", courseId)
      .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COURSE_NOT_FOUND", "courseId does not exist."));
    if (((Number) existing.get("teacher_id")).longValue() != teacherId) {
      throw new ApiException(HttpStatus.FORBIDDEN, "COURSE_TEACHER_MISMATCH", "courseId does not belong to teacherId.");
    }

    return repository.updateCourse(
      courseId,
      teacherId,
      require(body, "name"),
      blankToNull(body.get("subject")),
      blankToNull(body.get("gradeLevel")),
      blankToNull(body.get("description")),
      normalizeCourseStatus(body.getOrDefault("status", String.valueOf(existing.get("status"))))
    );
  }

  @Transactional
  public Map<String, Object> deleteCourse(long courseId, Map<String, String> body) {
    long teacherId = requireLong(body, "teacherId");
    requireUserRole(teacherId, "TEACHER");
    Map<String, Object> existing = repository.findById("courses", courseId)
      .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COURSE_NOT_FOUND", "courseId does not exist."));
    if (((Number) existing.get("teacher_id")).longValue() != teacherId) {
      throw new ApiException(HttpStatus.FORBIDDEN, "COURSE_TEACHER_MISMATCH", "courseId does not belong to teacherId.");
    }
    repository.softDeleteCourse(courseId);
    return Map.of("id", courseId, "deleted", true);
  }

  @Transactional
  public Map<String, Object> createAgent(Map<String, Object> body) {
    Map<String, String> stringBody = toStringMap(body);
    long teacherId = requireLong(stringBody, "teacherId");
    requireUserRole(teacherId, "TEACHER");
    Long courseId = optionalLong(stringBody, "courseId");
    if (courseId != null && !repository.existsById("courses", courseId)) {
      throw new ApiException(HttpStatus.NOT_FOUND, "COURSE_NOT_FOUND", "courseId does not exist.");
    }

    Map<String, Object> row = repository.createAgent(
      teacherId,
      courseId,
      require(stringBody, "name"),
      blankToNull(stringBody.get("description")),
      blankToNull(stringBody.get("systemPrompt")),
      blankToNull(stringBody.get("openingMessage")),
      blankToNull(stringBody.get("difyAppId")),
      blankToNull(stringBody.get("difyWorkflowId")),
      blankToNull(stringBody.get("difyApiKeyRef")),
      normalizeAgentStatus(stringBody.getOrDefault("status", "DRAFT"))
    );
    long agentId = ((Number) row.get("id")).longValue();
    persistAgentWorkflows(agentId, teacherId, body.get("workflows"));
    return row;
  }

  @Transactional
  public Map<String, Object> updateAgent(long agentId, Map<String, Object> body) {
    Map<String, String> stringBody = toStringMap(body);
    long teacherId = requireLong(stringBody, "teacherId");
    requireUserRole(teacherId, "TEACHER");
    Map<String, Object> existing = repository.findById("agents", agentId)
      .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "AGENT_NOT_FOUND", "agentId does not exist."));
    if (((Number) existing.get("teacher_id")).longValue() != teacherId) {
      throw new ApiException(HttpStatus.FORBIDDEN, "AGENT_TEACHER_MISMATCH", "agentId does not belong to teacherId.");
    }

    Long courseId = optionalLong(stringBody, "courseId");
    if (courseId != null && !repository.existsById("courses", courseId)) {
      throw new ApiException(HttpStatus.NOT_FOUND, "COURSE_NOT_FOUND", "courseId does not exist.");
    }

    Map<String, Object> row = repository.updateAgent(
      agentId,
      teacherId,
      courseId,
      require(stringBody, "name"),
      blankToNull(stringBody.get("description")),
      blankToNull(stringBody.get("openingMessage")),
      blankToNull(stringBody.get("difyWorkflowId")),
      normalizeAgentStatus(stringBody.getOrDefault("status", String.valueOf(existing.get("status"))))
    );
    if (body.containsKey("workflows")) {
      persistAgentWorkflows(agentId, teacherId, body.get("workflows"));
    }
    return row;
  }

  @Transactional
  public Map<String, Object> deleteAgent(long agentId, Map<String, String> body) {
    long teacherId = requireLong(body, "teacherId");
    requireUserRole(teacherId, "TEACHER");
    Map<String, Object> existing = repository.findById("agents", agentId)
      .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "AGENT_NOT_FOUND", "agentId does not exist."));
    if (((Number) existing.get("teacher_id")).longValue() != teacherId) {
      throw new ApiException(HttpStatus.FORBIDDEN, "AGENT_TEACHER_MISMATCH", "agentId does not belong to teacherId.");
    }
    repository.softDeleteAgent(agentId);
    return Map.of("id", agentId, "deleted", true);
  }

  public List<Map<String, Object>> listAgentWorkflows(long agentId) {
    if (!repository.existsById("agents", agentId)) {
      throw new ApiException(HttpStatus.NOT_FOUND, "AGENT_NOT_FOUND", "agentId does not exist.");
    }
    return repository.listAgentWorkflows(agentId).stream().map(this::toAgentWorkflowResponse).toList();
  }

  private void persistAgentWorkflows(long agentId, long teacherId, Object rawWorkflows) {
    if (rawWorkflows == null) {
      return;
    }
    if (!(rawWorkflows instanceof List<?> rawList)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_WORKFLOWS", "workflows must be an array.");
    }
    java.util.Set<String> seenKeys = new java.util.HashSet<>();
    List<Map<String, Object>> normalized = new java.util.ArrayList<>();
    for (Object raw : rawList) {
      if (!(raw instanceof Map<?, ?> rawMap)) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_WORKFLOWS", "workflow item must be an object.");
      }
      String workflowKey = blankToNull(stringValue(rawMap.get("workflowKey")));
      if (workflowKey == null) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_FIELD", "workflowKey is required for each workflow.");
      }
      if (!seenKeys.add(workflowKey)) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "DUPLICATE_WORKFLOW_KEY", "duplicate workflowKey: " + workflowKey);
      }
      String source = blankToNull(stringValue(rawMap.get("source")));
      Long workflowConfigId = parseOptionalLong(rawMap.get("workflowConfigId"));
      String customWorkflowId = blankToNull(stringValue(rawMap.get("customWorkflowId")));
      String customApiKey = blankToNull(stringValue(rawMap.get("customApiKey")));
      boolean enabled = rawMap.get("enabled") == null
        ? true
        : Boolean.parseBoolean(stringValue(rawMap.get("enabled")));

      Map<String, Object> normalizedRow = new LinkedHashMap<>();
      normalizedRow.put("workflowKey", workflowKey);
      normalizedRow.put("enabled", enabled);

      if ("CONFIG".equalsIgnoreCase(source)) {
        if (workflowConfigId == null) {
          throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_FIELD",
            "workflowConfigId is required when source=CONFIG.");
        }
        Map<String, Object> config = repository.findById("workflow_configs", workflowConfigId)
          .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WORKFLOW_CONFIG_NOT_FOUND",
            "workflowConfigId does not exist: " + workflowConfigId));
        if (((Number) config.get("teacher_id")).longValue() != teacherId) {
          throw new ApiException(HttpStatus.FORBIDDEN, "WORKFLOW_CONFIG_TEACHER_MISMATCH",
            "workflowConfigId does not belong to teacher.");
        }
        normalizedRow.put("workflowConfigId", workflowConfigId);
      } else if ("CUSTOM".equalsIgnoreCase(source)) {
        if (customWorkflowId == null) {
          throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_FIELD",
            "customWorkflowId is required when source=CUSTOM.");
        }
        normalizedRow.put("customWorkflowId", customWorkflowId);
        if (customApiKey != null) {
          normalizedRow.put("customApiKeyCiphertext", protectSecret(customApiKey));
          normalizedRow.put("customApiKeyLast4", last4(customApiKey));
        } else {
          Map<String, Object> existingBinding = findExistingBinding(agentId, workflowKey);
          if (existingBinding != null) {
            normalizedRow.put("customApiKeyCiphertext", existingBinding.get("custom_api_key_ciphertext"));
            normalizedRow.put("customApiKeyLast4", existingBinding.get("custom_api_key_last4"));
          }
        }
      } else {
        throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_WORKFLOW_SOURCE",
          "source must be CONFIG or CUSTOM.");
      }
      normalized.add(normalizedRow);
    }
    repository.replaceAgentWorkflows(agentId, normalized);
  }

  private Map<String, Object> findExistingBinding(long agentId, String workflowKey) {
    return repository.listAgentWorkflows(agentId).stream()
      .filter(row -> workflowKey.equals(row.get("workflow_key")))
      .findFirst()
      .orElse(null);
  }

  private Map<String, Object> toAgentWorkflowResponse(Map<String, Object> row) {
    Long configId = row.get("workflow_config_id") == null
      ? null
      : ((Number) row.get("workflow_config_id")).longValue();
    boolean isConfigSource = configId != null;
    String resolvedWorkflowId = isConfigSource
      ? stringValue(row.get("config_workflow_id"))
      : stringValue(row.get("custom_workflow_id"));
    String apiKeyLast4 = isConfigSource
      ? stringValue(row.get("config_api_key_last4"))
      : stringValue(row.get("custom_api_key_last4"));

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", row.get("id"));
    response.put("workflowKey", row.get("workflow_key"));
    response.put("source", isConfigSource ? "CONFIG" : "CUSTOM");
    response.put("workflowConfigId", configId == null ? "" : String.valueOf(configId));
    response.put("configName", row.get("config_name") == null ? "" : row.get("config_name"));
    response.put("customWorkflowId", row.get("custom_workflow_id") == null ? "" : row.get("custom_workflow_id"));
    response.put("resolvedWorkflowId", resolvedWorkflowId == null ? "" : resolvedWorkflowId);
    response.put("apiKeyConfigured", apiKeyLast4 != null && !apiKeyLast4.isBlank());
    response.put("apiKeyMasked", apiKeyLast4 == null || apiKeyLast4.isBlank() ? "" : "****" + apiKeyLast4);
    response.put("enabled", Boolean.TRUE.equals(row.get("enabled")) || Integer.valueOf(1).equals(row.get("enabled")));
    response.put("sortOrder", row.get("sort_order"));
    return response;
  }

  private static Map<String, String> toStringMap(Map<String, Object> source) {
    Map<String, String> result = new LinkedHashMap<>();
    if (source == null) return result;
    for (Map.Entry<String, Object> entry : source.entrySet()) {
      Object value = entry.getValue();
      if (value == null || value instanceof List<?> || value instanceof Map<?, ?>) {
        continue;
      }
      result.put(entry.getKey(), String.valueOf(value));
    }
    return result;
  }

  private static String stringValue(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private static Long parseOptionalLong(Object value) {
    if (value == null) return null;
    if (value instanceof Number n) return n.longValue();
    String s = String.valueOf(value).trim();
    if (s.isEmpty()) return null;
    try {
      return Long.parseLong(s);
    } catch (NumberFormatException error) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_NUMBER", "expected numeric id: " + s);
    }
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
    Long resolvedAgentId = agentId;
    if (resolvedAgentId == null && courseId != null) {
      resolvedAgentId = repository.findDefaultAgentIdForCourse(teacherId, courseId).orElse(null);
    }

    return repository.createClass(
      teacherId,
      courseId,
      resolvedAgentId,
      require(body, "name"),
      generateClassCode(),
      blankToNull(body.get("description"))
    );
  }

  @Transactional
  public Map<String, Object> updateClass(long classId, Map<String, String> body) {
    long teacherId = requireLong(body, "teacherId");
    requireUserRole(teacherId, "TEACHER");
    Map<String, Object> existing = repository.findById("classes", classId)
      .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CLASS_NOT_FOUND", "classId does not exist."));
    if (((Number) existing.get("teacher_id")).longValue() != teacherId) {
      throw new ApiException(HttpStatus.FORBIDDEN, "CLASS_TEACHER_MISMATCH", "classId does not belong to teacherId.");
    }
    Long courseId = optionalLong(body, "courseId");
    Long agentId = optionalLong(body, "agentId");
    if (courseId != null && !repository.existsById("courses", courseId)) {
      throw new ApiException(HttpStatus.NOT_FOUND, "COURSE_NOT_FOUND", "courseId does not exist.");
    }
    if (agentId != null && !repository.existsById("agents", agentId)) {
      throw new ApiException(HttpStatus.NOT_FOUND, "AGENT_NOT_FOUND", "agentId does not exist.");
    }
    repository.updateClass(classId, teacherId, courseId, agentId, require(body, "name"));
    return repository.findById("classes", classId).orElseThrow();
  }

  @Transactional
  public Map<String, Object> deleteClass(long classId, Map<String, String> body) {
    long teacherId = requireLong(body, "teacherId");
    requireUserRole(teacherId, "TEACHER");
    Map<String, Object> existing = repository.findById("classes", classId)
      .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CLASS_NOT_FOUND", "classId does not exist."));
    if (((Number) existing.get("teacher_id")).longValue() != teacherId) {
      throw new ApiException(HttpStatus.FORBIDDEN, "CLASS_TEACHER_MISMATCH", "classId does not belong to teacherId.");
    }
    repository.softDeleteClass(classId);
    return Map.of("id", classId, "deleted", true);
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
    Map<String, Object> conversation = repository.findConversationRuntimeContext(conversationId)
      .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND", "conversationId does not exist."));
    String content = require(body, "content");
    String senderType = body.getOrDefault("senderType", "STUDENT").toUpperCase();

    Map<String, Object> studentMessage = repository.createMessage(
      conversationId,
      senderType,
      optionalLong(body, "senderId"),
      content,
      "TEXT",
      null,
      null
    );

    repository.touchConversation(conversationId);
    if (!"STUDENT".equals(senderType)) {
      return Map.of(
        "conversationId", conversationId,
        "studentMessage", studentMessage,
        "agentMessageStatus", "SKIPPED"
      );
    }

    long startedAt = System.nanoTime();
    try {
      String workflowKey = resolveStudentWorkflowKey(body.get("workflowKey"), String.valueOf(conversation.get("title")));
      Map<String, Object> outputs = runAgentWorkflow(
        conversation,
        workflowKey,
        buildStudentWorkflowInputs(conversationId, conversation, workflowKey, content)
      );
      String reply = outputText(outputs, outputKeyForWorkflow(workflowKey));
      Map<String, Object> agentMessage = repository.createMessage(
        conversationId,
        "AGENT",
        null,
        reply,
        "MARKDOWN",
        null,
        null
      );
      int latencyMs = (int) Math.max(1, (System.nanoTime() - startedAt) / 1_000_000L);
      repository.updateMessageCallResult(((Number) agentMessage.get("id")).longValue(), latencyMs, null, null);
      repository.touchConversation(conversationId);

      return Map.of(
        "conversationId", conversationId,
        "studentMessage", studentMessage,
        "agentMessage", agentMessage,
        "agentReply", reply,
        "workflowKey", workflowKey,
        "agentMessageStatus", "COMPLETED"
      );
    } catch (ApiException error) {
      int latencyMs = (int) Math.max(1, (System.nanoTime() - startedAt) / 1_000_000L);
      String failureContent = "智能体线路调用失败：" + error.getMessage();
      Map<String, Object> errorMessage = repository.createMessage(
        conversationId,
        "SYSTEM",
        null,
        failureContent,
        "TEXT",
        null,
        null
      );
      repository.updateMessageCallResult(((Number) errorMessage.get("id")).longValue(), latencyMs, error.code(), error.getMessage());
      repository.touchConversation(conversationId);
      return Map.of(
        "conversationId", conversationId,
        "studentMessage", studentMessage,
        "agentMessage", errorMessage,
        "agentReply", failureContent,
        "agentMessageStatus", "FAILED",
        "errorCode", error.code(),
        "errorMessage", error.getMessage()
      );
    }
  }

  public List<Map<String, Object>> listWorkflowConfigs(long teacherId) {
    return repository.listWorkflowConfigs(teacherId).stream()
      .map(this::toWorkflowConfigResponse)
      .toList();
  }

  @Transactional
  public Map<String, Object> saveWorkflowConfig(Map<String, String> body) {
    long teacherId = requireLong(body, "teacherId");
    requireUserRole(teacherId, "TEACHER");

    String workflowKey = require(body, "workflowKey");
    String workflowId = require(body, "workflowId");
    String name = body.getOrDefault("name", workflowKey);
    String baseUrl = normalizeBaseUrl(body.getOrDefault("baseUrl", "https://api.dify.ai/v1"));
    String apiKey = blankToNull(body.get("apiKey"));
    boolean enabled = Boolean.parseBoolean(body.getOrDefault("enabled", "true"));

    Map<String, Object> row = repository.upsertWorkflowConfig(
      teacherId,
      workflowKey,
      name,
      baseUrl,
      workflowId,
      apiKey == null ? null : protectSecret(apiKey),
      apiKey == null ? null : last4(apiKey),
      enabled
    );

    return toWorkflowConfigResponse(row);
  }

  @Transactional
  public Map<String, Object> deleteWorkflowConfig(Map<String, String> body) {
    long teacherId = requireLong(body, "teacherId");
    requireUserRole(teacherId, "TEACHER");

    String workflowKey = require(body, "workflowKey");
    Map<String, Object> config = repository.findWorkflowConfig(teacherId, workflowKey)
      .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WORKFLOW_CONFIG_NOT_FOUND", "workflowKey has not been configured."));

    long id = ((Number) config.get("id")).longValue();
    repository.softDeleteWorkflowConfig(id);
    return Map.of(
      "workflowKey", workflowKey,
      "deleted", true,
      "message", "workflow config deleted"
    );
  }

  @Transactional
  public Map<String, Object> testWorkflowConfig(Map<String, String> body) {
    long teacherId = requireLong(body, "teacherId");
    requireUserRole(teacherId, "TEACHER");

    String workflowKey = require(body, "workflowKey");
    Map<String, Object> config = repository.findWorkflowConfig(teacherId, workflowKey)
      .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WORKFLOW_CONFIG_NOT_FOUND", "workflowKey has not been configured."));

    String apiKey = blankToNull(body.get("apiKey"));
    if (apiKey == null) {
      Object storedSecret = config.get("api_key_ciphertext");
      apiKey = storedSecret == null ? null : revealSecret(String.valueOf(storedSecret));
    }
    if (apiKey == null) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_API_KEY", "apiKey is required for the first connection test.");
    }

    String baseUrl = normalizeBaseUrl(String.valueOf(config.get("base_url")));
    long configId = ((Number) config.get("id")).longValue();

    try {
      restClient.get()
        .uri(baseUrl + "/parameters")
        .header("Authorization", "Bearer " + apiKey)
        .retrieve()
        .toBodilessEntity();

      repository.updateWorkflowTestResult(configId, true, "Dify API Key 可用");
      return Map.of(
        "workflowKey", workflowKey,
        "success", true,
        "message", "Dify 连接测试成功"
      );
    } catch (RestClientException error) {
      String message = error.getMessage() == null ? "Dify 连接测试失败" : error.getMessage();
      repository.updateWorkflowTestResult(configId, false, message);
      return Map.of(
        "workflowKey", workflowKey,
        "success", false,
        "message", message
      );
    }
  }

  private Map<String, Object> runDifyWorkflow(long teacherId, String workflowKey, Map<String, Object> inputs) {
    Map<String, Object> config = repository.findWorkflowConfig(teacherId, workflowKey)
      .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "WORKFLOW_CONFIG_NOT_FOUND",
        workflowKey + " 工作流尚未配置。"));
    if (!isTruthy(config.get("enabled"))) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "WORKFLOW_DISABLED", workflowKey + " 工作流未启用。");
    }

    Object storedSecret = config.get("api_key_ciphertext");
    String apiKey = storedSecret == null ? null : revealSecret(String.valueOf(storedSecret));
    if (apiKey == null || apiKey.isBlank()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_API_KEY", workflowKey + " 工作流未配置 API Key。");
    }

    String baseUrl = normalizeBaseUrl(String.valueOf(config.get("base_url")));
    Map<String, Object> requestBody = mapOf(
      "inputs", inputs,
      "response_mode", "blocking",
      "user", "teacher-" + teacherId
    );

    try {
      Map<String, Object> response = restClient.post()
        .uri(baseUrl + "/workflows/run")
        .header("Authorization", "Bearer " + apiKey)
        .body(requestBody)
        .retrieve()
        .body(new ParameterizedTypeReference<Map<String, Object>>() {});

      Map<String, Object> outputs = nestedMap(response, "data", "outputs");
      if (outputs.isEmpty()) {
        throw new ApiException(HttpStatus.BAD_GATEWAY, "DIFY_EMPTY_OUTPUT", "Dify 工作流未返回 outputs。");
      }
      return outputs;
    } catch (ApiException error) {
      throw error;
    } catch (RestClientException error) {
      throw new ApiException(HttpStatus.BAD_GATEWAY, "DIFY_WORKFLOW_FAILED",
        error.getMessage() == null ? "Dify 工作流调用失败。" : error.getMessage());
    }
  }

  private Map<String, Object> runAgentWorkflow(Map<String, Object> conversation, String workflowKey, Map<String, Object> inputs) {
    long agentId = ((Number) conversation.get("agent_id")).longValue();
    Map<String, Object> binding = repository.listAgentWorkflows(agentId).stream()
      .filter(row -> workflowKey.equals(row.get("workflow_key")))
      .filter(row -> isTruthy(row.get("enabled")))
      .findFirst()
      .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "AGENT_WORKFLOW_NOT_BOUND",
        "当前智能体未绑定或未启用 " + workflowKey + " 工作流。"));

    boolean configSource = binding.get("workflow_config_id") != null;
    if (configSource && !isTruthy(binding.get("config_enabled"))) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "WORKFLOW_DISABLED", workflowKey + " 工作流配置未启用。");
    }
    String workflowId = configSource
      ? stringValue(binding.get("config_workflow_id"))
      : stringValue(binding.get("custom_workflow_id"));
    String apiKeyCiphertext = configSource
      ? stringValue(binding.get("config_api_key_ciphertext"))
      : stringValue(binding.get("custom_api_key_ciphertext"));
    String apiKey = apiKeyCiphertext == null ? null : revealSecret(apiKeyCiphertext);
    String baseUrl = configSource
      ? normalizeBaseUrl(stringValue(binding.get("config_base_url")))
      : "https://api.dify.ai/v1";

    if (workflowId == null || workflowId.isBlank()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_WORKFLOW_ID", workflowKey + " 工作流未配置 workflowId。");
    }
    if (apiKey == null || apiKey.isBlank()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_API_KEY", workflowKey + " 工作流未配置 API Key。");
    }

    Map<String, Object> requestBody = mapOf(
      "inputs", inputs,
      "response_mode", "blocking",
      "user", "student-" + conversation.get("student_id")
    );

    try {
      Map<String, Object> response = restClient.post()
        .uri(baseUrl + "/workflows/run")
        .header("Authorization", "Bearer " + apiKey)
        .body(requestBody)
        .retrieve()
        .body(new ParameterizedTypeReference<Map<String, Object>>() {});

      Map<String, Object> outputs = nestedMap(response, "data", "outputs");
      if (outputs.isEmpty()) {
        throw new ApiException(HttpStatus.BAD_GATEWAY, "DIFY_EMPTY_OUTPUT", "Dify 工作流未返回 outputs。");
      }
      return outputs;
    } catch (ApiException error) {
      throw error;
    } catch (RestClientException error) {
      throw new ApiException(HttpStatus.BAD_GATEWAY, "DIFY_WORKFLOW_FAILED",
        error.getMessage() == null ? "Dify 工作流调用失败。" : error.getMessage());
    }
  }

  private Map<String, Object> buildStudentWorkflowInputs(
    long conversationId,
    Map<String, Object> conversation,
    String workflowKey,
    String studentMessage
  ) {
    String courseName = textOrFallback(conversation.get("course_name"), "旅游咨询实训课程");
    String title = textOrFallback(conversation.get("title"), courseName);
    String agentDescription = textOrFallback(conversation.get("agent_description"), textOrFallback(conversation.get("opening_message"), "旅游咨询 AI 实训"));
    String history = conversationHistory(conversationId);
    String courseContext = textOrFallback(conversation.get("course_description"), agentDescription);

    return switch (workflowKey) {
      case "simulated-customer" -> mapOf(
        "scenario", title,
        "customer_profile", agentDescription,
        "training_goal", courseContext,
        "difficulty", "中等",
        "coaching_mode", "角色扮演",
        "conversation_history", history,
        "student_message", studentMessage
      );
      case "ai-advisor" -> mapOf(
        "customer_profile", agentDescription,
        "discovered_needs", studentMessage,
        "budget", "",
        "travel_time", "",
        "companions", "",
        "destination_preferences", "",
        "constraints", "",
        "product_options", courseContext,
        "conversation_history", history
      );
      case "attraction-speech" -> mapOf(
        "attraction_name", title,
        "attraction_basic_info", studentMessage,
        "geography_type", "",
        "culture_tags", "",
        "customer_experience_level", "普通游客",
        "customer_scene", "学生景点讲解训练",
        "selling_goal", "生成可用于实训的景点讲解与追问回应",
        "course_context", courseContext
      );
      case "product-change" -> mapOf(
        "product_name", title,
        "change_date", java.time.LocalDate.now().toString(),
        "old_product_info", history.isBlank() ? "暂无历史产品信息" : history,
        "new_product_info", studentMessage,
        "compare_focus", "价格、景点、线路、酒店、交通、服务标准、退改规则、适合人群",
        "target_audience", "旅游咨询实训学生"
      );
      case "travel-simulation" -> mapOf(
        "route_name", title,
        "itinerary", courseContext,
        "node_name", "当前训练节点",
        "node_index", "",
        "customer_profile", agentDescription,
        "student_action", studentMessage,
        "simulation_state", history,
        "product_context", courseContext,
        "learning_goal", "训练旅游线路节点处置与客户沟通"
      );
      default -> mapOf(
        "course_name", courseName,
        "lesson_topic", title,
        "student_question", studentMessage,
        "student_profile", textOrFallback(conversation.get("student_name"), "学生"),
        "retrieved_context", courseContext,
        "answer_style", "面向旅游咨询实训，清晰、可操作"
      );
    };
  }

  private String conversationHistory(long conversationId) {
    List<Map<String, Object>> rows = repository.listConversationMessages(conversationId, 12);
    java.util.Collections.reverse(rows);
    return rows.stream()
      .map(row -> row.get("sender_type") + ": " + row.get("content"))
      .reduce((left, right) -> left + "\n" + right)
      .orElse("");
  }

  private static String resolveStudentWorkflowKey(String requestedWorkflowKey, String title) {
    String requested = blankToNull(requestedWorkflowKey);
    if (requested != null) return requested;
    String topic = title == null ? "" : title;
    if (topic.contains("模拟客户") || topic.contains("销售") || topic.contains("对练")) return "simulated-customer";
    if (topic.contains("参谋")) return "ai-advisor";
    if (topic.contains("景点")) return "attraction-speech";
    if (topic.contains("变更")) return "product-change";
    if (topic.contains("线路") || topic.contains("旅行模拟")) return "travel-simulation";
    return "course-qa";
  }

  private static String outputKeyForWorkflow(String workflowKey) {
    return switch (workflowKey) {
      case "simulated-customer" -> "customer_reply";
      case "ai-advisor" -> "advisor_result";
      case "attraction-speech" -> "speech_result";
      case "product-change" -> "change_summary";
      case "travel-simulation" -> "simulation_result";
      case "theory-knowledge" -> "theory_answer";
      default -> "answer";
    };
  }

  private static Map<String, Object> nestedMap(Map<String, Object> source, String firstKey, String secondKey) {
    if (source == null) return Map.of();
    Object first = source.get(firstKey);
    if (!(first instanceof Map<?, ?> firstMap)) return Map.of();
    Object second = firstMap.get(secondKey);
    if (!(second instanceof Map<?, ?> secondMap)) return Map.of();

    Map<String, Object> result = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : secondMap.entrySet()) {
      result.put(String.valueOf(entry.getKey()), entry.getValue());
    }
    return result;
  }

  private static String outputText(Map<String, Object> outputs, String key) {
    String value = blankToNull(stringValue(outputs.get(key)));
    if (value == null) {
      throw new ApiException(HttpStatus.BAD_GATEWAY, "DIFY_MISSING_OUTPUT", "Dify 工作流缺少输出变量：" + key);
    }
    return value;
  }

  private static String textOrFallback(Object value, String fallback) {
    String text = blankToNull(stringValue(value));
    return text == null ? fallback : text;
  }

  private static boolean isTruthy(Object value) {
    return value == Boolean.TRUE
      || Integer.valueOf(1).equals(value)
      || Long.valueOf(1).equals(value)
      || "1".equals(String.valueOf(value))
      || "true".equalsIgnoreCase(String.valueOf(value));
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

  private Map<String, Object> toCourseResponse(Map<String, Object> row) {
    return mapOf(
      "id", row.get("id"),
      "title", row.get("name"),
      "status", row.get("status"),
      "target", row.get("description") == null ? "" : row.get("description"),
      "classCount", row.get("class_count"),
      "agentCount", row.get("agent_count"),
      "updatedAt", row.get("updated_at"),
      "subject", row.get("subject")
    );
  }

  private Map<String, Object> toAgentResponse(Map<String, Object> row) {
    return mapOf(
      "id", row.get("id"),
      "name", row.get("name"),
      "courseId", row.get("course_id") == null ? "" : row.get("course_id"),
      "courseName", row.get("course_name") == null ? "未绑定课程" : row.get("course_name"),
      "status", row.get("status"),
      "knowledgeCount", row.get("knowledge_count"),
      "workflowId", row.get("dify_workflow_id") == null ? "" : row.get("dify_workflow_id"),
      "workflowCount", row.get("workflow_count") == null ? 0 : row.get("workflow_count"),
      "usageCount", row.get("usage_count"),
      "description", row.get("description") == null ? "" : row.get("description"),
      "openingMessage", row.get("opening_message") == null ? "" : row.get("opening_message")
    );
  }

  private Map<String, Object> toKnowledgeFileResponse(Map<String, Object> row) {
    return mapOf(
      "id", row.get("id"),
      "name", row.get("original_name"),
      "baseName", row.get("base_name"),
      "status", row.get("parse_status"),
      "size", row.get("file_size"),
      "updatedAt", row.get("updated_at"),
      "courseName", row.get("course_name") == null ? "" : row.get("course_name")
    );
  }

  private Map<String, Object> toClassResponse(Map<String, Object> row) {
    return mapOf(
      "id", row.get("id"),
      "name", row.get("name"),
      "code", row.get("class_code"),
      "courseName", row.get("course_name") == null ? "未绑定课程" : row.get("course_name"),
      "courseId", row.get("course_id") == null ? "" : row.get("course_id"),
      "studentCount", row.get("student_count"),
      "status", row.get("status"),
      "updatedAt", row.get("updated_at"),
      "agentId", row.get("agent_id") == null ? "" : String.valueOf(row.get("agent_id")),
      "agentName", row.get("agent_name") == null ? "" : row.get("agent_name"),
      "pendingTasks", row.get("pending_tasks")
    );
  }

  private Map<String, Object> toStudentClassResponse(Map<String, Object> row) {
    return mapOf(
      "id", row.get("id"),
      "name", row.get("name"),
      "teacherName", row.get("teacher_name"),
      "courseName", row.get("course_name") == null ? "未绑定课程" : row.get("course_name"),
      "agentId", row.get("agent_id") == null ? "" : String.valueOf(row.get("agent_id")),
      "agentName", row.get("agent_name") == null ? "未绑定智能体" : row.get("agent_name"),
      "status", row.get("status")
    );
  }

  private Map<String, Object> toMessageResponse(Map<String, Object> row) {
    return mapOf(
      "senderType", row.get("sender_type"),
      "content", row.get("content"),
      "createdAt", row.get("created_at")
    );
  }

  private Map<String, Object> toClassMemberResponse(Map<String, Object> row) {
    return mapOf(
      "id", row.get("id"),
      "studentId", row.get("student_id"),
      "username", row.get("username"),
      "displayName", row.get("display_name"),
      "nickname", row.get("nickname") == null ? "" : row.get("nickname"),
      "email", row.get("email") == null ? "" : row.get("email"),
      "phone", row.get("phone") == null ? "" : row.get("phone"),
      "status", row.get("status"),
      "joinedAt", row.get("joined_at"),
      "removedAt", row.get("removed_at")
    );
  }

  private Map<String, Object> toConversationResponse(Map<String, Object> row) {
    return mapOf(
      "id", row.get("id"),
      "studentName", row.get("student_name"),
      "className", row.get("class_name"),
      "topic", row.get("topic") == null ? "未命名会话" : row.get("topic"),
      "status", row.get("status"),
      "updatedAt", row.get("updated_at")
    );
  }

  private Map<String, Object> toTaskResponse(Map<String, Object> row) {
    return mapOf(
      "id", row.get("id"),
      "title", row.get("title"),
      "courseName", row.get("course_name") == null ? "未绑定课程" : row.get("course_name"),
      "className", row.get("class_name"),
      "dueAt", row.get("due_at") == null ? "" : row.get("due_at"),
      "submittedCount", row.get("submitted_count"),
      "totalCount", row.get("total_count"),
      "status", row.get("status")
    );
  }

  private Map<String, Object> toStudentTaskResponse(Map<String, Object> row) {
    return mapOf(
      "id", row.get("id"),
      "title", row.get("title"),
      "status", row.get("status"),
      "dueAt", row.get("due_at") == null ? "" : row.get("due_at"),
      "feedbackStatus", row.get("feedback_status")
    );
  }

  private Map<String, Object> toCallLogResponse(Map<String, Object> row) {
    return mapOf(
      "id", row.get("id"),
      "workflowName", row.get("workflow_name"),
      "caller", row.get("caller"),
      "status", row.get("status"),
      "latencyMs", row.get("latency_ms"),
      "createdAt", row.get("created_at")
    );
  }

  private Map<String, Object> toAuditLogResponse(Map<String, Object> row) {
    return mapOf(
      "id", row.get("id"),
      "action", row.get("action"),
      "actor", row.get("actor"),
      "target", row.get("target"),
      "createdAt", row.get("created_at")
    );
  }

  private Map<String, Object> toWorkflowConfigResponse(Map<String, Object> row) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", row.get("id"));
    response.put("teacherId", row.get("teacher_id"));
    response.put("workflowKey", row.get("workflow_key"));
    response.put("name", row.get("name"));
    response.put("baseUrl", row.get("base_url"));
    response.put("workflowId", row.get("workflow_id"));
    response.put("apiKeyConfigured", row.get("api_key_last4") != null);
    response.put("apiKeyMasked", row.get("api_key_last4") == null ? "" : "****" + row.get("api_key_last4"));
    response.put("enabled", row.get("enabled"));
    response.put("lastTestStatus", row.get("last_test_status"));
    response.put("lastTestMessage", row.get("last_test_message") == null ? "" : row.get("last_test_message"));
    response.put("lastTestAt", row.get("last_test_at") == null ? "" : row.get("last_test_at"));
    response.put("updatedAt", row.get("updated_at"));
    return response;
  }

  private Map<String, Object> toProductResponse(Map<String, Object> row) {
    return mapOf(
      "id", row.get("id"),
      "name", row.get("name"),
      "routeName", row.get("route_name") == null ? "" : row.get("route_name"),
      "contextStatus", row.get("context_status"),
      "changeStatus", row.get("change_status") == null ? "PENDING" : row.get("change_status"),
      "status", row.get("status"),
      "updatedAt", row.get("updated_at")
    );
  }

  private Map<String, Object> toAttractionSpeechResponse(Map<String, Object> row) {
    return mapOf(
      "id", row.get("id"),
      "attractionName", row.get("attraction_name"),
      "geographyType", row.get("geography_type") == null ? "" : row.get("geography_type"),
      "tagStatus", row.get("tag_status"),
      "speechStatus", row.get("speech_status"),
      "updatedAt", row.get("updated_at")
    );
  }

  private Map<String, Object> toCustomerProfileResponse(Map<String, Object> row) {
    return mapOf(
      "id", row.get("id"),
      "name", row.get("name"),
      "profileType", row.get("profile_type") == null ? "" : row.get("profile_type"),
      "sharedBy", row.get("shared_by") == null ? "" : row.get("shared_by"),
      "status", row.get("status"),
      "updatedAt", row.get("updated_at")
    );
  }

  private Map<String, Object> toTrainingScenarioResponse(Map<String, Object> row) {
    return mapOf(
      "id", row.get("id"),
      "name", row.get("name"),
      "trainingGoal", row.get("training_goal") == null ? "" : row.get("training_goal"),
      "difficulty", row.get("difficulty"),
      "coachingMode", row.get("coaching_mode"),
      "status", row.get("status"),
      "updatedAt", row.get("updated_at")
    );
  }

  private static Map<String, Object> stat(String label, Object value, String trend, String tone) {
    return Map.of(
      "label", label,
      "value", String.valueOf(value == null ? 0 : value),
      "trend", trend,
      "tone", tone
    );
  }

  private static Map<String, Object> adminStat(String label, Object value, String icon) {
    return Map.of(
      "label", label,
      "value", String.valueOf(value == null ? 0 : value),
      "icon", icon
    );
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

  private static String normalizeAgentStatus(String status) {
    String normalizedStatus = blankToNull(status) == null ? "DRAFT" : status.toUpperCase();
    if (!List.of("DRAFT", "PUBLISHED", "DISABLED").contains(normalizedStatus)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_AGENT_STATUS", "status must be DRAFT, PUBLISHED, or DISABLED.");
    }
    return normalizedStatus;
  }

  private static String normalizeCourseStatus(String status) {
    String normalizedStatus = blankToNull(status) == null ? "DRAFT" : status.toUpperCase();
    if (!List.of("DRAFT", "PUBLISHED", "ARCHIVED").contains(normalizedStatus)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_COURSE_STATUS", "status must be DRAFT, PUBLISHED, or ARCHIVED.");
    }
    return normalizedStatus;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private static Map<String, Object> mapOf(Object... keyValues) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int index = 0; index < keyValues.length; index += 2) {
      map.put(String.valueOf(keyValues[index]), keyValues[index + 1]);
    }
    return map;
  }

  private static String normalizeBaseUrl(String value) {
    String baseUrl = value == null || value.isBlank() ? "https://api.dify.ai/v1" : value.trim();
    while (baseUrl.endsWith("/")) {
      baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
    }
    return baseUrl;
  }

  private static String protectSecret(String value) {
    return "{plain}" + value;
  }

  private static String revealSecret(String value) {
    return value.startsWith("{plain}") ? value.substring("{plain}".length()) : value;
  }

  private static String last4(String value) {
    return value.length() <= 4 ? value : value.substring(value.length() - 4);
  }

  private static String generateClassCode() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
  }
}
