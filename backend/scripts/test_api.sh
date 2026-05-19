#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

status_code() {
  curl -s -o /tmp/teach-agent-response.json -w "%{http_code}" "$@"
}

json_value() {
  local key="$1"
  python3 - "$key" <<'PY'
import json
import sys

key = sys.argv[1]
with open("/tmp/teach-agent-response.json", "r", encoding="utf-8") as file:
    payload = json.load(file)

value = payload
for part in key.split("."):
    value = value[part]
print(value)
PY
}

expect_status() {
  local expected="$1"
  shift
  local actual
  actual="$(status_code "$@")"
  if [[ "$actual" != "$expected" ]]; then
    echo "Expected HTTP $expected but got HTTP $actual" >&2
    cat /tmp/teach-agent-response.json >&2
    exit 1
  fi
}

expect_status 200 "${BASE_URL}/api/health"
expect_status 201 -X POST "${BASE_URL}/api/users" \
  -H "Content-Type: application/json" \
  -d "{\"role\":\"TEACHER\",\"username\":\"teacher-$(date +%s)\",\"displayName\":\"王老师\"}"
teacher_id="$(json_value data.id)"

expect_status 201 -X POST "${BASE_URL}/api/users" \
  -H "Content-Type: application/json" \
  -d "{\"role\":\"STUDENT\",\"username\":\"student-$(date +%s)\",\"displayName\":\"李同学\"}"
student_id="$(json_value data.id)"

expect_status 201 -X POST "${BASE_URL}/api/courses" \
  -H "Content-Type: application/json" \
  -d "{\"teacherId\":${teacher_id},\"name\":\"AI 数学助教\",\"subject\":\"数学\",\"gradeLevel\":\"七年级\"}"
course_id="$(json_value data.id)"

expect_status 201 -X POST "${BASE_URL}/api/agents" \
  -H "Content-Type: application/json" \
  -d "{\"teacherId\":${teacher_id},\"courseId\":${course_id},\"name\":\"数学答疑智能体\",\"difyWorkflowId\":\"demo-workflow\"}"
agent_id="$(json_value data.id)"

expect_status 201 -X POST "${BASE_URL}/api/classes" \
  -H "Content-Type: application/json" \
  -d "{\"teacherId\":${teacher_id},\"courseId\":${course_id},\"agentId\":${agent_id},\"name\":\"七年级一班\"}"
class_id="$(json_value data.id)"
class_code="$(json_value data.class_code)"

if [[ -z "$class_code" ]]; then
  echo "Failed to read classCode from response:" >&2
  cat /tmp/teach-agent-response.json >&2
  exit 1
fi

expect_status 200 -X POST "${BASE_URL}/api/class-members/join" \
  -H "Content-Type: application/json" \
  -d "{\"studentId\":${student_id},\"classCode\":\"${class_code}\",\"nickname\":\"小李\"}"

expect_status 201 -X POST "${BASE_URL}/api/conversations" \
  -H "Content-Type: application/json" \
  -d "{\"classId\":${class_id},\"agentId\":${agent_id},\"studentId\":${student_id},\"title\":\"一次函数答疑\"}"
conversation_id="$(json_value data.id)"

expect_status 201 -X POST "${BASE_URL}/api/messages" \
  -H "Content-Type: application/json" \
  -d "{\"conversationId\":${conversation_id},\"senderType\":\"STUDENT\",\"content\":\"一次函数是什么？\"}"

echo "All API status checks passed."
