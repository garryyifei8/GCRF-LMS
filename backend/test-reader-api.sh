#!/bin/bash

echo "=========================================="
echo "Reader Service API 集成测试"
echo "=========================================="
echo ""

BASE_URL="http://localhost:8083/api/readers"

echo "1. 测试创建读者..."
CREATE_RESULT=$(curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "readerId": "TEST2021001",
    "name": "张三",
    "gender": "MALE",
    "idCard": "110101200001011234",
    "phone": "13800138000",
    "email": "zhangsan@test.com",
    "readerType": "STUDENT",
    "department": "计算机学院",
    "majorOrPosition": "计算机科学与技术"
  }')

echo "$CREATE_RESULT" | python3 -m json.tool 2>/dev/null || echo "$CREATE_RESULT"
echo ""

# 提取读者ID
READER_ID=$(echo $CREATE_RESULT | python3 -c "import sys, json; print(json.load(sys.stdin).get('data', {}).get('id', 0))" 2>/dev/null)

if [ "$READER_ID" != "0" ] && [ -n "$READER_ID" ]; then
    echo "✓ 创建成功,读者ID: $READER_ID"
    echo ""

    echo "2. 测试根据ID查询读者..."
    curl -s -X GET "$BASE_URL/$READER_ID" | python3 -m json.tool
    echo ""

    echo "3. 测试分页查询读者列表..."
    curl -s -X GET "$BASE_URL?pageNum=1&pageSize=10" | python3 -m json.tool
    echo ""

    echo "4. 测试激活借书卡..."
    curl -s -X POST "$BASE_URL/$READER_ID/activate" | python3 -m json.tool
    echo ""
else
    echo "✗ 创建失败"
fi

echo ""
echo "=========================================="
echo "测试完成"
echo "=========================================="
