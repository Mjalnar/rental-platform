package fr.efrei.rental;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CarCatalog {

    private final List<Car> cars = List.of(
            new Car("C1", "Renault Clio", "economy", true),
            new Car("C2", "Peugeot 308", "standard", true),
            new Car("C3", "Tesla Model 3", "premium", false));

    public List<Car> findAll() {
        return cars;
    }

    public Optional<Car> findById(String id) {
        return cars.stream().filter(car -> car.id().equals(id)).findFirst();
    }
}
