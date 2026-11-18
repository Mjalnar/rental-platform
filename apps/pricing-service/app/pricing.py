"""Business rules for rental pricing."""

from dataclasses import dataclass

DAILY_RATES = {
    "economy": 45,
    "standard": 60,
    "premium": 95,
}

LONG_RENTAL_THRESHOLD_DAYS = 7
LONG_RENTAL_DISCOUNT = 0.10


class PricingError(ValueError):
    """Raised when a quote cannot be computed."""


@dataclass(frozen=True)
class Quote:
    category: str
    days: int
    daily_rate: int
    discount: float
    total: float
    currency: str = "EUR"


def compute_quote(category: str, days: int) -> Quote:
    """Return the price of a rental for a car category and a number of days."""
    if category not in DAILY_RATES:
        raise PricingError(f"unknown category: {category}")
    if days < 1:
        raise PricingError("days must be greater than or equal to 1")

    daily_rate = DAILY_RATES[category]
    discount = LONG_RENTAL_DISCOUNT if days >= LONG_RENTAL_THRESHOLD_DAYS else 0.0
    total = round(daily_rate * days * (1 - discount), 2)
    return Quote(
        category=category,
        days=days,
        daily_rate=daily_rate,
        discount=discount,
        total=total,
    )
