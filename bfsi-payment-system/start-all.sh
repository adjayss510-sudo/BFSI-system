#!/usr/bin/env bash
# Builds everything and starts all services in the background (logs in ./logs). Stop with ./stop-all.sh
set -e
mkdir -p logs
mvn -q clean package -DskipTests
for s in discovery-server api-gateway auth-service customer-service ledger-service notification-service payment-service; do
  echo "Starting $s ..."
  nohup java -jar $s/target/$s-1.0.0.jar > logs/$s.log 2>&1 &
  echo $! > logs/$s.pid
  [ "$s" = "discovery-server" ] && sleep 15 || sleep 3
done
echo "All started. Eureka: http://localhost:8761  Gateway: http://localhost:8080"
