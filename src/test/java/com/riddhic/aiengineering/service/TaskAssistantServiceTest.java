package com.riddhic.aiengineering.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riddhic.aiengineering.exception.AssistantUnavailableException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskAssistantServiceTest {
    private final OpenAiResponsesClient api = mock(OpenAiResponsesClient.class);
    private final TaskAssistantToolExecutor tools = mock(TaskAssistantToolExecutor.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void executesReadOnlyToolAndReturnsFollowupModelText() throws Exception {
        when(tools.definitions()).thenReturn(List.of(Map.of("type", "function", "name", "getPendingTasks")));
        when(tools.execute("getPendingTasks", "{}")).thenReturn(Map.of("tasks", List.of(Map.of("id", 5))));
        when(api.createResponse(anyMap()))
                .thenReturn(objectMapper.readTree("""
                        {"output":[{"type":"reasoning","id":"rs_1","summary":[]},{"type":"function_call","name":"getPendingTasks","arguments":"{}","call_id":"call_1"}]}
                        """))
                .thenReturn(objectMapper.readTree("""
                        {"output":[{"type":"message","content":[{"type":"output_text","text":"You have one open task."}]}]}
                        """));

        TaskAssistantService service = new TaskAssistantService(api, tools, objectMapper, "test-key", "test-model");

        assertEquals("You have one open task.", service.answer("What should I work on?"));
        var requestCaptor = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(api, org.mockito.Mockito.times(2)).createResponse(requestCaptor.capture());
        Map<?, ?> followup = requestCaptor.getAllValues().get(1);
        List<?> input = (List<?>) followup.get("input");
        assertEquals("reasoning", ((com.fasterxml.jackson.databind.JsonNode) input.get(1)).path("type").asText());
        assertEquals("function_call", ((com.fasterxml.jackson.databind.JsonNode) input.get(2)).path("type").asText());
        assertEquals("function_call_output", ((Map<?, ?>) input.get(3)).get("type"));
        assertEquals("call_1", ((Map<?, ?>) input.get(3)).get("call_id"));
        assertEquals(false, followup.get("store"));
        verify(tools).execute("getPendingTasks", "{}");
    }

    @Test
    void missingApiKeyFailsClearlyWithoutCallingProvider() {
        TaskAssistantService service = new TaskAssistantService(api, tools, objectMapper, " ", "test-model");
        assertThrows(AssistantUnavailableException.class, () -> service.answer("Show my tasks"));
        verify(api, never()).createResponse(anyMap());
    }
}
