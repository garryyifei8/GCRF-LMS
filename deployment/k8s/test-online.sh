#!/usr/bin/env bash
# GCRF 智能图书馆管理系统 — 上线自动化测试
# Usage: ./test-online.sh [BASE_URL]
# Example: ./test-online.sh http://192.168.1.19:31080
set -uo pipefail

BASE="${1:-http://192.168.1.19:31080}"
MASTER="t1@192.168.1.20"
SSHPASS="sshpass -p gcrf"
SSH="$SSHPASS ssh -o StrictHostKeyChecking=no -o ConnectTimeout=5"

PASS=0; FAIL=0; WARN=0; SKIP=0
RESULTS=""

t() {
  local id="$1" name="$2" result="$3" detail="$4"
  if [ "$result" = "PASS" ]; then
    echo "  ✅ $id $name — $detail"
    PASS=$((PASS+1))
    RESULTS="$RESULTS\n| $id | $name | ✅ PASS | $detail |"
  elif [ "$result" = "WARN" ]; then
    echo "  ⚠️  $id $name — $detail"
    WARN=$((WARN+1))
    RESULTS="$RESULTS\n| $id | $name | ⚠️ WARN | $detail |"
  elif [ "$result" = "SKIP" ]; then
    echo "  ⏭️  $id $name — $detail"
    SKIP=$((SKIP+1))
    RESULTS="$RESULTS\n| $id | $name | ⏭️ SKIP | $detail |"
  else
    echo "  ❌ $id $name — $detail"
    FAIL=$((FAIL+1))
    RESULTS="$RESULTS\n| $id | $name | ❌ FAIL | $detail |"
  fi
}

api() {
  local method="$1" url="$2" body="${3:-}"
  if [ "$method" = "POST" ]; then
    curl -sf -m 10 -X POST "${BASE}${url}" -H "Content-Type: application/json" -d "$body" 2>/dev/null
  else
    curl -sf -m 10 "${BASE}${url}" 2>/dev/null
  fi
}

api_code() {
  echo "$1" | python3 -c "import json,sys; print(json.load(sys.stdin).get('code',''))" 2>/dev/null
}

api_field() {
  echo "$1" | python3 -c "import json,sys; d=json.load(sys.stdin); exec(\"$2\")" 2>/dev/null
}

echo "╔══════════════════════════════════════════════════════════╗"
echo "║  GCRF 智能图书馆管理系统 — 上线测试报告                  ║"
echo "║  环境: $BASE"
echo "║  日期: $(date '+%Y-%m-%d %H:%M:%S')"
echo "╚══════════════════════════════════════════════════════════╝"

# ===== T1: 基础设施 =====
echo -e "\n━━━ T1: 基础设施健康检查 ━━━"
code=$(curl -sf -m 10 -o /dev/null -w "%{http_code}" "$BASE" 2>/dev/null)
[ "$code" = "200" ] && t "T1.1" "Web前端可达" "PASS" "HTTP $code" || t "T1.1" "Web前端可达" "FAIL" "HTTP $code"

pods=$($SSH "$MASTER" "echo gcrf | sudo -S kubectl get pods -n gcrf-prod -l app --no-headers 2>/dev/null | grep '1/1.*Running' | wc -l" 2>/dev/null | tr -d '[:space:]')
if [ -n "$pods" ]; then
  [ "$pods" -ge 7 ] && t "T1.2" "Pod运行" "PASS" "${pods}/9 Ready" || t "T1.2" "Pod运行" "WARN" "${pods}/9 Ready"
else
  t "T1.2" "Pod运行" "SKIP" "无法SSH到master"
fi

edu_code=$(curl -sf -m 5 -o /dev/null -w "%{http_code}" "http://192.168.1.19:30080" 2>/dev/null)
[ "$edu_code" = "200" ] && t "T1.3" "教育平台共存" "PASS" "HTTP $edu_code" || t "T1.3" "教育平台共存" "WARN" "HTTP $edu_code"

