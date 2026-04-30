#!/bin/bash
# ============================================
# MySQL Master-Slave Replication Setup
# ============================================

set -e

echo "=== Setting up MySQL Master-Slave Replication ==="

# Wait for master to be ready
echo "Waiting for master to be ready..."
sleep 10

# Get master status
MASTER_STATUS=$(docker exec library-mysql-master mysql -uroot -plibrary_root_2024 -e "SHOW MASTER STATUS\G")
echo "Master Status:"
echo "$MASTER_STATUS"

MASTER_LOG_FILE=$(echo "$MASTER_STATUS" | grep "File:" | awk '{print $2}')
MASTER_LOG_POS=$(echo "$MASTER_STATUS" | grep "Position:" | awk '{print $2}')

echo "Master Log File: $MASTER_LOG_FILE"
echo "Master Log Position: $MASTER_LOG_POS"

# Configure slave
echo "Configuring slave..."
docker exec library-mysql-slave mysql -uroot -plibrary_root_2024 <<-EOSQL
    STOP SLAVE;
    CHANGE MASTER TO
        MASTER_HOST='mysql-master',
        MASTER_USER='repl',
        MASTER_PASSWORD='repl_password_2024',
        MASTER_LOG_FILE='$MASTER_LOG_FILE',
        MASTER_LOG_POS=$MASTER_LOG_POS,
        MASTER_CONNECT_RETRY=10,
        GET_MASTER_PUBLIC_KEY=1;
    START SLAVE;
    SHOW SLAVE STATUS\G
EOSQL

echo "=== Replication setup completed ==="
echo "You can verify replication status by running:"
echo "docker exec library-mysql-slave mysql -uroot -plibrary_root_2024 -e 'SHOW SLAVE STATUS\G'"
