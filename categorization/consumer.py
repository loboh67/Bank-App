import os
import json
import signal
import threading
from dotenv import load_dotenv
from confluent_kafka import Consumer

from categorize import categorize_transaction
from db import get_category_id_by_key, is_db_healthy, upsert_category
from health_server import HealthServer
from merchants import (
    fetch_merchants,
    find_merchant_for_transaction,
    update_transaction_merchant,
)

load_dotenv()

KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
KAFKA_TOPIC = os.getenv("KAFKA_TOPIC", "transactions.upserted")
KAFKA_GROUP_ID = os.getenv("KAFKA_GROUP_ID", "categorizer-service")
HEALTH_HOST = os.getenv("HEALTH_HOST", "0.0.0.0")
HEALTH_PORT = int(os.getenv("HEALTH_PORT", "8080"))

def create_consumer() -> Consumer:
    return Consumer({
        "bootstrap.servers": KAFKA_BOOTSTRAP_SERVERS,
        "group.id": KAFKA_GROUP_ID,
        "auto.offset.reset": "earliest",
    })

def process_event(event: dict, merchants: list[dict]):
    """
    event é o JSON que o teu serviço Java publicou no tópico.
    Esperamos algo do género:
    {
      "transactionId": 123,
      "userId": "...",
      "accountId": 1,
      "amount": 10.5,
      "currency": "EUR",
      "direction": "DEBIT",
      "bookingDate": "2025-12-10",
      "description": "UBER *TRIP"
    }
    """
    tx_id = event["transactionId"]
    description_display = event.get("descriptionDisplay")
    description_raw = event.get("descriptionRaw")
    description = description_display or description_raw or event.get("description")
    direction = event.get("direction")

    category_key = categorize_transaction(description, direction)
    category_id = get_category_id_by_key(category_key)
    if category_id is None:
        print(f"[WARN] category_key '{category_key}' not found in categories table")
        return

    # neste exemplo meto confiança fixa a 0.9
    upsert_category(transaction_id=tx_id, category_id=category_id, confidence=0.9, source="RULE")
    print(f"[OK] tx {tx_id}: categorized as {category_key} (id={category_id})")

    match = find_merchant_for_transaction(merchants, description_raw, description_display)
    if match:
        merchant = match["merchant"]
        update_transaction_merchant(tx_id, merchant["id"])
        print(
            f"[OK] tx {tx_id}: linked to merchant {merchant['key']} "
            f"(via {match['matched_on']} ~ '{match['matched_keyword']}')"
        )

def main():
    stop_event = threading.Event()

    def _handle_stop(signum, frame):  # type: ignore[no-untyped-def]
        stop_event.set()

    signal.signal(signal.SIGINT, _handle_stop)
    signal.signal(signal.SIGTERM, _handle_stop)

    health = HealthServer(
        host=HEALTH_HOST,
        port=HEALTH_PORT,
        liveness_check=lambda: True,
        readiness_check=lambda: is_db_healthy(),
    )
    health.start()

    consumer = create_consumer()
    consumer.subscribe([KAFKA_TOPIC])

    merchants = fetch_merchants()
    print(f"Loaded {len(merchants)} merchants.")

    print(f"Categorizer listening on topic '{KAFKA_TOPIC}'...")

    try:
        while not stop_event.is_set():
            msg = consumer.poll(1.0)
            if msg is None:
                continue
            if msg.error():
                print(f"[ERROR] Kafka error: {msg.error()}")
                continue

            try:
                event = json.loads(msg.value().decode("utf-8"))
            except Exception as e:
                print(f"[ERROR] Failed to parse message: {e}")
                continue

            try:
                process_event(event, merchants)
                consumer.commit(asynchronous=False)
            except Exception as e:
                print(f"[ERROR] Failed to process event: {e}")
                # podes decidir não fazer commit aqui, depende se queres voltar a tentar depois

    finally:
        print("Shutting down consumer...")
        consumer.close()
        health.stop()

if __name__ == "__main__":
    main()
