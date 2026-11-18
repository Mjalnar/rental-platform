package fr.efrei.rental;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CarCatalogTest {

    private final CarCatalog catalog = new CarCatalog();

    @Test
    void findsCarById() {
        assertThat(catalog.findById("C2")).map(Car::category).contains("standard");
    }

    @Test
    void returnsEmptyForUnknownCar() {
        assertThat(catalog.findById("nope")).isEmpty();
    }
}
