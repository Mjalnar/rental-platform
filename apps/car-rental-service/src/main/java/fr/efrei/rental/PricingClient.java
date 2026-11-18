package fr.efrei.rental;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PricingClient {

    private final RestClient restClient;

    public PricingClient(@Value("${pricing.base-url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    public double totalFor(String category, int days) {
        Map<?, ?> body = restClient.get()
                .uri("/quote?category={category}&days={days}", category, days)
                .retrieve()
                .body(Map.class);
        if (body == null || !(body.get("total") instanceof Number total)) {
            throw new IllegalStateException("pricing-service returned no total");
        }
        return total.doubleValue();
    }
}
