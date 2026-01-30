package projects.caregiver_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient paystackWebClient() {
        return WebClient.builder()
                .baseUrl("https://api.paystack.co")
                .defaultHeader("Authorization", "Bearer " + System.getenv("PAYSTACK_SECRET_KEY"))
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
