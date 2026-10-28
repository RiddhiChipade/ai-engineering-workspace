package com.riddhic.aiengineering.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riddhic.aiengineering.exception.AssistantUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TaskAssistantService {
    private static final int MAX_TOOL_ROUNDS = 3;
    private static final String INSTRUCTIONS = "You are a task assistant. Use the available read-only task tools for personal task data. " +
            "The backend scopes every tool call to the authenticated user. Never request or infer another user's records. " +
            "Do not invent tasks, due dates, or completion state. Summarize tool results clearly and concisely.";

    private final OpenAiResponsesClient openAiClient;
    private final TaskAssistantToolExecutor tools;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public TaskAssistantService(OpenAiResponsesClient openAiClient,
                                TaskAssistantToolExecutor tools,
                                ObjectMapper objectMapper,
                                @Value("${assistant.openai.api-key:}") String apiKey,
                                @Value("${assistant.openai.model:gpt-6-astra}") String model) {
        this.openAiClient = openAiClient;
        this.tools = tools;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    public String answer(String message) {
        if (apiKey == null || apiKey.isBlank())
            throw new AssistantUnavailableException("OpenAI is not configured. Set OPENAI_API_KEY to enable assistant chat.");

        List<Object> input = new ArrayList<>();
        input.add(Map.of("role", "user", "content", message));
        for (int round = 0; round <= MAX_TOOL_ROUNDS; round++) {
            JsonNode response = request(input);
            JsonNode output = response.path("output");
            if (!output.isArray()) throw new AssistantUnavailableException("OpenAI returned an invalid response.");

            List<JsonNode> calls = new ArrayList<>();
            for (JsonNode item : output) {
                if ("function_call".equals(item.path("type").asText())) calls.add(item);
            }
            if (calls.isEmpty()) {
                String answer = response.path("output_text").asText("");
                if (answer.isBlank()) answer = extractOutputText(output);
                if (answer.isBlank()) throw new AssistantUnavailableException("OpenAI returned no assistant text.");
                return answer;
            }
            if (round == MAX_TOOL_ROUNDS)
                throw new AssistantUnavailableException("The assistant exceeded the allowed number of read-only tool rounds.");

            // Preserve the complete model output, including reasoning items required by reasoning models,
            // before appending the results for the function calls.
            for (JsonNode item : output) input.add(item);

            for (JsonNode call : calls) {
                String callId = call.path("call_id").asText("");
                if (callId.isBlank()) throw new AssistantUnavailableException("OpenAI returned a tool call without an identifier.");
                Map<String, Object> result = tools.execute(
                        call.path("name").asText(""), call.path("arguments").asText("{}"));
                try {
                    input.add(Map.of("type", "function_call_output", "call_id", callId,
                            "output", objectMapper.writeValueAsString(result)));
                } catch (Exception ex) {
                    throw new AssistantUnavailableException("Could not encode the assistant tool result.", ex);
                }
            }
        }
        throw new AssistantUnavailableException("The assistant could not complete this request.");
    }

    private JsonNode request(List<Object> input) {
        try {
            JsonNode response = openAiClient.createResponse(Map.of(
                    "model", model,
                    "instructions", INSTRUCTIONS,
                    "input", input,
                    "tools", tools.definitions(),
                    "parallel_tool_calls", false,
                    "max_output_tokens", 500,
                    "store", false));
            if (response == null) throw new AssistantUnavailableException("OpenAI returned an empty response.");
            return response;
        } catch (RestClientException ex) {
            throw new AssistantUnavailableException("The OpenAI assistant service is unavailable.", ex);
        }
    }

    private String extractOutputText(JsonNode output) {
        StringBuilder text = new StringBuilder();
        for (JsonNode item : output) {
            if (!"message".equals(item.path("type").asText())) continue;
            for (JsonNode content : item.path("content")) {
                if ("output_text".equals(content.path("type").asText())) {
                    if (!text.isEmpty()) text.append('\n');
                    text.append(content.path("text").asText(""));
                }
            }
        }
        return text.toString();
    }
}
