from typing import Optional
import re
from unidecode import unidecode  # pip install unidecode (opcional mas ajuda)

def normalize(text: str) -> str:
    text = text or ""
    text = text.strip()
    text = unidecode(text)  # "Subsídio" -> "Subsidio"
    text = re.sub(r"\s+", " ", text)
    return text.lower()

def apply_rule_based_category(desc: str, direction: Optional[str]) -> Optional[str]:
    d = desc
    dir_up = (direction or "").upper()

    # 1) salário / subsídios
    if dir_up == "CREDIT":
        if any(x in d for x in ["subsidio natal", "subsidio", "salario", "vencimento"]):
            return "salary"
        if "wit software" in d:
            return "salary"

    # 2) investimentos
    if "to flexible cash funds" in d or "to robo portfolio" in d:
        return "investments"

    # 3) transferencias internas
    if "apple pay top-up" in d or "top-up" in d:
        return "transfers_internal"

    # 4) transferencias externas
    if "sent from revolut" in d:
        return "transfers_external"

    # 5) levantamentos
    if d.startswith("cash at"):
        return "cash_withdrawal"

    # 6) transportes
    if "uber" in d:
        return "transport"

    # 7) fast food
    if "mcdonalds" in d:
        return "fast_food"

    # 8) restauração / bares
    if any(x in d for x in ["tasca", "capicci"]):
        return "food_drink"

    # 9) subscricoes
    if any(x in d for x in ["apple.com/bill", "spotify", "amazon prime"]):
        return "subscriptions"

    # 10) software
    if "jetbrains" in d:
        return "software"
    if "wit software" in d and dir_up == "DEBIT":
        return "software"

    # 11) entertainment
    if "playstation network" in d:
        return "entertainment"

    # 12) viagens
    if any(x in d for x in ["booking.com", "salamancayfiesta.com"]):
        return "travel"

    # 13) shopping
    if any(x in d for x in ["shein", "amazon.es", "klarna*pcdiga", "douglas", "moeve"]):
        return "shopping"

    return None  # não encontrou regra

def categorize_transaction(description: Optional[str], direction: Optional[str]) -> str:
    if not description:
        return "uncategorized"

    desc_norm = normalize(description)

    # 1) tenta regras
    rule_category = apply_rule_based_category(desc_norm, direction)
    if rule_category:
        return rule_category

    # 2) aqui mais tarde chamas ML
    # cat_ml, conf = ml_predict(desc_norm, direction)
    # if conf >= 0.6:
    #     return cat_ml

    # fallback
    return "uncategorized"
