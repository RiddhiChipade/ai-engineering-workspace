package com.riddhic.aiengineering.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class AssistantConfig {
    @Bean
    public Clock applicationClock(@Value("${assistant.time-zone:UTC}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }

    @Bean
    public RestClient openAiRestClient(
            @Value("${assistant.openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${assistant.openai.api-key:}") String apiKey) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(45));
        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory);
        if (apiKey != null && !apiKey.isBlank()) builder.defaultHeader("Authorization", "Bearer " + apiKey);
        return builder.build();
    }
}
