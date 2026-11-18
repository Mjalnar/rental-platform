package fr.efrei.rental;

public record RentalQuote(String carId, String model, int days, double total, String currency) {
}
