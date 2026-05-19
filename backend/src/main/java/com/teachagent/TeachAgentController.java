package com.teachagent;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
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

  @PostMapping("/api/courses")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<Map<String, Object>> createCourse(@RequestBody Map<String, String> body) {
    return ApiResponse.ok(service.createCourse(body));
  }

  @PostMapping("/api/agents")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<Map<String, Object>> createAgent(@RequestBody Map<String, String> body) {
    return ApiResponse.ok(service.createAgent(body));
  }

  @PostMapping("/api/classes")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<Map<String, Object>> createClass(@RequestBody Map<String, String> body) {
    return ApiResponse.ok(service.createClass(body));
  }

  @PostMapping("/api/class-members/join")
  public ApiResponse<Map<String, Object>> joinClass(@RequestBody Map<String, String> body) {
    return ApiResponse.ok(service.joinClass(body));
  }

  @PostMapping("/api/conversations")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<Map<String, Object>> createConversation(@RequestBody Map<String, String> body) {
    return ApiResponse.ok(service.createConversation(body));
  }

  @PostMapping("/api/messages")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<Map<String, Object>> createMessage(@RequestBody Map<String, String> body) {
    return ApiResponse.ok(service.createMessage(body));
  }
}
