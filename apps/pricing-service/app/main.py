"""HTTP API of the pricing service."""

from dataclasses import asdict

from fastapi import FastAPI, HTTPException, Query

from app.pricing import DAILY_RATES, PricingError, compute_quote

app = FastAPI(title="pricing-service", version="1.0.0")


@app.get("/health")
def health() -> dict:
    return {"status": "UP", "service": "pricing-service"}


@app.get("/cars")
def car_categories() -> list[dict]:
    """Categories of cars that can be priced, with their daily rate."""
    return [
        {"category": category, "dailyRate": rate, "currency": "EUR"}
        for category, rate in DAILY_RATES.items()
    ]


@app.get("/quote")
def quote(
    category: str = Query(..., description="Car category"),
    days: int = Query(..., description="Number of rental days"),
) -> dict:
    try:
        return asdict(compute_quote(category, days))
    except PricingError as error:
        raise HTTPException(status_code=400, detail=str(error)) from error
