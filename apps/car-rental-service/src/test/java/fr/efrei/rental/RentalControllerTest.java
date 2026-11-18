package fr.efrei.rental;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RentalController.class)
@Import(CarCatalog.class)
class RentalControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PricingClient pricingClient;

    @Test
    void healthIsUp() throws Exception {
        mvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void listsAllCars() throws Exception {
        mvc.perform(get("/cars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void unknownCarReturns404() throws Exception {
        mvc.perform(get("/cars/UNKNOWN")).andExpect(status().isNotFound());
    }

    @Test
    void quoteUsesPricingService() throws Exception {
        when(pricingClient.totalFor("standard", 2)).thenReturn(120.0);

        mvc.perform(get("/cars/C2/quote").param("days", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(120.0))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    void quoteRejectsZeroDays() throws Exception {
        mvc.perform(get("/cars/C2/quote").param("days", "0")).andExpect(status().isBadRequest());
    }
}
