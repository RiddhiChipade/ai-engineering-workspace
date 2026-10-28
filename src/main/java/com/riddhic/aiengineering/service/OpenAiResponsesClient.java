package com.riddhic.aiengineering.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
public class OpenAiResponsesClient {
    private final RestClient restClient;

    public OpenAiResponsesClient(@Qualifier("openAiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public JsonNode createResponse(Map<String, Object> request) throws RestClientException {
        return restClient.post().uri("/responses").body(request).retrieve().body(JsonNode.class);
    }
}
