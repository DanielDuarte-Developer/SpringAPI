package com.CMASProject.SplitReceiptsProject.Configuration;

import com.CMASProject.SplitReceiptsProject.Services.TicketManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public TicketManager ticketManager(){
        return new TicketManager(new RestTemplate());
    }
}