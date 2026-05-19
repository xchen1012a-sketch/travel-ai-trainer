package com.teachagent;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {
  private final JdbcTemplate jdbcTemplate;

  public HealthController(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @GetMapping("/api/health")
  public ApiResponse<Map<String, Object>> health() {
    Integer database = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
    return ApiResponse.ok(Map.of(
      "status", "ok",
      "service", "teach-agent-backend",
      "database", database == null ? "unknown" : "ok",
      "time", Instant.now().toString()
    ));
  }
}
