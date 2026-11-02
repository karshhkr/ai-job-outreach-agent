package com.utkarsh.ai_job_outreach_agent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public RestClient openAiRestClient(
            RestClient.Builder builder,
            @org.springframework.beans.factory.annotation.Value(
                    "${app.gemini.base-url}"
            ) String baseUrl
    ) {
        return builder
                .baseUrl(baseUrl)
                .build();
    }
}