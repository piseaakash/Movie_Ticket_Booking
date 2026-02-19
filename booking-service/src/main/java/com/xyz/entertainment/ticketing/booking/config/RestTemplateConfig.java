package com.xyz.entertainment.ticketing.booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean("paymentServiceRestTemplate")
    public RestTemplate paymentServiceRestTemplate() {
        RestTemplate rt = new RestTemplate();
        rt.getInterceptors().add((request, body, execution) -> {
            String auth = currentAuthorizationHeader();
            if (auth != null) {
                request.getHeaders().set("Authorization", auth);
            }
            return execution.execute(request, body);
        });
        return rt;
    }

    private static String currentAuthorizationHeader() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servlet) {
            return servlet.getRequest().getHeader("Authorization");
        }
        return null;
    }
}