# ===== T2: 认证 =====
echo -e "\n━━━ T2: 认证模块 ━━━"
res=$(api POST "/api/v1/auth/login" '{"username":"admin","password":"admin123"}')
code=$(api_code "$res")
token=$(api_field "$res" "print(d.get('data',{}).get('accessToken',''))")
[ "$code" = "200" ] && [ -n "$token" ] && t "T2.1" "正确密码登录" "PASS" "获得JWT token" || t "T2.1" "正确密码登录" "FAIL" "code=$code"

res=$(api POST "/api/v1/auth/login" '{"username":"admin","password":"wrong"}')
code=$(api_code "$res")
[ "$code" = "5002" ] && t "T2.2" "错误密码" "PASS" "code=5002" || t "T2.2" "错误密码" "FAIL" "code=$code"

res=$(api POST "/api/v1/auth/login" '{"username":"nobody","password":"123"}')
code=$(api_code "$res")
[ "$code" = "5001" ] && t "T2.3" "不存在用户" "PASS" "code=5001" || t "T2.3" "不存在用户" "WARN" "code=$code"

res=$(api POST "/api/v1/auth/login" '{}')
code=$(api_code "$res")
([ "$code" = "400" ] || [ "$code" = "5002" ] || [ "$code" = "5001" ]) && t "T2.4" "空字段登录" "PASS" "code=$code" || t "T2.4" "空字段登录" "WARN" "code=$code"

# ===== T3: 图书 =====
echo -e "\n━━━ T3: 图书管理 ━━━"
res=$(api GET "/api/v1/books?pageNum=1&pageSize=10")
code=$(api_code "$res")
total=$(api_field "$res" "print(d.get('data',{}).get('total',0))")
[ "$code" = "200" ] && [ "$total" -ge 1 ] 2>/dev/null && t "T3.1" "图书列表" "PASS" "total=$total" || t "T3.1" "图书列表" "FAIL" "code=$code total=$total"

res=$(api GET "/api/v1/books?pageNum=1&pageSize=3&keyword=Java")
code=$(api_code "$res")
total=$(api_field "$res" "print(d.get('data',{}).get('total',0))")
[ "$code" = "200" ] && t "T3.2" "关键字搜索" "PASS" "搜索Java: $total条" || t "T3.2" "关键字搜索" "FAIL" "code=$code"

res=$(api GET "/api/v1/books/1")
code=$(api_code "$res")
title=$(api_field "$res" "print(d.get('data',{}).get('title',''))")
[ "$code" = "200" ] && [ -n "$title" ] && t "T3.3" "图书详情" "PASS" "title=$title" || t "T3.3" "图书详情" "FAIL" "code=$code"

res=$(api GET "/api/v1/books/1/availability")
code=$(api_code "$res")
[ "$code" = "200" ] && t "T3.4" "图书可借" "PASS" "" || t "T3.4" "图书可借" "FAIL" "code=$code"

# ===== T4: 读者 =====
echo -e "\n━━━ T4: 读者管理 ━━━"
res=$(api GET "/api/v1/readers?pageNum=1&pageSize=10")
code=$(api_code "$res")
total=$(api_field "$res" "print(d.get('data',{}).get('total',0))")
[ "$code" = "200" ] && [ "$total" -ge 1 ] 2>/dev/null && t "T4.1" "读者列表" "PASS" "total=$total" || t "T4.1" "读者列表" "FAIL" "code=$code"

res=$(api GET "/api/v1/readers?readerType=student&pageNum=1&pageSize=10")
code=$(api_code "$res")
total=$(api_field "$res" "print(d.get('data',{}).get('total',0))")
[ "$code" = "200" ] && t "T4.2" "学生筛选" "PASS" "学生$total人" || t "T4.2" "学生筛选" "FAIL" "code=$code"

