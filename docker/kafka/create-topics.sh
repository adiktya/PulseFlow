#!/usr/bin/env bash
set -euo pipefail

BOOTSTRAP="${KAFKA_BOOTSTRAP:-kafka:29092}"
REPLICATION="${KAFKA_REPLICATION:-1}"
PARTITIONS="${KAFKA_PARTITIONS:-3}"

topics=(
  campaign.created
  campaign.triggered
  notification.send
  notification.retry
  notification.dlq
  notification.completed
)

echo "Creating Kafka topics on ${BOOTSTRAP} ..."
for t in "${topics[@]}"; do
  kafka-topics --bootstrap-server "${BOOTSTRAP}" --create --if-not-exists \
    --topic "${t}" --replication-factor "${REPLICATION}" --partitions "${PARTITIONS}"
done

echo "Topics ready."
