package com.teachagent;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
public class TeachAgentController {
  private final TeachAgentService service;

  public TeachAgentController(TeachAgentService service) {
    this.service = service;
  }

  @PostMapping("/api/users")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<Map<String, Object>> createUser(@RequestBody Map<String, String> body) {
    return ApiResponse.ok(service.createUser(body));
  }

  @GetMapping("/api/dashboard/stats")
  public ApiResponse<Map<String, Object>> getDashboardStats() {
    return ApiResponse.ok(service.getDashboardStats());
  }

  @GetMapping("/api/users")
  public ApiResponse<List<Map<String, Object>>> listUsers(@RequestParam(name = "role", required = false) String role) {
    return ApiResponse.ok(service.listUsers(role));
  }

  @PostMapping("/api/courses")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<Map<String, Object>> createCourse(@RequestBody Map<String, String> body) {
    return ApiResponse.ok(service.createCourse(body));
  }

  @GetMapping("/api/courses")
  public ApiResponse<List<Map<String, Object>>> listCourses(@RequestParam(name = "teacherId", defaultValue = "1") long teacherId) {
    return ApiResponse.ok(service.listCourses(teacherId));
  }

  @PutMapping("/api/courses/{courseId}")
  public ApiResponse<Map<String, Object>> updateCourse(@PathVariable long courseId, @RequestBody Map<String, String> body) {
    return ApiResponse.ok(service.updateCourse(courseId, body));
  }

  @DeleteMapping("/api/courses/{courseId}")
  public ApiResponse<Map<String, Object>> deleteCourse(@PathVariable long courseId, @RequestBody Map<String, String> body) {
    return ApiResponse.ok(service.deleteCourse(courseId, body));
  }

  @PostMapping("/api/agents")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<Map<String, Object>> createAgent(@RequestBody Map<String, Object> body) {
    return ApiResponse.ok(service.createAgent(body));
  }

  @PutMapping("/api/agents/{agentId}")
  public ApiResponse<Map<String, Object>> updateAgent(@PathVariable long agentId, @RequestBody Map<String, Object> body) {
    return ApiResponse.ok(service.updateAgent(agentId, body));
  }

  @DeleteMapping("/api/agents/{agentId}")
  public ApiResponse<Map<String, Object>> deleteAgent(@PathVariable long agentId, @RequestBody Map<String, String> body) {
    return ApiResponse.ok(service.deleteAgent(agentId, body));
  }

  @GetMapping("/api/agents")
  public ApiResponse<List<Map<String, Object>>> listAgents(@RequestParam(name = "teacherId", defaultValue = "1") long teacherId) {
    return ApiResponse.ok(service.listAgents(teacherId));
  }

  @GetMapping("/api/agents/{agentId}/workflows")
  public ApiResponse<List<Map<String, Object>>> listAgentWorkflows(@PathVariable("agentId") long agentId) {
    return ApiResponse.ok(service.listAgentWorkflows(agentId));
  }

  @GetMapping("/api/knowledge-files")
  public ApiResponse<List<Map<String, Object>>> listKnowledgeFiles(@RequestParam(name = "teacherId", defaultValue = "1") long teacherId) {
    return ApiResponse.ok(service.listKnowledgeFiles(teacherId));
  }

  @PostMapping("/api/classes")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<Map<String, Object>> createClass(@RequestBody Map<String, String> body) {
    return ApiResponse.ok(service.createClass(body));
  }

  @PutMapping("/api/classes/{classId}")
  public ApiResponse<Map<String, Object>> updateClass(@PathVariable long classId, @RequestBody Map<String, String> body) {
    return ApiResponse.ok(service.updateClass(classId, body));
  }

  @DeleteMapping("/api/classes/{classId}")
  public ApiResponse<Map<String, Object>> deleteClass(@PathVariable long classId, @RequestBody Map<String, String> body) {
    return ApiResponse.ok(service.deleteClass(classId, body));
  }

  @GetMapping("/api/classes")
  public ApiResponse<List<Map<String, Object>>> listClasses(@RequestParam(name = "teacherId", defaultValue = "1") long teacherId) {
    return ApiResponse.ok(service.listClasses(teacherId));
  }

  @GetMapping("/api/classes/{classId}/members")
  public ApiResponse<List<Map<String, Object>>> listClassMembers(@PathVariable("classId") long classId) {
    return ApiResponse.ok(service.listClassMembers(classId));
  }

  @PostMapping("/api/class-members/join")
  public ApiResponse<Map<String, Object>> joinClass(@RequestBody Map<String, String> body) {
    return ApiResponse.ok(service.joinClass(body));
  }

  @GetMapping("/api/student/classes")
  public ApiResponse<List<Map<String, Object>>> listStudentClasses(@RequestParam(name = "studentId", defaultValue = "2") long studentId) {
    return ApiResponse.ok(service.listStudentClasses(studentId));
  }

  @PostMapping("/api/conversations")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<Map<String, Object>> createConversation(@RequestBody Map<String, String> body) {
    return ApiResponse.ok(service.createConversation(body));
  }

