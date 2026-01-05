import os
from psycopg_pool import ConnectionPool
from dotenv import load_dotenv

load_dotenv()

DATABASE_URL = os.getenv("DATABASE_URL") or os.getenv("DB_URL")
DB_USER = os.getenv("DB_USER")
DB_PASSWORD = os.getenv("DB_PASSWORD")
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_NAME = os.getenv("DB_NAME")

if DATABASE_URL:
    conninfo = DATABASE_URL
else:
    if not DB_USER or not DB_NAME:
        raise RuntimeError(
            "Database configuration missing: set DATABASE_URL (recommended) or "
            "DB_USER and DB_NAME (and optionally DB_PASSWORD/DB_HOST/DB_PORT)."
        )
    password_part = f":{DB_PASSWORD}" if DB_PASSWORD else ""
    conninfo = f"postgresql://{DB_USER}{password_part}@{DB_HOST}:{DB_PORT}/{DB_NAME}"

pool = ConnectionPool(
    conninfo=conninfo,
    min_size=1,
    max_size=10,
    open=True,
)

def close_pool():
    pool.close()


def is_db_healthy() -> bool:
    try:
        with pool.connection() as conn:
            with conn.cursor() as cur:
                cur.execute("SELECT 1")
                cur.fetchone()
        return True
    except Exception:
        return False

def upsert_category(transaction_id: int, category_id: int, confidence: float, source: str = "RULE"):
    """
    UPSERT simples na tabela transaction_categories.
    Ajusta o nome da tabela/colunas para bater certo com o teu schema.
    """
    sql = """
    INSERT INTO transaction_categories (transaction_id, category_id, confidence, source)
    VALUES (%s, %s, %s, %s)
    ON CONFLICT (transaction_id) DO UPDATE
        SET category_id = EXCLUDED.category_id,
            confidence = EXCLUDED.confidence,
            source = EXCLUDED.source,
            updated_at = NOW();
    """

    with pool.connection() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, (transaction_id, category_id, confidence, source))
        conn.commit()


def get_category_id_by_key(category_key: str) -> int | None:
    """
    Procura na tabela categories pelo campo 'key'.
    Ex: 'groceries', 'transport', etc.
    """
    sql = "SELECT id FROM categories WHERE key = %s"

    with pool.connection() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, (category_key,))
            row = cur.fetchone()
            if not row:
                return None
            return row[0]