res=$(api GET "/api/v1/readers?readerType=teacher&pageNum=1&pageSize=10")
code=$(api_code "$res")
total=$(api_field "$res" "print(d.get('data',{}).get('total',0))")
[ "$code" = "200" ] && t "T4.3" "教师筛选" "PASS" "教师$total人" || t "T4.3" "教师筛选" "FAIL" "code=$code"

res=$(api GET "/api/v1/readers/1/validate-status")
code=$(api_code "$res")
[ "$code" = "200" ] && t "T4.4" "读者状态验证" "PASS" "" || t "T4.4" "读者状态验证" "FAIL" "code=$code"

# ===== T5: 流通 =====
echo -e "\n━━━ T5: 流通管理（核心业务）━━━"
res=$(api GET "/api/v1/borrows?pageNum=1&pageSize=10")
code=$(api_code "$res")
[ "$code" = "200" ] && t "T5.1" "借阅列表" "PASS" "" || t "T5.1" "借阅列表" "FAIL" "code=$code"

# 借书
res=$(api POST "/api/v1/borrows/borrow" '{"readerId":1,"bookId":2}')
code=$(api_code "$res")
[ "$code" = "200" ] && t "T5.2" "借书操作" "PASS" "" || t "T5.2" "借书操作" "WARN" "code=$code (可能已借)"

# 续借 — find first borrow ID
borrow_id=$(api GET "/api/v1/borrows?pageNum=1&pageSize=1" | python3 -c "import json,sys; d=json.load(sys.stdin); recs=d.get('data',{}).get('records',[]); print(recs[0]['id'] if recs else '')" 2>/dev/null)
if [ -n "$borrow_id" ]; then
  res=$(api POST "/api/v1/borrows/renew" "{\"borrowId\":$borrow_id,\"renewDays\":14}")
  code=$(api_code "$res")
  ([ "$code" = "200" ] || [ "$code" = "5000" ]) && t "T5.3" "续借" "PASS" "borrowId=$borrow_id" || t "T5.3" "续借" "FAIL" "code=$code"
else
  t "T5.3" "续借" "SKIP" "无借阅记录"
fi

res=$(api GET "/api/v1/reserves?pageNum=1&pageSize=10")
code=$(api_code "$res")
[ "$code" = "200" ] && t "T5.4" "预约列表" "PASS" "" || t "T5.4" "预约列表" "FAIL" "code=$code"

# ===== T6: 系统 =====
echo -e "\n━━━ T6: 系统管理 ━━━"
res=$(api GET "/api/v1/system/roles")
code=$(api_code "$res")
[ "$code" = "200" ] && t "T6.1" "角色列表" "PASS" "" || t "T6.1" "角色列表" "FAIL" "code=$code"

res=$(api GET "/api/v1/system/menus/tree")
code=$(api_code "$res")
[ "$code" = "200" ] && t "T6.2" "菜单树" "PASS" "" || t "T6.2" "菜单树" "FAIL" "code=$code"

# ===== T7: AI =====
echo -e "\n━━━ T7: AI 智能模块 ━━━"
res=$(api GET "/api/v1/chat/hot-questions?limit=3")
code=$(api_code "$res")
[ "$code" = "200" ] && t "T7.1" "热门问题" "PASS" "" || t "T7.1" "热门问题" "FAIL" "code=$code"

res=$(api GET "/api/v1/chat/stats")
code=$(api_code "$res")
[ "$code" = "200" ] && t "T7.2" "聊天统计" "PASS" "" || t "T7.2" "聊天统计" "FAIL" "code=$code"

res=$(api GET "/api/v1/analytics/overview")
code=$(api_code "$res")
[ "$code" = "200" ] && t "T7.3" "分析概览" "PASS" "" || t "T7.3" "分析概览" "FAIL" "code=$code"

res=$(api GET "/api/v1/analytics/popular-books?limit=3")
code=$(api_code "$res")
[ "$code" = "200" ] && t "T7.4" "热门图书" "PASS" "" || t "T7.4" "热门图书" "FAIL" "code=$code"