  @GetMapping("/api/conversations")
  public ApiResponse<List<Map<String, Object>>> listConversations(@RequestParam(name = "teacherId", defaultValue = "1") long teacherId) {
    return ApiResponse.ok(service.listConversations(teacherId));
  }

  @GetMapping("/api/conversations/{conversationId}/messages")
  public ApiResponse<List<Map<String, Object>>> getConversationMessages(@PathVariable long conversationId) {
    return ApiResponse.ok(service.getConversationMessages(conversationId));
  }

  @PostMapping("/api/messages")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<Map<String, Object>> createMessage(@RequestBody Map<String, String> body) {
    return ApiResponse.ok(service.createMessage(body));
  }

  @GetMapping("/api/tasks")
  public ApiResponse<List<Map<String, Object>>> listTasks(@RequestParam(name = "teacherId", defaultValue = "1") long teacherId) {
    return ApiResponse.ok(service.listTasks(teacherId));
  }

  @GetMapping("/api/student/tasks")
  public ApiResponse<List<Map<String, Object>>> listStudentTasks(@RequestParam(name = "studentId", defaultValue = "2") long studentId) {
    return ApiResponse.ok(service.listStudentTasks(studentId));
  }

  @GetMapping("/api/products")
  public ApiResponse<List<Map<String, Object>>> listProducts(@RequestParam(name = "teacherId", defaultValue = "1") long teacherId) {
    return ApiResponse.ok(service.listProducts(teacherId));
  }

  @PostMapping("/api/products")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<Map<String, Object>> createProduct(@RequestBody Map<String, String> body) {
    return ApiResponse.ok(service.createProduct(body));
  }

  @PostMapping("/api/products/change-reminders")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<Map<String, Object>> triggerProductChange(@RequestBody Map<String, String> body) {
    return ApiResponse.ok(service.triggerProductChange(body));
  }

  @GetMapping("/api/attraction-speeches")
  public ApiResponse<List<Map<String, Object>>> listAttractionSpeeches(@RequestParam(name = "teacherId", defaultValue = "1") long teacherId) {
    return ApiResponse.ok(service.listAttractionSpeeches(teacherId));
  }

  @PostMapping("/api/attraction-speeches")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<Map<String, Object>> createAttractionSpeech(@RequestBody Map<String, String> body) {
    return ApiResponse.ok(service.createAttractionSpeech(body));
  }

  @PostMapping("/api/attraction-speeches/generate")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<Map<String, Object>> generateAttractionSpeech(@RequestBody Map<String, String> body) {
    return ApiResponse.ok(service.generateAttractionSpeech(body));
  }

  @GetMapping("/api/customer-profiles")
  public ApiResponse<List<Map<String, Object>>> listCustomerProfiles(@RequestParam(name = "teacherId", defaultValue = "1") long teacherId) {
    return ApiResponse.ok(service.listCustomerProfiles(teacherId));
  }

  @PostMapping("/api/customer-profiles")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<Map<String, Object>> createCustomerProfile(@RequestBody Map<String, String> body) {
    return ApiResponse.ok(service.createCustomerProfile(body));
  }

  @GetMapping("/api/training-scenarios")
  public ApiResponse<List<Map<String, Object>>> listTrainingScenarios(@RequestParam(name = "teacherId", defaultValue = "1") long teacherId) {
    return ApiResponse.ok(service.listTrainingScenarios(teacherId));
  }

  @PostMapping("/api/training-scenarios")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<Map<String, Object>> createTrainingScenario(@RequestBody Map<String, String> body) {
    return ApiResponse.ok(service.createTrainingScenario(body));
  }

  @GetMapping("/api/model-configs")
  public ApiResponse<List<Map<String, Object>>> listModelConfigs() {
    return ApiResponse.ok(service.listModelConfigs());
  }

  @GetMapping("/api/call-logs")
  public ApiResponse<List<Map<String, Object>>> listCallLogs() {
    return ApiResponse.ok(service.listCallLogs());
  }

  @GetMapping("/api/jobs")
  public ApiResponse<List<Map<String, Object>>> listJobs() {
    return ApiResponse.ok(service.listJobs());
  }

  @GetMapping("/api/audit-logs")
  public ApiResponse<List<Map<String, Object>>> listAuditLogs() {
    return ApiResponse.ok(service.listAuditLogs());
  }

  @GetMapping("/api/workflow-configs")
  public ApiResponse<List<Map<String, Object>>> listWorkflowConfigs(@RequestParam(name = "teacherId") long teacherId) {
    return ApiResponse.ok(service.listWorkflowConfigs(teacherId));
  }

  @PostMapping("/api/workflow-configs")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<Map<String, Object>> saveWorkflowConfig(@RequestBody Map<String, String> body) {
    return ApiResponse.ok(service.saveWorkflowConfig(body));
  }

  @DeleteMapping("/api/workflow-configs")
  public ApiResponse<Map<String, Object>> deleteWorkflowConfig(@RequestBody Map<String, String> body) {
    return ApiResponse.ok(service.deleteWorkflowConfig(body));
  }

  @PostMapping("/api/workflow-configs/test")
  public ApiResponse<Map<String, Object>> testWorkflowConfig(@RequestBody Map<String, String> body) {
    return ApiResponse.ok(service.testWorkflowConfig(body));
  }
}
