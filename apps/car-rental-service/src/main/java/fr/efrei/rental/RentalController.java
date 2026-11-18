package fr.efrei.rental;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class RentalController {

    private final CarCatalog catalog;
    private final PricingClient pricingClient;

    public RentalController(CarCatalog catalog, PricingClient pricingClient) {
        this.catalog = catalog;
        this.pricingClient = pricingClient;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "car-rental-service");
    }

    @GetMapping("/cars")
    public List<Car> cars() {
        return catalog.findAll();
    }

    @GetMapping("/cars/{id}")
    public Car car(@PathVariable String id) {
        return findCar(id);
    }

    @GetMapping("/cars/{id}/quote")
    public RentalQuote quote(@PathVariable String id, @RequestParam int days) {
        if (days < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "days must be >= 1");
        }
        Car car = findCar(id);
        double total = pricingClient.totalFor(car.category(), days);
        return new RentalQuote(car.id(), car.model(), days, total, "EUR");
    }

    private Car findCar(String id) {
        return catalog.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "unknown car " + id));
    }
}
