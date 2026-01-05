import os
import re
from typing import Dict, List, Optional, Sequence, Tuple

from categorize import normalize
from db import close_pool, pool

# Table names can be overridden via env vars in case your schema differs.
MERCHANTS_TABLE = os.getenv("MERCHANTS_TABLE", "merchants")
TRANSACTIONS_TABLE = os.getenv("TRANSACTIONS_TABLE", "transactions")
DEFAULT_BATCH_SIZE = int(os.getenv("MERCHANT_BATCH_SIZE", "500"))


def normalize_for_match(text: Optional[str]) -> str:
    """
    Clean up a description for substring matching against merchant keywords.
    """
    if not text:
        return ""

    cleaned = normalize(text)
    cleaned = re.sub(r"[^a-z0-9]+", " ", cleaned)
    return re.sub(r"\s+", " ", cleaned).strip()


def fetch_merchants() -> List[Dict]:
    sql = f"SELECT id, key, name, aliases FROM {MERCHANTS_TABLE}"

    with pool.connection() as conn:
        with conn.cursor() as cur:
            cur.execute(sql)
            rows = cur.fetchall()
            columns = [col.name for col in cur.description]

    merchants = []
    for row in rows:
        data = dict(zip(columns, row))
        aliases = data.get("aliases") or []
        alias_list = list(aliases) if isinstance(aliases, (list, tuple)) else []

        keywords_raw = [data.get("key"), data.get("name"), *alias_list]
        keywords = {normalize_for_match(kw) for kw in keywords_raw if kw}
        merchants.append(
            {
                "id": data["id"],
                "key": data.get("key") or "",
                "name": data.get("name") or "",
                "keywords": keywords,
            }
        )

    return merchants


def fetch_transactions_missing_merchants(limit: int) -> List[Dict]:
    """
    Pull a batch of transactions that still have no merchant linked.
    """
    sql = f"""
        SELECT id, description_raw, description_display
        FROM {TRANSACTIONS_TABLE}
        WHERE merchant_id IS NULL
        ORDER BY id
        LIMIT %s
    """

    with pool.connection() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, (limit,))
            rows = cur.fetchall()
            columns = [col.name for col in cur.description]

    return [dict(zip(columns, row)) for row in rows]


def update_transaction_merchant(transaction_id: int, merchant_id: int) -> None:
    sql = f"""
        UPDATE {TRANSACTIONS_TABLE}
        SET merchant_id = %s, updated_at = NOW()
        WHERE id = %s AND (merchant_id IS NULL OR merchant_id <> %s)
    """

    with pool.connection() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, (merchant_id, transaction_id, merchant_id))
        conn.commit()


def _best_keyword_match(
    keywords: Sequence[str], normalized_description: str
) -> Tuple[int, Optional[str]]:
    """
    Return (score, keyword) where score is based on keyword length.
    """
    best_score = 0
    best_keyword: Optional[str] = None

    for kw in keywords:
        if kw and kw in normalized_description:
            score = len(kw)
            if score > best_score:
                best_score = score
                best_keyword = kw

    return best_score, best_keyword


def find_merchant_for_transaction(
    merchants: List[Dict], description_raw: Optional[str], description_display: Optional[str]
) -> Optional[Dict]:
    """
    Try to find the merchant that best matches the provided descriptions.
    Preference order: display description, then raw.
    """
    candidates = [
        ("description_display", normalize_for_match(description_display)),
        ("description_raw", normalize_for_match(description_raw)),
    ]

    best_match: Optional[Dict] = None
    best_score = 0

    for source, normalized_desc in candidates:
        if not normalized_desc:
            continue

        for merchant in merchants:
            score, keyword = _best_keyword_match(merchant["keywords"], normalized_desc)
            if score > best_score:
                best_score = score
                best_match = {
                    "merchant": merchant,
                    "matched_keyword": keyword,
                    "matched_on": source,
                }

    return best_match


def process_transactions(batch_size: int = DEFAULT_BATCH_SIZE) -> None:
    merchants = fetch_merchants()
    print(f"Loaded {len(merchants)} merchants from '{MERCHANTS_TABLE}'.")

    processed = 0
    linked = 0

    while True:
        txs = fetch_transactions_missing_merchants(limit=batch_size)
        if not txs:
            break

        for tx in txs:
            processed += 1
            match = find_merchant_for_transaction(
                merchants, tx.get("description_raw"), tx.get("description_display")
            )
            if not match:
                continue

            merchant = match["merchant"]
            update_transaction_merchant(tx["id"], merchant["id"])
            linked += 1
            print(
                f"[linked] tx_id={tx['id']} merchant={merchant['key']} "
                f"(via {match['matched_on']} ~ '{match['matched_keyword']}')"
            )

    print(f"Finished. Reviewed {processed} transactions, linked {linked}.")


if __name__ == "__main__":
    try:
        process_transactions()
    finally:
        close_pool()
