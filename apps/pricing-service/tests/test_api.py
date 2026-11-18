from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "UP"


def test_cars_lists_categories():
    response = client.get("/cars")
    assert response.status_code == 200
    categories = {car["category"] for car in response.json()}
    assert {"economy", "standard", "premium"} <= categories


def test_quote_endpoint():
    response = client.get("/quote", params={"category": "standard", "days": 2})
    assert response.status_code == 200
    assert response.json()["total"] == 120


def test_quote_with_invalid_category_returns_400():
    response = client.get("/quote", params={"category": "spaceship", "days": 2})
    assert response.status_code == 400