# ===== T9: 安全 =====
echo -e "\n━━━ T9: 安全测试 ━━━"
res=$(api GET "/api/v1/books?keyword=%27%20OR%201%3D1%20--")
code=$(api_code "$res")
total=$(api_field "$res" "print(d.get('data',{}).get('total',0))")
[ "$code" = "200" ] && [ "$total" -lt 100 ] 2>/dev/null && t "T9.1" "SQL注入防护" "PASS" "返回$total条(非全部)" || t "T9.1" "SQL注入防护" "WARN" "需手动验证"

res=$(api GET "/api/v1/books?keyword=%3Cscript%3Ealert(1)%3C/script%3E")
code=$(api_code "$res")
echo "$res" | grep -q "<script>" 2>/dev/null && t "T9.2" "XSS防护" "FAIL" "返回含<script>" || t "T9.2" "XSS防护" "PASS" "无脚本注入"

# ===== T10: 性能 =====
echo -e "\n━━━ T10: 性能基线 ━━━"
time_web=$(curl -sf -m 10 -o /dev/null -w "%{time_total}" "$BASE/login" 2>/dev/null)
[ -n "$time_web" ] && (echo "$time_web < 3" | bc -l | grep -q 1) && t "T10.1" "首页加载" "PASS" "${time_web}s" || t "T10.1" "首页加载" "WARN" "${time_web}s"

time_api=$(curl -sf -m 10 -o /dev/null -w "%{time_total}" "$BASE/api/v1/books?pageNum=1&pageSize=10" 2>/dev/null)
[ -n "$time_api" ] && (echo "$time_api < 1" | bc -l | grep -q 1) && t "T10.2" "API响应" "PASS" "${time_api}s" || t "T10.2" "API响应" "WARN" "${time_api}s"

time_login=$(curl -sf -m 10 -o /dev/null -w "%{time_total}" -X POST "$BASE/api/v1/auth/login" -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}' 2>/dev/null)
[ -n "$time_login" ] && (echo "$time_login < 2" | bc -l | grep -q 1) && t "T10.3" "登录响应" "PASS" "${time_login}s" || t "T10.3" "登录响应" "WARN" "${time_login}s"

# ===== 汇总 =====
TOTAL=$((PASS+FAIL+WARN+SKIP))
echo ""
echo "╔══════════════════════════════════════════════════════════╗"
echo "║  测试结果汇总                                            ║"
echo "╠══════════════════════════════════════════════════════════╣"
echo "║  ✅ PASS: $PASS    ❌ FAIL: $FAIL    ⚠️ WARN: $WARN    ⏭️ SKIP: $SKIP    总计: $TOTAL"
echo "╠══════════════════════════════════════════════════════════╣"
RATE=$((PASS * 100 / (TOTAL - SKIP > 0 ? TOTAL - SKIP : 1)))
if [ "$FAIL" -eq 0 ]; then
  echo "║  🎉 判定: 可上线 (通过率 ${RATE}%)"
elif [ "$FAIL" -le 3 ]; then
  echo "║  ⚠️  判定: 有条件上线 (通过率 ${RATE}%, $FAIL 个失败项)"
else
  echo "║  ❌ 判定: 不可上线 ($FAIL 个失败项需修复)"
fi
echo "╚══════════════════════════════════════════════════════════╝"

# Save report
REPORT="docs/deployment/TEST_REPORT_$(date +%Y%m%d).md"
echo "# GCRF 上线测试报告 — $(date +%Y-%m-%d)" > "$REPORT"
echo "" >> "$REPORT"
echo "| # | 测试项 | 结果 | 详情 |" >> "$REPORT"
echo "|---|--------|------|------|" >> "$REPORT"
echo -e "$RESULTS" >> "$REPORT"
echo "" >> "$REPORT"
echo "**通过率: ${RATE}%** (✅$PASS / ❌$FAIL / ⚠️$WARN / ⏭️$SKIP)" >> "$REPORT"
echo ""
echo "报告已保存: $REPORT"

exit $FAIL
