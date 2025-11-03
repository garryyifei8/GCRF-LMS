#!/bin/bash

# ==================================================================
# 国创睿峰智能图书馆管理系统 - RabbitMQ队列初始化脚本
# ==================================================================

set -e

echo "========================================="
echo "RabbitMQ 交换机和队列初始化"
echo "========================================="

RABBITMQ_HOST="localhost"
RABBITMQ_PORT="15672"
RABBITMQ_USER="admin"
RABBITMQ_PASS="gcrf_rabbitmq_2024"
RABBITMQ_VHOST="/"

# 等待RabbitMQ启动
echo "等待RabbitMQ启动..."
until curl -f -u ${RABBITMQ_USER}:${RABBITMQ_PASS} http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/overview > /dev/null 2>&1; do
    echo "RabbitMQ未就绪，等待中..."
    sleep 5
done

echo "RabbitMQ已就绪，开始创建交换机和队列..."

# ==================================================================
# 创建交换机
# ==================================================================

echo ""
echo "创建交换机..."

# 1. 图书事件交换机（Topic类型）
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X PUT \
  http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/exchanges/${RABBITMQ_VHOST}/book.events \
  -H "content-type:application/json" \
  -d '{"type":"topic","durable":true}'

# 2. 流通事件交换机（Topic类型）
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X PUT \
  http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/exchanges/${RABBITMQ_VHOST}/circulation.events \
  -H "content-type:application/json" \
  -d '{"type":"topic","durable":true}'

# 3. 读者事件交换机（Topic类型）
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X PUT \
  http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/exchanges/${RABBITMQ_VHOST}/reader.events \
  -H "content-type:application/json" \
  -d '{"type":"topic","durable":true}'

# 4. 通知交换机（Fanout类型）
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X PUT \
  http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/exchanges/${RABBITMQ_VHOST}/notification.fanout \
  -H "content-type:application/json" \
  -d '{"type":"fanout","durable":true}'

# 5. 死信交换机（Direct类型）
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X PUT \
  http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/exchanges/${RABBITMQ_VHOST}/dlx.direct \
  -H "content-type:application/json" \
  -d '{"type":"direct","durable":true}'

echo "✓ 交换机创建完成"

# ==================================================================
# 创建队列
# ==================================================================

echo ""
echo "创建队列..."

# 死信队列配置
DLX_ARGS='"x-dead-letter-exchange":"dlx.direct","x-dead-letter-routing-key":"dlx","x-message-ttl":86400000'

# 1. 图书索引更新队列
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X PUT \
  http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/queues/${RABBITMQ_VHOST}/book.index.update \
  -H "content-type:application/json" \
  -d "{\"durable\":true,\"arguments\":{${DLX_ARGS}}}"

# 2. 图书缓存更新队列
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X PUT \
  http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/queues/${RABBITMQ_VHOST}/book.cache.update \
  -H "content-type:application/json" \
  -d "{\"durable\":true,\"arguments\":{${DLX_ARGS}}}"

# 3. 借阅通知队列
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X PUT \
  http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/queues/${RABBITMQ_VHOST}/circulation.borrow.notify \
  -H "content-type:application/json" \
  -d "{\"durable\":true,\"arguments\":{${DLX_ARGS}}}"

# 4. 归还通知队列
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X PUT \
  http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/queues/${RABBITMQ_VHOST}/circulation.return.notify \
  -H "content-type:application/json" \
  -d "{\"durable\":true,\"arguments\":{${DLX_ARGS}}}"

# 5. 逾期提醒队列
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X PUT \
  http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/queues/${RABBITMQ_VHOST}/circulation.overdue.notify \
  -H "content-type:application/json" \
  -d "{\"durable\":true,\"arguments\":{${DLX_ARGS}}}"

# 6. 读者注册通知队列
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X PUT \
  http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/queues/${RABBITMQ_VHOST}/reader.register.notify \
  -H "content-type:application/json" \
  -d "{\"durable\":true,\"arguments\":{${DLX_ARGS}}}"

# 7. 邮件通知队列
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X PUT \
  http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/queues/${RABBITMQ_VHOST}/notification.email \
  -H "content-type:application/json" \
  -d "{\"durable\":true,\"arguments\":{${DLX_ARGS}}}"

# 8. 短信通知队列
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X PUT \
  http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/queues/${RABBITMQ_VHOST}/notification.sms \
  -H "content-type:application/json" \
  -d "{\"durable\":true,\"arguments\":{${DLX_ARGS}}}"

# 9. 死信队列
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X PUT \
  http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/queues/${RABBITMQ_VHOST}/dlx.queue \
  -H "content-type:application/json" \
  -d '{"durable":true}'

echo "✓ 队列创建完成"

# ==================================================================
# 绑定队列到交换机
# ==================================================================

echo ""
echo "绑定队列到交换机..."

# 图书事件绑定
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X POST \
  http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/bindings/${RABBITMQ_VHOST}/e/book.events/q/book.index.update \
  -H "content-type:application/json" \
  -d '{"routing_key":"book.created","arguments":{}}'

curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X POST \
  http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/bindings/${RABBITMQ_VHOST}/e/book.events/q/book.index.update \
  -H "content-type:application/json" \
  -d '{"routing_key":"book.updated","arguments":{}}'

curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X POST \
  http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/bindings/${RABBITMQ_VHOST}/e/book.events/q/book.cache.update \
  -H "content-type:application/json" \
  -d '{"routing_key":"book.*","arguments":{}}'

# 流通事件绑定
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X POST \
  http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/bindings/${RABBITMQ_VHOST}/e/circulation.events/q/circulation.borrow.notify \
  -H "content-type:application/json" \
  -d '{"routing_key":"circulation.borrowed","arguments":{}}'

curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X POST \
  http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/bindings/${RABBITMQ_VHOST}/e/circulation.events/q/circulation.return.notify \
  -H "content-type:application/json" \
  -d '{"routing_key":"circulation.returned","arguments":{}}'

curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X POST \
  http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/bindings/${RABBITMQ_VHOST}/e/circulation.events/q/circulation.overdue.notify \
  -H "content-type:application/json" \
  -d '{"routing_key":"circulation.overdue","arguments":{}}'

# 读者事件绑定
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X POST \
  http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/bindings/${RABBITMQ_VHOST}/e/reader.events/q/reader.register.notify \
  -H "content-type:application/json" \
  -d '{"routing_key":"reader.registered","arguments":{}}'

# 通知交换机绑定（Fanout，不需要routing key）
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X POST \
  http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/bindings/${RABBITMQ_VHOST}/e/notification.fanout/q/notification.email \
  -H "content-type:application/json" \
  -d '{"routing_key":"","arguments":{}}'

curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X POST \
  http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/bindings/${RABBITMQ_VHOST}/e/notification.fanout/q/notification.sms \
  -H "content-type:application/json" \
  -d '{"routing_key":"","arguments":{}}'

# 死信队列绑定
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X POST \
  http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/bindings/${RABBITMQ_VHOST}/e/dlx.direct/q/dlx.queue \
  -H "content-type:application/json" \
  -d '{"routing_key":"dlx","arguments":{}}'

echo "✓ 队列绑定完成"

echo ""
echo "========================================="
echo "RabbitMQ初始化完成！"
echo "========================================="
echo ""
echo "访问管理界面: http://localhost:15672"
echo "用户名: admin"
echo "密码: gcrf_rabbitmq_2024"
echo ""
