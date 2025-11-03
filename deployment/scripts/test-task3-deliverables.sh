#!/bin/bash
# Quick validation script for Task 3 deliverables

echo "======================================"
echo "Task 3 Deliverables Validation"
echo "======================================"
echo ""

PASS=0
FAIL=0

check_file() {
  local file="$1"
  local min_lines="$2"
  
  if [ -f "$file" ]; then
    local lines=$(wc -l < "$file")
    if [ "$lines" -ge "$min_lines" ]; then
      echo "✓ $file (${lines} lines)"
      ((PASS++))
    else
      echo "✗ $file (only ${lines} lines, expected ≥${min_lines})"
      ((FAIL++))
    fi
  else
    echo "✗ $file (NOT FOUND)"
    ((FAIL++))
  fi
}

check_executable() {
  local file="$1"
  
  if [ -x "$file" ]; then
    echo "✓ $file (executable)"
    ((PASS++))
  else
    echo "✗ $file (NOT executable)"
    ((FAIL++))
  fi
}

echo "Checking documentation..."
check_file "../docs/NETWORK_SECURITY.md" 1000
check_file "../docs/NETWORK_DIAGRAM.txt" 300

echo ""
echo "Checking scripts..."
check_file "test-network-security.sh" 500
check_file "configure-firewall.sh" 400
check_file "TASK3_NETWORK_SECURITY_COMPLETION.md" 800

echo ""
echo "Checking script permissions..."
check_executable "test-network-security.sh"
check_executable "configure-firewall.sh"

echo ""
echo "======================================"
echo "Summary: $PASS passed, $FAIL failed"
echo "======================================"

if [ $FAIL -eq 0 ]; then
  echo "✅ All Task 3 deliverables verified!"
  exit 0
else
  echo "❌ Some deliverables missing or incomplete"
  exit 1
fi
